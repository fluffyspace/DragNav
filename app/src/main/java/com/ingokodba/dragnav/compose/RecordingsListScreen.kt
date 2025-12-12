package com.ingokodba.dragnav.compose

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.dragnav.R
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.modeli.VoiceRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsListScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var recordings by remember { mutableStateOf<List<VoiceRecording>>(emptyList()) }
    var currentlyPlaying by remember { mutableStateOf<Int?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackProgress by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf<VoiceRecording?>(null) }

    // Load recordings from database
    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        db.voiceRecordingDao().getAllFlow().collectLatest { list ->
            recordings = list
        }
    }

    // Clean up media player on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playRecording(recording: VoiceRecording) {
        scope.launch {
            try {
                // Stop current playback
                mediaPlayer?.release()
                mediaPlayer = null

                if (currentlyPlaying == recording.id) {
                    currentlyPlaying = null
                    playbackProgress = 0f
                    return@launch
                }

                val file = File(recording.filePath)
                if (!file.exists()) {
                    Log.e("RecordingsListScreen", "File not found: ${recording.filePath}")
                    return@launch
                }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(recording.filePath)
                    prepare()
                    start()

                    setOnCompletionListener {
                        currentlyPlaying = null
                        playbackProgress = 0f
                    }
                }
                currentlyPlaying = recording.id

                // Update progress
                while (mediaPlayer?.isPlaying == true) {
                    val current = mediaPlayer?.currentPosition ?: 0
                    val total = mediaPlayer?.duration ?: 1
                    playbackProgress = current.toFloat() / total.toFloat()
                    kotlinx.coroutines.delay(100)
                }
            } catch (e: Exception) {
                Log.e("RecordingsListScreen", "Error playing recording", e)
                currentlyPlaying = null
            }
        }
    }

    fun deleteRecording(recording: VoiceRecording) {
        scope.launch(Dispatchers.IO) {
            try {
                // Delete file
                val file = File(recording.filePath)
                if (file.exists()) {
                    file.delete()
                }

                // Delete from database
                val db = AppDatabase.getInstance(context)
                db.voiceRecordingDao().delete(recording)
            } catch (e: Exception) {
                Log.e("RecordingsListScreen", "Error deleting recording", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Recordings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_mic_24),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No recordings yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap the microphone button to start recording",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recordings, key = { it.id }) { recording ->
                    RecordingItem(
                        recording = recording,
                        isPlaying = currentlyPlaying == recording.id,
                        playbackProgress = if (currentlyPlaying == recording.id) playbackProgress else 0f,
                        onPlayClick = { playRecording(recording) },
                        onDeleteClick = { showDeleteDialog = recording }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { recording ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Recording?") },
            text = { Text("This will permanently delete the recording \"${recording.fileName}\".") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteRecording(recording)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecordingItem(
    recording: VoiceRecording,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play button
                FilledIconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isPlaying) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24
                        ),
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Recording info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (recording.title.isNotEmpty()) recording.title else recording.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDuration(recording.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(recording.startTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Delete button
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Progress bar when playing
            if (isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
