package com.example.sip

import com.example.data.CallDirection

enum class RegistrationStatus {
    REGISTERED,
    REGISTERING,
    FAILED,
    OFFLINE
}

data class AccountRegistrationInfo(
    val accountId: Long,
    val status: RegistrationStatus,
    val message: String = ""
)

enum class ActiveCallStatus {
    NONE,
    OUTGOING,
    RINGING,
    INCOMING,
    CONNECTED,
    PAUSED,
    ENDED
}

data class ActiveCallUiState(
    val remoteNumber: String = "",
    val remoteDisplayName: String = "",
    val accountId: Long = 0L,
    val accountLabel: String = "",
    val status: ActiveCallStatus = ActiveCallStatus.NONE,
    val direction: CallDirection = CallDirection.OUTGOING,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false,
    val durationSeconds: Long = 0L,
    val statusMessage: String = ""
) {
    val isCallActive: Boolean
        get() = status != ActiveCallStatus.NONE
}

data class CompletedCallRecord(
    val remoteNumber: String,
    val accountId: Long,
    val accountLabel: String,
    val direction: CallDirection,
    val durationSeconds: Long
)
