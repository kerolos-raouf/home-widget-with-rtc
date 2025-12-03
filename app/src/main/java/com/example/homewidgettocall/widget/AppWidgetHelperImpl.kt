package com.example.homewidgettocall.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
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
     */
    private fun RemoteViews.setFriendName() {
        val token = PreferencesHelper.getFirstFriendToken(context)
        val friendName = if (token != null) {
            // Extract email from SharedPreferences or show "First Friend"
            getFirstFriendEmail() ?: "First Friend"
        } else {
            "Add a Friend"
        }
        
        setTextViewText(R.id.txt_friend_name, friendName)
        Log.d(TAG, "Widget friend name set to: $friendName")
    }

    /**
     * Get the first friend's email from SharedPreferences
     */
    private fun getFirstFriendEmail(): String? {
        val prefs = context.getSharedPreferences("HomeWidgetToCallPrefs", Context.MODE_PRIVATE)
        return prefs.getString("first_friend_email", null)
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
     * Set up the edit button to open the app
     */
    private fun RemoteViews.setEditButton() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setOnClickPendingIntent(R.id.btn_edit, pendingIntent)
        Log.d(TAG, "Edit button configured to open app")
    }
    
    companion object {
        private const val TAG = "AppWidgetHelper"
    }
}
