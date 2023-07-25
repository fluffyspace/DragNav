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
 * Use the [MainFragmentTipke.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragmentTipke() : Fragment(), MainFragmentInterface {

    private val viewModel: ViewModel by activityViewModels()
    lateinit var mactivity:MainActivity
    lateinit var selected_text: TextView
    lateinit var global_view:View
    lateinit var addingMenuCancelOk:LinearLayout
    lateinit var circleView:PhoneKeypadView
    lateinit var cancelAddingApp:Button
    lateinit var addApp: Button

    override var fragment: Fragment = this

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    var birano: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mactivity = (activity as MainActivity)
        //MainActivity.changeLocale(requireContext())
    }


    interface IMyEventListener {
        fun onEventOccurred(number: Int)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        circleView = view.findViewById(R.id.phone_keypad_view)
        circleView.setEventListener(object :
            IMyEventListener {
            override fun onEventOccurred(number: Int) {
                Log.d("ingo", "onEventOccurred")
                if(number == 10){
                    birano = ""
                    selected_text.text = "---"
                } else biraj(number)
            }
        })

        selected_text = view.findViewById(R.id.selected_text)
        selected_text.text = "---"
        global_view = view

        /*view.findViewById<Button>(R.id.buttonr).setOnClickListener {
            birano = ""
            selected_text.text = "---"
        }*/
    }

    fun biraj(broj: Int){
        birano += broj
        if(birano.length == 1){
            selected_text.text = "${broj}"
        }
        if(birano.length == 2){
            selected_text.text = "${birano}"
        }
        if(birano.length == 3){
            openAppById(birano.toInt())
            birano = ""
            selected_text.text = "---"
        }
    }

    fun openAppById(id: Int){
        if(id > viewModel.appsList.value!!.size) return
        val app = viewModel.appsList.value!![id]
        val launchIntent: Intent? = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
        if(launchIntent != null) startActivity(launchIntent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tipke, container, false)
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
            MainFragmentTipke().apply {

            }
    }

    override fun iconsUpdated(){
        //circleView.icons = viewModel.icons.value!!
    }

    override fun selectedItemDeleted(){

    }

    fun settings(){
        //startActivity(Intent(android.provider.Settings.ACTION_SETTINGS), null);
        mactivity.showLayout(MainActivity.Companion.Layouts.LAYOUT_SETTINGS)

    }

    override fun refreshCurrentMenu(){

    }

    override fun toggleEditMode(){

    }
    override fun goToHome(){

    }

}