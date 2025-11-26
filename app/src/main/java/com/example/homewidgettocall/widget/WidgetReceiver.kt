package com.example.homewidgettocall.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

const val SWITCH_TO_NEXT_SUBSCRIPTION_LINE_ACTION = "com.example.homewidgettocall.action.SWITCH_TO_NEXT_SUBSCRIPTION_LINE"

class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (SWITCH_TO_NEXT_SUBSCRIPTION_LINE_ACTION == intent?.action) {
            Log.d("UsageWidgetReceiver", "onReceive: ${intent.extras?.getInt(WIDGET_ID)}")

            val serviceIntent = Intent(context, VoiceRecorderService::class.java)
            serviceIntent.action = ACTION_TOGGLE_RECORDING

            context?.startForegroundService(serviceIntent)
        }
    }
}