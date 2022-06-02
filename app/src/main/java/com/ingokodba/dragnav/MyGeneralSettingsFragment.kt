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


class MyGeneralSettingsFragment : PreferenceFragmentCompat() {
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



    /*override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = pref.fragment?.let {
            parentFragmentManager.fragmentFactory.instantiate(
                ClassLoader.getSystemClassLoader(),
                it
            )
        }
        if (fragment != null) {
            fragment.arguments = args
            fragment.setTargetFragment(caller, 0)
            // Replace the existing Fragment with the new Fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit()
            return true
        }
        return false
    }*/

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.general_preferences, rootKey)
        settingsActivity = (activity as SettingsActivity)

        val darkModeString = getString(R.string.dark_mode)
        val darkMode: ListPreference? = findPreference(darkModeString)
        val darkModeValues = resources.getStringArray(R.array.dark_mode_values)
        val darkModeValuesHumanReadable = resources.getStringArray(R.array.dark_mode_entries)
        val darkModeValueIndex = darkModeValues.indexOf(context?.let { PreferenceManager.getDefaultSharedPreferences(it).getString(darkModeString, darkModeValues[3]) })
        darkMode?.summary = darkModeValuesHumanReadable[if (darkModeValueIndex > 0) darkModeValueIndex else 0]
        darkMode?.setOnPreferenceChangeListener { preference, newValue ->
            when (newValue) {
                darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                darkModeValues[3] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                else -> {}
            }
            darkMode?.summary = newValue as CharSequence
            return@setOnPreferenceChangeListener true
        }

        val defaultApps: Preference? = findPreference(DEFAULT_APPS)
        defaultApps?.setOnPreferenceClickListener  {
            settingsActivity.openDefaultApps()
            return@setOnPreferenceClickListener true
        }

        val drop_database: Preference? = findPreference(DROP_DATABASE)
        drop_database?.setOnPreferenceClickListener  {
            data.putExtra("dropDatabase", true);
            (activity as SettingsActivity).setResult(Activity.RESULT_OK, data);
            (activity as SettingsActivity).finish()
            return@setOnPreferenceClickListener true
        }

        val import: Preference? = findPreference(IMPORT)
        import?.setOnPreferenceClickListener  {
            Toast.makeText(requireContext(), "TO DO", Toast.LENGTH_SHORT).show()
            //mactivity.import_export_action = MainActivity.ACTION_IMPORT
            return@setOnPreferenceClickListener true
        }

        val export: Preference? = findPreference(EXPORT)
        export?.setOnPreferenceClickListener  {
            Toast.makeText(requireContext(), "TO DO", Toast.LENGTH_SHORT).show()
            //mactivity.import_export_action = MainActivity.ACTION_EXPORT
            return@setOnPreferenceClickListener true
        }

        val feedback: Preference? = findPreference(FEEDBACK)
        feedback?.setOnPreferenceClickListener  {
            composeEmail(arrayOf("ingokodba@gamil.com"), "Feedback")
            return@setOnPreferenceClickListener true
        }

        val restart: Preference? = findPreference(RESTART)
        restart?.setOnPreferenceClickListener  {
            data.putExtra("restart", true);
            (activity as SettingsActivity).setResult(Activity.RESULT_OK, data);
            (activity as SettingsActivity).finish()
            return@setOnPreferenceClickListener true
        }

        val languageSwitch: SwitchPreference? = findPreference(UI_LANGUAGE_TOGGLE)
        languageSwitch?.setOnPreferenceChangeListener { preference, newValue ->
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
            return@setOnPreferenceChangeListener true
        }

        val onelineSwitch: SwitchPreference? = findPreference(UI_ONELINE)
        onelineSwitch?.setOnPreferenceChangeListener { preference, newValue ->
            data.putExtra("refresh", true);
            (activity as SettingsActivity).setResult(Activity.RESULT_OK, data);
            return@setOnPreferenceChangeListener true
        }

        val backButtonSwitch: SwitchPreference? = findPreference(UI_BACKBUTTON)
        backButtonSwitch?.setOnPreferenceChangeListener { preference, newValue ->
            data.putExtra("backButtonAction", newValue.toString().toBoolean());
            (activity as SettingsActivity).setResult(Activity.RESULT_OK, data);
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
        if (intent.resolveActivity(settingsActivity.packageManager) != null) {
            startActivity(intent)
        }
    }

}