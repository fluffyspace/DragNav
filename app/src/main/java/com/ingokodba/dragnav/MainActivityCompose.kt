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
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.ingokodba.dragnav.MySettingsFragment.Companion.DARK_MODE
import com.ingokodba.dragnav.TopExceptionHandler.ERORI_FILE
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao
import com.ingokodba.dragnav.baza.RainbowMapaDao
import com.ingokodba.dragnav.compose.AppNotification
import com.ingokodba.dragnav.compose.RainbowPathScreen
import com.ingokodba.dragnav.modeli.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
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
class MainActivityCompose : AppCompatActivity(), OnShortcutClick {

    val viewModel: ViewModel by viewModels()

    companion object {
        @Volatile private var instance: MainActivityCompose? = null

        fun getInstance(): MainActivityCompose? {
            return instance
        }

        lateinit var resources2: Resources
    }

    var iconDrawable: Drawable? = null
    var iconBitmap: Bitmap? = null
    var quality_icons = true

    var newApps: MutableList<AppInfo> = mutableListOf()
    var loadIconBool: Boolean = true
    var circleViewLoadIcons: Boolean = true
    val cache_apps: Boolean = true
    var gcolor: Int = 1
    var shortcutPopup: android.widget.PopupWindow? = null

    private var appListener: AppListener? = null
    private var launcherCallbacks: ModelLauncherCallbacks? = null
    private lateinit var launcherApps: android.content.pm.LauncherApps

    // Icon loading executor for async, priority-based icon loading
    private var iconLoadExecutor: IconLoadExecutor? = null

    // Notification broadcast receiver
    private var notificationReceiver: android.content.BroadcastReceiver? = null

    // Compose state for dialogs
    var shortcutDialogActions by mutableStateOf<List<ShortcutAction>?>(null)
    var shortcutDialogListener: OnShortcutClick? = null
    var folderNameDialogState by mutableStateOf<com.ingokodba.dragnav.compose.FolderNameDialogState?>(null)

    // Dialog state tracking
    var dialogState: DialogStates? = null

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
        changeLocale(this)

        // Enable edge-to-edge for Android 15+ compatibility
        enableEdgeToEdge()

        // Set up Compose UI
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    RainbowPathScreen(
                        mainActivity = this@MainActivityCompose,
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel
                    )

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
                }
            }
        }

        circleViewLoadIcons = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(MySettingsFragment.UI_ICONS_TOGGLE, true)

        lifecycleScope.launch(Dispatchers.IO) {
            initializeRoom()
            if (circleViewLoadIcons) loadIcons()
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

        // Register notification broadcast receiver
        notificationReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("MainActivityCompose", "=== Broadcast received ===")
                Log.d("MainActivityCompose", "Action: ${intent?.action}")
                if (intent?.action == NotificationListener.ACTION_NOTIFICATIONS_UPDATED) {
                    Log.d("MainActivityCompose", "Processing notification update")
                    val notificationDataList = intent.getParcelableArrayListExtra<NotificationData>(
                        NotificationListener.EXTRA_NOTIFICATIONS
                    ) ?: emptyList()
                    Log.d("MainActivityCompose", "Received ${notificationDataList.size} notification data items")
                    val notifications = notificationDataList.map { it.toAppNotification() }
                    viewModel.updateNotifications(notifications)
                    Log.d("MainActivityCompose", "Updated ViewModel with ${notifications.size} notifications")
                } else {
                    Log.d("MainActivityCompose", "Action doesn't match, expected: ${NotificationListener.ACTION_NOTIFICATIONS_UPDATED}")
                }
            }
        }
        val notificationFilter = IntentFilter(NotificationListener.ACTION_NOTIFICATIONS_UPDATED)
        Log.d("MainActivityCompose", "Registering receiver for action: ${NotificationListener.ACTION_NOTIFICATIONS_UPDATED}")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, notificationFilter, Context.RECEIVER_NOT_EXPORTED)
            Log.d("MainActivityCompose", "Receiver registered with RECEIVER_NOT_EXPORTED")
        } else {
            registerReceiver(notificationReceiver, notificationFilter)
            Log.d("MainActivityCompose", "Receiver registered (legacy)")
        }

        // Initialize icon loading executor
        iconLoadExecutor = IconLoadExecutor(this, quality_icons)

        // Set up direct callback from NotificationListener
        NotificationListener.onNotificationsChanged = { notifications ->
            Log.d("MainActivityCompose", "Received ${notifications.size} notifications via callback")
            viewModel.updateNotifications(notifications)
        }

        // Request initial notification update from NotificationListener
        Log.d("MainActivityCompose", "Requesting notification update from NotificationListener")
        NotificationListener.getInstance()?.let { service ->
            Log.d("MainActivityCompose", "NotificationListener service instance found, requesting update")
            val requestIntent = Intent("com.ingokodba.dragnav.REQUEST_NOTIFICATION_UPDATE")
            sendBroadcast(requestIntent)
        } ?: run {
            Log.d("MainActivityCompose", "NotificationListener service instance NOT found")
        }
    }

    // ========== ERROR CHECKING ==========

    private fun checkErrors() {
        var trace = ""
        var line: String? = null
        try {
            val appSpecificExternalDir: File = File(getExternalFilesDir(null), ERORI_FILE)
            val reader = BufferedReader(
                InputStreamReader(FileInputStream(appSpecificExternalDir)))

            while (reader.readLine().also { line = it } != null) {
                trace += line + "\n"
            }
            reader.close()
        } catch (fnfe: FileNotFoundException) {
            Log.e("MainActivityCompose", fnfe.toString())
        } catch (ioe: IOException) {
            Log.e("MainActivityCompose", ioe.toString())
        }

        if (trace != "") {
            Log.d("MainActivityCompose", "trace je $trace")
        }
    }

    // ========== LOCALE ==========

    fun changeLocale(c: Context) {
        val config = c.resources.configuration
        val lang: Boolean = PreferenceManager.getDefaultSharedPreferences(c)
            .getBoolean(MySettingsFragment.UI_LANGUAGE_TOGGLE, false)
        val langstr = if (lang) "hr" else "en"
        val locale = Locale(langstr)
        Locale.setDefault(locale)
        config.setLocale(locale)

        c.createConfigurationContext(config)
        resources2 = Resources(c.assets, c.resources.displayMetrics, config)
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

    suspend fun initializeRoom() {
        val db = AppDatabase.getInstance(this@MainActivityCompose)
        val appDao: AppInfoDao = db.appInfoDao()
        val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()

        // Load apps
        val installedApps = loadNewApps()

        if (cache_apps) {
            val allAppsCached = appDao.getAll().toMutableList()

            val appsNotInCache = installedApps.filter { installedApp ->
                allAppsCached.find { cachedApp -> cachedApp.packageName == installedApp.packageName } == null
            }
            newApps.addAll(appsNotInCache)

            val cachedAppsNoLongerInstalled = allAppsCached.filter { cachedApp ->
                installedApps.find { installedApp -> installedApp.packageName == cachedApp.packageName } == null
            }

            for (app in cachedAppsNoLongerInstalled) {
                appDao.delete(app)
            }

            withContext(Dispatchers.Main) {
                viewModel.addApps(installedApps)
            }
        } else {
            withContext(Dispatchers.Main) {
                viewModel.addApps(installedApps)
            }
        }

        // Load rainbow maps (folders)
        val rainbowMape = rainbowMapaDao.getAll().toMutableList()
        if (rainbowMape.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                viewModel.addRainbowMape(rainbowMape)
            }
        }

        Log.d("MainActivityCompose", "initializeRoom completed: ${installedApps.size} apps, ${rainbowMape.size} folders")
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
            val label = activity.label.toString()
            val id = newApps.size

            val appInfo = AppInfo(
                id = id,
                packageName = packageName,
                label = label,
                color = "",
                favorite = false,
                frequency = 0,
                lastLaunched = 0,
                hasShortcuts = false
            )

            newApps.add(appInfo)
        }

        Log.d("MainActivityCompose", "loadNewApps loaded ${newApps.size} apps")
        return newApps
    }

    suspend fun loadIcons() {
        Log.d("MainActivityCompose", "loadIcons start")
        val apps = viewModel.appsList.value ?: return
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

    fun rainbowMapaDeleteItem(polje: RainbowMapa) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivityCompose)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            rainbowMapaDao.delete(polje)
            Log.d("MainActivityCompose", "deleted ${polje.folderName}(${polje.id})")
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

        notificationReceiver?.let {
            unregisterReceiver(it)
        }

        // Clear notification callback
        NotificationListener.onNotificationsChanged = null

        iconLoadExecutor?.shutdown()

        instance = null

        Log.d("MainActivityCompose", "onDestroy")
    }
}
