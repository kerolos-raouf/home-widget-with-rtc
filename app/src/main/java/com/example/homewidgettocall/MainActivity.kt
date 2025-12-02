package com.example.homewidgettocall

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.homewidgettocall.fcm.FCMHelper
import com.example.homewidgettocall.model.VoiceRecording
import java.io.File

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    lateinit var adapter: VoiceRecordingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        }

        // Initialize FCM and get token
        initializeFCM()

        // Setup recordings list
        val recordings = getRecordedVoices(this)

        adapter = VoiceRecordingsAdapter(
            recordings,
            onPlayClick = { file ->
                playRecording(file)
            },
            onPauseClick = {
                pauseRecording()
            })

        findViewById<RecyclerView>(R.id.voices_list).adapter = adapter

        findViewById<Button>(R.id.refresh_list).setOnClickListener {
            val files = getRecordedVoices(this)
            adapter.updateList(files)
        }
    }

    /**
     * Initialize Firebase Cloud Messaging
     */
    private fun initializeFCM() {
        Log.d(TAG, "Initializing FCM...")
        
        // Get FCM token
        FCMHelper.getCurrentToken(this) { token ->
            if (token != null) {
                Log.d(TAG, "✅ FCM Token obtained: $token")
                Toast.makeText(this, "FCM Token: ${token.take(20)}...", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "❌ Failed to get FCM token")
                Toast.makeText(this, "Failed to get FCM token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getRecordedVoices(context: Context): List<VoiceRecording> {
        val dir = context.externalCacheDir ?: return emptyList()

        return dir.listFiles { file ->
            file.extension == "m4a"
        }?.map {
            VoiceRecording(
                file = it,
                name = it.name,
                date = it.lastModified()
            )
        }?.sortedByDescending { it.date } ?: emptyList()
    }

    fun playRecording(file: File) {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: ${file.absolutePath}")
            return
        }

        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { start() }
            setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "MediaPlayer error: $what, $extra")
                true
            }
            prepareAsync()
        }
    }

    fun pauseRecording() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
