package com.ingokodba.dragnav

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
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
 * Use the [ActivitiesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ActivitiesFragment : Fragment() {

    private val viewModel: ViewModel by activityViewModels()
    lateinit var search_bar: EditText
    lateinit var recycler_view: RecyclerView
    lateinit var popis_svih_aplikacija: FrameLayout
    lateinit var trazilica: LinearLayout
    lateinit var chipGroup: ChipGroup
    lateinit var imm: InputMethodManager
    lateinit var shortcutPopup: PopupWindow
    lateinit var recycle_scroller: RecycleScroller
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    var radapter: ApplicationsListAdapter? = null
    var rowsVisibleCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activities, container, false)
    }

    fun scrollRecyclerViewToPrecentage(precentage: Int){
        Log.d("ingo", "precentage is " + precentage)
        val pos = (((viewModel.appsList.value!!.size/100f)*precentage)).toInt()
        Log.d("ingo", "scrolling to " + pos)
        recycler_view.scrollToPosition(pos)
        //val maxY = recycler_view.getChildAt(recycler_view.childCount-1).y
        //Log.d("ingo", recycler_view.scrollY.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageButton>(R.id.settings).setOnClickListener {
            (activity as MainActivity).showLayout(MainActivity.Companion.Layouts.LAYOUT_SETTINGS)
        }
        recycler_view = view.findViewById(R.id.recycler_view)
        radapter = ApplicationsListAdapter(viewModel)
        search_bar = view.findViewById(R.id.search_bar)
        popis_svih_aplikacija = view.findViewById(R.id.popis_svih_aplikacija)
        trazilica = view.findViewById(R.id.trazilica)
        chipGroup = view.findViewById<ChipGroup>(R.id.chipgroup_aplikacije)
        imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        recycle_scroller = view.findViewById<RecycleScroller>(R.id.recycle_scroller)
        recycle_scroller.setCallback(::scrollRecyclerViewToPrecentage)
        recycler_view.adapter = radapter
        recycler_view.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                recycle_scroller.drawThumb = true

                val offset = recycler_view.computeVerticalScrollOffset()
                val extent = recycler_view.computeVerticalScrollExtent()
                val range = recycler_view.computeVerticalScrollRange()

                val percentage = 100.0f * offset / (range - extent).toFloat()
                recycle_scroller.precentageToScroll(percentage.toInt())
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if(!recycle_scroller.touchDown) recycle_scroller.drawThumb = false
                }
            }
        })
        //viewModel.setFilteredApps(viewModel.appsList.value!!)
        if(viewModel.appsList.value != null){
            radapter?.submitList(viewModel.appsList.value!!)
            Log.d("ingo", "activities fragment onViewCreated")
            Log.d("ingo", "" + viewModel.appsList.value!!.map{ it.label })
        }

        val decorView  = activity?.window?.decorView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            val showingKeyboard = insets.isVisible(WindowInsetsCompat.Type.ime())
            if(showingKeyboard){
                Log.d("ingo8", "keyboard shown")
                popis_svih_aplikacija.visibility = View.GONE
                trazilica.visibility = View.VISIBLE
            } else {
                Log.d("ingo8", "keyboard hidden")
                popis_svih_aplikacija.visibility = View.VISIBLE
                trazilica.visibility = View.GONE
            }
            insets
        }

        search_bar.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + search_bar.text.toString()))
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                    //intent.setPackage("com.android.chrome");
                    startActivity(intent)
                    (activity as MainActivity).showLayout(MainActivity.Companion.Layouts.LAYOUT_MAIN)
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
                        SearchFragment.getAppsByQuery(
                            viewModel.appsList.value!!,
                            search_bar.text.toString()
                        )
                }
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
                            (activity as MainActivity).onMessageEvent(MessageEvent(search_lista_aplikacija[chip.id].second.label, 0, search_lista_aplikacija[chip.id].second.packageName, search_lista_aplikacija[chip.id].second.color, app=search_lista_aplikacija[chip.id].second, type= MessageEventType.LAUNCH_APP))
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
        val touchHelperCallback: ItemTouchHelper.SimpleCallback =
            object : ItemTouchHelper.SimpleCallback(0,(ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)) {
                private val background = ColorDrawable(resources.getColor(R.color.colorPrimary))
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    radapter?.showMenu(viewHolder.adapterPosition)
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                    val itemView = viewHolder.itemView
                    if (dX > 0) {
                        background.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom
                        )
                    } else if (dX < 0) {
                        background.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )
                    } else {
                        background.setBounds(0, 0, 0, 0)
                    }
                    background.draw(c)
                }
            }
        val itemTouchHelper = ItemTouchHelper(touchHelperCallback)
        itemTouchHelper.attachToRecyclerView(view.findViewById<RecyclerView>(R.id.recycler_view))
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

    fun setAllAsVisible(){
        for(app in viewModel.appsList.value!!){
            app.visible = true
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ActivitiesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ActivitiesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}