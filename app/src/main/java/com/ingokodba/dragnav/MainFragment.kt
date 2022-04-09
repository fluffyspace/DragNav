package com.ingokodba.dragnav

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.MeniJednoPolje
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {

    lateinit var circleView: CircleView
    lateinit var bottomMenuView: BottomMenuView
    private val viewModel: NewRAdapterViewModel by activityViewModels()
    lateinit var mactivity:MainActivity
    lateinit var selected_text: TextView
    lateinit var global_view:View

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        mactivity = (activity as MainActivity)
        //MainActivity.changeLocale(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        circleView = view.findViewById(R.id.circleview)
        bottomMenuView = view.findViewById(R.id.bottomMenuView)
        /*viewModel.icons.observe(this) {
            circleView.icons = it
            circleView.lala()
            Log.d("ingo", "icons loaded")
        }*/
        //circleView?.mactivity.radapter = mactivity.radapter
        circleView.setEventListener(object :
            CircleView.IMyEventListener {
            override fun onEventOccurred(event: MotionEvent, counter: Int, current: Int) {
                touched(event, counter, current)
            }
        })
        bottomMenuView.setEventListener(object :
            BottomMenuView.IMyOtherEventListener {
            override fun onEventOccurred(event: MotionEvent, counter: Int) {
                touched2(event, counter)
            }
        })
        selected_text = view.findViewById(R.id.selected_text)
        global_view = view
        view.findViewById<LinearLayout>(R.id.relativelayout).updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(0, 0, 0, (bottomMenuView.detectSize*2+bottomMenuView.padding*3).toInt())
        }
        view.findViewById<LinearLayout>(R.id.relativelayout).setOnClickListener { bottomMenuView.collapse()
        Log.d("ingo", "collapse?")}
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
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
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun touched(event:MotionEvent, counter:Int, no_draw_position:Int){
        Log.d("ingo", "touched " + event.action.toString() + " " + counter)
        bottomMenuView.collapse()
        //Log.d("ingo", "pozvalo me")
        var sublist_counter = counter
        // counter govori koji po redu je oznacen, a sublist_counter govori koji textview je oznacen
        if(no_draw_position >= 0 && no_draw_position <= counter) sublist_counter++
        //if(viewModel.lastTextViewEnteredCounter <= sublist_counter && viewModel.lastTextViewEnteredCounter >= 0 ) sublist_counter++
        //if(counter+1 > viewModel.lastTextViewEnteredCounter) sublist_counter = counter+1
        //Toast.makeText(this, lista[counter], Toast.LENGTH_SHORT).show()
        if(counter == MainActivity.ACTION_ADD_APP){
            mactivity.addNewApp(mactivity.addingNewApp)
            mactivity.addingNewApp = null
            mactivity.circleViewToggleAddAppMode(0)
            return
        }
        if(counter == MainActivity.ACTION_CANCEL){
            /*viewModel.lastEnteredIntent = null
            viewModel.lastTextViewEnteredCounter = -1
            findViewById<TextView>(R.id.selected_text).text = viewModel.currentMenu.text*/
            goToPocetna()
            return
        }
        if(counter == MainActivity.ACTION_HOME){
            goToPocetna()
            return
        }
        if(event.action == MotionEvent.ACTION_UP && !viewModel.editMode && !viewModel.addNewAppMode){
            Log.d("ingo", "action up")
            if(counter == MainActivity.ACTION_LAUNCH){
                Log.d("ingo", "launch")
                // obavi intent
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
        Log.d("ingo", "" + no_draw_position + " " + viewModel.lastTextViewEnteredCounter)
        if(!viewModel.editMode && no_draw_position == viewModel.lastTextViewEnteredCounter) {
            selected_text.text = viewModel.currentSubmenuList[counter].text
            if (viewModel.currentSubmenuList[counter].nextIntent == "") { // mapa
                Log.d("ingo", "nextIntent je prazan")
                viewModel.lastEnteredIntent = null
                prebaciMeni(viewModel.currentSubmenuList[counter].id, sublist_counter)
            } else {
                viewModel.lastEnteredIntent = viewModel.currentSubmenuList[counter]
                Log.d("ingo", "viewModel.lastTextViewEnteredCounter " + viewModel.lastTextViewEnteredCounter)
                // TODO: potrebno pronaći prečace
                // mape/aplikacije -> prečaci/akcije
                if(!viewModel.currentSubmenuList[counter].shortcut) prebaciMeni(viewModel.currentSubmenuList[counter].id, sublist_counter, precaci = true)
                //viewModel.lastEnteredIntent = precaci[0].intent
                //launchLastEntered()
                //return
                // TODO("potraži ostale prečace: koje akcije se mogu raditi s trenutne aktivnosti/prečaca")
                //precaci_as_menijednopolje += pronadiMoguceAkcije(viewModel.currentSubmenuList[sublist_counter].nextIntent)

            }
            //Log.d("ingo", "MENU_SHORTCUT " + viewModel.lastEnteredIntent)
        } else if(viewModel.editMode && event.action == MotionEvent.ACTION_DOWN) {
            Log.d("ingo", "elseif viewModel.editMode down")
            if(counter >= 0) {
                Log.d("ingo", "well its true")
                if (viewModel.editSelected == -1 || viewModel.editSelected != counter) {
                    viewModel.editSelected = counter
                    bottomMenuView.selectedId = viewModel.editSelected
                    bottomMenuView.invalidate()
                    circleView.yellowIt(counter)
                    //Log.d("ingo", "selected " + sublist_counter + " " + viewModel.currentSubmenuList[sublist_counter].text + " " + viewModel.currentSubmenuList[sublist_counter].id)
                    //textView?.setBackgroundColor(Color.YELLOW)
                } else {
                    if (viewModel.editSelected == counter) {
                        //Log.d("ingo", getPolje(viewModel.editSelected))
                        deYellowAll()
                    } else {
                        // zamijeni indexe od MeniJednoPolje s id-evima viewModel.editSelected i subcounter u listi od trenutno prikazanog MeniJednoPolje
                    }
                }
            }
            if(counter == MainActivity.ACTION_ADD){
                addNew()
            }
        }
    }

    fun createAddMenu():View{
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.popup_add_which, null)
        view.findViewById<LinearLayout>(R.id.new_folder).setOnClickListener{
            //Toast.makeText(this, "New folder", Toast.LENGTH_SHORT).show()
            mactivity.openFolderNameMenu(view)
        }
        view.findViewById<LinearLayout>(R.id.new_shortcut).setOnClickListener{
            //Toast.makeText(this, "New shortcut", Toast.LENGTH_SHORT).show()
            mactivity.toggleAppMenu()
            mactivity.selectAppMenuOpened = true
            /*global_view.findViewById<TextView>(R.id.notification).apply{
                text = "Choose an app from app list or search. Click here to cancel."
                setOnClickListener {
                    mactivity.selectAppMenuOpened = false
                    it.visibility = View.INVISIBLE
                    mactivity.recycle_view_label.visibility = View.GONE
                }
                visibility = View.VISIBLE
            }*/
            mactivity.shortcutPopup?.dismiss()
            mactivity.recycle_view_label.visibility = View.VISIBLE
            //changeeditMode()
            //toggleAppMenu()
        }
        view.findViewById<LinearLayout>(R.id.new_action).setOnClickListener{
            Toast.makeText(requireContext(), "Not implemented yet:(", Toast.LENGTH_SHORT).show()
        }
        return view
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
                }
            }
        } else {
            Log.d("ingo", "launchLastEntered shortcut " + viewModel.lastEnteredIntent.toString())
            viewModel.lastEnteredIntent?.let { mactivity.startShortcut(it) }
        }
        viewModel.lastEnteredIntent = null
        viewModel.lastTextViewEnteredCounter = -1
        circleView.changeMiddleButtonState(CircleView.MIDDLE_BUTTON_HIDE)
        goToPocetna()
    }

    fun enterSelected(){
        if(viewModel.editSelected == -1) return
        prebaciMeni(viewModel.currentSubmenuList[viewModel.editSelected].id, viewModel.editSelected)
        deYellowAll()
    }

    fun touched2(event:MotionEvent, counter:Int) {
        if(counter >= 0) {
            when (counter) {
                0 -> addNew()
                1 -> mactivity.showLayout(MainActivity.LAYOUT_ACTIVITIES)
                2 -> changeeditMode()
                3 -> {
                    mactivity.showLayout(MainActivity.LAYOUT_SEARCH)//initializeSearch()
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
                -4 -> changeeditMode()
            }
        }
    }

    fun collapseMenu(){
        bottomMenuView.collapse()
    }

    fun settings(){
        //startActivity(Intent(android.provider.Settings.ACTION_SETTINGS), null);
        mactivity.showLayout(MainActivity.LAYOUT_SETTINGS)

    }

    fun addNew(){
        Log.d("ingo", "it's add")
        if((circleView.amIHomeVar && viewModel.currentSubmenuList.size >= 8) || (!circleView.amIHomeVar && viewModel.currentSubmenuList.size >= 7)) {
            Toast.makeText(requireContext(), "Max. elements present. ", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("ingo", "openaddmenu")
            mactivity.openAddMenu()
        }
    }
    fun refreshCurrentMenu(){
        prebaciMeni(viewModel.currentMenu.id, viewModel.selected_global)
        Log.d("ingo", "current menu refreshed")
    }
    fun updateStuff() {
        //findViewById<RecyclerView>(R.id.recycler_view).adapter?.notifyDataSetChanged()
        //mactivity.radapter.notifyItemInserted(mactivity.radapter.getItemCount() - 1)
        mactivity.radapter.submitList(viewModel.appsList.value)
        mactivity.radapter.notifyDataSetChanged()
    }
    fun deYellowAll(){
        circleView.deyellowAll()
        viewModel.editSelected = -1
        bottomMenuView.selectedId = -1
        bottomMenuView.invalidate()
    }
    fun prebaciMeni(id:Int, counter:Int, nostack:Boolean=false, precaci:Boolean=false): MeniJednoPolje? {

        val polje = getPolje(id)
        Log.d("ingo", "prebaciMeni " + id)
        if(polje != null){
            viewModel.currentMenu = polje
            prikaziPrecace(polje.id, counter)
            /*if(!precaci) {
                prikazi(polje.id, counter)
            } else {

            }*/

            selected_text.text = polje.text
            if(!nostack){
                viewModel.stack.add(Pair(viewModel.currentMenu.id, counter))
                Log.d("ingo", "adding " + viewModel.currentMenu.text + " to viewModel.stack.")
                //findViewById<Button>(R.id.back_button).isEnabled = true
            }
            viewModel.lastTextViewEnteredCounter = counter
            return polje
        } else {
            Log.d("ingo", "prebaciMeni null!")
        }
        return null
    }
    fun prikaziPrecace(prosiriId: Int, selected:Int){
        var precaci:MutableList<MeniJednoPolje> = mutableListOf()
        val trenutnoPolje = getPolje(prosiriId)
        val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if(launcherApps.hasShortcutHostPermission()){
            val precaci_info = trenutnoPolje?.let {
                mactivity.getShortcutFromPackage(
                    it.nextIntent
                )
            }
            //Log.d("ingo", "prikazi prečace2 " + precaci.map{ it.shortLabel.toString() + "->" + it.id }.toString())
            precaci = precaci_info?.map{
                MeniJednoPolje(id=0, text= it.shortLabel as String, nextIntent = it.`package`, nextId = it.id, shortcut = true)
            } as MutableList<MeniJednoPolje>
        }
        if(prosiriId == viewModel.pocetnaId){
            circleView.amIHome(true)
        } else {
            circleView.amIHome(false)
        }
        Log.d("ingo", "prikazi prečace " + precaci.map{ it.text + "->" + it.nextIntent + "->" + it.nextId }.toString())
        var polja = getSubPolja(prosiriId)
        if(trenutnoPolje?.nextIntent != "" ) {
            precaci.add(MeniJednoPolje(id=0, text= "App info", nextIntent = MainActivity.ACTION_APPINFO, nextId = trenutnoPolje!!.nextIntent))
        }
        viewModel.currentSubmenuList = precaci + polja
        circleView.setColorList(IntArray(precaci.size) { Color.WHITE }.map{it.toString()} + polja.map{ it.color })
        circleView.setTextList(viewModel.currentSubmenuList)
        circleView.setPosDontDraw(selected)
        viewModel.selected_global = selected
        viewModel.max_subcounter = (viewModel.currentSubmenuList).size
    }
    fun getPolje(id:Int): MeniJednoPolje?{
        for(polje in viewModel.listaMenija){
            if(polje.id == id) return polje
        }
        return null
    }
    fun changeeditMode(){
        viewModel.editMode = !viewModel.editMode
        circleView.editMode = !circleView.editMode!!
        circleView.changeMiddleButtonState(CircleView.MIDDLE_BUTTON_EDIT)
        circleView.invalidate()
        if(viewModel.editMode){
            bottomMenuView.editMode = true
            bottomMenuView.postInvalidate()
            circleView.invalidate()
        } else {
            deYellowAll()
            bottomMenuView.editMode = false
            bottomMenuView.postInvalidate()
        }
    }
    fun goToPocetna(){
        Log.d("ingo", "pocetna " + viewModel.pocetnaId)
        viewModel.stack.clear()
        prebaciMeni(viewModel.pocetnaId, -1)
        selected_text.setText(mactivity.resources2.getString(R.string.home))
        //prikaziPrecace
        //findViewById<Button>(R.id.back_button).isEnabled = false
    }
    fun getSubPolja(id:Int):List<MeniJednoPolje>{
        var lista:MutableList<MeniJednoPolje> = mutableListOf()
        var polje1 = getPolje(id)
        if(polje1 != null) {
            for (polje2 in viewModel.listaMenija) {
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

}