package com.ingokodba.dragnav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

class SettingsActivity : AppCompatActivity(R.layout.activity_settings){
    var navController: NavController? = null
    var navHostFragment: NavHostFragment? = null

    companion object{
        const val CREATE_BACKUP_FILE = 1
        const val OPEN_BACKUP_FILE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge for Android 15+ compatibility
        enableEdgeToEdge()
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
        
        // Handle window insets to prevent content from going behind system bars
        window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)?.let { content ->
            content.getChildAt(0)?.let { rootLayout ->
                ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                    WindowInsetsCompat.CONSUMED
                }
            }
        }
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
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "backup.sqlite")
            type = "application/x-sqlite3"
        }
        startActivityForResult(intent, CREATE_BACKUP_FILE)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == CREATE_BACKUP_FILE && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                val db = AppDatabase.getInstance(this)
                try {
                    db.close()
                    val currentDBPath = this.getDatabasePath(DATABASE_NAME).path
                    Log.e("ingo", currentDBPath)
                    Log.d("TAG", "DatabaseHandler: can write in sd")
                    val currentDB = File(currentDBPath)

                    contentResolver.openFileDescriptor(uri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { fos ->
                            FileInputStream(currentDB).use { fis ->
                                fos.channel.transferFrom(fis.channel, 0, fis.channel.size())
                            }
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                db.setInstanceToNull()
            }
        } else if (requestCode == OPEN_BACKUP_FILE && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                val db = AppDatabase.getInstance(this)
                try {
                    db.close()
                    val currentDBPath = this.getDatabasePath(DATABASE_NAME).path
                    Log.e("ingo", currentDBPath)
                    Log.d("TAG", "DatabaseHandler: can write in sd")
                    val currentDB = File(currentDBPath)
                    contentResolver.openFileDescriptor(uri, "r")?.use {
                        FileInputStream(it.fileDescriptor).use { fis ->
                            FileOutputStream(currentDB).use { fos ->
                                fos.channel.transferFrom(fis.channel, 0, fis.channel.size())
                            }
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                db.setInstanceToNull()
                val data = Intent()
                data.putExtra("restart", true);
                this.setResult(Activity.RESULT_OK, data);
                this.finish()
            }
        }
    }

    fun to_backup_old() {
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

    fun from_backup(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, OPEN_BACKUP_FILE)

    }

    fun from_backup_old() {
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