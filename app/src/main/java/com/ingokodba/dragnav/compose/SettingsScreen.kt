package com.ingokodba.dragnav.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.PreferenceKeys

/**
 * Main Settings Screen - Compose version of MySettingsFragment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var showRestartDialog by remember { mutableStateOf(false) }
    var restartAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Get UI design mode
    val uiDesignValues = context.resources.getStringArray(R.array.ui_designs_values)
    val uiDesignEntries = context.resources.getStringArray(R.array.ui_designs_entries)
    val currentUiDesign = prefs.getString(PreferenceKeys.UI_DESIGN, uiDesignValues[4]) ?: uiDesignValues[4]
    val currentUiDesignIndex = uiDesignValues.indexOf(currentUiDesign).coerceIn(0, uiDesignEntries.size - 1)

    // Determine which UI-specific settings to show
    val showCircleSettings = currentUiDesignIndex in 0..2 // CIRCLE modes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            // UI Design Preference
            item {
                var showDialog by remember { mutableStateOf(false) }

                PreferenceItem(
                    title = stringResource(R.string.ui_design),
                    summary = uiDesignEntries[currentUiDesignIndex],
                    onClick = { showDialog = true }
                )

                if (showDialog) {
                    RadioSelectionDialog(
                        title = stringResource(R.string.select_ui_design),
                        entries = uiDesignEntries.toList(),
                        values = uiDesignValues.toList(),
                        selectedValue = currentUiDesign,
                        onDismiss = { showDialog = false },
                        onSelected = { newValue ->
                            prefs.edit().putString(PreferenceKeys.UI_DESIGN, newValue).apply()
                            showDialog = false
                            restartAction = {
                                mainActivity.recreate()
                            }
                            showRestartDialog = true
                        }
                    )
                }
            }

            // Circle Settings (for CIRCLE UI modes)
            if (showCircleSettings) {
                item {
                    PreferenceItem(
                        title = stringResource(R.string.circle_settings),
                        summary = stringResource(R.string.circle_settings_summary),
                        onClick = { navController.navigate("settings/circle") }
                    )
                }
            }

            // Notifications Settings
            item {
                PreferenceItem(
                    title = stringResource(R.string.notifications),
                    summary = stringResource(R.string.notifications_summary),
                    onClick = { navController.navigate("settings/notifications") }
                )
            }

            // General Settings
            item {
                PreferenceItem(
                    title = stringResource(R.string.general),
                    summary = stringResource(R.string.general_summary),
                    onClick = { navController.navigate("settings/general") }
                )
            }

            // Hidden Apps
            item {
                PreferenceItem(
                    title = stringResource(R.string.hidden_apps),
                    summary = stringResource(R.string.hidden_apps_summary),
                    onClick = { navController.navigate(com.ingokodba.dragnav.navigation.NavRoute.SettingsHiddenApps.route) }
                )
            }

            // Help & About
            item {
                PreferenceItem(
                    title = stringResource(R.string.help_and_about),
                    summary = stringResource(R.string.help_summary),
                    onClick = { navController.navigate("settings/help") }
                )
            }

            // Default Apps
            item {
                PreferenceItem(
                    title = stringResource(R.string.default_apps),
                    summary = stringResource(R.string.default_apps_summary),
                    onClick = { mainActivity.openDefaultApps() }
                )
            }
        }
    }

    // Restart Dialog
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_required)) },
            text = { Text(stringResource(R.string.restart_for_changes)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    restartAction?.invoke()
                }) {
                    Text(stringResource(R.string.restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Reusable preference item composable
 */
@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
    HorizontalDivider()
}

