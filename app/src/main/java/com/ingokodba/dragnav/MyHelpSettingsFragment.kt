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
import com.ingokodba.dragnav.MySettingsFragment.Companion.DEFAULT_APPS
import com.ingokodba.dragnav.MySettingsFragment.Companion.DROP_DATABASE
import com.ingokodba.dragnav.MySettingsFragment.Companion.FEEDBACK
import com.ingokodba.dragnav.MySettingsFragment.Companion.RESTART


class MyHelpSettingsFragment : PreferenceFragmentCompat() {
    lateinit var settingsActivity:SettingsActivity

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
        setPreferencesFromResource(R.xml.help_preferences, rootKey)
        settingsActivity = (activity as SettingsActivity)

        val drop_database: Preference? = findPreference(DROP_DATABASE)
        drop_database?.setOnPreferenceClickListener  {
            data.putExtra("dropDatabase", true);
            (activity as SettingsActivity).setResult(Activity.RESULT_OK, data);
            (activity as SettingsActivity).finish()
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