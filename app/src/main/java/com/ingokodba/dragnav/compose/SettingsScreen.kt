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
import com.ingokodba.dragnav.UiDesignEnum

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
    val currentUiDesign = prefs.getString(PreferenceKeys.UI_DESIGN, uiDesignValues[0]) ?: uiDesignValues[0]
    val currentUiDesignIndex = uiDesignValues.indexOf(currentUiDesign).coerceIn(0, uiDesignEntries.size - 1)

    // Determine which UI-specific settings to show
    val showCircleSettings = currentUiDesignIndex in 2..4 // CIRCLE modes
    val showRainbowSettings = currentUiDesignIndex in 0..1 // RAINBOW modes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            // UI Design Preference
            item {
                var showDialog by remember { mutableStateOf(false) }

                PreferenceItem(
                    title = "UI Design",
                    summary = uiDesignEntries[currentUiDesignIndex],
                    onClick = { showDialog = true }
                )

                if (showDialog) {
                    UiDesignDialog(
                        currentValue = currentUiDesign,
                        entries = uiDesignEntries,
                        values = uiDesignValues,
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
                        title = "Circle Settings",
                        summary = "Customize circle appearance",
                        onClick = { navController.navigate("settings/circle") }
                    )
                }
            }

            // Rainbow Settings (for RAINBOW UI modes)
            if (showRainbowSettings) {
                item {
                    PreferenceItem(
                        title = "Rainbow Settings",
                        summary = "Customize rainbow appearance",
                        onClick = { navController.navigate("settings/rainbow") }
                    )
                }
            }

            // General Settings
            item {
                PreferenceItem(
                    title = "General",
                    summary = "App-wide preferences",
                    onClick = { navController.navigate("settings/general") }
                )
            }

            // Help & About
            item {
                PreferenceItem(
                    title = "Help & About",
                    summary = "Support, backup, and app info",
                    onClick = { navController.navigate("settings/help") }
                )
            }

            // Default Apps
            item {
                PreferenceItem(
                    title = "Default Apps",
                    summary = "Set as default launcher",
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
            text = { Text("The app needs to restart for changes to take effect.") },
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

/**
 * UI Design selection dialog
 */
@Composable
fun UiDesignDialog(
    currentValue: String,
    entries: Array<String>,
    values: Array<String>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select UI Design") },
        text = {
            LazyColumn {
                items(entries.size) { index ->
                    val value = values[index]
                    val entry = entries[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == currentValue,
                            onClick = { onSelected(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(entry)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
