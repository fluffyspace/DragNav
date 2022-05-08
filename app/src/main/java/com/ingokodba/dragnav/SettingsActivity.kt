package com.ingokodba.dragnav

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.dragnav.R

class SettingsActivity : AppCompatActivity(R.layout.activity_settings){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true);
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_view, MySettingsFragment())
            .commit()
    }

    fun startColorpicker(){
        val intent = Intent(this, ColorPickerActivity::class.java)
        startActivity(intent)
    }

    fun openDefaultApps(){
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}