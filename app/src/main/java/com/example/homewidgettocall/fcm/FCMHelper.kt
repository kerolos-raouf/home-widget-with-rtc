package com.example.homewidgettocall.fcm

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.content.edit

/**
 * Helper class for managing FCM tokens and operations
 */
object FCMHelper {
    private const val TAG = "FCMHelper"
    
    /**
     * Get the current FCM token
     */
    fun getCurrentToken(context: Context, onTokenReceived: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                onTokenReceived(null)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "📱 Current FCM token: $token")
            
            // Save token locally
            saveToken(context, token)
            
            onTokenReceived(token)
        })
    }
    
    /**
     * Save token to SharedPreferences
     */
    private fun saveToken(context: Context, token: String?) {
        if (token != null) {
            val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            prefs.edit { putString("fcm_token", token) }
        }
    }
    
    /**
     * Get saved token from SharedPreferences
     */
    fun getSavedToken(context: Context): String? {
        val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        return prefs.getString("fcm_token", null)
    }
    
    /**
     * Subscribe to a topic for group notifications
     */
    fun subscribeToTopic(topic: String, onComplete: (Boolean) -> Unit = {}) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Subscribed to topic: $topic")
                    onComplete(true)
                } else {
                    Log.e(TAG, "❌ Failed to subscribe to topic: $topic", task.exception)
                    onComplete(false)
                }
            }
    }
    
    /**
     * Unsubscribe from a topic
     */
    fun unsubscribeFromTopic(topic: String, onComplete: (Boolean) -> Unit = {}) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Unsubscribed from topic: $topic")
                    onComplete(true)
                } else {
                    Log.e(TAG, "❌ Failed to unsubscribe from topic: $topic", task.exception)
                    onComplete(false)
                }
            }
    }
    
    /**
     * Send token to your server (implement this with your backend API)
     */
    fun sendTokenToServer(token: String, userId: String? = null) {
        Log.d(TAG, "📤 Sending token to server: $token")
        
        // TODO: Implement API call to your server
        // Example:
        // val api = RetrofitClient.getInstance()
        // api.registerFCMToken(token, userId).enqueue(object : Callback<Response> {
        //     override fun onResponse(call: Call<Response>, response: Response<Response>) {
        //         Log.d(TAG, "✅ Token registered with server")
        //     }
        //     override fun onFailure(call: Call<Response>, t: Throwable) {
        //         Log.e(TAG, "❌ Failed to register token", t)
        //     }
        // })
    }
    
    /**
     * Delete token (for sign out)
     */
    fun deleteToken(context: Context, onComplete: (Boolean) -> Unit = {}) {
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Token deleted")
                    // Clear saved token
                    val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                    prefs.edit { remove("fcm_token") }
                    onComplete(true)
                } else {
                    Log.e(TAG, "❌ Failed to delete token", task.exception)
                    onComplete(false)
                }
            }
    }
}
