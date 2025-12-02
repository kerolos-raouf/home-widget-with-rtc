package com.example.homewidgettocall.audio

import android.content.Context
import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.io.File

/**
 * Client for uploading and downloading audio messages via Socket.IO
 */
class AudioMessageClient(
    private val serverUrl: String,
    private val listener: AudioMessageListener
) {
    private var socket: Socket? = null
    
    interface AudioMessageListener {
        fun onConnected()
        fun onDisconnected()
        fun onUploadSuccess(messageId: String)
        fun onUploadFailed(error: String)
        fun onDownloadSuccess(messageId: String, audioData: String, duration: Int)
        fun onDownloadFailed(error: String)
        fun onError(error: String)
    }
    
    fun connect() {
        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionDelay = 1000
                reconnectionAttempts = 10
                timeout = 100000
                transports = arrayOf("websocket")
                forceNew = true
            }
            
            socket = IO.socket(serverUrl, options)
            
            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "✅ Connected to audio server")
                    listener.onConnected()
                }
                
                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "❌ Disconnected from audio server")
                    listener.onDisconnected()
                }
                
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "Connection error: ${args.firstOrNull()}")
                    listener.onError("Connection failed")
                }
                
                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to server", e)
            listener.onError(e.message ?: "Connection error")
        }
    }
    
    /**
     * Upload audio file to server
     * Server will send FCM notification to recipient
     */
    fun uploadAudio(
        audioFile: File,
        recipientId: String,
        recipientFCMToken: String,
        duration: Int
    ) {
        if (socket?.connected() != true) {
            Log.e(TAG, "Not connected to server!")
            listener.onUploadFailed("Not connected to server")
            return
        }
        
        try {
            // Read file as base64
            val audioBytes = audioFile.readBytes()
            val base64Audio = android.util.Base64.encodeToString(
                audioBytes,
                android.util.Base64.NO_WRAP
            )
            
            val data = JSONObject().apply {
                put("recipientId", recipientId)
                put("recipientFCMToken", recipientFCMToken)
                put("audioBlob", base64Audio)
                put("mimeType", "audio/webm")
                put("duration", duration)
            }
            
            Log.d(TAG, "📤 Uploading audio: ${audioBytes.size} bytes, duration: ${duration}s")
            
            socket?.emit("upload-audio", data, Ack { args ->
                try {
                    val response = args[0] as JSONObject
                    val success = response.getBoolean("success")

                    if (success) {
                        val messageId = response.getString("messageId")
                        val size = response.optInt("size", 0)
                        Log.d(TAG, "✅ Upload successful: $messageId ($size bytes)")
                        listener.onUploadSuccess(messageId)
                    } else {
                        val error = response.optString("error", "Upload failed")
                        Log.e(TAG, "❌ Upload failed: $error")
                        listener.onUploadFailed(error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in upload callback", e)
                    listener.onUploadFailed(e.message ?: "Unknown error")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            listener.onUploadFailed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Download audio from server by messageId
     */
    fun downloadAudio(messageId: String) {
        if (socket?.connected() != true) {
            Log.e(TAG, "Not connected to server!")
            listener.onDownloadFailed("Not connected to server")
            return
        }

        val data = JSONObject().apply {
            put("messageId", messageId)
        }

        Log.d(TAG, "📥 Downloading audio: $messageId")

        socket?.emit("request-audio-download", data, Ack { args ->
            try {
                val response = args[0] as JSONObject
                val success = response.getBoolean("success")

                if (success) {
                    val audioData = response.getString("audioData")
                    val duration = response.optInt("duration", 0)
                    val senderId = response.optString("senderId", "unknown")

                    Log.d(TAG, "✅ Download successful: $messageId (${audioData.length} chars, ${duration}s)")
                    listener.onDownloadSuccess(messageId, audioData, duration)
                } else {
                    val error = response.optString("error", "Download failed")
                    Log.e(TAG, "❌ Download failed: $error")
                    listener.onDownloadFailed(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in download callback", e)
                listener.onDownloadFailed(e.message ?: "Unknown error")
            }
        })
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket = null
        Log.d(TAG, "Disconnected from server")
    }
    
    fun isConnected(): Boolean = socket?.connected() == true
    
    companion object {
        private const val TAG = "AudioMessageClient"
    }
}
