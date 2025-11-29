package com.example.homewidgettocall.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.homewidgettocall.fcm.FCMNotificationSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val RECORD_A_VOICE = "com.example.homewidgettocall.action.SWITCH_TO_NEXT_SUBSCRIPTION_LINE"
const val ACTION_START_WEBRTC_CALL = "com.example.homewidgettocall.action.ACTION_START_WEBRTC_CALL"
const val ACTION_END_WEBRTC_CALL = "com.example.homewidgettocall.action.ACTION_END_WEBRTC_CALL"
const val ACTION_TOGGLE_MUTE = "com.example.homewidgettocall.action.ACTION_TOGGLE_MUTE"
const val SERVER_URL_EXTRA = "server_url"
const val ROOM_ID_EXTRA = "room_id"

class WidgetReceiver : BroadcastReceiver() {

    // Target device FCM token (the device you want to call)
    companion object {
        private const val TAG = "WidgetReceiver"
        
        // TODO: Replace with the actual token of the device you want to call
        // Or store this in SharedPreferences and make it configurable
        private const val TARGET_DEVICE_TOKEN = "c7NCE0UPT2-WEgg5gwh2G-:APA91bENGuEPLaCfHfGphdP-TS_v1ad27rkMwinIHsGMoNHeh7JMQX5eW-r_DH9ebtgYGdXixhqyfIHVqqk01n6tPbXmlGmQiVAF8ZdddomEJMcXCwFuhPA"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        
        when (intent?.action) {
            RECORD_A_VOICE -> {
                Log.d("UsageWidgetReceiver", "onReceive: ${intent.extras?.getInt(WIDGET_ID)}")

                val serviceIntent = Intent(context, VoiceRecorderService::class.java)
                serviceIntent.action = ACTION_TOGGLE_RECORDING

                context.startForegroundService(serviceIntent)
            }

            ACTION_START_WEBRTC_CALL -> {
                val serverUrl = intent.getStringExtra(SERVER_URL_EXTRA) ?: return
                val roomId = intent.getStringExtra(ROOM_ID_EXTRA) ?: return
                Log.d(TAG, "📞 Starting WebRTC call to room: $roomId")
                
                // Start the call on THIS device
                AudioCallService.startCall(context, serverUrl, roomId)
                
                // Send FCM notification to TARGET device
                sendCallNotificationToTarget(context, serverUrl, roomId)
            }

            ACTION_END_WEBRTC_CALL -> {
                Log.d(TAG, "📴 Ending WebRTC call")
                AudioCallService.endCall(context)
                
                // Optionally notify the other device that call ended
                sendCallEndedNotification(context)
            }

            ACTION_TOGGLE_MUTE -> {
                Log.d(TAG, "🔇 Toggling mute")
                AudioCallService.toggleMute(context)
            }
        }
    }
    
    /**
     * Send FCM notification to target device to auto-join the call
     */
    private fun sendCallNotificationToTarget(context: Context, serverUrl: String, roomId: String) {
        Log.d(TAG, "📤 Sending FCM notification to target device...")
        
        // Use coroutine to send notification asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val success = FCMNotificationSender.sendIncomingCallNotification(
                context = context,  // Pass context for service account
                targetToken = TARGET_DEVICE_TOKEN,
                callerName = "Your Device", // You can get device name or user name here
                callerId = "caller_${System.currentTimeMillis()}",
                roomId = roomId,
                serverUrl = serverUrl
            )
            
            if (success) {
                Log.d(TAG, "✅ FCM notification sent successfully!")
            } else {
                Log.e(TAG, "❌ Failed to send FCM notification")
            }
        }
    }
    
    /**
     * Send notification that call has ended
     */
    private fun sendCallEndedNotification(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            FCMNotificationSender.sendCallEndedNotification(
                context = context,  // Pass context for service account
                targetToken = TARGET_DEVICE_TOKEN,
                callerId = "caller_${System.currentTimeMillis()}"
            )
        }
    }
}