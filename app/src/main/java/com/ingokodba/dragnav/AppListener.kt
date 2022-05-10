package com.ingokodba.dragnav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.baza.AppInfoDao


class AppListener : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO Auto-generated method stub
        if(intent.getExtras() != null) Log.v("ingo", "there is a broadcast " + intent.extras!!.keySet().map{it}.toString())
        Toast.makeText(context, "ndawndiwanad", Toast.LENGTH_SHORT).show()
        if(intent.getExtras() != null && (intent.extras!!.containsKey(Intent.ACTION_PACKAGE_ADDED) || intent.extras!!.containsKey("android.content.pm.extra.DATA_LOADER_TYPE"))) {
            MainActivity.getInstance(context)?.loadApp(intent.data!!.schemeSpecificPart)
        }
        if(intent.getExtras() != null && (intent.extras!!.containsKey(Intent.ACTION_PACKAGE_REMOVED) || intent.extras!!.containsKey(Intent.ACTION_PACKAGE_FULLY_REMOVED) || intent.extras!!.containsKey("android.intent.extra.REMOVED_FOR_ALL_USERS"))) {
            Log.d("ingo", intent.data!!.schemeSpecificPart)

            Intent(context, RemoveAppService::class.java).apply {putExtra("packageName", intent.data!!.schemeSpecificPart)}.also { intent ->
                context.startService(intent)
            }
        }
    }
}