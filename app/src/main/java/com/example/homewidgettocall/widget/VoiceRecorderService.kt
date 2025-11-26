package com.example.homewidgettocall.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.homewidgettocall.R

const val ACTION_TOGGLE_RECORDING = "ACTION_TOGGLE_RECORDING"
const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
const val ACTION_START_RECORDING = "ACTION_START_RECORDING"

class VoiceRecorderService : Service() {

    private var recorder: MediaRecorder? = null
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_START_RECORDING, ACTION_TOGGLE_RECORDING -> {
                if (!isRecording) startRecordingForeground()
                else stopSelf() // toggle behavior
            }

            ACTION_STOP_RECORDING -> stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startRecordingForeground() {
        val notification = buildRecordingNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(
                    1,
                    notification,
                )
            }
        } else {
            startForeground(1, notification)
        }

        startRecording()
        isRecording = true
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("${externalCacheDir?.absolutePath}/record_${System.currentTimeMillis()}.m4a")
            prepare()
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e("VoiceRecorderService", "Stop failed: ${e.message}")
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
        }
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "recording_channel",
            "Voice Recording",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildRecordingNotification(): Notification {
        val stopIntent = Intent(this, VoiceRecorderService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "recording_channel")
            .setContentTitle("Recording in progress")
            .setContentText("Tap to stop recording")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()
    }
}