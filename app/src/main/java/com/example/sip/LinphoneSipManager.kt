package com.example.sip

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.CallDirection
import com.example.data.SipAccountEntity
import com.example.data.SipTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import java.util.concurrent.ConcurrentHashMap

class LinphoneSipManager(private val context: Context) {

    companion object {
        private const val TAG = "LinphoneSipManager"
    }

    private var core: Core? = null
    private val accountMap = ConcurrentHashMap<Long, Account>()
    private val entityMap = ConcurrentHashMap<Long, SipAccountEntity>()
    private val accountFingerprintMap = ConcurrentHashMap<Long, String>()

    private val _registrationStates = MutableStateFlow<Map<Long, AccountRegistrationInfo>>(emptyMap())
    val registrationStates: StateFlow<Map<Long, AccountRegistrationInfo>> = _registrationStates.asStateFlow()

    private val _activeCallState = MutableStateFlow(ActiveCallUiState())
    val activeCallState: StateFlow<ActiveCallUiState> = _activeCallState.asStateFlow()

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    var onCallCompleted: ((CompletedCallRecord) -> Unit)? = null

    private var currentCallConnectedAtMs: Long = 0L
    private var currentCallWasAnswered: Boolean = false

    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            val accountId = findAccountId(account) ?: return
            val mappedStatus = when (state) {
                RegistrationState.Ok -> RegistrationStatus.REGISTERED
                RegistrationState.Progress, RegistrationState.Refreshing -> RegistrationStatus.REGISTERING
                RegistrationState.Failed -> RegistrationStatus.FAILED
                RegistrationState.Cleared, RegistrationState.None, null -> RegistrationStatus.OFFLINE
            }
            Log.d(TAG, "Account $accountId registration state: $mappedStatus ($message)")
            _registrationStates.update { current ->
                current + (accountId to AccountRegistrationInfo(accountId, mappedStatus, message))
            }
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            Log.d(TAG, "Call state changed: $state ($message)")
            val remoteAddress = call.remoteAddress
            val remoteNumber = remoteAddress.username?.takeIf { it.isNotBlank() }
                ?: remoteAddress.asStringUriOnly()
            val displayName = remoteAddress.displayName?.takeIf { it.isNotBlank() } ?: remoteNumber

            val matchedEntity = findEntityForCall(call)
            val accountId = matchedEntity?.id ?: _activeCallState.value.accountId
            val accountLabel = matchedEntity?.label ?: _activeCallState.value.accountLabel.ifBlank { "SIP" }

            when (state) {
                Call.State.OutgoingInit, Call.State.OutgoingProgress -> {
                    currentCallConnectedAtMs = 0L
                    currentCallWasAnswered = false
                    _activeCallState.value = ActiveCallUiState(
                        remoteNumber = remoteNumber,
                        remoteDisplayName = displayName,
                        accountId = accountId,
                        accountLabel = accountLabel,
                        status = ActiveCallStatus.OUTGOING,
                        direction = CallDirection.OUTGOING,
                        isMuted = !core.isMicEnabled,
                        isSpeakerOn = isSpeakerCurrentlyEnabled(core),
                        isOnHold = false,
                        durationSeconds = 0L,
                        statusMessage = message
                    )
                }

                Call.State.OutgoingRinging, Call.State.OutgoingEarlyMedia -> {
                    _activeCallState.update {
                        it.copy(
                            remoteNumber = remoteNumber,
                            remoteDisplayName = displayName,
                            accountId = accountId,
                            accountLabel = accountLabel,
                            status = ActiveCallStatus.RINGING,
                            statusMessage = message
                        )
                    }
                }

                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                    currentCallConnectedAtMs = 0L
                    currentCallWasAnswered = false
                    _activeCallState.value = ActiveCallUiState(
                        remoteNumber = remoteNumber,
                        remoteDisplayName = displayName,
                        accountId = accountId,
                        accountLabel = accountLabel,
                        status = ActiveCallStatus.INCOMING,
                        direction = CallDirection.INCOMING,
                        isMuted = false,
                        isSpeakerOn = false,
                        isOnHold = false,
                        durationSeconds = 0L,
                        statusMessage = message
                    )
                }

                Call.State.Connected, Call.State.StreamsRunning -> {
                    if (currentCallConnectedAtMs == 0L) {
                        currentCallConnectedAtMs = System.currentTimeMillis()
                    }
                    currentCallWasAnswered = true
                    _activeCallState.update {
                        it.copy(
                            remoteNumber = remoteNumber,
                            remoteDisplayName = displayName,
                            accountId = accountId,
                            accountLabel = accountLabel,
                            status = ActiveCallStatus.CONNECTED,
                            isOnHold = false,
                            isMuted = !core.isMicEnabled,
                            isSpeakerOn = isSpeakerCurrentlyEnabled(core),
                            statusMessage = message
                        )
                    }
                }

                Call.State.Paused, Call.State.Pausing, Call.State.PausedByRemote -> {
                    _activeCallState.update {
                        it.copy(
                            status = ActiveCallStatus.PAUSED,
                            isOnHold = true,
                            statusMessage = message
                        )
                    }
                }

                Call.State.End, Call.State.Released, Call.State.Error -> {
                    val prev = _activeCallState.value
                    if (prev.status != ActiveCallStatus.NONE && prev.status != ActiveCallStatus.ENDED) {
                        val elapsedSec = if (currentCallConnectedAtMs > 0L) {
                            ((System.currentTimeMillis() - currentCallConnectedAtMs) / 1000L).coerceAtLeast(0L)
                        } else {
                            call.duration.toLong().coerceAtLeast(0L)
                        }
                        val finalDirection = if (prev.direction == CallDirection.INCOMING && !currentCallWasAnswered) {
                            CallDirection.MISSED
                        } else {
                            prev.direction
                        }
                        onCallCompleted?.invoke(
                            CompletedCallRecord(
                                remoteNumber = prev.remoteNumber.ifBlank { remoteNumber },
                                accountId = prev.accountId,
                                accountLabel = prev.accountLabel.ifBlank { accountLabel },
                                direction = finalDirection,
                                durationSeconds = elapsedSec
                            )
                        )
                    }
                    currentCallConnectedAtMs = 0L
                    currentCallWasAnswered = false
                    _activeCallState.value = ActiveCallUiState()
                }

                else -> Unit
            }
        }
    }

    @Synchronized
    fun startEngine() {
        if (core != null) return
        try {
            val factory = Factory.instance()
            factory.setDebugMode(false, "SHCalling")
            val config = factory.createConfig(null)
            config.setBool("video", "capture", false)
            config.setBool("video", "display", false)
            config.setBool("video", "auto_start", false)
            config.setString("video", "device", "StaticImage: Static picture")

            val createdCore = factory.createCoreWithConfig(config, context.applicationContext)
            createdCore.isVideoCaptureEnabled = false
            createdCore.isVideoDisplayEnabled = false
            createdCore.isVideoPreviewEnabled = false
            createdCore.videoActivationPolicy.apply {
                automaticallyInitiate = false
                automaticallyAccept = false
            }
            createdCore.addListener(coreListener)
            createdCore.isAutoIterateEnabled = true
            createdCore.start()
            core = createdCore
            _engineError.value = null
            Log.i(TAG, "Linphone Core started successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start Linphone Core: ${t.message}", t)
            _engineError.value = "SIP engine init error: ${t.localizedMessage ?: "Native library unavailable"}"
        }
    }

    @Synchronized
    fun syncAccounts(accounts: List<SipAccountEntity>) {
        if (core == null) {
            startEngine()
        }
        val activeCore = core
        val incomingIds = accounts.map { it.id }.toSet()

        // Remove deleted accounts from Linphone Core
        val existingIds = accountMap.keys.toList()
        for (existingId in existingIds) {
            if (existingId !in incomingIds) {
                accountMap.remove(existingId)?.let { oldAcc ->
                    try {
                        activeCore?.removeAccount(oldAcc)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed removing old account $existingId: ${e.message}")
                    }
                }
                entityMap.remove(existingId)
                accountFingerprintMap.remove(existingId)
                _registrationStates.update { it - existingId }
            }
        }

        // Add or update accounts concurrently
        for (entity in accounts) {
            entityMap[entity.id] = entity
            val fingerprint = "${entity.username}|${entity.password}|${entity.domain}|${entity.port}|${entity.transport}"
            if (accountFingerprintMap[entity.id] != fingerprint || !accountMap.containsKey(entity.id)) {
                registerSingleAccount(entity, fingerprint)
            }
        }

        // Keep default account aligned
        val defaultEntity = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
        if (defaultEntity != null && activeCore != null) {
            accountMap[defaultEntity.id]?.let { acc ->
                activeCore.defaultAccount = acc
            }
        }
    }

    private fun registerSingleAccount(entity: SipAccountEntity, fingerprint: String) {
        val activeCore = core
        if (activeCore == null) {
            _registrationStates.update { current ->
                current + (entity.id to AccountRegistrationInfo(
                    entity.id,
                    RegistrationStatus.OFFLINE,
                    _engineError.value ?: "SIP core not running"
                ))
            }
            return
        }

        // Remove previous Linphone account instance if updating credentials
        accountMap.remove(entity.id)?.let { oldAcc ->
            try {
                activeCore.removeAccount(oldAcc)
            } catch (_: Exception) {
            }
        }

        try {
            val factory = Factory.instance()
            val cleanDomain = entity.domain.trim()
            val cleanUser = entity.username.trim()
            val transportType = when (entity.transportEnum) {
                SipTransport.UDP -> TransportType.Udp
                SipTransport.TCP -> TransportType.Tcp
            }

            val identityAddress = factory.createAddress("sip:$cleanUser@$cleanDomain")
            val serverAddress = factory.createAddress("sip:$cleanDomain:${entity.port}")
            serverAddress?.transport = transportType

            if (identityAddress == null || serverAddress == null) {
                _registrationStates.update { current ->
                    current + (entity.id to AccountRegistrationInfo(
                        entity.id,
                        RegistrationStatus.FAILED,
                        "Invalid SIP domain or username"
                    ))
                }
                return
            }

            val authInfo = factory.createAuthInfo(
                cleanUser,
                null,
                entity.password,
                null,
                null,
                cleanDomain
            )
            activeCore.addAuthInfo(authInfo)

            val accountParams = activeCore.createAccountParams()
            accountParams.identityAddress = identityAddress
            accountParams.serverAddress = serverAddress
            accountParams.isRegisterEnabled = true
            accountParams.expires = 300

            val account = activeCore.createAccount(accountParams)
            accountMap[entity.id] = account
            accountFingerprintMap[entity.id] = fingerprint

            _registrationStates.update { current ->
                current + (entity.id to AccountRegistrationInfo(
                    entity.id,
                    RegistrationStatus.REGISTERING,
                    "Registering…"
                ))
            }

            activeCore.addAccount(account)
            if (entity.isDefault || activeCore.defaultAccount == null) {
                activeCore.defaultAccount = account
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering account ${entity.label}: ${e.message}", e)
            _registrationStates.update { current ->
                current + (entity.id to AccountRegistrationInfo(
                    entity.id,
                    RegistrationStatus.FAILED,
                    e.localizedMessage ?: "Registration error"
                ))
            }
        }
    }

    fun reregisterAccount(accountId: Long) {
        val entity = entityMap[accountId] ?: return
        val fingerprint = "${entity.username}|${entity.password}|${entity.domain}|${entity.port}|${entity.transport}"
        registerSingleAccount(entity, fingerprint)
    }

    fun setActiveOutgoingAccount(accountId: Long) {
        val activeCore = core ?: return
        val account = accountMap[accountId] ?: return
        activeCore.defaultAccount = account
    }

    fun makeCall(destination: String, account: SipAccountEntity): Result<Unit> {
        val activeCore = core ?: return Result.failure(IllegalStateException("SIP engine is not initialized"))
        val linphoneAccount = accountMap[account.id]
            ?: return Result.failure(IllegalStateException("SIP account is not loaded in core"))

        val trimmed = destination.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a destination number"))
        }

        return try {
            activeCore.defaultAccount = linphoneAccount
            val targetUri = if (trimmed.startsWith("sip:", ignoreCase = true) || trimmed.contains("@")) {
                if (trimmed.startsWith("sip:", ignoreCase = true)) trimmed else "sip:$trimmed"
            } else {
                "sip:$trimmed@${account.domain.trim()}"
            }

            val remoteAddress = activeCore.interpretUrl(targetUri)
                ?: return Result.failure(IllegalArgumentException("Invalid SIP destination: $trimmed"))

            val callParams = activeCore.createCallParams(null)
                ?: return Result.failure(IllegalStateException("Unable to create call parameters"))
            callParams.isAudioEnabled = true
            callParams.isVideoEnabled = false
            callParams.account = linphoneAccount

            _activeCallState.value = ActiveCallUiState(
                remoteNumber = trimmed,
                remoteDisplayName = trimmed,
                accountId = account.id,
                accountLabel = account.label,
                status = ActiveCallStatus.OUTGOING,
                direction = CallDirection.OUTGOING,
                isMuted = false,
                isSpeakerOn = false,
                isOnHold = false,
                durationSeconds = 0L,
                statusMessage = "Calling…"
            )

            activeCore.inviteAddressWithParams(remoteAddress, callParams)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to place call: ${e.message}", e)
            _activeCallState.value = ActiveCallUiState()
            Result.failure(e)
        }
    }

    fun answerCall() {
        val activeCore = core ?: return
        val call = activeCore.currentCall ?: activeCore.calls.firstOrNull() ?: return
        try {
            val params = activeCore.createCallParams(call)
            if (params != null) {
                params.isAudioEnabled = true
                params.isVideoEnabled = false
                call.acceptWithParams(params)
            } else {
                call.accept()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call: ${e.message}", e)
        }
    }

    fun declineCall() {
        endCall()
    }

    fun endCall() {
        val activeCore = core
        try {
            val call = activeCore?.currentCall ?: activeCore?.calls?.firstOrNull()
            if (call != null) {
                call.terminate()
            } else {
                activeCore?.terminateAllCalls()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error terminating call: ${e.message}", e)
        }
        _activeCallState.value = ActiveCallUiState()
    }

    fun toggleMute() {
        val activeCore = core ?: return
        val newMicEnabled = !activeCore.isMicEnabled
        activeCore.isMicEnabled = newMicEnabled
        _activeCallState.update { it.copy(isMuted = !newMicEnabled) }
    }

    fun toggleSpeaker() {
        val activeCore = core ?: return
        val currentlyOnSpeaker = _activeCallState.value.isSpeakerOn
        val targetSpeaker = !currentlyOnSpeaker

        try {
            val audioDevices = activeCore.audioDevices
            val targetDevice = if (targetSpeaker) {
                audioDevices.firstOrNull {
                    it.type == AudioDevice.Type.Speaker &&
                        (it.capabilities == AudioDevice.Capabilities.CapabilityPlay ||
                            it.capabilities == AudioDevice.Capabilities.CapabilityAll)
                }
            } else {
                audioDevices.firstOrNull {
                    (it.type == AudioDevice.Type.Earpiece || it.type == AudioDevice.Type.Bluetooth) &&
                        (it.capabilities == AudioDevice.Capabilities.CapabilityPlay ||
                            it.capabilities == AudioDevice.Capabilities.CapabilityAll)
                }
            }
            if (targetDevice != null) {
                activeCore.currentCall?.outputAudioDevice = targetDevice
                activeCore.outputAudioDevice = targetDevice
            }
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = targetSpeaker
        } catch (e: Exception) {
            Log.w(TAG, "Speaker toggle warning: ${e.message}")
        }
        _activeCallState.update { it.copy(isSpeakerOn = targetSpeaker) }
    }

    fun toggleHold() {
        val activeCore = core ?: return
        val call = activeCore.currentCall ?: activeCore.calls.firstOrNull() ?: return
        try {
            if (_activeCallState.value.isOnHold || call.state == Call.State.Paused) {
                call.resume()
            } else {
                call.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hold toggle error: ${e.message}", e)
        }
    }

    fun sendDtmf(digit: Char) {
        val activeCore = core ?: return
        val call = activeCore.currentCall ?: activeCore.calls.firstOrNull() ?: return
        try {
            call.sendDtmf(digit)
        } catch (e: Exception) {
            Log.w(TAG, "Send DTMF failed: ${e.message}")
        }
    }

    fun tickCallDuration() {
        if (_activeCallState.value.status == ActiveCallStatus.CONNECTED && currentCallConnectedAtMs > 0L) {
            val seconds = ((System.currentTimeMillis() - currentCallConnectedAtMs) / 1000L).coerceAtLeast(0L)
            _activeCallState.update { it.copy(durationSeconds = seconds) }
        }
    }

    private fun isSpeakerCurrentlyEnabled(activeCore: Core): Boolean {
        val device = activeCore.currentCall?.outputAudioDevice ?: activeCore.outputAudioDevice
        return device?.type == AudioDevice.Type.Speaker
    }

    private fun findAccountId(account: Account): Long? {
        return accountMap.entries.firstOrNull { it.value == account }?.key
    }

    private fun findEntityForCall(call: Call): SipAccountEntity? {
        val toUser = call.callLog.localAddress?.username
        val toDomain = call.callLog.localAddress?.domain
        if (!toUser.isNullOrBlank() && !toDomain.isNullOrBlank()) {
            val matched = entityMap.values.firstOrNull {
                it.username.equals(toUser, ignoreCase = true) &&
                    it.domain.equals(toDomain, ignoreCase = true)
            }
            if (matched != null) return matched
        }
        val defaultAcc = core?.defaultAccount ?: return null
        val id = findAccountId(defaultAcc) ?: return null
        return entityMap[id]
    }

    @Synchronized
    fun stopEngine() {
        try {
            core?.removeListener(coreListener)
            core?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Linphone core: ${e.message}")
        }
        core = null
        accountMap.clear()
        accountFingerprintMap.clear()
    }
}
