package com.example.homewidgettocall.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.homewidgettocall.R

private const val PROGRESS_BAR_MAX_VALUE = 100
const val WIDGET_ID = "widget_id"

class AppWidgetHelperImpl(
    private val context: Context
) : AppWidgetHelper {

    private val squareWidgetComponent = ComponentName(context, WidgetProvider::class.java)
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    override fun getWidgetIdList(): List<Int> {
        val squareWidgetIds = appWidgetManager.getAppWidgetIds(squareWidgetComponent)
        val widgetIds = squareWidgetIds
        return listOf(
            *widgetIds.toTypedArray()
        )
    }

    override fun updateWidgetData(widgetId: Int) {
        val remoteViews = getRemoteViewsWithData(widgetId)
        appWidgetManager.updateAppWidget(widgetId, remoteViews)
    }

    private fun getRemoteViewsWithData(
        widgetId: Int,
    ) = RemoteViews(context.packageName, R.layout.widget_layout).apply {
        setReceiverPendingIntent(widgetId)
    }

    private fun RemoteViews.setReceiverPendingIntent(widgetId: Int) {
        val intent = Intent(context, WidgetReceiver::class.java).apply {
            action = SWITCH_TO_NEXT_SUBSCRIPTION_LINE_ACTION
            putExtra(WIDGET_ID, widgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("UsageWidgetReceiver", "onReceive: Setting pending intent")

        setOnClickPendingIntent(R.id.btn_start_recording, pendingIntent)
    }
}