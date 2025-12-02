package com.example.homewidgettocall.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.homewidgettocall.MainActivity
import com.example.homewidgettocall.R
import java.io.File
import java.io.FileOutputStream

/**
 * Service to download and auto-play audio messages
 * Triggered by FCM notification
 */
class AudioPlaybackService : Service() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var audioClient: AudioMessageClient? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AudioPlaybackService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            ACTION_DOWNLOAD_AND_PLAY -> {
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
                val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)
                val senderId = intent.getStringExtra(EXTRA_SENDER_ID)
                
                if (messageId == null || serverUrl == null) {
                    Log.e(TAG, "Missing required parameters!")
                    stopSelf()
                    return START_NOT_STICKY
                }
                
                Log.d(TAG, "🎙️ Download and play audio message: $messageId from $senderId")
                downloadAndPlayAudio(messageId, serverUrl, senderId ?: "unknown")
            }
            
            else -> {
                Log.w(TAG, "Unknown action: $action")
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun downloadAndPlayAudio(messageId: String, serverUrl: String, senderId: String) {
        Log.d(TAG, "Connecting to server: $serverUrl")
        startForeground(1, createNotification("Connecting to server...", "Please wait..."))
        
        audioClient = AudioMessageClient(this, serverUrl, object : AudioMessageClient.AudioMessageListener {
            override fun onConnected() {
                Log.d(TAG, "✅ Connected to server, downloading audio...")
                audioClient?.downloadAudio(messageId)
            }
            
            override fun onDisconnected() {
                Log.d(TAG, "Disconnected from server")
            }
            
            override fun onUploadSuccess(messageId: String) {
                // Not used in playback service
            }
            
            override fun onUploadFailed(error: String) {
                // Not used in playback service
            }
            
            override fun onDownloadSuccess(messageId: String, audioData: String, duration: Int) {
                Log.d(TAG, "✅ Download successful, playing audio... (${duration}s)")
                playAudioFromBase64(audioData, senderId)
            }
            
            override fun onDownloadFailed(error: String) {
                Log.e(TAG, "❌ Download failed: $error")
                stopSelf()
            }
            
            override fun onError(error: String) {
                Log.e(TAG, "❌ Error: $error")
                stopSelf()
            }
        })
        
        audioClient?.connect()
    }
    
    private fun playAudioFromBase64(base64Data: String, senderId: String) {
        try {
            // Remove data URL prefix if present
            val base64Audio = base64Data.replace(Regex("^data:audio/\\w+;base64,"), "")
            
            // Decode base64 to bytes
            val audioBytes = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT)
            
            Log.d(TAG, "Decoded audio: ${audioBytes.size} bytes")
            
            // Save to temp file
            val tempFile = File(cacheDir, "temp_audio_${System.currentTimeMillis()}.webm")
            FileOutputStream(tempFile).use { it.write(audioBytes) }
            
            Log.d(TAG, "🎵 Playing audio file: ${tempFile.absolutePath}")
            
            // Play audio
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                
                setOnPreparedListener {
                    Log.d(TAG, "▶️ Audio prepared, starting playback")
                    start()
                }
                
                setOnCompletionListener {
                    Log.d(TAG, "✅ Playback completed")
                    tempFile.delete()
                    cleanup()
                }
                
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "❌ MediaPlayer error: what=$what, extra=$extra")
                    tempFile.delete()
                    cleanup()
                    true
                }
                
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing audio", e)
            cleanup()
        }
    }

    // Notification management

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "CHANNEL_ID",
            "Audio Calls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing audio calls"
            setSound(null, null)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(title: String, text: String = ""): Notification {
        // Intent to open MainActivity
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )


        return NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, text: String = "") {
        val notification = createNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, notification)
    }
    
    private fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        audioClient?.disconnect()
        audioClient = null
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        audioClient?.disconnect()
        audioClient = null
        Log.d(TAG, "Service destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val TAG = "AudioPlaybackService"
        
        const val ACTION_DOWNLOAD_AND_PLAY = "com.example.homewidgettocall.DOWNLOAD_AND_PLAY"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_SENDER_ID = "sender_id"
        
        /**
         * Start service to download and play audio message
         */
        fun downloadAndPlay(
            context: Context, 
            messageId: String, 
            serverUrl: String,
            senderId: String = "unknown"
        ) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_DOWNLOAD_AND_PLAY
                putExtra(EXTRA_MESSAGE_ID, messageId)
                putExtra(EXTRA_SERVER_URL, serverUrl)
                putExtra(EXTRA_SENDER_ID, senderId)
            }
            context.startForegroundService(intent)
        }
    }
}
