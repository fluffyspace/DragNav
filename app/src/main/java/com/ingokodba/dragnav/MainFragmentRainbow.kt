package com.ingokodba.dragnav

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.R
import com.google.android.material.slider.Slider
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
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
class MainFragmentRainbow() : Fragment(), MainFragmentInterface {

    lateinit var circleView: Rainbow
    lateinit var relativeLayout: ConstraintLayout
    private val viewModel: ViewModel by activityViewModels()
    lateinit var mactivity:MainActivity
    lateinit var global_view:View
    var countdown: Job? = null
    var fling: Job? = null
    var app_index: Int? = null

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
        view.findViewById<ImageButton>(R.id.close_sliders).setOnClickListener {
            toggleSliders()
        }

        view.findViewById<ImageButton>(R.id.settings).setOnClickListener {
            settings()
        }
        circleView.setEventListener(object :
            IMyEventListener {
            override fun onEventOccurred(app:EventTypes, counter: Int) {
                when(app){
                    EventTypes.OPEN_APP->touched(counter)
                    EventTypes.START_COUNTDOWN->startCountdown(counter)
                    EventTypes.STOP_COUNTDOWN->stopCountdown()
                    EventTypes.OPEN_SHORTCUT->openShortcut(counter)
                    EventTypes.TOGGLE_FAVORITES->toggleFavorites()
                    EventTypes.START_FLING->startFlingAnimation()
                    EventTypes.TOGGLE_SLIDERS->toggleSliders()
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
    fun startCountdown(type: Int){
        countdown = lifecycleScope.launch(Dispatchers.IO) {
            delay(250)
            withContext(Dispatchers.Main){
                if(type == 3) {
                    // Long press on empty area - toggle sliders
                    circleView.longPressTriggered = true
                    view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    toggleSliders()
                } else {
                    // Long press on app - show shortcuts
                    val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    if(launcherApps.hasShortcutHostPermission()) {
                        app_index = circleView.getAppIndexImIn()
                        if (app_index != null) {
                            shortcuts = mactivity.getShortcutFromPackage(
                                getApps()[app_index!!].packageName
                            )
                            val shortcuts_for_custom_view = shortcuts.map{it.shortLabel.toString()}.toMutableList().apply { add(if(getApps()[app_index!!].favorite) "Makni iz omiljenih" else "Dodaj u omiljene") }
                            circleView.showShortcuts(app_index!!, shortcuts_for_custom_view)
                            if(shortcuts.isEmpty()){
                                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                            Log.d("ingo", "precaci ${shortcuts.map { it.id + " " + it.`package` }}")
                        }
                    }
                }
            }
        }
    }

    fun openShortcut(index: Int){
        if(index >= shortcuts.size && app_index != null){
            val app = getApps()[app_index!!]
            app.favorite = !app.favorite
            (activity as MainActivity).saveAppInfo(app)
            return
        }
        val launcherApps: LauncherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcherApps.startShortcut(
                shortcuts[index].`package`,
                shortcuts[index].id,
                null,
                null,
                android.os.Process.myUserHandle()
            )
        } catch (e:IllegalStateException){
            e.printStackTrace()
        } catch (e:android.content.ActivityNotFoundException ){
            e.printStackTrace()
        }
    }

    fun toggleFavorites(){
        changeSettings("onlyfavorites", !onlyfavorites)
        onlyfavorites = !onlyfavorites
        circleView.moveDistancedAccumulated = 0
        circleView.onlyfavorites = onlyfavorites
        prebaciMeni()
    }

    fun toggleSliders(){
        sliders = !sliders
        if(sliders){
            global_view.findViewById<Slider>(R.id.detectSize).visibility = View.VISIBLE
            global_view.findViewById<Slider>(R.id.distance).visibility = View.VISIBLE
            global_view.findViewById<Slider>(R.id.step).visibility = View.VISIBLE
            global_view.findViewById<ImageButton>(R.id.close_sliders).visibility = View.VISIBLE
        } else {
            global_view.findViewById<Slider>(R.id.detectSize).visibility = View.GONE
            global_view.findViewById<Slider>(R.id.distance).visibility = View.GONE
            global_view.findViewById<Slider>(R.id.step).visibility = View.GONE
            global_view.findViewById<ImageButton>(R.id.close_sliders).visibility = View.GONE
        }
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
        Log.d("ingo", "touched " + app_index)
        val launchIntent: Intent? =
            viewModel.appsList.value?.filter { if(onlyfavorites) it.favorite else true }?.get(app_index)
                ?.let { requireContext().packageManager.getLaunchIntentForPackage(it.packageName) }
        if (launchIntent != null) {
            startActivity(launchIntent)
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

    fun getApps(): List<AppInfo>{
        return if(!onlyfavorites) {
            viewModel.appsList.value!!
        } else {
            viewModel.appsList.value!!.filter { it.favorite }
        }
    }

    fun prikaziPoljaKruga(onlyfavorites: Boolean){
        if(viewModel.appsList.value == null) return
        Log.d("ingo", viewModel.appsList.value.toString())
        circleView.setColorList(getApps().map{ it.color })
        circleView.setAppInfoList(getApps())
        circleView.onlyfavorites = onlyfavorites
        circleView.invalidate()
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

}

enum class EventTypes{OPEN_APP, START_COUNTDOWN, STOP_COUNTDOWN, OPEN_SHORTCUT, TOGGLE_FAVORITES, START_FLING, TOGGLE_SLIDERS}