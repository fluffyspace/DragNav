package com.ingokodba.dragnav

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.ListPopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.google.android.material.slider.Slider
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
import com.ingokodba.dragnav.modeli.RainbowMapa
import com.skydoves.colorpickerview.kotlin.colorPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragmentRainbow.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragmentRainbow() : Fragment(), MainFragmentInterface, OnShortcutClick {

    lateinit var circleView: Rainbow
    lateinit var relativeLayout: ConstraintLayout
    private val viewModel: ViewModel by activityViewModels()
    lateinit var mactivity:MainActivity
    lateinit var global_view:View
    var countdown: Job? = null
    var fling: Job? = null
    var app_index: Int? = null
    var gcolor: Int? = null

    var dialogState: DialogStates? = null

    var shortcuts_recycler_view: RecyclerView? = null

    var shortcutPopup:PopupWindow? = null
    var radapter: ShortcutsAdapter? = null

    override var fragment: Fragment = this
    var sliders = false
    var onlyfavorites = false
    var shortcuts: List<ShortcutInfo> = listOf()

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

        Log.d(
            "ingo", "detectSize e " +
                    mactivity.getPreferences(MODE_PRIVATE).getFloat("detectSize", 0f).toString()
        )

        circleView.overrideDistance =
            mactivity.getPreferences(MODE_PRIVATE).getFloat("distance", 0f)
                .let { if (it != 0f) it else null }
        circleView.overrideDetectSize =
            mactivity.getPreferences(MODE_PRIVATE).getFloat("detectSize", 0f)
                .let { if (it != 0f) it else null }
        circleView.overrideStep = mactivity.getPreferences(MODE_PRIVATE).getFloat("step", 0f)
            .let { if (it != 0f) it else null }
        onlyfavorites = mactivity.getPreferences(MODE_PRIVATE).getBoolean("onlyfavorites", false)

        view.findViewById<CheckBox>(R.id.onlyfavoriteapps).let {
            it.setOnCheckedChangeListener { _, isChecked ->
                toggleFavorites()
            }
            it.isChecked = onlyfavorites
        }
        view.findViewById<Slider>(R.id.detectSize).let {
            it.addOnChangeListener { _, value, _ ->
                circleView.overrideDetectSize = value
                Log.d("ingo", "change1 to $value")
                circleView.invalidate()
                changeSettings("detectSize", value)
            }
            if (circleView.overrideDetectSize != null) it.value = circleView.overrideDetectSize!!
        }
        view.findViewById<Slider>(R.id.distance).let {
            it.addOnChangeListener { _, value, _ ->
                circleView.overrideDistance = value
                circleView.invalidate()
                changeSettings("distance", value)
                Log.d("ingo", "change2 to $value")
            }
            if (circleView.overrideDistance != null) it.value = circleView.overrideDistance!!
        }
        view.findViewById<Slider>(R.id.step).let{
            it.addOnChangeListener { _, value, _ ->
                circleView.overrideStep = value
                circleView.invalidate()
                changeSettings("step", value)
                Log.d("ingo", "change3 to $value")
            }
            if (circleView.overrideStep != null) it.value = circleView.overrideStep!!
        }
        view.findViewById<ImageButton>(R.id.sliders).setOnClickListener {
            sliders = !sliders
            if(sliders){
                view.findViewById<Slider>(R.id.detectSize).visibility = View.VISIBLE
                view.findViewById<Slider>(R.id.distance).visibility = View.VISIBLE
                view.findViewById<Slider>(R.id.step).visibility = View.VISIBLE
            } else {
                view.findViewById<Slider>(R.id.detectSize).visibility = View.GONE
                view.findViewById<Slider>(R.id.distance).visibility = View.GONE
                view.findViewById<Slider>(R.id.step).visibility = View.GONE
            }
        }

        view.findViewById<ImageButton>(R.id.settings).setOnClickListener {
            settings()
        }
        circleView.setEventListener(object :
            IMyEventListener {
            override fun onEventOccurred(app:EventTypes, counter: Int) {
                when(app){
                    EventTypes.OPEN_APP->touched(counter)
                    EventTypes.START_COUNTDOWN->startCountdown()
                    EventTypes.STOP_COUNTDOWN->stopCountdown()
                    EventTypes.OPEN_SHORTCUT->openShortcut(counter)
                    EventTypes.TOGGLE_FAVORITES->toggleFavorites()
                    EventTypes.START_FLING->startFlingAnimation()
                }
                Log.d("ingo", "onEventOccurred " + app.toString())

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
        goToHome()
        Log.d("ingo", "mainfragment created")
    }

    fun stopCountdown(){
        countdown?.cancel()
        Log.d("ingo", "job canceled")
    }

    fun startFlingAnimation(){
        fling = lifecycleScope.launch(Dispatchers.IO) {
            while(circleView.flingOn) {
                delay(1000 / 100)
                //Log.d("ingo", (1000/24).toString())
                withContext(Dispatchers.Main) {
                    circleView.flingUpdate()
                }
            }
        }
    }

    fun openCreateFolderDialog(){
        (activity as MainActivity).openFolderNameMenu(this.circleView, false) {
            val nova_mapa = RainbowMapa(0, it, mutableListOf(viewModel.rainbowAll.value!![app_index!!].apps.first()))
            viewModel.addRainbowMape(mutableListOf(nova_mapa))
            (activity as MainActivity).rainbowMapaInsertItem(nova_mapa)
            saveCurrentMoveDistance()
            prebaciMeni()
        }
    }

    fun startCountdown(){
        countdown = lifecycleScope.launch(Dispatchers.IO) {
            delay(250)
            withContext(Dispatchers.Main){
                val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                if(launcherApps.hasShortcutHostPermission()) {
                    app_index = circleView.getAppIndexImIn()
                    if (app_index != null) {
                        // je li mapa ili aplikacija
                        val thing = viewModel.rainbowAll.value!![app_index!!]
                        if(thing.folderName == null) {
                            shortcuts = mactivity.getShortcutFromPackage(
                                thing.apps.first().packageName
                            )
                            openShortcutsMenu()
                            if (shortcuts.isEmpty()) {
                                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            Log.d("ingo", "precaci ${shortcuts.map { it.id + " " + it.`package` }}")
                        } else {
                            Log.d("ingo", "držali smo mapu")
                            val actions = listOf(ShortcutAction("Preimenuj mapu", getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24)), ShortcutAction("Izbriši mapu", getDrawable(R.drawable.ic_baseline_delete_24)), if(thing.favorite == true) ShortcutAction("Makni iz omiljenih", getDrawable(R.drawable.star_fill)) else ShortcutAction("Dodaj u omiljene", getDrawable(R.drawable.star_empty)))
                            dialogState = DialogStates.FOLDER_OPTIONS
                            showDialogWithActions(actions)
                        }
                        circleView.clickIgnored = true
                    }
                }
            }
        }
    }

    fun openShortcut(index: Int){
        val thing = viewModel.rainbowAll.value!![app_index!!]
        if(thing.folderName == null) {
            if (index >= shortcuts.size) {
                if (index == shortcuts.size) {
                    // toggle app as favorite
                    Log.d("ingo", "toggle map as favorite")
                    val app = thing.apps.first()
                    app.favorite = !app.favorite
                    (activity as MainActivity).saveAppInfo(app)
                    circleView.invalidate()
                    shortcutPopup?.dismiss()
                }
                if (index == shortcuts.size + 1) {
                    Log.d("ingo", "dodavanje u mapu")
                    // dodavanje u mapu (prikaži novi dijalog s opcijama "Nova mapa" i popis svih ostalih mapa) ili micanje iz mape
                    if (isAppAlreadyInMap(thing.apps.first())) {
                        // micanje

                        val mapa = viewModel.rainbowMape.value!!.find{ it.apps.contains(thing.apps.first())}
                        if(mapa != null) {
                            val azurirana_mapa = mapa.copy(apps = mapa.apps.minus(thing.apps.first()).toMutableList())
                            viewModel.updateRainbowMapa(azurirana_mapa)
                            (activity as MainActivity).rainbowMapaUpdateItem(azurirana_mapa)
                            prebaciMeni()
                        }
                        shortcutPopup?.dismiss()
                    } else {
                        Log.d("ingo", "prikazivanje novog izbornika")
                        dialogState = DialogStates.ADDING_TO_FOLDER
                        showDialogWithActions(viewModel.rainbowMape.value!!.map { ShortcutAction(it.folderName, getDrawable(R.drawable.baseline_folder_24)) }.toMutableList()
                            .apply { add(ShortcutAction("Nova mapa", getDrawable(R.drawable.ic_baseline_create_new_folder_50))) })
                    }
                }
                return
            }
            shortcutPopup?.dismiss()
            val launcherApps: LauncherApps =
                requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            try {
                launcherApps.startShortcut(
                    shortcuts[index].`package`,
                    shortcuts[index].id,
                    null,
                    null,
                    android.os.Process.myUserHandle()
                )
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: android.content.ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    fun isAppAlreadyInMap(whatApp: AppInfo): Boolean{
        return viewModel.rainbowMape.value!!.find { mape -> mape.apps.find{ app -> app.packageName == whatApp.packageName} != null } != null
    }

    fun showDialogWithActions(actions: List<ShortcutAction>){
        val contentView = LayoutInflater.from(context).inflate(R.layout.popup_shortcut, null)
        radapter = ShortcutsAdapter(requireContext(), this)
        shortcuts_recycler_view = contentView.findViewById(R.id.shortcutList)
        radapter?.actionsList = actions
        shortcuts_recycler_view?.adapter = radapter
        shortcuts_recycler_view?.addItemDecoration(SimpleDivider(requireContext()))

        shortcutPopup?.dismiss()
        shortcutPopup = PopupWindow(contentView,
            ListPopupWindow.WRAP_CONTENT,
            ListPopupWindow.WRAP_CONTENT, true)
        //shortcutPopup?.animationStyle = R.style.PopupAnimation
        shortcutPopup?.showAtLocation(this@MainFragmentRainbow.circleView, Gravity.CENTER, 0, 0)
    }

    fun openShortcutsMenu(){
        val appDrawable = viewModel.icons.value!![viewModel.rainbowAll.value!![app_index!!].apps.first().packageName]
        val actions = shortcuts.map{ShortcutAction(it.shortLabel.toString(), appDrawable)}.toMutableList().apply {
            add(if(viewModel.rainbowAll.value!![app_index!!].apps.first().favorite) ShortcutAction("Makni iz omiljenih", getDrawable(R.drawable.star_fill)) else ShortcutAction("Dodaj u omiljene", getDrawable(R.drawable.star_empty)))
            add(if(isAppAlreadyInMap(viewModel.rainbowAll.value!![app_index!!].apps.first())) ShortcutAction("Makni iz mape", getDrawable(R.drawable.baseline_folder_off_24)) else ShortcutAction("Dodaj u mapu", getDrawable(R.drawable.ic_baseline_create_new_folder_50)))
        }
        dialogState = DialogStates.APP_SHORTCUTS
        showDialogWithActions(actions)
    }

    fun getDrawable(resourceId: Int): Drawable?{
        return ResourcesCompat.getDrawable(resources, resourceId, null)?.apply { setTint(Color.BLACK) }
    }

    fun addAppToMap(map_index: Int){
        shortcutPopup?.dismiss()
        if(map_index >= viewModel.rainbowMape.value!!.size){
            // nova mapa
            openCreateFolderDialog()
        } else {
            val mapa = viewModel.rainbowMape.value!![map_index]
            val nova_mapa = mapa.copy(apps = mapa.apps.plus(viewModel.rainbowAll.value!![app_index!!].apps.first()).toMutableList())
            Log.d("ingo", "addAppToMap $map_index $mapa $nova_mapa ${viewModel.rainbowAll.value!![app_index!!].apps.first()}")
            viewModel.updateRainbowMapa(nova_mapa)
            (activity as MainActivity).rainbowMapaUpdateItem(nova_mapa)
        }
        saveCurrentMoveDistance()
        prebaciMeni()
    }

    fun toggleFavorites(){
        if(!circleView.inFolder){
            saveCurrentMoveDistance()
            if(onlyfavorites){
                circleView.moveDistancedAccumulated = viewModel.modeDistanceAccumulated
            } else {
                circleView.moveDistancedAccumulated = viewModel.modeDistanceAccumulatedFavorites
            }
            changeSettings("onlyfavorites", !onlyfavorites)
            onlyfavorites = !onlyfavorites
            circleView.onlyfavorites = onlyfavorites
        }
        prebaciMeni()
    }

    private fun changeSettings(key: String, value: Any){
        Log.d("ingo", "should write $key as $value")
        val sharedPreferences: SharedPreferences = requireActivity().getPreferences(MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        if(value::class == Boolean::class) {
            editor.putBoolean(key, value as Boolean)
        } else if(value::class == Float::class) {
            editor.putFloat(key, value as Float)
        }
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

    fun touched(app_index:Int) {
        if(viewModel.rainbowAll.value!![app_index].folderName == null) {
            Log.d("ingo", "touched $app_index ${viewModel.rainbowAll.value!![app_index]}")
            val launchIntent: Intent? =
                viewModel.rainbowAll.value!![app_index].let {
                    requireContext().packageManager.getLaunchIntentForPackage(
                        it.apps.first().packageName
                    )
                }
            if (launchIntent != null) {
                saveCurrentMoveDistance()
                startActivity(launchIntent)
            }
        } else {
            otvoriMapu(app_index)
        }
    }

    fun settings(){
        //startActivity(Intent(android.provider.Settings.ACTION_SETTINGS), null);
        mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_SETTINGS)
    }

    interface IMyEventListener {
        fun onEventOccurred(app: EventTypes, counter:Int)
    }
    override fun refreshCurrentMenu(){
        circleView.updateDesign()
        circleView.invalidate()
        prebaciMeni()
        Log.d("ingo", "current menu refreshed")
    }

    fun deYellowAll(){
        circleView.deselectAll()
        viewModel.editSelected = -1
    }
    fun prebaciMeni() {
        prikaziPoljaKruga(onlyfavorites)
    }

    fun saveCurrentMoveDistance(){
        if(onlyfavorites){
            viewModel.modeDistanceAccumulatedFavorites = circleView.moveDistancedAccumulated
        } else {
            viewModel.modeDistanceAccumulated = circleView.moveDistancedAccumulated
        }
    }

    fun otvoriMapu(index: Int){
        val aplikacije = viewModel.rainbowAll.value!![index].apps
        Log.d("ingo", "aplikacije $aplikacije")
        viewModel.setRainbowAll(aplikacije.map{EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)}.toMutableList())
        Log.d("ingo", "aplikacije ${viewModel.rainbowAll.value!!}")
        circleView.inFolder = true
        saveCurrentMoveDistance()
        circleView.moveDistancedAccumulated = 0
        circleView.setAppInfoList(viewModel.rainbowAll.value!!)
        circleView.invalidate()
    }

    fun prikaziPoljaKruga(onlyfavorites: Boolean){
        if(viewModel.appsList.value == null) return
        viewModel.updateRainbowAll(onlyfavorites)

        Log.d("ingo", "viewModel.appslist ${viewModel.appsList.value.toString()}")
        for(enc in viewModel.rainbowAll.value!!.map{it.apps}){
            Log.d("ingo", "rainbowAll $enc")
        }

        circleView.inFolder = false
        circleView.onlyfavorites = onlyfavorites
        if(onlyfavorites){
            circleView.moveDistancedAccumulated = viewModel.modeDistanceAccumulatedFavorites
        } else {
            circleView.moveDistancedAccumulated = viewModel.modeDistanceAccumulated
        }
        circleView.setAppInfoList(viewModel.rainbowAll.value!!)
        //circleView.invalidate()
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

    override fun onPause() {
        super.onPause()
        countdown?.cancel()
        fling?.cancel()
    }
    override fun goToHome(){
        Log.d("ingo", "pocetna " + viewModel.pocetnaId)
        viewModel.stack.clear()
        prebaciMeni()
        //prikaziPrecace
        //findViewById<Button>(R.id.back_button).isEnabled = false
    }

    fun folderOptions(index: Int){
        shortcutPopup?.dismiss()
        when(index){
            0 -> {
                // uredi mapu (preimenuj)
                (activity as MainActivity).openFolderNameMenu(this.circleView, true){ime ->
                    val mapa = viewModel.rainbowMape.value!!.find{ it.folderName == viewModel.rainbowAll.value!![app_index!!].folderName }
                    val nova_mapa = mapa?.copy(folderName = ime)
                    if (nova_mapa != null) {
                        viewModel.updateRainbowMapa(nova_mapa)
                        (activity as MainActivity).rainbowMapaUpdateItem(nova_mapa)
                    }
                    saveCurrentMoveDistance()
                    prebaciMeni()
                }
            }
            1 -> {
                // obriši mapu
                val mapa = viewModel.rainbowMape.value!!.find{ it.folderName == viewModel.rainbowAll.value!![app_index!!].folderName }
                if (mapa != null) {
                    viewModel.deleteRainbowMapa(mapa)
                    (activity as MainActivity).rainbowMapaDeleteItem(mapa)
                }
                saveCurrentMoveDistance()
                prebaciMeni()
            }
            2 -> {
                // dodaj pod omiljeno
                val mapa = viewModel.rainbowMape.value!!.find{ it.folderName == viewModel.rainbowAll.value!![app_index!!].folderName }
                val nova_mapa = mapa?.copy(favorite = !mapa.favorite)
                if (nova_mapa != null) {
                    viewModel.updateRainbowMapa(nova_mapa)
                    (activity as MainActivity).rainbowMapaUpdateItem(nova_mapa)
                }
                saveCurrentMoveDistance()
                prebaciMeni()
            }
        }
    }

    override fun onShortcutClick(index: Int) {
        Log.d("ingo", "clicked on shortcut...")
        when(dialogState){
            DialogStates.APP_SHORTCUTS -> openShortcut(index)
            DialogStates.ADDING_TO_FOLDER -> addAppToMap(index)
            DialogStates.FOLDER_OPTIONS -> folderOptions(index)
            else -> {}
        }
    }

}

enum class EventTypes{OPEN_APP, START_COUNTDOWN, STOP_COUNTDOWN, OPEN_SHORTCUT, TOGGLE_FAVORITES, START_FLING}
enum class DialogStates{APP_SHORTCUTS, ADDING_TO_FOLDER, FOLDER_OPTIONS}

interface OnShortcutClick {
    fun onShortcutClick(index: Int)
}