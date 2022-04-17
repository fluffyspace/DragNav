package com.ingokodba.dragnav

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.dragnav.R

class SettingsActivity : AppCompatActivity(R.layout.activity_settings){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_view, MySettingsFragment())
            .commit()
    }

    fun startColorpicker(){
        val intent = Intent(this, ColorPickerActivity::class.java)
        startActivity(intent)
    }
}