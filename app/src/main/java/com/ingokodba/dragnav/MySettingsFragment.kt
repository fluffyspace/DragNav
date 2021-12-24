package com.ingokodba.dragnav

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.dragnav.R

class MySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}