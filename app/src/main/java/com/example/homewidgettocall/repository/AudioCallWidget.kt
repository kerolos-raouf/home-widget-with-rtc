//package com.example.homewidgettocall.repository
//
//import android.app.PendingIntent
//import android.appwidget.AppWidgetManager
//import android.appwidget.AppWidgetProvider
//import android.content.Context
//import android.content.Intent
//import android.widget.RemoteViews
//
///**
// * Example home widget for audio calls
// * User can start/end calls directly from home screen
// */
//class AudioCallWidget : AppWidgetProvider() {
//
//    override fun onUpdate(
//        context: Context,
//        appWidgetManager: AppWidgetManager,
//        appWidgetIds: IntArray
//    ) {
//        for (appWidgetId in appWidgetIds) {
//            updateAppWidget(context, appWidgetManager, appWidgetId)
//        }
//    }
//
//    companion object {
//        private const val ACTION_START_CALL = "com.yourapp.widget.START_CALL"
//        private const val ACTION_END_CALL = "com.yourapp.widget.END_CALL"
//        private const val ACTION_TOGGLE_MUTE = "com.yourapp.widget.TOGGLE_MUTE"
//
//        // TODO: Replace with your actual server URL and default room
//        private const val SERVER_URL = "https://your-ngrok-url.ngrok-app.com"
//        private const val DEFAULT_ROOM = "widget-room"
//
//        internal fun updateAppWidget(
//            context: Context,
//            appWidgetManager: AppWidgetManager,
//            appWidgetId: Int
//        ) {
//            val views = RemoteViews(context.packageName, R.layout.audio_call_widget)
//
//            // Start call button
//            val startCallIntent = Intent(context, AudioCallWidget::class.java).apply {
//                Intent.setAction = ACTION_START_CALL
//            }
//            val startCallPendingIntent = PendingIntent.getBroadcast(
//                context, 0, startCallIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//            views.setOnClickPendingIntent(R.id.btn_start_call, startCallPendingIntent)
//
//            // End call button
//            val endCallIntent = Intent(context, AudioCallWidget::class.java).apply {
//                Intent.setAction = ACTION_END_CALL
//            }
//            val endCallPendingIntent = PendingIntent.getBroadcast(
//                context, 1, endCallIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//            views.setOnClickPendingIntent(R.id.btn_end_call, endCallPendingIntent)
//
//            // Toggle mute button
//            val muteIntent = Intent(context, AudioCallWidget::class.java).apply {
//                Intent.setAction = ACTION_TOGGLE_MUTE
//            }
//            val mutePendingIntent = PendingIntent.getBroadcast(
//                context, 2, muteIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//            views.setOnClickPendingIntent(R.id.btn_toggle_mute, mutePendingIntent)
//
//            appWidgetManager.updateAppWidget(appWidgetId, views)
//        }
//    }
//
//    override fun onReceive(context: Context, intent: Intent) {
//        super.onReceive(context, intent)
//
//        when (intent.action) {
//            ACTION_START_CALL -> {
//                // Start audio call service
//                AudioCallService.startCall(context, SERVER_URL, DEFAULT_ROOM)
//            }
//            ACTION_END_CALL -> {
//                // End audio call
//                AudioCallService.endCall(context)
//            }
//            ACTION_TOGGLE_MUTE -> {
//                // Toggle mute
//                AudioCallService.toggleMute(context)
//            }
//        }
//    }
//}
