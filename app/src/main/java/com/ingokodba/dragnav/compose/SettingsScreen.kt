package com.ingokodba.dragnav.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.dragnav.R
import com.ingokodba.dragnav.MainActivityCompose

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
 * Switch Preference Item
 */
@Composable
fun SwitchPreferenceItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    PreferenceItem(
        title = title,
        summary = summary,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

/**
 * Number Input Dialog for numeric preferences
 */
@Composable
fun NumberInputDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isDecimal: Boolean = false
) {
    var textValue by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
                ),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textValue) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
