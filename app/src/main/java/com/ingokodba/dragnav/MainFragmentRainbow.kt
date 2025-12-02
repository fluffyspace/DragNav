package com.ingokodba.dragnav

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.R
import com.google.android.material.slider.Slider
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
import com.ingokodba.dragnav.modeli.RainbowMapa
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
class MainFragmentRainbow(leftOrRight: Boolean = true) : Fragment(), MainFragmentInterface, OnShortcutClick {

    lateinit var circleView: Rainbow
    lateinit var relativeLayout: ConstraintLayout
    private val viewModel: ViewModel by activityViewModels()
    lateinit var mactivity:MainActivity
    lateinit var global_view:View
    var countdown: Job? = null
    var fling: Job? = null
    var app_index: Int? = null
    var gcolor: Int? = null

    var globalThing: EncapsulatedAppInfoWithFolder? = null

    var dialogState: DialogStates? = null

    override var fragment: Fragment = this
    var sliders = false
    var onlyfavorites = false
    var shortcuts: List<ShortcutInfo> = listOf()
    var leftOrRight: Boolean

    init {
        this.leftOrRight = leftOrRight
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mactivity = (activity as MainActivity)
        //MainActivity.changeLocale(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        circleView = view.findViewById(R.id.circleview)
        circleView.leftOrRight = leftOrRight
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
        //onlyfavorites = mactivity.getPreferences(MODE_PRIVATE).getBoolean("onlyfavorites", false)

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
        view.findViewById<Slider>(R.id.radius).let{
            it.addOnChangeListener { _, value, _ ->
                circleView.overrideRadius = value
                circleView.invalidate()
                changeSettings("radius", value)
                Log.d("ingo", "change radius to $value")
            }
            it.value = circleView.overrideRadius ?: 1f
        }
        view.findViewById<Slider>(R.id.arcRotation).let{
            it.addOnChangeListener { _, value, _ ->
                circleView.overrideArcPosition = value
                circleView.invalidate()
                changeSettings("arcPosition", value)
                Log.d("ingo", "change arcPosition to $value")
            }
            it.value = circleView.overrideArcPosition ?: 0f
        }
        view.findViewById<ImageButton>(R.id.close_sliders).setOnClickListener {
            toggleSliders()
        }

        circleView.setEventListener(object :
            IMyEventListener {
            override fun onEventOccurred(app:EventTypes, counter: Int) {
                when(app){
                    EventTypes.OPEN_APP->touched(counter)
                    EventTypes.START_COUNTDOWN->startCountdown(counter)
                    EventTypes.STOP_COUNTDOWN->stopCountdown()
                    //EventTypes.OPEN_SHORTCUT->openShortcut(counter)
                    EventTypes.TOGGLE_FAVORITES->toggleFavorites()
                    EventTypes.START_FLING->startFlingAnimation()
                    else -> {}
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
            withContext(Dispatchers.Main) {
                circleView.invalidate()
            }
        }
    }

    fun openCreateFolderDialog(){
        (activity as MainActivity).openFolderNameMenu(this.circleView, false, "", false) {
            val novaMapa = RainbowMapa(0, it, mutableListOf(globalThing!!.apps.first()), true)
            (activity as MainActivity).rainbowMapaInsertItem(novaMapa)
        }
    }

    fun startCountdown(){
        countdown = lifecycleScope.launch(Dispatchers.IO) {
            delay(250)
            withContext(Dispatchers.Main){
                app_index = circleView.getAppIndexImIn()
                /*if(onlyfavorites){

                } else {
                    val tmp = viewModel.rainbowFiltered.value!![circleView.getAppIndexImIn()!!]
                    if(tmp.folderName == null){
                        app_index = viewModel.rainbowAll.indexOfFirst { it.folderName == null && it.apps[0].packageName == tmp.apps[0].packageName }
                    } else {
                        app_index = viewModel.rainbowAll.indexOfFirst { it.folderName == tmp.folderName }
                    }
                }*/
                if (app_index != null) {
                    // je li mapa ili aplikacija
                    val thing = viewModel.rainbowFiltered[app_index!!]
                    Log.d("ingo3", "$app_index $thing")
                    if(thing.folderName == null) {
                        openShortcutsMenu(thing)
                        if (shortcuts.isEmpty()) {
                            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        Log.d("ingo", "precaci ${shortcuts.map { it.id + " " + it.`package` }}")
                    } else {
                        Log.d("ingo", "držali smo mapu")
                        val actions = listOf(ShortcutAction(getTranslatedString(R.string.rename_folder), getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24)), ShortcutAction(getTranslatedString(R.string.delete_folder), getDrawable(R.drawable.ic_baseline_delete_24)), if(thing.favorite == true) ShortcutAction(getTranslatedString(R.string.remove_from_favorites), getDrawable(R.drawable.star_fill)) else ShortcutAction(getTranslatedString(R.string.add_to_favorites), getDrawable(R.drawable.star_empty)))
                        dialogState = DialogStates.FOLDER_OPTIONS
                        mactivity.showDialogWithActions(actions, this@MainFragmentRainbow, this@MainFragmentRainbow.circleView)
                    }
                    circleView.clickIgnored = true
                }
            }
        }
    }

    fun openShortcut(index: Int){
        val thing = globalThing!!
        if(thing.folderName == null) {
            if (index >= shortcuts.size) {
                if (index == shortcuts.size) {
                    // app info
                    Log.d("ingo", "app info")
                    val app = thing.apps.first()
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", app.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    mactivity.shortcutPopup?.dismiss()
                } else if (index == shortcuts.size + 1) {
                    // toggle app as favorite
                    Log.d("ingo", "toggle map as favorite")
                    val app = thing.apps.first()
                    app.favorite = !app.favorite
                    (activity as MainActivity).saveAppInfo(app)
                    circleView.invalidate()
                    mactivity.shortcutPopup?.dismiss()
                } else if (index == shortcuts.size + 2) {
                    Log.d("ingo", "dodavanje u mapu")
                    // dodavanje u mapu (prikaži novi dijalog s opcijama "Nova mapa" i popis svih ostalih mapa) ili micanje iz mape
                    if (mactivity.isAppAlreadyInMap(thing.apps.first())) {
                        // micanje

                        val mapa = viewModel.rainbowMape.value!!.find{ it.apps.contains(thing.apps.first())}
                        if(mapa != null) {
                            val azurirana_mapa = mapa.copy(apps = mapa.apps.minus(thing.apps.first()).toMutableList())
                            viewModel.updateRainbowMapa(azurirana_mapa)
                            (activity as MainActivity).rainbowMapaUpdateItem(azurirana_mapa)
                            prebaciMeni()
                        }
                        mactivity.shortcutPopup?.dismiss()
                    } else {
                        Log.d("ingo", "prikazivanje novog izbornika")
                        dialogState = DialogStates.ADDING_TO_FOLDER
                        mactivity.showDialogWithActions(viewModel.rainbowMape.value!!.map { ShortcutAction(it.folderName, getDrawable(R.drawable.baseline_folder_24)) }.toMutableList()
                            .apply { add(ShortcutAction("Nova mapa", getDrawable(R.drawable.ic_baseline_create_new_folder_50))) }, this, this@MainFragmentRainbow.circleView)
                    }
                }
                return
            }
            mactivity.shortcutPopup?.dismiss()
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

    fun openShortcutsMenu(thing: EncapsulatedAppInfoWithFolder){
        globalThing = thing
        val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if(launcherApps.hasShortcutHostPermission()) {
            shortcuts = mactivity.getShortcutFromPackage(
                thing.apps.first().packageName
            )
        } else {
            shortcuts = listOf()
        }
        val appDrawable = viewModel.icons.value!![globalThing!!.apps.first().packageName]
        val actions = shortcuts.map{ShortcutAction(it.shortLabel.toString(), appDrawable)}.toMutableList().apply {
            add(ShortcutAction(getTranslatedString(R.string.app_info), getDrawable(R.drawable.ic_outline_info_75)))
            add(if(globalThing!!.apps.first().favorite) ShortcutAction(getTranslatedString(R.string.remove_from_favorites), getDrawable(R.drawable.star_fill)) else ShortcutAction(getTranslatedString(R.string.add_to_favorites), getDrawable(R.drawable.star_empty)))
            add(if(mactivity.isAppAlreadyInMap(globalThing!!.apps.first())) ShortcutAction(getTranslatedString(R.string.remove_from_folder), getDrawable(R.drawable.baseline_folder_off_24)) else ShortcutAction(getTranslatedString(R.string.add_to_folder), getDrawable(R.drawable.ic_baseline_create_new_folder_50)))
        }
        dialogState = DialogStates.APP_SHORTCUTS
        mactivity.showDialogWithActions(actions, this, this@MainFragmentRainbow.circleView)
    }

    fun getDrawable(resourceId: Int): Drawable?{
        return ResourcesCompat.getDrawable(resources, resourceId, null)?.apply {
           when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    setTint(Color.WHITE)
                }
                else -> {
                    setTint(Color.BLACK)
                }
            }
        }
    }

    fun addAppToMap(map_index: Int){
        mactivity.shortcutPopup?.dismiss()
        if(map_index >= viewModel.rainbowMape.value!!.size){
            // nova mapa
            openCreateFolderDialog()
        } else {
            val mapa = viewModel.rainbowMape.value!![map_index]
            val nova_mapa = mapa.copy(apps = mapa.apps.plus(globalThing!!.apps.first()).toMutableList())
            Log.d("ingo", "addAppToMap $map_index $mapa $nova_mapa ${globalThing!!.apps.first()}")
            viewModel.updateRainbowMapa(nova_mapa)
            (activity as MainActivity).rainbowMapaUpdateItem(nova_mapa)
        }
        saveCurrentMoveDistance()
        prebaciMeni()
    }

    fun toggleFavorites(){
        if(!circleView.inFolder){
            if(!onlyfavorites && viewModel.appsList.value!!.find { it.favorite } == null && viewModel.rainbowMape.value!!.find { it.favorite } == null){
                Toast.makeText(requireContext(), "No favorites", Toast.LENGTH_SHORT).show()
                return
            }
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

    fun toggleSliders(){
        sliders = !sliders
        val visibility = if (sliders) View.VISIBLE else View.GONE
        global_view.findViewById<TextView>(R.id.label_detectSize).visibility = visibility
        global_view.findViewById<Slider>(R.id.detectSize).visibility = visibility
        global_view.findViewById<TextView>(R.id.label_distance).visibility = visibility
        global_view.findViewById<Slider>(R.id.distance).visibility = visibility
        global_view.findViewById<TextView>(R.id.label_step).visibility = visibility
        global_view.findViewById<Slider>(R.id.step).visibility = visibility
        global_view.findViewById<TextView>(R.id.label_radius).visibility = visibility
        global_view.findViewById<Slider>(R.id.radius).visibility = visibility
        global_view.findViewById<TextView>(R.id.label_arcRotation).visibility = visibility
        global_view.findViewById<Slider>(R.id.arcRotation).visibility = visibility
        global_view.findViewById<ImageButton>(R.id.close_sliders).visibility = visibility
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
        fun newInstance(leftOrRight: Boolean) =
            MainFragmentRainbow(leftOrRight).apply {

            }

        fun getTranslatedString(id: Int): String{
            return MainActivity.resources2.getString(id)
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

    override fun onBackPressed(): Boolean {
        if(circleView.inFolder){
            prebaciMeni()
            return true
        }
        return false
    }

    fun touched(app_index:Int) {
        if(viewModel.rainbowFiltered[app_index].folderName == null) {
            Log.d("ingo", "touched $app_index ${viewModel.rainbowFiltered[app_index]}")
            val launchIntent: Intent? =
                viewModel.rainbowFiltered[app_index].let {
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
        val aplikacije = viewModel.rainbowFiltered[index].apps
        Log.d("ingo", "aplikacije $aplikacije")
        viewModel.setRainbowFilteredValues(aplikacije.map{EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)}.toMutableList())
        Log.d("ingo", "aplikacije ${viewModel.rainbowFiltered}")
        circleView.inFolder = true
        saveCurrentMoveDistance()
        circleView.moveDistancedAccumulated = 0
        circleView.setAppInfoList(viewModel.rainbowFiltered)
        circleView.invalidate()
    }

    fun prikaziPoljaKruga(onlyfavorites: Boolean){
        if(viewModel.appsList.value == null) return
        viewModel.updateRainbowFiltered(onlyfavorites)
        viewModel.updateRainbowAll()
/*
        Log.d("ingo", "viewModel.appslist ${viewModel.appsList.value.toString()}")
        for(enc in viewModel.rainbowAll.value!!.map{it.apps}){
            Log.d("ingo", "rainbowAll $enc")
        }
*/
        circleView.inFolder = false
        circleView.onlyfavorites = onlyfavorites
        if(onlyfavorites){
            circleView.moveDistancedAccumulated = viewModel.modeDistanceAccumulatedFavorites
        } else {
            circleView.moveDistancedAccumulated = viewModel.modeDistanceAccumulated
        }
        circleView.setAppInfoList(viewModel.rainbowFiltered)
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
        mactivity.shortcutPopup?.dismiss()
        when(index){
            0 -> {
                // uredi mapu (preimenuj)
                val mapa = viewModel.rainbowMape.value!!.find{ it.folderName == globalThing!!.folderName }
                (activity as MainActivity).openFolderNameMenu(this.circleView, true, mapa!!.folderName, false){ime ->
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
                val mapa = viewModel.rainbowMape.value!!.find{ it.folderName == globalThing!!.folderName }
                if (mapa != null) {
                    viewModel.deleteRainbowMapa(mapa)
                    (activity as MainActivity).rainbowMapaDeleteItem(mapa)
                }
                saveCurrentMoveDistance()
                prebaciMeni()
            }
            2 -> {
                // dodaj pod omiljeno
                val mapa = viewModel.rainbowMape.value!!.find{ it.folderName == globalThing!!.folderName }
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

enum class EventTypes{OPEN_APP, START_COUNTDOWN, STOP_COUNTDOWN, OPEN_SHORTCUT, TOGGLE_FAVORITES, START_FLING, TOGGLE_SLIDERS}