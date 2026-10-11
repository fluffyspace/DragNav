package com.ingokodba.dragnav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.*
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.ingokodba.dragnav.PreferenceKeys.DARK_MODE
import com.ingokodba.dragnav.TopExceptionHandler.ERORI_FILE
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao
import com.ingokodba.dragnav.baza.RainbowMapaDao
import com.ingokodba.dragnav.compose.AppNavigation
import com.ingokodba.dragnav.compose.AppNotification
import com.ingokodba.dragnav.compose.RainbowPathScreen
import com.ingokodba.dragnav.modeli.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Compose-based MainActivity that displays RainbowPathView
 * This is a completely independent implementation that doesn't rely on the original MainActivity
 * All necessary code is duplicated here for independence
 */
@AndroidEntryPoint
class MainActivityCompose : AppCompatActivity(), OnShortcutClick {

    val viewModel: ViewModel by viewModels()

    companion object {
        @Volatile private var instance: MainActivityCompose? = null

        fun getInstance(): MainActivityCompose? {
            return instance
        }

        lateinit var resources2: Resources
    }

    // Navigation controller for Compose navigation
    var navController: NavHostController? = null

    var iconDrawable: Drawable? = null
    var iconBitmap: Bitmap? = null
    var quality_icons = true

    var newApps: MutableList<AppInfo> = mutableListOf()
    var loadIconBool: Boolean = true
    val cache_apps: Boolean = true
    var gcolor: Int = 1
    var shortcutPopup: android.widget.PopupWindow? = null

    // Status bar appearance
    var statusBarColor by mutableStateOf(android.graphics.Color.TRANSPARENT)
    var statusBarLightText by mutableStateOf(false)

    private var appListener: AppListener? = null
    private var launcherCallbacks: ModelLauncherCallbacks? = null
    private lateinit var launcherApps: android.content.pm.LauncherApps

    // Icon loading executor for async, priority-based icon loading
    private var iconLoadExecutor: IconLoadExecutor? = null

    // Notification broadcast receiver

    // Compose state for dialogs
    var shortcutDialogActions by mutableStateOf<List<ShortcutAction>?>(null)
    var shortcutDialogListener: OnShortcutClick? = null
    var folderNameDialogState by mutableStateOf<com.ingokodba.dragnav.compose.FolderNameDialogState?>(null)

    // Dialog state tracking
    var dialogState: DialogStates? = null

    // Errors dialog state
    var showErrorsDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply dark mode preference
        val darkModeValues = resources.getStringArray(R.array.dark_mode_values)
        when (PreferenceManager.getDefaultSharedPreferences(this).getString(DARK_MODE, darkModeValues[1])) {
            darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            darkModeValues[3] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            else -> {}
        }
        super.onCreate(savedInstanceState)

        Log.d("MainActivityCompose", "onCreate")
        Thread.setDefaultUncaughtExceptionHandler(TopExceptionHandler(this))

        checkErrors()

        instance = this

        viewModel.initialize()
        viewModel.setHiddenApps(loadHiddenAppsFromPrefs())
        changeLocale(this)

        // Enable edge-to-edge for Android 15+ compatibility
        enableEdgeToEdge()

        // Read status bar preferences
        statusBarColor = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt(PreferenceKeys.UI_STATUS_BAR_COLOR, android.graphics.Color.TRANSPARENT)
        statusBarLightText = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PreferenceKeys.UI_STATUS_BAR_LIGHT_TEXT, false)

        // Set up Compose UI with navigation
        setContent {
            MaterialTheme {
                val navControllerState = rememberNavController()
                navController = navControllerState

                // Apply status bar appearance reactively. Settings screens have a
                // white background, so force a white status bar with dark icons
                // there. Other screens follow the user-configured preferences.
                val view = LocalView.current
                val currentRoute = navControllerState.currentBackStackEntryAsState().value?.destination?.route
                val onSettingsScreen = currentRoute?.startsWith("settings") == true
                val effectiveStatusBarColor = if (onSettingsScreen) {
                    android.graphics.Color.WHITE
                } else {
                    this@MainActivityCompose.statusBarColor
                }
                val effectiveLightStatusIcons = if (onSettingsScreen) {
                    false
                } else {
                    this@MainActivityCompose.statusBarLightText
                }
                SideEffect {
                    val window = (view.context as Activity).window
                    @Suppress("DEPRECATION")
                    window.statusBarColor = effectiveStatusBarColor
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveLightStatusIcons
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    // Main navigation content
                    AppNavigation(
                        navController = navControllerState,
                        mainActivity = this@MainActivityCompose,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Global dialog overlays (shown above all screens)

                    // Shortcut dialog overlay
                    if (shortcutDialogActions != null) {
                        com.ingokodba.dragnav.compose.ShortcutDialog(
                            actions = shortcutDialogActions!!,
                            onDismissRequest = {
                                shortcutDialogActions = null
                                shortcutDialogListener = null
                            },
                            onActionClick = { index ->
                                val listener = shortcutDialogListener
                                shortcutDialogActions = null
                                shortcutDialogListener = null
                                listener?.onShortcutClick(index)
                            }
                        )
                    }

                    // Folder name dialog overlay
                    com.ingokodba.dragnav.compose.FolderNameDialog(
                        state = folderNameDialogState,
                        onDismissRequest = {
                            folderNameDialogState = null
                        }
                    )

                    // Errors dialog overlay
                    if (showErrorsDialog) {
                        com.ingokodba.dragnav.compose.ErrorsDialog(
                            onDownload = {
                                downloadErrorsFile()
                                showErrorsDialog = false
                            },
                            onDelete = {
                                deleteErrorsFile()
                                showErrorsDialog = false
                            },
                            onDismiss = {
                                showErrorsDialog = false
                            }
                        )
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // Initialize database flows for reactive updates
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            viewModel.initializeDatabaseFlows(
                appsFlow = db.appInfoDao().getAllFlow(),
                foldersFlow = db.rainbowMapaDao().getAllFlow()
            )

            val loadedApps = initializeRoom()
            loadIcons(loadedApps)
            Log.d("MainActivityCompose", "initializeRoom after loadicons")
            saveNewApps()
        }

        // Register LauncherApps.Callback for modern app lifecycle events
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
        launcherCallbacks = ModelLauncherCallbacks()
        launcherApps.registerCallback(launcherCallbacks)

        // Legacy BroadcastReceiver (kept as fallback for older Android versions)
        appListener = AppListener()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        registerReceiver(appListener, intentFilter)

        // Initialize icon loading executor
        iconLoadExecutor = IconLoadExecutor(this, quality_icons)

        // Set up direct callback from NotificationListener
        Log.d("NL_DEBUG", ">>> MainActivityCompose: Setting up onNotificationsChanged callback <<<")
        NotificationListener.onNotificationsChanged = { notifications ->
            Log.d("MainActivityCompose", "Received ${notifications.size} notifications via callback")
            Log.d("NL_DEBUG", ">>> MainActivityCompose: CALLBACK RECEIVED with ${notifications.size} notifications <<<")
            viewModel.updateNotifications(notifications)
        }
        Log.d("NL_DEBUG", ">>> MainActivityCompose: Callback is now SET <<<")

        // Request initial notification update from NotificationListener
        Log.d("MainActivityCompose", "Requesting notification update from NotificationListener")
        Log.d("NL_DEBUG", ">>> MainActivityCompose: Checking NotificationListener.getInstance() <<<")
        NotificationListener.getInstance()?.let { service ->
            Log.d("MainActivityCompose", "NotificationListener service instance found, requesting update")
            Log.d("NL_DEBUG", ">>> MainActivityCompose: Service instance EXISTS, sending broadcast <<<")
            val requestIntent = Intent("com.ingokodba.dragnav.REQUEST_NOTIFICATION_UPDATE").setPackage(packageName)
            sendBroadcast(requestIntent)
        } ?: run {
            Log.d("MainActivityCompose", "NotificationListener service instance NOT found")
            Log.d("NL_DEBUG", ">>> MainActivityCompose: Service instance is NULL - service not running? <<<")
        }
    }

    // ========== ERROR CHECKING ==========

    private fun checkErrors() {
        val errorsFile = File(getExternalFilesDir(null), ERORI_FILE)
        if (errorsFile.exists() && errorsFile.length() > 0) {
            Log.d("MainActivityCompose", "Errors file found with content")
            showErrorsDialog = true
        }
    }

    // ========== LOCALE ==========

    fun changeLocale(c: Context) {
        val config = c.resources.configuration
        val lang: Boolean = PreferenceManager.getDefaultSharedPreferences(c)
            .getBoolean(PreferenceKeys.UI_LANGUAGE_TOGGLE, false)
        val langstr = if (lang) "hr" else "en"
        val locale = Locale(langstr)
        Locale.setDefault(locale)
        config.setLocale(locale)

        c.createConfigurationContext(config)
        resources2 = Resources(c.assets, c.resources.displayMetrics, config)
        // Also set MainActivity.resources2 for backward compatibility with old fragments
        MainActivity.resources2 = resources2
        c.resources.updateConfiguration(config, c.resources.displayMetrics)
    }

    // ========== DATABASE OPERATIONS ==========

    fun saveNewApps() {
        Log.d("MainActivityCompose", "saveNewApps")
        if (cache_apps && newApps.isNotEmpty()) {
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val appDao: AppInfoDao = db.appInfoDao()
            for (app in newApps) {
                try {
                    appDao.insertAll(app)
                } catch (e: android.database.sqlite.SQLiteConstraintException) {
                    Log.d("MainActivityCompose", "caught exception with inserting app " + app.packageName)
                }
            }
        }
    }

    suspend fun initializeRoom(): List<AppInfo> {
        val db = AppDatabase.getInstance(this@MainActivityCompose)
        val appDao: AppInfoDao = db.appInfoDao()
        val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()

        // Load apps
        val installedApps = loadNewApps()

        val appsForUi: List<AppInfo> = if (cache_apps) {
            val allAppsCached = appDao.getAll().toMutableList()

            // Remove duplicate rows for the same package left over from older versions
            allAppsCached.groupBy { it.packageName }.values
                .filter { it.size > 1 }
                .forEach { dupes -> dupes.drop(1).forEach { dup -> appDao.delete(dup); allAppsCached.remove(dup) } }

            // Merge installed apps with cached data (preserve favorite, frequency, etc.)
            val mergedApps = installedApps.map { installedApp ->
                // Find matching cached app
                val cachedApp = allAppsCached.find { it.packageName == installedApp.packageName }
                if (cachedApp != null) {
                    // Use cached app data (preserves favorite, frequency, color, etc.)
                    cachedApp
                } else {
                    // New app, not in cache
                    newApps.add(installedApp)
                    installedApp
                }
            }.toMutableList()

            val cachedAppsNoLongerInstalled = allAppsCached.filter { cachedApp ->
                installedApps.find { installedApp -> installedApp.packageName == cachedApp.packageName } == null
            }

            for (app in cachedAppsNoLongerInstalled) {
                appDao.delete(app)
            }

            withContext(Dispatchers.Main) {
                viewModel.addApps(mergedApps)
            }
            mergedApps
        } else {
            withContext(Dispatchers.Main) {
                viewModel.addApps(installedApps)
            }
            installedApps
        }

        // Load rainbow maps (folders)
        val rainbowMape = rainbowMapaDao.getAll().toMutableList()
        val installedPackages = installedApps.map { it.packageName }.toSet()
        for (i in rainbowMape.indices) {
            val mapa = rainbowMape[i]
            val kept = mapa.apps.filter { it.packageName in installedPackages }
            if (kept.size != mapa.apps.size) {
                val updated = mapa.copy(apps = kept.toMutableList())
                rainbowMapaDao.update(updated)
                rainbowMape[i] = updated
            }
        }
        if (rainbowMape.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                viewModel.addRainbowMape(rainbowMape)
            }
        }

        Log.d("MainActivityCompose", "initializeRoom completed: ${installedApps.size} apps, ${rainbowMape.size} folders")
        return appsForUi
    }

    fun loadHiddenAppsFromPrefs(): Set<String> {
        val json = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(PreferenceKeys.HIDDEN_APPS, "[]") ?: "[]"
        return try {
            com.google.gson.Gson().fromJson(json, Array<String>::class.java)?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun saveHiddenAppsToPrefs(hidden: Set<String>) {
        val json = com.google.gson.Gson().toJson(hidden.toTypedArray())
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString(PreferenceKeys.HIDDEN_APPS, json).apply()
        viewModel.setHiddenApps(hidden)
    }

    @SuppressWarnings("ResourceType")
    fun loadNewApps(): MutableList<AppInfo> {
        Log.d("MainActivityCompose", "loadNewApps using LauncherApps API")

        val newApps: MutableList<AppInfo> = mutableListOf()
        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userHandle = android.os.Process.myUserHandle()

        val activities = launcherApps.getActivityList(null, userHandle)

        for (activity in activities) {
            val packageName = activity.applicationInfo.packageName
            if (packageName == this.packageName) continue
            if (newApps.any { it.packageName == packageName }) continue
            newApps.add(
                AppInfo(
                    id = 0,
                    packageName = packageName,
                    label = activity.label.toString(),
                    color = "",
                    favorite = false,
                    frequency = 0,
                    lastLaunched = 0,
                    hasShortcuts = false
                )
            )
        }

        Log.d("MainActivityCompose", "loadNewApps loaded ${newApps.size} apps")
        return newApps
    }

    suspend fun loadIcons(apps: List<AppInfo>) {
        Log.d("MainActivityCompose", "loadIcons start")
        val iconsMap = mutableMapOf<String, Drawable?>()

        for (app in apps) {
            try {
                val icon = packageManager.getApplicationIcon(app.packageName)
                iconsMap[app.packageName] = icon

                // Update color if not set
                if (app.color.isEmpty()) {
                    app.color = getBestPrimaryColor(icon).toString()
                }
            } catch (e: Exception) {
                Log.e("MainActivityCompose", "Failed to load icon for ${app.packageName}", e)
                iconsMap[app.packageName] = null
            }
        }

        withContext(Dispatchers.Main) {
            viewModel.setIcons(iconsMap.toMutableMap())
        }

        Log.d("MainActivityCompose", "loadIcons completed: ${iconsMap.size} icons")
    }

    fun getBestPrimaryColor(icon: Drawable): Int {
        val bitmap: Bitmap = icon.toBitmap(5, 5, Bitmap.Config.RGB_565)
        var best_diff_j = 0
        var best_diff_k = 0
        var best_diff = 0
        var current_diff = 0
        for (j in 0..4) {
            for (k in 0..4) {
                val c = bitmap.getPixel(j, k)
                current_diff = maxOf(
                    Color.red(c),
                    Color.green(c),
                    Color.blue(c)
                ) - minOf(
                    Color.red(c),
                    Color.green(c),
                    Color.blue(c)
                )
                if (current_diff > best_diff) {
                    best_diff = current_diff
                    best_diff_j = j
                    best_diff_k = k
                }
            }
        }
        return bitmap.getPixel(best_diff_j, best_diff_k)
    }

    // ========== APP OPERATIONS ==========

    fun saveAppInfo(application: AppInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val appDao: AppInfoDao = db.appInfoDao()
            appDao.update(application)
        }
    }

    fun addFrequency(application: AppInfo) {
        val db = AppDatabase.getInstance(this)
        val appDao: AppInfoDao = db.appInfoDao()
        application.frequency++
        application.lastLaunched = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        appDao.update(application)
    }

    fun isAppAlreadyInMap(whatApp: AppInfo): Boolean {
        return viewModel.rainbowMape.value?.find { mape ->
            mape.apps.find { app -> app.packageName == whatApp.packageName } != null
        } != null
    }

    // ========== SHORTCUTS ==========

    fun getShortcutFromPackage(packageName: String): List<ShortcutInfo> {
        val shortcutManager: LauncherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shortcutQuery: LauncherApps.ShortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        )
        shortcutQuery.setPackage(packageName.toString())
        return try {
            shortcutManager.getShortcuts(
                shortcutQuery,
                android.os.Process.myUserHandle()
            ) as MutableList<ShortcutInfo>
        } catch (e: SecurityException) {
            Collections.emptyList()
        }
    }

    fun showDialogWithActions(actions: List<ShortcutAction>, onShortcutClick: OnShortcutClick, view: View) {
        shortcutDialogActions = actions
        shortcutDialogListener = onShortcutClick
    }

    override fun onShortcutClick(index: Int) {
        Log.d("MainActivityCompose", "onShortcutClick: index=$index")
        // This is called when a shortcut is clicked, but the actual handling
        // is done by the listener passed to showDialogWithActions
    }

    // ========== APP LIFECYCLE OPERATIONS ==========

    fun isPackageNameOnDevice(packageName: String): Boolean {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun loadApp(packageName: String) {
        if (viewModel.appsList.value!!.find { it.packageName == packageName } != null ||
            viewModel.currentlyLoadingApps.contains(packageName)) {
            Log.d("ingokodba", "App already installed or loading: $packageName")
            return
        }

        viewModel.currentlyLoadingApps.add(packageName)
        val ctx = this

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appLoader = AppLoader(ctx)
                val newApp = appLoader.loadApp(packageName, quality_icons)

                if (newApp == null) {
                    Log.w("ingokodba", "Failed to load app: $packageName (not a launcher app?)")
                    viewModel.currentlyLoadingApps.remove(packageName)
                    return@launch
                }

                Log.d("ingokodba", "Loading newly installed app: ${newApp.label} ($packageName)")

                // Load icon
                val iconCache = IconCache(ctx)
                val (iconDrawable, color) = iconCache.getIcon(packageName, quality_icons)
                if (iconDrawable != null) {
                    newApp.color = color
                    withContext(Dispatchers.Main) {
                        viewModel.icons.value!![packageName] = iconDrawable
                    }
                }

                // Save to database
                val db = AppDatabase.getInstance(ctx)
                val appDao: AppInfoDao = db.appInfoDao()
                try {
                    appDao.insertAll(newApp)
                } catch (exception: android.database.sqlite.SQLiteConstraintException) {
                    Log.d("ingo", "App already in database: ${exception.message}")
                }

                // Add to ViewModel
                withContext(Dispatchers.Main) {
                    viewModel.addApps(mutableListOf(newApp))
                }

                delay(200)
            } catch (e: Exception) {
                Log.e("ingokodba", "Error loading app $packageName", e)
            } finally {
                viewModel.currentlyLoadingApps.remove(packageName)
            }
        }
    }

    fun appDeleted(packageName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Verify app is really deleted (quick check)
            if (isPackageNameOnDevice(packageName)) {
                Log.d("ingo", "Package not deleted, it's still on the device")
                return@launch
            }

            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val appDao: AppInfoDao = db.appInfoDao()

            // Remove from database
            val lista = appDao.getAll()
            lista.find { it.packageName == packageName }?.let { app ->
                appDao.delete(app)
                Log.v("ingokodba", "App removed from database: $packageName")
            }

            // Remove from ViewModel
            val app = viewModel.appsList.value?.find { it.packageName == packageName }
            if (app != null) {
                withContext(Dispatchers.Main) {
                    viewModel.removeApp(app)
                    Log.v("ingokodba", "App removed from UI: $packageName")
                }
            }

            // Remove from rainbow maps if present
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            val rainbowMaps = rainbowMapaDao.getAll()
            for (mapa in rainbowMaps) {
                if (mapa.apps.any { it.packageName == packageName }) {
                    val updatedMapa = mapa.copy(apps = mapa.apps.filter { it.packageName != packageName }.toMutableList())
                    rainbowMapaDao.update(updatedMapa)
                    withContext(Dispatchers.Main) {
                        viewModel.updateRainbowMapa(updatedMapa)
                    }
                    Log.v("ingokodba", "Removed app from folder: ${mapa.folderName}")
                }
            }
        }
    }

    fun updateApp(packageName: String) {
        Log.d("ingo", "Updating app: $packageName")
        lifecycleScope.launch(Dispatchers.IO) {
            // Reload the app info and icon
            val appLoader = AppLoader(this@MainActivityCompose)
            val updatedApp = appLoader.loadApp(packageName, quality_icons)

            if (updatedApp != null) {
                // Update existing app in ViewModel
                val existingApp = viewModel.appsList.value?.find { it.packageName == packageName }
                if (existingApp != null) {
                    // Update mutable fields
                    existingApp.color = updatedApp.color
                    existingApp.installed = true

                    // Reload icon (force refresh)
                    viewModel.icons.value?.remove(packageName)
                    val iconCache = IconCache(this@MainActivityCompose)
                    val (iconDrawable, color) = iconCache.getIcon(packageName, quality_icons)
                    if (iconDrawable != null) {
                        withContext(Dispatchers.Main) {
                            viewModel.icons.value!![packageName] = iconDrawable
                        }
                    }

                    // Update database
                    val db = AppDatabase.getInstance(this@MainActivityCompose)
                    val appDao: AppInfoDao = db.appInfoDao()
                    appDao.update(existingApp)
                }
            }
        }
    }

    fun onAppSuspended(packageName: String, suspended: Boolean) {
        Log.d("ingo", "App ${if (suspended) "suspended" else "unsuspended"}: $packageName")
        // For now, just log. Could add visual indication in the future
        // (e.g., gray out suspended apps)
    }

    fun onShortcutsChanged(packageName: String, shortcuts: MutableList<android.content.pm.ShortcutInfo>) {
        Log.d("ingo", "Shortcuts changed for $packageName: ${shortcuts.size} shortcuts")
        // Update hasShortcuts flag
        val app = viewModel.appsList.value?.find { it.packageName == packageName }
        if (app != null) {
            app.hasShortcuts = shortcuts.isNotEmpty()
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@MainActivityCompose)
                val appDao: AppInfoDao = db.appInfoDao()
                appDao.update(app)
            }
        }
    }

    fun onInstallProgress(packageName: String, progress: Float) {
        Log.v("ingo", "Install progress for $packageName: ${(progress * 100).toInt()}%")
        // Could show progress bar in the future
    }

    // ========== FOLDER OPERATIONS ==========

    fun openFolderNameMenu(view: View, addingOrEditing: Boolean, name: String, showPickColor: Boolean, callback: (String) -> Unit) {
        gcolor = Color.GRAY
        val title = if (addingOrEditing) getString(R.string.editing_a_folder) else getString(R.string.adding_a_folder)
        folderNameDialogState = com.ingokodba.dragnav.compose.FolderNameDialogState(
            title = title,
            initialName = name,
            showPickColor = showPickColor,
            onSubmit = { folderName ->
                callback(folderName)
            },
            onPickColor = if (showPickColor) {
                { startColorpicker() }
            } else {
                null
            }
        )
    }

    var colorResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                gcolor = data.getIntExtra("color", 0)
            }
        }
    }

    fun startColorpicker() {
        val intent = Intent(this@MainActivityCompose, ColorPickerActivity::class.java)
        colorResultLauncher.launch(intent)
    }

    var statusBarColorResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getIntExtra("color", 0)?.let { color ->
                statusBarColor = color
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putInt(PreferenceKeys.UI_STATUS_BAR_COLOR, color).apply()
            }
        }
    }

    fun startStatusBarColorPicker() {
        val intent = Intent(this@MainActivityCompose, ColorPickerActivity::class.java)
        statusBarColorResultLauncher.launch(intent)
    }

    fun clearStatusBarColor() {
        statusBarColor = android.graphics.Color.TRANSPARENT
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putInt(PreferenceKeys.UI_STATUS_BAR_COLOR, android.graphics.Color.TRANSPARENT).apply()
    }

    // Activity result launcher for downloading errors file
    private val errorsDownloadLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            try {
                val errorsFile = File(getExternalFilesDir(null), ERORI_FILE)
                if (errorsFile.exists()) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(errorsFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d("MainActivityCompose", "Errors file downloaded successfully")
                }
            } catch (e: Exception) {
                Log.e("MainActivityCompose", "Failed to download errors file", e)
            }
        }
    }

    private fun downloadErrorsFile() {
        errorsDownloadLauncher.launch(ERORI_FILE)
    }

    private fun deleteErrorsFile() {
        try {
            val errorsFile = File(getExternalFilesDir(null), ERORI_FILE)
            if (errorsFile.exists()) {
                errorsFile.delete()
                Log.d("MainActivityCompose", "Errors file deleted")
            }
        } catch (e: Exception) {
            Log.e("MainActivityCompose", "Failed to delete errors file", e)
        }
    }

    fun rainbowMapaUpdateItem(polje: RainbowMapa) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            rainbowMapaDao.update(polje)
            Log.d("MainActivityCompose", "updated ${polje.folderName}(${polje.id})")
        }
    }

    fun rainbowMapaInsertItem(polje: RainbowMapa) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            val roomId = rainbowMapaDao.insert(polje)
            withContext(Dispatchers.Main) {
                viewModel.addRainbowMape(mutableListOf(polje.apply { id = roomId.toInt() }))
            }
            Log.d("MainActivityCompose", "inserted ${polje.folderName}(${polje.id})")
        }
    }

    /**
     * Update KrugSAplikacijama item in database
     */
    fun databaseUpdateItem(item: KrugSAplikacijama) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val dao = db.krugSAplikacijamaDao()
            dao.update(item)
            Log.d("MainActivityCompose", "updated ${item.text}(${item.id})")
        }
    }

    fun rainbowMapaDeleteItem(polje: RainbowMapa) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            rainbowMapaDao.delete(polje)
            Log.d("MainActivityCompose", "deleted ${polje.folderName}(${polje.id})")
        }
    }

    // ========== NAVIGATION ==========

    /**
     * Navigate to the actions screen
     */
    fun navigateToActions() {
        navController?.navigate(com.ingokodba.dragnav.navigation.NavRoute.Actions.route)
    }

    /**
     * Navigate back to the main screen
     */
    fun navigateToMain() {
        navController?.popBackStack(com.ingokodba.dragnav.navigation.NavRoute.Main.route, false)
    }

    /**
     * Navigate back (pop back stack)
     */
    fun navigateBack(): Boolean {
        return navController?.popBackStack() ?: false
    }

    /**
     * Navigate to settings
     */
    fun navigateToSettings() {
        navController?.navigate(com.ingokodba.dragnav.navigation.NavRoute.Settings.route)
    }

    /**
     * Open default apps settings
     */
    fun openDefaultApps() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    /**
     * Drop/reset database
     */
    fun dropDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(this@MainActivityCompose)
                db.close()
                db.clearAllTables()
                db.setInstanceToNull()
                Log.d("MainActivityCompose", "Database dropped successfully")
            } catch (e: Exception) {
                Log.e("MainActivityCompose", "Failed to drop database", e)
            }
        }
    }

    /**
     * Backup database to file
     */
    fun to_backup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "backup.sqlite")
            type = "application/x-sqlite3"
        }
        backupLauncher.launch(intent)
    }

    /**
     * Restore database from file
     */
    fun from_backup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        restoreLauncher.launch(intent)
    }

    // Activity result launchers for backup/restore
    private val backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(this@MainActivityCompose)
                    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                    val currentDB = getDatabasePath(AppDatabase.DATABASE_NAME)
                    contentResolver.openOutputStream(uri, "w")?.use { out ->
                        FileInputStream(currentDB).use { it.copyTo(out) }
                    }
                    Log.d("MainActivityCompose", "Backup successful")
                } catch (e: Exception) {
                    Log.e("MainActivityCompose", "Backup failed", e)
                }
            }
        }
    }

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                val db = AppDatabase.getInstance(this)
                try {
                    db.close()
                    val currentDBPath = getDatabasePath(AppDatabase.DATABASE_NAME).path
                    val currentDB = File(currentDBPath)

                    contentResolver.openFileDescriptor(uri, "r")?.use {
                        FileInputStream(it.fileDescriptor).use { fis ->
                            FileOutputStream(currentDB).use { fos ->
                                fos.channel.transferFrom(fis.channel, 0, fis.channel.size())
                            }
                        }
                    }
                    Log.d("MainActivityCompose", "Restore successful")
                    db.setInstanceToNull()
                    recreate() // Restart app after restore
                } catch (e: Exception) {
                    Log.e("MainActivityCompose", "Restore failed", e)
                }
            }
        }
    }

    /**
     * Backup SharedPreferences to JSON file
     */
    fun to_backup_prefs() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "preferences.json")
            type = "application/json"
        }
        prefsBackupLauncher.launch(intent)
    }

    /**
     * Restore SharedPreferences from JSON file
     */
    fun from_backup_prefs() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        prefsRestoreLauncher.launch(intent)
    }

    private val prefsBackupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                try {
                    val all = PreferenceManager.getDefaultSharedPreferences(this).all
                    val typed = mutableMapOf<String, Map<String, Any?>>()
                    for ((k, v) in all) {
                        val type = when (v) {
                            is Boolean -> "boolean"
                            is Int -> "int"
                            is Long -> "long"
                            is Float -> "float"
                            is String -> "string"
                            is Set<*> -> "stringSet"
                            null -> "null"
                            else -> "string"
                        }
                        val value: Any? = if (v is Set<*>) v.filterIsInstance<String>() else v
                        typed[k] = mapOf("type" to type, "value" to value)
                    }
                    val json = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(typed)
                    contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            fos.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Log.d("MainActivityCompose", "Prefs backup successful (${typed.size} entries)")
                } catch (e: Exception) {
                    Log.e("MainActivityCompose", "Prefs backup failed", e)
                }
            }
        }
    }

    private val prefsRestoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                try {
                    val text = contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: return@also

                    val mapType = com.google.gson.reflect.TypeToken
                        .getParameterized(
                            MutableMap::class.java,
                            String::class.java,
                            com.google.gson.reflect.TypeToken.getParameterized(
                                MutableMap::class.java,
                                String::class.java,
                                Any::class.java
                            ).type
                        ).type
                    val parsed: MutableMap<String, MutableMap<String, Any?>> =
                        com.google.gson.Gson().fromJson(text, mapType)

                    val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
                    editor.clear()
                    for ((key, entry) in parsed) {
                        val type = entry["type"] as? String ?: continue
                        val value = entry["value"]
                        when (type) {
                            "boolean" -> editor.putBoolean(key, value as? Boolean ?: continue)
                            "int" -> editor.putInt(key, (value as? Number)?.toInt() ?: continue)
                            "long" -> editor.putLong(key, (value as? Number)?.toLong() ?: continue)
                            "float" -> editor.putFloat(key, (value as? Number)?.toFloat() ?: continue)
                            "string" -> editor.putString(key, value as? String)
                            "stringSet" -> {
                                @Suppress("UNCHECKED_CAST")
                                val list = value as? List<String> ?: continue
                                editor.putStringSet(key, list.toSet())
                            }
                            "null" -> editor.putString(key, null)
                        }
                    }
                    editor.apply()
                    Log.d("MainActivityCompose", "Prefs restore successful (${parsed.size} entries)")
                    recreate()
                } catch (e: Exception) {
                    Log.e("MainActivityCompose", "Prefs restore failed", e)
                }
            }
        }
    }

    /**
     * Start shortcut
     */
    fun startShortcut(item: KrugSAplikacijama) {
        Log.d("MainActivityCompose", "startShortcut: ${item.text}")

        val launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcherApps.startShortcut(
                item.nextIntent, // package name
                item.nextId,      // shortcut id
                null,
                null,
                android.os.Process.myUserHandle()
            )
        } catch (e: Exception) {
            Log.e("MainActivityCompose", "Error launching shortcut", e)
            android.widget.Toast.makeText(this,
                getString(R.string.failed_launch_shortcut, item.text),
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // ========== LIFECYCLE ==========

    override fun onStart() {
        super.onStart()
        Log.d("MainActivityCompose", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivityCompose", "onResume")
        Log.d("NL_DEBUG", ">>> onResume: Callback is ${if (NotificationListener.onNotificationsChanged != null) "SET" else "NULL"} <<<")
        Log.d("NL_DEBUG", ">>> onResume: Service instance is ${if (NotificationListener.getInstance() != null) "AVAILABLE" else "NULL"} <<<")

        // Re-request notifications on resume in case they changed while paused
        NotificationListener.getInstance()?.let { service ->
            Log.d("NL_DEBUG", ">>> onResume: Re-requesting notification update <<<")
            val requestIntent = Intent("com.ingokodba.dragnav.REQUEST_NOTIFICATION_UPDATE").setPackage(packageName)
            sendBroadcast(requestIntent)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivityCompose", "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister callbacks
        launcherCallbacks?.let {
            launcherApps.unregisterCallback(it)
        }

        appListener?.let {
            unregisterReceiver(it)
        }

        // Clear notification callback
        NotificationListener.onNotificationsChanged = null

        iconLoadExecutor?.shutdown()

        instance = null

        Log.d("MainActivityCompose", "onDestroy")
    }
}
