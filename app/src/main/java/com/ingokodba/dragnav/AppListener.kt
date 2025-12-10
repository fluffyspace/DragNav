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
        val packageName = intent.data?.schemeSpecificPart ?: return

        Log.v("ingokodba", "Received broadcast: action=${intent.action}, packageName=$packageName")

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                // Don't reload app if it's just being updated (REPLACING flag is set)
                if (intent.action == Intent.ACTION_PACKAGE_ADDED &&
                    intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    Log.d("ingokodba", "Package is being replaced, skipping initial add")
                    return
                }
                Log.d("ingokodba", "Loading/updating app: $packageName")
                MainActivityCompose.getInstance()?.loadApp(packageName)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // Don't remove if it's being replaced (will be re-added)
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    Log.d("ingokodba", "Package is being replaced, skipping removal")
                    return
                }
                Log.d("ingokodba", "Removing app: $packageName")
                MainActivityCompose.getInstance()?.appDeleted(packageName)
            }
        }
    }
}