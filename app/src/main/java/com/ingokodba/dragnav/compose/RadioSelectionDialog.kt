package com.ingokodba.dragnav.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable single-choice radio dialog. Used wherever a setting needs a list
 * of mutually-exclusive options. Tapping a row selects it; the radio button
 * is vertically centered with the label.
 */
@Composable
fun <T> RadioSelectionDialog(
    title: String,
    entries: List<String>,
    values: List<T>,
    selectedValue: T,
    onDismiss: () -> Unit,
    onSelected: (T) -> Unit,
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEachIndexed { index, entry ->
                    val value = values[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selectedValue,
                            onClick = { onSelected(value) }
                        )
                        Text(entry)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
