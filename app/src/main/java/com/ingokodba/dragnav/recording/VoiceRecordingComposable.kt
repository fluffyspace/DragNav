package com.ingokodba.dragnav.recording

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.dragnav.R
import com.ingokodba.dragnav.rainbow.RainbowPathView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun VoiceRecordingComposable(context: Context){

    val TAG = "VoiceRecordingComposable"
    var isDragging by remember { mutableStateOf(false) }
    var isDragEnabled by remember { mutableStateOf(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    // Voice recording state
    var recordingService by remember { mutableStateOf<VoiceRecordingService?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Service connection
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as VoiceRecordingService.LocalBinder
                recordingService = binder.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                recordingService = null
            }
        }
    }

    // Bind to service and collect recording state
    DisposableEffect(Unit) {
        val intent = android.content.Intent(context, VoiceRecordingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        onDispose {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service", e)
            }
        }
    }

    // Collect recording state from service
    LaunchedEffect(recordingService) {
        recordingService?.let { service ->
            launch {
                service.isRecording.collect { recording ->
                    isRecording = recording
                }
            }
            launch {
                service.recordingDuration.collect { duration ->
                    recordingDuration = duration
                }
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            // Start recording after permission granted
            val intent = android.content.Intent(context, VoiceRecordingService::class.java).apply {
                action = VoiceRecordingService.ACTION_START_RECORDING
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "recordButtonScale"
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) ComposeColor.Red else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 300),
        label = "recordButtonColor"
    )

    // Voice Recording Button - draggable round button
    val density = LocalDensity.current
    val buttonSizeDp = 56.dp
    val buttonSizePx = with(density) { buttonSizeDp.toPx() }

    // Load saved position from SharedPreferences
    val recordingButtonPrefs = remember {
        context.getSharedPreferences("recording_button_prefs", Context.MODE_PRIVATE)
    }

    // Track container size for boundary constraints
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Default position is bottom-right with padding
    val defaultOffsetX = remember(containerSize) {
        if (containerSize.width > 0) containerSize.width - buttonSizePx - with(density) { 16.dp.toPx() }
        else 0f
    }
    val defaultOffsetY = remember(containerSize) {
        if (containerSize.height > 0) containerSize.height - buttonSizePx - with(density) { 48.dp.toPx() }
        else 0f
    }

    // Load saved position or use default
    var offsetX by remember(containerSize) {
        mutableStateOf(
            recordingButtonPrefs.getFloat("button_x", -1f).let { saved ->
                if (saved >= 0f && containerSize.width > 0) saved.coerceIn(0f, (containerSize.width - buttonSizePx).coerceAtLeast(0f))
                else defaultOffsetX
            }
        )
    }
    var offsetY by remember(containerSize) {
        mutableStateOf(
            recordingButtonPrefs.getFloat("button_y", -1f).let { saved ->
                if (saved >= 0f && containerSize.height > 0) saved.coerceIn(0f, (containerSize.height - buttonSizePx).coerceAtLeast(0f))
                else defaultOffsetY
            }
        )
    }

    val pathViewRef = remember { mutableStateOf<RainbowPathView?>(null) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size
            }
    ) {
        Column(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPosition = down.position
                        var totalMovement = 0f
                        var fingerLeftButton = false

                        // Start a coroutine to enable dragging after 2 seconds
                        longPressJob?.cancel()
                        longPressJob = scope.launch {
                            delay(1000)
                            if (totalMovement < 20f && !fingerLeftButton) {
                                isDragEnabled = true
                                isDragging = true
                                // Haptic feedback when drag mode is enabled
                                pathViewRef.value?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                // Check if finger left the button area (using button size as bounds)
                                val currentPos = change.position
                                if (currentPos.x < 0 || currentPos.y < 0 ||
                                    currentPos.x > buttonSizePx || currentPos.y > buttonSizePx + with(density) { 40.dp.toPx() }) {
                                    fingerLeftButton = true
                                    // Cancel long press timer if finger left button
                                    if (!isDragEnabled) {
                                        longPressJob?.cancel()
                                    }
                                }

                                if (!change.pressed) {
                                    // Finger lifted
                                    longPressJob?.cancel()

                                    if (isDragEnabled && isDragging) {
                                        // Was dragging - save position
                                        recordingButtonPrefs.edit()
                                            .putFloat("button_x", offsetX)
                                            .putFloat("button_y", offsetY)
                                            .apply()
                                    } else if (!fingerLeftButton && totalMovement < 100f) {
                                        // Count as click if:
                                        // - finger didn't leave button area AND
                                        // - total movement < 100px
                                        if (!hasRecordPermission) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            val intent = android.content.Intent(context, VoiceRecordingService::class.java).apply {
                                                action = if (isRecording) {
                                                    VoiceRecordingService.ACTION_STOP_RECORDING
                                                } else {
                                                    VoiceRecordingService.ACTION_START_RECORDING
                                                }
                                            }
                                            if (!isRecording && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                        }
                                    }
                                    // If finger left button OR moved >= 100px, do nothing (cancelled gesture)

                                    isDragging = false
                                    isDragEnabled = false
                                    break
                                }

                                val dragAmount = change.position - change.previousPosition
                                totalMovement += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)

                                // Only move if drag is enabled (after 2 second hold)
                                if (isDragEnabled) {
                                    change.consume()
                                    val newX = (offsetX + dragAmount.x).coerceIn(
                                        0f,
                                        (containerSize.width - buttonSizePx).coerceAtLeast(0f)
                                    )
                                    val newY = (offsetY + dragAmount.y).coerceIn(
                                        0f,
                                        (containerSize.height - buttonSizePx).coerceAtLeast(0f)
                                    )
                                    offsetX = newX
                                    offsetY = newY
                                }
                            }
                        } catch (e: Exception) {
                            longPressJob?.cancel()
                            isDragging = false
                            isDragEnabled = false
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration display when recording
            if (isRecording) {
                val minutes = (recordingDuration / 60000).toInt()
                val seconds = ((recordingDuration % 60000) / 1000).toInt()
                val millis = ((recordingDuration % 1000) / 100).toInt()
                Text(
                    text = String.format("%02d:%02d.%d", minutes, seconds, millis),
                    color = ComposeColor.Red,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Recording button
            FloatingActionButton(
                onClick = { /* Click handled by pointerInput above */ },
                modifier = Modifier
                    .size(buttonSizeDp)
                    .scale(buttonScale),
                shape = CircleShape,
                containerColor = if (isDragEnabled) MaterialTheme.colorScheme.tertiary else buttonColor
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isDragEnabled) R.drawable.ic_baseline_open_with_24
                        else if (isRecording) R.drawable.ic_baseline_stop_24
                        else R.drawable.ic_baseline_mic_24
                    ),
                    contentDescription = if (isDragEnabled) "Drag to move"
                    else if (isRecording) "Stop Recording"
                    else "Start Recording",
                    tint = ComposeColor.White
                )
            }
        }
    }
}