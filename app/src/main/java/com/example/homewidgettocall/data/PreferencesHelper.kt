package com.example.homewidgettocall.data

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    
    private const val PREFS_NAME = "HomeWidgetToCallPrefs"
    private const val KEY_FIRST_FRIEND_FCM_TOKEN = "first_friend_fcm_token"
    private const val KEY_FIRST_FRIEND_EMAIL = "first_friend_email"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_CURRENT_FRIEND_INDEX = "current_friend_index"
    
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
     * Save the first friend's email to SharedPreferences
     */
    fun saveFirstFriendEmail(context: Context, email: String) {
        getPreferences(context).edit().apply {
            putString(KEY_FIRST_FRIEND_EMAIL, email)
            apply()
        }
    }
    
    /**
     * Get the first friend's email from SharedPreferences
     * @return Email or null if not found
     */
    fun getFirstFriendEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_FIRST_FRIEND_EMAIL, null)
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
    
    /**
     * Set user login state
     */
    fun setLoggedIn(context: Context, isLoggedIn: Boolean) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            apply()
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Save current friend index for widget navigation
     */
    fun setCurrentFriendIndex(context: Context, index: Int) {
        getPreferences(context).edit().apply {
            putInt(KEY_CURRENT_FRIEND_INDEX, index)
            apply()
        }
    }
    
    /**
     * Get current friend index for widget navigation
     */
    fun getCurrentFriendIndex(context: Context): Int {
        return getPreferences(context).getInt(KEY_CURRENT_FRIEND_INDEX, 0)
    }
    
    /**
     * Clear all user data (logout)
     */
    fun clearAllData(context: Context) {
        getPreferences(context).edit().apply {
            clear()
            apply()
        }
    }
}
