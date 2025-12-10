package com.ingokodba.dragnav.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.ViewModel
import com.ingokodba.dragnav.modeli.AppInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivitiesScreen(
    mainActivity: MainActivityCompose,
    viewModel: ViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Collect state from ViewModel
    val appsList by viewModel.appsListFlow.collectAsStateWithLifecycle()
    val icons by viewModel.iconsFlow.collectAsStateWithLifecycle()

    // Local UI state
    var searchQuery by remember { mutableStateOf("") }
    var showAppMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    // Filter apps based on search query
    val filteredApps by remember {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                appsList
            } else {
                // Use the fuzzy search algorithm from SearchFragment
                getAppsByQuery(appsList, searchQuery)
                    .sortedByDescending { it.second.lastLaunched }
                    .map { it.second }
            }
        }
    }

    // Handle back button
    BackHandler {
        keyboardController?.hide()
        mainActivity.navController?.popBackStack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar with search and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    // TODO: Navigate to settings
                }
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // App list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                items = filteredApps,
                key = { it.packageName }
            ) { app ->
                AppListItem(
                    app = app,
                    icon = icons[app.packageName],
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
                    },
                    modifier = Modifier.animateItemPlacement()
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
                showAppMenu = false
                selectedApp = null
                mainActivity.navController?.popBackStack()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppInfo,
    icon: android.graphics.drawable.Drawable?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = try {
            Color(app.color.toInt())
        } catch (e: NumberFormatException) {
            Color.DarkGray
        },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            if (icon != null) {
                val bitmap = icon.toBitmap()
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                // Placeholder if icon is not loaded
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray, shape = RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App name
            Text(
                text = app.label,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Fuzzy search algorithm from SearchFragment
private fun getAppsByQuery(apps: List<AppInfo>, query: String): List<Pair<Int, AppInfo>> {
    val searchResults = mutableListOf<Pair<Int, AppInfo>>()
    val queryLowercase = query.map { it.lowercaseChar() }

    for (app in apps) {
        // Split app name into words by capital letters
        var words = app.label.split(Regex("(?=[A-Z])"), 0)
        words = words.filter { it.isNotEmpty() }

        var count = 0
        var score = 0
        var pos = 0

        for ((i, word) in words.withIndex()) {
            if (pos >= query.length) break

            for (letter in word) {
                if (letter.lowercaseChar() == queryLowercase[pos]) {
                    count++
                    score += 10 - i
                    if (letter.isUpperCase()) score += 10
                    pos++
                    if (pos >= query.length) break
                } else {
                    break
                }
            }
        }

        if (count == query.length) {
            searchResults.add(Pair(score, app))
        }
    }

    return searchResults.sortedByDescending { it.first }
}
