package com.ingokodba.dragnav

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dragnav.R
import com.google.android.material.slider.Slider


class MainActivity2 : AppCompatActivity() {
    var lista = listOf("ingo", "sara", "metak", "kolokvij", "drugarice", "drugovi", "vaga", "riba", "žaba")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        findViewById<Slider>(R.id.slider).addOnChangeListener { slider, value, fromUser ->
            // Responds to when slider's value is changed
            Log.d("ingo", value.toString())
            findViewById<CircleView>(R.id.circleview).setStepSize(value)
            //findViewById<CircleView>(R.id.circleview).setTextList(lista)

        }
        /*findViewById<CircleView>(R.id.circleview).setEventListener(object :
            com.ingokodba.CircleView.IMyEventListener {
            override fun onEventOccurred(event: MotionEvent, counter: kotlin.Int) {
                // TODO Auto-generated method stub
                touched(counter)
            }
        })*/
    }

    fun touched(counter:Int){
        Toast.makeText(this, lista[counter], Toast.LENGTH_SHORT).show()

    }
}