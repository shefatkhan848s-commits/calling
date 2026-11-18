package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [SipAccountEntity::class, CallLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sipAccountDao(): SipAccountDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sh_calling_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SipRepository(
    private val sipAccountDao: SipAccountDao,
    private val callLogDao: CallLogDao
) {
    val allAccounts: Flow<List<SipAccountEntity>> = sipAccountDao.getAllAccounts()
    val recentCalls: Flow<List<CallLogEntity>> = callLogDao.getRecentCalls()

    suspend fun saveAccount(account: SipAccountEntity, setAsDefaultIfFirst: Boolean = false): Long {
        return if (account.id == 0L) {
            val newId = sipAccountDao.insertAccount(account)
            if (setAsDefaultIfFirst || account.isDefault) {
                sipAccountDao.setDefaultAccount(newId)
            }
            newId
        } else {
            sipAccountDao.updateAccount(account)
            if (account.isDefault) {
                sipAccountDao.setDefaultAccount(account.id)
            }
            account.id
        }
    }

    suspend fun deleteAccount(accountId: Long) {
        sipAccountDao.deleteAccountById(accountId)
    }

    suspend fun setDefaultAccount(accountId: Long) {
        sipAccountDao.setDefaultAccount(accountId)
    }

    suspend fun logCall(callLog: CallLogEntity) {
        callLogDao.insertCallLog(callLog)
    }

    suspend fun clearCallHistory() {
        callLogDao.clearAllCallLogs()
    }
}
