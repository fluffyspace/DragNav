package com.ingokodba.dragnav

import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import android.util.Log

/**
 * Implementation of LauncherApps.Callback which handles package lifecycle events
 * using the modern LauncherApps API instead of BroadcastReceiver.
 *
 * This provides more reliable event delivery and better integration with the Android system.
 */
class ModelLauncherCallbacks : LauncherApps.Callback() {

    override fun onPackageAdded(packageName: String, user: UserHandle) {
        Log.d(TAG, "Package added: $packageName (user: $user)")
        MainActivity.getInstance()?.loadApp(packageName)
    }

    override fun onPackageChanged(packageName: String, user: UserHandle) {
        Log.d(TAG, "Package changed: $packageName (user: $user)")
        MainActivity.getInstance()?.updateApp(packageName)
    }

    override fun onPackageRemoved(packageName: String, user: UserHandle) {
        Log.d(TAG, "Package removed: $packageName (user: $user)")
        MainActivity.getInstance()?.appDeleted(packageName)
    }

    override fun onPackagesAvailable(
        packageNames: Array<out String>,
        user: UserHandle,
        replacing: Boolean
    ) {
        Log.d(TAG, "Packages available: ${packageNames.joinToString()}, replacing: $replacing")
        if (!replacing) {
            // Packages are now available after being unavailable
            packageNames.forEach { packageName ->
                MainActivity.getInstance()?.loadApp(packageName)
            }
        }
    }

    override fun onPackagesUnavailable(
        packageNames: Array<out String>,
        user: UserHandle,
        replacing: Boolean
    ) {
        Log.d(TAG, "Packages unavailable: ${packageNames.joinToString()}, replacing: $replacing")
        if (!replacing) {
            // Packages are unavailable (e.g., SD card removed) - remove from UI
            packageNames.forEach { packageName ->
                MainActivity.getInstance()?.appDeleted(packageName)
            }
        }
    }

    override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
        Log.d(TAG, "Packages suspended: ${packageNames.joinToString()}")
        // Apps are suspended by admin/parental controls
        packageNames.forEach { packageName ->
            MainActivity.getInstance()?.onAppSuspended(packageName, true)
        }
    }

    override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
        Log.d(TAG, "Packages unsuspended: ${packageNames.joinToString()}")
        packageNames.forEach { packageName ->
            MainActivity.getInstance()?.onAppSuspended(packageName, false)
        }
    }

    override fun onShortcutsChanged(
        packageName: String,
        shortcuts: MutableList<ShortcutInfo>,
        user: UserHandle
    ) {
        Log.d(TAG, "Shortcuts changed for: $packageName, count: ${shortcuts.size}")
        MainActivity.getInstance()?.onShortcutsChanged(packageName, shortcuts)
    }

    override fun onPackageLoadingProgressChanged(
        packageName: String,
        user: UserHandle,
        progress: Float
    ) {
        Log.v(TAG, "Package loading progress: $packageName, progress: ${(progress * 100).toInt()}%")
        MainActivity.getInstance()?.onInstallProgress(packageName, progress)
    }

    companion object {
        private const val TAG = "ModelLauncherCallbacks"
    }
}
