package com.example.dragnav

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Browser
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.MotionEvent.ACTION_UP
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.baza.AppDatabase
import com.example.dragnav.baza.AppInfoDao
import com.example.dragnav.baza.MeniJednoPoljeDao
import com.example.dragnav.modeli.AppInfo
import com.example.dragnav.modeli.MeniJednoPolje
import com.example.dragnav.modeli.MessageEvent
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Collections.max
import java.util.Collections.min


class MainActivity : AppCompatActivity() {

    val MENU_UNDEFINED = -1
    val MENU_APPLICATION_OR_FOLDER = 0
    val MENU_SHORTCUT = 1
    val MENU_ACTION = 2

    companion object{
        val ACTION_CANCEL = -1
        val ACTION_LAUNCH = -2
        val ACTION_ADD = -3
        val ACTION_HOME = -4
    }

    val LAYOUT_MAIN = 0
    val LAYOUT_SEARCH = 1
    val LAYOUT_ACTIVITIES = 2

    var loadIconBool:Boolean = false
    var loadAppsBool:Boolean = true
    var lastTextViewEnteredCounter:Int = -1
    lateinit var currentMenu: MeniJednoPolje
    var currentSubmenuList: List<MeniJednoPolje> = listOf()
    var currentMenuType: Int = MENU_UNDEFINED
    var max_subcounter:Int = -1
    var stack:MutableList<Pair<Int,Int>> = mutableListOf()
    var shortcutPopup:PopupWindow? = null
    var highestId = -1
    var selected_global:Int = -1
    var lastEnteredIntent:MeniJednoPolje? = null

    var editMode:Boolean = false
    var editSelected:Int = -1
    var appListOpened:Boolean = false
    var pocetnaId:Int = -1
    val cache_apps:Boolean = true

    var selectAppMenuOpened:Boolean = false
    var circleView:CircleView? = null
    var currentLayout:Int = LAYOUT_MAIN
    lateinit var lista_aplikacija: MutableList<Pair<Int, AppInfo>>


    var roomMeniPolja:List<MeniJednoPolje>? = null

    /*val pocetna: MeniPolja = MeniPolja(0, mutableListOf(MeniJednoPolje("Gmail", 1), MeniJednoPolje("Whatsapp", 2)))
    val gmail: MeniPolja = MeniPolja(1, mutableListOf(
        MeniJednoPolje("Nova poruka", nextIntent = "novaporuka"),
        MeniJednoPolje("Inbox", nextIntent = "inbox"),
        MeniJednoPolje("Outbox", nextIntent = "outbox"),
        MeniJednoPolje("Skice", nextIntent = "skice"),
        MeniJednoPolje("Odgovori na zadnju poruku", nextIntent = "lastrespond")
    ))
    val whatsapp: MeniPolja = MeniPolja(2, mutableListOf(
        MeniJednoPolje("Zadnji razgovor", nextId = 3, nextIntent = "whatsapp_last"),
        MeniJednoPolje("Kodbači", nextIntent = "whatsapp_kodbaci"),
        MeniJednoPolje("Sara Tepeš", nextIntent = "whatsapp_sara"),
        MeniJednoPolje("Nataša", nextIntent = "whatsapp_natasa"),
        MeniJednoPolje("Stanko", nextIntent = "whatsapp_stanko")
    ))
    val whatsapp_last: MeniPolja = MeniPolja(3, mutableListOf(
        MeniJednoPolje("Pošalji zadnju sliku", nextIntent = "whatsapp_last_image"),
        MeniJednoPolje("Pošalji česti odgovor", nextIntent = "whatsapp_last_common")
    ))*/
    val pocetna:MeniJednoPolje = MeniJednoPolje(0, "Home")
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
    var defaultMenus:MutableList<MeniJednoPolje> = mutableListOf(pocetna, gmail, whatsapp, postavke, gmail_nova, gmail_inbox, gmail_outbox, postavke_wifi, postavke_torch, whatsapp_akcije, whatsapp_akcije_sendapetit, whatsapp_call, whatsapp_kodbaci, whatsapp_message, whatsapp_sara, whatsapp_akcije_sendpic)
    var listaMenija:MutableList<MeniJednoPolje> = mutableListOf()
    lateinit var radapter:RAdapter
    lateinit var chipGroup:ChipGroup

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w: Window = window // in Activity's onCreate() for instance
            /*w.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )*/
            //w.statusBarColor = Color.TRANSPARENT
        }
        findViewById<Button>(R.id.edit_mode_button).setOnClickListener{ changeEditMode() }
        findViewById<Button>(R.id.back_button).setOnClickListener{ goBack() }
        findViewById<Button>(R.id.edit_rename_button).setOnClickListener{ showDialog() }
        findViewById<Button>(R.id.edit_delete_button).setOnClickListener{ deleteSelectedItem() }
        findViewById<Button>(R.id.edit_cancel).setOnClickListener{
            prebaciMeni(selected_global, selected_global)
            deYellowAll()
        }
        findViewById<Button>(R.id.pocetna_button).setOnClickListener{
            goToPocetna()
        }
        radapter = RAdapter(this)
        findViewById<RecyclerView>(R.id.recycler_view).adapter = radapter
        //Toast.makeText(this, "App list loading", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            initializeRoom()
            withContext(Dispatchers.Main){
                goToPocetna()
            }
        }
        circleView = findViewById<CircleView>(R.id.circleview)
        circleView?.radapter = radapter
        circleView?.setEventListener(object :
            com.example.dragnav.CircleView.IMyEventListener {
            override fun onEventOccurred(event: MotionEvent, counter: kotlin.Int) {
                touched(event, counter)
            }
        })
        chipGroup = findViewById<ChipGroup>(R.id.chipgroup_aplikacije)
        val c:Context = this
        findViewById<FloatingActionButton>(R.id.floating_action_button).setOnClickListener{
            val search_bar = findViewById<EditText>(R.id.search_bar)
            search_bar.setText("")
            showLayout(LAYOUT_SEARCH)
            search_bar.apply {
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + search_bar.text.toString()))
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                        //intent.setPackage("com.android.chrome");
                        startActivity(intent)
                        showLayout(LAYOUT_MAIN)
                        search_bar.setText("")
                        return@setOnEditorActionListener true
                    }
                    false
                }
                requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                addTextChangedListener {
                    // find apps
                    if(search_bar.text.toString().length == 0) {
                        chipGroup.removeAllViews()
                        return@addTextChangedListener
                    }
                    lista_aplikacija = mutableListOf<Pair<Int, AppInfo>>()
                    var slova_search = search_bar.text.toString().map{ it }
                    var slova_search_lowercase = search_bar.text.toString().map{ it.lowercaseChar() }


                    for(app in radapter.appsList) {
                        var counter = 0
                        var index_counter = 0
                        for(slovo in app.label.toString().lowercase()){
                            if(slovo == search_bar.text.toString()[index_counter].lowercaseChar()){
                                index_counter++
                                if(index_counter == search_bar.text.toString().length) break
                            }
                        }
                        if(index_counter != search_bar.text.toString().length){
                            continue
                        }
                        if(app.label.toString().first().lowercaseChar() == slova_search_lowercase[0]){
                            counter += 5
                            if(search_bar.text.toString().length == 2) {
                                if (app.label.toString().last().lowercaseChar() == slova_search_lowercase[1]) {
                                    counter += 10
                                    Log.d("ingo", app.label.toString() + " ako počinje s prvim slovom i završava s drugim " + search_bar.text.toString())
                                }
                                val splitano = app.label.split(" ")
                                if (splitano.size > 1 && splitano[1].first().lowercaseChar() == slova_search_lowercase[1]) {
                                    counter += 5
                                    Log.d("ingo", app.label.toString() + " ako je prvo slovo prva riječ, drugo slovo druga riječ " + search_bar.text.toString())
                                }
                                val sslwf = slova_search_lowercase.subList(1, slova_search_lowercase.size
                                )
                                for ((index, slovo) in app.label.iterator().withIndex()) {
                                    if (index == 0) continue
                                    if (slovo.isUpperCase() && sslwf.contains(slovo.lowercaseChar())) {
                                        counter += 6
                                        Log.d("ingo", app.label.toString() + " upper case contains " + search_bar.text.toString())
                                    }
                                }
                            }
                            if(app.label.toString().lowercase().startsWith(search_bar.text.toString().lowercase())){
                                counter += 5*search_bar.text.toString().length
                                Log.d("ingo", app.label.toString() + " startsWith " + search_bar.text.toString())
                            }
                        }
                        var matches = countMatches(app.label.toString().lowercase(), search_bar.text.toString().lowercase())
                        if(matches == search_bar.text.toString().length) {
                            counter += matches*4
                            Log.d(
                                "ingo",
                                "matches " + app.label.toString() + " " + search_bar.text.toString() + " " + matches
                            )
                        }
                        if(counter > 0) lista_aplikacija.add(Pair(counter, app))
                    }

                    /*if(lista_aplikacija.size == 0){
                        for(app in radapter.appsList) {
                            var counter = 0
                        }
                    }
                    for(app in radapter.appsList){
                        var counter = 0

                        val app_slova = app.label.map { it.lowercase().first() }
                        // sadrži u sebi
                        /*for(slovo in slova_search_lowercase){
                            if(app_slova.contains(slovo)){
                                counter++
                                Log.d("ingo", app.label.toString() + " contains " + slovo)
                                break
                            }
                        }*/
                        val app_rijeci = app.label.toString().lowercase().split(" ")
                        for(app_rijec in app_rijeci){
                            // da li počinje s
                            if(app_rijec.startsWith(search_bar.text.toString().lowercase())){
                                counter += 5
                                Log.d("ingo", app.label.toString() + " startsWith " + search_bar.text.toString())
                            }
                            // case matches
                            if(slova_search_lowercase.contains(app_rijec.first().lowercase().first())){
                                if(slova_search.contains(app_rijec.first())){
                                    counter += 4
                                    Log.d("ingo", app.label.toString() + "same case match! " + slova_search + " " + app_rijec.first())
                                } else {
                                    counter += 2
                                    Log.d("ingo", app.label.toString() + " match " + slova_search + " " + app_rijec.first())
                                }
                            }
                        }
                        if(slova_search.size > 1) {
                            var matches = countMatches(app.label.toString(), search_bar.text.toString())*2
                            if(matches > 0) {
                                counter += matches
                                Log.d(
                                    "ingo",
                                    "matches " + app.label.toString() + " " + search_bar.text.toString() + " " + matches
                                )
                            }
                        }
                        if(counter > 0) lista_aplikacija.add(Pair(counter, app))
                    }*/
                    lista_aplikacija.sortByDescending { it.first }

                    chipGroup.setOnClickListener{
                        val chip = it as Chip
                        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(lista_aplikacija[chip.id].second.packageName.toString())
                        startActivity(launchIntent)
                    }
                    chipGroup.removeAllViews()
                    //var chips: MutableList<Chip> = mutableListOf()
                    for ((index,app) in lista_aplikacija.iterator().withIndex()) {
                        if(index > 10) break
                        val chip = layoutInflater.inflate(R.layout.chip_template, null) as Chip
                        val chip_text = app.second.label.toString()
                        chip.setText(chip_text)// + " " + app.first)
                        chip.id = index
                        try {
                            chip.chipBackgroundColor = ColorStateList.valueOf(app.second.color.toInt())
                        } catch (e:NumberFormatException){
                            e.printStackTrace()
                        }
                        chip.setOnClickListener {
                            Log.d(
                                "ingo", chip.text.toString() + " " + chip.id.toString()
                            )
                            // otvori ovu aplikaciju
                            val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(app.second.packageName.toString())
                            startActivity(launchIntent)
                        }
                        //chips.add(chip)
                        chipGroup.addView(chip)
                    }

                }
            }
        }
        //findViewById<ImageButton>(R.id.plus_button).setOnClickListener { openAddMenu(it) }
    }

    fun countMatches(string: String, pattern: String): Int {
        var max_match = 0
        var match_counter = 0
        var index_counter = 0
        for(i in string){
            if(i == pattern[index_counter]){
                index_counter++
                match_counter++
                if(match_counter > max_match) max_match = match_counter
                if(index_counter >= pattern.length) break
            } else {
                index_counter = 0
                match_counter = 0
            }
        }
        return max_match
    }

    fun showLayout(id:Int){
        val exclude = mutableListOf<View>()
        val all = listOf<View>(
            findViewById(R.id.search_container),
            findViewById(R.id.recycle_container),
            findViewById(R.id.relativelayout),
            findViewById(R.id.floating_action_button))
        when(id){
            LAYOUT_ACTIVITIES -> exclude.add(all[1])
            LAYOUT_SEARCH -> exclude.add(all[0])
            LAYOUT_MAIN -> exclude.addAll(listOf(all[2], all[3]))
        }
        for(view in all){
            if(exclude.contains(view)){
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.INVISIBLE
            }
        }
        currentLayout = id
    }

    fun startShortcut(shortcut:MeniJednoPolje){
        val launcherApps: LauncherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        Log.d("ingo", "startshortcut " + shortcut.nextIntent + "->" + shortcut.nextId)
        /*sendIntent.setAction(Intent.ACTION_SEND)
        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.")
        sendIntent.setType("text/plain")*/
        launcherApps.startShortcut(shortcut.nextIntent, shortcut.nextId, null, null, android.os.Process.myUserHandle())
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    fun goToPocetna(){
        stack.clear()
        prebaciMeni(pocetnaId, -1)
        //prikaziPrecace
        findViewById<Button>(R.id.back_button).isEnabled = false
        currentMenuType = MENU_APPLICATION_OR_FOLDER
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }

    fun showDialog() {
        val fragmentManager = supportFragmentManager
        currentSubmenuList[editSelected].let{
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

    fun goBack(){
        if(stack.size > 1){
            val lastitem = stack[stack.size-2]
            val lastpolje = lastitem.first
            val lastcounter = lastitem.second
            prebaciMeni(lastpolje, lastcounter, true)
            stack.removeAt(stack.lastIndex)
        }
    }

    suspend fun initializeRoom(){
        Log.d("ingo", "initializeRoom start")
        if(!loadAppsBool){
            listaMenija += pocetna
            pocetnaId = pocetna.id
            return
        }
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        var meniPolja:List<MeniJednoPolje> = recDao.getAll()
        if(meniPolja.size == 0){
            for(polje in defaultMenus){
                polje.id = 0
                //recDao.insertAll(polje)
            }
            pocetnaId = databaseAddNewPolje(pocetna).id
            meniPolja = recDao.getAll()
            Log.d("ingo", "initializeRoom initialized, total=" + meniPolja.size)
        } else {
            listaMenija += meniPolja
            pocetnaId = meniPolja.first().id
        }
        Log.d("ingo", "pocetnaId = " + pocetnaId)
        for(meni in listaMenija){
            if(meni.id > highestId) highestId = meni.id
        }
        val appDao: AppInfoDao = db.appInfoDao()
        Log.d("ingo", "initializeRoom before cache")
        if(cache_apps) {
            radapter.appsList = appDao.getAll() as MutableList<AppInfo>
            Log.d("ingo", "loaded " + radapter.appsList.size + " cached apps")
        }
        Log.d("ingo", "initializeRoom before loadnewapps")
        val newApps = loadNewApps()
        Log.d("ingo", "initializeRoom before insertall")
        if(cache_apps && newApps.isNotEmpty()){
            for(app in newApps){
                Log.d("ingo", "new app " + app.label)
                appDao.insertAll(app)
            }
        }
        Log.d("ingo", "initializeRoom before addall")
        radapter.appsList.addAll(newApps)
        radapter.appsList.sortBy { it.label.toString().lowercase() }
        //if(cache_apps){
        Log.d("ingo", "initializeRoom before loadicons")
        loadIcons()
        Log.d("ingo", "initializeRoom after loadicons")
        //}
        var c:Context = this
        withContext(Dispatchers.Main) {
            updateStuff();
            Toast.makeText(c, "App list loaded", Toast.LENGTH_SHORT).show()
        }
    }


    fun loadIcons(){
        for(app in radapter.appsList) {
            if (radapter.icons.containsKey(app.packageName)) continue
            /*try {
            icons[pname] = context.packageManager.getApplicationIcon(pname)
        } catch (e: PackageManager.NameNotFoundException){
            e.printStackTrace()
        }*/
            val applicationInfo = packageManager.getApplicationInfo(app.packageName, 0)
            val res: Resources = packageManager.getResourcesForApplication(applicationInfo)
            val icon = res.getDrawableForDensity(
                applicationInfo.icon,
                DisplayMetrics.DENSITY_LOW,
                null
            )
            radapter.icons[app.packageName] = icon
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

    fun changeEditMode(){
        editMode = !editMode
        circleView?.editMode = !circleView?.editMode!!
        circleView?.changeMiddleButtonState(CircleView.MIDDLE_BUTTON_EDIT)
        circleView?.invalidate()
        if(editMode){
            circleView?.invalidate()
        } else {
            deYellowAll()
        }
    }

    fun getPolje(id:Int):MeniJednoPolje?{
        for(polje in listaMenija){
            if(polje.id == id) return polje
        }
        return null
    }

    fun getSubPolja(id:Int):List<MeniJednoPolje>{
        var lista:MutableList<MeniJednoPolje> = mutableListOf()
        var polje1 = getPolje(id)
        if(polje1 != null) {
            for (polje2 in listaMenija) {
                if(polje1.polja!!.contains(polje2.id)){
                    Log.d("ingo", "dodajem " + polje2.text)
                    lista.add(polje2)
                }
            }
            Log.d("ingo", "getSubPolja od " + id + " je " + lista.map{ it.text }.toString())
            return lista
        }
        return listOf()
    }

    fun prikaziPrecace(prosiriId: Int, selected:Int){
        circleView?.setPosDontDraw(selected)
        selected_global = selected
        var precaci:MutableList<MeniJednoPolje> = mutableListOf()
        val launcherApps: LauncherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if(launcherApps.hasShortcutHostPermission()){
            val precaci_info = getPolje(prosiriId)?.let {
                radapter.getShortcutFromPackage(
                    it.nextIntent,
                    this
                )
            }
            //Log.d("ingo", "prikazi prečace2 " + precaci.map{ it.shortLabel.toString() + "->" + it.id }.toString())
            precaci = precaci_info?.map{
                MeniJednoPolje(id=0, text= it.shortLabel as String, nextIntent = it.`package`, nextId = it.id, shortcut = true)
            } as MutableList<MeniJednoPolje>
        }
        if(prosiriId == pocetnaId){
            circleView?.amIHome(true)
        } else {
            circleView?.amIHome(false)
        }
        Log.d("ingo", "prikazi prečace " + precaci.map{ it.text + "->" + it.nextIntent + "->" + it.nextId }.toString())
        var polja = getSubPolja(prosiriId)
        currentSubmenuList = precaci + polja
        currentMenuType = MENU_SHORTCUT
        circleView?.setColorList(IntArray(precaci.size) { Color.WHITE }.map{it.toString()} + polja.map{ it.color })
        val precaci_i_polja = precaci+polja
        circleView?.setTextList(precaci_i_polja)
        max_subcounter = (precaci_i_polja).size
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

    fun prikazi(prosiriId: Int, selected:Int){
        if(prosiriId == pocetnaId){
            circleView?.amIHome(true)
        } else {
            circleView?.amIHome(false)
        }
        selected_global = selected
        Log.d("ingo", "prikazi " + prosiriId)
        var polja = getSubPolja(prosiriId)//getRoomMeniPolja(prosiri.polja)
        currentSubmenuList = polja
        currentMenuType = MENU_APPLICATION_OR_FOLDER
        //var lista:List<String> = polja.map{ it.text }
        circleView?.setPosDontDraw(selected)
        circleView?.setTextList(polja)
        max_subcounter = polja.size
    }

    fun prebaciMeni(id:Int, counter:Int, noStack:Boolean=false, precaci:Boolean=false): MeniJednoPolje? {
        lastTextViewEnteredCounter = counter
        Log.d("ingo", "lastTextViewEnteredCounter " + lastTextViewEnteredCounter)
        val polje = getPolje(id)
        Log.d("ingo", "prebaciMeni " + id)
        if(polje != null){
            currentMenu = polje
            prikaziPrecace(polje.id, counter)
            /*if(!precaci) {
                prikazi(polje.id, counter)
            } else {

            }*/

            findViewById<TextView>(R.id.selected_text).text = polje.text
            if(!noStack){
                stack.add(Pair(currentMenu.id, counter))
                Log.d("ingo", "adding " + currentMenu.text + " to stack.")
                findViewById<Button>(R.id.back_button).isEnabled = true
            }
            return polje
        } else {
            Log.d("ingo", "prebaciMeni null!")
        }
        return null
    }

    fun deYellowAll(){
        circleView?.deyellowAll()
        findViewById<ConstraintLayout>(R.id.editbuttonslayout).visibility = View.INVISIBLE
        editSelected = -1
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

    fun isAppLoaded(p:String): Boolean{
        return radapter.appsList.map{ it.packageName }.contains(p)
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
        var pixels = Array<Int>(9){0}
        var newApps: MutableList<AppInfo> = mutableListOf()
        val packs = packageManager.getInstalledPackages(0)
        var colorPrimary: Int = 0
        for (i in packs.indices) {
            val p = packs[i]
            if (!isSystemPackage(p)) {
                if(isAppLoaded(p.packageName)) {
                    Log.d("ingo", "already loaded " + p.packageName)
                    continue
                }
                val appName = p.applicationInfo.loadLabel(packageManager).toString()
                Log.d("ingo", "loading new app " + appName)
                var icon:Drawable? = null
                val res: Resources = packageManager.getResourcesForApplication(p.applicationInfo)

                if(loadIconBool){

                    icon = res.getDrawableForDensity(
                        p.applicationInfo.icon,
                        DisplayMetrics.DENSITY_LOW,
                        null
                    )
                    radapter.icons[p.applicationInfo.packageName] = icon
                    if(icon != null) {
                        colorPrimary = getBestPrimaryColor(icon)
                    }
                    //icon = p.applicationInfo.loadIcon(packageManager)
                } else {
                    colorPrimary = 0
                }
                val packageName = p.applicationInfo.packageName
                if (appName != packageName.toString()) {
                    newApps.add(AppInfo(0, appName, packageName, colorPrimary.toString()))
                }
            }
        }

        val i = Intent(Intent.ACTION_MAIN, null)
        i.addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = packageManager.queryIntentActivities(i, 0)
        for (ri in allApps) {
            if(isAppLoaded(ri.activityInfo.packageName) || newApps.map{it.packageName}.contains(ri.activityInfo.packageName)) continue
            Log.d("ingo", ri.activityInfo.packageName)
            //val res: Resources = packageManager.getResourcesForApplication(ri.activityInfo.packageName)
            if(loadIconBool){
                val icon = ri.loadIcon(packageManager)
                radapter.icons[ri.activityInfo.packageName] = icon
                if(icon != null) {
                    colorPrimary = getBestPrimaryColor(icon)
                }
            } else {
                colorPrimary = 0
            }
            val app: AppInfo = AppInfo(0, ri.loadLabel(packageManager).toString(), ri.activityInfo.packageName, colorPrimary.toString())
            //Log.d("ingo", app.label.toString() + " " + app.packageName)
            //Log.d("ingo", app.label.toString() + " " + app.packageName.toString() + " loaded")
            //appsList.add(app)
            //radapter.appsList.add(app)
            newApps.add(app)
            /*if(!radapter.appsList.map { it.packageName.toString() }.contains(app.packageName.toString())) {
                Log.d("ingo", app.label.toString())
                radapter.appsList.add(app)
            }*/
        }
        return newApps
    }

    fun updateStuff() {
        //findViewById<RecyclerView>(R.id.recycler_view).adapter?.notifyDataSetChanged()
        //radapter.notifyItemInserted(radapter.getItemCount() - 1)
        radapter.notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        // Do something
        if (event != null) {
            if(selectAppMenuOpened){
                // izaberi ovu aplikaciju
                lifecycleScope.launch(Dispatchers.IO) {
                    val dodanoPolje = databaseAddNewPolje(MeniJednoPolje(id=0, text = event.text, nextIntent = event.launchIntent, color=event.color))
                    val trenutnoPolje = getPolje(currentMenu.id)
                    if(trenutnoPolje != null){
                        trenutnoPolje.polja = trenutnoPolje.polja.plus(dodanoPolje.id)
                        databaseUpdateItem(trenutnoPolje)
                    }
                    withContext(Dispatchers.Main){
                        toggleAppMenu()
                        refreshCurrentMenu()
                    }
                }
                selectAppMenuOpened = false
            } else {
                // otvori ovu aplikaciju
                val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(event.launchIntent)
                startActivity(launchIntent)
            }
            Log.d("ingo", "on message event " + event.launchIntent + " " + selectAppMenuOpened)
        }
    }

    private fun toggleAppMenu(){
        updateStuff()
        appListOpened = !appListOpened
        if(appListOpened){
            showLayout(LAYOUT_ACTIVITIES)
        } else {
            findViewById<TextView>(R.id.recycle_view_label).visibility = View.GONE
            showLayout(LAYOUT_MAIN)
        }
    }

    override fun onBackPressed() {
        if(currentLayout == LAYOUT_SEARCH){
            showLayout(LAYOUT_MAIN)
        } else {
            toggleAppMenu()
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

    fun openAddMenu(){
        val contentView = createAddMenu()
        shortcutPopup?.dismiss()
        shortcutPopup = PopupWindow(contentView,
            ListPopupWindow.WRAP_CONTENT,
            ListPopupWindow.WRAP_CONTENT, true)
        //shortcutPopup?.animationStyle = R.style.PopupAnimation
        shortcutPopup?.showAtLocation(circleView, Gravity.CENTER, 0, 0)
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
                    val trenutnoPolje = getPolje(currentMenu.id)
                    if(trenutnoPolje != null){
                        trenutnoPolje.polja = trenutnoPolje.polja.plus(dodanoPolje.id)
                        databaseUpdateItem(trenutnoPolje)
                        Log.d("ingo", trenutnoPolje.polja.toString())
                    }
                    withContext(Dispatchers.Main){
                        refreshCurrentMenu()
                        shortcutPopup?.dismiss()
                    }
                }

            }
        }
        return view
    }

    fun refreshCurrentMenu(){
        prebaciMeni(currentMenu.id, selected_global)
        Log.d("ingo", "current menu refreshed")
    }

    fun databaseUpdateItem(polje:MeniJednoPolje){
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        recDao.update(polje)
        var staro_polje = getPolje(polje.id)
        staro_polje = polje
        Log.d("ingo", "updated " + polje.text + "(" + polje.id + ")")
    }

    fun databaseGetItemByRowId(id:Long):MeniJednoPolje{
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        Log.d("ingo", "databaseGetItemByRowId " + id)
        return recDao.findByRowId(id).first()
    }

    fun deleteSelectedItem(){
        lifecycleScope.launch(Dispatchers.IO) {
            databaseDeleteById(currentSubmenuList[editSelected].id)
            withContext(Dispatchers.Main){
                deYellowAll()
                refreshCurrentMenu()
            }
        }
    }

    fun databaseDeleteById(id: Int){
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        getPolje(id)?.let {
            recDao.delete(it)
            currentMenu.polja = currentMenu.polja.filter { it != id }
            listaMenija.filter{ it.id != id }
            recDao.update(currentMenu)
            Log.d("ingo", "deleted " + id)
        }
        Log.d("ingo", "deleted- " + id)
    }

    fun databaseAddNewPolje(polje:MeniJednoPolje):MeniJednoPolje{
        polje.id = 0
        Log.d("ingo", "databaseAddNewPolje(" + polje.text + ")")
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        val rowid = recDao.insertAll(polje)
        val polje = databaseGetItemByRowId(rowid.first())
        listaMenija.add(polje)
        return polje
    }

    fun createAddMenu():View{
        val view = LayoutInflater.from(this).inflate(R.layout.popup_add_which, null)
        view.findViewById<ImageView>(R.id.new_folder).setOnClickListener{
            //Toast.makeText(this, "New folder", Toast.LENGTH_SHORT).show()
            openFolderNameMenu(view)
        }
        view.findViewById<ImageView>(R.id.new_shortcut).setOnClickListener{
            //Toast.makeText(this, "New shortcut", Toast.LENGTH_SHORT).show()
            selectAppMenuOpened = true
            shortcutPopup?.dismiss()
            findViewById<TextView>(R.id.recycle_view_label).visibility = View.VISIBLE
            toggleAppMenu()
        }
        view.findViewById<ImageView>(R.id.new_action).setOnClickListener{
            Toast.makeText(this, "Not yet implemented:(", Toast.LENGTH_SHORT).show()
        }
        return view
    }

    fun pronadiMoguceAkcije(intent:String):List<MeniJednoPolje>{
        return listOf()
    }

    fun launchLastEntered(){
        if(lastEnteredIntent == null) return
        if(!lastEnteredIntent?.shortcut!!) {
            Log.d("ingo", "launchLastEntered app_or_folder " + lastEnteredIntent.toString())
            val launchIntent: Intent? =
                lastEnteredIntent?.let { packageManager.getLaunchIntentForPackage(it.nextIntent) }
            startActivity(launchIntent)
        } else {
            Log.d("ingo", "launchLastEntered shortcut " + lastEnteredIntent.toString())
            lastEnteredIntent?.let { startShortcut(it) }
        }
        lastEnteredIntent = null
        lastTextViewEnteredCounter = -1
        circleView?.changeMiddleButtonState(CircleView.MIDDLE_BUTTON_HIDE)
        goToPocetna()
    }

    fun touched(event:MotionEvent, counter:Int){
        Log.d("ingo", event.action.toString() + " " + counter)
        //Log.d("ingo", "pozvalo me")
        var sublist_counter = counter
        //if(counter+1 > lastTextViewEnteredCounter) sublist_counter = counter+1
        //Toast.makeText(this, lista[counter], Toast.LENGTH_SHORT).show()
        if(counter == ACTION_CANCEL){
            /*lastEnteredIntent = null
            lastTextViewEnteredCounter = -1
            findViewById<TextView>(R.id.selected_text).text = currentMenu.text*/
            goToPocetna()
            return
        }
        if(counter == ACTION_HOME){
            goToPocetna()
            return
        }
        if(event.action == ACTION_UP && !editMode){
            Log.d("ingo", "action up")
            if(counter == ACTION_LAUNCH){
                Log.d("ingo", "launch")
                // obavi intent
                launchLastEntered()
            }
            //Toast.makeText(this, findViewById<TextView>(R.id.selected_text).text, Toast.LENGTH_SHORT).show()
            return
        }
        /*if(counter == lastTextViewEnteredCounter || sublist_counter >= max_subcounter ) {
            Log.d("ingo", "returnic")
            return
        }*/
        //Log.d("ingo", "mi smo unutar " + counter + " texta")
        //Log.d("ingo", subcounter.toString() + " " + counter.toString() + " "  + lastItem.toString() + " " + getSubPolja(currentMenu.id)[subcounter].nextId.toString())
        if(!editMode && sublist_counter != selected_global) {

            findViewById<TextView>(R.id.selected_text).text = currentSubmenuList[sublist_counter].text
            if (currentSubmenuList[sublist_counter].nextIntent == "") { // mapa
                Log.d("ingo", "nextIntent je prazan")
                lastEnteredIntent = null
                prebaciMeni(currentSubmenuList[sublist_counter].id, counter)
            } else {
                lastEnteredIntent = currentSubmenuList[sublist_counter]
                Log.d("ingo", "lastTextViewEnteredCounter " + lastTextViewEnteredCounter)
                // TODO: potrebno pronaći prečace
                // mape/aplikacije -> prečaci/akcije
                prebaciMeni(currentSubmenuList[sublist_counter].id, counter, precaci = true)
                //lastEnteredIntent = precaci[0].intent
                //launchLastEntered()
                //return
                // TODO("potraži ostale prečace: koje akcije se mogu raditi s trenutne aktivnosti/prečaca")
                //precaci_as_menijednopolje += pronadiMoguceAkcije(currentSubmenuList[sublist_counter].nextIntent)

            }
            Log.d("ingo", "MENU_SHORTCUT " + lastEnteredIntent)
        } else if(editMode && event.action == MotionEvent.ACTION_DOWN) {
            if(sublist_counter >= 0) {
                if (editSelected == -1) {
                    editSelected = sublist_counter
                    circleView?.yellowIt(sublist_counter)
                    Log.d("ingo", "selected " + sublist_counter + " " + currentSubmenuList[sublist_counter].text + " " + currentSubmenuList[sublist_counter].id)
                    //textView?.setBackgroundColor(Color.YELLOW)
                    findViewById<ConstraintLayout>(R.id.editbuttonslayout).visibility = View.VISIBLE
                } else {
                    if (editSelected == sublist_counter) {
                        //Log.d("ingo", getPolje(editSelected))
                        deYellowAll()
                        findViewById<ConstraintLayout>(R.id.editbuttonslayout).visibility =
                            View.INVISIBLE
                    } else {
                        // zamijeni indexe od MeniJednoPolje s id-evima editSelected i subcounter u listi od trenutno prikazanog MeniJednoPolje
                    }
                }
            }
            if(sublist_counter == ACTION_ADD){
                if((currentMenu.id == 0 && currentSubmenuList.size >= 8) || (currentMenu.id != 0 && currentSubmenuList.size >= 7)) {
                    Toast.makeText(this, "Max. elements present", Toast.LENGTH_SHORT)
                } else {
                    openAddMenu()
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        /*if(appListOpened) return super.onTouchEvent(event)
        var textview_counter = 0 // govori na kojem textviewu je polje
        var sublist_counter = 0 // govori koje je polje po redu
        var textView:TextView? = null // govori u kojem smo textviewu
        var found:Boolean = false
        for(txt in textovi){
            var rect: Rect = Rect(0, 0, 0, 0)
            txt.getGlobalVisibleRect(rect)
            //rect = Rect(rect.centerX()-50, rect.top, rect.centerX()+50, rect.bottom)
            if (event != null && event.x != null && event.y != null) {
                if(rect.contains(event.x.toInt(), event.y.toInt())){
                    textView = txt
                    found = true
                    break
                }
            }
            if(textview_counter != lastTextViewEnteredCounter){
                sublist_counter++
            }
            textview_counter++
        }
        // ovdje sad imamo found, textview_counter i sublist_counter definirano
        if(found){

        }

        /*if(currentMenu?.polja?.get(counter)?.nextId != null){
            findViewById<TextView>(R.id.selected_text).text = currentMenu?.polja?.get(counter)?.text
            currentMenu = prebaciMeni(currentMenu?.polja?.get(counter)?.nextId!!, counter)
        }*/
        //Log.d("ingo", event?.action.toString() + " " + event?.historySize)

         */
        return super.onTouchEvent(event)
    }
}