package com.ingokodba.dragnav.compose

import android.content.pm.PackageManager
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
import com.ingokodba.dragnav.MainActivityCompose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Screen to select which apps to hide from the launcher (search, app list, rainbow, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsScreen(
    mainActivity: MainActivityCompose,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hiddenApps by remember { mutableStateOf(mainActivity.loadHiddenAppsFromPrefs()) }

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    pm.getLaunchIntentForPackage(app.packageName) != null &&
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Apps") },
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
                item {
                    Text(
                        text = "Hidden apps will not appear in search, the app list, or the rainbow launcher.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                }
                items(installedApps) { app ->
                    val isHidden = hiddenApps.contains(app.packageName)

                    HiddenAppItem(
                        appInfo = app,
                        isHidden = isHidden,
                        onToggle = { hidden ->
                            hiddenApps = if (hidden) {
                                hiddenApps + app.packageName
                            } else {
                                hiddenApps - app.packageName
                            }
                            mainActivity.saveHiddenAppsToPrefs(hiddenApps)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAppItem(
    appInfo: InstalledAppInfo,
    isHidden: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isHidden) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Checkbox(
                checked = isHidden,
                onCheckedChange = { onToggle(it) }
            )
        }
    }
    HorizontalDivider()
}
