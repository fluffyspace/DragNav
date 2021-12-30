package com.ingokodba.dragnav

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.madrapps.pikolo.ColorPicker
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener

class ColorPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.colorpickerlayout)
        displaySetColor()

        var gcolor:Int = 0
        findViewById<ColorPicker>(R.id.colorpicker).setColorSelectionListener(object : SimpleColorSelectionListener() {
            override fun onColorSelected(color: Int) {
                // Do whatever you want with the color
                gcolor = color
            }
        })
        findViewById<Button>(R.id.pickcolorbutton).setOnClickListener {
            val preferences: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this)
            val editor = preferences.edit()
            editor.putString("ui_color", gcolor.toString())
            editor.apply()
            displaySetColor()
        }
    }

    fun displaySetColor(){
        //Toast.makeText(this, "" + PreferenceManager.getDefaultSharedPreferences(this).getString("ui_color", "haha"), Toast.LENGTH_SHORT).show()
    }
}