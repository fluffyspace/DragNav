package com.ingokodba.dragnav

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dragnav.R
import com.google.android.material.slider.Slider


class MainActivity2 : AppCompatActivity() {
    // testna aktivnost
    lateinit var bottom:BottomMenuView
    lateinit var circle:CircleView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test)
        bottom = findViewById<BottomMenuView>(R.id.bottomMenuView)
        circle = findViewById<CircleView>(R.id.bottomMenuView)
        bottom.buttonsState = BottomMenuView.BUTTONS_SHOWN
        findViewById<Slider>(R.id.slider1).addOnChangeListener { slider, value, fromUser ->
            Log.d("ingo", value.toString())
            bottom.overridenRadius = (value*1000).toInt()
            sliderChanged()
        }
        findViewById<Slider>(R.id.slider2).addOnChangeListener { slider, value, fromUser ->
            Log.d("ingo", value.toString())
            bottom.overridenDetectSize = (value*400)
            bottom.overridenEditDetectSize = (value*400)
            sliderChanged()
        }
        // top
        findViewById<Slider>(R.id.slider3).addOnChangeListener { slider, value, fromUser ->
            Log.d("ingo", value.toString())
            bottom.overridenEditDetectSize = (value*400)
            sliderChanged()
        }
        /*findViewById<CircleView>(R.id.circleview).setEventListener(object :
            com.ingokodba.CircleView.IMyEventListener {
            override fun onEventOccurred(event: MotionEvent, counter: kotlin.Int) {
                // TODO Auto-generated method stub
                touched(counter)
            }
        })*/
    }

    fun sliderChanged(){
        //findViewById<TextView>(R.id.tekstic).setText(findViewById<Slider>(R.id.slider1).value.toString() + " " + findViewById<Slider>(R.id.slider2).value.toString() + " " + findViewById<Slider>(R.id.slider3).value.toString())
        bottom.invalidate()
        bottom.requestLayout()
        circle.invalidate()
        circle.requestLayout()
    }
}