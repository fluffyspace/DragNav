package com.ingokodba.dragnav.compose

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
 * Rainbow Settings Screen - Compose version of RainbowSettingsFragment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainbowSettingsScreen(
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
    val showCircles = prefs.getBoolean(PreferenceKeys.UI_CIRCLES_TOGGLE, true)
    val showShadow = prefs.getBoolean(PreferenceKeys.UI_SHADOW_TOGGLE, true)
    val showAppNames = prefs.getBoolean(PreferenceKeys.UI_SHOW_APP_NAMES, true)
    val showBigCircle = prefs.getBoolean(PreferenceKeys.UI_BIG_CIRCLE, true)
    val colorOnPrimary = prefs.getBoolean(PreferenceKeys.UI_COLOR_ON_PRIMARY, false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rainbow_settings)) },
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
            // Color Picker
            item {
                PreferenceItem(
                    title = stringResource(R.string.primary_color),
                    summary = stringResource(R.string.choose_color_theme),
                    onClick = {
                        mainActivity.startColorpicker()
                    }
                )
            }

            // Show Circles Toggle
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.show_circles),
                    checked = showCircles,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_CIRCLES_TOGGLE, checked).apply()
                    }
                )
            }

            // Show Shadow Toggle
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.shadow),
                    checked = showShadow,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_SHADOW_TOGGLE, checked).apply()
                    }
                )
            }

            // Show App Names Toggle
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.ui_show_app_names),
                    checked = showAppNames,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_SHOW_APP_NAMES, checked).apply()
                    }
                )
            }

            // Show Big Circle Toggle
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.show_big_circle),
                    checked = showBigCircle,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_BIG_CIRCLE, checked).apply()
                    }
                )
            }

            // Color on Primary Toggle
            item {
                SwitchPreferenceItem(
                    title = stringResource(R.string.color_on_primary),
                    checked = colorOnPrimary,
                    onCheckedChange = { checked ->
                        prefs.edit().putBoolean(PreferenceKeys.UI_COLOR_ON_PRIMARY, checked).apply()
                    }
                )
            }

            // Border Width
            item {
                PreferenceItem(
                    title = stringResource(R.string.debljina_ruba),
                    summary = borderWidth,
                    onClick = { showBorderWidthDialog = true }
                )
            }

            // Text Size
            item {
                PreferenceItem(
                    title = stringResource(R.string.text_size),
                    summary = textSize,
                    onClick = { showTextSizeDialog = true }
                )
            }

            // Smaller Text Size
            item {
                PreferenceItem(
                    title = stringResource(R.string.smaller_text_size),
                    summary = smallerTextSize,
                    onClick = { showSmallerTextSizeDialog = true }
                )
            }

            // Transparency
            item {
                PreferenceItem(
                    title = stringResource(R.string.transparency),
                    summary = "$transparency (0.0 - 1.0)",
                    onClick = { showTransparencyDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showBorderWidthDialog) {
        NumberInputDialog(
            title = stringResource(R.string.debljina_ruba),
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
            title = stringResource(R.string.text_size),
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
            title = stringResource(R.string.smaller_text_size),
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
            title = stringResource(R.string.transparency),
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
