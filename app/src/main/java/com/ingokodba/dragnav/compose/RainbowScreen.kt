package com.ingokodba.dragnav.compose

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragnav.R
import com.ingokodba.dragnav.*
import com.ingokodba.dragnav.modeli.MiddleButtonStates
import com.ingokodba.dragnav.modeli.RainbowMapa
import kotlinx.coroutines.*

/**
 * Compose screen for Rainbow UI mode (RAINBOW_RIGHT/RAINBOW_LEFT).
 * Wraps the custom Rainbow view and provides settings sliders, favorites toggle, and dialog overlays.
 *
 * @param mainActivity Reference to MainActivityCompose for navigation and database operations
 * @param rightSide True for RAINBOW_RIGHT, false for RAINBOW_LEFT
 * @param modifier Optional Modifier for the root composable
 * @param viewModel Shared ViewModel containing app data
 */
@Composable
fun RainbowScreen(
    mainActivity: MainActivityCompose,
    rightSide: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE) }

    // Collect app data from ViewModel
    val appsList by viewModel.appsListFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val icons by viewModel.iconsFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val rainbowFiltered by remember { derivedStateOf { viewModel.rainbowFiltered } }
    val rainbowMape by viewModel.rainbowMapeFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // UI state
    var slidersVisible by remember { mutableStateOf(false) }
    var onlyFavorites by remember { mutableStateOf(prefs.getBoolean("onlyfavorites", false)) }
    var inFolder by remember { mutableStateOf(false) }

    // Settings state
    var detectSize by remember {
        mutableFloatStateOf(prefs.getFloat("detectSize", 0f).let { if (it != 0f) it else 100f })
    }
    var distance by remember {
        mutableFloatStateOf(prefs.getFloat("distance", 0f).let { if (it != 0f) it else 0.5f })
    }
    var step by remember {
        mutableFloatStateOf(prefs.getFloat("step", 0f).let { if (it != 0f) it else 0.5f })
    }
    var radius by remember {
        mutableFloatStateOf(prefs.getFloat("radius", 1f))
    }
    var arcPosition by remember {
        mutableFloatStateOf(prefs.getFloat("arcPosition", 0f))
    }

    // Dialog state
    var dialogState by remember { mutableStateOf<DialogStates?>(null) }
    var globalThing by remember { mutableStateOf<EncapsulatedAppInfoWithFolder?>(null) }
    var shortcuts by remember { mutableStateOf<List<ShortcutInfo>>(emptyList()) }

    // Quick swipe letter indicator
    var quickSwipeLetterVisible by remember { mutableStateOf(false) }
    var quickSwipeLetter by remember { mutableStateOf<Char?>(null) }

    // View reference
    val rainbowViewRef = remember { mutableStateOf<Rainbow?>(null) }

    // Countdown jobs for long press
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    var quickSwipeMonitorJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // Helper functions
    fun saveSettings(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun saveSettings(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun saveCurrentMoveDistance() {
        rainbowViewRef.value?.let { rainbow ->
            if (onlyFavorites) {
                viewModel.modeDistanceAccumulatedFavorites = rainbow.moveDistancedAccumulated
            } else {
                viewModel.modeDistanceAccumulated = rainbow.moveDistancedAccumulated
            }
        }
    }

    fun updateMenu(favoritesOnly: Boolean = onlyFavorites) {
        if (appsList.isEmpty()) return
        viewModel.updateRainbowFiltered(favoritesOnly)
        viewModel.updateRainbowAll()

        rainbowViewRef.value?.let { rainbow ->
            rainbow.inFolder = false
            rainbow.onlyfavorites = favoritesOnly
            rainbow.moveDistancedAccumulated = if (favoritesOnly) {
                viewModel.modeDistanceAccumulatedFavorites
            } else {
                viewModel.modeDistanceAccumulated
            }
            rainbow.setAppInfoList(viewModel.rainbowFiltered)
        }
    }

    fun toggleFavorites() {
        rainbowViewRef.value?.let { rainbow ->
            if (!rainbow.inFolder) {
                if (!onlyFavorites && appsList.none { it.favorite } && rainbowMape.none { it.favorite }) {
                    Toast.makeText(context, "No favorites", Toast.LENGTH_SHORT).show()
                    return
                }

                saveCurrentMoveDistance()
                val newValue = !onlyFavorites
                onlyFavorites = newValue
                rainbow.onlyfavorites = newValue
                rainbow.moveDistancedAccumulated = if (newValue) {
                    viewModel.modeDistanceAccumulatedFavorites
                } else {
                    viewModel.modeDistanceAccumulated
                }
                saveSettings("onlyfavorites", newValue)
            }
            updateMenu(onlyFavorites)
        }
    }

    fun startFlingAnimation() {
        flingJob?.cancel()
        flingJob = scope.launch {
            while (isActive && rainbowViewRef.value?.flingOn == true) {
                delay(10) // ~100fps
                withContext(Dispatchers.Main) {
                    rainbowViewRef.value?.flingUpdate()
                }
            }
            withContext(Dispatchers.Main) {
                rainbowViewRef.value?.invalidate()
            }
        }
    }

    fun startCountdown(counter: Int) {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            delay(250)
            withContext(Dispatchers.Main) {
                rainbowViewRef.value?.let { rainbow ->
                    // Toggle sliders on long press in empty area (counter == 3)
                    if (counter == 3) {
                        if (!slidersVisible) {
                            slidersVisible = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        return@withContext
                    }

                    val appIndex = rainbow.getAppIndexImIn()
                    if (appIndex != null) {
                        val thing = viewModel.rainbowFiltered[appIndex]
                        globalThing = thing

                        if (thing.folderName == null) {
                            // It's an app - show shortcuts
                            val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                            shortcuts = if (launcherApps.hasShortcutHostPermission()) {
                                mainActivity.getShortcutFromPackage(thing.apps.first().packageName)
                            } else {
                                emptyList()
                            }

                            val appDrawable = icons[thing.apps.first().packageName]
                            val actions = shortcuts.map {
                                ShortcutAction(it.shortLabel.toString(), appDrawable)
                            }.toMutableList().apply {
                                add(ShortcutAction(context.getString(R.string.app_info), getDrawable(context, R.drawable.ic_outline_info_75)))
                                add(if (thing.apps.first().favorite) {
                                    ShortcutAction(context.getString(R.string.remove_from_favorites), getDrawable(context, R.drawable.star_fill))
                                } else {
                                    ShortcutAction(context.getString(R.string.add_to_favorites), getDrawable(context, R.drawable.star_empty))
                                })
                                add(if (mainActivity.isAppAlreadyInMap(thing.apps.first())) {
                                    ShortcutAction(context.getString(R.string.remove_from_folder), getDrawable(context, R.drawable.baseline_folder_off_24))
                                } else {
                                    ShortcutAction(context.getString(R.string.add_to_folder), getDrawable(context, R.drawable.ic_baseline_create_new_folder_50))
                                })
                            }

                            if (shortcuts.isEmpty()) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }

                            dialogState = DialogStates.APP_SHORTCUTS
                            mainActivity.showDialogWithActions(
                                actions,
                                object : OnShortcutClick {
                                    override fun onShortcutClick(index: Int) {
                                        handleShortcutClick(index, thing, shortcuts, mainActivity, viewModel, context,
                                            onRefresh = { updateMenu() },
                                            onDismiss = { dialogState = null }
                                        )
                                    }
                                },
                                rainbow
                            )
                        } else {
                            // It's a folder - show folder options
                            dialogState = DialogStates.FOLDER_OPTIONS
                            mainActivity.showDialogWithActions(
                                getFolderActions(thing, context),
                                object : OnShortcutClick {
                                    override fun onShortcutClick(index: Int) {
                                        handleFolderOptions(index, thing, mainActivity, viewModel, rainbow,
                                            onRefresh = { updateMenu() },
                                            onDismiss = { dialogState = null }
                                        )
                                    }
                                },
                                rainbow
                            )
                        }
                        rainbow.clickIgnored = true
                    }
                }
            }
        }
    }

    fun openFolder(index: Int) {
        val folder = viewModel.rainbowFiltered[index]
        viewModel.setRainbowFilteredValues(
            folder.apps.map {
                EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)
            }.toMutableList()
        )

        rainbowViewRef.value?.let { rainbow ->
            rainbow.inFolder = true
            inFolder = true
            saveCurrentMoveDistance()
            rainbow.moveDistancedAccumulated = 0
            rainbow.setAppInfoList(viewModel.rainbowFiltered)
            rainbow.invalidate()
        }
    }

    fun touched(appIndex: Int) {
        val item = viewModel.rainbowFiltered[appIndex]
        if (item.folderName == null) {
            // Launch app
            val launchIntent = context.packageManager.getLaunchIntentForPackage(item.apps.first().packageName)
            if (launchIntent != null) {
                saveCurrentMoveDistance()
                context.startActivity(launchIntent)
            }
        } else {
            // Open folder
            openFolder(appIndex)
        }
    }

    // Start quick swipe letter monitor
    LaunchedEffect(Unit) {
        quickSwipeMonitorJob?.cancel()
        quickSwipeMonitorJob = launch {
            while (isActive) {
                delay(16) // ~60fps
                rainbowViewRef.value?.let { rainbow ->
                    if (rainbow.quickSwipeEntered && rainbow.currentQuickSwipeLetter != null) {
                        quickSwipeLetter = rainbow.currentQuickSwipeLetter
                        quickSwipeLetterVisible = true
                    } else {
                        quickSwipeLetterVisible = false
                    }
                }
            }
        }
    }

    // Update view when data changes
    LaunchedEffect(appsList, icons, rainbowFiltered) {
        rainbowViewRef.value?.let { rainbow ->
            if (icons.isNotEmpty()) {
                rainbow.icons = icons.toMutableMap()
            }
            if (!rainbow.inFolder) {
                updateMenu(onlyFavorites)
            }
            rainbow.invalidate()
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            countdownJob?.cancel()
            flingJob?.cancel()
            quickSwipeMonitorJob?.cancel()
        }
    }

    // Main UI
    Box(modifier = modifier.fillMaxSize()) {
        // Top touch area for settings long press
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .zIndex(if (slidersVisible) 0f else 1f)
        ) {
            // Touch handling is done in AndroidView
        }

        // Quick swipe letter indicator
        if (quickSwipeLetterVisible && quickSwipeLetter != null) {
            Text(
                text = quickSwipeLetter.toString(),
                fontSize = 72.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .zIndex(20f)
            )
        }

        // Rainbow custom view
        AndroidView(
            factory = { ctx ->
                Rainbow(ctx).apply {
                    leftOrRight = rightSide
                    overrideDetectSize = if (detectSize != 0f) detectSize else null
                    overrideDistance = if (distance != 0f) distance else null
                    overrideStep = if (step != 0f) step else null
                    overrideRadius = radius
                    overrideArcPosition = arcPosition

                    setEventListener(object : IMyEventListener {
                        override fun onEventOccurred(app: EventTypes, counter: Int) {
                            when (app) {
                                EventTypes.OPEN_APP -> touched(counter)
                                EventTypes.START_COUNTDOWN -> startCountdown(counter)
                                EventTypes.STOP_COUNTDOWN -> countdownJob?.cancel()
                                EventTypes.TOGGLE_FAVORITES -> toggleFavorites()
                                EventTypes.START_FLING -> startFlingAnimation()
                                else -> {}
                            }
                        }
                    })

                    rainbowViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Settings overlay
        if (slidersVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(16.dp)
                    .zIndex(10f)
            ) {
                // Close button
                IconButton(
                    onClick = { slidersVisible = false },
                    modifier = Modifier
                        .align(Alignment.End)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_close_50),
                        contentDescription = stringResource(R.string.close)
                    )
                }

                // Detect Size slider
                Text("Detect Size")
                Slider(
                    value = detectSize,
                    onValueChange = {
                        detectSize = it
                        rainbowViewRef.value?.overrideDetectSize = it
                        rainbowViewRef.value?.invalidate()
                        saveSettings("detectSize", it)
                    },
                    valueRange = 0f..200f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Distance slider
                Text("Distance")
                Slider(
                    value = distance,
                    onValueChange = {
                        distance = it
                        rainbowViewRef.value?.overrideDistance = it
                        rainbowViewRef.value?.invalidate()
                        saveSettings("distance", it)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Step slider
                Text(stringResource(R.string.slider_app_spacing))
                Slider(
                    value = step,
                    onValueChange = {
                        step = it
                        rainbowViewRef.value?.overrideStep = it
                        rainbowViewRef.value?.invalidate()
                        saveSettings("step", it)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Radius slider
                Text(stringResource(R.string.slider_circle_size))
                Slider(
                    value = radius,
                    onValueChange = {
                        radius = it
                        rainbowViewRef.value?.overrideRadius = it
                        rainbowViewRef.value?.invalidate()
                        saveSettings("radius", it)
                    },
                    valueRange = 0.5f..3f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Arc Position slider
                Text(stringResource(R.string.slider_arc_position))
                Slider(
                    value = arcPosition,
                    onValueChange = {
                        arcPosition = it
                        rainbowViewRef.value?.overrideArcPosition = it
                        rainbowViewRef.value?.invalidate()
                        saveSettings("arcPosition", it)
                    },
                    valueRange = -1.57f..1.57f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Only favorites checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = onlyFavorites,
                        onCheckedChange = { toggleFavorites() }
                    )
                    Text(stringResource(R.string.show_only_favorites))
                }
            }
        }
    }

    // Back button handler
    androidx.activity.compose.BackHandler(enabled = inFolder) {
        saveCurrentMoveDistance()
        inFolder = false
        updateMenu(onlyFavorites)
    }
}

// Helper function to get drawable with proper tinting
private fun getDrawable(context: Context, resourceId: Int): Drawable? {
    return ResourcesCompat.getDrawable(context.resources, resourceId, null)?.apply {
        val nightMode = context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        setTint(if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.BLACK
        })
    }
}

private fun getFolderActions(thing: EncapsulatedAppInfoWithFolder, context: Context): List<ShortcutAction> {
    return listOf(
        ShortcutAction(context.getString(R.string.rename_folder), getDrawable(context, R.drawable.ic_baseline_drive_file_rename_outline_24)),
        ShortcutAction(context.getString(R.string.delete_folder), getDrawable(context, R.drawable.ic_baseline_delete_24)),
        if (thing.favorite == true) {
            ShortcutAction(context.getString(R.string.remove_from_favorites), getDrawable(context, R.drawable.star_fill))
        } else {
            ShortcutAction(context.getString(R.string.add_to_favorites), getDrawable(context, R.drawable.star_empty))
        }
    )
}

private fun handleFolderOptions(
    index: Int,
    thing: EncapsulatedAppInfoWithFolder,
    mainActivity: MainActivityCompose,
    viewModel: ViewModel,
    rainbowView: Rainbow?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    mainActivity.shortcutPopup?.dismiss()
    when (index) {
        0 -> {
            // Rename folder
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == thing.folderName }
            mapa?.let { folder ->
                rainbowView?.let { view ->
                    mainActivity.openFolderNameMenu(view, true, folder.folderName, false) { newName ->
                        val updatedFolder = folder.copy(folderName = newName)
                        viewModel.updateRainbowMapa(updatedFolder)
                        mainActivity.rainbowMapaUpdateItem(updatedFolder)
                        onRefresh()
                    }
                }
            }
        }
        1 -> {
            // Delete folder
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == thing.folderName }
            mapa?.let { folder ->
                viewModel.deleteRainbowMapa(folder)
                mainActivity.rainbowMapaDeleteItem(folder)
                onRefresh()
            }
        }
        2 -> {
            // Toggle favorite
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == thing.folderName }
            mapa?.let { folder ->
                val updatedFolder = folder.copy(favorite = !folder.favorite)
                viewModel.updateRainbowMapa(updatedFolder)
                mainActivity.rainbowMapaUpdateItem(updatedFolder)
                onRefresh()
            }
        }
    }
    onDismiss()
}

private fun handleShortcutClick(
    index: Int,
    thing: EncapsulatedAppInfoWithFolder,
    shortcuts: List<ShortcutInfo>,
    mainActivity: MainActivityCompose,
    viewModel: ViewModel,
    context: Context,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    if (index >= shortcuts.size) {
        // Handle extra actions (app info, favorites, folder)
        when (index - shortcuts.size) {
            0 -> {
                // App info
                val intent = android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", thing.apps.first().packageName, null)
                }
                context.startActivity(intent)
                mainActivity.shortcutPopup?.dismiss()
            }
            1 -> {
                // Toggle favorite
                val app = thing.apps.first()
                app.favorite = !app.favorite
                mainActivity.saveAppInfo(app)
                onRefresh()
                mainActivity.shortcutPopup?.dismiss()
            }
            2 -> {
                // Add to/remove from folder
                if (mainActivity.isAppAlreadyInMap(thing.apps.first())) {
                    // Remove from folder
                    val mapa = viewModel.rainbowMape.value?.find { it.apps.contains(thing.apps.first()) }
                    mapa?.let { folder ->
                        val updatedFolder = folder.copy(apps = folder.apps.minus(thing.apps.first()).toMutableList())
                        viewModel.updateRainbowMapa(updatedFolder)
                        mainActivity.rainbowMapaUpdateItem(updatedFolder)
                        onRefresh()
                    }
                    mainActivity.shortcutPopup?.dismiss()
                } else {
                    // Show folder selection dialog
                    onDismiss()
                    // We need a View to show the dialog - use a temporary view from context
                    val tempView = android.view.View(context)
                    mainActivity.showDialogWithActions(
                        viewModel.rainbowMape.value!!.map {
                            ShortcutAction(it.folderName, getDrawable(context, R.drawable.baseline_folder_24))
                        }.toMutableList().apply {
                            add(ShortcutAction("Nova mapa", getDrawable(context, R.drawable.ic_baseline_create_new_folder_50)))
                        },
                        object : OnShortcutClick {
                            override fun onShortcutClick(mapIndex: Int) {
                                handleAddToFolder(mapIndex, thing, viewModel, mainActivity, context, tempView, onRefresh)
                            }
                        },
                        tempView
                    )
                }
            }
        }
    } else {
        // Launch shortcut
        mainActivity.shortcutPopup?.dismiss()
        val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcherApps.startShortcut(
                shortcuts[index].`package`,
                shortcuts[index].id,
                null,
                null,
                android.os.Process.myUserHandle()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    onDismiss()
}

private fun handleAddToFolder(
    mapIndex: Int,
    thing: EncapsulatedAppInfoWithFolder,
    viewModel: ViewModel,
    mainActivity: MainActivityCompose,
    context: Context,
    view: android.view.View,
    onRefresh: () -> Unit
) {
    mainActivity.shortcutPopup?.dismiss()
    if (mapIndex >= viewModel.rainbowMape.value!!.size) {
        // Create new folder
        mainActivity.openFolderNameMenu(view, false, "", false) { folderName ->
            val newFolder = RainbowMapa(0, folderName, mutableListOf(thing.apps.first()), false)
            mainActivity.rainbowMapaInsertItem(newFolder)
            onRefresh()
        }
    } else {
        // Add to existing folder
        val folder = viewModel.rainbowMape.value!![mapIndex]
        val updatedFolder = folder.copy(apps = folder.apps.plus(thing.apps.first()).toMutableList())
        viewModel.updateRainbowMapa(updatedFolder)
        mainActivity.rainbowMapaUpdateItem(updatedFolder)
        onRefresh()
    }
}
