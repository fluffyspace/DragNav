package com.ingokodba.dragnav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import androidx.navigation.ui.setupActionBarWithNavController
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppDatabase.Companion.DATABASE_NAME
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference

class SettingsActivity : AppCompatActivity(R.layout.activity_settings){
    var navController: NavController? = null
    var navHostFragment: NavHostFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.settings_toolbar))
        //supportActionBar?.setDisplayHomeAsUpEnabled(true);
        /*supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_view, MySettingsFragment())
            .commit()*/

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment!!.navController
        setupActionBarWithNavController(navController!!)
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

    override fun onSupportNavigateUp() = navController!!.navigateUp()

    fun startColorpicker(){
        val intent = Intent(this@SettingsActivity, ColorPickerActivity::class.java)
        resultLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ingo", "settings destroyed")
    }

    fun openDefaultApps(){
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    fun to_backup() {
        val db = AppDatabase.getInstance(this)
        try {
            db.close()
            val currentDBPath = this.getDatabasePath(DATABASE_NAME).path
            Log.e("ingo", currentDBPath)
            Log.d("TAG", "DatabaseHandler: can write in sd")
            //Replace with YOUR_PACKAGE_NAME and YOUR_DB_NAME
            //Replace with YOUR_FOLDER_PATH and TARGET_DB_NAME in the SD card
            val copieDBPath = "${DATABASE_NAME}_backup"
            val currentDB = File(currentDBPath)
            val copieDB = File(this.filesDir, copieDBPath)
            if (currentDB.exists()) {
                Log.d("TAG", "DatabaseHandler: DB exist")
                val src = FileInputStream(currentDB).channel
                val dst = FileOutputStream(copieDB).channel
                dst.transferFrom(src, 0, src.size())
                src.close()
                dst.close()
            } else {
                Log.e("ingo", "currentDB doesnt exist")
            }
            db.setInstanceToNull()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun from_backup() {
        val db = AppDatabase.getInstance(this)
        try {
            db.close()
            Log.d("TAG", "DatabaseHandler: can write in sd")
            //Replace with YOUR_PACKAGE_NAME and YOUR_DB_NAME
            val currentDBPath = this.getDatabasePath(DATABASE_NAME).path

            //Replace with YOUR_FOLDER_PATH and TARGET_DB_NAME in the SD card
            val copieDBPath = "${DATABASE_NAME}_backup"
            val currentDB = File(currentDBPath)
            val copieDB = File(this.filesDir, copieDBPath)
            if (copieDB.exists()) {
                Log.d("TAG", "DatabaseHandler: DB exist")
                val src = FileInputStream(copieDB).channel
                val dst = FileOutputStream(currentDB).channel
                dst.transferFrom(src, 0, src.size())
                src.close()
                dst.close()
            } else {
                Log.e("ingo", "copieDB doesnt exist")
            }
            db.setInstanceToNull()
            val data = Intent()
            data.putExtra("reload_apps", true);
            this.setResult(Activity.RESULT_OK, data);
            this.finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = pref.fragment?.let {
            supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                it
            )
        }
        if (fragment != null) {
            fragment.arguments = args
            fragment.setTargetFragment(caller, 0)
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .addToBackStack(null)
                .commit()
            return true
        }
        return false
    }*/

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->         // Respond to the action bar's Up/Home button
                return false
        }
        return super.onOptionsItemSelected(item)
    }*/



    /*override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        //supportActionBar?.title = pref.title
        Log.d("ingo", "klik ${pref.title}")
        val args = pref.extras
        val fragment = pref.fragment?.let {
            supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                it
            )
        }
        if (fragment != null) {
            fragment.arguments = args
            fragment.setTargetFragment(caller, 0)
            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .addToBackStack(null)
                .commit()
        }

        return true
    }*/

}