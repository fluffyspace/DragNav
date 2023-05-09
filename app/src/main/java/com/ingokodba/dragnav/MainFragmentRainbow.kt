package com.ingokodba.dragnav

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.google.android.material.slider.Slider
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragmentRainbow.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragmentRainbow() : Fragment(), MainFragmentInterface {

    lateinit var circleView: RightHandCircleView
    lateinit var relativeLayout: ConstraintLayout
    private val viewModel: ViewModel by activityViewModels()
    lateinit var mactivity:MainActivity
    lateinit var global_view:View

    override var fragment: Fragment = this
    var sliders = false

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mactivity = (activity as MainActivity)
        //MainActivity.changeLocale(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        circleView = view.findViewById(R.id.circleview)
        relativeLayout = view.findViewById(R.id.relativelayout)

        Log.d("ingo", "detectSize e " +
            mactivity.getPreferences(MODE_PRIVATE).getFloat("detectSize", 0f).toString()
        )

        circleView.overrideDistance = mactivity.getPreferences(MODE_PRIVATE).getFloat("distance", 0f).let { if(it != 0f) it else null }
        circleView.overrideDetectSize = mactivity.getPreferences(MODE_PRIVATE).getFloat("detectSize", 0f).let { if(it != 0f) it else null }

        view.findViewById<Slider>(R.id.detectSize).addOnChangeListener { slider, value, fromUser ->
            circleView.overrideDetectSize = value
            Log.d("ingo", "change1 to $value")
            circleView.invalidate()
            changeSettings("detectSize", value)
        }
        view.findViewById<Slider>(R.id.distance).addOnChangeListener { slider, value, fromUser ->
            circleView.overrideDistance = value
            circleView.invalidate()
            changeSettings("distance", value)
            Log.d("ingo", "change2 to $value")
        }
        view.findViewById<ImageButton>(R.id.sliders).setOnClickListener {
            sliders = !sliders
            if(sliders){
                view.findViewById<Slider>(R.id.detectSize).visibility = View.VISIBLE
                view.findViewById<Slider>(R.id.distance).visibility = View.VISIBLE
            } else {
                view.findViewById<Slider>(R.id.detectSize).visibility = View.GONE
                view.findViewById<Slider>(R.id.distance).visibility = View.GONE
            }
        }

        view.findViewById<ImageButton>(R.id.settings).setOnClickListener {
            settings()
        }
        /*viewModel.icons.observe(this) {
            circleView.icons = it
            circleView.lala()
            Log.d("ingo", "icons loaded")
        }*/
        //circleView?.mactivity.radapter = mactivity.radapter
        circleView.setEventListener(object :
            IMyEventListener {
            override fun onEventOccurred(event: MotionEvent, app_index: Int) {
                Log.d("ingo", "onEventOccurred")
                touched(event, app_index)
            }
        })
        global_view = view
        relativeLayout.setOnClickListener {

            Log.d("ingo", "collapse?")
        }
        if(viewModel.icons.value != null){
            circleView.icons = viewModel.icons.value!!
            Log.d("ingo", "icons who??")
        }


        if (viewModel.currentMenuId == -1) {
            goToPocetna()
        } else {
            refreshCurrentMenu()
        }

        Log.d("ingo", "mainfragment created")
    }

    private fun changeSettings(key: String, value: Float){
        Log.d("ingo", "should write $key as $value")
        val sharedPreferences: SharedPreferences = activity!!.getPreferences(MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat(key, value)
        editor.apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_rainbow, container, false)
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
            MainFragmentRainbow().apply {

            }
    }

    override fun iconsUpdated(){
        circleView.icons = viewModel.icons.value!!
        circleView.invalidate()
    }

    fun cancelAddingAppHandler(){
        mactivity.addingNewAppEvent = null
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

    fun touched(event:MotionEvent, app_index:Int) {
        Log.d("ingo", "touched " + app_index)
        val launchIntent: Intent? =
            viewModel.appsList.value?.get(app_index)
                ?.let { requireContext().packageManager.getLaunchIntentForPackage(it.packageName) }
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    fun enterSelected(){
        if(viewModel.editSelected == -1) return
        prebaciMeni(viewModel.trenutnoPrikazanaPolja[viewModel.editSelected].id, viewModel.editSelected)
        deYellowAll()
    }

    fun collapseMenu(){
        //bottomMenuView.collapse()
    }

    fun settings(){
        //startActivity(Intent(android.provider.Settings.ACTION_SETTINGS), null);
        mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_SETTINGS)

    }

    interface IMyEventListener {
        fun onEventOccurred(event: MotionEvent, counter:Int)
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

    override fun refreshCurrentMenu(){
        circleView.updateDesign()
        circleView.invalidate()
        prebaciMeni(viewModel.currentMenuId, viewModel.no_draw_position)
        Log.d("ingo", "current menu refreshed")
    }
    override fun updateStuff() {
        //findViewById<RecyclerView>(R.id.recycler_view).adapter?.notifyDataSetChanged()
        //mactivity.radapter.notifyItemInserted(mactivity.radapter.getItemCount() - 1)
        //mactivity.radapter.submitList(viewModel.appsList.value)
        //mactivity.radapter.notifyDataSetChanged()
    }
    fun deYellowAll(){
        circleView.deselectAll()
        viewModel.editSelected = -1
    }
    fun prebaciMeni(id:Int, counter:Int, nostack:Boolean=false, precaci:Boolean=false): KrugSAplikacijama? {
        val polje = getPolje(id)
        Log.d("ingo", "prebaciMeni " + id)
        if(polje != null){
            viewModel.currentMenu = polje
            viewModel.currentMenuId = id
            prikaziPoljaKruga(polje.id, counter)


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
                KrugSAplikacijama(id=0, text= it.shortLabel as String, nextIntent = it.`package`, nextId = it.id, shortcut = true)
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
        circleView.setKrugSAplikacijamaList(viewModel.appsList.value!!)
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
            circleView.invalidate()
        } else {
            deYellowAll()
        }
    }
    override fun goToPocetna(){
        Log.d("ingo", "pocetna " + viewModel.pocetnaId)
        viewModel.stack.clear()
        prebaciMeni(viewModel.pocetnaId, -1)
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