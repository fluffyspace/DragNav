package com.ingokodba.dragnav.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.NotificationUtils
import com.ingokodba.dragnav.PreferenceKeys

/**
 * Notification Settings Screen - manage notification permissions and excluded apps
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check notification permission status
    var isNotificationAccessEnabled by remember {
        mutableStateOf(NotificationUtils.isNotificationAccessEnabled(context))
    }

    // Get excluded apps count
    val excludedAppsJson = prefs.getString(PreferenceKeys.NOTIFICATION_EXCLUDED_APPS, "[]") ?: "[]"
    val excludedAppsCount = remember(excludedAppsJson) {
        try {
            com.google.gson.Gson().fromJson(excludedAppsJson, Array<String>::class.java)?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // Refresh permission status when returning to this screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationAccessEnabled = NotificationUtils.isNotificationAccessEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Notification Access Permission
            item {
                PreferenceItem(
                    title = "Notification Access",
                    summary = if (isNotificationAccessEnabled) "Enabled" else "Disabled - tap to enable",
                    onClick = {
                        NotificationUtils.openNotificationSettings(context)
                    }
                )
            }

            // Excluded Apps
            item {
                PreferenceItem(
                    title = "Exclude Apps",
                    summary = if (excludedAppsCount > 0) "$excludedAppsCount apps excluded" else "No apps excluded",
                    onClick = { navController.navigate("settings/notifications/excluded") }
                )
            }
        }
    }
}
