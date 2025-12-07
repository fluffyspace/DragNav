package com.ingokodba.dragnav.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dragnav.R
import com.ingokodba.dragnav.ShortcutAction

@Composable
fun ShortcutDialog(
    actions: List<ShortcutAction>,
    onDismissRequest: () -> Unit,
    onActionClick: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    text = "Shortcuts", // Or pass a title if needed
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                LazyColumn {
                    itemsIndexed(actions) { index, action ->
                        ShortcutItem(
                            action = action,
                            onClick = { onActionClick(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutItem(
    action: ShortcutAction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = action.icon
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap(48.dp.value.toInt(), 48.dp.value.toInt()).asImageBitmap(), // Helper or extension needed if toBitmap is not available on Drawable directly in the right context, but core-ktx usually has it
                contentDescription = null,
                modifier = Modifier.size(24.dp) // Material design icon size
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Simple extension to convert Drawable to Bitmap if not already available
// Assuming core-ktx is available which adds toBitmap
private fun Drawable.toBitmap(width: Int, height: Int): android.graphics.Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) {
        return this.bitmap
    }
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}
