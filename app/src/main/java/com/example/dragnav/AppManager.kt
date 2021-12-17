package com.example.dragnav

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.MATCH_ALL

class AppManager(ctx:Context) {
    /*private val packageManager = ctx.packageManager

    fun getLaunchableApps(): List<Application> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, MATCH_ALL)
            .map { it.activityInfo }
            .map { Application(it.packageName, it.loadLabel(packageManager).toString(), it)}
    }*/
}