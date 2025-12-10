package com.ingokodba.dragnav.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.PreferenceKeys

/**
 * Circle Settings Screen - Compose version of CircleSettingsFragment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleSettingsScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var showBorderWidthDialog by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showSmallerTextSizeDialog by remember { mutableStateOf(false) }
    var showTransparencyDialog by remember { mutableStateOf(false) }

    val borderWidth = prefs.getString(PreferenceKeys.UI_BORDER_WIDTH, "4") ?: "4"
    val textSize = prefs.getString(PreferenceKeys.UI_TEXT_SIZE, "30") ?: "30"
    val smallerTextSize = prefs.getString(PreferenceKeys.UI_SMALLER_TEXT_SIZE, "30") ?: "30"
    val transparency = prefs.getString(PreferenceKeys.UI_TRANSPARENCY, "1.0") ?: "1.0"
    val showIcons = prefs.getBoolean(PreferenceKeys.UI_ICONS_TOGGLE, true)
    val showCircles = prefs.getBoolean(PreferenceKeys.UI_CIRCLES_TOGGLE, true)
    val showShadow = prefs.getBoolean(PreferenceKeys.UI_SHADOW_TOGGLE, true)
    val showAppNames = prefs.getBoolean(PreferenceKeys.UI_SHOW_APP_NAMES, true)
    val showBigCircle = prefs.getBoolean(PreferenceKeys.UI_BIG_CIRCLE, true)
    val colorOnPrimary = prefs.getBoolean(PreferenceKeys.UI_COLOR_ON_PRIMARY, false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Circle Settings") },
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
            // Color Picker
            item {
                PreferenceItem(
                    title = "Primary Color",
                    summary = "Choose app color theme",
                    onClick = {
                        mainActivity.startColorpicker()
                    }
                )
            }

            // Show Icons Toggle
            item {
                SwitchPreferenceItem(
                    title = "Show Icons",
                    checked = showIcons,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_ICONS_TOGGLE, checked).apply()
                        if (checked) {
                            // Trigger icon reload
                            mainActivity.recreate()
                        }
                    }
                )
            }

            // Show Circles Toggle
            item {
                SwitchPreferenceItem(
                    title = "Show Circles",
                    checked = showCircles,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_CIRCLES_TOGGLE, checked).apply()
                    }
                )
            }

            // Show Shadow Toggle
            item {
                SwitchPreferenceItem(
                    title = "Show Shadow",
                    checked = showShadow,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_SHADOW_TOGGLE, checked).apply()
                    }
                )
            }

            // Show App Names Toggle
            item {
                SwitchPreferenceItem(
                    title = "Show App Names",
                    checked = showAppNames,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_SHOW_APP_NAMES, checked).apply()
                    }
                )
            }

            // Show Big Circle Toggle
            item {
                SwitchPreferenceItem(
                    title = "Show Big Circle",
                    checked = showBigCircle,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_BIG_CIRCLE, checked).apply()
                    }
                )
            }

            // Color on Primary Toggle
            item {
                SwitchPreferenceItem(
                    title = "Color on Primary",
                    checked = colorOnPrimary,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_COLOR_ON_PRIMARY, checked).apply()
                    }
                )
            }

            // Border Width
            item {
                PreferenceItem(
                    title = "Border Width",
                    summary = borderWidth,
                    onClick = { showBorderWidthDialog = true }
                )
            }

            // Text Size
            item {
                PreferenceItem(
                    title = "Text Size",
                    summary = textSize,
                    onClick = { showTextSizeDialog = true }
                )
            }

            // Smaller Text Size
            item {
                PreferenceItem(
                    title = "Smaller Text Size",
                    summary = smallerTextSize,
                    onClick = { showSmallerTextSizeDialog = true }
                )
            }

            // Transparency
            item {
                PreferenceItem(
                    title = "Transparency",
                    summary = "$transparency (0.0 - 1.0)",
                    onClick = { showTransparencyDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showBorderWidthDialog) {
        NumberInputDialog(
            title = "Border Width",
            currentValue = borderWidth,
            onDismiss = { showBorderWidthDialog = false },
            onConfirm = { value ->
                prefs.edit().putString(PreferenceKeys.UI_BORDER_WIDTH, value).apply()
                showBorderWidthDialog = false
            },
            isDecimal = false
        )
    }

    if (showTextSizeDialog) {
        NumberInputDialog(
            title = "Text Size",
            currentValue = textSize,
            onDismiss = { showTextSizeDialog = false },
            onConfirm = { value ->
                prefs.edit().putString(PreferenceKeys.UI_TEXT_SIZE, value).apply()
                showTextSizeDialog = false
            },
            isDecimal = false
        )
    }

    if (showSmallerTextSizeDialog) {
        NumberInputDialog(
            title = "Smaller Text Size",
            currentValue = smallerTextSize,
            onDismiss = { showSmallerTextSizeDialog = false },
            onConfirm = { value ->
                prefs.edit().putString(PreferenceKeys.UI_SMALLER_TEXT_SIZE, value).apply()
                showSmallerTextSizeDialog = false
            },
            isDecimal = false
        )
    }

    if (showTransparencyDialog) {
        NumberInputDialog(
            title = "Transparency",
            currentValue = transparency,
            onDismiss = { showTransparencyDialog = false },
            onConfirm = { value ->
                prefs.edit().putString(PreferenceKeys.UI_TRANSPARENCY, value).apply()
                showTransparencyDialog = false
            },
            isDecimal = true
        )
    }
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
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
