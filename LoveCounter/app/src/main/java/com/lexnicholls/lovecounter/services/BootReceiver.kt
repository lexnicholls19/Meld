package com.lexnicholls.lovecounter.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPrefs.getBoolean("persistence_service_enabled", false)
            
            if (isEnabled) {
                PersistenceService.startService(context)
            }
        }
    }
}
