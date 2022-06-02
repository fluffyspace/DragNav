package com.ingokodba.dragnav

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.example.dragnav.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MyCircleSettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity:SettingsActivity
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
        val RESTART = "restart"
        val DROP_DATABASE = "drop_database"
        val DEFAULT_APPS = "default_apps"
    }

    val data:Intent = Intent()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.circle_preferences, rootKey)
        settingsActivity = (activity as SettingsActivity)

        val colorPick: Preference? = findPreference(UI_COLOR)
        colorPick?.setOnPreferenceClickListener  {
            settingsActivity.startColorpicker()
            return@setOnPreferenceClickListener true
        }

        val numberPreference: EditTextPreference? = findPreference(UI_BORDER_WIDTH)
        numberPreference?.summary = "Currently " +
            context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString(UI_BORDER_WIDTH, "4") }
        numberPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        numberPreference?.setOnPreferenceChangeListener { preference, newValue ->
            numberPreference?.summary = "Currently $newValue"
            return@setOnPreferenceChangeListener true
        }
    }
}