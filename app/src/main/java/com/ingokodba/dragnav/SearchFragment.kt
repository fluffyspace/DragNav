package com.ingokodba.dragnav

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.dragnav.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MessageEvent
import com.ingokodba.dragnav.modeli.MessageEventType
import org.greenrobot.eventbus.EventBus

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment() {

    private val viewModel: ViewModel by activityViewModels()
    lateinit var search_bar: EditText
    lateinit var imm: InputMethodManager
    lateinit var chipGroup: ChipGroup
    lateinit var mactivity:MainActivity
    lateinit var shortcutPopup: PopupWindow

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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chipGroup = view.findViewById<ChipGroup>(R.id.chipgroup_aplikacije)
        imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        search_bar = view.findViewById<EditText>(R.id.search_bar)
        search_bar.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + search_bar.text.toString()))
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                    //intent.setPackage("com.android.chrome");
                    startActivity(intent)
                    mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_MAIN)
                    search_bar.setText("")
                    return@setOnEditorActionListener true
                }
                false
            }
            addTextChangedListener {
                Log.d("ingo", "text changed to " + search_bar.text.toString())
                // find apps
                val search_lista_aplikacija: MutableList<Pair<Int, AppInfo>>
                if(search_bar.text.toString().length == 0) {
                    //val search_lista_aplikacija: MutableList<AppInfo> = mutableListOf()
                    val sortirano = viewModel.appsList.value!!.filter{ it.frequency > 0 }.sortedByDescending { it.lastLaunched }
                    val max = if (sortirano.size > 5) 5 else sortirano.size
                    search_lista_aplikacija = (0 until max)
                        .map { Pair(it, sortirano[it]) }.toMutableList()
                } else {
                    search_lista_aplikacija =
                        getAppsByQuery(viewModel.appsList.value!!, search_bar.text.toString())
                }

                /*chipGroup.setOnClickListener{
                    val chip = it as Chip
                    val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(lista_aplikacija[chip.id].second.packageName.toString())
                    if(launchIntent != null) {
                        onMessageEvent(MessageEvent(lista_aplikacija[chip.id].second.label, 0, lista_aplikacija[chip.id].second.packageName, lista_aplikacija[chip.id].second.color))
                    }
                    //startActivity(launchIntent)
                }*/
                chipGroup.removeAllViews()
                //var chips: MutableList<Chip> = mutableListOf()
                for ((index,app) in search_lista_aplikacija.iterator().withIndex()) {
                    if(index > 10) break
                    val chip = layoutInflater.inflate(R.layout.chip_template, null) as Chip
                    val chip_text = app.second.label.toString()
                    chip.setText(chip_text)// + " " + app.first)
                    chip.id = index
                    try {
                        chip.chipBackgroundColor = ColorStateList.valueOf(app.second.color.toInt())
                    } catch (e:NumberFormatException){
                        e.printStackTrace()
                        Log.d("ingo", "neuspjela boja")
                    }
                    chip.setOnClickListener {
                        Log.d(
                            "ingo", chip.text.toString() + " " + chip.id.toString()
                        )
                        // otvori ovu aplikaciju
                        val launchIntent: Intent? = requireContext().packageManager.getLaunchIntentForPackage(app.second.packageName.toString())
                        if(launchIntent != null) {
                            mactivity.onMessageEvent(MessageEvent(search_lista_aplikacija[chip.id].second.label, 0, search_lista_aplikacija[chip.id].second.packageName, search_lista_aplikacija[chip.id].second.color, app=search_lista_aplikacija[chip.id].second, type=MessageEventType.LAUNCH_APP))
                        }
                        imm.hideSoftInputFromWindow(windowToken, 0)
                        //startActivity(launchIntent)
                    }
                    chip.setOnLongClickListener{
                        val contentView = createMenu(app.second)
                        shortcutPopup = PopupWindow(contentView,
                            ListPopupWindow.WRAP_CONTENT,
                            ListPopupWindow.WRAP_CONTENT, true)
                        //shortcutPopup?.animationStyle = R.style.PopupAnimation
                        val location = locateView(chip)
                        shortcutPopup?.showAtLocation(view, Gravity.TOP or Gravity.LEFT, location!!.left, location!!.bottom)
                        return@setOnLongClickListener true
                    }
                    //chips.add(chip)
                    chipGroup.addView(chip)
                }

            }
        }
        initializeSearch()
    }

    fun locateView(v: View?): Rect? {
        val loc_int = IntArray(2)
        if (v == null) return null
        try {
            v.getLocationOnScreen(loc_int)
        } catch (npe: NullPointerException) {
            //Happens when the view doesn't exist on screen anymore.
            return null
        }
        val location = Rect()
        location.left = loc_int[0]
        location.top = loc_int[1]
        location.right = location.left + v.width
        location.bottom = location.top + v.height
        return location
    }

    fun createMenu(app:AppInfo):View{
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.popup_app_info_or_add, null)
        view.findViewById<LinearLayout>(R.id.open_appinfo).setOnClickListener{
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", app.packageName, null)
            intent.data = uri
            it.context.startActivity(intent)
            shortcutPopup.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.start_adding).setOnClickListener{
            //Toast.makeText(context, getString(R.string.drag_and_drop_app), Toast.LENGTH_SHORT).show()
            EventBus.getDefault().post(MessageEvent(app.label, 0, app.packageName, app.color, type = MessageEventType.DRAG_N_DROP, app = app))
            shortcutPopup.dismiss()
        }
        return view
    }

    fun dragAndDropApp(pos:Int, context: Context){

    }

    fun initializeSearch(){
        if(::search_bar.isInitialized) {
            search_bar.setText("")
            search_bar.requestFocus()
        }
        imm.showSoftInput(search_bar, InputMethodManager.SHOW_IMPLICIT)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SearchFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        fun getAppsByQuery(apps: List<AppInfo>, query:String):MutableList<Pair<Int, AppInfo>>{
            var search_lista_aplikacija: MutableList<Pair<Int, AppInfo>> = mutableListOf()
            //var slova_search = query.map{ it }
            val slova_search_lowercase = query.map{ it.lowercaseChar() }
            for(app in apps) {
                var counter = 0
                var index_counter = 0
                // provjerava ako je svako koje je u query prisutno u labeli aplikacije. ako nije, preskače se aplikacija
                for(slovo in app.label.lowercase()){
                    if(slovo == query[index_counter].lowercaseChar()){
                        index_counter++
                        if(index_counter == query.length) break
                    }
                }
                if(index_counter != query.length){
                    continue
                }
                if(app.label.first().lowercaseChar() == slova_search_lowercase[0]){
                    counter += 5
                    if(query.length == 2) {
                        // prvo i zadnje slovo
                        if (app.label.last().lowercaseChar() == slova_search_lowercase[1]) {
                            counter += 10
                            Log.d("ingo", app.label + " ako počinje s prvim slovom i završava s drugim " + query)
                        }
                        // prva slova riječi odvojene razmakom
                        val splitano = app.label.split(" ")
                        if (splitano.size > 1 && splitano[1].first().lowercaseChar() == slova_search_lowercase[1]) {
                            counter += 5
                            Log.d("ingo", app.label + " ako je prvo slovo prva riječ, drugo slovo druga riječ " + query)
                        }
                    }
                    if(app.label.lowercase().startsWith(query.lowercase())){
                        counter += 5*query.length
                        Log.d("ingo", app.label + " startsWith " + query)
                    }
                }
                // velika slova
                var counter2 = 0
                for ((index, slovo) in app.label.iterator().withIndex()) {
                    //if (index == 0) continue
                    counter2 = 0
                    if (slovo.isUpperCase() && slova_search_lowercase.contains(slovo.lowercaseChar())) {
                        counter2++
                        Log.d("ingo", app.label + " upper case contains " + query)
                    }
                    if(counter2 == query.length) counter += 10
                }
                if(query.length > 1) {
                    val matches = countMatches(app.label.lowercase(), query.lowercase())
                    if (matches == query.length) {
                        counter += matches * 7
                        Log.d(
                            "ingo",
                            "matches " + app.label.toString() + " " + query + " " + matches
                        )
                    }
                }
                if(counter > 0) search_lista_aplikacija.add(Pair(counter, app))
            }
            search_lista_aplikacija.sortByDescending { it.first }
            return search_lista_aplikacija
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
    }




}