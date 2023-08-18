package com.ingokodba.dragnav

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.example.dragnav.R
import com.ingokodba.dragnav.MySettingsFragment.Companion.DROP_DATABASE
import com.ingokodba.dragnav.MySettingsFragment.Companion.FEEDBACK
import com.ingokodba.dragnav.MySettingsFragment.Companion.FROM_BACKUP
import com.ingokodba.dragnav.MySettingsFragment.Companion.RENEW_INSTANCE
import com.ingokodba.dragnav.MySettingsFragment.Companion.RESTART
import com.ingokodba.dragnav.MySettingsFragment.Companion.TO_BACKUP
import com.ingokodba.dragnav.baza.AppDatabase
import kotlinx.coroutines.launch


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

        val to_backup: Preference? = findPreference(TO_BACKUP)
        to_backup?.setOnPreferenceClickListener  {
            (activity as SettingsActivity).to_backup()
            Toast.makeText(requireContext(), "Backup successful", Toast.LENGTH_SHORT).show()
            return@setOnPreferenceClickListener true
        }

        val from_backup: Preference? = findPreference(FROM_BACKUP)
        from_backup?.setOnPreferenceClickListener  {
            Toast.makeText(requireContext(), "Backup restored", Toast.LENGTH_SHORT).show()
            (activity as SettingsActivity).from_backup()
            return@setOnPreferenceClickListener true
        }

        /*val renew_instance: Preference? = findPreference(RENEW_INSTANCE)
        renew_instance?.setOnPreferenceClickListener  {
            val db = AppDatabase.getInstance(requireContext())
            db.(requireContext())
            Toast.makeText(requireContext(), "From backup", Toast.LENGTH_SHORT).show()
            return@setOnPreferenceClickListener true
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