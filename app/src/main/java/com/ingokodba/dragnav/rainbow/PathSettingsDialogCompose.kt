package com.ingokodba.dragnav.rainbow

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dragnav.R

/**
 * Jetpack Compose version of PathSettingsDialog
 * Floating settings dialog with categorized options
 */
@Composable
fun PathSettingsDialogCompose(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit,
    onDismiss: () -> Unit,
    onCategoryChanged: ((PathSettingsDialog.Category) -> Unit)? = null,
    showAsFullScreenOverlay: Boolean = false
) {
    var currentCategory by remember { mutableStateOf(PathSettingsDialog.Category.PATH) }
    var currentConfig by remember { mutableStateOf(config) }
    var isTransparent by remember { mutableStateOf(false) }

    // Notify category changes
    LaunchedEffect(currentCategory) {
        onCategoryChanged?.invoke(currentCategory)
    }

    DisposableEffect(Unit) {
        onDispose {
            onCategoryChanged?.invoke(PathSettingsDialog.Category.PATH)
        }
    }

    val dialogContent = @Composable {
        Surface(
            modifier = Modifier
                .then(
                    if (showAsFullScreenOverlay) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth(0.9f)
                            .wrapContentHeight()
                    }
                ),
            shape = if (showAsFullScreenOverlay) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp),
            color = if (isTransparent) {
                Color(0x1A000000) // 10% opacity when transparent
            } else {
                Color(0xE6222222) // 90% opacity dark background
            },
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Top button row: Visibility button and Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visibility toggle button
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isTransparent = true
                                        tryAwaitRelease()
                                        isTransparent = false
                                    }
                                )
                            }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.visibility),
                            contentDescription = "Toggle Visibility",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Close button
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Title
                Text(
                    text = stringResource(R.string.rainbow_preferences),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Tab layout
                ScrollableTabRow(
                    selectedTabIndex = currentCategory.ordinal,
                    containerColor = Color(0x33FFFFFF),
                    contentColor = Color.White,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PathSettingsDialog.Category.values().forEach { category ->
                        Tab(
                            selected = category == currentCategory,
                            onClick = { currentCategory = category },
                            text = {
                                Text(
                                    text = category.title,
                                    color = if (category == currentCategory) Color.White else Color.Gray
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 400.dp, max = 600.dp)
                ) {
                    when (currentCategory) {
                        PathSettingsDialog.Category.PATH -> PathSettings(
                            config = currentConfig,
                            onConfigChanged = {
                                currentConfig = it
                                onConfigChanged(it)
                            }
                        )
                        PathSettingsDialog.Category.APPS -> AppsSettings(
                            config = currentConfig,
                            onConfigChanged = {
                                currentConfig = it
                                onConfigChanged(it)
                            }
                        )
                        PathSettingsDialog.Category.APP_NAMES -> AppNamesSettings(
                            config = currentConfig,
                            onConfigChanged = {
                                currentConfig = it
                                onConfigChanged(it)
                            }
                        )
                        PathSettingsDialog.Category.FAVORITES -> FavoritesSettings(
                            config = currentConfig,
                            onConfigChanged = {
                                currentConfig = it
                                onConfigChanged(it)
                            }
                        )
                        PathSettingsDialog.Category.SEARCH -> SearchSettings(
                            config = currentConfig,
                            onConfigChanged = {
                                currentConfig = it
                                onConfigChanged(it)
                            }
                        )
                        PathSettingsDialog.Category.LETTERS -> LettersSettings(
                            config = currentConfig,
                            onConfigChanged = {
                                currentConfig = it
                                onConfigChanged(it)
                            }
                        )
                        PathSettingsDialog.Category.NOTIFICATIONS -> NotificationsSettings(
                            config = currentConfig,
                            onConfigChanged = {
                                currentConfig = it
                                onConfigChanged(it)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAsFullScreenOverlay) {
        // Full screen overlay version
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            dialogContent()
        }
    } else {
        // Dialog version
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            dialogContent()
        }
    }
}

@Composable
private fun PathSettings(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Path shape selector
        SettingsLabel("Path Shape")
        PathShapeSpinner(
            selected = config.pathShape,
            onSelected = { onConfigChanged(config.copy(pathShape = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Start point
        SettingsLabel("Start Point")
        PointSliders(
            point = config.startPoint,
            onPointChanged = { onConfigChanged(config.copy(startPoint = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // End point
        SettingsLabel("End Point")
        PointSliders(
            point = config.endPoint,
            onPointChanged = { onConfigChanged(config.copy(endPoint = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Curve intensity
        SettingsLabel("Curve Intensity")
        SettingsSlider(
            value = config.curveIntensity,
            valueRange = -1f..1f,
            onValueChange = { onConfigChanged(config.copy(curveIntensity = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Polygon segments
        SettingsLabel("Polygon Segments")
        SettingsSlider(
            value = config.polygonSegments.toFloat(),
            valueRange = 2f..20f,
            steps = 17,
            onValueChange = { onConfigChanged(config.copy(polygonSegments = it.toInt())) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Path line color
        SettingsLabel("Path Line Color (Hue)")
        SettingsSubtext("0 = Red, 120 = Green, 240 = Blue, 360 = Red")
        SettingsSlider(
            value = config.pathHue,
            valueRange = 0f..360f,
            onValueChange = { onConfigChanged(config.copy(pathHue = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Path line transparency
        SettingsLabel("Path Line Transparency")
        SettingsSubtext("0 = Fully transparent, 255 = Fully opaque")
        SettingsSlider(
            value = config.pathAlpha,
            valueRange = 0f..255f,
            onValueChange = { onConfigChanged(config.copy(pathAlpha = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Path line width
        SettingsLabel("Path Line Width")
        SettingsSubtext("Line width in pixels")
        SettingsSlider(
            value = config.pathWidth,
            valueRange = 0.5f..20f,
            onValueChange = { onConfigChanged(config.copy(pathWidth = it)) }
        )
    }
}

@Composable
private fun AppsSettings(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Sort order
        SettingsLabel("Sort Order")
        SortOrderSpinner(
            selected = config.appSortOrder,
            onSelected = { onConfigChanged(config.copy(appSortOrder = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Icon size
        SettingsLabel("Icon Size")
        SettingsSlider(
            value = config.appIconSize,
            valueRange = 0.03f..0.5f,
            onValueChange = { onConfigChanged(config.copy(appIconSize = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App spacing
        SettingsLabel("App Spacing")
        SettingsSlider(
            value = config.appSpacing,
            valueRange = 0.02f..0.15f,
            onValueChange = { onConfigChanged(config.copy(appSpacing = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scroll sensitivity
        SettingsLabel("Scroll Sensitivity")
        SettingsSubtext("Higher values = faster scrolling (e.g., 10 = 1cm touch scrolls 10cm)")
        SettingsSlider(
            value = config.scrollSensitivity,
            valueRange = 0.5f..20f,
            onValueChange = { onConfigChanged(config.copy(scrollSensitivity = it)) }
        )
    }
}

@Composable
private fun AppNamesSettings(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Show app names toggle
        SettingsCheckbox(
            text = "Show App Names",
            checked = config.showAppNames,
            onCheckedChange = { onConfigChanged(config.copy(showAppNames = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name position
        SettingsLabel("Name Position (offset from icon center)")

        SettingsSubtext("Horizontal Offset (-1 = left, 1 = right)")
        SettingsSlider(
            value = config.appNameOffsetX,
            valueRange = -1f..1f,
            onValueChange = { onConfigChanged(config.copy(appNameOffsetX = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSubtext("Vertical Offset (-1 = below, 1 = above)")
        SettingsSlider(
            value = config.appNameOffsetY,
            valueRange = -1f..1f,
            onValueChange = { onConfigChanged(config.copy(appNameOffsetY = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Text size
        SettingsLabel("Text Size")
        SettingsSlider(
            value = config.appNameSize,
            valueRange = 8f..24f,
            onValueChange = { onConfigChanged(config.copy(appNameSize = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Style
        SettingsLabel("Text Style")
        AppNameStyleSpinner(
            selected = config.appNameStyle,
            onSelected = { onConfigChanged(config.copy(appNameStyle = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Border width
        SettingsLabel("Border Width (for bordered style)")
        SettingsSlider(
            value = config.appNameBorderWidth,
            valueRange = 1f..8f,
            onValueChange = { onConfigChanged(config.copy(appNameBorderWidth = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Font
        SettingsLabel("Font")
        AppNameFontSpinner(
            selected = config.appNameFont,
            onSelected = { onConfigChanged(config.copy(appNameFont = it)) }
        )
    }
}

@Composable
private fun FavoritesSettings(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Button position
        SettingsLabel("Button Position")
        PointSliders(
            point = config.favButtonPosition,
            onPointChanged = { onConfigChanged(config.copy(favButtonPosition = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button size
        SettingsLabel("Button Size")
        SettingsSlider(
            value = config.favButtonSize,
            valueRange = 0.05f..0.5f,
            onValueChange = { onConfigChanged(config.copy(favButtonSize = it)) }
        )
    }
}

@Composable
private fun SearchSettings(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Button position
        SettingsLabel("Button Position")
        PointSliders(
            point = config.searchButtonPosition,
            onPointChanged = { onConfigChanged(config.copy(searchButtonPosition = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button size
        SettingsLabel("Button Size")
        SettingsSlider(
            value = config.searchButtonSize,
            valueRange = 0.05f..0.5f,
            onValueChange = { onConfigChanged(config.copy(searchButtonSize = it)) }
        )
    }
}

@Composable
private fun LettersSettings(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Enable letter index
        SettingsCheckbox(
            text = "Enable Letter Index",
            checked = config.letterIndexEnabled,
            onCheckedChange = { onConfigChanged(config.copy(letterIndexEnabled = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Position
        SettingsLabel("Position")
        LetterPositionSpinner(
            selected = config.letterIndexPosition,
            onSelected = { onConfigChanged(config.copy(letterIndexPosition = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Size
        SettingsLabel("Letter Size")
        SettingsSlider(
            value = config.letterIndexSize,
            valueRange = 0.02f..0.08f,
            onValueChange = { onConfigChanged(config.copy(letterIndexSize = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Padding from edge
        SettingsLabel("Padding from Edge")
        SettingsSlider(
            value = config.letterIndexPadding,
            valueRange = 0f..0.1f,
            onValueChange = { onConfigChanged(config.copy(letterIndexPadding = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pan from letters
        SettingsLabel("Pan from Letters")
        SettingsSubtext("Clickable area padding from letters toward edge")
        SettingsSlider(
            value = config.letterIndexPanFromLetters,
            valueRange = 0f..0.3f,
            onValueChange = { onConfigChanged(config.copy(letterIndexPanFromLetters = it)) }
        )
    }
}

@Composable
private fun NotificationsSettings(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Anchor position
        SettingsLabel("Anchor Position")
        NotificationAnchorSpinner(
            selected = config.notificationAnchor,
            onSelected = { onConfigChanged(config.copy(notificationAnchor = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Icon size
        SettingsLabel("Icon Size (dp)")
        SettingsSlider(
            value = config.notificationIconSize,
            valueRange = 24f..96f,
            onValueChange = { onConfigChanged(config.copy(notificationIconSize = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Icon spacing
        SettingsLabel("Icon Spacing (dp)")
        SettingsSlider(
            value = config.notificationIconSpacing,
            valueRange = 0f..32f,
            onValueChange = { onConfigChanged(config.copy(notificationIconSpacing = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Offset X
        SettingsLabel("Horizontal Offset (dp)")
        SettingsSubtext("Offset from center (negative = left, positive = right)")
        SettingsSlider(
            value = config.notificationOffsetX,
            valueRange = -200f..200f,
            onValueChange = { onConfigChanged(config.copy(notificationOffsetX = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Offset Y
        SettingsLabel("Vertical Offset (dp)")
        SettingsSubtext("Offset from anchor (negative = toward center, positive = toward edge)")
        SettingsSlider(
            value = config.notificationOffsetY,
            valueRange = -200f..200f,
            onValueChange = { onConfigChanged(config.copy(notificationOffsetY = it)) }
        )
    }
}

// Helper composables

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsSubtext(text: String) {
    Text(
        text = text,
        color = Color.LightGray,
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Slider(
        value = value.coerceIn(valueRange),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.Gray
        )
    )
}

@Composable
private fun PointSliders(
    point: PointF,
    onPointChanged: (PointF) -> Unit
) {
    Column {
        SettingsSubtext("X (left → right)")
        SettingsSlider(
            value = point.x,
            valueRange = 0f..1f,
            onValueChange = { onPointChanged(PointF(it, point.y)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSubtext("Y (bottom → top)")
        SettingsSlider(
            value = point.y,
            valueRange = 0f..1f,
            onValueChange = { onPointChanged(PointF(point.x, it)) }
        )
    }
}

@Composable
private fun SettingsCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.Black
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PathShapeSpinner(
    selected: PathShape,
    onSelected: (PathShape) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x33FFFFFF),
                unfocusedContainerColor = Color(0x33FFFFFF)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PathShape.values().forEach { shape ->
                DropdownMenuItem(
                    text = { Text(shape.name.replace("_", " ")) },
                    onClick = {
                        onSelected(shape)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortOrderSpinner(
    selected: AppSortOrder,
    onSelected: (AppSortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = when (selected) {
                AppSortOrder.ASCENDING -> "A to Z"
                AppSortOrder.DESCENDING -> "Z to A"
            },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x33FFFFFF),
                unfocusedContainerColor = Color(0x33FFFFFF)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppSortOrder.values().forEach { order ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (order) {
                                AppSortOrder.ASCENDING -> "A to Z"
                                AppSortOrder.DESCENDING -> "Z to A"
                            }
                        )
                    },
                    onClick = {
                        onSelected(order)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNameStyleSpinner(
    selected: AppNameStyle,
    onSelected: (AppNameStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x33FFFFFF),
                unfocusedContainerColor = Color(0x33FFFFFF)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppNameStyle.values().forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.name.replace("_", " ")) },
                    onClick = {
                        onSelected(style)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNameFontSpinner(
    selected: AppNameFont,
    onSelected: (AppNameFont) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x33FFFFFF),
                unfocusedContainerColor = Color(0x33FFFFFF)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppNameFont.values().forEach { font ->
                DropdownMenuItem(
                    text = { Text(font.name.replace("_", " ")) },
                    onClick = {
                        onSelected(font)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LetterPositionSpinner(
    selected: LetterIndexPosition,
    onSelected: (LetterIndexPosition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x33FFFFFF),
                unfocusedContainerColor = Color(0x33FFFFFF)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LetterIndexPosition.values().forEach { position ->
                DropdownMenuItem(
                    text = { Text(position.name) },
                    onClick = {
                        onSelected(position)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationAnchorSpinner(
    selected: NotificationAnchor,
    onSelected: (NotificationAnchor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x33FFFFFF),
                unfocusedContainerColor = Color(0x33FFFFFF)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NotificationAnchor.values().forEach { anchor ->
                DropdownMenuItem(
                    text = { Text(anchor.name.replace("_", " ")) },
                    onClick = {
                        onSelected(anchor)
                        expanded = false
                    }
                )
            }
        }
    }
}
