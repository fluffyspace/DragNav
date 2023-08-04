package com.ingokodba.dragnav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.navigation.Navigation.findNavController
import androidx.preference.*
import com.example.dragnav.R
import kotlin.reflect.typeOf

class MySettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity:SettingsActivity
    companion object{
        val UI_COLOR = "ui_color"
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
        val UI_RIGHT_HAND = "ui_right_hand"
        val IMPORT = "import"
        val EXPORT = "export"
        val FEEDBACK = "feedback"
        val RESTART = "restart"
        val DROP_DATABASE = "drop_database"
        val DEFAULT_APPS = "default_apps"
    }

    val data:Intent = Intent()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        settingsActivity = (activity as SettingsActivity)
        Log.d("ingo", settingsActivity::class.simpleName.toString())

        val circle_preferences: Preference? = findPreference("circle_preferences")
        circle_preferences?.setOnPreferenceClickListener { preference ->
            //settingsActivity.navController?.findDestination(R.id.action_mySettingsFragment_to_circleSettingsFragment)?.label = "trakošćan"
            val action =
                MySettingsFragmentDirections.actionMySettingsFragmentToCircleSettingsFragment()
            settingsActivity.navController?.navigate(action)
            true
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
}