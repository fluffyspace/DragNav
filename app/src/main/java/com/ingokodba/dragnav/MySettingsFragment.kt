package com.ingokodba.dragnav

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.InputType
import android.widget.Toast
import androidx.preference.*
import com.example.dragnav.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MySettingsFragment : PreferenceFragmentCompat() {
    lateinit var mactivity:MainActivity
    companion object{
        val UI_COLOR = "ui_color"
        val UI_SHADOW_TOGGLE = "ui_shadow_toggle"
        val UI_BORDER_WIDTH = "ui_border_width"
        val UI_CIRCLES_TOGGLE = "ui_circles_toggle"
        val UI_ICONS_TOGGLE = "ui_icons_toggle"
        val UI_LANGUAGE_TOGGLE = "ui_language_toggle"
        val UI_ONELINE = "ui_oneline_buttons_toggle"
        val UI_BACKBUTTON = "ui_backbutton_toggle"
        val IMPORT = "import"
        val EXPORT = "export"
        val FEEDBACK = "feedback"
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        view?.setBackgroundColor(Color.BLACK)
        mactivity = (activity as MainActivity)

        val colorPick: Preference? = findPreference(UI_COLOR)
        colorPick?.setOnPreferenceClickListener  {
            mactivity.startColorpicker()
            return@setOnPreferenceClickListener true
        }

        val import: Preference? = findPreference(IMPORT)
        import?.setOnPreferenceClickListener  {
            Toast.makeText(requireContext(), "TO DO", Toast.LENGTH_SHORT).show()
            mactivity.import_export_action = MainActivity.ACTION_IMPORT
            mactivity.openFile()
            return@setOnPreferenceClickListener true
        }

        val export: Preference? = findPreference(EXPORT)
        export?.setOnPreferenceClickListener  {
            Toast.makeText(requireContext(), "TO DO", Toast.LENGTH_SHORT).show()
            mactivity.import_export_action = MainActivity.ACTION_EXPORT
            mactivity.createFile()
            return@setOnPreferenceClickListener true
        }

        val feedback: Preference? = findPreference(FEEDBACK)
        feedback?.setOnPreferenceClickListener  {
            composeEmail(arrayOf("ingokodba@gamil.com"), "Feedback")
            return@setOnPreferenceClickListener true
        }



        val languageSwitch: SwitchPreference? = findPreference(UI_LANGUAGE_TOGGLE)
        languageSwitch?.setOnPreferenceChangeListener { preference, newValue ->
            MaterialAlertDialogBuilder(requireContext(),
                androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dialog_Alert)
                .setMessage(mactivity.resources2.getString(R.string.restart_required))
                .setNegativeButton(mactivity.resources2.getString(R.string.cancel)) { dialog, which ->
                    // Respond to negative button press
                }
                .setPositiveButton(mactivity.resources2.getString(R.string.restart)) { dialog, which ->
                    // Respond to negative button press
                    mactivity.showLayout(MainActivity.LAYOUT_MAIN)
                    //mactivity.recreate()
                    startActivity(Intent.makeRestartActivityTask(activity?.intent?.component));


                }
                .show()
            return@setOnPreferenceChangeListener true
        }

        val numberPreference: EditTextPreference? = findPreference(UI_BORDER_WIDTH)
        numberPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        val onelineSwitch: SwitchPreference? = findPreference(UI_ONELINE)
        onelineSwitch?.setOnPreferenceChangeListener { preference, newValue ->
            mactivity.mainFragment.bottomMenuView.requestLayout()
            mactivity.mainFragment.bottomMenuView.invalidate()
            return@setOnPreferenceChangeListener true
        }

        val backButtonSwitch: SwitchPreference? = findPreference(UI_BACKBUTTON)
        backButtonSwitch?.setOnPreferenceChangeListener { preference, newValue ->
            mactivity.loadOnBackButtonPreference()
            return@setOnPreferenceChangeListener true
        }

        /*numberPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }*/
    }

    fun composeEmail(addresses: Array<String?>?, subject: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        if (intent.resolveActivity(mactivity.packageManager) != null) {
            startActivity(intent)
        }
    }



}