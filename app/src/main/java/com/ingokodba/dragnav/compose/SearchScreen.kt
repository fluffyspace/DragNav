package com.ingokodba.dragnav.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import android.provider.Settings
import android.view.Gravity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.ViewModel
import com.ingokodba.dragnav.modeli.AppInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    mainActivity: MainActivityCompose,
    viewModel: ViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Collect state from ViewModel
    val appsList by viewModel.appsListFlow.collectAsStateWithLifecycle()

    // Local UI state
    var searchQuery by remember { mutableStateOf("") }
    var showAppMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    // Calculate search results
    val searchResults by remember {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                // Show recently used apps (max 5)
                appsList
                    .filter { it.frequency > 0 }
                    .sortedByDescending { it.lastLaunched }
                    .take(5)
                    .map { Pair(0, it) }
            } else {
                // Search apps with fuzzy matching
                getAppsByQuery(appsList, searchQuery)
                    .sortedByDescending { it.second.lastLaunched }
                    .take(11) // Show max 11 results
            }
        }
    }

    // Handle back button
    BackHandler {
        keyboardController?.hide()
        mainActivity.navController?.popBackStack()
    }

    // Request focus on first composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("Search apps") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    // Open Google search in browser
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=$searchQuery")
                    )
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                    context.startActivity(intent)
                    keyboardController?.hide()
                    mainActivity.navController?.popBackStack()
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.DarkGray,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results as chips in flow layout
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            searchResults.forEach { (score, app) ->
                AppChip(
                    app = app,
                    onClick = {
                        // Launch app
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                        keyboardController?.hide()
                        mainActivity.navController?.popBackStack()
                    },
                    onLongClick = {
                        selectedApp = app
                        showAppMenu = true
                    }
                )
            }
        }
    }

    // App menu dialog
    if (showAppMenu && selectedApp != null) {
        AppMenuDialog(
            app = selectedApp!!,
            onDismiss = {
                showAppMenu = false
                selectedApp = null
            },
            onOpenAppInfo = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", selectedApp!!.packageName, null)
                intent.data = uri
                context.startActivity(intent)
                showAppMenu = false
                selectedApp = null
            },
            onAddToMenu = {
                // Add app to current menu
                // TODO: Implement proper add to menu functionality
                // For now, just close the dialog
                showAppMenu = false
                selectedApp = null
                mainActivity.navController?.popBackStack()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppChip(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = try {
            Color(app.color.toInt())
        } catch (e: NumberFormatException) {
            Color.Gray
        },
        tonalElevation = 4.dp
    ) {
        Text(
            text = app.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AppMenuDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onAddToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column {
                TextButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open App Info", modifier = Modifier.fillMaxWidth())
                }
                TextButton(
                    onClick = onAddToMenu,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Menu", modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Fuzzy search algorithm using SearchUtils
private fun getAppsByQuery(apps: List<AppInfo>, query: String): List<Pair<Int, AppInfo>> {
    return com.ingokodba.dragnav.SearchUtils.getAppsByQuery(apps, query).sortedByDescending { it.first }
}
