package com.example.homewidgettocall.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.homewidgettocall.LoginActivity
import com.example.homewidgettocall.R
import com.example.homewidgettocall.data.PreferencesHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

const val WIDGET_ID = "widget_id"
const val ACTION_PREVIOUS_FRIEND = "com.example.homewidgettocall.ACTION_PREVIOUS_FRIEND"
const val ACTION_NEXT_FRIEND = "com.example.homewidgettocall.ACTION_NEXT_FRIEND"

class AppWidgetHelperImpl(
    private val context: Context
) : AppWidgetHelper {

    private val squareWidgetComponent = ComponentName(context, WidgetProvider::class.java)
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun getWidgetIdList(): List<Int> {
        val squareWidgetIds = appWidgetManager.getAppWidgetIds(squareWidgetComponent)
        return squareWidgetIds.toList()
    }

    override fun updateWidgetData(widgetId: Int) {
        val remoteViews = getRemoteViewsWithData(widgetId)
        appWidgetManager.updateAppWidget(widgetId, remoteViews)
    }

    private fun getRemoteViewsWithData(widgetId: Int) = 
        RemoteViews(context.packageName, R.layout.widget_layout).apply {
            setFriendInfo()
            setRecordingButton(widgetId)
            setNavigationButtons(widgetId)
        }

    /**
     * Set friend information from Firestore
     */
    private fun RemoteViews.setFriendInfo() {
        val isLoggedIn = PreferencesHelper.isLoggedIn(context)
        
        if (!isLoggedIn) {
            // User is logged out, show "Login"
            setTextViewText(R.id.txt_friend_name, "Login")
            setTextViewText(R.id.txt_friend_index, "")
            Log.d(TAG, "Widget: User logged out")
            return
        }
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            setTextViewText(R.id.txt_friend_name, "Login")
            setTextViewText(R.id.txt_friend_index, "")
            Log.d(TAG, "Widget: No user ID")
            return
        }
        
        // Load friends from Firestore
        loadFriendsAndDisplay(this, userId)
    }
    
    private fun loadFriendsAndDisplay(remoteViews: RemoteViews, userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val friendsData = document.get("friends") as? List<Map<String, String>> ?: emptyList()
                    
                    if (friendsData.isEmpty()) {
                        remoteViews.setTextViewText(R.id.txt_friend_name, "Add a Friend")
                        remoteViews.setTextViewText(R.id.txt_friend_index, "")
                        PreferencesHelper.setCurrentFriendIndex(context, 0)
                        Log.d(TAG, "Widget: No friends")
                    } else {
                        var currentIndex = PreferencesHelper.getCurrentFriendIndex(context)
                        
                        // Ensure index is valid
                        if (currentIndex >= friendsData.size) {
                            currentIndex = 0
                            PreferencesHelper.setCurrentFriendIndex(context, 0)
                        }
                        
                        val currentFriend = friendsData[currentIndex]
                        val email = currentFriend["email"] ?: "Unknown"
                        val fcmToken = currentFriend["fcmToken"] ?: ""
                        
                        // Save current friend data
                        PreferencesHelper.saveFirstFriendEmail(context, email)
                        PreferencesHelper.saveFirstFriendToken(context, fcmToken)
                        
                        // Update widget display
                        remoteViews.setTextViewText(R.id.txt_friend_name, email)
                        remoteViews.setTextViewText(R.id.txt_friend_index, "${currentIndex + 1} of ${friendsData.size}")
                        
                        Log.d(TAG, "Widget: Showing friend ${currentIndex + 1}/${friendsData.size}: $email")
                    }
                    
                    // Update widget
                    val widgetIds = appWidgetManager.getAppWidgetIds(squareWidgetComponent)
                    widgetIds.forEach { widgetId ->
                        appWidgetManager.updateAppWidget(widgetId, remoteViews)
                    }
                } else {
                    Log.w(TAG, "Widget: User document not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Widget: Error loading friends", e)
            }
    }

    /**
     * Set up navigation buttons (previous/next friend)
     */
    private fun RemoteViews.setNavigationButtons(widgetId: Int) {
        // Previous button
        val previousIntent = Intent(context, WidgetReceiver::class.java).apply {
            action = ACTION_PREVIOUS_FRIEND
            putExtra(WIDGET_ID, widgetId)
        }
        val previousPendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 1000 + 1,
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setOnClickPendingIntent(R.id.btn_previous_friend, previousPendingIntent)
        
        // Next button
        val nextIntent = Intent(context, WidgetReceiver::class.java).apply {
            action = ACTION_NEXT_FRIEND
            putExtra(WIDGET_ID, widgetId)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId * 1000 + 2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setOnClickPendingIntent(R.id.btn_next_friend, nextPendingIntent)
        
        Log.d(TAG, "Widget: Navigation buttons configured")
    }

    /**
     * Set up the recording button to start/stop recording
     */
    private fun RemoteViews.setRecordingButton(widgetId: Int) {
        val intent = Intent(context, WidgetReceiver::class.java).apply {
            action = RECORD_A_VOICE
            putExtra(WIDGET_ID, widgetId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setOnClickPendingIntent(R.id.btn_start_recording, pendingIntent)
    }
    
    /**
     * Navigate to previous friend
     */
    fun navigateToPreviousFriend() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val friendsData = document.get("friends") as? List<*>

                    if (friendsData?.isNotEmpty() == true) {
                        var currentIndex = PreferencesHelper.getCurrentFriendIndex(context)

                        // Move to previous friend (wrap around)
                        currentIndex = if (currentIndex > 0) {
                            currentIndex - 1
                        } else {
                            friendsData.size - 1
                        }
                        
                        PreferencesHelper.setCurrentFriendIndex(context, currentIndex)
                        Log.d(TAG, "Widget: Navigated to previous friend (index: $currentIndex)")
                        
                        // Update widget
                        updateAllWidgets()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Widget: Error navigating to previous friend", e)
            }
    }
    
    /**
     * Navigate to next friend
     */
    fun navigateToNextFriend() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val friendsData = document.get("friends") as? List<*>
                    
                    if (friendsData?.isNotEmpty() == true) {
                        var currentIndex = PreferencesHelper.getCurrentFriendIndex(context)
                        
                        // Move to next friend (wrap around)
                        currentIndex = if (currentIndex < friendsData.size - 1) {
                            currentIndex + 1
                        } else {
                            0
                        }
                        
                        PreferencesHelper.setCurrentFriendIndex(context, currentIndex)
                        Log.d(TAG, "Widget: Navigated to next friend (index: $currentIndex)")
                        
                        // Update widget
                        updateAllWidgets()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Widget: Error navigating to next friend", e)
            }
    }
    
    private fun updateAllWidgets() {
        val widgetIds = getWidgetIdList()
        widgetIds.forEach { widgetId ->
            updateWidgetData(widgetId)
        }
    }
    
    companion object {
        private const val TAG = "AppWidgetHelper"
    }
}
