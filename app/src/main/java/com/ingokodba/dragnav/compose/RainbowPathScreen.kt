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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.RainbowMapa
import com.ingokodba.dragnav.rainbow.PathConfig
import com.ingokodba.dragnav.rainbow.PathSettingsDialog
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
            .clickable(onClick = onClick)
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
    mainActivity: AppCompatActivity, // Accept MainActivity or MainActivityCompose
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

    // Helper functions to call methods on either activity type
    fun callSaveAppInfo(app: AppInfo) {
        when (mainActivity) {
            is MainActivity -> mainActivity.saveAppInfo(app)
            is MainActivityCompose -> mainActivity.saveAppInfo(app)
        }
    }

    fun callIsAppAlreadyInMap(app: AppInfo): Boolean {
        return when (mainActivity) {
            is MainActivity -> mainActivity.isAppAlreadyInMap(app)
            is MainActivityCompose -> mainActivity.isAppAlreadyInMap(app)
            else -> false
        }
    }

    fun callShowDialogWithActions(actions: List<ShortcutAction>, handler: OnShortcutClick, view: View) {
        when (mainActivity) {
            is MainActivity -> mainActivity.showDialogWithActions(actions, handler, view)
            is MainActivityCompose -> mainActivity.showDialogWithActions(actions, handler, view)
        }
    }

    fun callRainbowMapaUpdateItem(mapa: RainbowMapa) {
        when (mainActivity) {
            is MainActivity -> mainActivity.rainbowMapaUpdateItem(mapa)
            is MainActivityCompose -> mainActivity.rainbowMapaUpdateItem(mapa)
        }
    }

    fun callRainbowMapaDeleteItem(mapa: RainbowMapa) {
        when (mainActivity) {
            is MainActivity -> mainActivity.rainbowMapaDeleteItem(mapa)
            is MainActivityCompose -> mainActivity.rainbowMapaDeleteItem(mapa)
        }
    }

    fun callOpenFolderNameMenu(view: View, editing: Boolean, name: String, showColor: Boolean, callback: (String) -> Unit) {
        when (mainActivity) {
            is MainActivity -> mainActivity.openFolderNameMenu(view, editing, name, showColor, callback)
            is MainActivityCompose -> mainActivity.openFolderNameMenu(view, editing, name, showColor, callback)
        }
    }

    fun callRainbowMapaInsertItem(mapa: RainbowMapa) {
        when (mainActivity) {
            is MainActivity -> mainActivity.rainbowMapaInsertItem(mapa)
            is MainActivityCompose -> mainActivity.rainbowMapaInsertItem(mapa)
        }
    }

    fun callGetShortcutFromPackage(packageName: String): List<ShortcutInfo> {
        return when (mainActivity) {
            is MainActivity -> mainActivity.getShortcutFromPackage(packageName)
            is MainActivityCompose -> mainActivity.getShortcutFromPackage(packageName)
            else -> emptyList()
        }
    }

    fun dismissShortcutPopup() {
        when (mainActivity) {
            is MainActivity -> mainActivity.shortcutPopup?.dismiss()
            is MainActivityCompose -> mainActivity.shortcutPopup?.dismiss()
        }
    }

    // State management
    var config by remember { mutableStateOf(loadConfig(context)) }
    var inFolder by remember { mutableStateOf(false) }
    var currentAppIndex by remember { mutableStateOf<Int?>(null) }
    var shortcuts by remember { mutableStateOf<List<ShortcutInfo>>(emptyList()) }
    var globalThing by remember { mutableStateOf<EncapsulatedAppInfoWithFolder?>(null) }
    var dialogState by remember { mutableStateOf<DialogStates?>(null) }

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

    // Observe ViewModel - these are already LiveData, no need for extra state
    val appsList by viewModel.appsList.observeAsState()
    val rainbowMape by viewModel.rainbowMape.observeAsState()
    val icons by viewModel.icons.observeAsState()
    val notifications by viewModel.notifications.observeAsState(emptyList())

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

    // Initialize rainbow filtered if needed
    LaunchedEffect(appsList) {
        if (viewModel.rainbowFiltered.isEmpty() && appsList != null) {
            Log.d(TAG, "Initializing rainbowFiltered")
            viewModel.updateRainbowFiltered(onlyFavorites)
        }
    }

    // Update apps when data changes - include icons in key
    LaunchedEffect(appsList, rainbowMape, inFolder, onlyFavorites, icons) {
        Log.d(TAG, "LaunchedEffect(update apps) triggered")
        pathViewRef.value?.let { pathView ->
            val startTime = System.currentTimeMillis()
            updateApps(
                pathView = pathView,
                viewModel = viewModel,
                inFolder = inFolder,
                onlyFavorites = onlyFavorites,
                icons = icons
            )
            pathView.invalidate()
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "updateApps + invalidate took ${elapsed}ms")
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
    val getDisplayedApps = remember {
        {
            viewModel.rainbowFiltered.map {
                EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)
            }
        }
    }

    val updateAppsCallback = remember {
        {
            pathViewRef.value?.let { pathView ->
                updateApps(
                    pathView = pathView,
                    viewModel = viewModel,
                    inFolder = inFolder,
                    onlyFavorites = onlyFavorites,
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
        }.toMutableList()
        Log.d("RainbowPathScreen", "Setting rainbowFiltered with ${folderEncapsulated.size} items")
        viewModel.setRainbowFilteredValues(folderEncapsulated)

        pathViewRef.value?.resetFolderScroll()
        updateAppsCallback()
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
                    app.favorite = !app.favorite
                    callSaveAppInfo(app)
                    updateAppsCallback()
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
                            updateAppsCallback()
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
        if (mapIndex >= (rainbowMape?.size ?: 0)) {
            openCreateFolderDialog(globalThing, mainActivity, pathViewRef.value)
        } else {
            val mapa = rainbowMape!![mapIndex]
            val nova_mapa = mapa.copy(apps = mapa.apps.plus(globalThing!!.apps.first()).toMutableList())
            viewModel.updateRainbowMapa(nova_mapa)
            callRainbowMapaUpdateItem(nova_mapa)
            updateAppsCallback()
        }
    }

    fun folderOptions(index: Int) {
        dismissShortcutPopup()
        when (index) {
            0 -> {
                // Rename folder
                val mapa = rainbowMape?.find { it.folderName == globalThing!!.folderName }
                callOpenFolderNameMenu(pathViewRef.value ?: return, true, mapa!!.folderName, false) { ime ->
                    val nova_mapa = mapa?.copy(folderName = ime)
                    if (nova_mapa != null) {
                        viewModel.updateRainbowMapa(nova_mapa)
                        callRainbowMapaUpdateItem(nova_mapa)
                    }
                    updateAppsCallback()
                }
            }
            1 -> {
                // Delete folder
                val mapa = rainbowMape?.find { it.folderName == globalThing!!.folderName }
                if (mapa != null) {
                    viewModel.deleteRainbowMapa(mapa)
                    callRainbowMapaDeleteItem(mapa)
                }
                updateAppsCallback()
            }
            2 -> {
                // Toggle favorite
                val mapa = rainbowMape?.find { it.folderName == globalThing!!.folderName }
                val nova_mapa = mapa?.copy(favorite = !mapa.favorite)
                if (nova_mapa != null) {
                    viewModel.updateRainbowMapa(nova_mapa)
                    callRainbowMapaUpdateItem(nova_mapa)
                }
                updateAppsCallback()
            }
        }
    }

    fun toggleFavorites() {
        if (inFolder) return

        onlyFavorites = !onlyFavorites
        prefs.edit().putBoolean("only_favorites", onlyFavorites).apply()
        pathViewRef.value?.onlyFavorites = onlyFavorites
        updateAppsCallback()
    }

    fun goToHome() {
        if (inFolder) {
            pathViewRef.value?.restoreScrollPosition()
        }

        inFolder = false
        pathViewRef.value?.inFolder = false
        updateAppsCallback()
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
    
    fun showSearchOverlay() {
        val allAppsForSearch = mutableListOf<EncapsulatedAppInfoWithFolder>()
        
        appsList?.let { apps ->
            allAppsForSearch.addAll(
                apps.map { EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite) }
            )
        }
        
        rainbowMape?.let { folders ->
            allAppsForSearch.addAll(
                folders.map { EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite) }
            )
        }

        searchOverlayRef.value?.setApps(allAppsForSearch)
        icons?.let {
            searchOverlayRef.value?.setIcons(it)
        }

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
                    updateAppsCallback()

                    val apps = getDisplayedApps()
                    val folderIndex = apps.indexOfFirst {
                        it.folderName == app.folderName
                    }
                    if (folderIndex >= 0) {
                        openFolder(folderIndex)
                    } else {
                        onlyFavorites = wasOnlyFavorites
                        pathViewRef.value?.onlyFavorites = wasOnlyFavorites
                        updateAppsCallback()
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
                searchOverlayRef.value?.hide()
            }
        })

        searchOverlayRef.value?.show()
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
    
    Box(modifier = modifier.fillMaxSize()) {
        // Main RainbowPathView and SearchOverlay (full size, underneath)
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
                                    }.toMutableList()
                                    Log.d("RainbowPathScreen", "Setting rainbowFiltered with ${folderEncapsulated.size} items")
                                    viewModel.setRainbowFilteredValues(folderEncapsulated)

                                    this@apply.resetFolderScroll()
                                    updateAppsCallback()
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

                    // SearchOverlayMaterialView
                    val searchOverlay = SearchOverlayMaterialView(ctx).apply {
                        id = View.generateViewId()
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        visibility = View.GONE
                    }
                    addView(searchOverlay)
                    searchOverlayRef.value = searchOverlay

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
                    // Launch the notification's content intent
                    notification.contentIntent?.let { pendingIntent ->
                        try {
                            pendingIntent.send()
                            Log.d(TAG, "Launched notification for ${notification.packageName}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to launch notification", e)
                        }
                    } ?: run {
                        // Fallback: launch the app if no content intent
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                            Log.d(TAG, "Launched app ${notification.packageName}")
                        } else {
                            Log.e(TAG, "No launch intent for ${notification.packageName}")
                        }
                    }
                }
            )
        } else {
            Log.d(TAG, "Not showing NotificationsList - empty list")
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
    mainActivity: AppCompatActivity,
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
        when (mainActivity) {
            is MainActivity -> mainActivity.getShortcutFromPackage(thing.apps.first().packageName)
            is MainActivityCompose -> mainActivity.getShortcutFromPackage(thing.apps.first().packageName)
            else -> emptyList()
        }
    } else {
        emptyList()
    }
    val appDrawable = icons?.get(thing.apps.first().packageName)
    val isInMap = when (mainActivity) {
        is MainActivity -> mainActivity.isAppAlreadyInMap(thing.apps.first())
        is MainActivityCompose -> mainActivity.isAppAlreadyInMap(thing.apps.first())
        else -> false
    }
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
    when (mainActivity) {
        is MainActivity -> mainActivity.showDialogWithActions(actions, shortcutClickHandler, pathView ?: return)
        is MainActivityCompose -> mainActivity.showDialogWithActions(actions, shortcutClickHandler, pathView ?: return)
    }
}

private fun addAppToMap(
    mapIndex: Int,
    globalThing: EncapsulatedAppInfoWithFolder?,
    viewModel: ViewModel,
    mainActivity: AppCompatActivity,
    pathView: View?,
    onUpdate: () -> Unit
) {
    if (globalThing == null || pathView == null) return

    when (mainActivity) {
        is MainActivity -> mainActivity.shortcutPopup?.dismiss()
        is MainActivityCompose -> mainActivity.shortcutPopup?.dismiss()
    }

    val rainbowMape = viewModel.rainbowMape.value
    if (mapIndex >= (rainbowMape?.size ?: 0)) {
        openCreateFolderDialog(globalThing, mainActivity, pathView)
    } else {
        val mapa = rainbowMape!![mapIndex]
        val nova_mapa = mapa.copy(apps = mapa.apps.plus(globalThing.apps.first()).toMutableList())
        viewModel.updateRainbowMapa(nova_mapa)
        when (mainActivity) {
            is MainActivity -> mainActivity.rainbowMapaUpdateItem(nova_mapa)
            is MainActivityCompose -> mainActivity.rainbowMapaUpdateItem(nova_mapa)
        }
        onUpdate()
    }
}

private fun openCreateFolderDialog(
    globalThing: EncapsulatedAppInfoWithFolder?,
    mainActivity: AppCompatActivity,
    pathView: View?
) {
    if (globalThing == null || pathView == null) return

    when (mainActivity) {
        is MainActivity -> mainActivity.openFolderNameMenu(pathView, false, "", false) {
            val novaMapa = RainbowMapa(0, it, mutableListOf(globalThing.apps.first()), true)
            mainActivity.rainbowMapaInsertItem(novaMapa)
        }
        is MainActivityCompose -> mainActivity.openFolderNameMenu(pathView, false, "", false) {
            val novaMapa = RainbowMapa(0, it, mutableListOf(globalThing.apps.first()), true)
            mainActivity.rainbowMapaInsertItem(novaMapa)
        }
    }
}

private fun folderOptions(
    index: Int,
    globalThing: EncapsulatedAppInfoWithFolder?,
    viewModel: ViewModel,
    mainActivity: AppCompatActivity,
    pathView: View?,
    onUpdate: () -> Unit
) {
    if (globalThing == null || pathView == null) return

    when (mainActivity) {
        is MainActivity -> mainActivity.shortcutPopup?.dismiss()
        is MainActivityCompose -> mainActivity.shortcutPopup?.dismiss()
    }

    when (index) {
        0 -> {
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == globalThing.folderName }
            when (mainActivity) {
                is MainActivity -> mainActivity.openFolderNameMenu(pathView, true, mapa!!.folderName, false) { ime ->
                    val nova_mapa = mapa?.copy(folderName = ime)
                    if (nova_mapa != null) {
                        viewModel.updateRainbowMapa(nova_mapa)
                        mainActivity.rainbowMapaUpdateItem(nova_mapa)
                    }
                    onUpdate()
                }
                is MainActivityCompose -> mainActivity.openFolderNameMenu(pathView, true, mapa!!.folderName, false) { ime ->
                    val nova_mapa = mapa?.copy(folderName = ime)
                    if (nova_mapa != null) {
                        viewModel.updateRainbowMapa(nova_mapa)
                        mainActivity.rainbowMapaUpdateItem(nova_mapa)
                    }
                    onUpdate()
                }
            }
        }
        1 -> {
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == globalThing.folderName }
            if (mapa != null) {
                viewModel.deleteRainbowMapa(mapa)
                when (mainActivity) {
                    is MainActivity -> mainActivity.rainbowMapaDeleteItem(mapa)
                    is MainActivityCompose -> mainActivity.rainbowMapaDeleteItem(mapa)
                }
            }
            onUpdate()
        }
        2 -> {
            val mapa = viewModel.rainbowMape.value?.find { it.folderName == globalThing.folderName }
            val nova_mapa = mapa?.copy(favorite = !mapa.favorite)
            if (nova_mapa != null) {
                viewModel.updateRainbowMapa(nova_mapa)
                when (mainActivity) {
                    is MainActivity -> mainActivity.rainbowMapaUpdateItem(nova_mapa)
                    is MainActivityCompose -> mainActivity.rainbowMapaUpdateItem(nova_mapa)
                }
            }
            onUpdate()
        }
    }
}

