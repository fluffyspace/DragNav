package com.ingokodba.dragnav

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.ingokodba.dragnav.modeli.AppInfo
import java.util.Collections.max
import java.util.Collections.min

/**
 * Utility class for loading app information using the modern LauncherApps API.
 * This replaces the deprecated PackageManager-based approach.
 */
class AppLoader(private val context: Context) {

    private val launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val packageManager: PackageManager = context.packageManager

    /**
     * Load all launcher apps for all user profiles.
     * Uses LauncherApps.getActivityList() for better launcher integration.
     */
    fun loadAllApps(qualityIcons: Boolean = true): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val currentUser = Process.myUserHandle()

        // Get all user profiles (main user + work profile if exists)
        val userProfiles = try {
            // For now, just use current user
            // Multi-user support can be added later
            listOf(currentUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profiles", e)
            listOf(currentUser)
        }

        for (user in userProfiles) {
            try {
                // Query all launcher activities for this user
                val launcherActivities = launcherApps.getActivityList(null, user)

                for (activityInfo in launcherActivities) {
                    try {
                        val packageName = activityInfo.applicationInfo.packageName
                        val uid = activityInfo.applicationInfo.uid
                        val label = activityInfo.label.toString()

                        // Skip the launcher itself
                        if (packageName == context.packageName) {
                            continue
                        }

                        // Extract icon and color
                        var colorPrimary = Color.BLACK
                        var icon: Drawable? = null

                        try {
                            val density = if (qualityIcons) {
                                DisplayMetrics.DENSITY_HIGH
                            } else {
                                DisplayMetrics.DENSITY_LOW
                            }

                            icon = activityInfo.getIcon(density)
                            if (icon != null) {
                                colorPrimary = extractPrimaryColor(icon)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error loading icon for $packageName", e)
                        }

                        val appInfo = AppInfo(
                            id = uid,
                            label = label,
                            packageName = packageName,
                            color = colorPrimary.toString(),
                            installed = true
                        )

                        apps.add(appInfo)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading app info", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps for user $user", e)
            }
        }

        Log.d(TAG, "Loaded ${apps.size} apps using LauncherApps API")
        return apps
    }

    /**
     * Load a single app by package name.
     * Returns null if the app is not found or is not a launcher app.
     */
    fun loadApp(packageName: String, qualityIcons: Boolean = true): AppInfo? {
        val currentUser = Process.myUserHandle()

        try {
            // Get activities for this specific package
            val activities = launcherApps.getActivityList(packageName, currentUser)

            if (activities.isEmpty()) {
                Log.w(TAG, "No launcher activities found for $packageName")
                return null
            }

            // Use the first launcher activity
            val activityInfo = activities[0]
            val uid = activityInfo.applicationInfo.uid
            val label = activityInfo.label.toString()

            // Extract icon and color
            var colorPrimary = Color.BLACK

            try {
                val density = if (qualityIcons) {
                    DisplayMetrics.DENSITY_HIGH
                } else {
                    DisplayMetrics.DENSITY_LOW
                }

                val icon = activityInfo.getIcon(density)
                if (icon != null) {
                    colorPrimary = extractPrimaryColor(icon)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading icon for $packageName", e)
            }

            return AppInfo(
                id = uid,
                label = label,
                packageName = packageName,
                color = colorPrimary.toString(),
                installed = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading app $packageName", e)
            return null
        }
    }

    /**
     * Check if a package is installed and is a launcher app.
     */
    fun isLauncherApp(packageName: String): Boolean {
        val currentUser = Process.myUserHandle()

        try {
            val activities = launcherApps.getActivityList(packageName, currentUser)
            return activities.isNotEmpty()
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Extract the primary/dominant color from an app icon.
     * Samples the icon at 5x5 and finds the pixel with highest color saturation.
     */
    private fun extractPrimaryColor(icon: Drawable): Int {
        try {
            val bitmap: Bitmap = icon.toBitmap(5, 5, Bitmap.Config.RGB_565)
            var bestDiffJ = 0
            var bestDiffK = 0
            var bestDiff = 0

            // Find pixel with highest color saturation (RGB difference)
            for (j in 0..4) {
                for (k in 0..4) {
                    val c = bitmap.getPixel(j, k)
                    val currentDiff = max(listOf(c.red, c.green, c.blue)) -
                                     min(listOf(c.red, c.green, c.blue))
                    if (currentDiff > bestDiff) {
                        bestDiff = currentDiff
                        bestDiffJ = j
                        bestDiffK = k
                    }
                }
            }

            return bitmap.getPixel(bestDiffJ, bestDiffK)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting color from icon", e)
            return Color.BLACK
        }
    }

    companion object {
        private const val TAG = "AppLoader"
    }
}
