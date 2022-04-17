package com.ingokodba.dragnav

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R

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

    private val viewModel: NewRAdapterViewModel by activityViewModels()
    lateinit var search_bar: EditText
    lateinit var recycler_view: RecyclerView
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var radapter: NewRAdapter

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val smoothScroller: RecyclerView.SmoothScroller =
            object : LinearSmoothScroller(context) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }
        radapter = NewRAdapter(viewModel)
        search_bar = view.findViewById(R.id.search_bar)
        recycler_view = view.findViewById(R.id.recycler_view)
        recycler_view.adapter = radapter
        radapter.submitList(viewModel.appsList.value!!)
        Log.d("ingo", "activities fragment onViewCreated")
        Log.d("ingo", "" + viewModel.appsList.value!!.map{ it.label })
        search_bar.apply {
            addTextChangedListener {
                Log.d("ingo", "text changed to " + search_bar.text.toString())
                // find apps
                if (search_bar.text.toString().length == 0) {
                    return@addTextChangedListener
                } else {
                    val search_lista_aplikacija =
                        SearchFragment.getAppsByQuery(viewModel.appsList.value!!, search_bar.text.toString())
                    if (search_lista_aplikacija.size > 0) {
                        for (i in 0..viewModel.appsList.value!!.size) {
                            if (viewModel.appsList.value!![i] == search_lista_aplikacija[0].second) {
                                //recycler_view.scrollToPosition(i)
                                smoothScroller.targetPosition = i
                                (recycler_view.layoutManager)?.startSmoothScroll(smoothScroller)
                                Log.d(
                                    "ingo",
                                    "scrolling for " + viewModel.appsList.value!![i].label
                                )
                                break
                            }
                        }
                    }
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
                    radapter.showMenu(viewHolder.adapterPosition)
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