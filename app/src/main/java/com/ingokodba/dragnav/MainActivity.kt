package com.ingokodba.dragnav

//import com.example.dragnav.databinding.ActivityMainBinding

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.window.layout.WindowMetricsCalculator
import com.example.dragnav.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao
import com.ingokodba.dragnav.baza.MeniJednoPoljeDao
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MeniJednoPolje
import com.ingokodba.dragnav.modeli.MessageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.Collections.max
import java.util.Collections.min


class MainActivity : AppCompatActivity(){
    val viewModel: NewRAdapterViewModel by viewModels()


    enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }
    companion object{
        lateinit var resources2:Resources
        val ACTION_CANCEL = -1
        val ACTION_LAUNCH = -2
        val ACTION_ADD = -3
        val ACTION_HOME = -4
        val ACTION_ADD_APP = -5
        val ACTION_APPINFO = "appinfo"
        val MENU_UNDEFINED = -1
        val MENU_APPLICATION_OR_FOLDER = 0
        val MENU_SHORTCUT = 1
        val MENU_ACTION = 2
        enum class Layouts {
            LAYOUT_MAIN, LAYOUT_SEARCH, LAYOUT_ACTIVITIES, LAYOUT_SETTINGS
        }
        val ACTION_IMPORT = 1
        val ACTION_EXPORT = 2
    }

    var loadIconBool:Boolean = false
    var circleViewLoadIcons:Boolean = true
    var loadAppsBool:Boolean = true

    var shortcutPopup:PopupWindow? = null

    var appListOpened:Boolean = false
    val cache_apps:Boolean = true
    var selectAppMenuOpened:Boolean = false
    var currentLayout:Layouts = Layouts.LAYOUT_MAIN
    var colorpicker:Boolean = false
    var addingNewApp:MessageEvent? = null

    var roomMeniPolja:List<MeniJednoPolje>? = null
    lateinit var pocetna: MeniJednoPolje
    val gmail = MeniJednoPolje(1, "Gmail", polja=listOf(4,5,6))
    val whatsapp = MeniJednoPolje(2, "Whatsapp", polja=listOf(9,10))
    val postavke = MeniJednoPolje(3, "Postavke", polja=listOf(7,8))
    val gmail_nova = MeniJednoPolje(4, "Nova Poruka")
    val gmail_inbox = MeniJednoPolje(5, "Inbox")
    val gmail_outbox = MeniJednoPolje(6, "Outbox")
    val postavke_wifi = MeniJednoPolje(7, "WiFi")
    val postavke_torch = MeniJednoPolje(8, "Torch")
    val whatsapp_kodbaci = MeniJednoPolje(9, "Kodbači", polja=listOf(11,14,15))
    val whatsapp_sara = MeniJednoPolje(10, "Sara", polja=listOf(11,14,15))
    val whatsapp_akcije = MeniJednoPolje(11, "Posebne akcije", polja=listOf(12,13))
    val whatsapp_akcije_sendpic = MeniJednoPolje(12, "Pošalji zadnju sliku")
    val whatsapp_akcije_sendapetit = MeniJednoPolje(13, "Pošalji 'Dobar tek'")
    val whatsapp_call = MeniJednoPolje(14, "Nazovi")
    val whatsapp_message = MeniJednoPolje(15, "Šalji poruku")
    //var defaultMenus:MutableList<MeniJednoPolje> = mutableListOf(pocetna, gmail, whatsapp, postavke, gmail_nova, gmail_inbox, gmail_outbox, postavke_wifi, postavke_torch, whatsapp_akcije, whatsapp_akcije_sendapetit, whatsapp_call, whatsapp_kodbaci, whatsapp_message, whatsapp_sara, whatsapp_akcije_sendpic)

    var backButtonAction:Boolean = false


    var import_export_action:Int = 0

    lateinit var mainFragment:MainFragment
    lateinit var searchFragment:SearchFragment
    lateinit var activitiesFragment:ActivitiesFragment

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
        changeLocale(this)
        pocetna = MeniJednoPolje(0, resources2.getString(R.string.home))
        loadOnBackButtonPreference()
        this.setContentView(R.layout.activity_main)

        /*val binding: ActivityMainBinding = DataBindingUtil.setContentView(this,
            R.layout.activity_main
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w: Window = window // in Activity's onCreate() for instance
            /*w.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )*/
            //w.statusBarColor = Color.TRANSPARENT
        }
        val icons = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(MySettingsFragment.UI_ICONS_TOGGLE, true)
        circleViewLoadIcons = icons
        /*findViewById<Button>(R.id.edit_mode_button).setOnClickListener{ changeEditMode() }
        findViewById<Button>(R.id.back_button).setOnClickListener{ goBack() }
        findViewById<Button>(R.id.edit_rename_button).setOnClickListener{ showDialog() }
        findViewById<Button>(R.id.edit_delete_button).setOnClickListener{ deleteSelectedItem() }
        findViewById<Button>(R.id.edit_cancel).setOnClickListener{
            enterSelected()
        }*/
        supportFragmentManager.commit { setReorderingAllowed(true) }
        loadFragments()
        //loadMainFragment()


        /*findViewById<Button>(R.id.pocetna_button).setOnClickListener{
            goToPocetna()
        }*/





        //binding.recyclerView.adapter = radapter
        //findViewById<RecyclerView>(R.id.recycler_view).adapter = radapter
        //Toast.makeText(this, "App list loading", Toast.LENGTH_SHORT).show()
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

                //Toast.makeText(c, "App list loaded", Toast.LENGTH_SHORT).show()
            }
        }
        //findViewById<ImageButton>(R.id.plus_button).setOnClickListener { openAddMenu(it) }
    }

    fun loadFragments(){
        mainFragment = MainFragment()
        searchFragment = SearchFragment()
        activitiesFragment = ActivitiesFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, mainFragment, "main")
            .setReorderingAllowed(true)
            .commit()
    }

    fun showMainFragment(){
        supportFragmentManager.popBackStack()
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
        Log.d("ingo", "show layout " + id.name)
        //findViewById<FrameLayout>(R.id.mainlayout).setBackgroundColor(Color.TRANSPARENT)

        var changeId = true
        if(currentLayout == id) return
        when(id){
            Layouts.LAYOUT_ACTIVITIES -> {
                //findViewById<View>(R.id.recycle_container).visibility = View.VISIBLE
                showActivitiesFragment()
                Log.d("ingo", "showActivitiesFragment")
            }
            Layouts.LAYOUT_SEARCH -> {
                //findViewById<View>(R.id.recycle_container).visibility = View.GONE
                showSearchFragment()
            }
            Layouts.LAYOUT_MAIN -> {
                showMainFragment()
                mainFragment.circleView.updateDesign()
                mainFragment.circleView.invalidate()
            }
            Layouts.LAYOUT_SETTINGS -> {
                //findViewById<FrameLayout>(R.id.mainlayout).setBackgroundColor(Color.WHITE)
                //findViewById<View>(R.id.recycle_container).visibility = View.GONE
                //showSettingsFragment()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(import_export_action){
            ACTION_IMPORT -> {
                //importDatabase(data?.data)
            }
            ACTION_EXPORT -> {
                //exportDatabase(data?.data)
            }
        }
        import_export_action = 0
        Log.d("ingo", "onActivityResult " + requestCode + " " + resultCode + " " + data.toString())
    }

    fun startShortcut(shortcut: MeniJednoPolje){
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
                    MeniJednoPolje(
                        id = 0,
                        text = event.text,
                        nextIntent = event.launchIntent,
                        color = event.color
                    )
                )
                if (trenutnoPolje != null) {
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
        mainFragment.circleView.invalidate()
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

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        val widthDp = metrics.bounds.width() /
                resources.displayMetrics.density
        val widthWindowSizeClass = when {
            widthDp < 600f -> WindowSizeClass.COMPACT
            widthDp < 840f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        val heightDp = metrics.bounds.height() /
                resources.displayMetrics.density
        val heightWindowSizeClass = when {
            heightDp < 480f -> WindowSizeClass.COMPACT
            heightDp < 900f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        // Use widthWindowSizeClass and heightWindowSizeClass
    }

    fun goBack(){
        if(viewModel.stack.size > 1){
            val lastitem = viewModel.stack[viewModel.stack.size-2]
            val lastpolje = lastitem.first
            val lastcounter = lastitem.second
            mainFragment.prebaciMeni(lastpolje, lastcounter, true)
            viewModel.stack.removeAt(viewModel.stack.lastIndex)
        }
    }

    suspend fun initializeRoom(){
        Log.d("ingo", "initializeRoom start")
        if(!loadAppsBool){
            viewModel.listaMenija += pocetna
            viewModel.pocetnaId = pocetna.id
            return
        }
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        var meniPolja:List<MeniJednoPolje> = recDao.getAll()
        if(meniPolja.size == 0){
            viewModel.pocetnaId = databaseAddNewPolje(pocetna).id
            withContext(Dispatchers.Main) {
                showIntroPopup()
            }
        } else {
            viewModel.listaMenija += meniPolja
            viewModel.pocetnaId = meniPolja.first().id
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
            Log.d("ingo", "loaded " + (viewModel.appsList.value?.size ?: 0) + " cached apps")
        }

        Log.d("ingo", "initializeRoom before loadnewapps")
        val newApps = loadNewApps()

        // potrebno izbaciti aplikacije koje su deinstalirane
        for( i in (viewModel.appsList.value!!.size-1) downTo 0){
            val pn = viewModel.appsList.value!![i].packageName
            Log.d("ingo", "check for remove " + pn)
            if(!viewModel.appsList.value!![i].installed){
                appDao.delete(viewModel.appsList.value!![i])
                withContext(Dispatchers.Main) {
                    viewModel.removeApp(viewModel.appsList.value!![i])
                }
                Log.d("ingo", "removed " + pn)
                for(polje in meniPolja.reversed()){
                    if(polje.nextIntent == pn){
                        recDao.delete(polje)
                        viewModel.listaMenija.remove(polje)
                    }
                }
            }
        }

        Log.d("ingo", "initializeRoom before insertall")
        if(cache_apps && newApps.isNotEmpty()){
            for(app in newApps){
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
            viewModel.addApps(newApps)
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
                viewModel.icons.value!![pname] = icon
                if(icon != null) {

                    viewModel.appsList.value!!.findLast { it.packageName == pname }?.color = getBestPrimaryColor(icon).toString()
                }
            } catch (e: Resources.NotFoundException){}
        } catch (e: PackageManager.NameNotFoundException){}
    }

    fun loadIcons(){
        for(app in viewModel.appsList.value!!) {
            loadIcon(app.packageName)
        }
    }

    fun databaseGetMeniPolja(ids:List<Int>?): List<MeniJednoPolje>{
        if(ids == null) return listOf()
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        val meniPolja:MutableList<MeniJednoPolje> = mutableListOf()
        for(id in ids){
            meniPolja += recDao.findById(id)
        }
        Log.d("ingo", "getAllMeniPolja")
        meniPolja.forEach{
            Log.d("ingo", "\tuzeli polje  " + it.text)
        }
        return meniPolja
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

    fun getIcon(pname:String):Drawable? {
        //if(radapter.icons.containsKey(pname)) continue
        try {
            return packageManager.getApplicationIcon(pname)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun ucitajIkonu(view:ImageView, pname: String){
        lifecycleScope.launch(Dispatchers.IO) {
            var drawable:Drawable? = null
            try {
                drawable = packageManager.getApplicationIcon(pname)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main){
                if(drawable != null) {
                    view.setImageDrawable(drawable)
                }
            }
        }
    }

    fun getActivityIcon(context: Context, packageName: String?, activityName: String?): Drawable? {
        val pm: PackageManager = context.getPackageManager()
        val intent = Intent()
        intent.component = ComponentName(packageName!!, activityName!!)
        val resolveInfo = pm.resolveActivity(intent, 0)
        return resolveInfo!!.loadIcon(pm)
    }

    private fun isSystemPackage(pkgInfo: PackageInfo): Boolean {
        //Log.d("ingo", "" + pkgInfo.packageName + " " + pkgInfo.applicationInfo.flags + " " + (pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0))
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
    suspend fun loadNewApps(): MutableList<AppInfo>{
        var newApps: MutableList<AppInfo> = mutableListOf()

        val packs = packageManager.getInstalledPackages(0)
        var colorPrimary: Int = Color.BLACK
        for (i in packs.indices) {
            val p = packs[i]
            if (!isSystemPackage(p)) {
                if(isAppLoaded(p.applicationInfo.uid)) {
                    val app = viewModel.appsList.value?.find { it.packageName == p.packageName }
                    Log.d("ingo", "isAppLoaded " + p.packageName)
                    if(app != null) {
                        app.installed = true
                        Log.d("ingo", "installed true " + app.packageName)
                    }
                    continue
                }
                val appName = p.applicationInfo.loadLabel(packageManager).toString()
                Log.d("ingo", "loading new app " + appName)
                val res: Resources = packageManager.getResourcesForApplication(p.applicationInfo)

                if(loadIconBool){
                    val icon = res.getDrawableForDensity(
                        p.applicationInfo.icon,
                        DisplayMetrics.DENSITY_LOW,
                        null
                    )
                    if(icon != null) {
                        colorPrimary = getBestPrimaryColor(icon)
                    }
                    viewModel.icons.value!![p.applicationInfo.packageName] = icon
                } else {
                    colorPrimary = Color.BLACK
                }
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
            Log.d("ingo", "uid " + uid)
            if(isAppLoaded(uid) || newApps.map{it.id}.contains(uid)) {
                viewModel.appsList.value?.find { it.id == uid }?.installed = true
                continue
            }
            Log.d("ingo", ri.activityInfo.packageName)
            if(loadIconBool){
                val icon = ri.loadIcon(packageManager)
                viewModel.icons.value!![ri.activityInfo.packageName] = icon
                if(icon != null) {
                    colorPrimary = getBestPrimaryColor(icon)
                }
            } else {
                colorPrimary = 0
            }
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
        } else {
            mainFragment.circleView.amIHome()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        // Do something
        if (event != null) {
            Log.d("ingo", "on message event " + event.launchIntent + " " + selectAppMenuOpened)
            if(event.draganddrop){
                showLayout(Layouts.LAYOUT_MAIN)
                addingNewApp = event
                circleViewToggleAddAppMode(1)
                Log.d("ingo", "it's dragndrop")
                return
            }
            if(selectAppMenuOpened){
                // izaberi ovu aplikaciju
                addNewApp(event)
                //selectAppMenuOpened = false
                //findViewById<TextView>(R.id.notification).visibility = View.INVISIBLE
            } else {
                // otvori ovu aplikaciju
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
        if(colorpicker){
            showLayout(Layouts.LAYOUT_SETTINGS)
            colorpicker = false
            return
        }
        if(currentLayout != Layouts.LAYOUT_MAIN){
            showLayout(Layouts.LAYOUT_MAIN)
        } else {
            if(backButtonAction) {
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
                    val dodanoPolje = databaseAddNewPolje(MeniJednoPolje(id=0, text=ime))
                    val trenutnoPolje = mainFragment.getPolje(viewModel.currentMenu.id)
                    if(trenutnoPolje != null){
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

    fun databaseUpdateItem(polje: MeniJednoPolje){
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        recDao.update(polje)
        var staro_polje = mainFragment.getPolje(polje.id)
        staro_polje = polje
        Log.d("ingo", "updated " + polje.text + "(" + polje.id + ")")
    }

    fun databaseGetItemByRowId(id:Long): MeniJednoPolje {
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
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
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        mainFragment.getPolje(id)?.let {
            recDao.delete(it)
            viewModel.currentMenu.polja = viewModel.currentMenu.polja.filter { it != id }
            viewModel.listaMenija.filter{ it.id != id }
            recDao.update(viewModel.currentMenu)
            Log.d("ingo", "deleted " + id)
        }
        Log.d("ingo", "deleted- " + id)
    }

    fun databaseAddNewPolje(polje: MeniJednoPolje): MeniJednoPolje {
        polje.id = 0
        Log.d("ingo", "databaseAddNewPolje(" + polje.text + ")")
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        val rowid = recDao.insertAll(polje)
        val polje = databaseGetItemByRowId(rowid.first())
        viewModel.listaMenija.add(polje)
        return polje
    }

    fun pronadiMoguceAkcije(intent:String):List<MeniJednoPolje>{
        return listOf()
    }






}