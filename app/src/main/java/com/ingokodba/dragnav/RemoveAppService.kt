package com.ingokodba.dragnav

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao
import kotlinx.coroutines.*

class RemoveAppService : Service() {
    lateinit var korutina: Job
    val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.extras?.getString("packageName")
        val ctx = this
        korutina = scope.launch {
            // New coroutine that can call suspend functions
            val db = AppDatabase.getInstance(ctx)
            val appDao: AppInfoDao = db.appInfoDao()
            val lista = appDao.getAll()
            lista.find { it.packageName == packageName }.let { app ->
                Log.v("ingo", "all apps -> " + lista.map { it.packageName }.toString())
                Log.v("ingo", "removing app " + packageName)
                if (app != null) {
                    appDao.delete(app)
                    Log.v("ingo", "app removed")
                }
            }
            val mainActivity = MainActivity.getInstance(ctx)
            if(mainActivity != null) {
                val app =
                    mainActivity.viewModel.appsList.value!!.find { it.packageName == packageName }
                if (app != null) {
                    withContext(Dispatchers.Main) {
                        mainActivity.viewModel.removeApp(app)
                        mainActivity.activitiesFragment.radapter.notifyDataSetChanged()
                        Log.v("ingo", "app removed for sure!")
                    }
                }
            }
        }
        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}