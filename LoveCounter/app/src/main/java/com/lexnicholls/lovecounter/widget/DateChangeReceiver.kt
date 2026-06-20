package com.lexnicholls.lovecounter.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action == Intent.ACTION_DATE_CHANGED) {

            CoroutineScope(Dispatchers.IO).launch {

                LoveWidget().updateAll(context)

            }
        }
    }
}