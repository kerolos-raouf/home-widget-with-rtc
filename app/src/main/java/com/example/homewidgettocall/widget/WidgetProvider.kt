package com.example.homewidgettocall.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log

open class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            Log.d("UsageWidgetReceiver", "onReceive: Updating widget $it")

            AppWidgetHelperImpl(context.applicationContext).updateWidgetData(it)
        }
    }
}