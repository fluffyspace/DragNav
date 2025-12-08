package com.ingokodba.dragnav.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dragnav.R

data class FolderNameDialogState(
    val title: String,
    val initialName: String = "",
    val showPickColor: Boolean = false,
    val onSubmit: (String) -> Unit,
    val onPickColor: (() -> Unit)? = null
)

@Composable
fun FolderNameDialog(
    state: FolderNameDialogState?,
    onDismissRequest: () -> Unit
) {
    if (state == null) return

    val context = LocalContext.current
    var folderName by remember(state.initialName) { mutableStateOf(state.initialName) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                // Text input
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(context.getString(R.string.folder_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // Pick color button (if enabled)
                if (state.showPickColor && state.onPickColor != null) {
                    OutlinedButton(
                        onClick = { state.onPickColor() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(context.getString(R.string.odaberi_boju))
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.cancel))
                    }
                    
                    val submitText = remember(state.title) {
                        if (state.title.contains(context.getString(R.string.editing_a_folder), ignoreCase = true)) {
                            context.getString(R.string.save)
                        } else {
                            context.getString(R.string.add_folder)
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (folderName.isNotBlank()) {
                                keyboardController?.hide()
                                state.onSubmit(folderName)
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = folderName.isNotBlank()
                    ) {
                        Text(submitText)
                    }
                }
            }
        }
    }

    // Request focus and show keyboard when dialog appears
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
