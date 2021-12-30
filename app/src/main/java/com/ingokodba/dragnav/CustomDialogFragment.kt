package com.ingokodba.dragnav

import android.Manifest
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.MeniJednoPolje
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomDialogFragment(polje: MeniJednoPolje) : DialogFragment() {
    var polje: MeniJednoPolje
    init {
        this.polje=polje
    }
    lateinit var toolbar: Toolbar

    var imgPath: TextView? = null
    private val PICK_IMAGE_REQUEST = 9544
    var image: ImageView? = null
    var selectedImage: Uri? = null
    var part_image: String? = null

    // Permissions for accessing the storage
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /** The system calls this to get the DialogFragment's layout, regardless
    of whether it's being displayed as a dialog or an embedded fragment. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment

        val view: View = inflater.inflate(R.layout.editall_dialog, container, false)
        view.findViewById<TextView>(R.id.submit).setOnClickListener{
            //pick(it)
        }


        toolbar = view.findViewById(R.id.toolbar)

        return view
    }

    override fun onStart() {
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.getWindow()?.setLayout(width, height)
        }
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setNavigationOnClickListener { v -> dismiss() }
        toolbar.setTitle(R.string.edit_link)
        //toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener { item ->
            dismiss()
            true
        }
        var recipeName = (view.findViewById<TextInputLayout>(R.id.labela) as EditText)
        var recipeDesc = (view.findViewById<TextInputLayout>(R.id.intent) as EditText)
        var recipeId = (view.findViewById<TextView>(R.id.id_linka) as TextView)

        view.findViewById<Button>(R.id.submit).setOnClickListener{ view ->
            if(recipeName.toString() == "") {
                Toast.makeText(requireContext(), "There has to be at least name.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch(Dispatchers.IO) {
                polje.text = recipeName.text.toString()
                polje.nextIntent = recipeDesc.text.toString()
                (activity as MainActivity).databaseUpdateItem(polje)
                withContext(Dispatchers.Main){
                    (activity as MainActivity).mainFragment.refreshCurrentMenu()
                }
            }
            dismiss()
        }
        recipeName.setText(polje.text.toString())
        recipeDesc.setText(polje.nextIntent.toString())
        recipeId.setText("id = " + polje.id.toString())
        super.onViewCreated(view, savedInstanceState)
        //polje.nextIntent = recipeDesc.editText?.text.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    /** The system calls this only when creating the layout in a dialog. */
    /*override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }*/

}