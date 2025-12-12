package com.ingokodba.dragnav.compose.notifications

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import com.ingokodba.dragnav.compose.AppNotification
import com.ingokodba.dragnav.rainbow.PathConfig

/**
 * Safely converts a drawable to ImageBitmap, handling recycled bitmaps
 */
private fun android.graphics.drawable.Drawable.toSafeImageBitmap(width: Int, height: Int): ImageBitmap? {
    return try {
        // Check if it's a BitmapDrawable with a recycled bitmap
        if (this is BitmapDrawable && this.bitmap != null && this.bitmap.isRecycled) {
            return null
        }
        toBitmap(width, height).asImageBitmap()
    } catch (e: Exception) {
        Log.e("NotificationComposables", "Failed to convert drawable to bitmap", e)
        null
    }
}

/**
 * Composable function to display app icons for apps with unread notifications
 * Shows a vertical list of icons only, positioned according to PathConfig
 */
@Composable
fun NotificationIconsList(
    notifications: List<AppNotification>,
    config: PathConfig,
    modifier: Modifier = Modifier,
    selectedPackage: String? = null,
    onIconClick: (AppNotification) -> Unit = {}
) {
    // Group notifications by package name to show unique app icons
    Log.d("NL_DEBUG", ">>> NotificationIconsList: Input notifications count: ${notifications.size} <<<")
    val uniqueAppNotifications = notifications
        .distinctBy { it.packageName }
        .filter { it.appIcon != null }

    Log.d("NL_DEBUG", ">>> NotificationIconsList: After distinctBy+filter: ${uniqueAppNotifications.size} unique apps with icons <<<")
    notifications.forEach { n ->
        Log.d("NL_DEBUG", "  Input: ${n.packageName}, hasIcon=${n.appIcon != null}")
    }
    uniqueAppNotifications.forEach { n ->
        Log.d("NL_DEBUG", "  Unique: ${n.packageName}")
    }

    if (uniqueAppNotifications.isEmpty()) {
        Log.d("NL_DEBUG", ">>> NotificationIconsList: RETURNING EARLY - no unique notifications with icons! <<<")
        return
    }

    // Map anchor to bias values (0.0 = left/top, 0.5 = center, 1.0 = right/bottom)
    val (anchorBiasX, anchorBiasY) = when (config.notificationAnchor) {
        com.ingokodba.dragnav.rainbow.NotificationAnchor.TOP_LEFT -> Pair(0f, 0f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.TOP_CENTER -> Pair(0.5f, 0f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.TOP_RIGHT -> Pair(1f, 0f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.CENTER_LEFT -> Pair(0f, 0.5f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.CENTER -> Pair(0.5f, 0.5f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.CENTER_RIGHT -> Pair(1f, 0.5f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.BOTTOM_LEFT -> Pair(0f, 1f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.BOTTOM_CENTER -> Pair(0.5f, 1f)
        com.ingokodba.dragnav.rainbow.NotificationAnchor.BOTTOM_RIGHT -> Pair(1f, 1f)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = this.constraints.maxWidth.toFloat()
        val screenHeightPx = this.constraints.maxHeight.toFloat()

        // Where to position the anchor point (absolute screen coordinates in pixels)
        val targetXPx = config.notificationOffsetX * screenWidthPx
        val targetYPx = config.notificationOffsetY * screenHeightPx

        Column(
            modifier = Modifier
                .wrapContentSize()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    // Calculate position: target position minus anchor offset
                    val x = (targetXPx - placeable.width * anchorBiasX).toInt()
                    val y = (targetYPx - placeable.height * anchorBiasY).toInt()

                    // Logging for debugging
                    /*Log.d("NotificationIconsList", "=== NOTIFICATION POSITIONING DEBUG ===")
                    Log.d("NotificationIconsList", "Anchor: ${config.notificationAnchor.name}")
                    Log.d("NotificationIconsList", "Anchor bias: ($anchorBiasX, $anchorBiasY)")
                    Log.d("NotificationIconsList", "Screen dimensions: ${screenWidthPx}px x ${screenHeightPx}px")
                    Log.d("NotificationIconsList", "Offset percentages: X=${config.notificationOffsetX}, Y=${config.notificationOffsetY}")
                    Log.d("NotificationIconsList", "Target position (anchor point): X=${targetXPx}px, Y=${targetYPx}px")
                    Log.d("NotificationIconsList", "Column dimensions: ${placeable.width}px x ${placeable.height}px")
                    Log.d("NotificationIconsList", "Calculated Column position: X=${x}px, Y=${y}px")
                    Log.d("NotificationIconsList", "Number of notifications: ${uniqueAppNotifications.size}")
                    Log.d("NotificationIconsList", "=====================================")*/

                    layout(placeable.width, placeable.height) {
                        placeable.place(x, y)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(config.notificationIconSpacing.dp)
        ) {
            uniqueAppNotifications.forEach { notification ->
                val isSelected = notification.packageName == selectedPackage
                notification.appIcon?.let { drawable ->
                    val iconSize = config.notificationIconSize.toInt() * 2
                    val imageBitmap = remember(notification.packageName, iconSize) {
                        drawable.toSafeImageBitmap(iconSize, iconSize)
                    }
                    imageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = "App icon for ${notification.packageName}",
                            modifier = Modifier
                                .size(config.notificationIconSize.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onIconClick(notification) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable function to display app notifications in a list
 */
@Composable
fun NotificationsList(
    notifications: List<AppNotification>,
    modifier: Modifier = Modifier,
    onNotificationClick: (AppNotification) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications) { notification ->
            NotificationItem(
                notification = notification,
                onClick = { onNotificationClick(notification) }
            )
        }
    }
}

/**
 * Individual notification item
 */
@Composable
fun NotificationItem(
    notification: AppNotification,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                Log.d("NotificationItem", "Notification item clicked: ${notification.packageName} - ${notification.title}")
                onClick()
            }
            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
            .border(
                width = 1.dp,
                color = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        notification.appIcon?.let { drawable ->
            val imageBitmap = remember(notification.packageName) {
                drawable.toSafeImageBitmap(64, 64)
            }
            imageBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "App icon",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Notification title and content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = notification.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.content,
                fontSize = 14.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
            )
        }
    }
}