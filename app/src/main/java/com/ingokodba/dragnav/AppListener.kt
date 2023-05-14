package com.ingokodba.dragnav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson


class AppListener : BroadcastReceiver() {
    // broadcasts of application installs, uninstalls, updates
    override fun onReceive(context: Context, intent: Intent) {
        // TODO Auto-generated method stub
        if(intent.extras != null){
            Log.v("ingokodba", "there is a broadcast " + intent.extras!!.keySet().map{it}.toString() + ", packageName: " + intent.data!!.schemeSpecificPart + " " + Gson().toJson(intent))
            if(intent.extras!!.containsKey(Intent.ACTION_PACKAGE_ADDED) || intent.extras!!.containsKey("android.content.pm.extra.DATA_LOADER_TYPE")) {
                Log.d("ingokodba", "loading app!")
                MainActivity.getInstance()?.loadApp(intent.data!!.schemeSpecificPart)
            }
            if(intent.extras!!.containsKey(Intent.ACTION_PACKAGE_REMOVED) || intent.extras!!.containsKey(Intent.ACTION_PACKAGE_FULLY_REMOVED) || intent.extras!!.containsKey("android.intent.extra.REMOVED_FOR_ALL_USERS")) {
                Log.d("ingokodba", "removing an app " + intent.data!!.schemeSpecificPart)
                MainActivity.getInstance()?.appDeleted(intent.data!!.schemeSpecificPart)
                /*Intent(context, RemoveAppService::class.java).apply {putExtra("packageName", intent.data!!.schemeSpecificPart)}.also { intent ->
                    context.startForegroundService(intent)
                }*/
            }
        }
    }
}