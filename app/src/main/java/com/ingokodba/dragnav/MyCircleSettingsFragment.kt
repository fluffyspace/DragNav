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
import com.ingokodba.dragnav.MySettingsFragment.Companion.UI_BORDER_WIDTH
import com.ingokodba.dragnav.MySettingsFragment.Companion.UI_COLOR


class MyCircleSettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity:SettingsActivity

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