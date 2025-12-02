package com.example.homewidgettocall.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.homewidgettocall.R

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
            setRecordingButton(widgetId)
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
    
    companion object {
        private const val TAG = "AppWidgetHelper"
    }
}
