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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.dragnav.R
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
    val backupSuccessful = stringResource(R.string.backup_successful)
    val backupRestored = stringResource(R.string.backup_restored)
    val prefsBackupSuccessful = stringResource(R.string.preferences_backup_successful)
    val prefsBackupRestored = stringResource(R.string.preferences_backup_restored)
    val feedbackSubject = stringResource(R.string.feedback_email_subject)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_and_about)) },
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
            // Backup Database
            item {
                PreferenceItem(
                    title = stringResource(R.string.backup_database),
                    summary = stringResource(R.string.backup_database_summary),
                    onClick = {
                        mainActivity.to_backup()
                        Toast.makeText(context, backupSuccessful, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Restore from Backup
            item {
                PreferenceItem(
                    title = stringResource(R.string.restore_backup),
                    summary = stringResource(R.string.restore_backup_summary),
                    onClick = {
                        mainActivity.from_backup()
                        Toast.makeText(context, backupRestored, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Backup Preferences
            item {
                PreferenceItem(
                    title = stringResource(R.string.backup_preferences),
                    summary = stringResource(R.string.backup_preferences_summary),
                    onClick = {
                        mainActivity.to_backup_prefs()
                        Toast.makeText(context, prefsBackupSuccessful, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Restore Preferences
            item {
                PreferenceItem(
                    title = stringResource(R.string.restore_preferences),
                    summary = stringResource(R.string.restore_preferences_summary),
                    onClick = {
                        mainActivity.from_backup_prefs()
                        Toast.makeText(context, prefsBackupRestored, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Reset Database
            item {
                var showConfirmDialog by remember { mutableStateOf(false) }

                PreferenceItem(
                    title = stringResource(R.string.reset_database),
                    summary = stringResource(R.string.reset_database_summary),
                    onClick = { showConfirmDialog = true }
                )

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text(stringResource(R.string.reset_database_q)) },
                        text = { Text(stringResource(R.string.reset_database_confirm)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showConfirmDialog = false
                                mainActivity.dropDatabase()
                                mainActivity.recreate()
                            }) {
                                Text(stringResource(R.string.reset))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            // Send Feedback
            item {
                PreferenceItem(
                    title = stringResource(R.string.send_feedback),
                    summary = stringResource(R.string.feedback_summary),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("ingokodba@gamil.com"))
                            putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
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
                    title = stringResource(R.string.restart_app),
                    summary = stringResource(R.string.restart_app_summary),
                    onClick = {
                        mainActivity.recreate()
                    }
                )
            }

            // App Version (placeholder)
            item {
                PreferenceItem(
                    title = stringResource(R.string.app_version),
                    summary = "1.0.0", // TODO: Get from BuildConfig
                    onClick = { }
                )
            }
        }
    }
}
