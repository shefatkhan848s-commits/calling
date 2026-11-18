package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ShCallingApplication
import com.example.data.CallLogEntity
import com.example.data.SipAccountEntity
import com.example.data.SipTransport
import com.example.sip.AccountRegistrationInfo
import com.example.sip.ActiveCallStatus
import com.example.sip.ActiveCallUiState
import com.example.sip.DtmfTonePlayer
import com.example.sip.RegistrationStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SipAccountUiModel(
    val entity: SipAccountEntity,
    val status: RegistrationStatus,
    val statusMessage: String
)

data class SoftphoneUiState(
    val accounts: List<SipAccountUiModel> = emptyList(),
    val activeAccount: SipAccountUiModel? = null,
    val recentCalls: List<CallLogEntity> = emptyList(),
    val dialedNumber: String = "",
    val activeCall: ActiveCallUiState = ActiveCallUiState(),
    val engineError: String? = null
)

class SoftphoneViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShCallingApplication
    private val repository = app.repository
    private val sipManager = app.sipManager
    private val dtmfPlayer = DtmfTonePlayer()

    private val _dialedNumber = MutableStateFlow("")
    val dialedNumber: StateFlow<String> = _dialedNumber.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<SoftphoneUiState> = combine(
        repository.allAccounts,
        sipManager.registrationStates,
        repository.recentCalls,
        _dialedNumber,
        sipManager.activeCallState,
        sipManager.engineError
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val accounts = values[0] as List<SipAccountEntity>
        @Suppress("UNCHECKED_CAST")
        val regMap = values[1] as Map<Long, AccountRegistrationInfo>
        @Suppress("UNCHECKED_CAST")
        val recents = values[2] as List<CallLogEntity>
        val dialed = values[3] as String
        val activeCall = values[4] as ActiveCallUiState
        val engineError = values[5] as String?

        val uiAccounts = accounts.map { entity ->
            val info = regMap[entity.id]
            SipAccountUiModel(
                entity = entity,
                status = info?.status ?: RegistrationStatus.OFFLINE,
                statusMessage = info?.message ?: "Offline"
            )
        }
        val activeAccount = uiAccounts.firstOrNull { it.entity.isDefault } ?: uiAccounts.firstOrNull()

        SoftphoneUiState(
            accounts = uiAccounts,
            activeAccount = activeAccount,
            recentCalls = recents,
            dialedNumber = dialed,
            activeCall = activeCall,
            engineError = engineError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SoftphoneUiState()
    )

    init {
        sipManager.startEngine()
        sipManager.onCallCompleted = { record ->
            viewModelScope.launch {
                repository.logCall(
                    CallLogEntity(
                        remoteNumber = record.remoteNumber,
                        accountId = record.accountId,
                        accountLabel = record.accountLabel,
                        direction = record.direction.name,
                        durationSeconds = record.durationSeconds,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // Keep Linphone Core concurrently registered with all saved Room accounts
        viewModelScope.launch {
            repository.allAccounts.collectLatest { accounts ->
                sipManager.syncAccounts(accounts)
            }
        }

        // Update live call timer every second while connected
        viewModelScope.launch {
            sipManager.activeCallState.collectLatest { callState ->
                if (callState.status == ActiveCallStatus.CONNECTED) {
                    while (true) {
                        sipManager.tickCallDuration()
                        delay(1000L)
                    }
                }
            }
        }
    }

    fun appendDigit(digit: String) {
        if (digit.length == 1) {
            dtmfPlayer.playDigit(digit[0])
        }
        if (_dialedNumber.value.length < 40) {
            _dialedNumber.value += digit
        }
    }

    fun deleteLastDigit() {
        val current = _dialedNumber.value
        if (current.isNotEmpty()) {
            _dialedNumber.value = current.dropLast(1)
        }
    }

    fun clearDialedNumber() {
        _dialedNumber.value = ""
    }

    fun setDialedNumber(number: String) {
        _dialedNumber.value = number
    }

    fun selectActiveAccount(accountId: Long) {
        viewModelScope.launch {
            repository.setDefaultAccount(accountId)
            sipManager.setActiveOutgoingAccount(accountId)
        }
    }

    fun saveSipAccount(
        existingId: Long = 0L,
        label: String,
        domain: String,
        port: Int,
        username: String,
        password: String,
        transport: SipTransport
    ) {
        val trimmedLabel = label.trim().ifBlank { "$username@$domain" }
        val trimmedDomain = domain.trim()
        val trimmedUsername = username.trim()
        if (trimmedDomain.isBlank() || trimmedUsername.isBlank()) {
            _snackbarEvents.tryEmit("SIP Domain and Username are required")
            return
        }

        viewModelScope.launch {
            val isFirstAccount = uiState.value.accounts.isEmpty()
            val existingDefault = uiState.value.accounts
                .firstOrNull { it.entity.id == existingId }?.entity?.isDefault ?: false

            val entity = SipAccountEntity(
                id = existingId,
                label = trimmedLabel,
                domain = trimmedDomain,
                port = port.coerceIn(1, 65535),
                username = trimmedUsername,
                password = password,
                transport = transport.name,
                isDefault = isFirstAccount || existingDefault
            )
            repository.saveAccount(entity, setAsDefaultIfFirst = isFirstAccount)
            _snackbarEvents.tryEmit("Saved \"$trimmedLabel\" — registering…")
        }
    }

    fun deleteSipAccount(accountId: Long) {
        viewModelScope.launch {
            val remaining = uiState.value.accounts.filter { it.entity.id != accountId }
            repository.deleteAccount(accountId)
            if (remaining.isNotEmpty() && remaining.none { it.entity.isDefault }) {
                repository.setDefaultAccount(remaining.first().entity.id)
            }
            _snackbarEvents.tryEmit("SIP account deleted")
        }
    }

    fun reregisterAccount(accountId: Long) {
        sipManager.reregisterAccount(accountId)
        _snackbarEvents.tryEmit("Re-registering SIP account…")
    }

    fun placeCall(destinationOverride: String? = null, accountIdOverride: Long? = null) {
        val target = (destinationOverride ?: _dialedNumber.value).trim()
        if (target.isEmpty()) {
            _snackbarEvents.tryEmit("Enter a number or SIP address to call")
            return
        }

        val state = uiState.value
        val account = if (accountIdOverride != null) {
            state.accounts.firstOrNull { it.entity.id == accountIdOverride }?.entity
                ?: state.activeAccount?.entity
        } else {
            state.activeAccount?.entity
        }

        if (account == null) {
            _snackbarEvents.tryEmit("Add and select a SIP account first")
            return
        }

        val result = sipManager.makeCall(target, account)
        result.onFailure { error ->
            _snackbarEvents.tryEmit(error.localizedMessage ?: "Unable to place SIP call")
        }
    }

    fun answerCall() = sipManager.answerCall()
    fun declineCall() = sipManager.declineCall()
    fun endCall() = sipManager.endCall()
    fun toggleMute() = sipManager.toggleMute()
    fun toggleSpeaker() = sipManager.toggleSpeaker()
    fun toggleHold() = sipManager.toggleHold()

    fun sendInCallDtmf(digit: Char) {
        dtmfPlayer.playDigit(digit)
        sipManager.sendDtmf(digit)
    }

    fun clearCallHistory() {
        viewModelScope.launch {
            repository.clearCallHistory()
            _snackbarEvents.tryEmit("Call history cleared")
        }
    }

    override fun onCleared() {
        super.onCleared()
        dtmfPlayer.release()
    }
}
