package com.ingokodba.dragnav.rainbow

import android.graphics.PointF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import android.view.Gravity
import android.view.WindowManager
import com.example.dragnav.R

enum class PathSettingsCategory(val title: String) {
    PATH("Path"),
    APPS("Apps"),
    APP_NAMES("App Names"),
    FAVORITES("Favorites"),
    SEARCH("Search"),
    LETTERS("Letters"),
    NOTIFICATIONS("Notifications")
}

private val PanelColor = Color(0xA6101216)      // ~65% opaque: launcher stays visible underneath
private val CardColor = Color(0x1FFFFFFF)
private val AccentColor = Color(0xFF7CC4FF)

@Composable
fun PathSettingsDialogCompose(
    config: PathConfig,
    onConfigChanged: (PathConfig) -> Unit,
    onDismiss: () -> Unit,
    onCategoryChanged: ((PathSettingsCategory) -> Unit)? = null,
    showAsFullScreenOverlay: Boolean = false
) {
    var currentCategory by remember { mutableStateOf(PathSettingsCategory.PATH) }
    var currentConfig by remember { mutableStateOf(config) }
    var isTransparent by remember { mutableStateOf(false) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val update: (PathConfig) -> Unit = {
        currentConfig = it
        onConfigChanged(it)
    }

    LaunchedEffect(currentCategory) {
        onCategoryChanged?.invoke(currentCategory)
    }

    DisposableEffect(Unit) {
        onDispose {
            onCategoryChanged?.invoke(PathSettingsCategory.PATH)
        }
    }

    val dialogContent = @Composable {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (isTransparent) 0.08f else 1f },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = PanelColor,
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                // Drag-handle style grip
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .background(Color(0x66FFFFFF), RoundedCornerShape(2.dp))
                )

                // Header: title, hold-to-peek, close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.rainbow_preferences),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isTransparent = true
                                        tryAwaitRelease()
                                        isTransparent = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.visibility),
                            contentDescription = stringResource(R.string.toggle_visibility),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White
                        )
                    }
                }

                // Category chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PathSettingsCategory.values().forEach { category ->
                        val selected = category == currentCategory
                        FilterChip(
                            selected = selected,
                            onClick = { currentCategory = category },
                            label = { Text(category.title) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = Color(0xCCFFFFFF),
                                selectedContainerColor = AccentColor.copy(alpha = 0.28f),
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = Color(0x44FFFFFF),
                                selectedBorderColor = AccentColor
                            )
                        )
                    }
                }

                // Content: capped so at least the top ~40% of the launcher stays visible
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeight * 0.5f)
                ) {
                    when (currentCategory) {
                        PathSettingsCategory.PATH -> PathSettings(currentConfig, update)
                        PathSettingsCategory.APPS -> AppsSettings(currentConfig, update)
                        PathSettingsCategory.APP_NAMES -> AppNamesSettings(currentConfig, update)
                        PathSettingsCategory.FAVORITES -> FavoritesSettings(currentConfig, update)
                        PathSettingsCategory.SEARCH -> SearchSettings(currentConfig, update)
                        PathSettingsCategory.LETTERS -> LettersSettings(currentConfig, update)
                        PathSettingsCategory.NOTIFICATIONS -> NotificationsSettings(currentConfig, update)
                    }
                }
            }
        }
    }

    if (showAsFullScreenOverlay) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            dialogContent()
        }
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            // No dim, bottom-anchored, so the launcher above stays fully visible and live
            (LocalView.current.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(0f)
                setGravity(Gravity.BOTTOM)
                setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                )
            }
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

        SettingsDivider()

        // Start point
        SettingsLabel("Start Point")
        PointSliders(
            point = config.startPoint,
            onPointChanged = { onConfigChanged(config.copy(startPoint = it)) }
        )

        SettingsDivider()

        // End point
        SettingsLabel("End Point")
        PointSliders(
            point = config.endPoint,
            onPointChanged = { onConfigChanged(config.copy(endPoint = it)) }
        )

        SettingsDivider()

        // Curve intensity
        SettingsLabel("Curve Intensity")
        SettingsSlider(
            value = config.curveIntensity,
            valueRange = -1f..1f,
            onValueChange = { onConfigChanged(config.copy(curveIntensity = it)) }
        )

        SettingsDivider()

        // Icon spacing along the path
        SettingsLabel("Spread Position (bottom to top)")
        SettingsSlider(
            value = config.spacingFocus,
            valueRange = 0f..1f,
            onValueChange = { onConfigChanged(config.copy(spacingFocus = it)) }
        )

        SettingsDivider()

        SettingsLabel("Spread Amount (negative squeezes)")
        SettingsSlider(
            value = config.spacingStrength,
            valueRange = -0.9f..3f,
            onValueChange = { onConfigChanged(config.copy(spacingStrength = it)) }
        )

        SettingsDivider()

        // Polygon segments
        SettingsLabel("Polygon Segments")
        SettingsSlider(
            value = config.polygonSegments.toFloat(),
            valueRange = 2f..20f,
            steps = 17,
            onValueChange = { onConfigChanged(config.copy(polygonSegments = it.toInt())) }
        )

        SettingsDivider()

        // Path line color
        SettingsLabel("Path Line Color (Hue)")
        SettingsSubtext("0 = Red, 120 = Green, 240 = Blue, 360 = Red")
        SettingsSlider(
            value = config.pathHue,
            valueRange = 0f..360f,
            onValueChange = { onConfigChanged(config.copy(pathHue = it)) }
        )

        SettingsDivider()

        // Path line transparency
        SettingsLabel("Path Line Transparency")
        SettingsSubtext("0 = Fully transparent, 255 = Fully opaque")
        SettingsSlider(
            value = config.pathAlpha,
            valueRange = 0f..255f,
            onValueChange = { onConfigChanged(config.copy(pathAlpha = it)) }
        )

        SettingsDivider()

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

        SettingsDivider()

        // Icon size
        SettingsLabel("Icon Size")
        SettingsSlider(
            value = config.appIconSize,
            valueRange = 0.03f..0.5f,
            onValueChange = { onConfigChanged(config.copy(appIconSize = it)) }
        )

        SettingsDivider()

        // App spacing
        SettingsLabel("App Spacing")
        SettingsSlider(
            value = config.appSpacing,
            valueRange = 0.02f..0.15f,
            onValueChange = { onConfigChanged(config.copy(appSpacing = it)) }
        )

        SettingsDivider()

        // Scroll sensitivity
        SettingsLabel("Scroll Sensitivity")
        SettingsSubtext("Higher values = faster scrolling (e.g., 10 = 1cm touch scrolls 10cm)")
        SettingsSlider(
            value = config.scrollSensitivity,
            valueRange = 0.5f..20f,
            onValueChange = { onConfigChanged(config.copy(scrollSensitivity = it)) }
        )

        SettingsDivider()

        // Elastic physics
        SettingsLabel("Elastic Physics")
        SettingsSubtext("Icons trail behind the scroll as if tied together with elastic string, and stretch or squash with speed")
        SettingsCheckbox(
            text = "Enable elastic physics",
            checked = config.physicsEnabled,
            onCheckedChange = { onConfigChanged(config.copy(physicsEnabled = it)) }
        )
        if (config.physicsEnabled) {
            SettingsLabel("Stiffness")
            SettingsSlider(
                value = config.physicsStiffness,
                valueRange = 0f..1f,
                onValueChange = { onConfigChanged(config.copy(physicsStiffness = it)) }
            )
            SettingsLabel("Damping (low = wobbly)")
            SettingsSlider(
                value = config.physicsDamping,
                valueRange = 0f..1f,
                onValueChange = { onConfigChanged(config.copy(physicsDamping = it)) }
            )
            SettingsLabel("Neighbour Coupling")
            SettingsSlider(
                value = config.physicsCoupling,
                valueRange = 0f..1f,
                onValueChange = { onConfigChanged(config.copy(physicsCoupling = it)) }
            )
            SettingsLabel("Stretch & Squash")
            SettingsSlider(
                value = config.physicsStretch,
                valueRange = 0f..1f,
                onValueChange = { onConfigChanged(config.copy(physicsStretch = it)) }
            )
        }
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

        SettingsDivider()

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

        SettingsDivider()

        // Text size
        SettingsLabel("Text Size")
        SettingsSlider(
            value = config.appNameSize,
            valueRange = 8f..24f,
            onValueChange = { onConfigChanged(config.copy(appNameSize = it)) }
        )

        SettingsDivider()

        // Alignment
        SettingsLabel("Text Alignment")
        AppNameAlignmentSpinner(
            selected = config.appNameAlignment,
            onSelected = { onConfigChanged(config.copy(appNameAlignment = it)) }
        )

        SettingsDivider()

        // Style
        SettingsLabel("Text Style")
        AppNameStyleSpinner(
            selected = config.appNameStyle,
            onSelected = { onConfigChanged(config.copy(appNameStyle = it)) }
        )

        SettingsDivider()

        // Border width
        SettingsLabel("Border Width (for bordered style)")
        SettingsSlider(
            value = config.appNameBorderWidth,
            valueRange = 1f..8f,
            onValueChange = { onConfigChanged(config.copy(appNameBorderWidth = it)) }
        )

        SettingsDivider()

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

        SettingsDivider()

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

        SettingsDivider()

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

        SettingsDivider()

        // Position
        SettingsLabel("Position")
        LetterPositionSpinner(
            selected = config.letterIndexPosition,
            onSelected = { onConfigChanged(config.copy(letterIndexPosition = it)) }
        )

        SettingsDivider()

        // Size
        SettingsLabel("Letter Size")
        SettingsSlider(
            value = config.letterIndexSize,
            valueRange = 0.02f..0.08f,
            onValueChange = { onConfigChanged(config.copy(letterIndexSize = it)) }
        )

        SettingsDivider()

        // Padding from edge
        SettingsLabel("Padding from Edge")
        SettingsSlider(
            value = config.letterIndexPadding,
            valueRange = 0f..0.1f,
            onValueChange = { onConfigChanged(config.copy(letterIndexPadding = it)) }
        )

        SettingsDivider()

        // Pan from letters
        SettingsLabel("Pan from Letters")
        SettingsSubtext("Clickable area padding from letters toward edge")
        SettingsSlider(
            value = config.letterIndexPanFromLetters,
            valueRange = 0f..0.3f,
            onValueChange = { onConfigChanged(config.copy(letterIndexPanFromLetters = it)) }
        )

        SettingsDivider()

        // Letter index height
        SettingsLabel("Letters View Height")
        SettingsSubtext("Height as fraction of screen (shrinks from top)")
        SettingsSlider(
            value = config.letterIndexHeight,
            valueRange = 0.2f..1f,
            onValueChange = { onConfigChanged(config.copy(letterIndexHeight = it)) }
        )

        SettingsDivider()

        // Letter index bottom offset
        SettingsLabel("Bottom Offset")
        SettingsSubtext("Offset from bottom of screen")
        SettingsSlider(
            value = config.letterIndexBottomOffset,
            valueRange = 0f..0.5f,
            onValueChange = { onConfigChanged(config.copy(letterIndexBottomOffset = it)) }
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

        SettingsDivider()

        // Icon size
        SettingsLabel("Icon Size (dp)")
        SettingsSlider(
            value = config.notificationIconSize,
            valueRange = 24f..96f,
            onValueChange = { onConfigChanged(config.copy(notificationIconSize = it)) }
        )

        SettingsDivider()

        // Icon spacing
        SettingsLabel("Icon Spacing (dp)")
        SettingsSlider(
            value = config.notificationIconSpacing,
            valueRange = 0f..32f,
            onValueChange = { onConfigChanged(config.copy(notificationIconSpacing = it)) }
        )

        SettingsDivider()

        // Offset X
        SettingsLabel("Horizontal Position")
        SettingsSubtext("Screen position where anchor point is placed (0.0 = left, 0.5 = center, 1.0 = right)")
        SettingsSlider(
            value = config.notificationOffsetX,
            valueRange = 0f..1f,
            onValueChange = { onConfigChanged(config.copy(notificationOffsetX = it)) }
        )

        SettingsDivider()

        // Offset Y
        SettingsLabel("Vertical Position")
        SettingsSubtext("Screen position where anchor point is placed (0.0 = top, 0.5 = center, 1.0 = bottom)")
        SettingsSlider(
            value = config.notificationOffsetY,
            valueRange = 0f..1f,
            onValueChange = { onConfigChanged(config.copy(notificationOffsetY = it)) }
        )
    }
}

// Helper composables

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        color = AccentColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun SettingsSubtext(text: String) {
    Text(
        text = text,
        color = Color(0xB3FFFFFF),
        fontSize = 12.sp,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun SettingsSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    val span = valueRange.endInclusive - valueRange.start
    val zeroInInterior = valueRange.start < 0f && valueRange.endInclusive > 0f
    val snapThreshold = span * 0.03f

    Column {
        Text(
            text = formatSliderValue(value, valueRange, steps),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = { newValue ->
                val snapped = if (zeroInInterior && kotlin.math.abs(newValue) < snapThreshold) 0f else newValue
                onValueChange(snapped)
            },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = AccentColor,
                activeTrackColor = AccentColor,
                inactiveTrackColor = Color(0x44FFFFFF)
            )
        )
    }
}

private fun formatSliderValue(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): String {
    val span = valueRange.endInclusive - valueRange.start
    val isIntegerStepped = steps > 0 && span >= steps + 1
    return when {
        isIntegerStepped -> value.toInt().toString()
        span >= 100f -> "%.0f".format(value)
        span >= 10f -> "%.1f".format(value)
        else -> "%.2f".format(value)
    }
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
                checkedColor = AccentColor,
                uncheckedColor = Color(0x99FFFFFF),
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
            colors = settingsFieldColors()
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
            colors = settingsFieldColors()
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
            colors = settingsFieldColors()
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
private fun AppNameAlignmentSpinner(
    selected: AppNameAlignment,
    onSelected: (AppNameAlignment) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    fun label(alignment: AppNameAlignment): String = when (alignment) {
        AppNameAlignment.LEFT -> "Left"
        AppNameAlignment.CENTER -> "Center"
        AppNameAlignment.RIGHT -> "Right"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = label(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = settingsFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppNameAlignment.values().forEach { alignment ->
                DropdownMenuItem(
                    text = { Text(label(alignment)) },
                    onClick = {
                        onSelected(alignment)
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
            colors = settingsFieldColors()
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
            colors = settingsFieldColors()
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

    // Helper to format anchor name for display (e.g., "TOP_LEFT" -> "Top Left")
    fun formatAnchorName(anchor: NotificationAnchor): String {
        return anchor.name.replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = formatAnchorName(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = settingsFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NotificationAnchor.values().forEach { anchor ->
                DropdownMenuItem(
                    text = { Text(formatAnchorName(anchor)) },
                    onClick = {
                        onSelected(anchor)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun settingsFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = CardColor,
    unfocusedContainerColor = CardColor,
    focusedIndicatorColor = AccentColor,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTrailingIconColor = Color.White,
    unfocusedTrailingIconColor = Color.White
)

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = Color(0x1FFFFFFF)
    )
}
