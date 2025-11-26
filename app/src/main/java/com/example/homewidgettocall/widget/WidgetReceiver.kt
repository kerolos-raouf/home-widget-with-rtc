package com.example.homewidgettocall.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

const val SWITCH_TO_NEXT_SUBSCRIPTION_LINE_ACTION = "com.example.homewidgettocall.action.SWITCH_TO_NEXT_SUBSCRIPTION_LINE"

class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        
        when (intent?.action) {
            SWITCH_TO_NEXT_SUBSCRIPTION_LINE_ACTION -> {
                Log.d("UsageWidgetReceiver", "onReceive: ${intent.extras?.getInt(WIDGET_ID)}")

                val serviceIntent = Intent(context, VoiceRecorderService::class.java)
                serviceIntent.action = ACTION_TOGGLE_RECORDING

                context.startForegroundService(serviceIntent)
            }
            
            "ACTION_START_WEBRTC_CALL" -> {
                val serverUrl = intent.getStringExtra("server_url") ?: return
                val roomId = intent.getStringExtra("room_id") ?: return
                Log.d("WidgetReceiver", "Starting WebRTC call to room: $roomId")
                AudioCallService.startCall(context, serverUrl, roomId)
            }
            
            "ACTION_END_WEBRTC_CALL" -> {
                Log.d("WidgetReceiver", "Ending WebRTC call")
                AudioCallService.endCall(context)
            }
            
            "ACTION_TOGGLE_MUTE" -> {
                Log.d("WidgetReceiver", "Toggling mute")
                AudioCallService.toggleMute(context)
            }
        }
    }
}