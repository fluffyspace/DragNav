package com.ingokodba.dragnav

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ingokodba.dragnav.compose.AppNotification

class NotificationListener : NotificationListenerService() {

    private var updateRequestReceiver: BroadcastReceiver? = null

    companion object {
        const val ACTION_NOTIFICATIONS_UPDATED = "com.ingokodba.dragnav.NOTIFICATIONS_UPDATED"
        const val EXTRA_NOTIFICATIONS = "notifications"

        @Volatile
        private var instance: NotificationListener? = null

        fun getInstance(): NotificationListener? = instance

        // Callback for when notifications change
        var onNotificationsChanged: ((List<AppNotification>) -> Unit)? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.d("NotificationListener", "Notification posted: ${sbn?.packageName}")
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d("NotificationListener", "Notification removed: ${sbn?.packageName}")
        updateNotifications()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d("NotificationListener", "=== Listener connected ===")

        // Register receiver for update requests
        updateRequestReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("NotificationListener", "Received update request")
                updateNotifications()
            }
        }
        val filter = IntentFilter("com.ingokodba.dragnav.REQUEST_NOTIFICATION_UPDATE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateRequestReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateRequestReceiver, filter)
        }

        updateNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.d("NotificationListener", "=== Listener disconnected ===")
        updateRequestReceiver?.let {
            unregisterReceiver(it)
        }
    }

    private fun updateNotifications() {
        try {
            val activeNotifications = activeNotifications ?: run {
                Log.d("NotificationListener", "No active notifications (null)")
                return
            }

            Log.d("NotificationListener", "Processing ${activeNotifications.size} active notifications")
            val notificationMap = mutableMapOf<String, AppNotification>()

            for (sbn in activeNotifications) {
                val packageName = sbn.packageName
                Log.d("NotificationListener", "Found notification from: $packageName")

                // Skip system notifications and our own app
                if (packageName == "android" ||
                    packageName == "com.android.systemui" ||
                    packageName == "com.ingokodba.dragnav") {
                    Log.d("NotificationListener", "Skipping system/self notification: $packageName")
                    continue
                }

                val notification = sbn.notification ?: continue
                val extras = notification.extras ?: continue

                val title = extras.getCharSequence("android.title")?.toString() ?: "Notification"
                val text = extras.getCharSequence("android.text")?.toString() ?: ""

                // Get app icon
                val appIcon = try {
                    packageManager.getApplicationIcon(packageName)
                } catch (e: Exception) {
                    null
                }

                // Only keep one notification per app (the most recent)
                val appNotification = AppNotification(
                    packageName = packageName,
                    appIcon = appIcon,
                    title = title,
                    content = text
                )
                notificationMap[packageName] = appNotification
                Log.d("NotificationListener", "Added: $packageName - $title")
            }

            Log.d("NotificationListener", "=== Final notification count: ${notificationMap.size} ===")

            val notificationsList = notificationMap.values.toList()

            // Call the callback directly
            onNotificationsChanged?.invoke(notificationsList)
            Log.d("NotificationListener", "Called callback with ${notificationsList.size} notifications")

            // Also broadcast for backwards compatibility
            val intent = Intent(ACTION_NOTIFICATIONS_UPDATED)
            intent.putParcelableArrayListExtra(EXTRA_NOTIFICATIONS,
                ArrayList(notificationsList.map { NotificationData(it) }))
            sendBroadcast(intent)

            Log.d("NotificationListener", "Broadcast sent with ${notificationsList.size} notifications")
        } catch (e: Exception) {
            Log.e("NotificationListener", "Error updating notifications", e)
        }
    }
}
