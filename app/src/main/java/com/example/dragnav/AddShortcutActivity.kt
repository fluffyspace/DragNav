package com.example.dragnav

import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class AddShortcutActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        val shortcutManager = getSystemService(LauncherApps::class.java)
        shortcutManager.getPinItemRequest( this.getIntent() );

        //handle request...

        this.finish();
    }
}