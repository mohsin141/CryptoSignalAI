package com.cryptosignalai

import android.app.Application
import com.cryptosignalai.security.SecureStorage
import com.cryptosignalai.work.SignalScheduler

/** Application entry point. Schedules background checks on first launch. */
class CryptoSignalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val storage = SecureStorage.get(this)
        if (storage.backgroundEnabled) {
            SignalScheduler.schedule(this)
        }
    }
}
