package com.example.homewidgettocall.worker

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.homewidgettocall.audio.AudioMessageClient
import com.example.homewidgettocall.audio.AudioPlaybackService.Companion.EXTRA_MESSAGE_ID
import com.example.homewidgettocall.audio.AudioPlaybackService.Companion.EXTRA_SENDER_ID
import com.example.homewidgettocall.widget.WidgetReceiver
import java.io.File
import java.io.FileOutputStream

class AudioDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioClient: AudioMessageClient? = null

    override suspend fun doWork(): Result {
        val messageId = inputData.getString(EXTRA_MESSAGE_ID) ?: return Result.failure()
        val senderId = inputData.getString(EXTRA_SENDER_ID) ?: "unknown"

        downloadAndPlayAudio(messageId, WidgetReceiver.Companion.DEFAULT_SERVER_URL, senderId)
        return Result.success()
    }

    private fun downloadAndPlayAudio(messageId: String, serverUrl: String, senderId: String) {
        Log.d(TAG, "Connecting to server: $serverUrl")

        audioClient = AudioMessageClient(applicationContext, serverUrl, object : AudioMessageClient.AudioMessageListener {
            override fun onConnected() {
                Log.d(TAG, "✅ Connected to server, downloading audio...")
                audioClient?.downloadAudio(messageId)
            }

            override fun onDisconnected() {
                Log.d(TAG, "Disconnected from server")
            }

            override fun onUploadSuccess(messageId: String) {
                // Not used in playback service
            }

            override fun onUploadFailed(error: String) {
                // Not used in playback service
            }

            override fun onDownloadSuccess(messageId: String, audioData: String, duration: Int) {
                Log.d(TAG, "✅ Download successful, playing audio... (${duration}s)")
                playAudioFromBase64(audioData, senderId)
            }

            override fun onDownloadFailed(error: String) {
                Log.e(TAG, "❌ Download failed: $error")
            }

            override fun onError(error: String) {
                Log.e(TAG, "❌ Error: $error")
            }
        })

        audioClient?.connect()
    }

    private fun playAudioFromBase64(base64Data: String, senderId: String) {
        try {
            // Remove data URL prefix if present
            val base64Audio = base64Data.replace(Regex("^data:audio/\\w+;base64,"), "")

            // Decode base64 to bytes
            val audioBytes = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT)

            Log.d(TAG, "Decoded audio: ${audioBytes.size} bytes")

            // Save to temp file
            val tempFile = File(applicationContext.cacheDir, "temp_audio_${System.currentTimeMillis()}.webm")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            Log.d(TAG, "🎵 Playing audio file: ${tempFile.absolutePath}")

            // Play audio
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)

                setOnPreparedListener {
                    Log.d(TAG, "▶️ Audio prepared, starting playback")
                    start()
                }

                setOnCompletionListener {
                    Log.d(TAG, "✅ Playback completed")
                    tempFile.delete()
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "❌ MediaPlayer error: what=$what, extra=$extra")
                    tempFile.delete()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing audio", e)
        }
    }

    companion object {
        const val TAG = "AudioDownloadWorker"
    }
}
