package com.ingokodba.dragnav

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.dragnav.R

class MainActivity3 : AppCompatActivity() {
    // testna aktivnost
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge for Android 15+ compatibility
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_recycle_scroller)
    }
}