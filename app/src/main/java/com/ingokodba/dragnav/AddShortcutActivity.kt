package com.ingokodba.dragnav

import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.MeniJednoPoljeDao
import com.ingokodba.dragnav.modeli.MeniJednoPolje
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class AddShortcutActivity : AppCompatActivity() {
    var pocetnaId = 0
    var meniPolja:List<MeniJednoPolje> = listOf()
    var pinItemRequest:LauncherApps.PinItemRequest? = null
    lateinit var shortcut_as_meni: MeniJednoPolje
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_shortcut)
        Log.d("ingo", "AddShortcutActivity oncreate")
        findViewById<Button>(R.id.cancel).setOnClickListener {
            Toast.makeText(this, "Shortcut not added.", Toast.LENGTH_SHORT).show()
            val launchNewIntent = Intent(this@AddShortcutActivity, MainActivity::class.java)
            startActivity(launchNewIntent, null)
        }
        val launcherApps = getSystemService(LauncherApps::class.java)
        pinItemRequest = launcherApps.getPinItemRequest( this.getIntent() );
        if(pinItemRequest != null) {
            val shortcut = pinItemRequest!!.shortcutInfo
            shortcut_as_meni = MeniJednoPolje(
                0,
                shortcut?.shortLabel.toString(),
                nextIntent = shortcut?.`package`.toString(),
                nextId = shortcut?.id.toString(),
                shortcut = true,
                color = "0"
            )
            val context = this
            lifecycleScope.launch(Dispatchers.IO) {
                if (initializeRoom()) {
                    withContext(Dispatchers.Main) {
                        //goToPocetna()
                        val foldersAdapter = FoldersAdapter(context, meniPolja)
                        findViewById<RecyclerView>(R.id.recycler_view4).adapter = foldersAdapter
                        foldersAdapter.notifyDataSetChanged()
                    }
                } else {
                    Log.d("ingo", "initialize bad")
                }
                //loadIcons()
                Log.d("ingo", "end of initialization")
            }

        } else {
            Toast.makeText(this, "no", Toast.LENGTH_SHORT).show()
        }

        //handle request...

        //this.finish();
    }

    data class Veve (val iid:Int
    )

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(id: Veve) {
        lifecycleScope.launch(Dispatchers.IO) {
            val novo_polje = databaseAddNewPolje(shortcut_as_meni)
            meniPolja[id.iid].polja = meniPolja[id.iid].polja.plus(novo_polje.id)
            Log.d("ingo", "onMessageEvent dodaj u polje " + meniPolja[id.iid].text)
            databaseUpdateItem(meniPolja[id.iid])
            Log.d("ingo", "onMessageEvent ended")
            withContext(Dispatchers.Main){
                pinItemRequest?.accept()
                finish()
            }
        }
    }

    fun databaseUpdateItem(polje: MeniJednoPolje){
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        recDao.update(polje)
        Log.d("ingo", "updated " + polje.text + "(" + polje.id + ")")
    }

    fun databaseAddNewPolje(polje: MeniJednoPolje): MeniJednoPolje {
        polje.id = 0
        Log.d("ingo", "databaseAddNewPolje(" + polje.text + ")")
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        val rowid = recDao.insertAll(polje)
        return databaseGetItemByRowId(rowid.first())
    }

    fun databaseGetItemByRowId(id:Long): MeniJednoPolje {
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        Log.d("ingo", "databaseGetItemByRowId " + id)
        return recDao.findByRowId(id).first()
    }

    suspend fun initializeRoom():Boolean{
        Log.d("ingo", "initializeRoom start")
        val db = AppDatabase.getInstance(this)
        val recDao: MeniJednoPoljeDao = db.meniJednoPoljeDao()
        meniPolja = recDao.getAll()
        if(meniPolja.size == 0) return false
        Log.d("ingo", meniPolja.map{it.text}.toString())
        pocetnaId = meniPolja.first().id
        /*val appDao: AppInfoDao = db.appInfoDao()
        Log.d("ingo", "initializeRoom before cache")
        if(cache_apps) {
            val apps = appDao.getAll() as MutableList<AppInfo>
            withContext(Dispatchers.Main) {
                viewModel.addApps(apps)
            }

            Log.d("ingo", "loaded " + (viewModel.appsList.value?.size ?: 0) + " cached apps")
        }
        Log.d("ingo", "initializeRoom before loadnewapps")
        val newApps = loadNewApps()
        Log.d("ingo", "initializeRoom before insertall")
        if(cache_apps && newApps.isNotEmpty()){
            for(app in newApps){
                Log.d("ingo", "new app " + app.label)
                appDao.insertAll(app)
            }
        }
        for(app in listaMenija){
            loadIcon(app.nextIntent)
        }
        Log.d("ingo", "initializeRoom before addall")
        withContext(Dispatchers.Main) {
            viewModel.addApps(newApps)
            circleView.icons = viewModel.icons.value!!
        }
        //radapter.appsList.sortBy { it.label.toString().lowercase() }
        //if(cache_apps){
        Log.d("ingo", "initializeRoom before loadicons")*/
        return true
    }
}