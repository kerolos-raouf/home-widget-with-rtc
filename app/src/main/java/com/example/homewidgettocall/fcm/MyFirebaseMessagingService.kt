package com.example.homewidgettocall.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.homewidgettocall.MainActivity
import com.example.homewidgettocall.R
import com.example.homewidgettocall.widget.AudioCallService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.content.edit
import com.example.homewidgettocall.widget.AudioCallService.Companion.CALLER_NAME
import com.example.homewidgettocall.widget.AudioCallService.Companion.EXTRA_AUTO_JOIN_MUTED

/**
 * Firebase Messaging Service for handling push notifications
 * Handles incoming call notifications and FCM token updates
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FirebaseMessagingService created")
        createNotificationChannel()
    }

    /**
     * Called when a new FCM token is generated
     * Send this token to your server to enable push notifications
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "📱 New FCM Token: $token")
        
        // TODO: Send token to your server
        // Example: sendTokenToServer(token)
        
        // Save token locally
        saveTokenLocally(token)
    }

    /**
     * Called when a message is received from Firebase
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "📨 Message received from: ${message.from}")
        
        // Check if message contains data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            handleDataMessage(message.data)
        }

        // Check if message contains notification payload
        message.notification?.let { notification ->
            Log.d(TAG, "Message notification: ${notification.title}")
            showNotification(
                notification.title ?: "New Message",
                notification.body ?: ""
            )
        }
    }

    /**
     * Handle data messages (custom payload)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        
        when (type) {
            "incoming_call" -> {
                val callerId = data["caller_id"] ?: "Unknown"
                val callerName = data["caller_name"] ?: "Someone"
                val roomId = data["room_id"] ?: "default-room"
                val serverUrl = data["server_url"] ?: ""
                
                Log.d(TAG, "📞 Incoming call from: $callerName (ID: $callerId)")
                
                // Automatically answer the call (no notification)
                Log.d(TAG, "🤖 Auto-answering call...")
                autoJoinCall(serverUrl, roomId, callerName)
                //showIncomingCallNotification(callerName, callerId, roomId, serverUrl)
            }
            
            "call_ended" -> {
                val callerId = data["caller_id"] ?: "Unknown"
                Log.d(TAG, "📴 Call ended by: $callerId")
                // You could dismiss notifications or end active call here
                dismissNotification(INCOMING_CALL_NOTIFICATION_ID)
            }
            
            "message" -> {
                val title = data["title"] ?: "New Message"
                val body = data["body"] ?: ""
                showNotification(title, body)
            }
            
            else -> {
                Log.w(TAG, "Unknown message type: $type")
                // Show generic notification
                val title = data["title"] ?: "Notification"
                val body = data["body"] ?: ""
                showNotification(title, body)
            }
        }
    }

    /**
     * Show incoming call notification with answer/decline actions
     */
    private fun showIncomingCallNotification(
        callerName: String,
        callerId: String,
        roomId: String,
        serverUrl: String
    ) {
        // Intent to answer call
        val answerIntent = Intent(this, AudioCallService::class.java).apply {
            action = AudioCallService.ACTION_START_CALL
            putExtra(AudioCallService.EXTRA_ROOM_ID, roomId)
            putExtra(AudioCallService.EXTRA_SERVER_URL, serverUrl)
            putExtra("caller_id", callerId)
            putExtra("caller_name", callerName)
        }
        val answerPendingIntent = PendingIntent.getService(
            this,
            ANSWER_REQUEST_CODE,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to decline call
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra("caller_id", callerId)
            putExtra("notification_id", INCOMING_CALL_NOTIFICATION_ID)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            this,
            DECLINE_REQUEST_CODE,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open app when notification is tapped
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("caller_id", callerId)
            putExtra("caller_name", callerName)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            OPEN_APP_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📞 Incoming Call")
            .setContentText("$callerName is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Answer",
                answerPendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Decline",
                declinePendingIntent
            )
            .setFullScreenIntent(openPendingIntent, true)  // Show as heads-up notification
            .setSound(android.provider.Settings.System.DEFAULT_RINGTONE_URI)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    /**
     * Show generic notification
     */
    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(GENERAL_NOTIFICATION_ID, notification)
    }

    /**
     * Dismiss notification by ID
     */
    private fun dismissNotification(notificationId: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    /**
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Channel for incoming calls (high priority)
        val callChannel = NotificationChannel(
            CALL_CHANNEL_ID,
            "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for incoming calls"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            setSound(
                android.provider.Settings.System.DEFAULT_RINGTONE_URI,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        // Channel for general notifications
        val defaultChannel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            "General Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General app notifications"
        }

        notificationManager.createNotificationChannel(callChannel)
        notificationManager.createNotificationChannel(defaultChannel)

        Log.d(TAG, "Notification channels created")
    }

    /**
     * Save FCM token locally for later use
     */
    private fun saveTokenLocally(token: String) {
        val prefs = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        prefs.edit { putString("fcm_token", token) }
        Log.d(TAG, "Token saved locally")
    }
    
    /**
     * Auto-join call when receiving FCM notification
     * Joins WITHOUT requesting microphone permission (Google Meet style!)
     */
    private fun autoJoinCall(serverUrl: String, roomId: String, callerName: String) {
        Log.d(TAG, "🤫 Auto-joining call SILENTLY (no mic - Google Meet style)")
        
        // Start AudioCallService WITHOUT calling startAudioCall()
        // This means localAudioTrack stays null = no permission needed!
        val intent = Intent(this, AudioCallService::class.java).apply {
            action = AudioCallService.ACTION_START_CALL
            putExtra(AudioCallService.EXTRA_ROOM_ID, roomId)
            putExtra(AudioCallService.EXTRA_SERVER_URL, serverUrl)
            putExtra(CALLER_NAME, callerName)
            putExtra(EXTRA_AUTO_JOIN_MUTED, true)  // Flag to skip audio initialization
        }
        
        startForegroundService(intent)
        
        Log.d(TAG, "✅ Joined WITHOUT mic - will enable when user unmutes")
    }
    
    /**
     * Show notification for user to tap and join call
     * Tapping opens MainActivity which will handle permission and join
     */
    private fun showTapToJoinNotification(
        callerName: String,
        roomId: String,
        serverUrl: String
    ) {
        // Intent to open MainActivity which will join the call
        val joinIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_join_call", true)
            putExtra("caller_name", callerName)
            putExtra("room_id", roomId)
            putExtra("server_url", serverUrl)
        }
        val joinPendingIntent = PendingIntent.getActivity(
            this,
            0,
            joinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📞 Incoming Call")
            .setContentText("$callerName is calling. Tap to join!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(joinPendingIntent)
            .setFullScreenIntent(joinPendingIntent, true)  // Show as heads-up
            .setSound(android.provider.Settings.System.DEFAULT_RINGTONE_URI)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Join Call",
                joinPendingIntent
            )
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }
    
    /**
     * Show notification when auto-joining call
     */
    private fun showAutoJoiningNotification(callerName: String, roomId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📞 Joining Call")
            .setContentText("Connecting to $callerName...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(AUTO_JOIN_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "FCMService"
        
        // Notification channels
        private const val CALL_CHANNEL_ID = "incoming_calls_channel"
        private const val DEFAULT_CHANNEL_ID = "default_channel"
        
        // Notification IDs
        private const val INCOMING_CALL_NOTIFICATION_ID = 2001
        private const val GENERAL_NOTIFICATION_ID = 2002
        private const val AUTO_JOIN_NOTIFICATION_ID = 2003
        
        // Request codes for PendingIntents
        private const val ANSWER_REQUEST_CODE = 100
        private const val DECLINE_REQUEST_CODE = 101
        private const val OPEN_APP_REQUEST_CODE = 102
        
        // Actions
        const val ACTION_DECLINE_CALL = "com.example.homewidgettocall.DECLINE_CALL"
        
        /**
         * Get saved FCM token
         */
        fun getSavedToken(context: Context): String? {
            val prefs = context.getSharedPreferences("fcm_prefs", MODE_PRIVATE)
            return prefs.getString("fcm_token", null)
        }
    }
}
