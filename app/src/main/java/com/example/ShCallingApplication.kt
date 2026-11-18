package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.SipRepository
import com.example.sip.LinphoneSipManager

class ShCallingApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: SipRepository by lazy {
        SipRepository(
            sipAccountDao = database.sipAccountDao(),
            callLogDao = database.callLogDao()
        )
    }
    val sipManager: LinphoneSipManager by lazy { LinphoneSipManager(this) }
}
