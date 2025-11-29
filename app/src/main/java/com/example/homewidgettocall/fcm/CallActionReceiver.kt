package com.example.homewidgettocall.fcm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles actions from FCM notifications (like declining a call)
 */
class CallActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received action: $action")
        
        when (action) {
            MyFirebaseMessagingService.ACTION_DECLINE_CALL -> {
                val callerId = intent.getStringExtra("caller_id")
                val notificationId = intent.getIntExtra("notification_id", -1)
                
                Log.d(TAG, "📵 Declining call from: $callerId")
                
                // Dismiss the notification
                if (notificationId != -1) {
                    val notificationManager = 
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }
                
                // TODO: Send decline message to server
                // Example: sendDeclineToServer(callerId)
            }
        }
    }
    
    companion object {
        private const val TAG = "CallActionReceiver"
    }
}
