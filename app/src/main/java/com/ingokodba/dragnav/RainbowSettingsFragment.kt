package com.ingokodba.dragnav

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.preference.*
import com.example.dragnav.R
import com.ingokodba.dragnav.MySettingsFragment.Companion.UI_BORDER_WIDTH
import com.ingokodba.dragnav.MySettingsFragment.Companion.UI_COLOR


class RainbowSettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity:SettingsActivity

    val data:Intent = Intent()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.rainbow_preferences, rootKey)
        settingsActivity = (activity as SettingsActivity)

        val colorPick: Preference? = findPreference(UI_COLOR)
        colorPick?.setOnPreferenceClickListener  {
            settingsActivity.startColorpicker()
            return@setOnPreferenceClickListener true
        }

        val numberPreference: EditTextPreference? = findPreference(UI_BORDER_WIDTH)
        numberPreference?.summary = "" +
            context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString(UI_BORDER_WIDTH, "4") }
        numberPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        numberPreference?.setOnPreferenceChangeListener { preference, newValue ->
            numberPreference?.summary = "$newValue"
            return@setOnPreferenceChangeListener true
        }

        val numberPreference2: EditTextPreference? = findPreference(MySettingsFragment.UI_TEXT_SIZE)
        numberPreference2?.summary = "" +
                context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString(
                    MySettingsFragment.UI_TEXT_SIZE, "30") }
        numberPreference2?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        numberPreference2?.setOnPreferenceChangeListener { preference, newValue ->
            numberPreference2?.summary = "$newValue"
            return@setOnPreferenceChangeListener true
        }

        val numberPreference4: EditTextPreference? = findPreference(MySettingsFragment.UI_SMALLER_TEXT_SIZE)
        numberPreference4?.summary = "" +
                context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString(
                    MySettingsFragment.UI_SMALLER_TEXT_SIZE, "30") }
        numberPreference4?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        numberPreference4?.setOnPreferenceChangeListener { preference, newValue ->
            numberPreference4?.summary = "$newValue"
            return@setOnPreferenceChangeListener true
        }

        val numberPreference3: EditTextPreference? = findPreference(MySettingsFragment.UI_TRANSPARENCY)
        numberPreference3?.summary = "" +
                context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString(
                    MySettingsFragment.UI_TRANSPARENCY, "1.0") }
        numberPreference3?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        numberPreference3?.setOnPreferenceChangeListener { preference, newValue ->
            numberPreference3?.summary = "$newValue"
            return@setOnPreferenceChangeListener true
        }
    }
}