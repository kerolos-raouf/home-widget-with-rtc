package com.example.homewidgettocall.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.homewidgettocall.audio.AudioMessageClient
import com.example.homewidgettocall.fcm.FCMNotificationSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

const val RECORD_A_VOICE = "com.example.homewidgettocall.action.SWITCH_TO_NEXT_SUBSCRIPTION_LINE"
const val ACTION_SEND_AUDIO_MESSAGE = "com.example.homewidgettocall.action.SEND_AUDIO_MESSAGE"
const val SERVER_URL_EXTRA = "server_url"
const val RECIPIENT_FCM_TOKEN_EXTRA = "recipient_fcm_token"
const val AUDIO_FILE_PATH_EXTRA = "audio_file_path"

class WidgetReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WidgetReceiver"
        
        // TODO: Replace with your actual server URL
        const val DEFAULT_SERVER_URL = "https://synostotic-maverick-infinitesimally.ngrok-free.dev"  // Android emulator localhost
        
        // TODO: Replace with the actual FCM token of the device you want to send to
        private const val TARGET_DEVICE_FCM_TOKEN = "c7NCE0UPT2-WEgg5gwh2G-:APA91bENGuEPLaCfHfGphdP-TS_v1ad27rkMwinIHsGMoNHeh7JMQX5eW-r_DH9ebtgYGdXixhqyfIHVqqk01n6tPbXmlGmQiVAF8ZdddomEJMcXCwFuhPA"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        
        when (intent?.action) {
            RECORD_A_VOICE -> {
                Log.d(TAG, "📹 Record voice button pressed")
                
                // Start voice recorder service
                val serviceIntent = Intent(context, VoiceRecorderService::class.java)
                serviceIntent.action = ACTION_TOGGLE_RECORDING
                context.startForegroundService(serviceIntent)
            }

            ACTION_SEND_AUDIO_MESSAGE -> {
                val audioFilePath = intent.getStringExtra(AUDIO_FILE_PATH_EXTRA) ?: return
                val serverUrl = intent.getStringExtra(SERVER_URL_EXTRA) ?: DEFAULT_SERVER_URL

                Log.d(TAG, "📤 Sending audio message from: $audioFilePath")
                sendAudioMessage(context, audioFilePath, serverUrl)
            }
        }
    }
    
    /**
     * Upload audio file to server and send FCM notification
     */
    private fun sendAudioMessage(
        context: Context,
        audioFilePath: String,
        serverUrl: String,
    ) {
        val audioFile = File(audioFilePath)
        
        if (!audioFile.exists()) {
            Log.e(TAG, "❌ Audio file not found: $audioFilePath")
            return
        }
        
        Log.d(TAG, "📁 Audio file found: ${audioFile.length()} bytes")
        
        // Calculate duration (estimate: 1 byte = 0.001 seconds for compressed audio)
        val durationSeconds = (audioFile.length() / 1000).toInt()
        
        // Create audio client - declare as var to capture in callbacks
        var audioClient: AudioMessageClient? = null
        
        audioClient = AudioMessageClient(
            serverUrl,
            object : AudioMessageClient.AudioMessageListener {
                override fun onConnected() {
                    Log.d(TAG, "✅ Connected to server, uploading...")
                    
                    // Upload audio
                    // Note: recipientId is the Socket.IO ID, which we don't have yet
                    // For now, use FCM token as a temporary ID
                    audioClient?.uploadAudio(
                        audioFile = audioFile,
                        recipientId = TARGET_DEVICE_FCM_TOKEN,  // Temporary - should be Socket.IO ID
                        recipientFCMToken = TARGET_DEVICE_FCM_TOKEN,
                        duration = durationSeconds
                    )
                }
                
                override fun onDisconnected() {
                    Log.d(TAG, "Disconnected from server")
                }
                
                override fun onUploadSuccess(messageId: String) {
                    Log.d(TAG, "✅ Upload successful! Message ID: $messageId")

                    // Send FCM notification manually
                    //sendFCMNotification(context, messageId, audioFile.length(), durationSeconds, serverUrl)
                    
                    // Disconnect after upload
                    audioClient?.disconnect()
                }
                
                override fun onUploadFailed(error: String) {
                    Log.e(TAG, "❌ Upload failed: $error")
                    audioClient?.disconnect()
                }
                
                override fun onDownloadSuccess(messageId: String, audioData: String, duration: Int) {
                    // Not used for sending
                }
                
                override fun onDownloadFailed(error: String) {
                    // Not used for sending
                }
                
                override fun onError(error: String) {
                    Log.e(TAG, "❌ Error: $error")
                    audioClient?.disconnect()
                }
            }
        )
        
        // Connect to server
        audioClient.connect()
    }
    
    /**
     * Send FCM notification to recipient after successful upload
     * This is a workaround since the server sends Socket.IO notifications
     * instead of FCM notifications
     */
    private fun sendFCMNotification(
        context: Context,
        messageId: String,
        size: Long,
        duration: Int,
        serverUrl: String
    ) {
        Log.d(TAG, "📤 Sending FCM notification for message: $messageId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = FCMNotificationSender.sendAudioMessageNotification(
                    context = context,
                    targetToken = TARGET_DEVICE_FCM_TOKEN,
                    messageId = messageId,
                    senderId = "you",  // Could be device name
                    size = size.toString(),
                    duration = duration.toString(),
                    serverUrl = serverUrl
                )
                
                if (success) {
                    Log.d(TAG, "✅ FCM notification sent successfully!")
                } else {
                    Log.e(TAG, "❌ Failed to send FCM notification")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending FCM: ${e.message}", e)
            }
        }
    }
}
