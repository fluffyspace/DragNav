package com.ingokodba.dragnav

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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