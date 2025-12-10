package com.ingokodba.dragnav.compose

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ingokodba.dragnav.MainActivityCompose

/**
 * Help & About Settings Screen - Compose version of MyHelpSettingsFragment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSettingsScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & About") },
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
            // Backup Database
            item {
                PreferenceItem(
                    title = "Backup Database",
                    summary = "Save a backup of your app data",
                    onClick = {
                        mainActivity.to_backup()
                        Toast.makeText(context, "Backup successful", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Restore from Backup
            item {
                PreferenceItem(
                    title = "Restore from Backup",
                    summary = "Restore app data from backup",
                    onClick = {
                        mainActivity.from_backup()
                        Toast.makeText(context, "Backup restored", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Reset Database
            item {
                var showConfirmDialog by remember { mutableStateOf(false) }

                PreferenceItem(
                    title = "Reset Database",
                    summary = "Clear all app data (requires restart)",
                    onClick = { showConfirmDialog = true }
                )

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("Reset Database?") },
                        text = { Text("This will delete all your app data and restart the app. This action cannot be undone.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showConfirmDialog = false
                                mainActivity.dropDatabase()
                                mainActivity.recreate()
                            }) {
                                Text("Reset")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // Send Feedback
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

            // App Version (placeholder)
            item {
                PreferenceItem(
                    title = "App Version",
                    summary = "1.0.0", // TODO: Get from BuildConfig
                    onClick = { }
                )
            }
        }
    }
}
