package com.ingokodba.dragnav

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.preference.*
import com.example.dragnav.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MySettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity:SettingsActivity
    companion object{
        val UI_COLOR = "ui_color"
        val UI_COLOR_ON_PRIMARY = "ui_color_on_primary"
        val UI_SHADOW_TOGGLE = "ui_shadow_toggle"
        val UI_BORDER_WIDTH = "ui_border_width"
        val UI_TEXT_SIZE = "ui_text_size"
        val UI_SMALLER_TEXT_SIZE = "ui_smaller_text_size"
        val UI_TRANSPARENCY = "ui_transparency"
        val UI_CIRCLES_TOGGLE = "ui_circles_toggle"
        val UI_ICONS_TOGGLE = "ui_icons_toggle"
        val UI_SHOW_APP_NAMES = "ui_show_app_names"
        val UI_DESIGN = "ui_design"
        val UI_BIG_CIRCLE = "ui_big_circle"
        val UI_LANGUAGE_TOGGLE = "ui_language_toggle"
        val UI_ONELINE = "ui_oneline_buttons_toggle"
        val UI_BACKBUTTON = "ui_backbutton_toggle"
        val DARK_MODE = "dark_mode"
        val UI_RIGHT_HAND = "ui_right_hand"
        val IMPORT = "import"
        val EXPORT = "export"
        val FEEDBACK = "feedback"
        val RESTART = "restart"
        val FROM_BACKUP = "from_backup"
        val TO_BACKUP = "to_backup"
        val RENEW_INSTANCE = "renew_instance"
        val DROP_DATABASE = "drop_database"
        val DEFAULT_APPS = "default_apps"
    }

    val data:Intent = Intent()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        settingsActivity = (activity as SettingsActivity)
        Log.d("ingo", settingsActivity::class.simpleName.toString())

        val uiDesignValues = resources.getStringArray(R.array.ui_designs_values)
        val uiDesignValueIndex = uiDesignValues.indexOf(context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString(
            UI_DESIGN, uiDesignValues[0]) })
        when(uiDesignValueIndex){
            2, 3, 4 -> {
                val circlePreferences: Preference? = findPreference("circle_preferences")
                circlePreferences?.isVisible = true
                circlePreferences?.setOnPreferenceClickListener { preference ->
                    //settingsActivity.navController?.findDestination(R.id.action_mySettingsFragment_to_circleSettingsFragment)?.label = "trakošćan"
                    val action =
                        MySettingsFragmentDirections.actionMySettingsFragmentToCircleSettingsFragment()
                    settingsActivity.navController?.navigate(action)
                    true
                }
            }
            0, 1 -> {
                val circlePreferences: Preference? = findPreference("rainbow_preferences")
                circlePreferences?.isVisible = true
                circlePreferences?.setOnPreferenceClickListener { preference ->
                    //settingsActivity.navController?.findDestination(R.id.action_mySettingsFragment_to_circleSettingsFragment)?.label = "trakošćan"
                    val action =
                        MySettingsFragmentDirections.actionMySettingsFragmentToRainbowSettingsFragment()
                    settingsActivity.navController?.navigate(action)
                    true
                }
            }
        }

        val uiDesign: ListPreference? = findPreference(UI_DESIGN)
        val uiDesignValuesHumanReadable = resources.getStringArray(R.array.ui_designs_entries)
        uiDesign?.summary = uiDesignValuesHumanReadable[if (uiDesignValueIndex > 0) uiDesignValueIndex else 0]
        uiDesign?.setValueIndex(uiDesignValueIndex)
        uiDesign?.setOnPreferenceChangeListener { preference, newValue ->
            val newValueIndex = uiDesignValues.indexOfFirst{it == newValue}
            // Validate index to prevent IndexOutOfBoundsException
            val safeIndex = if (newValueIndex >= 0 && newValueIndex < uiDesignValuesHumanReadable.size) {
                newValueIndex
            } else {
                // Fallback to current index if new value not found
                uiDesignValueIndex.coerceIn(0, uiDesignValuesHumanReadable.size - 1)
            }
            uiDesign.summary = uiDesignValuesHumanReadable[safeIndex]
            showRestartDialog()
            return@setOnPreferenceChangeListener true
        }

        val general_preferences: Preference? = findPreference("general_preferences")
        general_preferences?.setOnPreferenceClickListener { preference ->
            //settingsActivity.navController?.findDestination(R.id.action_mySettingsFragment_to_circleSettingsFragment)?.label = "trakošćan"
            val action =
                MySettingsFragmentDirections.actionMySettingsFragmentToMyGeneralSettingsFragment()
            settingsActivity.navController?.navigate(action)
            true
        }
        val help: Preference? = findPreference("help")
        help?.setOnPreferenceClickListener { preference ->
            //settingsActivity.navController?.findDestination(R.id.action_mySettingsFragment_to_circleSettingsFragment)?.label = "trakošćan"
            val action =
                MySettingsFragmentDirections.actionMySettingsFragmentToMyHelpSettingsFragment()
            settingsActivity.navController?.navigate(action)
            true
        }
        val defaultApps: Preference? = findPreference(DEFAULT_APPS)
        defaultApps?.setOnPreferenceClickListener  {
            settingsActivity.openDefaultApps()
            return@setOnPreferenceClickListener true
        }
    }

    fun composeEmail(addresses: Array<String?>?, subject: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        if (intent.resolveActivity(settingsActivity.packageManager) != null) {
            startActivity(intent)
        }
    }

    fun showRestartDialog(){
        MaterialAlertDialogBuilder(requireContext(),
            androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dialog_Alert)
            .setMessage(MainActivity.resources2.getString(R.string.restart_required))
            .setNegativeButton(MainActivity.resources2.getString(R.string.cancel)) { dialog, which ->
                // Respond to negative button press
            }
            .setPositiveButton(MainActivity.resources2.getString(R.string.restart)) { dialog, which ->
                data.putExtra("restart", true);
                (activity as SettingsActivity).setResult(Activity.RESULT_OK, data);
                (activity as SettingsActivity).finish()
            }
            .show()
    }
}