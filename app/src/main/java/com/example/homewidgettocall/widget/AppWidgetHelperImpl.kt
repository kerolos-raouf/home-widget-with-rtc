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

const val WIDGET_ID = "widget_id"

class AppWidgetHelperImpl(
    private val context: Context
) : AppWidgetHelper {

    private val squareWidgetComponent = ComponentName(context, WidgetProvider::class.java)
    private val appWidgetManager = AppWidgetManager.getInstance(context)

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
            setFriendName()
            setRecordingButton(widgetId)
            setEditButton()
        }

    /**
     * Set the friend name from SharedPreferences
     * Shows "Login" if user is logged out
     */
    private fun RemoteViews.setFriendName() {
        // Check if user is logged in
        val isLoggedIn = PreferencesHelper.isLoggedIn(context)
        
        val friendName = if (!isLoggedIn) {
            // User is logged out, show "Login"
            "Login"
        } else {
            // User is logged in, check for friend
            val friendEmail = PreferencesHelper.getFirstFriendEmail(context)
            val token = PreferencesHelper.getFirstFriendToken(context)
            
            if (friendEmail != null && token != null) {
                // Has friend, show email
                friendEmail
            } else {
                // No friend, show prompt
                "Add a Friend"
            }
        }
        
        setTextViewText(R.id.txt_friend_name, friendName)
        Log.d(TAG, "Widget friend name set to: $friendName (Logged in: $isLoggedIn)")
    }

    /**
     * Set up the recording button to start/stop recording
     * When recording stops, it will automatically send the audio
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

        Log.d(TAG, "Setting recording button pending intent for widget: $widgetId")

        // Set the click listener on the recording button
        setOnClickPendingIntent(R.id.btn_start_recording, pendingIntent)
    }

    /**
     * Set up the edit button
     * Opens LoginActivity if logged out, MainActivity if logged in
     */
    private fun RemoteViews.setEditButton() {
        val isLoggedIn = PreferencesHelper.isLoggedIn(context)
        
        val intent = if (isLoggedIn) {
            // User is logged in, open MainActivity
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            // User is logged out, open LoginActivity
            Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setOnClickPendingIntent(R.id.btn_edit, pendingIntent)
        Log.d(TAG, "Edit button configured (Logged in: $isLoggedIn)")
    }
    
    companion object {
        private const val TAG = "AppWidgetHelper"
    }
}
