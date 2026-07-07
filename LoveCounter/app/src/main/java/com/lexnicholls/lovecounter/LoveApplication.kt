package com.lexnicholls.lovecounter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LoveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val sharedPrefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (sharedPrefs.getBoolean("persistence_service_enabled", false)) {
            com.lexnicholls.lovecounter.services.PersistenceService.startService(this)
        }
    }
}
