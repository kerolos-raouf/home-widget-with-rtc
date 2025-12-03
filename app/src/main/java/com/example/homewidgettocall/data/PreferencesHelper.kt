package com.example.homewidgettocall.data

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    
    private const val PREFS_NAME = "HomeWidgetToCallPrefs"
    private const val KEY_FIRST_FRIEND_FCM_TOKEN = "first_friend_fcm_token"
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save the first friend's FCM token to SharedPreferences
     */
    fun saveFirstFriendToken(context: Context, token: String) {
        getPreferences(context).edit().apply {
            putString(KEY_FIRST_FRIEND_FCM_TOKEN, token)
            apply()
        }
    }
    
    /**
     * Get the first friend's FCM token from SharedPreferences
     * @return FCM token or null if not found
     */
    fun getFirstFriendToken(context: Context): String? {
        return getPreferences(context).getString(KEY_FIRST_FRIEND_FCM_TOKEN, null)
    }
    
    /**
     * Clear the first friend's FCM token from SharedPreferences
     */
    fun clearFirstFriendToken(context: Context) {
        getPreferences(context).edit().apply {
            remove(KEY_FIRST_FRIEND_FCM_TOKEN)
            apply()
        }
    }
    
    /**
     * Check if first friend token exists
     */
    fun hasFirstFriendToken(context: Context): Boolean {
        return getFirstFriendToken(context) != null
    }
}
