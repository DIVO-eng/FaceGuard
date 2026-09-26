package com.faceguard.app

import android.app.Application
import com.faceguard.app.util.RetentionScheduler
import com.faceguard.app.util.SecurePrefs

class FaceGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SecurePrefs.applyDefaults(this)
        RetentionScheduler.scheduleDailyDeletionIfConfigured(this)
    }
}
