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
import java.io.File

const val ACTION_TOGGLE_RECORDING = "ACTION_TOGGLE_RECORDING"
const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
const val ACTION_START_RECORDING = "ACTION_START_RECORDING"

class VoiceRecorderService : Service() {

    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_START_RECORDING, ACTION_TOGGLE_RECORDING -> {
                if (!isRecording) {
                    startRecordingForeground()
                } else {
                    // Stop recording and send audio
                    stopRecordingAndSend()
                }
            }

            ACTION_STOP_RECORDING -> {
                stopRecordingAndSend()
            }
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
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }

        startRecording()
        isRecording = true
    }

    private fun startRecording() {
        // Create output file
        outputFile = File(
            externalCacheDir?.absolutePath,
            "record_${System.currentTimeMillis()}.m4a"
        )
        
        Log.d(TAG, "📹 Starting recording to: ${outputFile?.absolutePath}")
        
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile?.absolutePath)
            prepare()
            start()
        }
    }

    private fun stopRecordingAndSend() {
        Log.d(TAG, "⏹️ Stopping recording...")
        
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecording = false
            
            // Send the recorded audio
            outputFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "✅ Recording saved: ${file.absolutePath} (${file.length()} bytes)")
                    
                    // Trigger audio message send via WidgetReceiver
                    val sendIntent = Intent(this, WidgetReceiver::class.java).apply {
                        action = ACTION_SEND_AUDIO_MESSAGE
                        putExtra(AUDIO_FILE_PATH_EXTRA, file.absolutePath)
                        // Optional: Add custom server URL and recipient
                        // putExtra(SERVER_URL_EXTRA, "your_server_url")
                        // putExtra(RECIPIENT_FCM_TOKEN_EXTRA, "recipient_token")
                    }
                    sendBroadcast(sendIntent)
                    
                    Log.d(TAG, "📤 Triggered audio send")
                } else {
                    Log.e(TAG, "❌ Recording file is empty or doesn't exist")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Stop recording failed: ${e.message}", e)
        } finally {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isRecording) {
                recorder?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop failed on destroy: ${e.message}")
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
            .setContentTitle("🎙️ Recording in progress")
            .setContentText("Tap to stop and send")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "Stop & Send", stopPendingIntent)
            .build()
    }
    
    companion object {
        private const val TAG = "VoiceRecorderService"
    }
}
