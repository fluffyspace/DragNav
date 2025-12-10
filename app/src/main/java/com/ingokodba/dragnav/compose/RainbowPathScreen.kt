package com.ingokodba.dragnav.compose

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragnav.R
import com.google.gson.Gson
import androidx.appcompat.app.AppCompatActivity
import com.ingokodba.dragnav.*
import com.ingokodba.dragnav.DisplayMode
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.RainbowMapa
import com.ingokodba.dragnav.rainbow.PathConfig
import com.ingokodba.dragnav.rainbow.PathSettingsDialog
import com.ingokodba.dragnav.rainbow.PathSettingsDialogCompose
import com.ingokodba.dragnav.rainbow.RainbowPathView
import com.ingokodba.dragnav.rainbow.SearchOverlayMaterialView
import kotlinx.coroutines.*

/**
 * Data class representing a notification
 */
data class AppNotification(
    val packageName: String,
    val appIcon: Drawable?,
    val title: String,
    val content: String,
    val contentIntent: android.app.PendingIntent? = null
)

/**
 * Composable function to display app icons for apps with unread notifications
 * Shows a vertical list of icons only, positioned according to PathConfig
 */
@Composable
fun NotificationIconsList(
    notifications: List<AppNotification>,
    config: PathConfig,
    modifier: Modifier = Modifier,
    onIconClick: (AppNotification) -> Unit = {}
) {
    // Group notifications by package name to show unique app icons
    val uniqueAppNotifications = notifications
        .distinctBy { it.packageName }
        .filter { it.appIcon != null }

    if (uniqueAppNotifications.isEmpty()) return

    // Map anchor to bias values (0.0 = left/top, 0.5 = center, 1.0 = right/bottom)
    val (anchorBiasX, anchorBiasY) = when (config.notificationAnchor) {
        com.ingokodba.dragnav.rainbow.NotificationAnchor.TOP_LEFT -> Pair(0f, 0f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.TOP_CENTER -> Pair(0.5f, 0f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.TOP_RIGHT -> Pair(1f, 0f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.CENTER_LEFT -> Pair(0f, 0.5f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.CENTER -> Pair(0.5f, 0.5f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.CENTER_RIGHT -> Pair(1f, 0.5f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.BOTTOM_LEFT -> Pair(0f, 1f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.BOTTOM_CENTER -> Pair(0.5f, 1f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.BOTTOM_RIGHT -> Pair(1f, 1f)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // Where to position the anchor point (absolute screen coordinates in pixels)
        val targetXPx = config.notificationOffsetX * screenWidthPx
        val targetYPx = config.notificationOffsetY * screenHeightPx

        Column(
            modifier = Modifier
                .wrapContentSize()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    // Calculate position: target position minus anchor offset
                    val x = (targetXPx - placeable.width * anchorBiasX).toInt()
                    val y = (targetYPx - placeable.height * anchorBiasY).toInt()

                    // Logging for debugging
                    Log.d("NotificationIconsList", "=== NOTIFICATION POSITIONING DEBUG ===")
                    Log.d("NotificationIconsList", "Anchor: ${config.notificationAnchor.name}")
                    Log.d("NotificationIconsList", "Anchor bias: ($anchorBiasX, $anchorBiasY)")
                    Log.d("NotificationIconsList", "Screen dimensions: ${screenWidthPx}px x ${screenHeightPx}px")
                    Log.d("NotificationIconsList", "Offset percentages: X=${config.notificationOffsetX}, Y=${config.notificationOffsetY}")
                    Log.d("NotificationIconsList", "Target position (anchor point): X=${targetXPx}px, Y=${targetYPx}px")
                    Log.d("NotificationIconsList", "Column dimensions: ${placeable.width}px x ${placeable.height}px")
                    Log.d("NotificationIconsList", "Calculated Column position: X=${x}px, Y=${y}px")
                    Log.d("NotificationIconsList", "Number of notifications: ${uniqueAppNotifications.size}")
                    Log.d("NotificationIconsList", "=====================================")

                    layout(placeable.width, placeable.height) {
                        placeable.place(x, y)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(config.notificationIconSpacing.dp)
        ) {
            uniqueAppNotifications.forEach { notification ->
                notification.appIcon?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap(
                            width = (config.notificationIconSize.toInt() * 2),
                            height = (config.notificationIconSize.toInt() * 2)
                        ).asImageBitmap(),
                        contentDescription = "App icon for ${notification.packageName}",
                        modifier = Modifier
                            .size(config.notificationIconSize.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onIconClick(notification) }
                    )
                }
            }
        }
    }
}

/**
 * Composable function to display app notifications in a list
 */
@Composable
fun NotificationsList(
    notifications: List<AppNotification>,
    modifier: Modifier = Modifier,
    onNotificationClick: (AppNotification) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications) { notification ->
            NotificationItem(
                notification = notification,
                onClick = { onNotificationClick(notification) }
            )
        }
    }
}

/**
 * Individual notification item
 */
@Composable
fun NotificationItem(
    notification: AppNotification,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                Log.d("NotificationItem", "Notification item clicked: ${notification.packageName} - ${notification.title}")
                onClick()
            }
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
            .border(
                width = 1.dp,
                color = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        notification.appIcon?.let { drawable ->
            Image(
                bitmap = drawable.toBitmap(64, 64).asImageBitmap(),
                contentDescription = "App icon",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Notification title and content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = notification.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.content,
                fontSize = 14.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Jetpack Compose screen version of MainFragmentRainbowPath
 * Wraps RainbowPathView and SearchOverlayMaterialView using AndroidView
 */
@Composable
fun RainbowPathScreen(
    mainActivity: MainActivityCompose,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Logging tag
    val TAG = "RainbowPathScreen"

    // Track recompositions
    SideEffect {
        Log.d(TAG, ">>> RECOMPOSITION TRIGGERED <<<")
    }

    // Helper functions to call methods directly on MainActivityCompose
    fun callSaveAppInfo(app: AppInfo) {
        mainActivity.saveAppInfo(app)
    }

    fun callIsAppAlreadyInMap(app: AppInfo): Boolean {
        return mainActivity.isAppAlreadyInMap(app)
    }

    fun callShowDialogWithActions(actions: List<ShortcutAction>, handler: OnShortcutClick, view: View) {
        mainActivity.showDialogWithActions(actions, handler, view)
    }

    fun callRainbowMapaUpdateItem(mapa: RainbowMapa) {
        mainActivity.rainbowMapaUpdateItem(mapa)
    }

    fun callRainbowMapaDeleteItem(mapa: RainbowMapa) {
        mainActivity.rainbowMapaDeleteItem(mapa)
    }

    fun callOpenFolderNameMenu(view: View, editing: Boolean, name: String, showColor: Boolean, callback: (String) -> Unit) {
        mainActivity.openFolderNameMenu(view, editing, name, showColor, callback)
    }

    fun callRainbowMapaInsertItem(mapa: RainbowMapa) {
        mainActivity.rainbowMapaInsertItem(mapa)
    }

    fun callGetShortcutFromPackage(packageName: String): List<ShortcutInfo> {
        return mainActivity.getShortcutFromPackage(packageName)
    }

    fun dismissShortcutPopup() {
        mainActivity.shortcutPopup?.dismiss()
    }

    // State management
    var config by remember { mutableStateOf(loadConfig(context)) }
    var inFolder by remember { mutableStateOf(false) }
    var currentAppIndex by remember { mutableStateOf<Int?>(null) }
    var shortcuts by remember { mutableStateOf<List<ShortcutInfo>>(emptyList()) }
    var globalThing by remember { mutableStateOf<EncapsulatedAppInfoWithFolder?>(null) }
    var dialogState by remember { mutableStateOf<DialogStates?>(null) }
    var showPathSettings by remember { mutableStateOf(false) }

    // Coroutine jobs - use plain variables, not state
    var flingJob by remember { mutableStateOf<Job?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    var settingsCountdownJob by remember { mutableStateOf<Job?>(null) }

    // View references - use plain remember, not state (to avoid recomposition on ref updates)
    val pathViewRef = remember { mutableStateOf<RainbowPathView?>(null) }
    val searchOverlayRef = remember { mutableStateOf<SearchOverlayMaterialView?>(null) }
    val topTouchAreaRef = remember { mutableStateOf<View?>(null) }

    // SharedPreferences
    val prefs = remember {
        context.getSharedPreferences("rainbow_path_config", Context.MODE_PRIVATE)
    }
    var onlyFavorites by remember {
        mutableStateOf(prefs.getBoolean("only_favorites", false))
    }

    // Observe ViewModel using Flow-based state
    val appsList by viewModel.appsListFlow.collectAsStateWithLifecycle()
    val rainbowMape by viewModel.rainbowMapeFlow.collectAsStateWithLifecycle()
    val icons by viewModel.iconsFlow.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsFlow.collectAsStateWithLifecycle()
    val rainbowFilteredFlow by viewModel.rainbowFilteredFlow.collectAsStateWithLifecycle()
    val rainbowAllFlow by viewModel.rainbowAllFlow.collectAsStateWithLifecycle()

    // Log notifications for debugging
    LaunchedEffect(notifications) {
        Log.d(TAG, "=== NOTIFICATIONS UPDATE ===")
        Log.d(TAG, "Notifications count: ${notifications.size}")
        notifications.forEachIndexed { index, notification ->
            Log.d(TAG, "[$index] Package: ${notification.packageName}, Title: ${notification.title}, Content: ${notification.content}")
        }
        Log.d(TAG, "=========================")
    }

    // Log state changes
    LaunchedEffect(appsList) {
        Log.d(TAG, "appsList changed: ${appsList?.size} apps")
    }
    LaunchedEffect(rainbowMape) {
        Log.d(TAG, "rainbowMape changed: ${rainbowMape?.size} folders")
    }
    LaunchedEffect(icons) {
        Log.d(TAG, "icons changed: ${icons?.size} icons")
    }
    LaunchedEffect(inFolder) {
        Log.d(TAG, "inFolder changed: $inFolder")
    }
    LaunchedEffect(onlyFavorites) {
        Log.d(TAG, "onlyFavorites changed: $onlyFavorites")
    }

    // Update display mode when onlyFavorites changes
    LaunchedEffect(onlyFavorites) {
        Log.d(TAG, "Setting display mode to ${if (onlyFavorites) "FAVORITES_ONLY" else "ALL_APPS"}")
        viewModel.setDisplayMode(
            if (onlyFavorites) DisplayMode.FAVORITES_ONLY else DisplayMode.ALL_APPS
        )
    }

    // Update apps when data changes - use flow-based data
    // Only update when NOT in folder - folder apps are set manually
    LaunchedEffect(rainbowFilteredFlow, rainbowAllFlow, inFolder, icons) {
        if (!inFolder) {
            Log.d(TAG, "LaunchedEffect(update apps) triggered - using flow data")
            pathViewRef.value?.let { pathView ->
                val startTime = System.currentTimeMillis()
                updateAppsFromFlows(
                    pathView = pathView,
                    rainbowFiltered = rainbowFilteredFlow,
                    rainbowAll = rainbowAllFlow,
                    inFolder = inFolder,
                    icons = icons
                )
                pathView.invalidate()
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "updateApps + invalidate took ${elapsed}ms")
            }
        } else {
            Log.d(TAG, "Skipping flow-based update while in folder")
        }
    }

    // Update search overlay icons separately
    LaunchedEffect(icons) {
        icons?.let { iconMap ->
            searchOverlayRef.value?.setIcons(iconMap)
            Log.d(TAG, "Updated search overlay icons")
        }
    }
    
    // Lifecycle handling for pause/resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    pathViewRef.value?.saveScrollPosition()
                    flingJob?.cancel()
                    countdownJob?.cancel()
                    settingsCountdownJob?.cancel()
                    Log.d("RainbowPathScreen", "onPause - scroll position saved")
                }
                Lifecycle.Event.ON_DESTROY -> {
                    flingJob?.cancel()
                    countdownJob?.cancel()
                    settingsCountdownJob?.cancel()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Helper functions - use remember to avoid recreating on every recomposition
    val getDisplayedApps = remember(rainbowFilteredFlow) {
        {
            rainbowFilteredFlow
        }
    }

    val updateAppsCallback = remember(rainbowFilteredFlow, rainbowAllFlow, inFolder, icons) {
        {
            pathViewRef.value?.let { pathView ->
                updateAppsFromFlows(
                    pathView = pathView,
                    rainbowFiltered = rainbowFilteredFlow,
                    rainbowAll = rainbowAllFlow,
                    inFolder = inFolder,
                    icons = icons
                )
            }
        }
    }
    
    fun launchApp(appIndex: Int) {
        val apps = getDisplayedApps()
        if (appIndex < 0 || appIndex >= apps.size) return

        val thing = apps[appIndex]
        if (thing.folderName != null) return // Should not happen, but safety check

        val app = thing.apps.first()
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            countdownJob?.cancel()
            pathViewRef.value?.saveScrollPosition()
            context.startActivity(launchIntent)
        }
    }

    fun openFolder(appIndex: Int) {
        countdownJob?.cancel()

        val apps = getDisplayedApps()
        if (appIndex < 0 || appIndex >= apps.size) return

        val thing = apps[appIndex]
        if (thing.folderName == null) return

        pathViewRef.value?.saveScrollPosition()

        val folderApps = thing.apps
        Log.d("RainbowPathScreen", "Opening folder with ${folderApps.size} apps: ${folderApps.map { it.label }}")

        inFolder = true
        pathViewRef.value?.inFolder = true

        val folderEncapsulated = folderApps.map {
            EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)
        }
        Log.d("RainbowPathScreen", "Setting folder apps with ${folderEncapsulated.size} items")

        // When in folder, directly set the apps instead of using flow
        pathViewRef.value?.setApps(folderEncapsulated)
        pathViewRef.value?.resetFolderScroll()
        pathViewRef.value?.invalidate()
    }
    
    // Create OnShortcutClick handler - must be defined before showShortcuts uses it
    // But it references functions defined later, so we'll use a lazy approach
    var shortcutClickHandlerRef by remember { mutableStateOf<OnShortcutClick?>(null) }
    
    fun showShortcuts(appIndex: Int) {
        countdownJob?.cancel()
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                val apps = getDisplayedApps()
                if (appIndex < 0 || appIndex >= apps.size) return@withContext
                
                currentAppIndex = appIndex
                val thing = apps[appIndex]
                globalThing = thing
                
                val handler = shortcutClickHandlerRef ?: return@withContext
                
                if (thing.folderName == null) {
                    openShortcutsMenu(thing, mainActivity, viewModel, icons, context, pathViewRef.value, shortcuts, handler) { newState, newShortcuts ->
                        dialogState = newState
                        shortcuts = newShortcuts
                    }
                } else {
                    val actions = listOf(
                        ShortcutAction(
                            getTranslatedString(R.string.rename_folder, context),
                            getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24, context)
                        ),
                        ShortcutAction(
                            getTranslatedString(R.string.delete_folder, context),
                            getDrawable(R.drawable.ic_baseline_delete_24, context)
                        ),
                        if (thing.favorite == true)
                            ShortcutAction(
                                getTranslatedString(R.string.remove_from_favorites, context),
                                getDrawable(R.drawable.star_fill, context)
                            )
                        else
                            ShortcutAction(
                                getTranslatedString(R.string.add_to_favorites, context),
                                getDrawable(R.drawable.star_empty, context)
                            )
                    )
                    dialogState = DialogStates.FOLDER_OPTIONS
                    callShowDialogWithActions(actions, handler, pathViewRef.value ?: return@withContext)
                }

                pathViewRef.value?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
    
    fun openShortcut(index: Int) {
        val thing = globalThing ?: return
        
        if (thing.folderName == null) {
            if (index >= shortcuts.size) {
                if (index == shortcuts.size) {
                    // App info
                    val app = thing.apps.first()
                    val intent = android.content.Intent()
                    intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = android.net.Uri.fromParts("package", app.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                    dismissShortcutPopup()
                } else if (index == shortcuts.size + 1) {
                    // Toggle favorite
                    val app = thing.apps.first()
                    val newFavoriteValue = !app.favorite

                    Log.d("RainbowPathScreen", "=== TOGGLING FAVORITE ===")
                    Log.d("RainbowPathScreen", "App: ${app.label} (${app.packageName})")
                    Log.d("RainbowPathScreen", "Old favorite value: ${app.favorite}")
                    Log.d("RainbowPathScreen", "New favorite value: $newFavoriteValue")

                    // Find the app in ViewModel's appsList and update it
                    val appInList = appsList?.find { it.packageName == app.packageName }
                    if (appInList != null) {
                        Log.d("RainbowPathScreen", "Found app in appsList, updating favorite to $newFavoriteValue")
                        appInList.favorite = newFavoriteValue
                        // Also update the local copy
                        app.favorite = newFavoriteValue
                    } else {
                        Log.e("RainbowPathScreen", "App NOT FOUND in appsList! This is a bug.")
                        // Still update local copy and database
                        app.favorite = newFavoriteValue
                    }

                    callSaveAppInfo(app)
                    // Flow will automatically update UI when database changes
                    dismissShortcutPopup()
                } else if (index == shortcuts.size + 2) {
                    // Add/remove from folder
                    val isInMap = callIsAppAlreadyInMap(thing.apps.first())
                    if (isInMap) {
                        // Remove from folder
                        val mapa = rainbowMape?.firstOrNull { rainbowMapa -> rainbowMapa.apps.contains(thing.apps.first()) }
                        if (mapa != null) {
                            val azurirana_mapa = mapa.copy(apps = mapa.apps.minus(thing.apps.first()).toMutableList())
                            viewModel.updateRainbowMapa(azurirana_mapa)
                            callRainbowMapaUpdateItem(azurirana_mapa)
                            // Flow will automatically update UI when database changes
                        }
                        dismissShortcutPopup()
                    } else {
                        // Show folder selection dialog
                        dialogState = DialogStates.ADDING_TO_FOLDER
                        shortcutClickHandlerRef?.let { handler ->
                            callShowDialogWithActions(
                                rainbowMape?.map {
                                    ShortcutAction(it.folderName, getDrawable(R.drawable.baseline_folder_24, context))
                                }?.toMutableList()?.apply {
                                    add(ShortcutAction("Nova mapa", getDrawable(R.drawable.ic_baseline_create_new_folder_50, context)))
                                } ?: mutableListOf(),
                                handler,
                                pathViewRef.value ?: return
                            )
                        }
                    }
                }
                return
            }

            dismissShortcutPopup()
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
                Log.e("RainbowPathScreen", "Failed to start shortcut", e)
            }
        }
    }
    
    fun addAppToMap(mapIndex: Int) {
        dismissShortcutPopup()
        if (mapIndex >= (rainbowMape.size)) {
            openCreateFolderDialog(globalThing, mainActivity, pathViewRef.value)
        } else {
            val mapa = rainbowMape[mapIndex]
            val nova_mapa = mapa.copy(apps = mapa.apps.plus(globalThing!!.apps.first()).toMutableList())
            viewModel.updateRainbowMapa(nova_mapa)
            callRainbowMapaUpdateItem(nova_mapa)
            // Flow will automatically update UI when database changes
        }
    }

    fun folderOptions(index: Int) {
        dismissShortcutPopup()
        when (index) {
            0 -> {
                // Rename folder
                val mapa = rainbowMape.find { it.folderName == globalThing!!.folderName }
                callOpenFolderNameMenu(pathViewRef.value ?: return, true, mapa!!.folderName, false) { ime ->
                    val nova_mapa = mapa.copy(folderName = ime)
                    viewModel.updateRainbowMapa(nova_mapa)
                    callRainbowMapaUpdateItem(nova_mapa)
                    // Flow will automatically update UI when database changes
                }
            }
            1 -> {
                // Delete folder
                val mapa = rainbowMape.find { it.folderName == globalThing!!.folderName }
                if (mapa != null) {
                    viewModel.deleteRainbowMapa(mapa)
                    callRainbowMapaDeleteItem(mapa)
                    // Flow will automatically update UI when database changes
                }
            }
            2 -> {
                // Toggle favorite
                val mapa = rainbowMape.find { it.folderName == globalThing!!.folderName }
                val nova_mapa = mapa?.copy(favorite = !mapa.favorite)
                if (nova_mapa != null) {
                    viewModel.updateRainbowMapa(nova_mapa)
                    callRainbowMapaUpdateItem(nova_mapa)
                    // Flow will automatically update UI when database changes
                }
            }
        }
    }

    fun toggleFavorites() {
        if (inFolder) return

        onlyFavorites = !onlyFavorites
        prefs.edit().putBoolean("only_favorites", onlyFavorites).apply()
        pathViewRef.value?.onlyFavorites = onlyFavorites
        // Flow will automatically update UI when display mode changes (via LaunchedEffect)
    }

    fun goToHome() {
        if (inFolder) {
            pathViewRef.value?.restoreScrollPosition()
        }

        inFolder = false
        pathViewRef.value?.inFolder = false

        // When exiting folder, update with flow data
        pathViewRef.value?.let { pathView ->
            updateAppsFromFlows(
                pathView = pathView,
                rainbowFiltered = rainbowFilteredFlow,
                rainbowAll = rainbowAllFlow,
                inFolder = false,
                icons = icons
            )
            pathView.invalidate()
        }
    }
    
    // Create OnShortcutClick handler that delegates to screen functions
    // Defined after all functions it references (openShortcut, addAppToMap, folderOptions)
    // Initialize the handler reference now that all functions are defined
    LaunchedEffect(Unit) {
        shortcutClickHandlerRef = object : OnShortcutClick {
            override fun onShortcutClick(index: Int) {
                when (dialogState) {
                    DialogStates.APP_SHORTCUTS -> openShortcut(index)
                    DialogStates.ADDING_TO_FOLDER -> addAppToMap(index)
                    DialogStates.FOLDER_OPTIONS -> folderOptions(index)
                    else -> {}
                }
            }
        }
    }

    fun showSettingsDialog() {
        PathSettingsDialog(
            context,
            config,
            onConfigChanged = { newConfig ->
                config = newConfig
                pathViewRef.value?.config = config
                saveConfig(context, config)
            },
            onCategoryChanged = { category ->
                pathViewRef.value?.showLetterIndexBackground = (category == PathSettingsDialog.Category.LETTERS)
                pathViewRef.value?.invalidate()
            }
        ).show()
    }

    fun showSearchOverlay() {
        // Use the precomputed rainbowAll which includes both apps and folders
        searchOverlayRef.value?.setApps(rainbowAllFlow)
        searchOverlayRef.value?.setIcons(icons)
        searchOverlayRef.value?.setPathConfig(config)

        searchOverlayRef.value?.setListener(object : SearchOverlayMaterialView.SearchOverlayListener {
            override fun onAppClicked(app: EncapsulatedAppInfoWithFolder) {
                searchOverlayRef.value?.hide()
                if (app.folderName == null && app.apps.isNotEmpty()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.apps.first().packageName)
                    if (launchIntent != null) {
                        pathViewRef.value?.saveScrollPosition()
                        context.startActivity(launchIntent)
                    }
                } else if (app.folderName != null) {
                    if (inFolder) {
                        goToHome()
                    }
                    val wasOnlyFavorites = onlyFavorites
                    onlyFavorites = true
                    pathViewRef.value?.onlyFavorites = true
                    // Flow will automatically update when onlyFavorites changes

                    val apps = getDisplayedApps()
                    val folderIndex = apps.indexOfFirst {
                        it.folderName == app.folderName
                    }
                    if (folderIndex >= 0) {
                        openFolder(folderIndex)
                    } else {
                        onlyFavorites = wasOnlyFavorites
                        pathViewRef.value?.onlyFavorites = wasOnlyFavorites
                        // Flow will automatically update when onlyFavorites changes
                        Log.d("RainbowPathScreen", "Folder not found in displayed apps: ${app.folderName}")
                    }
                }
            }

            override fun onAppLongPressed(app: EncapsulatedAppInfoWithFolder) {
                globalThing = app
                val handler = shortcutClickHandlerRef ?: return
                if (app.folderName == null && app.apps.isNotEmpty()) {
                    openShortcutsMenu(app, mainActivity, viewModel, icons, context, pathViewRef.value, shortcuts, handler) { newState, newShortcuts ->
                        dialogState = newState
                        shortcuts = newShortcuts
                    }
                } else if (app.folderName != null) {
                    val actions = listOf(
                        ShortcutAction(
                            getTranslatedString(R.string.rename_folder, context),
                            getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24, context)
                        ),
                        ShortcutAction(
                            getTranslatedString(R.string.delete_folder, context),
                            getDrawable(R.drawable.ic_baseline_delete_24, context)
                        ),
                        if (app.favorite == true)
                            ShortcutAction(
                                getTranslatedString(R.string.remove_from_favorites, context),
                                getDrawable(R.drawable.star_fill, context)
                            )
                        else
                            ShortcutAction(
                                getTranslatedString(R.string.add_to_favorites, context),
                                getDrawable(R.drawable.star_empty, context)
                            )
                    )
                    dialogState = DialogStates.FOLDER_OPTIONS
                    callShowDialogWithActions(actions, handler, searchOverlayRef.value ?: return)
                }
            }

            override fun onDismiss() {
                Log.d(TAG, "SearchOverlay onDismiss called")
                searchOverlayRef.value?.hide()
            }

            override fun onSettingsClick() {
                searchOverlayRef.value?.hide()
                showPathSettings = true
            }

            override fun onPathConfigChanged(newConfig: PathConfig) {
                config = newConfig
                pathViewRef.value?.config = config
                saveConfig(context, config)
            }
        })

        searchOverlayRef.value?.show()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main RainbowPathView (full size, underneath)
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Top touch area for settings
                    val topTouchArea = View(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            (100 * resources.displayMetrics.density).toInt()
                        ).apply {
                            topMargin = 0
                        }
                        isClickable = true
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                    addView(topTouchArea)
                    topTouchAreaRef.value = topTouchArea

                    // RainbowPathView
                    val pathView = RainbowPathView(ctx).apply {
                        id = View.generateViewId()
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            topMargin = (100 * resources.displayMetrics.density).toInt()
                        }

                        // Set initial config
                        this.config = config
                        this.onlyFavorites = onlyFavorites

                        // Set up event listener ONCE in factory
                        setEventListener(object : RainbowPathView.EventListener {
                            override fun onAppClicked(appIndex: Int) {
                                // Use the view's getDisplayedApps() which applies sorting
                                val apps = this@apply.getDisplayedApps()
                                if (appIndex < 0 || appIndex >= apps.size) return

                                val thing = apps[appIndex]
                                if (thing.folderName == null) {
                                    // Launch the app directly here instead of calling launchApp
                                    // to avoid using the Composable's unsorted getDisplayedApps
                                    val app = thing.apps.first()
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) {
                                        countdownJob?.cancel()
                                        this@apply.saveScrollPosition()
                                        context.startActivity(launchIntent)
                                    }
                                } else {
                                    // Open folder - need to pass the thing directly
                                    countdownJob?.cancel()
                                    this@apply.saveScrollPosition()

                                    val folderApps = thing.apps
                                    Log.d("RainbowPathScreen", "Opening folder with ${folderApps.size} apps: ${folderApps.map { it.label }}")

                                    inFolder = true
                                    this@apply.inFolder = true

                                    val folderEncapsulated = folderApps.map {
                                        EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)
                                    }
                                    Log.d("RainbowPathScreen", "Setting folder apps with ${folderEncapsulated.size} items")

                                    // When in folder, directly set the apps instead of using flow
                                    this@apply.setApps(folderEncapsulated)
                                    this@apply.resetFolderScroll()
                                    this@apply.invalidate()
                                }
                            }

                            override fun onAppLongPressed(appIndex: Int) {
                                // Use the view's getDisplayedApps() which applies sorting
                                countdownJob?.cancel()
                                scope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) {
                                        val apps = this@apply.getDisplayedApps()
                                        if (appIndex < 0 || appIndex >= apps.size) return@withContext

                                        currentAppIndex = appIndex
                                        val thing = apps[appIndex]
                                        globalThing = thing

                                        val handler = shortcutClickHandlerRef ?: return@withContext

                                        if (thing.folderName == null) {
                                            openShortcutsMenu(thing, mainActivity, viewModel, icons, context, this@apply, shortcuts, handler) { newState, newShortcuts ->
                                                dialogState = newState
                                                shortcuts = newShortcuts
                                            }
                                        } else {
                                            val actions = listOf(
                                                ShortcutAction(
                                                    getTranslatedString(R.string.rename_folder, context),
                                                    getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24, context)
                                                ),
                                                ShortcutAction(
                                                    getTranslatedString(R.string.delete_folder, context),
                                                    getDrawable(R.drawable.ic_baseline_delete_24, context)
                                                ),
                                                if (thing.favorite == true)
                                                    ShortcutAction(
                                                        getTranslatedString(R.string.remove_from_favorites, context),
                                                        getDrawable(R.drawable.star_fill, context)
                                                    )
                                                else
                                                    ShortcutAction(
                                                        getTranslatedString(R.string.add_to_favorites, context),
                                                        getDrawable(R.drawable.star_empty, context)
                                                    )
                                            )
                                            dialogState = DialogStates.FOLDER_OPTIONS
                                            callShowDialogWithActions(actions, handler, this@apply)
                                        }
                                    }
                                }
                            }

                            override fun onShortcutClicked(shortcutIndex: Int) {
                                openShortcut(shortcutIndex)
                            }

                            override fun onFavoritesToggled() {
                                toggleFavorites()
                            }

                            override fun onBackButtonPressed() {
                                if (searchOverlayRef.value?.visibility == View.VISIBLE) {
                                    searchOverlayRef.value?.hide()
                                } else if (inFolder) {
                                    goToHome()
                                }
                            }

                            override fun onFlingStarted() {
                                flingJob?.cancel()
                                flingJob = scope.launch(Dispatchers.Main) {
                                    while (isActive) {
                                        delay(16) // ~60fps
                                        pathViewRef.value?.flingUpdate()
                                    }
                                }
                            }

                            override fun onFlingEnded() {
                                flingJob?.cancel()
                            }

                            override fun onLongPressStart(appIndex: Int) {
                                countdownJob?.cancel()
                                countdownJob = scope.launch {
                                    delay(250)
                                    pathViewRef.value?.triggerLongPress()
                                }
                            }

                            override fun onSearchButtonClicked() {
                                showSearchOverlay()
                            }
                        })
                    }
                    addView(pathView)
                    pathViewRef.value = pathView

                    // Set up top touch area for settings ONCE in factory
                    var touchStartX = 0f
                    var touchStartY = 0f
                    topTouchArea.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                touchStartX = event.x
                                touchStartY = event.y
                                settingsCountdownJob?.cancel()
                                settingsCountdownJob = scope.launch(Dispatchers.IO) {
                                    delay(250)
                                    withContext(Dispatchers.Main) {
                                        showSettingsDialog()
                                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                }
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val moveDistance = kotlin.math.abs(event.x - touchStartX) + kotlin.math.abs(event.y - touchStartY)
                                if (moveDistance > 50) {
                                    settingsCountdownJob?.cancel()
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                settingsCountdownJob?.cancel()
                                true
                            }
                            else -> false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
            // Remove update block entirely - all updates handled by LaunchedEffect
        )

        // Notifications overlay at the top (over everything else)
        if (notifications.isNotEmpty()) {
            Log.d(TAG, "Composing NotificationsList with ${notifications.size} notifications")
            NotificationsList(
                notifications = notifications,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .align(Alignment.TopCenter),
                onNotificationClick = { notification ->
                    Log.d(TAG, "=== NOTIFICATION CLICKED ===")
                    Log.d(TAG, "Package: ${notification.packageName}")
                    Log.d(TAG, "Title: ${notification.title}")
                    Log.d(TAG, "Content: ${notification.content}")
                    Log.d(TAG, "Has contentIntent: ${notification.contentIntent != null}")

                    // Launch the notification's content intent
                    notification.contentIntent?.let { pendingIntent ->
                        try {
                            Log.d(TAG, "Attempting to send PendingIntent with ActivityOptions...")
                            Log.d(TAG, "PendingIntent details: $pendingIntent")

                            // Create ActivityOptions to ensure foreground launch (fixes BAL restrictions)
                            val options = android.app.ActivityOptions.makeBasic().apply {
                                // Mark this as allowed to start background activities
                                // This is needed because Android blocks "Background Activity Launch" (BAL)
                                pendingIntentBackgroundActivityStartMode =
                                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            }.toBundle()

                            // Send with context and options to bypass BAL restrictions
                            pendingIntent.send(
                                context,
                                0, // requestCode
                                null, // intent
                                null, // onFinished callback
                                null, // handler
                                null, // requiredPermission
                                options // ActivityOptions bundle
                            )
                            Log.d(TAG, "Successfully sent PendingIntent for ${notification.packageName}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send PendingIntent", e)
                            Log.e(TAG, "Exception type: ${e.javaClass.name}")
                            Log.e(TAG, "Exception message: ${e.message}")
                            e.printStackTrace()

                            // Fallback to launching app on error
                            Log.d(TAG, "Trying fallback: launching app directly")
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                            if (launchIntent != null) {
                                try {
                                    launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                    Log.d(TAG, "Fallback: Launched app ${notification.packageName}")
                                } catch (e2: Exception) {
                                    Log.e(TAG, "Fallback failed", e2)
                                }
                            } else {
                                Log.e(TAG, "Fallback failed: No launch intent for ${notification.packageName}")
                            }
                        }
                    } ?: run {
                        // Fallback: launch the app if no content intent
                        Log.d(TAG, "No contentIntent available, trying to launch app directly")
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                        if (launchIntent != null) {
                            try {
                                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                                Log.d(TAG, "Successfully launched app ${notification.packageName}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to launch app ${notification.packageName}", e)
                            }
                        } else {
                            Log.e(TAG, "No launch intent available for ${notification.packageName}")
                        }
                    }
                    Log.d(TAG, "=========================")
                }
            )
        } else {
            Log.d(TAG, "Not showing NotificationsList - empty list")
        }

        // Notification icons list - shows app icons for apps with notifications
        if (notifications.isNotEmpty()) {
            NotificationIconsList(
                notifications = notifications,
                config = config,
                modifier = Modifier.fillMaxSize(),
                onIconClick = { notification ->
                    Log.d(TAG, "Notification icon clicked: ${notification.packageName}")
                    // Launch the app
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                    if (launchIntent != null) {
                        try {
                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to launch app ${notification.packageName}", e)
                        }
                    }
                }
            )
        }

        // SearchOverlayMaterialView - shown above everything else
        AndroidView(
            factory = { ctx ->
                SearchOverlayMaterialView(ctx).apply {
                    id = View.generateViewId()
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    visibility = View.GONE
                }.also { searchOverlayRef.value = it }
            },
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
        )

        // Independent PathSettingsDialog - shown at root level
        if (showPathSettings) {
            PathSettingsDialogCompose(
                config = config,
                onConfigChanged = { newConfig ->
                    config = newConfig
                    pathViewRef.value?.config = config
                    saveConfig(context, config)
                },
                onDismiss = { showPathSettings = false },
                onCategoryChanged = { category ->
                    pathViewRef.value?.showLetterIndexBackground = (category == PathSettingsDialog.Category.LETTERS)
                    pathViewRef.value?.invalidate()
                }
            )
        }
    }

    // Handle shortcut click callbacks
    LaunchedEffect(dialogState) {
        // Dialog state changes are handled by the OnShortcutClick callbacks
    }
}

// Helper functions
private fun loadConfig(context: Context): PathConfig {
    val prefs = context.getSharedPreferences("rainbow_path_config", Context.MODE_PRIVATE)
    val configJson = prefs.getString("config", null)
    return if (configJson != null) {
        try {
            Gson().fromJson(configJson, PathConfig::class.java)
        } catch (e: Exception) {
            Log.e("RainbowPathScreen", "Failed to load config", e)
            PathConfig()
        }
    } else {
        PathConfig()
    }
}

private fun saveConfig(context: Context, config: PathConfig) {
    val prefs = context.getSharedPreferences("rainbow_path_config", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("config", Gson().toJson(config))
        .apply()
}

private fun updateApps(
    pathView: RainbowPathView,
    viewModel: ViewModel,
    inFolder: Boolean,
    onlyFavorites: Boolean,
    icons: Map<String, Drawable?>?
) {
    // Update rainbowAll to get the full combined list (apps + folders)
    if (!inFolder) {
        viewModel.updateRainbowAll()
    }

    // Always provide the folder structure from rainbowAll so badges can be shown correctly
    val folders = viewModel.rainbowAll.filter { it.folderName != null && it.apps.size > 1 }
    Log.d("RainbowPathScreen", "updateApps: Found ${folders.size} folders from rainbowAll (size=${viewModel.rainbowAll.size})")
    pathView.setFolders(folders)

    if (inFolder) {
        val folderApps = viewModel.rainbowFiltered.map {
            EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)
        }
        pathView.setApps(folderApps)
    } else {
        viewModel.updateRainbowFiltered(onlyFavorites)
        val combinedApps = viewModel.rainbowFiltered.map {
            EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)
        }
        pathView.setApps(combinedApps)
    }

    icons?.let {
        pathView.icons = it.toMutableMap()
    }
    pathView.invalidate()
}

/**
 * Flow-based version of updateApps that uses computed StateFlows
 * This eliminates manual cache invalidation and provides automatic reactivity
 */
private fun updateAppsFromFlows(
    pathView: RainbowPathView,
    rainbowFiltered: List<EncapsulatedAppInfoWithFolder>,
    rainbowAll: List<EncapsulatedAppInfoWithFolder>,
    inFolder: Boolean,
    icons: Map<String, Drawable?>?
) {
    // Always provide the folder structure from rainbowAll so badges can be shown correctly
    val folders = rainbowAll.filter { it.folderName != null && it.apps.size > 1 }
    Log.d("RainbowPathScreen", "updateAppsFromFlows: Found ${folders.size} folders from rainbowAll (size=${rainbowAll.size})")
    pathView.setFolders(folders)

    // Use the pre-computed rainbowFiltered from Flow
    // No need to call viewModel.updateRainbowFiltered() - it's automatically computed
    pathView.setApps(rainbowFiltered)

    icons?.let {
        pathView.icons = it.toMutableMap()
    }
    pathView.invalidate()
}

private fun getTranslatedString(id: Int, context: Context): String {
    return context.resources.getString(id)
}

private fun getDrawable(resourceId: Int, context: Context): Drawable? {
    return ResourcesCompat.getDrawable(context.resources, resourceId, null)?.apply {
        when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                setTint(Color.WHITE)
            }
            else -> {
                setTint(Color.BLACK)
            }
        }
    }
}

private fun openShortcutsMenu(
    thing: EncapsulatedAppInfoWithFolder,
    mainActivity: MainActivityCompose,
    viewModel: ViewModel,
    icons: Map<String, Drawable?>?,
    context: Context,
    pathView: View?,
    currentShortcuts: List<ShortcutInfo>,
    shortcutClickHandler: OnShortcutClick,
    onStateChanged: (DialogStates, List<ShortcutInfo>) -> Unit
) {
    val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val newShortcuts = if (launcherApps.hasShortcutHostPermission()) {
        mainActivity.getShortcutFromPackage(thing.apps.first().packageName)
    } else {
        emptyList()
    }
    val appDrawable = icons?.get(thing.apps.first().packageName)
    val isInMap = mainActivity.isAppAlreadyInMap(thing.apps.first())
    val actions = newShortcuts.map { ShortcutAction(it.shortLabel.toString(), appDrawable) }.toMutableList().apply {
        add(ShortcutAction(getTranslatedString(R.string.app_info, context), getDrawable(R.drawable.ic_outline_info_75, context)))
        add(if (thing.apps.first().favorite)
            ShortcutAction(getTranslatedString(R.string.remove_from_favorites, context), getDrawable(R.drawable.star_fill, context))
        else
            ShortcutAction(getTranslatedString(R.string.add_to_favorites, context), getDrawable(R.drawable.star_empty, context)))
        add(if (isInMap)
            ShortcutAction(getTranslatedString(R.string.remove_from_folder, context), getDrawable(R.drawable.baseline_folder_off_24, context))
        else
            ShortcutAction(getTranslatedString(R.string.add_to_folder, context), getDrawable(R.drawable.ic_baseline_create_new_folder_50, context)))
    }
    onStateChanged(DialogStates.APP_SHORTCUTS, newShortcuts)
    mainActivity.showDialogWithActions(actions, shortcutClickHandler, pathView ?: return)
}

private fun addAppToMap(
    mapIndex: Int,
    globalThing: EncapsulatedAppInfoWithFolder?,
    viewModel: ViewModel,
    mainActivity: MainActivityCompose,
    pathView: View?,
    onUpdate: () -> Unit
) {
    if (globalThing == null || pathView == null) return

    mainActivity.shortcutPopup?.dismiss()

    val rainbowMape = viewModel.rainbowMape.value
    if (mapIndex >= (rainbowMape?.size ?: 0)) {
        openCreateFolderDialog(globalThing, mainActivity, pathView)
    } else {
        val mapa = rainbowMape!![mapIndex]
        val nova_mapa = mapa.copy(apps = mapa.apps.plus(globalThing.apps.first()).toMutableList())
        viewModel.updateRainbowMapa(nova_mapa)
        mainActivity.rainbowMapaUpdateItem(nova_mapa)
        onUpdate()
    }
}

private fun openCreateFolderDialog(
    globalThing: EncapsulatedAppInfoWithFolder?,
    mainActivity: MainActivityCompose,
    pathView: View?
) {
    if (globalThing == null || pathView == null) return

    mainActivity.openFolderNameMenu(pathView, false, "", false) {
        val novaMapa = RainbowMapa(0, it, mutableListOf(globalThing.apps.first()), true)
        mainActivity.rainbowMapaInsertItem(novaMapa)
    }
}

private fun folderOptions(
    index: Int,
    globalThing: EncapsulatedAppInfoWithFolder?,
    viewModel: ViewModel,
    mainActivity: MainActivityCompose,
    pathView: View?,
    onUpdate: () -> Unit
) {
    if (globalThing == null || pathView == null) return

    mainActivity.shortcutPopup?.dismiss()

    when (index) {
        0 -> {
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == globalThing.folderName }
            mainActivity.openFolderNameMenu(pathView, true, mapa!!.folderName, false) { ime ->
                val nova_mapa = mapa?.copy(folderName = ime)
                if (nova_mapa != null) {
                    viewModel.updateRainbowMapa(nova_mapa)
                    mainActivity.rainbowMapaUpdateItem(nova_mapa)
                }
                onUpdate()
            }
        }
        1 -> {
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == globalThing.folderName }
            if (mapa != null) {
                viewModel.deleteRainbowMapa(mapa)
                mainActivity.rainbowMapaDeleteItem(mapa)
            }
            onUpdate()
        }
        2 -> {
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == globalThing.folderName }
            val nova_mapa = mapa?.copy(favorite = !mapa.favorite)
            if (nova_mapa != null) {
                viewModel.updateRainbowMapa(nova_mapa)
                mainActivity.rainbowMapaUpdateItem(nova_mapa)
            }
            onUpdate()
        }
    }
}

