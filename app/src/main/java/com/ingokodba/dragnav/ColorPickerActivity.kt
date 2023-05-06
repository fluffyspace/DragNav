package com.ingokodba.dragnav

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.dragnav.R
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorListener
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar


class ColorPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.colorpickerlayout)
        displaySetColor()

        var gcolor:Int = 0

        val colorPickerView = findViewById<ColorPickerView>(R.id.colorPickerView)
        colorPickerView.setColorListener(ColorListener { color, fromUser ->
            gcolor = color
        })
        val brightnessSlideBar = findViewById<BrightnessSlideBar>(R.id.brightnessSlide)
        colorPickerView.attachBrightnessSlider(brightnessSlideBar)
        findViewById<Button>(R.id.pickcolorbutton).setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra("color", gcolor).putExtra("forPrimaryColor", true))
            finish()
            /*val preferences: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this)
            val editor = preferences.edit()
            editor.putString("ui_color", gcolor.toString())
            editor.apply()
            displaySetColor()
            finish()*/
        }
    }

    fun displaySetColor(){
        //Toast.makeText(this, "" + PreferenceManager.getDefaultSharedPreferences(this).getString("ui_color", "haha"), Toast.LENGTH_SHORT).show()
    }
}