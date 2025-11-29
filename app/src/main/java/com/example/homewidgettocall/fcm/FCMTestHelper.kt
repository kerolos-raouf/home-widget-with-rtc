package com.example.homewidgettocall.fcm

import android.content.Context
import android.util.Log
import android.widget.Toast

/**
 * Utility for testing FCM functionality
 * Use this during development to test notifications
 */
object FCMTestHelper {
    private const val TAG = "FCMTestHelper"
    
    /**
     * Log all FCM information for debugging
     */
    fun logFCMInfo(context: Context) {
        val token = FCMHelper.getSavedToken(context)
        
        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "FCM DEBUGGING INFORMATION")
        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "Token: $token")
        Log.d(TAG, "Token Length: ${token?.length ?: 0}")
        Log.d(TAG, "Token Saved: ${token != null}")
        Log.d(TAG, "=".repeat(50))
        
        if (token != null) {
            Toast.makeText(
                context,
                "FCM Token available - Check Logcat for full token",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "No FCM Token found - Check Firebase setup",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Simulate incoming call notification (for testing UI)
     */
    fun simulateIncomingCallNotification(context: Context) {
        Log.d(TAG, "🧪 Simulating incoming call notification...")
        
        val data = mapOf(
            "type" to "incoming_call",
            "caller_id" to "test_user_123",
            "caller_name" to "Test Caller",
            "room_id" to "test-room",
            "server_url" to "https://test-server.com"
        )
        
        // This won't actually trigger the FCM service, but you can use it
        // to test your notification UI by calling the service method directly
        Toast.makeText(
            context,
            "Check Logcat for simulated notification data",
            Toast.LENGTH_SHORT
        ).show()
        
        Log.d(TAG, "Simulated data: $data")
    }
    
    /**
     * Print cURL command to send test notification
     */
    fun getCurlCommand(context: Context, serverKey: String): String {
        val token = FCMHelper.getSavedToken(context) ?: return "No token available"
        
        val curl = """
            curl -X POST https://fcm.googleapis.com/fcm/send \
            -H "Authorization: key=$serverKey" \
            -H "Content-Type: application/json" \
            -d '{
              "to": "$token",
              "priority": "high",
              "data": {
                "type": "incoming_call",
                "caller_id": "user123",
                "caller_name": "John Doe",
                "room_id": "widget-room",
                "server_url": "https://your-server.com"
              }
            }'
        """.trimIndent()
        
        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "CURL COMMAND TO TEST FCM")
        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, curl)
        Log.d(TAG, "=".repeat(50))
        
        return curl
    }
    
    /**
     * Validate Firebase setup
     */
    fun validateSetup(context: Context): ValidationResult {
        val token = FCMHelper.getSavedToken(context)
        val issues = mutableListOf<String>()
        
        if (token == null) {
            issues.add("❌ FCM Token not available")
            issues.add("   - Check google-services.json")
            issues.add("   - Check Firebase dependencies")
            issues.add("   - Check internet connection")
        } else {
            Log.d(TAG, "✅ FCM Token available")
        }
        
        // Check notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (!permissionGranted) {
                issues.add("⚠️ Notification permission not granted")
            } else {
                Log.d(TAG, "✅ Notification permission granted")
            }
        }
        
        val result = ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
        
        Log.d(TAG, "=".repeat(50))
        Log.d(TAG, "FCM SETUP VALIDATION")
        Log.d(TAG, "=".repeat(50))
        if (result.isValid) {
            Log.d(TAG, "✅ Setup is valid - Ready to receive notifications!")
        } else {
            Log.e(TAG, "❌ Setup has issues:")
            result.issues.forEach { Log.e(TAG, it) }
        }
        Log.d(TAG, "=".repeat(50))
        
        return result
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val issues: List<String>
    )
}
