package com.example.homewidgettocall.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.homewidgettocall.MainActivity
import com.example.homewidgettocall.R
import com.example.homewidgettocall.webrtc.AudioOnlyWebRTCClient

/**
 * Foreground service for audio-only WebRTC calls
 * Works with home widgets - runs in background
 */
class AudioCallService : Service(), AudioOnlyWebRTCClient.AudioCallListener {
    
    private var audioClient: AudioOnlyWebRTCClient? = null
    private val binder = AudioCallBinder()
    
    private var isConnected = false
    private var isInCall = false
    private var currentRoomId: String? = null
    
    inner class AudioCallBinder : Binder() {
        fun getService(): AudioCallService = this@AudioCallService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AudioCallService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                val roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: return START_NOT_STICKY
                val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL) ?: return START_NOT_STICKY
                startCall(serverUrl, roomId)
            }
            ACTION_END_CALL -> {
                endCall()
            }
            ACTION_TOGGLE_MUTE -> {
                toggleMute()
            }
        }
        
        return START_STICKY
    }

    private fun startCall(serverUrl: String, roomId: String) {
        Log.d(TAG, "Starting call to room: $roomId")
        
        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("Connecting...", ""),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Connecting...", ""))
        }
        
        // Initialize WebRTC client
        audioClient = AudioOnlyWebRTCClient(this, serverUrl, this)
        audioClient?.connectToServer()
        
        currentRoomId = roomId
    }

    private fun actuallyJoinRoom() {
        currentRoomId?.let { roomId ->
            audioClient?.joinRoom(roomId)
            audioClient?.startAudioCall()
        }
    }

    private fun endCall() {
        Log.d(TAG, "Ending call")
        audioClient?.disconnect()
        audioClient = null
        isInCall = false
        isConnected = false
        currentRoomId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        // Broadcast to widget
        sendBroadcast(Intent(ACTION_CALL_STATE_CHANGED).apply {
            putExtra(EXTRA_CALL_STATE, STATE_DISCONNECTED)
        })
    }

    private fun toggleMute() {
        audioClient?.let { client ->
            val isMuted = client.toggleMute()
            updateNotification(
                if (isInCall) "In call" else "Connected",
                if (isMuted) "🔇 Muted" else "🔊 Unmuted"
            )
        }
    }

    // AudioCallListener callbacks
    
    override fun onConnectedToServer() {
        Log.d(TAG, "Connected to server")
        isConnected = true
        updateNotification("Connected", "Joining room...")
        actuallyJoinRoom()
    }

    override fun onDisconnectedFromServer() {
        Log.d(TAG, "Disconnected from server")
        isConnected = false
        updateNotification("Disconnected", "Reconnecting...")
    }

    override fun onJoinedRoom(roomId: String) {
        Log.d(TAG, "Joined room: $roomId")
        updateNotification("In room: $roomId", "Waiting for caller...")
    }

    override fun onUserJoined(userId: String) {
        Log.d(TAG, "User joined")
        updateNotification("User joined", "Connecting call...")
    }

    override fun onUserLeft(userId: String) {
        Log.d(TAG, "User left")
        isInCall = false
        updateNotification("User left", "Waiting for caller...")
    }

    override fun onCallConnected() {
        Log.d(TAG, "Call connected!")
        isInCall = true
        updateNotification("📞 In call", "Tap to open")
        
        // Broadcast call state change
        sendBroadcast(Intent(ACTION_CALL_STATE_CHANGED).apply {
            putExtra(EXTRA_CALL_STATE, STATE_IN_CALL)
        })
    }

    override fun onCallDisconnected() {
        Log.d(TAG, "Call disconnected")
        isInCall = false
        updateNotification("Call ended", "")
        
        sendBroadcast(Intent(ACTION_CALL_STATE_CHANGED).apply {
            putExtra(EXTRA_CALL_STATE, STATE_DISCONNECTED)
        })
    }

    override fun onError(error: String) {
        Log.e(TAG, "Error: $error")
        updateNotification("Error", error)
    }

    // Notification management
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing audio calls"
                setSound(null, null)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String = ""): Notification {
        // Intent to open MainActivity
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to end call
        val endIntent = Intent(this, AudioCallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endPendingIntent = PendingIntent.getService(
            this, 1, endIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent to toggle mute
        val muteIntent = Intent(this, AudioCallService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this, 2, muteIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_launcher_foreground,
                if (audioClient?.isMuted() == true) "Unmute" else "Mute",
                mutePendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "End Call",
                endPendingIntent
            )
            .build()
    }

    private fun updateNotification(title: String, text: String = "") {
        val notification = createNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    // Public API
    
    fun getCallState(): CallState {
        return CallState(
            isConnected = isConnected,
            isInCall = isInCall,
            isMuted = audioClient?.isMuted() ?: false,
            roomId = currentRoomId
        )
    }

    data class CallState(
        val isConnected: Boolean,
        val isInCall: Boolean,
        val isMuted: Boolean,
        val roomId: String?
    )

    override fun onDestroy() {
        super.onDestroy()
        audioClient?.disconnect()
        Log.d(TAG, "Service destroyed")
    }

    companion object {
        private const val TAG = "AudioCallService"
        private const val CHANNEL_ID = "audio_call_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_CALL = "com.example.homewidgettocall.START_CALL"
        const val ACTION_END_CALL = "com.example.homewidgettocall.END_CALL"
        const val ACTION_TOGGLE_MUTE = "com.example.homewidgettocall.TOGGLE_MUTE"
        const val ACTION_CALL_STATE_CHANGED = "com.example.homewidgettocall.CALL_STATE_CHANGED"
        
        const val EXTRA_ROOM_ID = "room_id"
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_CALL_STATE = "call_state"
        
        const val STATE_CONNECTING = 0
        const val STATE_IN_CALL = 1
        const val STATE_DISCONNECTED = 2
        
        /**
         * Start a call from anywhere (activity, widget, etc.)
         */
        fun startCall(context: Context, serverUrl: String, roomId: String) {
            val intent = Intent(context, AudioCallService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_SERVER_URL, serverUrl)
                putExtra(EXTRA_ROOM_ID, roomId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * End call from anywhere
         */
        fun endCall(context: Context) {
            val intent = Intent(context, AudioCallService::class.java).apply {
                action = ACTION_END_CALL
            }
            context.startService(intent)
        }
        
        /**
         * Toggle mute from anywhere
         */
        fun toggleMute(context: Context) {
            val intent = Intent(context, AudioCallService::class.java).apply {
                action = ACTION_TOGGLE_MUTE
            }
            context.startService(intent)
        }
    }
}
