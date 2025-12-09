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
import com.ingokodba.dragnav.*
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
    val content: String
)

/**
 * Composable function to display app notifications in a list
 */
@Composable
fun NotificationsList(
    notifications: List<AppNotification>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications) { notification ->
            NotificationItem(notification = notification)
        }
    }
}

/**
 * Individual notification item
 */
@Composable
fun NotificationItem(
    notification: AppNotification,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
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
    mainActivity: MainActivity,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // State management
    var config by remember { mutableStateOf(loadConfig(context)) }
    var inFolder by remember { mutableStateOf(false) }
    var currentAppIndex by remember { mutableStateOf<Int?>(null) }
    var shortcuts by remember { mutableStateOf<List<ShortcutInfo>>(emptyList()) }
    var globalThing by remember { mutableStateOf<EncapsulatedAppInfoWithFolder?>(null) }
    var dialogState by remember { mutableStateOf<DialogStates?>(null) }
    
    // Coroutine jobs
    var flingJob by remember { mutableStateOf<Job?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    var settingsCountdownJob by remember { mutableStateOf<Job?>(null) }
    
    // View references
    var pathViewRef by remember { mutableStateOf<RainbowPathView?>(null) }
    var searchOverlayRef by remember { mutableStateOf<SearchOverlayMaterialView?>(null) }
    var topTouchAreaRef by remember { mutableStateOf<View?>(null) }
    
    // SharedPreferences
    val prefs = remember {
        context.getSharedPreferences("rainbow_path_config", Context.MODE_PRIVATE)
    }
    var onlyFavorites by remember {
        mutableStateOf(prefs.getBoolean("only_favorites", false))
    }
    
    // Observe ViewModel
    val appsList by viewModel.appsList.observeAsState()
    val rainbowMape by viewModel.rainbowMape.observeAsState()
    val icons by viewModel.icons.observeAsState()
    
    // Initialize rainbow filtered if needed
    LaunchedEffect(Unit) {
        if (viewModel.rainbowFiltered.isEmpty() && appsList != null) {
            viewModel.updateRainbowFiltered(onlyFavorites)
        }
    }
    
    // Update apps when data changes
    LaunchedEffect(appsList, rainbowMape, inFolder, onlyFavorites) {
        pathViewRef?.let { pathView ->
            updateApps(
                pathView = pathView,
                viewModel = viewModel,
                inFolder = inFolder,
                onlyFavorites = onlyFavorites,
                icons = icons
            )
        }
    }
    
    // Update icons when they change
    LaunchedEffect(icons) {
        icons?.let { iconMap: Map<String, Drawable?> ->
            pathViewRef?.let { pathView: RainbowPathView ->
                pathView.icons = iconMap.toMutableMap()
                pathView.invalidate()
            }
            searchOverlayRef?.setIcons(iconMap)
        }
    }
    
    // Lifecycle handling for pause/resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    pathViewRef?.saveScrollPosition()
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
    
    // Helper functions
    fun getDisplayedApps(): List<EncapsulatedAppInfoWithFolder> {
        return viewModel.rainbowFiltered.map {
            EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)
        }
    }
    
    fun updateApps() {
        pathViewRef?.let { pathView ->
            updateApps(
                pathView = pathView,
                viewModel = viewModel,
                inFolder = inFolder,
                onlyFavorites = onlyFavorites,
                icons = icons
            )
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
            pathViewRef?.saveScrollPosition()
            context.startActivity(launchIntent)
        }
    }
    
    fun openFolder(appIndex: Int) {
        countdownJob?.cancel()
        
        val apps = getDisplayedApps()
        if (appIndex < 0 || appIndex >= apps.size) return
        
        val thing = apps[appIndex]
        if (thing.folderName == null) return
        
        pathViewRef?.saveScrollPosition()
        
        val folderApps = thing.apps
        Log.d("RainbowPathScreen", "Opening folder with ${folderApps.size} apps: ${folderApps.map { it.label }}")
        
        inFolder = true
        pathViewRef?.inFolder = true
        
        val folderEncapsulated = folderApps.map {
            EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)
        }.toMutableList()
        Log.d("RainbowPathScreen", "Setting rainbowFiltered with ${folderEncapsulated.size} items")
        viewModel.setRainbowFilteredValues(folderEncapsulated)
        
        pathViewRef?.resetFolderScroll()
        updateApps()
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
                    openShortcutsMenu(thing, mainActivity, viewModel, icons, context, pathViewRef, shortcuts, handler) { newState, newShortcuts ->
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
                    mainActivity.showDialogWithActions(actions, handler, pathViewRef ?: return@withContext)
                }
                
                pathViewRef?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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
                    mainActivity.shortcutPopup?.dismiss()
                } else if (index == shortcuts.size + 1) {
                    // Toggle favorite
                    val app = thing.apps.first()
                    app.favorite = !app.favorite
                    mainActivity.saveAppInfo(app)
                    updateApps()
                    mainActivity.shortcutPopup?.dismiss()
                } else if (index == shortcuts.size + 2) {
                    // Add/remove from folder
                    if (mainActivity.isAppAlreadyInMap(thing.apps.first())) {
                        // Remove from folder
                        val mapa = rainbowMape?.firstOrNull { rainbowMapa -> rainbowMapa.apps.contains(thing.apps.first()) }
                        if (mapa != null) {
                            val azurirana_mapa = mapa.copy(apps = mapa.apps.minus(thing.apps.first()).toMutableList())
                            viewModel.updateRainbowMapa(azurirana_mapa)
                            mainActivity.rainbowMapaUpdateItem(azurirana_mapa)
                            updateApps()
                        }
                        mainActivity.shortcutPopup?.dismiss()
                    } else {
                        // Show folder selection dialog
                        dialogState = DialogStates.ADDING_TO_FOLDER
                        shortcutClickHandlerRef?.let { handler ->
                            mainActivity.showDialogWithActions(
                                rainbowMape?.map {
                                    ShortcutAction(it.folderName, getDrawable(R.drawable.baseline_folder_24, context))
                                }?.toMutableList()?.apply {
                                    add(ShortcutAction("Nova mapa", getDrawable(R.drawable.ic_baseline_create_new_folder_50, context)))
                                } ?: mutableListOf(),
                                handler,
                                pathViewRef ?: return
                            )
                        }
                    }
                }
                return
            }
            
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
                Log.e("RainbowPathScreen", "Failed to start shortcut", e)
            }
        }
    }
    
    fun addAppToMap(mapIndex: Int) {
        mainActivity.shortcutPopup?.dismiss()
        if (mapIndex >= (rainbowMape?.size ?: 0)) {
            openCreateFolderDialog(globalThing, mainActivity, pathViewRef)
        } else {
            val mapa = rainbowMape!![mapIndex]
            val nova_mapa = mapa.copy(apps = mapa.apps.plus(globalThing!!.apps.first()).toMutableList())
            viewModel.updateRainbowMapa(nova_mapa)
            mainActivity.rainbowMapaUpdateItem(nova_mapa)
            updateApps()
        }
    }
    
    fun folderOptions(index: Int) {
        mainActivity.shortcutPopup?.dismiss()
        when (index) {
            0 -> {
                // Rename folder
                val mapa = rainbowMape?.find { it.folderName == globalThing!!.folderName }
                mainActivity.openFolderNameMenu(pathViewRef ?: return, true, mapa!!.folderName, false) { ime ->
                    val nova_mapa = mapa?.copy(folderName = ime)
                    if (nova_mapa != null) {
                        viewModel.updateRainbowMapa(nova_mapa)
                        mainActivity.rainbowMapaUpdateItem(nova_mapa)
                    }
                    updateApps()
                }
            }
            1 -> {
                // Delete folder
                val mapa = rainbowMape?.find { it.folderName == globalThing!!.folderName }
                if (mapa != null) {
                    viewModel.deleteRainbowMapa(mapa)
                    mainActivity.rainbowMapaDeleteItem(mapa)
                }
                updateApps()
            }
            2 -> {
                // Toggle favorite
                val mapa = rainbowMape?.find { it.folderName == globalThing!!.folderName }
                val nova_mapa = mapa?.copy(favorite = !mapa.favorite)
                if (nova_mapa != null) {
                    viewModel.updateRainbowMapa(nova_mapa)
                    mainActivity.rainbowMapaUpdateItem(nova_mapa)
                }
                updateApps()
            }
        }
    }
    
    fun toggleFavorites() {
        if (inFolder) return
        
        onlyFavorites = !onlyFavorites
        prefs.edit().putBoolean("only_favorites", onlyFavorites).apply()
        pathViewRef?.onlyFavorites = onlyFavorites
        updateApps()
    }
    
    fun goToHome() {
        if (inFolder) {
            pathViewRef?.restoreScrollPosition()
        }
        
        inFolder = false
        pathViewRef?.inFolder = false
        updateApps()
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
        
        searchOverlayRef?.setApps(allAppsForSearch)
        icons?.let {
            searchOverlayRef?.setIcons(it)
        }
        
        searchOverlayRef?.setListener(object : SearchOverlayMaterialView.SearchOverlayListener {
            override fun onAppClicked(app: EncapsulatedAppInfoWithFolder) {
                searchOverlayRef?.hide()
                if (app.folderName == null && app.apps.isNotEmpty()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.apps.first().packageName)
                    if (launchIntent != null) {
                        pathViewRef?.saveScrollPosition()
                        context.startActivity(launchIntent)
                    }
                } else if (app.folderName != null) {
                    if (inFolder) {
                        goToHome()
                    }
                    val wasOnlyFavorites = onlyFavorites
                    onlyFavorites = true
                    pathViewRef?.onlyFavorites = true
                    updateApps()
                    
                    val apps = getDisplayedApps()
                    val folderIndex = apps.indexOfFirst {
                        it.folderName == app.folderName
                    }
                    if (folderIndex >= 0) {
                        openFolder(folderIndex)
                    } else {
                        onlyFavorites = wasOnlyFavorites
                        pathViewRef?.onlyFavorites = wasOnlyFavorites
                        updateApps()
                        Log.d("RainbowPathScreen", "Folder not found in displayed apps: ${app.folderName}")
                    }
                }
            }
            
            override fun onAppLongPressed(app: EncapsulatedAppInfoWithFolder) {
                globalThing = app
                val handler = shortcutClickHandlerRef ?: return
                if (app.folderName == null && app.apps.isNotEmpty()) {
                    openShortcutsMenu(app, mainActivity, viewModel, icons, context, pathViewRef, shortcuts, handler) { newState, newShortcuts ->
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
                    mainActivity.showDialogWithActions(actions, handler, searchOverlayRef ?: return)
                }
            }
            
            override fun onDismiss() {
                searchOverlayRef?.hide()
            }
        })
        
        searchOverlayRef?.show()
    }
    
    fun showSettingsDialog() {
        PathSettingsDialog(
            context,
            config,
            onConfigChanged = { newConfig ->
                config = newConfig
                pathViewRef?.config = config
                saveConfig(context, config)
            },
            onCategoryChanged = { category ->
                pathViewRef?.showLetterIndexBackground = (category == PathSettingsDialog.Category.LETTERS)
                pathViewRef?.invalidate()
            }
        ).show()
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Main container with RainbowPathView and SearchOverlay
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
                    topTouchAreaRef = topTouchArea
                    
                    // RainbowPathView
                    val pathView = RainbowPathView(ctx).apply {
                        id = View.generateViewId()
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            topMargin = (100 * resources.displayMetrics.density).toInt()
                        }
                    }
                    addView(pathView)
                    pathViewRef = pathView
                    
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
                    searchOverlayRef = searchOverlay
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { container ->
                // Initialize pathView
                pathViewRef?.let { pathView ->
                    pathView.config = config
                    pathView.onlyFavorites = onlyFavorites
                    
                    // Set up event listener
                    pathView.setEventListener(object : RainbowPathView.EventListener {
                        override fun onAppClicked(appIndex: Int) {
                            val apps = getDisplayedApps()
                            if (appIndex < 0 || appIndex >= apps.size) return
                            
                            val thing = apps[appIndex]
                            if (thing.folderName == null) {
                                launchApp(appIndex)
                            } else {
                                openFolder(appIndex)
                            }
                        }
                        
                        override fun onAppLongPressed(appIndex: Int) {
                            showShortcuts(appIndex)
                        }
                        
                        override fun onShortcutClicked(shortcutIndex: Int) {
                            openShortcut(shortcutIndex)
                        }
                        
                        override fun onFavoritesToggled() {
                            toggleFavorites()
                        }
                        
                        override fun onBackButtonPressed() {
                            if (searchOverlayRef?.visibility == View.VISIBLE) {
                                searchOverlayRef?.hide()
                            } else if (inFolder) {
                                goToHome()
                            }
                        }
                        
                        override fun onFlingStarted() {
                            flingJob?.cancel()
                            flingJob = scope.launch(Dispatchers.Main) {
                                while (isActive) {
                                    delay(16) // ~60fps
                                    pathView.flingUpdate()
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
                                pathView.triggerLongPress()
                            }
                        }
                        
                        override fun onSearchButtonClicked() {
                            showSearchOverlay()
                        }
                    })
                    
                    icons?.let {
                        pathView.icons = it
                        pathView.invalidate()
                    }
                    
                    updateApps()
                }
                
                // Set up top touch area for settings
                var touchStartX = 0f
                var touchStartY = 0f
                topTouchAreaRef?.setOnTouchListener { v, event ->
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
        )
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
    return MainActivity.resources2.getString(id)
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
    mainActivity: MainActivity,
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
    val actions = newShortcuts.map { ShortcutAction(it.shortLabel.toString(), appDrawable) }.toMutableList().apply {
        add(ShortcutAction(getTranslatedString(R.string.app_info, context), getDrawable(R.drawable.ic_outline_info_75, context)))
        add(if (thing.apps.first().favorite)
            ShortcutAction(getTranslatedString(R.string.remove_from_favorites, context), getDrawable(R.drawable.star_fill, context))
        else
            ShortcutAction(getTranslatedString(R.string.add_to_favorites, context), getDrawable(R.drawable.star_empty, context)))
        add(if (mainActivity.isAppAlreadyInMap(thing.apps.first()))
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
    mainActivity: MainActivity,
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
    mainActivity: MainActivity,
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
    mainActivity: MainActivity,
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

