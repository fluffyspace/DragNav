package com.ingokodba.dragnav

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment() : Fragment(), MainFragmentInterface {

    lateinit var circleView: CircleView
    var bottomMenuView: BottomMenuView? = null
    lateinit var relativeLayout: LinearLayout
    private val viewModel: ViewModel by activityViewModels()
    lateinit var mactivity:MainActivity
    lateinit var selected_text: TextView
    lateinit var global_view:View
    lateinit var addingMenuCancelOk:LinearLayout
    lateinit var cancelAddingApp:Button
    lateinit var addApp: Button

    override var fragment: Fragment = this

    var editMenu: LinearLayout? = null
    var normalMenu: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mactivity = (activity as MainActivity)
        //MainActivity.changeLocale(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        circleView = view.findViewById(R.id.circleview)
        if(mactivity.uiDesignMode == UiDesignEnum.CIRCLE) bottomMenuView = view.findViewById(R.id.bottomMenuView)
        relativeLayout = view.findViewById(R.id.relativelayout)

        if(mactivity.uiDesignMode == UiDesignEnum.CIRCLE_RIGHT_HAND || mactivity.uiDesignMode == UiDesignEnum.CIRCLE_LEFT_HAND) {
            editMenu = view.findViewById(R.id.editMenu)
            normalMenu = view.findViewById(R.id.normalMenu)

            view.findViewById<ImageButton>(R.id.search).setOnClickListener {
                mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_SEARCH)
            }
            view.findViewById<ImageButton>(R.id.list).setOnClickListener {
                mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_ACTIVITIES)
            }
            view.findViewById<ImageButton>(R.id.edit).setOnClickListener {
                toggleEditMode()
            }
            view.findViewById<ImageButton>(R.id.settings).setOnClickListener {
                settings()
            }

            view.findViewById<ImageButton>(R.id.rename).setOnClickListener {
                mactivity.showMyDialog(viewModel.editSelected)
            }
            view.findViewById<ImageButton>(R.id.delete).setOnClickListener {
                mactivity.deleteSelectedItem(viewModel.editSelected)
            }
            view.findViewById<ImageButton>(R.id.enter).setOnClickListener {
                enterSelected()
            }
            view.findViewById<ImageButton>(R.id.cancel).setOnClickListener {
                toggleEditMode()
            }
        }

        bottomMenuView?.updateTexts(listOf(MainActivity.resources2.getString(R.string.rename), MainActivity.resources2.getString(R.string.delete), MainActivity.resources2.getString(R.string.enter), MainActivity.resources2.getString(R.string.cancel)))

        addingMenuCancelOk = view.findViewById(R.id.addingMenuCancelOk)
        cancelAddingApp = view.findViewById(R.id.cancelAddingApp)
        addApp = view.findViewById(R.id.addApp)

        cancelAddingApp.setOnClickListener {
            cancelAddingAppHandler()
        }

        addApp.setOnClickListener {
            addNewAppHandler()
        }

        /*viewModel.icons.observe(this) {
            circleView.icons = it
            circleView.lala()
            Log.d("ingo", "icons loaded")
        }*/
        //circleView?.mactivity.radapter = mactivity.radapter
        circleView.setEventListener(object :
            IMyEventListener {
            override fun onEventOccurred(event: MotionEvent, redniBrojPolja: Int, current: Int) {
                Log.d("ingo", "onEventOccurred")
                touched(event, redniBrojPolja, current)
            }
        })
        bottomMenuView?.setEventListener(object :
            BottomMenuView.IMyOtherEventListener {
            override fun onEventOccurred(event: MotionEvent, counter: Int) {
                touched2(event, counter)
            }
        })
        selected_text = view.findViewById(R.id.selected_text)
        global_view = view
        /*view.findViewById<LinearLayout>(R.id.relativelayout).updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(0, 0, 0, (bottomMenuView.detectSize*2+bottomMenuView.padding*3).toInt())
        }*/
        relativeLayout.setOnClickListener {
            bottomMenuView?.collapse()
            Log.d("ingo", "collapse?")
        }
        if(viewModel.icons.value != null){
            circleView.icons = viewModel.icons.value!!
            Log.d("ingo", "icons who??")
        }
        if(viewModel.addNewAppMode){
            circleView.addAppMode = viewModel.addNewAppMode
            if(viewModel.addNewAppMode) {
                circleView.changeMiddleButtonState(MIDDLE_BUTTON_CHECK)
                bottomMenuView?.visibility = View.GONE
                addingMenuCancelOk.visibility = View.VISIBLE
            } else {
                circleView.amIHome(null)
                addingMenuCancelOk.visibility = View.GONE
                bottomMenuView?.visibility = View.VISIBLE
            }
        }

        if (viewModel.currentMenuId == -1) {
            goToHome()
        } else {
            refreshCurrentMenu()
        }

        Log.d("ingo", "mainfragment created")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(
            when(mactivity.uiDesignMode){
                UiDesignEnum.CIRCLE_RIGHT_HAND -> R.layout.fragment_main_right_hand
                UiDesignEnum.CIRCLE_LEFT_HAND -> R.layout.fragment_main_left_hand
                else -> R.layout.fragment_main
            }, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainFragment().apply {

            }
    }

    override fun iconsUpdated(){
        circleView.icons = viewModel.icons.value!!
    }

    fun cancelAddingAppHandler(){
        mactivity.addingNewAppEvent = null
        circleViewToggleAddAppMode(0)
    }

    override fun selectedItemDeleted(){
        deYellowAll()
        refreshCurrentMenu()
    }

    fun addNewAppHandler(){
        mactivity.addNewApp(mactivity.addingNewAppEvent)
        cancelAddingAppHandler()
        Log.d("ingo", "trebalo je cancelat")
    }

    fun touched(event:MotionEvent, redniBrojPolja:Int, no_draw_position:Int){
        Log.d("ingo", "touched " + event.action.toString() + " " + redniBrojPolja + " " + no_draw_position)
        bottomMenuView?.collapse()
        //Log.d("ingo", "pozvalo me")
        var sublist_counter = redniBrojPolja
        // redniBrojPolja govori koji po redu je oznacen, a sublist_counter govori koji textview je oznacen
        // koliko sam shvatio, ovo ispod se nikad ne poziva... treba maknuti
        if(no_draw_position >= 0 && no_draw_position <= redniBrojPolja){
            sublist_counter++
            Log.d("ingo", "sublist_counter povecan")
        }
        //if(viewModel.lastTextViewEnteredCounter <= sublist_counter && viewModel.lastTextViewEnteredCounter >= 0 ) sublist_counter++
        //if(counter+1 > viewModel.lastTextViewEnteredCounter) sublist_counter = counter+1
        //Toast.makeText(this, lista[counter], Toast.LENGTH_SHORT).show()
        if(redniBrojPolja == MainActivity.ACTION_ADD_APP){
            addNewAppHandler()
            return
        }
        if(redniBrojPolja == MainActivity.ACTION_ADD){
            addNew()
            return
        }
        if(redniBrojPolja == MainActivity.ACTION_CANCEL){
            /*viewModel.lastEnteredIntent = null
            viewModel.lastTextViewEnteredCounter = -1
            findViewById<TextView>(R.id.selected_text).text = viewModel.currentMenu.text*/
            goToHome()
            return
        }
        if(redniBrojPolja == MainActivity.ACTION_HOME){
            goToHome()
            return
        }
        if(event.action == MotionEvent.ACTION_UP && !viewModel.editMode && !viewModel.addNewAppMode){
            Log.d("ingo", "action up")
            if(redniBrojPolja == MainActivity.ACTION_LAUNCH){
                Log.d("ingo", "launch")
                launchLastEntered()
            }
            //Toast.makeText(this, findViewById<TextView>(R.id.selected_text).text, Toast.LENGTH_SHORT).show()
            return
        }
        /*if(counter == viewModel.lastTextViewEnteredCounter || sublist_counter >= viewModel.max_subcounter ) {
            Log.d("ingo", "returnic")
            return
        }*/
        //Log.d("ingo", "mi smo unutar " + counter + " texta")
        //Log.d("ingo", subcounter.toString() + " " + counter.toString() + " "  + lastItem.toString() + " " + getSubPolja(viewModel.currentMenu.id)[subcounter].nextId.toString())
        if(!viewModel.editMode) {
            if(viewModel.trenutnoPrikazanaPolja[redniBrojPolja].nextIntent != MainActivity.ACTION_ADD_PRECAC) {
                selected_text.text = viewModel.trenutnoPrikazanaPolja[redniBrojPolja].text
            }
            if (viewModel.trenutnoPrikazanaPolja[redniBrojPolja].nextIntent == "") { // mapa
                Log.d("ingo", "nextIntent je prazan")
                viewModel.lastEnteredIntent = null
                prebaciMeni(viewModel.trenutnoPrikazanaPolja[redniBrojPolja].id, sublist_counter)
            } else {
                viewModel.lastEnteredIntent = viewModel.trenutnoPrikazanaPolja[redniBrojPolja]
                // TODO: potrebno pronaći prečace
                // mape/aplikacije -> prečaci/akcije
                if(!viewModel.trenutnoPrikazanaPolja[redniBrojPolja].shortcut) prebaciMeni(viewModel.trenutnoPrikazanaPolja[redniBrojPolja].id, sublist_counter, precaci = true)
                //viewModel.lastEnteredIntent = precaci[0].intent
                //launchLastEntered()
                //return
                // TODO("potraži ostale prečace: koje akcije se mogu raditi s trenutne aktivnosti/prečaca")
                //precaci_as_menijednopolje += pronadiMoguceAkcije(viewModel.currentSubmenuList[sublist_counter].nextIntent)

            }
            //Log.d("ingo", "MENU_SHORTCUT " + viewModel.lastEnteredIntent)
        } else if(viewModel.editMode && event.action == MotionEvent.ACTION_DOWN) {
            Log.d("ingo", "elseif viewModel.editMode down")
            if(redniBrojPolja >= 0) {
                Log.d("ingo", "well its true")
                if (viewModel.editSelected == -1 || viewModel.editSelected != redniBrojPolja) {
                    viewModel.editSelected = redniBrojPolja
                    bottomMenuView?.selectedId = viewModel.editSelected
                    bottomMenuView?.invalidate()
                    circleView.selectPolje(redniBrojPolja)
                    //Log.d("ingo", "selected " + sublist_counter + " " + viewModel.currentSubmenuList[sublist_counter].text + " " + viewModel.currentSubmenuList[sublist_counter].id)
                    //textView?.setBackgroundColor(Color.YELLOW)
                } else {
                    if (viewModel.editSelected == redniBrojPolja) {
                        //Log.d("ingo", getPolje(viewModel.editSelected))
                        deYellowAll()
                    } else {
                        // zamijeni indexe od MeniJednoPolje s id-evima viewModel.editSelected i subcounter u listi od trenutno prikazanog MeniJednoPolje
                    }
                }
            }
        }
    }

    fun launchLastEntered(){
        if(viewModel.lastEnteredIntent == null) return
        if(!viewModel.lastEnteredIntent?.shortcut!!) {
            Log.d("ingo", "launchLastEntered app_or_folder " + viewModel.lastEnteredIntent.toString())
            val launchIntent: Intent? =
                viewModel.lastEnteredIntent?.let { requireContext().packageManager.getLaunchIntentForPackage(it.nextIntent) }
            if(launchIntent != null) {
                startActivity(launchIntent)
            } else {
                if(viewModel.lastEnteredIntent!!.nextIntent == MainActivity.ACTION_APPINFO){
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", viewModel.lastEnteredIntent!!.nextId, null)
                    intent.data = uri
                    startActivity(intent)
                } else if(viewModel.lastEnteredIntent!!.nextIntent == MainActivity.ACTION_ADD_PRECAC){
                    addNew()
                    viewModel.lastEnteredIntent = null
                    return
                }
            }
        } else {
            Log.d("ingo", "launchLastEntered shortcut " + viewModel.lastEnteredIntent.toString())
            viewModel.lastEnteredIntent?.let { mactivity.startShortcut(it) }
        }
        viewModel.lastEnteredIntent = null
        //circleView.changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
        goToHome()
    }

    fun enterSelected(){
        if(viewModel.editSelected == -1) return
        prebaciMeni(viewModel.trenutnoPrikazanaPolja[viewModel.editSelected].id, viewModel.editSelected)
        deYellowAll()
    }

    fun touched2(event:MotionEvent, counter:Int) {
        if(counter >= 0) {
            when (counter) {
                0 -> addNew()
                1 -> mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_ACTIVITIES)
                2 -> toggleEditMode()
                3 -> {
                    mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_SEARCH)//initializeSearch()
                }
                4 -> settings()
                5 -> collapseMenu()
            }
        } else {
            if(counter != -4 && viewModel.editSelected == -1){
                Toast.makeText(requireContext(), "Nothing selected.", Toast.LENGTH_SHORT).show()
                return
            }
            when (counter) {
                -1 -> mactivity.showMyDialog(viewModel.editSelected)
                -2 -> mactivity.deleteSelectedItem(viewModel.editSelected)
                -3 -> {
                    enterSelected()
                }
                -4 -> toggleEditMode()
            }
        }
    }

    fun collapseMenu(){
        bottomMenuView?.collapse()
    }

    fun settings(){
        //startActivity(Intent(android.provider.Settings.ACTION_SETTINGS), null);
        mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_SETTINGS)

    }

    interface IMyEventListener {
        fun onEventOccurred(event: MotionEvent, counter:Int, current:Int)
    }



    fun maxElementsPresent(): Boolean{
        val currentSizeWithoutPlusButton = viewModel.trenutnoPrikazanaPolja.size - if(viewModel.trenutnoPrikazanaPolja.find{it.nextIntent == MainActivity.ACTION_ADD_PRECAC} != null) 1 else 0
        return (circleView.amIHomeVar && currentSizeWithoutPlusButton >= 8) || (!circleView.amIHomeVar && currentSizeWithoutPlusButton >= 7)
    }

    fun addNew(){
        Log.d("ingo", "it's add")
        if(maxElementsPresent()) {
            Toast.makeText(requireContext(), "Max. elements present. ", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("ingo", "openaddmenu")
            mactivity.openAddMenu()
        }
    }

    fun circleViewToggleAddAppMode(yesOrNo:Int = -1){
        if(yesOrNo != -1) {
            viewModel.addNewAppMode = yesOrNo != 0
        } else {
            viewModel.addNewAppMode = !viewModel.addNewAppMode
        }
        circleView.addAppMode = viewModel.addNewAppMode
        if(viewModel.addNewAppMode) {
            circleView.changeMiddleButtonState(MIDDLE_BUTTON_CHECK)
            bottomMenuView?.visibility = View.GONE
            addingMenuCancelOk.visibility = View.VISIBLE
        } else {
            circleView.amIHome(null)
            addingMenuCancelOk.visibility = View.GONE
            bottomMenuView?.visibility = View.VISIBLE
        }
    }
    override fun refreshCurrentMenu(){
        circleView.updateDesign()
        circleView.invalidate()
        prebaciMeni(viewModel.currentMenuId, viewModel.no_draw_position)
        Log.d("ingo", "current menu refreshed")
    }
    fun deYellowAll(){
        circleView.deselectAll()
        viewModel.editSelected = -1
        bottomMenuView?.selectedId = -1
        bottomMenuView?.invalidate()
    }
    fun prebaciMeni(id:Int, counter:Int, nostack:Boolean=false, precaci:Boolean=false): KrugSAplikacijama? {
        val polje = getPolje(id)
        Log.d("ingo", "prebaciMeni " + id)
        if(polje != null){
            viewModel.currentMenu = polje
            viewModel.currentMenuId = id
            prikaziPoljaKruga(polje.id, counter)

            selected_text.text = polje.text
            if(!nostack){
                viewModel.stack.add(Pair(viewModel.currentMenu.id, counter))
                Log.d("ingo", "adding " + viewModel.currentMenu.text + " to viewModel.stack.")
                //findViewById<Button>(R.id.back_button).isEnabled = true
            }
            return polje
        } else {
            Log.d("ingo", "prebaciMeni null!")
        }
        return null
    }

    /*fun putInAllApps(){
        val polja = viewModel.appsList.value
        //val krugovi = polja!!.map { KrugSAplikacijama(id=0, text=it.label, nextIntent = null, nextId = trenutnoPolje!!.nextIntent)}


        circleView.setColorList(polja!!.map{ it.color })
        circleView.setKrugSAplikacijamaList(polja)
        Log.d("ingo", "currentSubmenuList " + viewModel.trenutnoPrikazanaPolja.map{it.text}.toString())
        circleView.setPosDontDraw(-1)
        viewModel.no_draw_position = -1
        //viewModel.max_subcounter = (viewModel.trenutnoPrikazanaPolja).size
    }*/
    fun prikaziPoljaKruga(idKruga: Int, selected:Int){
        var precaci:MutableList<KrugSAplikacijama> = mutableListOf()
        val trenutnoPolje = getPolje(idKruga)
        val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if(launcherApps.hasShortcutHostPermission()){
            val precaci_info = trenutnoPolje?.let {
                mactivity.getShortcutFromPackage(
                    it.nextIntent
                )
            }
            //Log.d("ingo", "prikazi prečace2 " + precaci.map{ it.shortLabel.toString() + "->" + it.id }.toString())
            precaci = precaci_info?.map{
                KrugSAplikacijama(id=0, text= it.shortLabel.toString(), nextIntent = it.`package`, nextId = it.id, shortcut = true)
            } as MutableList<KrugSAplikacijama>
        }
        if(idKruga == viewModel.pocetnaId){
            circleView.amIHome(true)
        } else {
            circleView.amIHome(false)
        }
        Log.d("ingo", "prikazi prečace " + precaci.map{ it.text + "->" + it.nextIntent + "->" + it.nextId }.toString())
        var polja = getSubPolja(idKruga)
        if(trenutnoPolje?.nextIntent != "" ) {
            precaci.add(KrugSAplikacijama(id=0, text= "App info", nextIntent = MainActivity.ACTION_APPINFO, nextId = trenutnoPolje!!.nextIntent))
        } else {
            if(!(circleView.amIHomeVar && precaci.size+polja.size >= 8) && !(!circleView.amIHomeVar && precaci.size+polja.size >= 7))
                //resources.getString(R.string.add_app)
                polja.add(KrugSAplikacijama(id=0, text="", nextIntent = MainActivity.ACTION_ADD_PRECAC, nextId = trenutnoPolje!!.nextIntent))
        }
        viewModel.trenutnoPrikazanaPolja = precaci + polja
        circleView.setColorList(IntArray(precaci.size) { Color.WHITE }.map{it.toString()} + polja.map{ it.color })
        circleView.setKrugSAplikacijamaList(viewModel.trenutnoPrikazanaPolja)
        Log.d("ingo", "currentSubmenuList " + viewModel.trenutnoPrikazanaPolja.map{it.text}.toString())
        circleView.setPosDontDraw(selected)
        viewModel.no_draw_position = selected
        viewModel.max_subcounter = (viewModel.trenutnoPrikazanaPolja).size
    }
    fun getPolje(id:Int): KrugSAplikacijama?{
        return viewModel.sviKrugovi.find{it.id == id}
    }
    override fun toggleEditMode(){
        viewModel.editMode = !viewModel.editMode
        circleView.editMode = !circleView.editMode!!
        circleView.changeMiddleButtonState(MIDDLE_BUTTON_EDIT)
        circleView.invalidate()
        if(viewModel.editMode){
            bottomMenuView?.editMode = true
            bottomMenuView?.postInvalidate()
            circleView.invalidate()
            normalMenu?.visibility = View.GONE
            editMenu?.visibility = View.VISIBLE
        } else {
            deYellowAll()
            bottomMenuView?.editMode = false
            bottomMenuView?.postInvalidate()
            normalMenu?.visibility = View.VISIBLE
            editMenu?.visibility = View.GONE
        }
    }
    override fun goToHome(){
        Log.d("ingo", "pocetna " + viewModel.pocetnaId)
        viewModel.stack.clear()
        prebaciMeni(viewModel.pocetnaId, -1)
        selected_text.setText(MainActivity.resources2.getString(R.string.home))
        //prikaziPrecace
        //findViewById<Button>(R.id.back_button).isEnabled = false
    }
    fun getSubPolja(id:Int):MutableList<KrugSAplikacijama>{
        var lista:MutableList<KrugSAplikacijama> = mutableListOf()
        var polje1 = getPolje(id)
        if(polje1 != null) {
            for (polje2 in viewModel.sviKrugovi) {
                if(polje1.polja!!.contains(polje2.id)){
                    //Log.d("ingo", "dodajem " + polje2.text)
                    lista.add(polje2)
                }
            }
            Log.d("ingo", "getSubPolja od " + id + " je " + lista.map{ it.text }.toString())
            return lista
        }
        return mutableListOf()
    }

}