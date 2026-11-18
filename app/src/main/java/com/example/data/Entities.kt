package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SipTransport(val label: String) {
    UDP("UDP"),
    TCP("TCP");

    companion object {
        fun fromString(value: String): SipTransport =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UDP
    }
}

@Entity(tableName = "sip_accounts")
data class SipAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val domain: String,
    val port: Int = 5060,
    val username: String,
    val password: String,
    val transport: String = SipTransport.UDP.name,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val transportEnum: SipTransport
        get() = SipTransport.fromString(transport)

    val sipUri: String
        get() = "sip:$username@$domain"
}

enum class CallDirection {
    OUTGOING,
    INCOMING,
    MISSED
}

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val remoteNumber: String,
    val accountId: Long,
    val accountLabel: String,
    val direction: String = CallDirection.OUTGOING.name,
    val durationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val directionEnum: CallDirection
        get() = CallDirection.entries.firstOrNull { it.name == direction } ?: CallDirection.OUTGOING
}
