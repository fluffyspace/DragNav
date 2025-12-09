package com.ingokodba.dragnav

//import com.example.dragnav.databinding.ActivityMainBinding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.*
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.ListPopupWindow
import android.widget.PopupWindow
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ingokodba.dragnav.navigation.NavRoute
import androidx.preference.PreferenceManager

import com.ingokodba.dragnav.compose.AppNavigation
import com.example.dragnav.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ingokodba.dragnav.MySettingsFragment.Companion.DARK_MODE
import com.ingokodba.dragnav.MySettingsFragment.Companion.UI_DESIGN
import com.ingokodba.dragnav.TopExceptionHandler.ERORI_FILE
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao
import com.ingokodba.dragnav.baza.KrugSAplikacijamaDao
import com.ingokodba.dragnav.baza.RainbowMapaDao
import com.ingokodba.dragnav.modeli.*
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
import com.ingokodba.dragnav.rainbow.MainFragmentRainbowPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.Collections.max
import java.util.Collections.min


open class MainActivity : AppCompatActivity(), OnShortcutClick{
    val viewModel: ViewModel by viewModels()
    var uiDesignMode: UiDesignEnum = UiDesignEnum.RAINBOW_RIGHT
    enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }
    companion object{

        // For Singleton instantiation
        @Volatile private var instance: MainActivity? = null

        fun getInstance(): MainActivity? {
            return instance
        }

        lateinit var resources2:Resources
        val ACTION_CANCEL = -1
        val ACTION_LAUNCH = -2
        val ACTION_ADD = -3
        val ACTION_HOME = -4
        val ACTION_ADD_APP = -5
        val ACTION_APPINFO = "appinfo"
        val ACTION_ADD_PRECAC = "add_shortcut"
        val MENU_UNDEFINED = -1
        val MENU_APPLICATION_OR_FOLDER = 0
        val MENU_SHORTCUT = 1
        val MENU_ACTION = 2
        enum class Layouts {
            LAYOUT_MAIN, LAYOUT_SEARCH, LAYOUT_ACTIVITIES, LAYOUT_SETTINGS, LAYOUT_ACTIONS
        }
        enum class ActionTypes {
            ACTION_SEND_TO_APP
        }
        val ACTION_IMPORT = 1
        val ACTION_EXPORT = 2
    }

    var iconDrawable: Drawable? = null
    var iconBitmap: Bitmap? = null
    var quality_icons = true

    var newApps: MutableList<AppInfo> = mutableListOf()

    var loadIconBool:Boolean = true
    var circleViewLoadIcons:Boolean = true
    var dontLoadApps:Boolean = false
    var appListOpened:Boolean = false
    val cache_apps:Boolean = true
    var selectAppMenuOpened:Boolean = false
    var currentLayout:Layouts = Layouts.LAYOUT_MAIN
    var addingNewAppEvent:MessageEvent? = null
    var gcolor:Int = 1
    var shortcutPopup:PopupWindow? = null
    var colorPickerPopup:PopupWindow? = null
    lateinit var pocetna: KrugSAplikacijama

    var backButtonAction:Boolean = false
    private var lastWindowFocusLostTime: Long = 0
    private var lastActivitiesShownTime: Long = 0
    private var lastPauseTime: Long = 0
    private val HOME_BUTTON_DETECTION_WINDOW_MS = 1000L // Window to detect home button press
    private val TOGGLE_COOLDOWN_MS = 300L // Cooldown to prevent rapid toggling (300ms)

    var actions: List<Action> = listOf(
        Action(title="SEND_LAST_IMAGE", description = "Send last image", type = ActionTypes.ACTION_SEND_TO_APP),
        Action(title="TIMER", description = "Timer", type = ActionTypes.ACTION_SEND_TO_APP),
    )

    // shortcuts dialog
    // shortcuts dialog
    var shortcuts: List<ShortcutInfo> = listOf()
    var dialogState: DialogStates? = null


    var import_export_action:Int = 0

    var mainFragment: MainFragmentInterface? = null
    var searchFragment: SearchFragment? = null
    var activitiesFragment: ActivitiesFragment? = null
    var actionsFragment: ActionsFragment? = null
    var fragmentContainer: FragmentContainerView? = null
    
    // Navigation controller for Compose navigation
    var navController: androidx.navigation.NavController? = null

    private var appListener: AppListener? = null
    private var launcherCallbacks: ModelLauncherCallbacks? = null
    private lateinit var launcherApps: android.content.pm.LauncherApps

    // Icon loading executor for async, priority-based icon loading
    private var iconLoadExecutor: IconLoadExecutor? = null

    var shortcutDialogActions by mutableStateOf<List<ShortcutAction>?>(null)
    var shortcutDialogListener: OnShortcutClick? = null

    var folderNameDialogState by mutableStateOf<com.ingokodba.dragnav.compose.FolderNameDialogState?>(null)

    fun showDialogWithActions(actions: List<ShortcutAction>, onShortcutClick: OnShortcutClick, view: View){
        shortcutDialogActions = actions
        shortcutDialogListener = onShortcutClick
    }

    fun openShortcutsMenu(app_index: Int){
        if(uiDesignMode == UiDesignEnum.RAINBOW_RIGHT || uiDesignMode == UiDesignEnum.RAINBOW_LEFT){
            var app = viewModel.rainbowAll.indexOfFirst{it.apps.first().packageName == viewModel.appsList.value!![app_index].packageName}
            if(app == -1){
                // tu treba proći po mapama + app_index ne vrijedi zato jer se u rainbow fragmentu računa samo ona mapa koja je otvorena.
                app = viewModel.rainbowAll.indexOfFirst{it.apps.first().packageName == viewModel.appsList.value!![app_index].packageName}
            }
            if (app != -1) {
                (mainFragment as MainFragmentRainbow).app_index = app
                (mainFragment as MainFragmentRainbow).openShortcutsMenu(viewModel.rainbowAll[app])
            } else {
                Log.e("ingo", "openShortcutsMenu returned null")
            }
        }
        if(uiDesignMode == UiDesignEnum.CIRCLE || uiDesignMode == UiDesignEnum.CIRCLE_LEFT_HAND || uiDesignMode == UiDesignEnum.CIRCLE_RIGHT_HAND){

        }
    }

    fun openShortcut(index: Int){
        if(uiDesignMode == UiDesignEnum.RAINBOW_RIGHT || uiDesignMode == UiDesignEnum.RAINBOW_LEFT){
            (mainFragment as MainFragmentRainbow).openShortcut(index)
        }
    }

    fun isAppAlreadyInMap(whatApp: AppInfo): Boolean{
        return viewModel.rainbowMape.value!!.find { mape -> mape.apps.find{ app -> app.packageName == whatApp.packageName} != null } != null
    }

    override fun onShortcutClick(index: Int) {
        Log.d("ingo", "clicked on shortcut...")
        when(dialogState){
            DialogStates.APP_SHORTCUTS -> openShortcut(index)
            else -> {}
        }
    }

    fun changeLocale(c:Context){
        val config = c.resources.configuration
        var lang:Boolean = PreferenceManager.getDefaultSharedPreferences(c).getBoolean(MySettingsFragment.UI_LANGUAGE_TOGGLE, false)
        val langstr = if (lang) "hr" else "en"
        val locale = Locale(langstr)
        Locale.setDefault(locale)
        config.setLocale(locale)

        c.createConfigurationContext(config)
        resources2 = Resources(assets, c.resources.displayMetrics, config);
        c.resources.updateConfiguration(config, c.resources.displayMetrics)
    }

    private fun getStringResourceByName(aString: String): String {
        val packageName = packageName
        val resId = this.resources.getIdentifier(aString, "string", packageName)
        return this.getString(resId)
    }

    fun loadOnBackButtonPreference(){
        backButtonAction = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(MySettingsFragment.UI_BACKBUTTON, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // ako ovo ne bi bilo prije super.onCreate(savedInstanceState), onCreate funkcija bi se pozivala dva puta i developer bi si počupao kosu jer ne bi znao zašto aplikacija ne radi kako treba
        val darkModeValues = resources.getStringArray(R.array.dark_mode_values)
        when (PreferenceManager.getDefaultSharedPreferences(this).getString(DARK_MODE, darkModeValues[1])) {
            darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            darkModeValues[3] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            else -> {}
        }
        super.onCreate(savedInstanceState)

        Log.d("ingo", "oncreate mainactivity")
        Thread.setDefaultUncaughtExceptionHandler(TopExceptionHandler(this));
        //Thread.getDefaultUncaughtExceptionHandler()

        checkErrors()

        instance = this as MainActivity

        val ui_design_values = resources.let{it.getStringArray(R.array.ui_designs_values)}
        uiDesignMode = when(PreferenceManager.getDefaultSharedPreferences(this).getString(UI_DESIGN, ui_design_values[0])){
            ui_design_values[0] -> UiDesignEnum.RAINBOW_RIGHT
            ui_design_values[1] -> UiDesignEnum.RAINBOW_LEFT
            ui_design_values[2] -> UiDesignEnum.CIRCLE
            ui_design_values[3] -> UiDesignEnum.CIRCLE_RIGHT_HAND
            ui_design_values[4] -> UiDesignEnum.CIRCLE_LEFT_HAND
            ui_design_values[5] -> UiDesignEnum.KEYPAD
            ui_design_values[6] -> UiDesignEnum.RAINBOW_PATH
            else -> UiDesignEnum.CIRCLE
        }

        viewModel.initialize()
        changeLocale(this)
        pocetna = KrugSAplikacijama(0, resources2.getString(R.string.home))
        loadOnBackButtonPreference()
        
        // Enable edge-to-edge for Android 15+ compatibility
        enableEdgeToEdge()
        
        // Set up Compose UI
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                
                // Track current layout state
                var currentLayoutState by remember { mutableStateOf(Layouts.LAYOUT_MAIN) }
                
                // Handle back press
                androidx.activity.compose.BackHandler(enabled = currentLayoutState != Layouts.LAYOUT_MAIN) {
                    if (currentLayoutState != Layouts.LAYOUT_MAIN) {
                        navController.popBackStack()
                        currentLayoutState = Layouts.LAYOUT_MAIN
                    }
                }
                
                AppNavigation(
                    navController = navController,
                    mainActivity = this@MainActivity,
                    currentLayout = currentLayoutState,
                    onLayoutChange = { currentLayoutState = it }
                )
                
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

                com.ingokodba.dragnav.compose.FolderNameDialog(
                    state = folderNameDialogState,
                    onDismissRequest = {
                        folderNameDialogState = null
                    }
                )
            }
        }


        circleViewLoadIcons = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(MySettingsFragment.UI_ICONS_TOGGLE, true)


        loadFragments()

        lifecycleScope.launch(Dispatchers.IO) {
            initializeRoom()
            withContext(Dispatchers.Main) {
                // Wait for mainFragment to be initialized by Compose
                // This will be called after the fragment is created
                // mainFragment.iconsUpdated()
                // mainFragment.goToHome()
            }
            if(circleViewLoadIcons) loadIcons()
            Log.d("ingo", "initializeRoom after loadicons")
            saveNewApps()
            withContext(Dispatchers.Main) {
                Log.d("ingo", "vm.pocetna ima id " + viewModel.pocetnaId.toString())
                // Wait for fragment initialization
                // mainFragment.iconsUpdated()
            }
        }

        // Register LauncherApps.Callback for modern app lifecycle events
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
        launcherCallbacks = ModelLauncherCallbacks()
        launcherApps.registerCallback(launcherCallbacks)

        // Legacy BroadcastReceiver (kept as fallback for older Android versions)
        // Will be completely removed after testing confirms LauncherApps.Callback works
        appListener = AppListener()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        registerReceiver(appListener, intentFilter)

        // Initialize icon loading executor
        iconLoadExecutor = IconLoadExecutor(this, quality_icons)

    }

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
            Log.e("ingo", fnfe.toString())
        } catch (ioe: IOException) {
            Log.e("ingo", ioe.toString())
        }

        if(trace != "") {
            Log.d("ingo", "trace je $trace")
            val sendIntent = Intent(Intent.ACTION_SEND)
            val subject = "Error report"
            val body = """
            ${"Pošalji ovaj zapis na ingokodba@gmail.com: \n$trace"}
            
            """.trimIndent()

            sendIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("ingokodba@gmail.com"))
            sendIntent.putExtra(Intent.EXTRA_TEXT, body)
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            sendIntent.type = "message/rfc822"

            //this@MainActivity.startActivity(Intent.createChooser(sendIntent, "Title:"))
            //this@MainActivity.deleteFile("stack.trace")
        } else {
            Log.d("ingo", "trace je prazan")
        }
    }

    fun saveNewApps(){
        Log.d("ingo", "saveNewApps")
        if(cache_apps && newApps.isNotEmpty()){
            val db = AppDatabase.getInstance(this@MainActivity)
            val appDao: AppInfoDao = db.appInfoDao()
            for(app in newApps){
                //Log.d("ingo", "newapp ${app.label} ${app.color}")
                try {
                    appDao.insertAll(app)
                } catch (e:android.database.sqlite.SQLiteConstraintException) {
                    Log.d("ingo", "caught exception with inserting app " + app.packageName)
                }
            }
        }
    }

    // Fragments are now loaded via Compose navigation, but we keep references for compatibility
    fun loadFragments(){
        // Fragments will be created by Compose composables as needed
        // This method is kept for compatibility but fragments are created lazily
    }

    fun getPolje(id:Int): KrugSAplikacijama?{
        return viewModel.sviKrugovi.find{it.id == id}
    }

    fun showMainFragment(){
        navController?.popBackStack(NavRoute.Main.route, false)
        mainFragment?.refreshCurrentMenu()
    }

    fun showActionsFragment(){
        navController?.navigate(NavRoute.Actions.route)
    }

    fun showActivitiesFragment(){
        navController?.navigate(NavRoute.Activities.route)
    }

    fun showSearchFragment(){
        navController?.navigate(NavRoute.Search.route)
    }




    fun showLayout(id:Layouts){
        Log.d("ingo", "show layout " + id.name + " currently " + currentLayout)
        //findViewById<FrameLayout>(R.id.mainlayout).setBackgroundColor(Color.TRANSPARENT)

        var changeId = true
        if(currentLayout == id) return
        when(id){
            Layouts.LAYOUT_ACTIVITIES -> {
                lastActivitiesShownTime = System.currentTimeMillis()
                Log.d("HomeButtonTest", "showLayout - LAYOUT_ACTIVITIES shown, timestamp: $lastActivitiesShownTime")
                showActivitiesFragment()
                /*WindowCompat.setDecorFitsSystemWindows(window, false)

                val windowInsetsController =
                    ViewCompat.getWindowInsetsController(window.decorView)

                windowInsetsController?.isAppearanceLightNavigationBars = false*/

                //show content behind status bar
                /*window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                //make status bar transparent
                window?.statusBarColor = Color.TRANSPARENT*/

                //fragmentContainer.setPadding()


                Log.d("ingo", "showActivitiesFragment")
            }
            Layouts.LAYOUT_ACTIONS -> {
                actionsFragment?.actions = actions
                showActionsFragment()
                Log.d("ingo", "showActionsFragment")
            }
            Layouts.LAYOUT_SEARCH -> {
                showSearchFragment()
            }
            Layouts.LAYOUT_MAIN -> {
                showMainFragment()
                mainFragment?.refreshCurrentMenu()
            }
            Layouts.LAYOUT_SETTINGS -> {
                val intent = Intent(this, SettingsActivity::class.java)
                resultLauncher.launch(intent)
                changeId = false
            }
        }
        if(changeId){
            currentLayout = id
        }
        return
    }

    fun contentProvideLastPictureInGallery(){
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.MIME_TYPE
        )
        val cursor: Cursor? = contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                null, MediaStore.Images.Media.DATE_ADDED + " DESC"
            )

        if (cursor?.moveToFirst() == true) {
            Log.d("ingo", "entered")
            val imageLocation: String = cursor.getString(1)

            val b = BitmapFactory.decodeFile(imageLocation)

            val share = Intent(Intent.ACTION_SEND)
            share.type = "image/jpeg"
            val bytes = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(contentResolver, b, "Title", null)
            val imageUri: Uri = Uri.parse(path)
            share.putExtra(Intent.EXTRA_STREAM, imageUri)
            startActivity(Intent.createChooser(share, "Select"))
        }
        cursor?.close()
    }

    fun runAction(action: Action){
        when(action.title){
            "SEND_LAST_IMAGE" -> {
                contentProvideLastPictureInGallery()
                Toast.makeText(this, action.title, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // used by settings fragment to apply actions
    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            Log.d("ingo", "registerForActivityResult")
            if(data != null){
                if(data.hasExtra("refresh"))
                {
                    mainFragment?.refreshCurrentMenu()
                }
                if(data.hasExtra("reload_apps"))
                {
                    lifecycleScope.launch(Dispatchers.IO) {
                    initializeRoom()
                        withContext(Dispatchers.Main){
                            mainFragment?.refreshCurrentMenu()
                        }
                    }
                }
                if(data.hasExtra("backButtonAction"))
                {
                    backButtonAction = data.getBooleanExtra("backButtonAction", false)
                }
                if(data.hasExtra("restart"))
                {
                    startActivity(Intent.makeRestartActivityTask(intent?.component));
                }
                if(data.hasExtra("dropDatabase"))
                {
                    Log.d("ingo", "dropDatabase")
                    dropDatabase()
                }
                if(data.hasExtra("loadIcons"))
                {
                    Log.d("ingo", "loadIcons")
                    loadIcons()
                }
            }
        }
    }

    fun dropDatabase(){
        val con = this
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(con).clearAllTables()
        }
        startActivity(Intent.makeRestartActivityTask(this.intent?.component));
    }

    fun startShortcut(shortcut: KrugSAplikacijama){
        val launcherApps: LauncherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        Log.d("ingo", "startshortcut " + shortcut.nextIntent + "->" + shortcut.nextId)
        /*sendIntent.setAction(Intent.ACTION_SEND)
        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.")
        sendIntent.setType("text/plain")*/
        try {
            launcherApps.startShortcut(
                shortcut.nextIntent,
                shortcut.nextId,
                null,
                null,
                android.os.Process.myUserHandle()
            )
        } catch (e:IllegalStateException){
            e.printStackTrace()
            Toast.makeText(this, "IllegalStateException", Toast.LENGTH_SHORT).show()
        } catch (e:android.content.ActivityNotFoundException ){
            e.printStackTrace()
            Toast.makeText(this, "ActivityNotFoundException", Toast.LENGTH_SHORT).show()
        }
    }

    fun addNewApp(event:MessageEvent?){
        if(event != null) {
            val trenutnoPolje = getPolje(viewModel.currentMenu.id)
            lifecycleScope.launch(Dispatchers.IO) {
                val dodanoPolje = databaseAddNewPolje(
                    KrugSAplikacijama(
                        id = 0,
                        text = event.text,
                        nextIntent = event.launchIntent,
                        color = event.color
                    )
                )
                if (trenutnoPolje != null && dodanoPolje != null) {
                    trenutnoPolje.polja = trenutnoPolje.polja.plus(dodanoPolje.id)
                    databaseUpdateItem(trenutnoPolje)
                }
                withContext(Dispatchers.Main) {
                    toggleAppMenu(0)
                    showLayout(Layouts.LAYOUT_MAIN)
                    mainFragment?.refreshCurrentMenu()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lastPauseTime = System.currentTimeMillis()
        Log.d("HomeButtonTest", "onPause called - currentLayout: $currentLayout, timestamp: $lastPauseTime")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("HomeButtonTest", "onNewIntent called - currentLayout: $currentLayout, intent: $intent")
        setIntent(intent)
        
        // Home button toggle disabled - keeping logging for debugging
        Log.d("HomeButtonTest", "onNewIntent - Home button toggle disabled")
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeButtonTest", "onResume called - currentLayout: $currentLayout")
        
        // Home button toggle disabled - keeping logging for debugging
        Log.d("HomeButtonTest", "onResume - Home button toggle disabled")
        
        // Reset pause time after checking
        lastPauseTime = 0
        
        //recreate()
        mainFragment?.refreshCurrentMenu()
        Log.d("ingo", "invalidated")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d("HomeButtonTest", "onWindowFocusChanged - hasFocus: $hasFocus, currentLayout: $currentLayout")
        
        // Home button toggle disabled - keeping logging for debugging
        if (!hasFocus) {
            lastWindowFocusLostTime = System.currentTimeMillis()
            Log.d("HomeButtonTest", "onWindowFocusChanged - Window lost focus, timestamp: $lastWindowFocusLostTime")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        lastWindowFocusLostTime = System.currentTimeMillis()
        Log.d("HomeButtonTest", "onUserLeaveHint called - currentLayout: $currentLayout, timestamp: $lastWindowFocusLostTime")
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_HOME) {
            Log.d("HomeButtonTest", "dispatchKeyEvent - Home button detected - action: ${event.action}, currentLayout: $currentLayout")
            if (event.action == android.view.KeyEvent.ACTION_UP && currentLayout == Layouts.LAYOUT_MAIN) {
                val hasOpenDialogs = (shortcutDialogActions != null) || (shortcutPopup?.isShowing == true) || (colorPickerPopup?.isShowing == true)
                Log.d("HomeButtonTest", "dispatchKeyEvent - hasOpenDialogs: $hasOpenDialogs")
                if (!hasOpenDialogs) {
                    Log.d("HomeButtonTest", "dispatchKeyEvent - Opening LAYOUT_ACTIVITIES")
                    showLayout(Layouts.LAYOUT_ACTIVITIES)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        Log.d("ingo", "onstart main")
        super.onStart()
        EventBus.getDefault().register(this);
    }

    fun checkAllAppsForShortcuts(){
        Log.d("ingo", "checkAllAppsForShortcuts")
        for(app in viewModel.appsList.value!!){
            app.hasShortcuts = getShortcutFromPackage(app.packageName).isNotEmpty()
            saveAppInfo(app)
        }
        Log.d("ingo", "checkAllAppsForShortcuts finished")
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }

    override fun onDestroy() {
        super.onDestroy()

        // Shutdown icon loading executor
        iconLoadExecutor?.shutdown()
        iconLoadExecutor = null

        // Unregister LauncherApps.Callback
        launcherCallbacks?.let {
            try {
                launcherApps.unregisterCallback(it)
            } catch (e: Exception) {
                Log.w("MainActivity", "Error unregistering LauncherApps callback", e)
            }
        }

        // Unregister BroadcastReceiver
        appListener?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Log.w("MainActivity", "Receiver not registered", e)
            }
        }
    }

    fun showMyDialog(editSelected:Int) {
        if(editSelected == -1) return
        viewModel.trenutnoPrikazanaPolja[editSelected].let { krugSAplikacijom ->
            // Use window decor view as fallback if fragmentContainer is null
            val containerView = fragmentContainer ?: window?.decorView?.rootView
            if (containerView != null) {
                openFolderNameMenu(containerView, true, krugSAplikacijom.text, false) { ime ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val novoPolje = krugSAplikacijom.copy(text=ime, color=this@MainActivity.gcolor.toString())
                        databaseUpdateItem(novoPolje)
                        withContext(Dispatchers.Main){
                            mainFragment?.refreshCurrentMenu()
                        }
                    }
                }
            }
        }
        /*
        val fragmentManager = supportFragmentManager
        viewModel.trenutnoPrikazanaPolja[editSelected].let{
            val newFragment = CustomDialogFragment(it)

            val isLargeLayout = true
            if (isLargeLayout) {
                // The device is using a large layout, so show the fragment as a dialog
                newFragment.show(fragmentManager, "dialog")
            } else {
                // The device is smaller, so show the fragment fullscreen
                val transaction = fragmentManager.beginTransaction()
                // For a little polish, specify a transition animation
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                // To make it fullscreen, use the 'content' root view as the container
                // for the fragment, which is always the root view for the activity
                transaction
                    .add(android.R.id.content, newFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }*/
    }

    fun saveAppInfo(application: AppInfo){
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val appDao: AppInfoDao = db.appInfoDao()
            appDao.update(application)
            //val lol = appDao.getAll()
            //Log.d("ingo", lol.map { it.label + " " + it.frequency }.toString())
        }
    }

    fun addFrequency(application: AppInfo){
        val db = AppDatabase.getInstance(this)
        val appDao: AppInfoDao = db.appInfoDao()
        application.frequency++// = application.frequency!! + 1
        application.lastLaunched = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        appDao.update(application)
        //val lol = appDao.getAll()
        //Log.d("ingo", lol.map{ it.label + " " + it.frequency }.toString())
    }

    fun isPackageNameOnDevice(packageName: String): Boolean{
        return try{
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException ){
            false
        }
    }

    fun loadApp(packageName: String){
        if(viewModel.appsList.value!!.find{it.packageName == packageName} != null ||
           viewModel.currentlyLoadingApps.contains(packageName)){
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
                loadIcon(packageName)

                // Save to database
                val db = AppDatabase.getInstance(ctx)
                val appDao: AppInfoDao = db.appInfoDao()
                try {
                    appDao.insertAll(newApp)
                } catch(exception: android.database.sqlite.SQLiteConstraintException){
                    Log.d("ingo", "App already in database: ${exception.message}")
                }

                // Add to ViewModel
                withContext(Dispatchers.Main) {
                    viewModel.addApps(mutableListOf(newApp))
                }

                delay(200)

                // Notify UI
                withContext(Dispatchers.Main) {
                    val index = viewModel.appsList.value!!.indexOf(
                        viewModel.appsList.value!!.find{it.packageName == packageName}
                    )
                    activitiesFragment?.radapter?.notifyItemInserted(index)
                }
            } catch (e: Exception) {
                Log.e("ingokodba", "Error loading app $packageName", e)
            } finally {
                viewModel.currentlyLoadingApps.remove(packageName)
            }
        }
    }

    private suspend fun initializeRoom(){
        Log.d("ingo", "initializeRoom start")
        viewModel.sviKrugovi = mutableListOf()
        if(viewModel.appsList.value?.size!! > 0) {
            withContext(Dispatchers.Main) {
                viewModel.clearApps()
            }
        }
        viewModel.highestId = 0
        if(dontLoadApps){
            viewModel.sviKrugovi += pocetna
            viewModel.pocetnaId = pocetna.id
            viewModel.currentMenuId = pocetna.id
            return
        }
        rainbowMapaGetItems()
        val db = AppDatabase.getInstance(this)
        val krugSAplikacijamaDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        var krugSAplikacijama:List<KrugSAplikacijama> = krugSAplikacijamaDao.getAll()
        if(krugSAplikacijama.size == 0){
            viewModel.pocetnaId = databaseAddNewPolje(pocetna)?.id ?: -1
            withContext(Dispatchers.Main) {
                //showIntroPopup()
            }
        } else {
            withContext(Dispatchers.Main) {
                viewModel.sviKrugovi += krugSAplikacijama
                viewModel.pocetnaId = krugSAplikacijama.first().id
            }
        }
        for(meni in viewModel.sviKrugovi){
            if(meni.id > viewModel.highestId) viewModel.highestId = meni.id
        }
        val appDao: AppInfoDao = db.appInfoDao()
        Log.d("ingo", "initializeRoom before cache")
        if(cache_apps) {
            val cached_apps = appDao.getAll() as MutableList<AppInfo>
            for(app in cached_apps){
                app.installed = false
            }
            withContext(Dispatchers.Main) {
                viewModel.addApps(cached_apps)
            }
            Log.d("ingo", "loadedaa " + cached_apps.map{it.label}.toString() + " cached apps in mainactivity")
            Log.d("ingo", "loaded " + (viewModel.appsList.value?.size ?: 0) + " cached apps to viewmodel")
        }

        Log.d("ingo", "initializeRoom before loadnewapps")
        newApps = loadNewApps()
        Log.d("ingo", "grgr " + newApps.map{it.label}.toString())

        // potrebno izbaciti aplikacije koje su deinstalirane
        val remove_duplicates = true
        if(remove_duplicates) {
            for (i in (viewModel.appsList.value!!.size - 1) downTo 0) {
                val pn = viewModel.appsList.value!![i].packageName
                //Log.d("ingo", "check for remove " + pn)
                if (!viewModel.appsList.value!![i].installed) {
                    appDao.delete(viewModel.appsList.value!![i])
                    withContext(Dispatchers.Main) {
                        viewModel.removeApp(viewModel.appsList.value!![i])
                    }
                    Log.d("ingo", "removed " + pn)
                    for (polje in krugSAplikacijama.reversed()) {
                        if (polje.nextIntent == pn) {
                            krugSAplikacijamaDao.delete(polje)
                            viewModel.sviKrugovi.remove(polje)
                        }
                    }
                }
            }
        }

        Log.d("ingo", "initializeRoom before loadIcon for each app")
        if(circleViewLoadIcons) {
            for (app in viewModel.sviKrugovi) {
                loadIcon(app.nextIntent)
            }
        }

        Log.d("ingo", "initializeRoom before addall")
        withContext(Dispatchers.Main) {
            viewModel.addApps(newApps)
        }
        //radapter.appsList.sortBy { it.label.toString().lowercase() }
        //if(cache_apps){
        Log.d("ingo", "initializeRoom before loadicons")

        checkAllAppsForShortcuts()
    }

    fun appDeleted(packageName: String){
        lifecycleScope.launch(Dispatchers.IO) {
            // Verify app is really deleted (quick check)
            if(isPackageNameOnDevice(packageName)) {
                Log.d("ingo", "Package not deleted, it's still on the device")
                return@launch
            }

            val db = AppDatabase.getInstance(this@MainActivity)
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
                    activitiesFragment?.radapter?.notifyDataSetChanged()
                    Log.v("ingokodba", "App removed from UI: $packageName")
                }
            }

            // Remove from menu items (KrugSAplikacijama)
            val krugSAplikacijamaDao = db.krugSAplikacijamaDao()
            val krugSAplikacijama = krugSAplikacijamaDao.getAll()
            for (polje in krugSAplikacijama) {
                if (polje.nextIntent == packageName) {
                    krugSAplikacijamaDao.delete(polje)
                    viewModel.sviKrugovi.remove(polje)
                    Log.v("ingokodba", "Removed menu item for: $packageName")
                }
            }
        }
    }

    // New methods for ModelLauncherCallbacks support

    fun updateApp(packageName: String) {
        Log.d("ingo", "Updating app: $packageName")
        lifecycleScope.launch(Dispatchers.IO) {
            // Reload the app info and icon
            val appLoader = AppLoader(this@MainActivity)
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
                    loadIcon(packageName)

                    // Update database
                    val db = AppDatabase.getInstance(this@MainActivity)
                    val appDao: AppInfoDao = db.appInfoDao()
                    appDao.update(existingApp)

                    withContext(Dispatchers.Main) {
                        activitiesFragment?.radapter?.notifyDataSetChanged()
                    }
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
                val db = AppDatabase.getInstance(this@MainActivity)
                val appDao: AppInfoDao = db.appInfoDao()
                appDao.update(app)
            }
        }
    }

    fun onInstallProgress(packageName: String, progress: Float) {
        Log.v("ingo", "Install progress for $packageName: ${(progress * 100).toInt()}%")
        // Could show progress bar in the future
    }

    fun Bitmap.scaleWith(scale: Float) = Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt(),
        (height * scale).toInt(),
        false
    )
    fun loadIcon(pname: String){
        if (pname == "" || viewModel.icons.value!!.containsKey(pname)) return

        try {
            // Use IconCache for persistent caching
            val iconCache = IconCache(this)
            val (iconDrawable, color) = iconCache.getIcon(pname, quality_icons)

            if (iconDrawable != null) {
                // Update app color
                viewModel.appsList.value?.findLast { it.packageName == pname }?.color = color
                newApps.findLast { it.packageName == pname }?.color = color

                // Store in ViewModel for runtime access
                viewModel.icons.value!![pname] = iconDrawable
            } else {
                // Fallback icon if loading failed
                viewModel.icons.value!![pname] = resources.getDrawable(R.drawable.ic_baseline_close_50)
            }
        } catch (e: Exception) {
            Log.w("ingo", "Error loading icon for $pname", e)
            viewModel.icons.value!![pname] = resources.getDrawable(R.drawable.ic_baseline_close_50)
        }
    }

    fun loadIcons(){
        val apps = viewModel.appsList.value ?: return
        val packageNames = apps.map { it.packageName }

        // Submit bulk icon loading tasks with low priority (background loading)
        iconLoadExecutor?.submitBulk(
            packageNames,
            IconLoadTask.Priority.LOW
        ) { packageName, drawable, color ->
            // Callback executed when icon is loaded
            lifecycleScope.launch(Dispatchers.Main) {
                if (drawable != null) {
                    viewModel.icons.value?.set(packageName, drawable)

                    // Update app color
                    viewModel.appsList.value?.find { it.packageName == packageName }?.color = color
                    newApps.find { it.packageName == packageName }?.color = color
                }
            }
        }

        Log.d("ingo", "Submitted ${packageNames.size} icons for async loading")
    }

    /**
     * Load a single icon with high priority (for immediate display).
     */
    fun loadIconAsync(packageName: String, priority: IconLoadTask.Priority = IconLoadTask.Priority.MEDIUM): IconLoadTask? {
        if (packageName.isEmpty() || viewModel.icons.value?.containsKey(packageName) == true) {
            return null
        }

        return iconLoadExecutor?.submitTask(packageName, priority) { pkgName, drawable, color ->
            lifecycleScope.launch(Dispatchers.Main) {
                if (drawable != null) {
                    viewModel.icons.value?.set(pkgName, drawable)

                    // Update app color
                    viewModel.appsList.value?.find { it.packageName == pkgName }?.color = color
                    newApps.find { it.packageName == pkgName }?.color = color
                }
            }
        }
    }

    fun databaseGetMeniPolja(ids:List<Int>?): List<KrugSAplikacijama>{
        if(ids == null) return listOf()
        val db = AppDatabase.getInstance(this)
        val krugSAplikacijamaDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        val krugSAplikacijama:MutableList<KrugSAplikacijama> = mutableListOf()
        for(id in ids){
            krugSAplikacijama += krugSAplikacijamaDao.findById(id)
        }
        Log.d("ingo", "getAllMeniPolja")
        krugSAplikacijama.forEach{
            Log.d("ingo", "\tuzeli polje  " + it.text)
        }
        return krugSAplikacijama
    }

    fun getShortcutFromPackage(packageName:String): List<ShortcutInfo>{
        val shortcutManager:LauncherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shortcutQuery: LauncherApps.ShortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        shortcutQuery.setPackage(packageName.toString())
        return try {
            shortcutManager.getShortcuts(
                shortcutQuery,
                android.os.Process.myUserHandle()
            ) as MutableList<ShortcutInfo>
        } catch (e: SecurityException){
            Collections.emptyList()
        }
    }

    private fun isSystemPackage(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
    }

    fun isAppLoaded(guid:Int): Boolean{
        return viewModel.appsList.value!!.find{ it.id == guid } != null
    }



    fun getBestPrimaryColor(icon:Drawable): Int{
        val bitmap: Bitmap = icon.toBitmap(5, 5, Bitmap.Config.RGB_565)
        var best_diff_j = 0
        var best_diff_k = 0
        var best_diff = 0
        var current_diff = 0
        for(j in 0..4){
            for(k in 0..4){
                val c = bitmap.getPixel(j,k)
                current_diff = max(listOf(c.red,c.green,c.blue)) - min(listOf(c.red,c.green,c.blue))
                if(current_diff > best_diff){
                    best_diff = current_diff
                    best_diff_j = j
                    best_diff_k = k
                }
            }
        }
        return bitmap.getPixel(best_diff_j, best_diff_k)
    }

    @SuppressWarnings("ResourceType")
    fun loadNewApps(): MutableList<AppInfo>{
        Log.d("ingo", "loadNewApps using LauncherApps API")

        val newApps: MutableList<AppInfo> = mutableListOf()
        val appLoader = AppLoader(this)

        // Load all launcher apps using the modern LauncherApps API
        val allApps = appLoader.loadAllApps(quality_icons)

        for (app in allApps) {
            // Check if app is already loaded
            if (isAppLoaded(app.id)) {
                val existingApp = viewModel.appsList.value?.find { it.packageName == app.packageName }
                if (existingApp != null) {
                    existingApp.installed = true
                }
                continue
            }

            // Add to new apps list
            newApps.add(app)
        }

        Log.d("ingo", "loadNewApps found ${newApps.size} new apps")
        return newApps
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        if (event != null) {
            Log.d("ingo", "on message event " + event.launchIntent + " " + selectAppMenuOpened)
            if(event.type == MessageEventType.DRAG_N_DROP){
                showLayout(Layouts.LAYOUT_MAIN)
                /*addingNewAppEvent = event
                circleViewToggleAddAppMode(1)*/
                Log.d("ingo", "it's dragndrop")
                addNewApp(event)
            } else if(event.type == MessageEventType.LAUNCH_APP){
                if(selectAppMenuOpened){
                    // kad se dodaje aplikacija pomoću bottom viewa
                    // izaberi ovu aplikaciju
                    showLayout(Layouts.LAYOUT_MAIN)
                    Toast.makeText(this, "Adding " + event.text, Toast.LENGTH_SHORT).show()
                    addNewApp(event)
                    //selectAppMenuOpened = false
                    //findViewById<TextView>(R.id.notification).visibility = View.INVISIBLE
                } else {
                    // otvori ovu aplikaciju
                    lifecycleScope.launch(Dispatchers.IO) {
                        addFrequency(event.app)
                    }
                    val launchIntent: Intent? =
                        packageManager.getLaunchIntentForPackage(event.launchIntent)
                    startActivity(launchIntent)
                    showLayout(Layouts.LAYOUT_MAIN)
                }
            } else if(event.type == MessageEventType.FAVORITE){
                saveAppInfo(event.app)
            } else if(event.type == MessageEventType.LONG_HOLD){
                openShortcutsMenu(event.pos)
            }
        }
    }

    fun toggleAppMenu(open:Int=-1){
        //mainFragment.updateStuff()
        selectAppMenuOpened = false
        when(open){
            -1 -> appListOpened = !appListOpened
            0 -> appListOpened = false
            1 -> appListOpened = true
        }
        if(appListOpened){
            showLayout(Layouts.LAYOUT_ACTIVITIES)
        } else {
            showLayout(Layouts.LAYOUT_MAIN)
        }
    }

    override fun onBackPressed() {
        if(currentLayout != Layouts.LAYOUT_MAIN){
            showLayout(Layouts.LAYOUT_MAIN)
        } else {
            val processed = mainFragment?.onBackPressed() ?: false
            if(!processed){
                if(uiDesignMode != UiDesignEnum.RAINBOW_PATH){
                    showLayout(Layouts.LAYOUT_ACTIVITIES)
                }
            }
            /*
            if(viewModel.editMode){
                mainFragment?.toggleEditMode()
            } else if(backButtonAction) {
                showLayout(Layouts.LAYOUT_SEARCH)
            } else {
                showLayout(Layouts.LAYOUT_ACTIVITIES)
            }*/
            //mainFragment?.updateStuff()
        }
        //super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_HOME) {
            Log.d("HomeButtonTest", "Home button pressed - keyCode: $keyCode")
            Log.d("HomeButtonTest", "Current layout: $currentLayout")
            // Home button: open list of apps if on home screen and no dialogs are opened
            if(currentLayout == Layouts.LAYOUT_MAIN){
                Log.d("HomeButtonTest", "On LAYOUT_MAIN, checking for dialogs...")
                // Check if any dialogs are currently showing
                val shortcutShowing = shortcutPopup?.isShowing == true
                val colorPickerShowing = colorPickerPopup?.isShowing == true
                val hasOpenDialogs = shortcutShowing || colorPickerShowing
                Log.d("HomeButtonTest", "shortcutPopup.isShowing: $shortcutShowing")
                Log.d("HomeButtonTest", "colorPickerPopup.isShowing: $colorPickerShowing")
                Log.d("HomeButtonTest", "hasOpenDialogs: $hasOpenDialogs")
                if(!hasOpenDialogs){
                    Log.d("HomeButtonTest", "No dialogs open, opening LAYOUT_ACTIVITIES")
                    // On main view and no dialogs, switch to list of apps
                    showLayout(Layouts.LAYOUT_ACTIVITIES)
                } else {
                    Log.d("HomeButtonTest", "Dialogs are open, not opening activities layout")
                }
            } else {
                Log.d("HomeButtonTest", "Not on LAYOUT_MAIN (current: $currentLayout), doing nothing")
            }
            return true
        }
        Log.d("HomeButtonTest", "Key pressed but not HOME - keyCode: $keyCode")
        return super.onKeyDown(keyCode, event)
    }

    fun openFolderNameMenu(view: View, addingOrEditing: Boolean, name: String, showPickColor: Boolean, callback: (String) -> Unit){
        gcolor = Color.GRAY
        val title = if(addingOrEditing) getString(R.string.editing_a_folder) else getString(R.string.adding_a_folder)
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

    fun showIntroPopup(){
        MaterialAlertDialogBuilder(this,
            androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setMessage(resources.getString(R.string.intro_message))
            .setNegativeButton(resources2.getString(R.string.close)) { dialog, which ->
                // Respond to negative button press
            }
            .show()
    }

    fun showActionsTodoPopup(){
        MaterialAlertDialogBuilder(this,
            androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setMessage(resources.getString(R.string.actions_todo_message))
            .setNegativeButton(resources2.getString(R.string.close)) { dialog, which ->
                // Respond to negative button press
            }
            .show()
    }

    fun createAddMenu():View{
        val view = LayoutInflater.from(applicationContext).inflate(R.layout.popup_add_which, null)
        view.findViewById<LinearLayout>(R.id.new_folder).setOnClickListener{
            //Toast.makeText(this, "New folder", Toast.LENGTH_SHORT).show()
            openFolderNameMenu(view, false, "", false){createCircleFolder(it)}
        }
        view.findViewById<LinearLayout>(R.id.new_shortcut).setOnClickListener{
            //Toast.makeText(this, "New shortcut", Toast.LENGTH_SHORT).show()
            toggleAppMenu()
            selectAppMenuOpened = true
            shortcutPopup?.dismiss()
            showLayout(MainActivity.Companion.Layouts.LAYOUT_ACTIVITIES)
            //changeeditMode()
            //toggleAppMenu()
        }
        return view
    }

    fun openAddMenu(){
        val contentView = createAddMenu()
        shortcutPopup?.dismiss()
        shortcutPopup = PopupWindow(contentView,
            ListPopupWindow.WRAP_CONTENT,
            ListPopupWindow.WRAP_CONTENT, true)
        //shortcutPopup?.animationStyle = R.style.PopupAnimation
        shortcutPopup?.showAtLocation(this@MainActivity.findViewById(R.id.mainlayout), Gravity.CENTER, 0, 0)
    }

    var colorResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if(data != null) {
                gcolor = data.getIntExtra("color", 0)
            }
        }
    }
    fun startColorpicker(){
        val intent = Intent(this@MainActivity, ColorPickerActivity::class.java)
        colorResultLauncher.launch(intent)
    }

    fun createCircleFolder(ime: String){
        lifecycleScope.launch(Dispatchers.IO) {
            val dodanoPolje = databaseAddNewPolje(
                KrugSAplikacijama(
                    id = 0,
                    text = ime,
                    color = gcolor.toString()
                )
            )
            val trenutnoPolje = getPolje(viewModel.currentMenu.id)
            if (trenutnoPolje != null && dodanoPolje != null) {
                trenutnoPolje.polja = trenutnoPolje.polja.plus(dodanoPolje.id)
                databaseUpdateItem(trenutnoPolje)
                Log.d("ingo", trenutnoPolje.polja.toString())
            }
            withContext(Dispatchers.Main) {
                mainFragment?.refreshCurrentMenu()
            }
        }
    }

    fun rainbowMapaUpdateItem(polje: RainbowMapa){
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            rainbowMapaDao.update(polje)
            Log.d("ingo", "updated " + polje.folderName + "(" + polje.id + ")")
        }
    }

    fun rainbowMapaInsertItem(polje: RainbowMapa){
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            val roomId = rainbowMapaDao.insert(polje)
            withContext(Dispatchers.Main){
                viewModel.addRainbowMape(mutableListOf(polje.apply { id = roomId.toInt() }))
                when (mainFragment) {
                    is MainFragmentRainbow -> {
                        (mainFragment as MainFragmentRainbow).saveCurrentMoveDistance()
                        (mainFragment as MainFragmentRainbow).prebaciMeni()
                    }
                    is MainFragmentRainbowPath -> {
                        (mainFragment as MainFragmentRainbowPath).updateApps()
                    }
                }
            }
            Log.d("ingo", "inserted " + polje.folderName + "(" + polje.id + ")")
        }
    }

    fun rainbowMapaDeleteItem(polje: RainbowMapa){
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            rainbowMapaDao.delete(polje)
            Log.d("ingo", "deleted " + polje.folderName + "(" + polje.id + ")")
        }
    }

    fun rainbowMapaGetItems(){
        Log.d("ingo", "rainbowMapaGetItems")
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val rainbowMapaDao: RainbowMapaDao = db.rainbowMapaDao()
            val rainbowMape = rainbowMapaDao.getAll().toMutableList()
            if(rainbowMape.size == 0) return@launch
            withContext(Dispatchers.Main){
                viewModel.addRainbowMape(rainbowMape)
            }
        }
    }

    fun databaseUpdateItem(polje: KrugSAplikacijama){
        val db = AppDatabase.getInstance(this)
        val krugSAplikacijamaDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        krugSAplikacijamaDao.update(polje)
        Log.d("ingo", "updated " + polje.text + "(" + polje.id + ")")
    }

    fun databaseGetItemByRowId(id:Long): KrugSAplikacijama {
        val db = AppDatabase.getInstance(this)
        val krugSAplikacijamaDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        Log.d("ingo", "databaseGetItemByRowId " + id)
        return krugSAplikacijamaDao.findByRowId(id).first()
    }

    fun deleteSelectedItem(editSelected:Int){
        if(editSelected == -1) return
        lifecycleScope.launch(Dispatchers.IO) {
            databaseDeleteById(viewModel.trenutnoPrikazanaPolja[editSelected].id)
            withContext(Dispatchers.Main){
                mainFragment?.selectedItemDeleted()
            }
        }
    }

    fun databaseDeleteById(id: Int){
        val db = AppDatabase.getInstance(this)
        val krugSAplikacijamaDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        getPolje(id)?.let {
            krugSAplikacijamaDao.delete(it)
            viewModel.currentMenu.polja = viewModel.currentMenu.polja.filter { it != id }
            viewModel.sviKrugovi.filter{ it.id != id }
            krugSAplikacijamaDao.update(viewModel.currentMenu)
            Log.d("ingo", "deleted " + id)
        }
        Log.d("ingo", "deleted- " + id)
    }

    fun databaseAddNewPolje(polje: KrugSAplikacijama): KrugSAplikacijama? {
        polje.id = 0
        Log.d("ingo", "databaseAddNewPolje(" + polje.text + ")")
        val db = AppDatabase.getInstance(this)
        val krugSAplikacijamaDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        try {
            val rowid = krugSAplikacijamaDao.insertAll(polje)
            val polje = databaseGetItemByRowId(rowid.first())
            viewModel.sviKrugovi.add(polje)
            return polje
        }catch(exception: android.database.sqlite.SQLiteConstraintException){

        }
        return null
    }
}

enum class DialogStates{APP_SHORTCUTS, ADDING_TO_FOLDER, FOLDER_OPTIONS}

interface OnShortcutClick {
    fun onShortcutClick(index: Int)
}