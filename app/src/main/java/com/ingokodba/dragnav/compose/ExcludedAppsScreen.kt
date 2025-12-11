package com.ingokodba.dragnav.compose

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.PreferenceKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class for installed app info
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

/**
 * Screen to select which apps to exclude from notification display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val gson = remember { Gson() }

    // Load excluded apps from preferences
    var excludedApps by remember {
        val json = prefs.getString(PreferenceKeys.NOTIFICATION_EXCLUDED_APPS, "[]") ?: "[]"
        val apps = try {
            gson.fromJson(json, Array<String>::class.java)?.toMutableSet() ?: mutableSetOf()
        } catch (e: Exception) {
            mutableSetOf()
        }
        mutableStateOf(apps)
    }

    // Load installed apps
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    // Only show apps with a launcher intent (user-visible apps)
                    pm.getLaunchIntentForPackage(app.packageName) != null &&
                    // Exclude our own app
                    app.packageName != context.packageName
                }
                .map { app ->
                    InstalledAppInfo(
                        packageName = app.packageName,
                        appName = try {
                            pm.getApplicationLabel(app).toString()
                        } catch (e: Exception) {
                            app.packageName
                        },
                        icon = try {
                            pm.getApplicationIcon(app.packageName)
                        } catch (e: Exception) {
                            null
                        }
                    )
                }
                .sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                installedApps = apps
                isLoading = false
            }
        }
    }

    // Save excluded apps when they change
    fun saveExcludedApps(apps: Set<String>) {
        val json = gson.toJson(apps.toTypedArray())
        prefs.edit().putString(PreferenceKeys.NOTIFICATION_EXCLUDED_APPS, json).apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exclude Apps") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(installedApps) { app ->
                    val isExcluded = excludedApps.contains(app.packageName)

                    AppExcludeItem(
                        appInfo = app,
                        isExcluded = isExcluded,
                        onToggle = { excluded ->
                            excludedApps = if (excluded) {
                                (excludedApps + app.packageName).toMutableSet()
                            } else {
                                (excludedApps - app.packageName).toMutableSet()
                            }
                            saveExcludedApps(excludedApps)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppExcludeItem(
    appInfo: InstalledAppInfo,
    isExcluded: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isExcluded) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            appInfo.icon?.let { drawable ->
                Image(
                    bitmap = drawable.toBitmap(48, 48).asImageBitmap(),
                    contentDescription = "App icon",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } ?: Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // App name
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Checkbox
            Checkbox(
                checked = isExcluded,
                onCheckedChange = { onToggle(it) }
            )
        }
    }
    HorizontalDivider()
}
