package com.example.homewidgettocall.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.homewidgettocall.MainActivity
import com.example.homewidgettocall.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.core.content.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.homewidgettocall.audio.AudioPlaybackService
import com.example.homewidgettocall.audio.AudioPlaybackService.Companion.EXTRA_MESSAGE_ID
import com.example.homewidgettocall.audio.AudioPlaybackService.Companion.EXTRA_SENDER_ID
import com.example.homewidgettocall.widget.WidgetReceiver
import com.example.homewidgettocall.worker.AudioDownloadWorker

/**
 * Firebase Messaging Service for handling push notifications
 * Handles audio message notifications and FCM token updates
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
            "audio_message" -> {
                // Handle incoming audio message
                val messageId = data["message_id"] ?: "Empty"
                val senderId = data["sender_id"] ?: "Unknown"
                val size = data["size"] ?: "0"
                val duration = data["duration"] ?: "0"

                Log.d(TAG, "🎙️ Audio message received: $messageId from $senderId")
                Log.d(TAG, "Size: $size bytes, Duration: ${duration}s")

                // Auto-download and play audio
                val work = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                    .setInputData(
                        workDataOf(
                            EXTRA_MESSAGE_ID to messageId,
                            EXTRA_SENDER_ID to senderId
                        )
                    )
                    .build()

                WorkManager.getInstance(applicationContext).enqueue(work)
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
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Channel for general notifications
        val defaultChannel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            "General Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General app notifications"
        }

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

    companion object {
        private const val TAG = "FCMService"

        // Notification channels
        private const val DEFAULT_CHANNEL_ID = "default_channel"

        // Notification IDs
        private const val GENERAL_NOTIFICATION_ID = 2002

        /**
         * Get saved FCM token
         */
        fun getSavedToken(context: android.content.Context): String? {
            val prefs =
                context.getSharedPreferences("fcm_prefs", android.content.Context.MODE_PRIVATE)
            return prefs.getString("fcm_token", null)
        }
    }
}
