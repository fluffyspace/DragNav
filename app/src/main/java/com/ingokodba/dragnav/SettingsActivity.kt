package com.ingokodba.dragnav

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
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

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if(data != null) {
                if(data.getBooleanExtra("forPrimaryColor", false)){
                    val preferences: SharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(this)
                    val editor = preferences.edit()
                    editor.putString("ui_color", data.getIntExtra("color", 0).toString())
                    editor.apply()
                }
            }
        }
    }

    fun startColorpicker(){
        val intent = Intent(this@SettingsActivity, ColorPickerActivity::class.java)
        resultLauncher.launch(intent)
    }

    fun openDefaultApps(){
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}