package com.ingokodba.dragnav.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dragnav.R
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.modeli.VoiceRecording
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VoiceRecordingService : Service() {

    companion object {
        private const val TAG = "VoiceRecordingService"
        private const val NOTIFICATION_CHANNEL_ID = "voice_recording_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_RECORDING = "com.ingokodba.dragnav.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.ingokodba.dragnav.STOP_RECORDING"
    }

    private val binder = LocalBinder()
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var currentFileName: String? = null
    private var recordingStartTime: Long = 0
    private var currentRecordingId: Int = -1

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private var durationJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): VoiceRecordingService = this@VoiceRecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Voice Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when voice recording is active"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivityCompose::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Recording...")
            .setContentText("Tap to open app")
            .setSmallIcon(R.drawable.ic_baseline_mic_24)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_baseline_stop_24, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun startRecording() {
        if (_isRecording.value) {
            Log.d(TAG, "Already recording, ignoring start request")
            return
        }

        try {
            // Create recordings directory
            val recordingsDir = File(getExternalFilesDir(null), "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }

            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentFileName = "recording_$timestamp.m4a"
            currentFilePath = File(recordingsDir, currentFileName!!).absolutePath

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentFilePath)
                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true

            // Save initial recording entry to database
            serviceScope.launch {
                val recording = VoiceRecording(
                    filePath = currentFilePath!!,
                    fileName = currentFileName!!,
                    startTime = recordingStartTime
                )
                val db = AppDatabase.getInstance(applicationContext)
                val id = db.voiceRecordingDao().insert(recording)
                currentRecordingId = id.toInt()
                Log.d(TAG, "Recording started, saved with id: $currentRecordingId")
            }

            // Start duration tracking
            startDurationTracking()

            // Start foreground service with notification
            startForeground(NOTIFICATION_ID, createNotification())

            Log.d(TAG, "Recording started: $currentFilePath")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanupRecording()
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) {
            Log.d(TAG, "Not recording, ignoring stop request")
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            val endTime = System.currentTimeMillis()
            val duration = endTime - recordingStartTime

            // Update recording in database
            serviceScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val recording = db.voiceRecordingDao().findById(currentRecordingId)
                if (recording != null) {
                    val file = File(currentFilePath!!)
                    val updatedRecording = recording.copy(
                        endTime = endTime,
                        durationMs = duration,
                        fileSize = if (file.exists()) file.length() else 0
                    )
                    db.voiceRecordingDao().update(updatedRecording)
                    Log.d(TAG, "Recording saved: duration=${duration}ms, size=${file.length()} bytes")
                }
            }

            _isRecording.value = false
            _recordingDuration.value = 0
            durationJob?.cancel()

            // Stop foreground service
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            Log.d(TAG, "Recording stopped: $currentFilePath")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        } finally {
            cleanupRecording()
        }
    }

    private fun cleanupRecording() {
        mediaRecorder?.release()
        mediaRecorder = null
        _isRecording.value = false
        _recordingDuration.value = 0
        durationJob?.cancel()
    }

    private fun startDurationTracking() {
        durationJob?.cancel()
        durationJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive && _isRecording.value) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                _recordingDuration.value = elapsed
                delay(100) // Update every 100ms
            }
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }
}
