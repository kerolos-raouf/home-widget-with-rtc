package com.example.homewidgettocall.widget


interface AppWidgetHelper {
    fun getWidgetIdList(): List<Int>

    fun updateWidgetData(widgetId: Int)
}