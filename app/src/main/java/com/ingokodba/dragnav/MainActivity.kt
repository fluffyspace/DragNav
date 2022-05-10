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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao
import com.ingokodba.dragnav.baza.KrugSAplikacijamaDao
import com.ingokodba.dragnav.modeli.Action
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MessageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.Collections.max
import java.util.Collections.min


class MainActivity : AppCompatActivity(){
    val viewModel: ViewModel by viewModels()

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

    var loadIconBool:Boolean = true
    var circleViewLoadIcons:Boolean = true
    var dontLoadApps:Boolean = false
    var appListOpened:Boolean = false
    val cache_apps:Boolean = true
    var selectAppMenuOpened:Boolean = false
    var currentLayout:Layouts = Layouts.LAYOUT_MAIN
    var addingNewAppEvent:MessageEvent? = null

    var shortcutPopup:PopupWindow? = null
    lateinit var pocetna: KrugSAplikacijama

    var backButtonAction:Boolean = false

    var actions: List<Action> = listOf(
        Action(title="SEND_LAST_IMAGE", description = "Send last image", type = ActionTypes.ACTION_SEND_TO_APP),
        Action(title="TIMER", description = "Timer", type = ActionTypes.ACTION_SEND_TO_APP),
    )

    var import_export_action:Int = 0

    lateinit var mainFragment:MainFragment
    lateinit var searchFragment:SearchFragment
    lateinit var activitiesFragment:ActivitiesFragment
    lateinit var actionsFragment:ActionsFragment

    fun changeLocale(c:Context){
        val config = c.resources.configuration
        var lang:Boolean = PreferenceManager.getDefaultSharedPreferences(c).getBoolean(MySettingsFragment.UI_LANGUAGE_TOGGLE, false)
        val langstr = if (lang) "hr" else "en"
        val locale = Locale(langstr)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            config.setLocale(locale)
        else
            config.locale = locale

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            c.createConfigurationContext(config)
        resources2 = Resources(assets, c.resources.displayMetrics, config);
        c.resources.updateConfiguration(config, c.resources.displayMetrics)
    }

    private fun getStringResourceByName(aString: String): String {
        val packageName = packageName
        val resId = this.resources.getIdentifier(aString, "string", packageName)
        var vrati = this.getString(resId)
        if(vrati == null) vrati = aString
        return vrati
    }

    fun loadOnBackButtonPreference(){
        backButtonAction = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(MySettingsFragment.UI_BACKBUTTON, false)
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this as MainActivity

        val darkModeString = getString(R.string.dark_mode)
        val darkModeValues = resources.getStringArray(R.array.dark_mode_values)
        val darkModePreference = PreferenceManager.getDefaultSharedPreferences(this).getString(darkModeString, darkModeValues[3])
        when (darkModePreference) {
            darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            darkModeValues[3] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            else -> {}
        }

        viewModel.initialize()
        changeLocale(this)
        pocetna = KrugSAplikacijama(0, resources2.getString(R.string.home))
        loadOnBackButtonPreference()
        this.setContentView(R.layout.activity_main)

        val icons = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(MySettingsFragment.UI_ICONS_TOGGLE, true)
        circleViewLoadIcons = icons

        supportFragmentManager.commit { setReorderingAllowed(true) }
        loadFragments()

        lifecycleScope.launch(Dispatchers.IO) {
            initializeRoom()
            withContext(Dispatchers.Main) {
                mainFragment.circleView.icons = viewModel.icons.value!!
                mainFragment.goToPocetna()
                mainFragment.updateStuff();
                mainFragment.bottomMenuView.updateTexts(listOf(resources2.getString(R.string.rename), resources2.getString(R.string.delete), resources2.getString(R.string.enter), resources2.getString(R.string.cancel)))
            }
            if(circleViewLoadIcons) loadIcons()
            Log.d("ingo", "initializeRoom after loadicons")
            withContext(Dispatchers.Main) {
                Log.d("ingo", "vm.pocetna ima id " + viewModel.pocetnaId.toString())
                mainFragment.bottomMenuView.updateTexts(listOf(resources2.getString(R.string.rename), resources2.getString(R.string.delete), resources2.getString(R.string.enter), resources2.getString(R.string.cancel)))
                mainFragment.circleView.icons = viewModel.icons.value!!
            }
        }

        val br: AppListener = AppListener()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        intentFilter.addDataScheme("package")
        registerReceiver(br, intentFilter)
    }

    fun packageIsGame(context: Context, packageName: String): Boolean {
        return try {
            val info: ApplicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                info.category == ApplicationInfo.CATEGORY_GAME
            } else {
                // We are suppressing deprecation since there are no other options in this API Level
                @Suppress("DEPRECATION")
                (info.flags and ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Util", "Package info not found for name: " + packageName, e)
            // Or throw an exception if you want
            false
        }
    }

    fun loadFragments(){
        mainFragment = MainFragment()
        searchFragment = SearchFragment()
        activitiesFragment = ActivitiesFragment()
        actionsFragment = ActionsFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, mainFragment, "main")
            .setReorderingAllowed(true)
            .commit()
    }

    fun showMainFragment(){
        supportFragmentManager.popBackStack()
    }

    fun showActionsFragment(){
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.slideup,
                R.anim.fadeout,
                R.anim.fadein,
                R.anim.slidedown
            )
            replace(R.id.settings_container, actionsFragment, "actions")
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    fun showActivitiesFragment(){
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.slideup,
                R.anim.fadeout,
                R.anim.fadein,
                R.anim.slidedown
            )
            replace(R.id.settings_container, activitiesFragment, "activities")
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    fun showSearchFragment(){
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.slidein,
                R.anim.fadeout,
                R.anim.fadein,
                R.anim.slideout
            )
            replace(R.id.settings_container, searchFragment, "search")
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    fun showLayout(id:Layouts){
        Log.d("ingo", "show layout " + id.name + " currently " + currentLayout)
        //findViewById<FrameLayout>(R.id.mainlayout).setBackgroundColor(Color.TRANSPARENT)

        var changeId = true
        if(currentLayout == id) return
        when(id){
            Layouts.LAYOUT_ACTIVITIES -> {
                showActivitiesFragment()
                Log.d("ingo", "showActivitiesFragment")
            }
            Layouts.LAYOUT_ACTIONS -> {
                actionsFragment.actions = actions
                showActionsFragment()
                Log.d("ingo", "showActionsFragment")
            }
            Layouts.LAYOUT_SEARCH -> {
                showSearchFragment()
            }
            Layouts.LAYOUT_MAIN -> {
                showMainFragment()
                mainFragment.circleView.updateDesign()
                mainFragment.circleView.invalidate()
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
                    mainFragment.bottomMenuView.requestLayout()
                    mainFragment.bottomMenuView.invalidate()
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
            val trenutnoPolje = mainFragment.getPolje(viewModel.currentMenu.id)
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
                    mainFragment.refreshCurrentMenu()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //recreate()
        mainFragment.circleView.updateDesign()
        mainFragment.circleView.invalidate()
        Log.d("ingo", "invalidated")
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }

    fun showMyDialog(editSelected:Int) {
        if(editSelected == -1) return
        val fragmentManager = supportFragmentManager
        viewModel.currentSubmenuList[editSelected].let{
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
        }
    }

    fun addFrequency(application: AppInfo){
        val db = AppDatabase.getInstance(this)
        val appDao: AppInfoDao = db.appInfoDao()
        application.frequency++// = application.frequency!! + 1
        application.lastLaunched = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        appDao.update(application)
        val lol = appDao.getAll()
        Log.d("ingo", lol.map{ it.label + " " + it.frequency }.toString())
    }

    fun loadApp(packageName: String){
        if(viewModel.appsList.value!!.find{it.packageName == packageName} != null) return
        val ctx = this
        lifecycleScope.launch(Dispatchers.IO) {
            val applicationInfo = ctx.packageManager.getApplicationInfo(packageName, 0)
            val appName = applicationInfo.loadLabel(ctx.packageManager).toString()
            Log.d("ingo", "loading newly installed app " + appName + " " + packageName)
            val res: Resources = ctx.packageManager.getResourcesForApplication(applicationInfo)
            var colorPrimary: Int = 0
            val icon = res.getDrawableForDensity(
                applicationInfo.icon,
                DisplayMetrics.DENSITY_LOW,
                null
            )
            if (icon != null) {
                colorPrimary = getBestPrimaryColor(icon)
            }
            viewModel.icons.value!![packageName] = icon

            if (appName != packageName) {
                val newApp =
                    AppInfo(
                        applicationInfo.uid,
                        appName,
                        packageName,
                        colorPrimary.toString(),
                        installed = true
                    )

                val db = AppDatabase.getInstance(ctx)
                val appDao: AppInfoDao = db.appInfoDao()
                try {
                    appDao.insertAll(newApp)
                }catch(exception: android.database.sqlite.SQLiteConstraintException){

                }
                withContext(Dispatchers.Main) {
                    viewModel.addApps(mutableListOf(newApp))
                }
                delay(200)
                withContext(Dispatchers.Main) {
                    val index = viewModel.appsList.value!!.indexOf(viewModel.appsList.value!!.find{it.packageName == packageName})
                    activitiesFragment.radapter.notifyItemInserted(index)
                }
            }
        }
    }

    private suspend fun initializeRoom(){
        Log.d("ingo", "initializeRoom start")
        if(dontLoadApps){
            viewModel.listaMenija += pocetna
            viewModel.pocetnaId = pocetna.id
            viewModel.currentMenuId = pocetna.id
            return
        }
        val db = AppDatabase.getInstance(this)
        val recDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        var krugSAplikacijama:List<KrugSAplikacijama> = recDao.getAll()
        if(krugSAplikacijama.size == 0){
            viewModel.pocetnaId = databaseAddNewPolje(pocetna)?.id ?: -1
            withContext(Dispatchers.Main) {
                showIntroPopup()
            }
        } else {
            withContext(Dispatchers.Main) {
                viewModel.listaMenija += krugSAplikacijama
                viewModel.pocetnaId = krugSAplikacijama.first().id
            }
        }
        for(meni in viewModel.listaMenija){
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
        val newAppsInitialize = loadNewApps()
        Log.d("ingo", "grgr " + newAppsInitialize.map{it.label}.toString())

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
                            recDao.delete(polje)
                            viewModel.listaMenija.remove(polje)
                        }
                    }
                }
            }
        }

        Log.d("ingo", "initializeRoom before insertall")
        if(cache_apps && newAppsInitialize.isNotEmpty()){
            for(app in newAppsInitialize){
                Log.d("ingo", "new app " + app.label)
                try {
                    appDao.insertAll(app)
                } catch (e:android.database.sqlite.SQLiteConstraintException) {
                    Log.d("ingo", "caught exception with inserting app " + app.packageName)
                }
            }
        }
        if(circleViewLoadIcons) {
            for (app in viewModel.listaMenija) {
                loadIcon(app.nextIntent)
            }
        }
        Log.d("ingo", "initializeRoom before addall")
        withContext(Dispatchers.Main) {
            viewModel.addApps(newAppsInitialize)
            //mainFragment.circleView.icons = viewModel.icons.value!!
        }
        //radapter.appsList.sortBy { it.label.toString().lowercase() }
        //if(cache_apps){
        Log.d("ingo", "initializeRoom before loadicons")
    }
    fun loadIcon(pname: String, quality:Boolean=true){
        if (pname == "" || viewModel.icons.value!!.containsKey(pname)) return
        try {
            val applicationInfo = packageManager.getApplicationInfo(pname, 0)
            val res: Resources = packageManager.getResourcesForApplication(applicationInfo)
            try {
                val quality_density:Int = if (quality) DisplayMetrics.DENSITY_HIGH else DisplayMetrics.DENSITY_LOW
                val icon = res.getDrawableForDensity(
                    applicationInfo.icon,
                    quality_density,
                    null
                )
                if(icon != null) {
                    viewModel.appsList.value!!.findLast { it.packageName == pname }?.color = getBestPrimaryColor(icon).toString()
                    viewModel.icons.value!![pname] = icon
                } else {
                    viewModel.icons.value!![pname] = resources.getDrawable(R.drawable.ic_baseline_close_50)
                }
            } catch (e: Resources.NotFoundException){}
        } catch (e: PackageManager.NameNotFoundException){}
    }

    fun loadIcons(){
        for(app in viewModel.appsList.value!!) {
            loadIcon(app.packageName)
        }
    }

    fun databaseGetMeniPolja(ids:List<Int>?): List<KrugSAplikacijama>{
        if(ids == null) return listOf()
        val db = AppDatabase.getInstance(this)
        val recDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        val krugSAplikacijama:MutableList<KrugSAplikacijama> = mutableListOf()
        for(id in ids){
            krugSAplikacijama += recDao.findById(id)
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
        return pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
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
        var newApps: MutableList<AppInfo> = mutableListOf()
        var colorPrimary: Int = Color.BLACK
        val packs = packageManager.getInstalledPackages(0)
        for (i in packs.indices) {
            val p = packs[i]
            if (!isSystemPackage(p)) {
                if(isAppLoaded(p.applicationInfo.uid)) {
                    val app = viewModel.appsList.value?.find { it.packageName == p.packageName }
                    //Log.d("ingo", "isAppLoaded " + p.packageName)
                    if(app != null) {
                        app.installed = true
                        //Log.d("ingo", "installed true " + app.packageName)
                    }
                    continue
                }
                val appName = p.applicationInfo.loadLabel(packageManager).toString()
                Log.d("ingo", "loading new app " + appName + " " + p.packageName)
                colorPrimary = Color.BLACK
                val packageName = p.applicationInfo.packageName
                if (appName != packageName.toString()) {

                    newApps.add(AppInfo(p.applicationInfo.uid, appName, packageName, colorPrimary.toString(), installed = true))
                }
            }
        }

        val i = Intent(Intent.ACTION_MAIN, null)
        i.addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = packageManager.queryIntentActivities(i, 0)
        for (ri in allApps) {
            val uid = packageManager.getPackageUid(ri.activityInfo.packageName, 0)
            //Log.d("ingo", "uid " + uid)
            if(isAppLoaded(uid) || newApps.map{it.id}.contains(uid)) {
                viewModel.appsList.value?.find { it.id == uid }?.installed = true
                continue
            }
            Log.d("ingo", ri.activityInfo.packageName)
            colorPrimary = 0
            newApps.add(AppInfo(uid, ri.loadLabel(packageManager).toString(), ri.activityInfo.packageName, colorPrimary.toString(), installed = true))
        }
        return newApps
    }

    fun circleViewToggleAddAppMode(yesOrNo:Int = -1){
        if(yesOrNo != -1) {
            viewModel.addNewAppMode = yesOrNo != 0
        } else {
            viewModel.addNewAppMode = !viewModel.addNewAppMode
        }
        mainFragment.circleView.addAppMode = viewModel.addNewAppMode
        if(viewModel.addNewAppMode) {
            mainFragment.circleView.changeMiddleButtonState(CircleView.MIDDLE_BUTTON_CHECK)
            mainFragment.bottomMenuView.visibility = View.GONE
            mainFragment.addingMenuCancelOk.visibility = View.VISIBLE
        } else {
            mainFragment.circleView.amIHome()
            mainFragment.addingMenuCancelOk.visibility = View.GONE
            mainFragment.bottomMenuView.visibility = View.VISIBLE
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        if (event != null) {
            Log.d("ingo", "on message event " + event.launchIntent + " " + selectAppMenuOpened)
            if(event.draganddrop){
                showLayout(Layouts.LAYOUT_MAIN)
                addingNewAppEvent = event
                circleViewToggleAddAppMode(1)
                Log.d("ingo", "it's dragndrop")
                return
            }
            // kad se dodaje aplikacija pomoću bottom viewa
            if(selectAppMenuOpened){
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
                val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(event.launchIntent)
                startActivity(launchIntent)
            }

        }
    }

    fun toggleAppMenu(open:Int=-1){
        mainFragment.updateStuff()
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
            if(viewModel.editMode){
                mainFragment.changeeditMode()
            } else if(backButtonAction) {
                showLayout(Layouts.LAYOUT_SEARCH)
            } else {
                showLayout(Layouts.LAYOUT_ACTIVITIES)
            }
            mainFragment.updateStuff()
        }
        //super.onBackPressed()
    }

    fun openFolderNameMenu(view: View){
        val contentView = createFolderNameMenu()
        val locations = IntArray(2, {0})
        view.getLocationOnScreen(locations)
        shortcutPopup?.dismiss()
        shortcutPopup = PopupWindow(contentView,
            ListPopupWindow.MATCH_PARENT,
            ListPopupWindow.WRAP_CONTENT, true)
        //shortcutPopup?.animationStyle = R.style.PopupAnimation
        shortcutPopup?.showAtLocation(view, Gravity.CENTER, 0, 0)
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

    fun openAddMenu(){
        val contentView = mainFragment.createAddMenu()
        shortcutPopup?.dismiss()
        shortcutPopup = PopupWindow(contentView,
            ListPopupWindow.WRAP_CONTENT,
            ListPopupWindow.WRAP_CONTENT, true)
        //shortcutPopup?.animationStyle = R.style.PopupAnimation
        shortcutPopup?.showAtLocation(mainFragment.circleView, Gravity.CENTER, 0, 0)
    }

    fun createFolderNameMenu():View{
        val view = LayoutInflater.from(this).inflate(R.layout.popup_folder_name, null)
        view.findViewById<Button>(R.id.popup_folder_cancel).setOnClickListener {
            shortcutPopup?.dismiss()
        }
        view.findViewById<Button>(R.id.popup_folder_submit).setOnClickListener{
            val ime:String = view.findViewById<EditText>(R.id.popup_folder_name).text.toString()
            if(ime != ""){
                Log.d("ingo", "usli")
                // create folder
                lifecycleScope.launch(Dispatchers.IO) {
                    val dodanoPolje = databaseAddNewPolje(KrugSAplikacijama(id=0, text=ime))
                    val trenutnoPolje = mainFragment.getPolje(viewModel.currentMenu.id)
                    if(trenutnoPolje != null && dodanoPolje != null){
                        trenutnoPolje.polja = trenutnoPolje.polja.plus(dodanoPolje.id)
                        databaseUpdateItem(trenutnoPolje)
                        Log.d("ingo", trenutnoPolje.polja.toString())
                    }
                    withContext(Dispatchers.Main){
                        mainFragment.refreshCurrentMenu()
                        shortcutPopup?.dismiss()
                    }
                }

            }
        }
        return view
    }

    fun databaseUpdateItem(polje: KrugSAplikacijama){
        val db = AppDatabase.getInstance(this)
        val recDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        recDao.update(polje)
        var staro_polje = mainFragment.getPolje(polje.id)
        staro_polje = polje
        Log.d("ingo", "updated " + polje.text + "(" + polje.id + ")")
    }

    fun databaseGetItemByRowId(id:Long): KrugSAplikacijama {
        val db = AppDatabase.getInstance(this)
        val recDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        Log.d("ingo", "databaseGetItemByRowId " + id)
        return recDao.findByRowId(id).first()
    }

    fun deleteSelectedItem(editSelected:Int){
        if(editSelected == -1) return
        lifecycleScope.launch(Dispatchers.IO) {
            databaseDeleteById(viewModel.currentSubmenuList[editSelected].id)
            withContext(Dispatchers.Main){
                mainFragment.deYellowAll()
                mainFragment.refreshCurrentMenu()
            }
        }
    }

    fun databaseDeleteById(id: Int){
        val db = AppDatabase.getInstance(this)
        val recDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        mainFragment.getPolje(id)?.let {
            recDao.delete(it)
            viewModel.currentMenu.polja = viewModel.currentMenu.polja.filter { it != id }
            viewModel.listaMenija.filter{ it.id != id }
            recDao.update(viewModel.currentMenu)
            Log.d("ingo", "deleted " + id)
        }
        Log.d("ingo", "deleted- " + id)
    }

    fun databaseAddNewPolje(polje: KrugSAplikacijama): KrugSAplikacijama? {
        polje.id = 0
        Log.d("ingo", "databaseAddNewPolje(" + polje.text + ")")
        val db = AppDatabase.getInstance(this)
        val recDao: KrugSAplikacijamaDao = db.krugSAplikacijamaDao()
        try {
            val rowid = recDao.insertAll(polje)
            val polje = databaseGetItemByRowId(rowid.first())
            viewModel.listaMenija.add(polje)
            return polje
        }catch(exception: android.database.sqlite.SQLiteConstraintException){

        }
        return null
    }
}