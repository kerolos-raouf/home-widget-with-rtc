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
import kotlin.coroutines.resumeWithException

class AudioDownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioClient: AudioMessageClient? = null

    override suspend fun doWork(): Result {
        val messageId = inputData.getString(EXTRA_MESSAGE_ID) ?: return Result.failure()
        val senderId = inputData.getString(EXTRA_SENDER_ID) ?: "unknown"


        return try {
            val audioData = downloadAudioSuspend(messageId)
            playAudioSuspend(audioData)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ AudioWorker failed", e)
            Result.retry()
        }
    }

    private suspend fun downloadAudioSuspend(messageId: String): String =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            audioClient = AudioMessageClient(
                WidgetReceiver.DEFAULT_SERVER_URL,
                object : AudioMessageClient.AudioMessageListener {
                    override fun onConnected() {
                        audioClient?.downloadAudio(messageId)
                    }

                    override fun onDisconnected() {
                        if (!cont.isCompleted) cont.resumeWithException(Exception("Disconnected"))
                    }

                    override fun onDownloadSuccess(
                        messageId: String,
                        audioData: String,
                        duration: Int
                    ) {
                        if (!cont.isCompleted) cont.resume(audioData) {}
                    }

                    override fun onDownloadFailed(error: String) {
                        if (!cont.isCompleted) cont.resumeWithException(Exception(error))
                    }

                    override fun onError(error: String) {
                        if (!cont.isCompleted) cont.resumeWithException(Exception(error))
                    }

                    override fun onUploadSuccess(messageId: String) {}
                    override fun onUploadFailed(error: String) {}
                })

            audioClient?.connect()

            cont.invokeOnCancellation {
                audioClient?.disconnect()
            }
        }


    private suspend fun playAudioSuspend(base64Data: String) =
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
            try {
                val audioBytes = android.util.Base64.decode(
                    base64Data.replace(
                        Regex("^data:audio/\\w+;base64,"),
                        ""
                    ), android.util.Base64.DEFAULT
                )
                val tempFile = File(
                    applicationContext.cacheDir,
                    "temp_audio_${System.currentTimeMillis()}.webm"
                )
                FileOutputStream(tempFile).use { it.write(audioBytes) }

                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        tempFile.delete()
                        cont.resume(Unit) {}
                    }
                    setOnErrorListener { mp, what, extra ->
                        tempFile.delete()
                        cont.resumeWithException(Exception("MediaPlayer error: what=$what, extra=$extra"))
                        true
                    }
                    prepareAsync()
                }

                cont.invokeOnCancellation {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    tempFile.delete()
                }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }


    companion object {
        const val TAG = "AudioDownloadWorker"
    }
}
