package com.ingokodba.dragnav.compose

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 * General Settings Screen - Compose version of MyGeneralSettingsFragment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var showRestartDialog by remember { mutableStateOf(false) }
    var restartAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDarkModeDialog by remember { mutableStateOf(false) }

    val darkModeValues = context.resources.getStringArray(R.array.dark_mode_values)
    val darkModeEntries = context.resources.getStringArray(R.array.dark_mode_entries)
    val currentDarkMode = prefs.getString(PreferenceKeys.DARK_MODE, darkModeValues[1]) ?: darkModeValues[1]
    val currentDarkModeIndex = darkModeValues.indexOf(currentDarkMode).coerceIn(0, darkModeEntries.size - 1)

    val languageToggle = prefs.getBoolean(PreferenceKeys.UI_LANGUAGE_TOGGLE, false)
    val onelineButtons = prefs.getBoolean(PreferenceKeys.UI_ONELINE, false)
    val backButton = prefs.getBoolean(PreferenceKeys.UI_BACKBUTTON, false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General Settings") },
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
            // Dark Mode
            item {
                PreferenceItem(
                    title = "Dark Mode",
                    summary = darkModeEntries[currentDarkModeIndex],
                    onClick = { showDarkModeDialog = true }
                )
            }

            // Language Toggle
            item {
                SwitchPreferenceItem(
                    title = "Alternative Language",
                    summary = "Switch app language",
                    checked = languageToggle,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_LANGUAGE_TOGGLE, checked).apply()
                        restartAction = {
                            mainActivity.recreate()
                        }
                        showRestartDialog = true
                    }
                )
            }

            // One-line Buttons
            item {
                SwitchPreferenceItem(
                    title = "One-line Buttons",
                    summary = "Show buttons in a single line",
                    checked = onelineButtons,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_ONELINE, checked).apply()
                    }
                )
            }

            // Back Button
            item {
                SwitchPreferenceItem(
                    title = "Back Button",
                    summary = "Enable back button behavior",
                    checked = backButton,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_BACKBUTTON, checked).apply()
                    }
                )
            }

            // Notification Access
            item {
                PreferenceItem(
                    title = "Notification Access",
                    summary = "Manage notification permissions",
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
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

            // Import (placeholder)
            item {
                PreferenceItem(
                    title = "Import",
                    summary = "Import app configuration",
                    onClick = {
                        // TODO: Implement import
                    }
                )
            }

            // Export (placeholder)
            item {
                PreferenceItem(
                    title = "Export",
                    summary = "Export app configuration",
                    onClick = {
                        // TODO: Implement export
                    }
                )
            }

            // Feedback
            item {
                PreferenceItem(
                    title = "Send Feedback",
                    summary = "Report bugs or suggest features",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("ingokodba@gamil.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // Drop Database
            item {
                PreferenceItem(
                    title = "Reset Database",
                    summary = "Clear all app data (requires restart)",
                    onClick = {
                        restartAction = {
                            mainActivity.dropDatabase()
                            mainActivity.recreate()
                        }
                        showRestartDialog = true
                    }
                )
            }

            // Restart App
            item {
                PreferenceItem(
                    title = "Restart App",
                    summary = "Restart the application",
                    onClick = {
                        mainActivity.recreate()
                    }
                )
            }
        }
    }

    // Dark Mode Dialog
    if (showDarkModeDialog) {
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text("Select Dark Mode") },
            text = {
                Column {
                    darkModeEntries.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = darkModeValues[index] == currentDarkMode,
                                onClick = {
                                    val newValue = darkModeValues[index]
                                    prefs.edit().putString(PreferenceKeys.DARK_MODE, newValue).apply()
                                    when (newValue) {
                                        darkModeValues[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                        darkModeValues[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                        darkModeValues[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                        darkModeValues[3] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                                    }
                                    showDarkModeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(entry)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDarkModeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
