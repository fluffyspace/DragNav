package com.ingokodba.dragnav.rainbow

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ingokodba.dragnav.EncapsulatedAppInfoWithFolder
import com.ingokodba.dragnav.SearchFragment
import kotlinx.coroutines.delay

/**
 * Material 3 Compose version of SearchOverlay with glassy search bar and app list
 */
@Composable
fun SearchOverlayMaterial(
    visible: Boolean,
    apps: List<EncapsulatedAppInfoWithFolder>,
    icons: Map<String, Drawable?>,
    onAppClicked: (EncapsulatedAppInfoWithFolder) -> Unit,
    onAppLongPressed: (EncapsulatedAppInfoWithFolder) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var filteredApps by remember { mutableStateOf(apps) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Update filtered apps when query or apps change
    LaunchedEffect(searchQuery, apps) {
        filteredApps = if (searchQuery.isEmpty()) {
            apps
        } else {
            searchApps(apps, searchQuery)
        }
        // Scroll to top when search changes
        if (listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
    }
    
    // Show keyboard when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            delay(100)
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            searchQuery = ""
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() }
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        if (filteredApps.isNotEmpty()) {
                            onAppClicked(filteredApps[0])
                        }
                    },
                    onDismiss = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                )
                
                // App list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clickable(enabled = false) { /* Prevent background click through */ },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = filteredApps,
                        key = { index, item ->
                            item.folderName ?: item.apps.firstOrNull()?.packageName ?: index.toString()
                        }
                    ) { index, item ->
                        AppListItem(
                            item = item,
                            icon = item.apps.firstOrNull()?.let { app ->
                                icons[app.packageName]
                            },
                            onClick = { onAppClicked(item) },
                            onLongClick = { onAppLongPressed(item) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    },
                placeholder = {
                    Text(
                        text = "Search apps and folders...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = EditorInfo.IME_ACTION_SEARCH
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch() }
                )
            )
        }
    }
}

@Composable
private fun AppListItem(
    item: EncapsulatedAppInfoWithFolder,
    icon: Drawable?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(item) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.folderName != null) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.folderName != null) {
                    // Folder icon - simple circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    )
                } else if (icon != null) {
                    // App icon
                    val bitmap = remember(icon) {
                        icon.toBitmap(56, 56)
                    }
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.apps.firstOrNull()?.label,
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Fallback: colored circle
                    val colorValue = try {
                        item.apps.firstOrNull()?.color?.takeIf { it.isNotEmpty() }?.toInt()
                            ?: android.graphics.Color.GRAY
                    } catch (e: Exception) {
                        android.graphics.Color.GRAY
                    }
                    val composeColor = Color(colorValue)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(composeColor)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Text
            Text(
                text = item.folderName ?: item.apps.firstOrNull()?.label ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun searchApps(
    apps: List<EncapsulatedAppInfoWithFolder>,
    query: String
): List<EncapsulatedAppInfoWithFolder> {
    val results = mutableListOf<Pair<Int, EncapsulatedAppInfoWithFolder>>()
    val queryLower = query.lowercase()
    
    for (item in apps) {
        if (item.folderName != null) {
            // Search folder name using similar algorithm to app search
            item.folderName?.let { folderName ->
                var words = folderName.split(Regex("(?=[A-Z])"), 0)
                words = words.filter { it != "" }
                var count = 0
                var score = 0
                var pos = 0
                for ((i, word) in words.withIndex()) {
                    if (pos >= query.length) break
                    for (letter in word) {
                        if (letter.lowercaseChar() == queryLower[pos]) {
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
                    results.add(Pair(score, item))
                }
            }
        } else if (item.apps.isNotEmpty()) {
            // Search app name using existing algorithm
            val app = item.apps.first()
            val scoredApps = SearchFragment.getAppsByQuery(listOf(app), query)
            if (scoredApps.isNotEmpty()) {
                results.add(Pair(scoredApps.first().first, item))
            }
        }
    }
    
    return results.sortedByDescending { it.first }.map { it.second }
}

/**
 * Android View wrapper for SearchOverlayMaterial to use in XML layouts
 */
class SearchOverlayMaterialView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val composeView: ComposeView
    private val allAppsState = mutableStateOf<List<EncapsulatedAppInfoWithFolder>>(emptyList())
    private val iconsState = mutableStateOf<Map<String, Drawable?>>(emptyMap())
    private val isVisibleState = mutableStateOf(false)
    
    interface SearchOverlayListener {
        fun onAppClicked(app: EncapsulatedAppInfoWithFolder)
        fun onAppLongPressed(app: EncapsulatedAppInfoWithFolder)
        fun onDismiss()
    }
    
    private var listener: SearchOverlayListener? = null
    
    init {
        // Create ComposeView as a child
        composeView = ComposeView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }
        addView(composeView)
        
        composeView.setContent {
            MaterialTheme {
                SearchOverlayMaterial(
                    visible = isVisibleState.value,
                    apps = allAppsState.value,
                    icons = iconsState.value,
                    onAppClicked = { listener?.onAppClicked(it) },
                    onAppLongPressed = { listener?.onAppLongPressed(it) },
                    onDismiss = { listener?.onDismiss() }
                )
            }
        }
    }
    
    fun setListener(listener: SearchOverlayListener) {
        this.listener = listener
    }
    
    fun setApps(apps: List<EncapsulatedAppInfoWithFolder>) {
        allAppsState.value = apps
    }
    
    fun setIcons(icons: Map<String, Drawable?>) {
        iconsState.value = icons
    }
    
    fun show() {
        isVisibleState.value = true
        visibility = VISIBLE
        bringToFront()
        requestFocus()
    }
    
    fun hide() {
        isVisibleState.value = false
        visibility = GONE
        clearFocus()
    }
}

