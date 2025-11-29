package com.example.homewidgettocall.fcm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.google.auth.oauth2.GoogleCredentials
import java.io.InputStream

/**
 * Service to send FCM notifications to other devices
 * Uses Firebase Admin SDK with Service Account JSON
 */
object FCMNotificationSender {
    private const val TAG = "FCMNotificationSender"

    // Firebase project ID - Get from Firebase Console -> Project Settings
    private const val PROJECT_ID = "homewidget-3fa21" // e.g., "my-app-12345"

    // FCM v1 API URL
    private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    // Scopes for Firebase Cloud Messaging
    private val SCOPES = listOf("https://www.googleapis.com/auth/firebase.messaging")

    /**
     * Get OAuth2 access token from service account JSON
     */
    private fun getAccessToken(context: Context): String? {
        return try {
            // Read service account JSON from assets or raw folder
            val serviceAccountStream: InputStream = context.assets.open("service_account.json")

            val googleCredentials = GoogleCredentials
                .fromStream(serviceAccountStream)
                .createScoped(SCOPES)

            googleCredentials.refreshIfExpired()
            val accessToken = googleCredentials.accessToken.tokenValue

            Log.d(TAG, "✅ Access token obtained")
            accessToken
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting access token", e)
            null
        }
    }

    /**
     * Send incoming call notification to a specific device using FCM v1 API
     */
    suspend fun sendIncomingCallNotification(
        context: Context,
        targetToken: String,
        callerName: String,
        callerId: String,
        roomId: String,
        serverUrl: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📤 Sending call notification to: ${targetToken.take(20)}...")

                // Get OAuth2 access token
                val accessToken = getAccessToken(context)
                if (accessToken == null) {
                    Log.e(TAG, "❌ Failed to get access token")
                    return@withContext false
                }

                // Build FCM v1 message payload
                val message = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", targetToken)

                        // Data payload
                        put("data", JSONObject().apply {
                            put("type", "incoming_call")
                            put("caller_name", callerName)
                            put("caller_id", callerId)
                            put("room_id", roomId)
                            put("server_url", serverUrl)
                            put("auto_answer", "true")
                        })

                        // Android specific config
                        put("android", JSONObject().apply {
                            put("priority", "HIGH")
                        })
                    })
                }

                Log.d(TAG, "Payload: $message")

                // Send HTTP request
                val connection = URL(FCM_URL).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json; UTF-8")
                    doOutput = true
                }

                // Write payload
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(message.toString())
                    writer.flush()
                }

                // Get response
                val responseCode = connection.responseCode
                val responseMessage = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                }

                Log.d(TAG, "FCM Response Code: $responseCode")
                Log.d(TAG, "FCM Response: $responseMessage")

                if (responseCode == 200) {
                    Log.d(TAG, "✅ Notification sent successfully!")
                    true
                } else {
                    Log.e(TAG, "❌ Failed to send notification: $responseCode - $responseMessage")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending notification", e)
                false
            }
        }
    }

    /**
     * Send call ended notification
     */
    suspend fun sendCallEndedNotification(
        context: Context,
        targetToken: String,
        callerId: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📤 Sending call ended notification")

                val accessToken = getAccessToken(context)
                if (accessToken == null) {
                    Log.e(TAG, "❌ Failed to get access token")
                    return@withContext false
                }

                val message = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", targetToken)
                        put("data", JSONObject().apply {
                            put("type", "call_ended")
                            put("caller_id", callerId)
                        })
                    })
                }

                val connection = URL(FCM_URL).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json; UTF-8")
                    doOutput = true
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(message.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                responseCode == 200
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending call ended notification", e)
                false
            }
        }
    }
}