package com.example.dragnav

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.baza.AppDatabase
import com.example.dragnav.baza.MeniJednoPoljeDao
import com.example.dragnav.modeli.MeniJednoPolje
import com.example.dragnav.modeli.MessageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity() {

    val MENU_UNDEFINED = -1
    val MENU_FOLDER = 0
    val MENU_APPLICATION = 1
    val MENU_SHORTCUT = 2
    val MENU_ACTION = 3

    var loadIconBool:Boolean = false
    var textovi:List<TextView> = mutableListOf()
    var lastTextViewEnteredCounter:Int = -1
    lateinit var currentMenu: MeniJednoPolje
    var currentMenuType: Int = MENU_UNDEFINED
    var max_subcounter:Int = -1
    var stack:MutableList<Pair<Int,Int>> = mutableListOf()
    var shortcutPopup:PopupWindow? = null
    var highestId = -1
    var selected_global:Int = -1
    var lastEnteredIntent:Intent? = null

    var editMode:Boolean = false
    var editSelected:Int = -1
    var appListOpened:Boolean = false
    var pocetnaId:Int = -1

    var selectAppMenuOpened:Boolean = false

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
    val pocetna:MeniJednoPolje = MeniJednoPolje(0, "Početna")
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

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textovi = listOf(findViewById<TextView>(R.id.t1),findViewById<TextView>(R.id.t2),findViewById<TextView>(R.id.t3),findViewById<TextView>(R.id.t4),
            findViewById<TextView>(R.id.t5),findViewById<TextView>(R.id.t6),findViewById<TextView>(R.id.t7),findViewById<TextView>(R.id.t8))
        findViewById<Button>(R.id.edit_mode_button).setOnClickListener{ changeEditMode() }
        findViewById<Button>(R.id.back_button).setOnClickListener{ goBack() }
        findViewById<Button>(R.id.edit_rename_button).setOnClickListener{ showDialog() }
        findViewById<Button>(R.id.edit_delete_button).setOnClickListener{ deleteSelectedItem() }
        findViewById<Button>(R.id.edit_cancel).setOnClickListener{ deYellowAll() }
        findViewById<Button>(R.id.pocetna_button).setOnClickListener{
            stack.clear()
            prebaciMeni(pocetnaId, -1)
            findViewById<Button>(R.id.back_button).isEnabled = false
        }
        radapter = RAdapter(this)
        findViewById<RecyclerView>(R.id.recycler_view).adapter = radapter
        lifecycleScope.launch(Dispatchers.IO) {
            initializeRoom()
            withContext(Dispatchers.Main){
                prebaciMeni(pocetnaId, -1)
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            loadApps()
        }
        findViewById<ImageButton>(R.id.plus_button).setOnClickListener { openAddMenu(it) }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }

    fun showDialog() {
        val fragmentManager = supportFragmentManager
        getPolje(currentMenu.polja[editSelected])?.let{
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

    fun initializeRoom(){
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
        if(editMode){
            findViewById<ImageButton>(R.id.plus_button).visibility = View.VISIBLE
        } else {
            findViewById<ImageButton>(R.id.plus_button).visibility = View.INVISIBLE
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
                    //Log.d("ingo", "dodajem " + polje2.text)
                    lista.add(polje2)
                }
            }
            Log.d("ingo", "getSubPolja od " + id + " je " + lista.map{ it.text }.toString())
            return lista
        }
        return listOf()
    }

    fun prikaziPrecace(precaci: List<ShortcutInfo>, selected:Int){
        selected_global = selected
        Log.d("ingo", "prikazi prečace " + precaci.map{ it.shortLabel }.toString())
        var counter = 0
        for(polje in textovi){
            polje.text = ""
        }
        for(polje in precaci){
            if(selected==counter){
                counter++
            }
            if(counter >= 8) break
            textovi[counter].text = polje.shortLabel
            counter++
        }
        max_subcounter = precaci.size
    }

    fun prikazi(prosiriId: Int, selected:Int){
        selected_global = selected
        Log.d("ingo", "prikazi " + prosiriId)
        var polja = getSubPolja(prosiriId)//getRoomMeniPolja(prosiri.polja)
        var counter = 0
        for(polje in textovi){
            polje.text = ""
        }
        for(polje in polja){
            if(selected==counter){
                counter++
            }
            if(counter >= 8) break
            textovi[counter].text = polje.text
            counter++
        }
        max_subcounter = polja.size
    }

    fun prebaciMeni(nextId:Int, counter:Int, noStack:Boolean=false): MeniJednoPolje? {
        lastTextViewEnteredCounter = counter
        val polje = getPolje(nextId)
        Log.d("ingo", "prebaciMeni " + nextId)
        if(polje != null){
            prikazi(polje.id, counter)
            currentMenu = polje
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
        for(txt in textovi) {
            txt.setBackgroundColor(Color.TRANSPARENT)
        }
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
        Log.d("ingo", "" + pkgInfo.packageName + " " + pkgInfo.applicationInfo.flags + " " + (pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0))
        return pkgInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    suspend fun loadApps() {
        val packs = packageManager.getInstalledPackages(0)
        for (i in packs.indices) {
            val p = packs[i]
            if (!isSystemPackage(p)) {
                val appName = p.applicationInfo.loadLabel(packageManager).toString()
                var icon:Drawable? = null
                if(loadIconBool) icon = p.applicationInfo.loadIcon(packageManager)
                val packages = p.applicationInfo.packageName
                if (appName != packages.toString()) radapter.appsList.add(
                    AppInfo(
                        appName,
                        packages,
                        icon
                    )
                )
            }
        }
        /*val shortcutManager = getSystemService(LauncherApps::class.java)
        val shortcutQuery: LauncherApps.ShortcutQuery = LauncherApps.ShortcutQuery()
        val shortcutInfo:MutableList<ShortcutInfo> =
            shortcutManager.getShortcuts(shortcutQuery, android.os.Process.myUserHandle()) as MutableList<ShortcutInfo>
*/
        /*val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        val shortcuts = packageManager.queryIntentActivities(shortcutIntent, 0)
        shortcuts.forEach {
            println("name = ${it.activityInfo.name}, label = ${it.loadLabel(packageManager)}")
        }*/
        /*//val appsList: MutableList<AppInfo> = mutableListOf()
        */
        val i = Intent(Intent.ACTION_MAIN, null)
        i.addCategory(Intent.CATEGORY_LAUNCHER)

        val allApps = packageManager.queryIntentActivities(i, 0)
        for (ri in allApps) {
            var app:AppInfo? = null
            if(loadIconBool){
                app = AppInfo(ri.loadLabel(packageManager), ri.activityInfo.packageName, ri.activityInfo.loadIcon(packageManager))
            } else {
                app = AppInfo(ri.loadLabel(packageManager), ri.activityInfo.packageName, null)
            }

            Log.d("ingo", app.label.toString() + " " + app.packageName.toString() + " loaded")
            //appsList.add(app)
            //radapter.appsList.add(app)
            if(!radapter.appsList.map { it.packageName.toString() }.contains(app.packageName.toString())) radapter.appsList.add(app)
        }
        radapter.appsList.sortBy { it.label.toString() }
        withContext(Dispatchers.Main) {
            updateStuff();
        }
    }

    /*fun getShortcutFromPackage(){
        val shortcutManager:LauncherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shortcutQuery: LauncherApps.ShortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        shortcutQuery.setPackage(appsList[pos].packageName.toString())
        try {
            val shortcutInfo: MutableList<ShortcutInfo> =
                shortcutManager.getShortcuts(
                    shortcutQuery,
                    android.os.Process.myUserHandle()
                ) as MutableList<ShortcutInfo>
            shortcutInfo.forEach {
                println("name = ${it.`package`}, label = ${it.shortLabel}")
            }
        } catch (e: SecurityException){
            //Collections.emptyList<>()
            Log.d("ingo", e.toString())
        }
    }*/

    fun updateStuff() {
        //findViewById<RecyclerView>(R.id.recycler_view).adapter?.notifyDataSetChanged()
        radapter.notifyItemInserted(radapter.getItemCount() - 1)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        // Do something
        if (event != null) {
            if(selectAppMenuOpened){
                // izaberi ovu aplikaciju
                lifecycleScope.launch(Dispatchers.IO) {
                    val dodanoPolje = databaseAddNewPolje(MeniJednoPolje(id=0, text = event.text, nextIntent = event.launchIntent))
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
            } else {
                // otvori ovu aplikaciju
                val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(event.launchIntent)
                startActivity(launchIntent)
            }
            selectAppMenuOpened = !selectAppMenuOpened
            Log.d("ingo", "on message event " + event.launchIntent)
        }
    }

    fun toggleAppMenu(){
        updateStuff()
        appListOpened = !appListOpened
        if(appListOpened){
            findViewById<LinearLayout>(R.id.recycle_container).visibility = View.VISIBLE
            findViewById<ConstraintLayout>(R.id.relativelayout).visibility = View.INVISIBLE
        } else {
            findViewById<TextView>(R.id.recycle_view_label).visibility = View.GONE
            findViewById<LinearLayout>(R.id.recycle_container).visibility = View.INVISIBLE
            findViewById<ConstraintLayout>(R.id.relativelayout).visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        toggleAppMenu()
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

    fun openAddMenu(view: View){
        val contentView = createAddMenu()
        val locations = IntArray(2, {0})
        view.getLocationOnScreen(locations)
        shortcutPopup?.dismiss()
        shortcutPopup = PopupWindow(contentView,
            ListPopupWindow.WRAP_CONTENT,
            ListPopupWindow.WRAP_CONTENT, true)
        //shortcutPopup?.animationStyle = R.style.PopupAnimation
        shortcutPopup?.showAtLocation(view, Gravity.CENTER, 0, 0)
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
        prikazi(currentMenu.id, selected_global)
    }

    fun databaseUpdateItem(polje:MeniJednoPolje){
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        recDao.update(polje)
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
            databaseDeleteById(currentMenu.polja[editSelected])
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
            Toast.makeText(this, "new folder", Toast.LENGTH_SHORT).show()
            openFolderNameMenu(view)
        }
        view.findViewById<ImageView>(R.id.new_shortcut).setOnClickListener{
            Toast.makeText(this, "new shortcut", Toast.LENGTH_SHORT).show()
            selectAppMenuOpened = true
            shortcutPopup?.dismiss()
            findViewById<TextView>(R.id.recycle_view_label).visibility = View.VISIBLE
            toggleAppMenu()
        }
        return view
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(appListOpened) return super.onTouchEvent(event)
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
            if(event?.action == ACTION_UP && editMode == false){
                Log.d("ingo", "action up")
                if(textview_counter == lastTextViewEnteredCounter){
                    // obavi intent
                    Log.d("ingo", "pokrecemo intent " + lastEnteredIntent.toString())
                    startActivity(lastEnteredIntent)
                    lastEnteredIntent = null
                    lastTextViewEnteredCounter = -1
                }
                //Toast.makeText(this, findViewById<TextView>(R.id.selected_text).text, Toast.LENGTH_SHORT).show()
                return super.onTouchEvent(event)
            }
            if(textview_counter == lastTextViewEnteredCounter || sublist_counter >= max_subcounter ) return super.onTouchEvent(event)
            Log.d("ingo", "mi smo unutar " + textview_counter + " texta")
            //Log.d("ingo", subcounter.toString() + " " + counter.toString() + " "  + lastItem.toString() + " " + getSubPolja(currentMenu.id)[subcounter].nextId.toString())
            if(!editMode) {
                val trenutnaSubPolja = getSubPolja(currentMenu.id)
                if (trenutnaSubPolja[sublist_counter].nextIntent == "") {
                    Log.d("ingo", "nextIntent je prazan")
                    findViewById<TextView>(R.id.selected_text).text = trenutnaSubPolja[sublist_counter].text
                    prebaciMeni(trenutnaSubPolja[sublist_counter].id, textview_counter)
                } else {
                    val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(trenutnaSubPolja[sublist_counter].nextIntent)
                    lastEnteredIntent = launchIntent
                    lastTextViewEnteredCounter = textview_counter
                    // TODO: potrebno pronaći prečace
                    // mape -> aplikacije -> prečaci -> akcije
                    prikaziPrecace(radapter.getShortcutFromPackage(trenutnaSubPolja[sublist_counter].nextIntent, this), textview_counter)
                    currentMenuType = MENU_SHORTCUT
                }
            } else if(event?.action == MotionEvent.ACTION_DOWN) {
                if(editSelected == -1){
                    editSelected = sublist_counter
                    textView?.setBackgroundColor(Color.YELLOW)
                    findViewById<ConstraintLayout>(R.id.editbuttonslayout).visibility = View.VISIBLE
                } else {
                    if(editSelected == sublist_counter){
                        //Log.d("ingo", getPolje(editSelected))
                        deYellowAll()
                        findViewById<ConstraintLayout>(R.id.editbuttonslayout).visibility = View.INVISIBLE
                    } else {
                        // zamijeni indexe od MeniJednoPolje s id-evima editSelected i subcounter u listi od trenutno prikazanog MeniJednoPolje
                    }
                }
            }
        }

        /*if(currentMenu?.polja?.get(counter)?.nextId != null){
            findViewById<TextView>(R.id.selected_text).text = currentMenu?.polja?.get(counter)?.text
            currentMenu = prebaciMeni(currentMenu?.polja?.get(counter)?.nextId!!, counter)
        }*/
        //Log.d("ingo", event?.action.toString() + " " + event?.historySize)
        return super.onTouchEvent(event)
    }
}