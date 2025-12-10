package com.ingokodba.dragnav.rainbow

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.R
import com.google.gson.Gson
import com.ingokodba.dragnav.*
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.RainbowMapa
import kotlinx.coroutines.*

/**
 * Fragment hosting the new RainbowPathView with fully configurable path layout
 */
class MainFragmentRainbowPath : Fragment(), MainFragmentInterface, OnShortcutClick {

    private lateinit var pathView: RainbowPathView
    private lateinit var settingsButton: ImageButton
    private lateinit var searchOverlay: SearchOverlayMaterialView
    private lateinit var mActivity: MainActivity

    private val viewModel: ViewModel by activityViewModels()

    override var fragment: Fragment = this

    private var config: PathConfig = PathConfig()
    private var flingJob: Job? = null
    private var countdownJob: Job? = null
    private var settingsCountdownJob: Job? = null
    private var currentAppIndex: Int? = null
    private var shortcuts: List<ShortcutInfo> = emptyList()
    private var globalThing: EncapsulatedAppInfoWithFolder? = null
    private var dialogState: DialogStates? = null
    var inFolder: Boolean = false

    private val prefs: SharedPreferences by lazy {
        requireActivity().getSharedPreferences("rainbow_path_config", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main_rainbow_path, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pathView = view.findViewById(R.id.rainbow_path_view)
        settingsButton = view.findViewById(R.id.settings_button)
        searchOverlay = view.findViewById(R.id.search_overlay)

        // Restore fragment state if available
        savedInstanceState?.let {
            inFolder = it.getBoolean("in_folder", false)
            pathView.inFolder = inFolder
            Log.d("MainFragmentRainbowPath", "onViewCreated - restored inFolder=$inFolder from savedInstanceState")
        }

        // Load saved config
        loadConfig()

        // Apply config to view
        pathView.config = config
        pathView.onlyFavorites = prefs.getBoolean("only_favorites", false)

        // Set up event listener
        pathView.setEventListener(object : RainbowPathView.EventListener {
            override fun onAppClicked(appIndex: Int) {
                Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onAppClicked: CALLED with appIndex=$appIndex")
                val apps = getDisplayedApps()
                Log.d("RainbowPath", "onAppClicked: appIndex=$appIndex, apps.size=${apps.size}, inFolder=$inFolder")
                
                if (appIndex < 0 || appIndex >= apps.size) {
                    Log.e("RainbowPathTouch", "MainFragmentRainbowPath.onAppClicked: BLOCKED - Invalid appIndex=$appIndex (apps.size=${apps.size})")
                    return
                }
                
                val thing = apps[appIndex]
                val appName = getNameOfApp(thing)
                Log.d("RainbowPath", "onAppClicked: Clicked on app at index $appIndex: '$appName' (packageName=${if (thing.apps.isNotEmpty()) thing.apps.first().packageName else "N/A"})")
                
                // Log all apps for debugging
                apps.forEachIndexed { idx, app ->
                    val name = getNameOfApp(app)
                    val pkg = if (app.apps.isNotEmpty()) app.apps.first().packageName else "N/A"
                    Log.d("RainbowPath", "  App[$idx]: '$name' ($pkg)")
                }
                
                if (thing.folderName == null) {
                    Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onAppClicked: Calling launchApp($appIndex)")
                    launchApp(appIndex)
                } else {
                    Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onAppClicked: Calling openFolder($appIndex)")
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
                onBackPressed()
            }

            override fun onFlingStarted() {
                startFlingAnimation()
            }

            override fun onFlingEnded() {
                flingJob?.cancel()
            }

            override fun onLongPressStart(appIndex: Int) {
                startLongPressCountdown()
            }

            override fun onSearchButtonClicked() {
                showSearchOverlay()
            }
        })

        // Set up top touch area to detect long press for opening settings
        val topTouchArea = view.findViewById<View>(R.id.top_touch_area)
        topTouchArea.isClickable = true
        var topTouchStartX = 0f
        var topTouchStartY = 0f
        topTouchArea.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    topTouchStartX = event.x
                    topTouchStartY = event.y
                    startCountdownForSettings()
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    // Stop countdown if user moves finger significantly
                    val moveDistance = kotlin.math.abs(event.x - topTouchStartX) + kotlin.math.abs(event.y - topTouchStartY)
                    if (moveDistance > 50) { // 50dp threshold
                        stopCountdownForSettings()
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    stopCountdownForSettings()
                    true
                }
                else -> false
            }
        }

        // Load icons if available
        viewModel.icons.value?.let {
            pathView.icons = it
            pathView.onIconsUpdated() // Check if visible app icons have been loaded
        }

        // Load apps
        updateApps()

        // Observe apps list for changes
        viewModel.appsList.observe(viewLifecycleOwner) { apps ->
            Log.d("RainbowPath", "appsList observer triggered with ${apps?.size ?: 0} apps")
            apps?.let {
                updateApps()
            }
        }
        
        // Observe rainbow maps for changes
        viewModel.rainbowMape.observe(viewLifecycleOwner) { maps ->
            Log.d("RainbowPath", "rainbowMape observer triggered with ${maps?.size ?: 0} maps")
            updateApps()
        }
        
        // Initialize rainbow filtered if needed
        if (viewModel.rainbowFiltered.isEmpty() && viewModel.appsList.value != null) {
            viewModel.updateRainbowFiltered(pathView.onlyFavorites)
        }

        // Observe icons for changes
        viewModel.icons.observe(viewLifecycleOwner) { icons ->
            Log.d("RainbowPath", "icons observer triggered")
            icons?.let {
                pathView.icons = it
                pathView.invalidate()
                pathView.onIconsUpdated() // Check if visible app icons have been loaded
                searchOverlay.setIcons(it)
            }
        }

        Log.d("RainbowPath", "Fragment created")
    }

    private fun loadConfig() {
        val configJson = prefs.getString("config", null)
        if (configJson != null) {
            try {
                config = Gson().fromJson(configJson, PathConfig::class.java)
            } catch (e: Exception) {
                Log.e("RainbowPath", "Failed to load config", e)
                config = PathConfig()
            }
        }
    }

    private fun saveConfig() {
        prefs.edit()
            .putString("config", Gson().toJson(config))
            .apply()
    }

    private fun showSettingsDialog() {
        PathSettingsDialog(
            requireContext(),
            config,
            onConfigChanged = { newConfig ->
                config = newConfig
                pathView.config = config
                saveConfig()
            },
            onCategoryChanged = { category ->
                // Show letter index background when Letters tab is active
                pathView.showLetterIndexBackground = (category == PathSettingsDialog.Category.LETTERS)
                pathView.invalidate()
            }
        ).show()
    }

    fun updateApps() {
        // Always provide the folder structure from rainbowAll so badges can be shown correctly
        val folders = viewModel.rainbowAll.filter { it.folderName != null && it.apps.size > 1 }
        pathView.setFolders(folders)

        if (inFolder) {
            // When in folder, show folder contents
            val folderApps = viewModel.rainbowFiltered.map {
                EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)
            }
            Log.d("RainbowPath", "updateApps (inFolder=true): Setting ${folderApps.size} apps")
            folderApps.forEachIndexed { idx, app ->
                val name = getNameOfApp(app)
                val pkg = if (app.apps.isNotEmpty()) app.apps.first().packageName else "N/A"
                Log.d("RainbowPath", "  FolderApp[$idx] (before sort): '$name' ($pkg)")
            }
            pathView.setApps(folderApps)
        } else {
            // Normal view - combine apps and folders
            viewModel.updateRainbowFiltered(pathView.onlyFavorites)
            val combinedApps = viewModel.rainbowFiltered.map {
                EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)
            }
            pathView.setApps(combinedApps)
        }

        viewModel.icons.value?.let {
            pathView.icons = it
            pathView.onIconsUpdated() // Check if visible app icons have been loaded
        }
        pathView.invalidate()
    }

    private fun getDisplayedApps(): List<EncapsulatedAppInfoWithFolder> {
        // Get apps from viewModel (same as what was passed to pathView.setApps)
        val apps = if (inFolder) {
            viewModel.rainbowFiltered.map { 
                EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite) 
            }
        } else {
            viewModel.rainbowFiltered.map { 
                EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite) 
            }
        }
        
        // Apply same sorting logic as RainbowPathView.getDisplayedApps()
        // This ensures the index from pathView matches the index in this list
        val filtered = if (inFolder) {
            apps
        } else if (pathView.onlyFavorites) {
            // When onlyFavorites is true, show only folders (folderName != null)
            apps.filter { it.folderName != null }
        } else {
            // When onlyFavorites is false, show only individual apps (folderName == null)
            apps.filter { it.folderName == null }
        }

        // Sort based on config (same logic as RainbowPathView)
        val sorted = when (config.appSortOrder) {
            AppSortOrder.ASCENDING -> filtered.sortedBy { 
                val name = if (it.folderName == null) {
                    it.apps.firstOrNull()?.label ?: ""
                } else {
                    it.folderName ?: ""
                }
                name.lowercase()
            }
            AppSortOrder.DESCENDING -> filtered.sortedByDescending { 
                val name = if (it.folderName == null) {
                    it.apps.firstOrNull()?.label ?: ""
                } else {
                    it.folderName ?: ""
                }
                name.lowercase()
            }
        }
        
        return sorted
    }
    
    private fun getNameOfApp(thing: EncapsulatedAppInfoWithFolder): String {
        return if (thing.folderName != null && thing.folderName!!.isNotEmpty()) {
            thing.folderName!!
        } else if (thing.apps.isNotEmpty()) {
            try {
                val firstApp = thing.apps.first()
                val label = firstApp.label
                (label as? String)?.takeIf { it.isNotEmpty() } ?: ""
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
    }

    private fun launchApp(appIndex: Int) {
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: CALLED with appIndex=$appIndex")
        val apps = getDisplayedApps()
        if (appIndex < 0 || appIndex >= apps.size) {
            Log.e("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: BLOCKED - Invalid appIndex=$appIndex (apps.size=${apps.size})")
            return
        }

        val thing = apps[appIndex]
        if (thing.folderName != null) {
            Log.e("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: BLOCKED - Expected app but got folder at index $appIndex")
            return // Should not happen, but safety check
        }

        val app = thing.apps.first()
        val appName = getNameOfApp(thing)
        Log.d("RainbowPath", "launchApp: Launching app at index $appIndex: '$appName' (${app.packageName})")
        
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            Log.d("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: Launch intent found, starting activity for ${app.packageName}")
            // Cancel any pending long press countdown to prevent menu from opening on return
            val countdownWasActive = countdownJob?.isActive == true
            countdownJob?.cancel()
            Log.d("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: Cancelled countdown job (wasActive=$countdownWasActive)")
            // Save scroll position before launching app
            pathView.saveScrollPosition()
            Log.d("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: About to call startActivity()")
            startActivity(launchIntent)
            Log.d("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: startActivity() called")
        } else {
            Log.e("RainbowPathTouch", "MainFragmentRainbowPath.launchApp: BLOCKED - No launch intent found for ${app.packageName}")
        }
    }
    
    private fun openFolder(appIndex: Int) {
        // Cancel any pending long press countdown to prevent shortcuts menu from opening
        countdownJob?.cancel()
        
        val apps = getDisplayedApps()
        if (appIndex < 0 || appIndex >= apps.size) return
        
        val thing = apps[appIndex]
        if (thing.folderName == null) return
        
        // Save current scroll position before entering folder (saves to allAppsScrollOffset or favoritesScrollOffset)
        pathView.saveScrollPosition()
        
        val folderApps = thing.apps
        Log.d("RainbowPath", "Opening folder with ${folderApps.size} apps: ${folderApps.map { it.label }}")
        
        // Set inFolder state FIRST before setting apps
        inFolder = true
        pathView.inFolder = true
        
        // Set folder apps in viewModel
        val folderEncapsulated = folderApps.map { 
            EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite) 
        }.toMutableList()
        Log.d("RainbowPath", "Setting rainbowFiltered with ${folderEncapsulated.size} items")
        viewModel.setRainbowFilteredValues(folderEncapsulated)
        
        // Reset folder scroll to show first app (this sets folderScrollOffset and scrollOffset)
        pathView.resetFolderScroll()
        
        // Update apps - this will use the folder contents from viewModel.rainbowFiltered
        updateApps()
    }

    private fun startLongPressCountdown() {
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.startLongPressCountdown: Starting countdown")
        val wasActive = countdownJob?.isActive == true
        countdownJob?.cancel()
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.startLongPressCountdown: Previous job wasActive=$wasActive, cancelled")
        countdownJob = lifecycleScope.launch {
            Log.d("RainbowPathTouch", "MainFragmentRainbowPath.startLongPressCountdown: Waiting 250ms... (job started at ${System.currentTimeMillis()})")
            try {
                delay(250)
                val currentTime = System.currentTimeMillis()
                Log.d("RainbowPathTouch", "MainFragmentRainbowPath.startLongPressCountdown: 250ms elapsed at $currentTime, calling triggerLongPress")
                pathView.triggerLongPress()
                Log.d("RainbowPathTouch", "MainFragmentRainbowPath.startLongPressCountdown: triggerLongPress completed")
            } catch (e: Exception) {
                Log.d("RainbowPathTouch", "MainFragmentRainbowPath.startLongPressCountdown: Countdown job cancelled or failed: ${e.message}")
            }
        }
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.startLongPressCountdown: New countdown job created, isActive=${countdownJob?.isActive}")
    }

    private fun startCountdownForSettings() {
        settingsCountdownJob?.cancel()
        settingsCountdownJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(250)
            withContext(Dispatchers.Main) {
                showSettingsDialog()
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    private fun stopCountdownForSettings() {
        settingsCountdownJob?.cancel()
    }

    private fun showSearchOverlay() {
        // Get all apps and folders for search
        val allAppsForSearch = mutableListOf<EncapsulatedAppInfoWithFolder>()
        
        // Add all individual apps
        viewModel.appsList.value?.let { apps ->
            allAppsForSearch.addAll(
                apps.map { EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite) }
            )
        }
        
        // Add all folders
        viewModel.rainbowMape.value?.let { folders ->
            allAppsForSearch.addAll(
                folders.map { EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite) }
            )
        }
        
        searchOverlay.setApps(allAppsForSearch)
        viewModel.icons.value?.let {
            searchOverlay.setIcons(it)
        }
        
        searchOverlay.setListener(object : SearchOverlayMaterialView.SearchOverlayListener {
            override fun onAppClicked(app: EncapsulatedAppInfoWithFolder) {
                searchOverlay.hide()
                if (app.folderName == null && app.apps.isNotEmpty()) {
                    // Launch app
                    val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(app.apps.first().packageName)
                    if (launchIntent != null) {
                        pathView.saveScrollPosition()
                        startActivity(launchIntent)
                    }
                } else if (app.folderName != null) {
                    // Open folder - need to find it in current displayed apps
                    // First ensure we're not in folder view
                    if (inFolder) {
                        onBackPressed()
                    }
                    // Switch to favorites view (which shows folders)
                    val wasOnlyFavorites = pathView.onlyFavorites
                    pathView.onlyFavorites = true
                    updateApps()
                    
                    // Find folder in displayed apps
                    val apps = getDisplayedApps()
                    val folderIndex = apps.indexOfFirst { 
                        it.folderName == app.folderName
                    }
                    if (folderIndex >= 0) {
                        openFolder(folderIndex)
                    } else {
                        // Folder not found - restore previous state
                        pathView.onlyFavorites = wasOnlyFavorites
                        updateApps()
                        Log.d("SearchOverlay", "Folder not found in displayed apps: ${app.folderName}")
                    }
                }
            }

            override fun onAppLongPressed(app: EncapsulatedAppInfoWithFolder) {
                // Show shortcuts menu (overlay stays open)
                globalThing = app
                if (app.folderName == null && app.apps.isNotEmpty()) {
                    openShortcutsMenu(app)
                } else if (app.folderName != null) {
                    // Folder options menu
                    val actions = listOf(
                        ShortcutAction(getTranslatedString(R.string.rename_folder), getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24)),
                        ShortcutAction(getTranslatedString(R.string.delete_folder), getDrawable(R.drawable.ic_baseline_delete_24)),
                        if (app.favorite == true) 
                            ShortcutAction(getTranslatedString(R.string.remove_from_favorites), getDrawable(R.drawable.star_fill)) 
                        else 
                            ShortcutAction(getTranslatedString(R.string.add_to_favorites), getDrawable(R.drawable.star_empty))
                    )
                    dialogState = DialogStates.FOLDER_OPTIONS
                    mActivity.showDialogWithActions(actions, this@MainFragmentRainbowPath, searchOverlay)
                }
            }

            override fun onDismiss() {
                searchOverlay.hide()
            }
        })
        
        searchOverlay.show()
    }

    private fun showShortcuts(appIndex: Int) {
        countdownJob?.cancel()
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                val apps = getDisplayedApps()
                if (appIndex < 0 || appIndex >= apps.size) return@withContext

                currentAppIndex = appIndex
                val thing = apps[appIndex]
                globalThing = thing

                if (thing.folderName == null) {
                    // App shortcuts menu
                    openShortcutsMenu(thing)
                } else {
                    // Folder options menu
                    val actions = listOf(
                        ShortcutAction(getTranslatedString(R.string.rename_folder), getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24)),
                        ShortcutAction(getTranslatedString(R.string.delete_folder), getDrawable(R.drawable.ic_baseline_delete_24)),
                        if (thing.favorite == true) 
                            ShortcutAction(getTranslatedString(R.string.remove_from_favorites), getDrawable(R.drawable.star_fill)) 
                        else 
                            ShortcutAction(getTranslatedString(R.string.add_to_favorites), getDrawable(R.drawable.star_empty))
                    )
                    dialogState = DialogStates.FOLDER_OPTIONS
                    mActivity.showDialogWithActions(actions, this@MainFragmentRainbowPath, pathView)
                }

                // Always provide haptic feedback on long press
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
    
    private fun openShortcutsMenu(thing: EncapsulatedAppInfoWithFolder) {
        globalThing = thing
        val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if (launcherApps.hasShortcutHostPermission()) {
            shortcuts = mActivity.getShortcutFromPackage(thing.apps.first().packageName)
        } else {
            shortcuts = emptyList()
        }
        val appDrawable = viewModel.icons.value!![globalThing!!.apps.first().packageName]
        val actions = shortcuts.map { ShortcutAction(it.shortLabel.toString(), appDrawable) }.toMutableList().apply {
            add(ShortcutAction(getTranslatedString(R.string.app_info), getDrawable(R.drawable.ic_outline_info_75)))
            add(if (globalThing!!.apps.first().favorite) 
                ShortcutAction(getTranslatedString(R.string.remove_from_favorites), getDrawable(R.drawable.star_fill)) 
            else 
                ShortcutAction(getTranslatedString(R.string.add_to_favorites), getDrawable(R.drawable.star_empty)))
            add(if (mActivity.isAppAlreadyInMap(globalThing!!.apps.first())) 
                ShortcutAction(getTranslatedString(R.string.remove_from_folder), getDrawable(R.drawable.baseline_folder_off_24)) 
            else 
                ShortcutAction(getTranslatedString(R.string.add_to_folder), getDrawable(R.drawable.ic_baseline_create_new_folder_50)))
        }
        dialogState = DialogStates.APP_SHORTCUTS
        mActivity.showDialogWithActions(actions, this, pathView)
    }
    
    private fun getTranslatedString(id: Int): String {
        return MainActivity.resources2.getString(id)
    }
    
    private fun getDrawable(resourceId: Int): Drawable? {
        return ResourcesCompat.getDrawable(resources, resourceId, null)?.apply {
            when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    setTint(Color.WHITE)
                }
                else -> {
                    setTint(Color.BLACK)
                }
            }
        }
    }

    private fun openShortcut(index: Int) {
        val thing = globalThing ?: return
        
        if (thing.folderName == null) {
            // App shortcuts
            if (index >= shortcuts.size) {
                if (index == shortcuts.size) {
                    // App info
                    val app = thing.apps.first()
                    val intent = android.content.Intent()
                    intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = android.net.Uri.fromParts("package", app.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    mActivity.shortcutPopup?.dismiss()
                } else if (index == shortcuts.size + 1) {
                    // Toggle favorite
                    val app = thing.apps.first()
                    val newFavoriteValue = !app.favorite

                    Log.d("MainFragmentRainbowPath", "=== TOGGLING FAVORITE ===")
                    Log.d("MainFragmentRainbowPath", "App: ${app.label} (${app.packageName})")
                    Log.d("MainFragmentRainbowPath", "Old favorite value: ${app.favorite}")
                    Log.d("MainFragmentRainbowPath", "New favorite value: $newFavoriteValue")

                    // Find the app in ViewModel's appsList and update it
                    val appInList = viewModel.appsList.value?.find { it.packageName == app.packageName }
                    if (appInList != null) {
                        Log.d("MainFragmentRainbowPath", "Found app in appsList, updating favorite to $newFavoriteValue")
                        appInList.favorite = newFavoriteValue
                        // Also update the local copy
                        app.favorite = newFavoriteValue
                    } else {
                        Log.e("MainFragmentRainbowPath", "App NOT FOUND in appsList! This is a bug.")
                        // Still update local copy and database
                        app.favorite = newFavoriteValue
                    }

                    mActivity.saveAppInfo(app)
                    updateApps()
                    mActivity.shortcutPopup?.dismiss()
                } else if (index == shortcuts.size + 2) {
                    // Add/remove from folder
                    if (mActivity.isAppAlreadyInMap(thing.apps.first())) {
                        // Remove from folder
                        val mapa = viewModel.rainbowMape.value!!.find { it.apps.contains(thing.apps.first()) }
                        if (mapa != null) {
                            val azurirana_mapa = mapa.copy(apps = mapa.apps.minus(thing.apps.first()).toMutableList())
                            viewModel.updateRainbowMapa(azurirana_mapa)
                            mActivity.rainbowMapaUpdateItem(azurirana_mapa)
                            updateApps()
                        }
                        mActivity.shortcutPopup?.dismiss()
                    } else {
                        // Show folder selection dialog
                        dialogState = DialogStates.ADDING_TO_FOLDER
                        mActivity.showDialogWithActions(
                            viewModel.rainbowMape.value!!.map { 
                                ShortcutAction(it.folderName, getDrawable(R.drawable.baseline_folder_24)) 
                            }.toMutableList().apply { 
                                add(ShortcutAction("Nova mapa", getDrawable(R.drawable.ic_baseline_create_new_folder_50))) 
                            }, 
                            this, 
                            pathView
                        )
                    }
                }
                return
            }
            
            mActivity.shortcutPopup?.dismiss()
            val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            try {
                launcherApps.startShortcut(
                    shortcuts[index].`package`,
                    shortcuts[index].id,
                    null,
                    null,
                    android.os.Process.myUserHandle()
                )
            } catch (e: Exception) {
                Log.e("RainbowPath", "Failed to start shortcut", e)
            }
        }
    }
    
    fun addAppToMap(map_index: Int) {
        mActivity.shortcutPopup?.dismiss()
        if (map_index >= viewModel.rainbowMape.value!!.size) {
            // New folder
            openCreateFolderDialog()
        } else {
            val mapa = viewModel.rainbowMape.value!![map_index]
            val nova_mapa = mapa.copy(apps = mapa.apps.plus(globalThing!!.apps.first()).toMutableList())
            viewModel.updateRainbowMapa(nova_mapa)
            mActivity.rainbowMapaUpdateItem(nova_mapa)
            updateApps()
        }
    }
    
    fun openCreateFolderDialog() {
        mActivity.openFolderNameMenu(pathView, false, "", false) {
            val novaMapa = RainbowMapa(0, it, mutableListOf(globalThing!!.apps.first()), true)
            mActivity.rainbowMapaInsertItem(novaMapa)
        }
    }
    
    fun folderOptions(index: Int) {
        mActivity.shortcutPopup?.dismiss()
        when (index) {
            0 -> {
                // Rename folder
                val mapa = viewModel.rainbowMape.value!!.find { it.folderName == globalThing!!.folderName }
                mActivity.openFolderNameMenu(pathView, true, mapa!!.folderName, false) { ime ->
                    val nova_mapa = mapa?.copy(folderName = ime)
                    if (nova_mapa != null) {
                        viewModel.updateRainbowMapa(nova_mapa)
                        mActivity.rainbowMapaUpdateItem(nova_mapa)
                    }
                    updateApps()
                }
            }
            1 -> {
                // Delete folder
                val mapa = viewModel.rainbowMape.value!!.find { it.folderName == globalThing!!.folderName }
                if (mapa != null) {
                    viewModel.deleteRainbowMapa(mapa)
                    mActivity.rainbowMapaDeleteItem(mapa)
                }
                updateApps()
            }
            2 -> {
                // Toggle favorite
                val mapa = viewModel.rainbowMape.value!!.find { it.folderName == globalThing!!.folderName }
                val nova_mapa = mapa?.copy(favorite = !mapa.favorite)
                if (nova_mapa != null) {
                    viewModel.updateRainbowMapa(nova_mapa)
                    mActivity.rainbowMapaUpdateItem(nova_mapa)
                }
                updateApps()
            }
        }
    }

    private fun toggleFavorites() {
        // Don't toggle if in folder
        if (inFolder) return
        
        // Toggle favorites state (setter will handle saving/restoring scroll position)
        pathView.onlyFavorites = !pathView.onlyFavorites
        prefs.edit().putBoolean("only_favorites", pathView.onlyFavorites).apply()
        
        // Update apps list to show/hide favorites
        updateApps()
    }

    private fun startFlingAnimation() {
        flingJob?.cancel()
        flingJob = lifecycleScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(16) // ~60fps
                pathView.flingUpdate()
            }
        }
    }

    override fun iconsUpdated() {
        viewModel.icons.value?.let {
            pathView.icons = it
            pathView.invalidate()
            pathView.onIconsUpdated() // Check if visible app icons have been loaded
        }
    }

    override fun selectedItemDeleted() {
        updateApps()
    }

    override fun refreshCurrentMenu() {
        updateApps()
    }

    override fun toggleEditMode() {
        // Not implemented for path view
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save fragment state (inFolder flag)
        outState.putBoolean("in_folder", inFolder)
        // The view will save scroll positions via SharedPreferences in onDetachedFromWindow
        Log.d("MainFragmentRainbowPath", "onSaveInstanceState - saved inFolder=$inFolder")
    }
    
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Restore fragment state
        savedInstanceState?.let {
            inFolder = it.getBoolean("in_folder", false)
            pathView.inFolder = inFolder
            Log.d("MainFragmentRainbowPath", "onViewStateRestored - restored inFolder=$inFolder")
        }
    }

    override fun onResume() {
        super.onResume()
        val resumeTimestamp = System.currentTimeMillis()
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onResume: CALLED at $resumeTimestamp")
        // Cancel any lingering countdown jobs when resuming (safety measure)
        val countdownWasActive = countdownJob?.isActive == true
        val countdownJobInfo = if (countdownJob != null) "exists" else "null"
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onResume: countdownJob=$countdownJobInfo, isActive=$countdownWasActive")
        flingJob?.cancel()
        countdownJob?.cancel()
        settingsCountdownJob?.cancel()
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onResume: Cancelled countdown job (wasActive=$countdownWasActive) at $resumeTimestamp")
    }

    override fun onPause() {
        super.onPause()
        val pauseTimestamp = System.currentTimeMillis()
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onPause: CALLED at $pauseTimestamp")
        // Save scroll position as safety net when fragment pauses
        pathView.saveScrollPosition()
        val countdownWasActive = countdownJob?.isActive == true
        val countdownJobInfo = if (countdownJob != null) "exists" else "null"
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onPause: countdownJob=$countdownJobInfo, isActive=$countdownWasActive")
        flingJob?.cancel()
        countdownJob?.cancel()
        settingsCountdownJob?.cancel()
        Log.d("RainbowPathTouch", "MainFragmentRainbowPath.onPause: Cancelled countdown job (wasActive=$countdownWasActive) at $pauseTimestamp")
        Log.d("MainFragmentRainbowPath", "onPause - scroll position saved")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Save scroll position one final time before destruction
        pathView.saveScrollPosition()
        flingJob?.cancel()
        countdownJob?.cancel()
        settingsCountdownJob?.cancel()
        Log.d("MainFragmentRainbowPath", "onDestroy - scroll position saved")
    }

    override fun onBackPressed(): Boolean {
        // If search overlay is visible, dismiss it first
        if (searchOverlay.visibility == View.VISIBLE) {
            searchOverlay.hide()
            return true
        }
        if (inFolder) {
            goToHome()
            return true
        }
        return false
    }
    
    override fun goToHome() {
        // Restore scroll position when exiting folder
        if (inFolder) {
            pathView.restoreScrollPosition()
        }
        
        inFolder = false
        pathView.inFolder = false
        updateApps()
    }

    override fun onShortcutClick(index: Int) {
        Log.d("RainbowPath", "clicked on shortcut...")
        when (dialogState) {
            DialogStates.APP_SHORTCUTS -> openShortcut(index)
            DialogStates.ADDING_TO_FOLDER -> addAppToMap(index)
            DialogStates.FOLDER_OPTIONS -> folderOptions(index)
            else -> {}
        }
    }

    companion object {
        fun newInstance(): MainFragmentRainbowPath {
            return MainFragmentRainbowPath()
        }
    }
}
