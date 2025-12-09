package com.ingokodba.dragnav

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {
    /**
     * Check if notification listener permission is granted
     */
    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListeners.contains(context.packageName)
    }

    /**
     * Open notification listener settings
     */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Request notification access if not enabled
     * @return true if already enabled, false if settings were opened
     */
    fun requestNotificationAccessIfNeeded(context: Context): Boolean {
        return if (isNotificationAccessEnabled(context)) {
            true
        } else {
            openNotificationSettings(context)
            false
        }
    }
}
