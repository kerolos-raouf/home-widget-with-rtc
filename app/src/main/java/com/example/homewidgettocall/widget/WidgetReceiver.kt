package com.example.homewidgettocall.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

const val SWITCH_TO_NEXT_SUBSCRIPTION_LINE_ACTION = "com.example.homewidgettocall.action.SWITCH_TO_NEXT_SUBSCRIPTION_LINE"
const val ACTION_START_WEBRTC_CALL = "com.example.homewidgettocall.action.ACTION_START_WEBRTC_CALL"
const val ACTION_END_WEBRTC_CALL = "com.example.homewidgettocall.action.ACTION_END_WEBRTC_CALL"
const val ACTION_TOGGLE_MUTE = "com.example.homewidgettocall.action.ACTION_TOGGLE_MUTE"
const val SERVER_URL_EXTRA = "server_url"
const val ROOM_ID_EXTRA = "room_id"

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

            ACTION_START_WEBRTC_CALL -> {
                val serverUrl = intent.getStringExtra(SERVER_URL_EXTRA) ?: return
                val roomId = intent.getStringExtra(ROOM_ID_EXTRA) ?: return
                Log.d("WidgetReceiver", "Starting WebRTC call to room: $roomId")
                AudioCallService.startCall(context, serverUrl, roomId)
            }

            ACTION_END_WEBRTC_CALL -> {
                Log.d("WidgetReceiver", "Ending WebRTC call")
                AudioCallService.endCall(context)
            }

            ACTION_TOGGLE_MUTE -> {
                Log.d("WidgetReceiver", "Toggling mute")
                AudioCallService.toggleMute(context)
            }
        }
    }
}