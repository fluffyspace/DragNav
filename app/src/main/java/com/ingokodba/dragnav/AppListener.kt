package com.ingokodba.dragnav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao


class AppListener : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO Auto-generated method stub
        Log.v("ingo", "there is a broadcast")
        if(intent.getExtras() != null && intent.extras!!.containsKey(Intent.ACTION_PACKAGE_REMOVED)) {
            val db = AppDatabase.getInstance(context)
            val appDao: AppInfoDao = db.appInfoDao()
            val lista = appDao.getAll()
            val app = lista.find { it.packageName ==  intent.data?.schemeSpecificPart }
            Log.v("ingo", "removing app")
            if (app != null) {
                appDao.delete(app)
                Log.v("ingo", "app removed")
            }
        }
    }
}