package com.example.homewidgettocall.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.homewidgettocall.audio.AudioMessageClient
import com.example.homewidgettocall.data.PreferencesHelper
import java.io.File

const val RECORD_A_VOICE = "com.example.homewidgettocall.action.SWITCH_TO_NEXT_SUBSCRIPTION_LINE"
const val ACTION_SEND_AUDIO_MESSAGE = "com.example.homewidgettocall.action.SEND_AUDIO_MESSAGE"
const val SERVER_URL_EXTRA = "server_url"
const val AUDIO_FILE_PATH_EXTRA = "audio_file_path"

class WidgetReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WidgetReceiver"
        
        // TODO: Replace with your actual server URL
        const val DEFAULT_SERVER_URL = "https://synostotic-maverick-infinitesimally.ngrok-free.dev"  // Android emulator localhost
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
        
        // Get first friend's FCM token from SharedPreferences
        val recipientFcmToken = PreferencesHelper.getFirstFriendToken(context)
        
        if (recipientFcmToken == null) {
            Log.e(TAG, "❌ No friend FCM token found in SharedPreferences")
            return
        }
        
        Log.d(TAG, "📁 Audio file found: ${audioFile.length()} bytes")
        Log.d(TAG, "📮 Recipient FCM token: ${recipientFcmToken.take(20)}...")
        
        // Calculate duration (estimate: 1 byte = 0.001 seconds for compressed audio)
        val durationSeconds = (audioFile.length() / 1000).toInt()
        
        // Create audio client - declare as var to capture in callbacks
        var audioClient: AudioMessageClient? = null
        
        audioClient = AudioMessageClient(
            serverUrl,
            object : AudioMessageClient.AudioMessageListener {
                override fun onConnected() {
                    Log.d(TAG, "✅ Connected to server, uploading...")
                    
                    // Upload audio with FCM token from SharedPreferences
                    // Note: recipientId is the Socket.IO ID, which we don't have yet
                    // For now, use FCM token as a temporary ID
                    audioClient?.uploadAudio(
                        audioFile = audioFile,
                        recipientId = recipientFcmToken,  // Temporary - should be Socket.IO ID
                        recipientFCMToken = recipientFcmToken,  // Using token from SharedPreferences
                        duration = durationSeconds
                    )
                }
                
                override fun onDisconnected() {
                    Log.d(TAG, "Disconnected from server")
                }
                
                override fun onUploadSuccess(messageId: String) {
                    Log.d(TAG, "✅ Upload successful! Message ID: $messageId")

                    // Send FCM notification manually (if needed)
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
}
