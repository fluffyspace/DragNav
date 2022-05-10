package com.ingokodba.dragnav

import android.os.Bundle
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.Action
import com.ingokodba.dragnav.modeli.OnActionClick

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [ActionsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ActionsFragment : Fragment(), OnActionClick {
    lateinit var recycler_view: RecyclerView
    var radapter: RAdapter? = null
    lateinit var mactivity:MainActivity

    var actions: List<Action> = listOf()
        get() = field
        set(value) {
            field = value
            radapter?.notifyDataSetChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mactivity = (activity as MainActivity)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view = view.findViewById(R.id.actions_recycler_view)
        radapter = RAdapter(requireContext(), this)
        recycler_view.adapter = radapter
        radapter?.actionsList = actions
    }

    override fun onActionClick(action: Action) {
        mactivity.runAction(action)
    }
}