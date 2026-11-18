package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SipAccountDao {
    @Query("SELECT * FROM sip_accounts ORDER BY isDefault DESC, createdAt ASC")
    fun getAllAccounts(): Flow<List<SipAccountEntity>>

    @Query("SELECT * FROM sip_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): SipAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: SipAccountEntity): Long

    @Update
    suspend fun updateAccount(account: SipAccountEntity)

    @Query("DELETE FROM sip_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("UPDATE sip_accounts SET isDefault = 0")
    suspend fun clearDefaultAccounts()

    @Query("UPDATE sip_accounts SET isDefault = 1 WHERE id = :id")
    suspend fun markAsDefault(id: Long)

    @Transaction
    suspend fun setDefaultAccount(id: Long) {
        clearDefaultAccounts()
        markAsDefault(id)
    }
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentCalls(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogEntity): Long

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()
}
