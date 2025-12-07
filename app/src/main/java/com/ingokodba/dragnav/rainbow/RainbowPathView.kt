package com.ingokodba.dragnav.rainbow

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.EdgeEffect
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.example.dragnav.R
import com.ingokodba.dragnav.EncapsulatedAppInfoWithFolder
import com.ingokodba.dragnav.modeli.AppInfo
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A fully configurable path-based app launcher view
 * Apps are laid out along a customizable path with various shape options
 */
class RainbowPathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ========== FLING CONFIGURATION ==========
    // Adjust these values to tune fling behavior
    private val flingFriction = 0.97f           // Higher = slower deceleration, longer fling (0.97 = ~3 second duration)
    private val minFlingVelocity = 0.0001f      // Almost always trigger fling (very small threshold)
    private val flingStopThreshold = 0.001f     // Velocity below which fling stops
    // =========================================

    // Configuration
    var config: PathConfig = PathConfig()
        set(value) {
            field = value
            updateDebugPaintColor()
            updateDebugPaintWidth()
            invalidate()
        }

    // App data
    private var appList: List<EncapsulatedAppInfoWithFolder> = emptyList()
    var icons: MutableMap<String, Drawable?> = mutableMapOf()
    var inFolder: Boolean = false
    var onlyFavorites: Boolean = false
        set(value) {
            if (field != value) {
                // Save current scroll position (only if not in initial state)
                if (allAppsScrollOffset != 0f || favoritesScrollOffset != 0f) {
                    if (field) {
                        favoritesScrollOffset = scrollOffset
                    } else {
                        allAppsScrollOffset = scrollOffset
                    }
                }

                field = value

                // Restore scroll position for new view (only if initialized)
                if (allAppsScrollOffset != 0f || favoritesScrollOffset != 0f) {
                    scrollOffset = if (value) {
                        favoritesScrollOffset
                    } else {
                        allAppsScrollOffset
                    }
                }

                // Rebuild letter index for the new app list
                updateLetterPositions()

                // Save state after toggling favorites
                saveScrollStateToPrefs()

                invalidate()
            }
        }

    // Scroll state
    // Start with first app fully visible (at beginning of path, not cut off)
    private var scrollOffset: Float = 0f
        get() = field
        set(value) { field = value }
    private var scrollVelocity: Float = 0f
    private var allAppsScrollOffset: Float = 0f
    private var favoritesScrollOffset: Float = 0f
    private var folderScrollOffset: Float = 0f
    private var lastTouchY: Float = 0f
    private var lastTouchX: Float = 0f
    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var isDragging: Boolean = false
    private var isFling: Boolean = false
    private var lastMoveTime: Long = 0L
    private var initialScrollOffset: Float = 0f  // Track scroll offset when drag starts

    // Long press detection
    private var longPressTriggered: Boolean = false
    private var touchedAppIndex: Int? = null

    // Fling animation runnable
    private val flingRunnable = object : Runnable {
        override fun run() {
            if (isFling) {
                flingUpdate()
                postOnAnimation(this)
            }
        }
    }
    
    // Icon loading check runnable - periodically checks if visible app icons have been loaded
    private val iconCheckRunnable = object : Runnable {
        override fun run() {
            if (visibleAppsMissingIcons.isNotEmpty()) {
                checkAndInvalidateIfIconsLoaded()
                // Continue checking every 100ms until all visible icons are loaded
                postDelayed(this, 100)
            }
        }
    }
    
    private var iconCheckActive = false

    // Overscroll effects
    private var topEdgeEffect: EdgeEffect? = null
    private var bottomEdgeEffect: EdgeEffect? = null
    private var overscrollDistance: Float = 0f
    private val maxOverscrollDistance = 0.15f // Maximum overscroll as fraction of spacing

    // Drawn app tracking for touch detection
    private data class DrawnAppInfo(
        val index: Int,
        val rect: RectF,
        val pathPosition: Float
    )
    private val drawnApps = mutableListOf<DrawnAppInfo>()
    
    // Visible apps tracking for icon loading detection
    private val visiblePackageNames = mutableSetOf<String>()
    private val visibleAppsMissingIcons = mutableSetOf<String>()

    // Letter index for fast scrolling
    private val letterPositions = mutableMapOf<Char, Int>()
    private var letterIndexRect: RectF? = null
    private var isInLetterIndex = false
    private var touchStartedInLetterIndex = false  // Track if touch started in letter index
    private var currentLetterIndexLetter: Char? = null  // Track current letter to avoid repeated haptic feedback
    var showLetterIndexBackground = false  // Force show background (e.g., when in settings)

    // Shortcuts
    private var shortcutsAppIndex: Int? = null
    var shortcuts: List<String> = emptyList()
    private val shortcutRects = mutableListOf<RectF>()

    // Event listener
    private var eventListener: EventListener? = null

    // SharedPreferences for persistent scroll state storage (survives process death)
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("rainbow_path_view_state", Context.MODE_PRIVATE)
    }
    
    // Unique ID for this view instance (used to differentiate multiple instances)
    private val viewStateId: String = "rainbow_path_view_default"

    // Paints
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val textBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
    }
    private val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    private val bigLetterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 2f, 2f, Color.BLACK)
    }
    private val bigLetterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC000000")
        style = Paint.Style.FILL
    }
    private val letterIndexBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }
    private val favButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }
    private val searchButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }
    private val shortcutBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC333333")
        style = Paint.Style.FILL
    }
    private val shortcutTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEB3B")
        style = Paint.Style.FILL
    }
    private val favoriteIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
    }
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private fun updateDebugPaintColor() {
        // Convert hue (0-360) to RGB color using HSV color space
        // Use full saturation (1.0) and full brightness (1.0) for vibrant colors
        val hsv = floatArrayOf(
            config.pathHue.coerceIn(0f, 360f),
            1.0f,  // Saturation
            1.0f   // Value (brightness)
        )
        val alpha = config.pathAlpha.coerceIn(0f, 255f).toInt()
        debugPaint.color = Color.HSVToColor(alpha, hsv)
    }
    
    private fun updateDebugPaintWidth() {
        debugPaint.strokeWidth = config.pathWidth.coerceAtLeast(0.5f)
    }

    // Debug mode
    var debugMode = true

    interface EventListener {
        fun onAppClicked(appIndex: Int)
        fun onAppLongPressed(appIndex: Int)
        fun onShortcutClicked(shortcutIndex: Int)
        fun onFavoritesToggled()
        fun onBackButtonPressed()
        fun onFlingStarted()
        fun onFlingEnded()
        fun onLongPressStart(appIndex: Int)
        fun onSearchButtonClicked()
    }
    
    // Log when event listener is called
    private fun logEventListenerCall(methodName: String, appIndex: Int? = null) {
        Log.d("RainbowPathTouch", "EventListener.$methodName called${if (appIndex != null) " with appIndex=$appIndex" else ""}")
    }

    fun setEventListener(listener: EventListener) {
        eventListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        topEdgeEffect = EdgeEffect(context)
        bottomEdgeEffect = EdgeEffect(context)
        topEdgeEffect?.setSize(w, h)
        bottomEdgeEffect?.setSize(w, h)
        updateDebugPaintColor()
        updateDebugPaintWidth()
    }
    
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        val timestamp = System.currentTimeMillis()
        Log.d("RainbowPathTouch", "onWindowFocusChanged: hasWindowFocus=$hasWindowFocus, timestamp=$timestamp, isDragging=$isDragging, isFling=$isFling, longPressTriggered=$longPressTriggered, touchedAppIndex=$touchedAppIndex")
        // Reset touch state when window regains focus (e.g., after returning from another app)
        // This ensures touch events work correctly after the app returns to foreground
        if (hasWindowFocus) {
            Log.d("RainbowPathTouch", "onWindowFocusChanged: Window gained focus, resetting touch state at timestamp=$timestamp")
            resetTouchState()
            Log.d("RainbowPathTouch", "onWindowFocusChanged: Touch state reset complete at timestamp=$timestamp, longPressTriggered=$longPressTriggered")
        } else {
            Log.d("RainbowPathTouch", "onWindowFocusChanged: Window lost focus at timestamp=$timestamp - NOTE: Fragment should cancel countdown job in onPause")
        }
    }
    
    /**
     * Reset all touch-related state variables to their initial state.
     * Called when touch is cancelled or window regains focus.
     */
    private fun resetTouchState() {
        Log.d("RainbowPathTouch", "resetTouchState: BEFORE - isDragging=$isDragging, isFling=$isFling, longPressTriggered=$longPressTriggered, touchedAppIndex=$touchedAppIndex, touchStartedInLetterIndex=$touchStartedInLetterIndex")
        isInLetterIndex = false
        currentLetterIndexLetter = null
        isDragging = false
        longPressTriggered = false
        touchedAppIndex = null
        touchStartedInLetterIndex = false
        scrollVelocity = 0f
        
        // Release edge effects
        topEdgeEffect?.onRelease()
        bottomEdgeEffect?.onRelease()
        
        // Stop any ongoing fling
        if (isFling) {
            removeCallbacks(flingRunnable)
            isFling = false
            eventListener?.onFlingEnded()
        }
        
        // Reset overscroll
        if (overscrollDistance != 0f) {
            overscrollDistance = 0f
            invalidate()
        }
        Log.d("RainbowPathTouch", "resetTouchState: AFTER - isDragging=$isDragging, isFling=$isFling, longPressTriggered=$longPressTriggered, touchedAppIndex=$touchedAppIndex, touchStartedInLetterIndex=$touchStartedInLetterIndex")
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Save state when view is detached (e.g., Activity going to background)
        saveScrollStateToPrefs()
        Log.d("RainbowPath", "onDetachedFromWindow - saved scroll state")
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Try to restore state when view is attached (may be after process death)
        if (allAppsScrollOffset == 0f && favoritesScrollOffset == 0f && folderScrollOffset == 0f) {
            loadScrollStateFromPrefs()
            Log.d("RainbowPath", "onAttachedToWindow - restored scroll state from prefs")
        }
    }
    
    /**
     * Save scroll state to SharedPreferences (survives process death)
     */
    private fun saveScrollStateToPrefs() {
        prefs.edit().apply {
            putFloat("${viewStateId}_all_apps_scroll", allAppsScrollOffset)
            putFloat("${viewStateId}_favorites_scroll", favoritesScrollOffset)
            putFloat("${viewStateId}_folder_scroll", folderScrollOffset)
            putBoolean("${viewStateId}_only_favorites", onlyFavorites)
            putBoolean("${viewStateId}_in_folder", inFolder)
            putFloat("${viewStateId}_current_scroll", scrollOffset)
            apply()
        }
        Log.d("RainbowPath", "Saved scroll state: allApps=$allAppsScrollOffset, favorites=$favoritesScrollOffset, folder=$folderScrollOffset, inFolder=$inFolder, current=$scrollOffset")
    }
    
    /**
     * Load scroll state from SharedPreferences (called after process death)
     */
    private fun loadScrollStateFromPrefs() {
        allAppsScrollOffset = prefs.getFloat("${viewStateId}_all_apps_scroll", 0f)
        favoritesScrollOffset = prefs.getFloat("${viewStateId}_favorites_scroll", 0f)
        folderScrollOffset = prefs.getFloat("${viewStateId}_folder_scroll", 0f)
        val savedOnlyFavorites = prefs.getBoolean("${viewStateId}_only_favorites", false)
        val savedInFolder = prefs.getBoolean("${viewStateId}_in_folder", false)
        val savedCurrentScroll = prefs.getFloat("${viewStateId}_current_scroll", 0f)
        
        // Only restore if we have saved values (not defaults)
        if (allAppsScrollOffset != 0f || favoritesScrollOffset != 0f || folderScrollOffset != 0f || savedCurrentScroll != 0f) {
            onlyFavorites = savedOnlyFavorites
            inFolder = savedInFolder
            scrollOffset = savedCurrentScroll
            Log.d("RainbowPath", "Loaded scroll state from prefs: allApps=$allAppsScrollOffset, favorites=$favoritesScrollOffset, folder=$folderScrollOffset, inFolder=$inFolder, current=$scrollOffset")
        }
    }

    fun setApps(apps: List<EncapsulatedAppInfoWithFolder>) {
        Log.d("RainbowPath", "setApps called with ${apps.size} apps, inFolder=$inFolder, onlyFavorites=$onlyFavorites")
        appList = apps
        
        // Log apps in the order they're received
        apps.forEachIndexed { idx, app ->
            val name = getNameOfApp(app)
            val pkg = if (app.apps.isNotEmpty()) app.apps.first().packageName else "N/A"
            Log.d("RainbowPath", "  setApps[$idx] (raw order): '$name' ($pkg)")
        }
        
        // Log what getDisplayedApps() will return (after sorting)
        val displayed = getDisplayedApps()
        Log.d("RainbowPath", "setApps: getDisplayedApps() returns ${displayed.size} apps (after filtering/sorting)")
        displayed.forEachIndexed { idx, app ->
            val name = getNameOfApp(app)
            val pkg = if (app.apps.isNotEmpty()) app.apps.first().packageName else "N/A"
            Log.d("RainbowPath", "  DisplayedApp[$idx] (after sort): '$name' ($pkg)")
        }
        
        updateLetterPositions()

        // Try to load saved scroll state from SharedPreferences first (for process death recovery)
        if (allAppsScrollOffset == 0f && favoritesScrollOffset == 0f && folderScrollOffset == 0f) {
            loadScrollStateFromPrefs()
        }

        // Initialize scroll positions to show first app fully visible at start of path
        // Using maxScroll value to position first app at t≈0 (start of path)
        // Only initialize to default if we still don't have saved values
        if (allAppsScrollOffset == 0f && favoritesScrollOffset == 0f) {
            allAppsScrollOffset = config.appSpacing
            favoritesScrollOffset = config.appSpacing
            // Save default values
            saveScrollStateToPrefs()
        }
        
        // Initialize folder scroll offset if not set
        if (folderScrollOffset == 0f) {
            folderScrollOffset = config.appSpacing
        }
        
        // Set current scroll offset based on state
        if (inFolder) {
            // When in folder, use folderScrollOffset (which should have been set by resetFolderScroll when entering)
            // Don't override it - just use the current value
            scrollOffset = folderScrollOffset
        } else {
            // Normal view - use saved scroll positions
            scrollOffset = if (onlyFavorites) favoritesScrollOffset else allAppsScrollOffset
        }
        
        Log.d("RainbowPath", "setApps: apps.size=${apps.size}, inFolder=$inFolder, scrollOffset=$scrollOffset, folderScrollOffset=$folderScrollOffset, allAppsScrollOffset=$allAppsScrollOffset, favoritesScrollOffset=$favoritesScrollOffset")

        invalidate()
        
        // After invalidate, the next draw will populate visibleAppsMissingIcons
        // Post a check to start monitoring after the first draw completes
        post {
            // This will start the periodic check if there are missing icons after the first draw
            checkAndInvalidateIfIconsLoaded()
        }
    }
    
    fun saveScrollPosition() {
        // Save current scroll position to parent view offset
        // This is called BEFORE entering folder, so we always save to parent view
        if (onlyFavorites) {
            favoritesScrollOffset = scrollOffset
        } else {
            allAppsScrollOffset = scrollOffset
        }
        // Also save to SharedPreferences for process death recovery
        saveScrollStateToPrefs()
    }
    
    fun restoreScrollPosition() {
        // Restore scroll position based on current state (after exiting folder)
        scrollOffset = if (onlyFavorites) {
            favoritesScrollOffset
        } else {
            allAppsScrollOffset
        }
    }
    
    fun resetFolderScroll() {
        // Reset folder scroll to show first app at the start
        folderScrollOffset = config.appSpacing
        scrollOffset = folderScrollOffset
        invalidate()
    }
    
    private fun updateFolderScrollPosition() {
        // Update folder scroll offset when scrolling inside folder
        if (inFolder) {
            folderScrollOffset = scrollOffset
            // Save to SharedPreferences periodically during scrolling
            saveScrollStateToPrefs()
        }
    }

    private fun getDisplayedApps(): List<EncapsulatedAppInfoWithFolder> {
        // When in folder, always show all apps (ignore onlyFavorites filter)
        val filtered = if (inFolder) {
            appList
        } else if (onlyFavorites) {
            // When onlyFavorites is true, show only folders (folderName != null)
            appList.filter { it.folderName != null }
        } else {
            // When onlyFavorites is false, show only individual apps (folderName == null)
            appList.filter { it.folderName == null }
        }

        // Sort based on config
        return when (config.appSortOrder) {
            AppSortOrder.ASCENDING -> filtered.sortedBy { 
                val name = if (it.folderName == null) {
                    it.apps.firstOrNull()?.label ?: ""
                } else {
                    it.folderName ?: ""
                }
                name.lowercase()
            }
            AppSortOrder.DESCENDING -> filtered.sortedByDescending { 
                val name = if (it.folderName == null) {
                    it.apps.firstOrNull()?.label ?: ""
                } else {
                    it.folderName ?: ""
                }
                name.lowercase()
            }
        }
    }
    
    private fun getFirstLetterOfApp(thing: EncapsulatedAppInfoWithFolder): Char {
        val name: String = try {
            if (thing.folderName != null && thing.folderName!!.isNotEmpty()) {
                thing.folderName!!
            } else if (thing.apps.isNotEmpty()) {
                try {
                    val firstApp = thing.apps.first()
                    val label = firstApp.label
                    (label as? String)?.takeIf { it.isNotEmpty() } ?: ""
                } catch (e: Exception) {
                    ""
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
        return try {
            if(name.isNotEmpty()) name.first().uppercaseChar() else '?'
        } catch (e: Exception) {
            '?'
        }
    }
    
    private fun getNameOfApp(thing: EncapsulatedAppInfoWithFolder): String {
        return try {
            if (thing.folderName != null && thing.folderName!!.isNotEmpty()) {
                thing.folderName!!
            } else if (thing.apps.isNotEmpty()) {
                try {
                    val firstApp = thing.apps.first()
                    val label = firstApp.label
                    (label as? String)?.takeIf { it.isNotEmpty() } ?: ""
                } catch (e: Exception) {
                    ""
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun updateLetterPositions() {
        letterPositions.clear()
        val apps = getDisplayedApps()
        apps.forEachIndexed { index, thing ->
            val name = getNameOfApp(thing)
            if (name.isNotEmpty()) {
                val letter = name[0].uppercaseChar()
                if (!letterPositions.containsKey(letter)) {
                    letterPositions[letter] = index
                }
            }
        }
    }

    fun showShortcuts(appIndex: Int, shortcutLabels: List<String>) {
        shortcutsAppIndex = appIndex
        shortcuts = shortcutLabels
        invalidate()
    }

    fun hideShortcuts() {
        shortcutsAppIndex = null
        shortcuts = emptyList()
        shortcutRects.clear()
        invalidate()
    }

    fun flingUpdate() {
        if (!isFling) return

        scrollVelocity *= flingFriction
        val apps = getDisplayedApps()

        if (apps.isNotEmpty()) {
            // Allow scrolling one app space before first app (maxScroll = config.appSpacing)
            // and three app spaces after last app appears at end of path (t=1.0)
            // Last app is at index (apps.size - 1), so:
            // baseT = (apps.size - 1) * spacing + minScroll should equal 1.0 + 3*spacing
            // minScroll = 1.0 + 3*spacing - (apps.size - 1)*spacing = 1.0 - (apps.size - 4)*spacing
            // But we want to scroll 3 app spaces PAST the end, so we need MORE negative scrolling
            val maxScroll = config.appSpacing
            val minScroll = 1.0f - (apps.size + 2) * config.appSpacing

            // Ensure valid range (for very few apps, minScroll might be > maxScroll)
            val validMinScroll = minScroll.coerceAtMost(maxScroll)
            val validMaxScroll = maxScroll.coerceAtLeast(minScroll)

            val newScrollOffset = scrollOffset + scrollVelocity

            // Check for boundary collision during fling
            val atTop = scrollOffset >= validMaxScroll && scrollVelocity > 0
            val atBottom = scrollOffset <= validMinScroll && scrollVelocity < 0

            if (atTop || atBottom) {
                // Hit boundary - absorb velocity in edge effect and stop fling
                val absorbVelocity = (abs(scrollVelocity) * 10000).toInt()
                if (atTop) {
                    topEdgeEffect?.onAbsorb(absorbVelocity)
                } else {
                    bottomEdgeEffect?.onAbsorb(absorbVelocity)
                }
                isFling = false
                scrollVelocity = 0f
                eventListener?.onFlingEnded()
                // Save state when fling ends at boundary
                saveScrollStateToPrefs()
            } else {
                // Normal fling
                scrollOffset = newScrollOffset.coerceIn(validMinScroll, validMaxScroll)
                // Update folder scroll position if in folder
                updateFolderScrollPosition()
            }
        }

        // Stop fling when velocity gets very low
        if (abs(scrollVelocity) < flingStopThreshold) {
            isFling = false
            eventListener?.onFlingEnded()
            // Save state when fling ends
            saveScrollStateToPrefs()
        }

        invalidate()
    }

    private fun animateOverscrollRelease() {
        val startDistance = overscrollDistance
        val startTime = System.currentTimeMillis()
        val duration = 150L // ms

        val animator = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                // Ease out interpolation
                val interpolated = 1f - (1f - progress) * (1f - progress)

                overscrollDistance = startDistance * (1f - interpolated)

                if (progress < 1f) {
                    invalidate()
                    postDelayed(this, 16)
                } else {
                    overscrollDistance = 0f
                    invalidate()
                }
            }
        }

        post(animator)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawnApps.clear()
        shortcutRects.clear()
        
        // Track visible apps and missing icons for automatic rerender when icons load
        visiblePackageNames.clear()
        visibleAppsMissingIcons.clear()

        val w = width.toFloat()
        val h = height.toFloat()

        // Update text sizes and styles based on config
        textPaint.textSize = config.appNameSize * resources.displayMetrics.scaledDensity
        textBorderPaint.textSize = config.appNameSize * resources.displayMetrics.scaledDensity
        textBorderPaint.strokeWidth = config.appNameBorderWidth

        // Apply font
        val typeface = when (config.appNameFont) {
            AppNameFont.DEFAULT -> Typeface.DEFAULT
            AppNameFont.SANS_SERIF -> Typeface.SANS_SERIF
            AppNameFont.SERIF -> Typeface.SERIF
            AppNameFont.MONOSPACE -> Typeface.MONOSPACE
        }
        textPaint.typeface = typeface
        textBorderPaint.typeface = typeface

        // Set text alignment to left horizontally, center vertically
        textPaint.textAlign = Paint.Align.LEFT
        textBorderPaint.textAlign = Paint.Align.LEFT

        // Apply style-specific effects
        when (config.appNameStyle) {
            AppNameStyle.PLAIN -> {
                textPaint.clearShadowLayer()
            }
            AppNameStyle.SHADOW -> {
                textPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }
            AppNameStyle.BORDERED -> {
                textPaint.clearShadowLayer()
            }
        }

        letterPaint.textSize = config.letterIndexSize * w
        shortcutTextPaint.textSize = config.appNameSize * resources.displayMetrics.scaledDensity

        // Draw debug path if enabled
        if (debugMode) {
            drawDebugPath(canvas, w, h)
        }

        // Draw apps along path
        drawAppsOnPath(canvas, w, h)

        // Draw letter index
        if (config.letterIndexEnabled) {
            drawLetterIndex(canvas, w, h)
        }

        // Draw favorites button
        drawFavoritesButton(canvas, w, h)

        // Draw search button
        drawSearchButton(canvas, w, h)

        // Draw shortcuts popup if active
        if (shortcutsAppIndex != null && shortcuts.isNotEmpty()) {
            drawShortcutsPopup(canvas, w, h)
        }

        // Draw big letter indicator when scrolling through letter index
        if (isInLetterIndex && currentLetterIndexLetter != null) {
            drawBigLetterIndicator(canvas, w, h)
        }

        // Draw edge effects for overscroll (LAST, with isolated canvas state)
        drawEdgeEffects(canvas, w.toInt(), h.toInt())
        
        // Check if any visible apps' icons have been loaded after draw completes
        // Post this check to run after the draw cycle completes
        if (visibleAppsMissingIcons.isNotEmpty()) {
            post {
                checkAndInvalidateIfIconsLoaded()
            }
        }
    }
    
    /**
     * Check if icons for visible apps that were previously missing have now been loaded.
     * If so, invalidate to trigger a redraw.
     * Also starts/stops the periodic check runnable based on whether icons are missing.
     */
    private fun checkAndInvalidateIfIconsLoaded() {
        if (visibleAppsMissingIcons.isEmpty()) {
            // No missing icons - stop periodic checking
            if (iconCheckActive) {
                removeCallbacks(iconCheckRunnable)
                iconCheckActive = false
            }
            return
        }
        
        // Start periodic checking if not already active
        if (!iconCheckActive) {
            iconCheckActive = true
            post(iconCheckRunnable)
        }
        
        // Check if any of the previously missing icons are now available
        val nowAvailable = visibleAppsMissingIcons.filter { packageName ->
            icons[packageName] != null
        }
        
        if (nowAvailable.isNotEmpty()) {
            // Remove from missing set since they're now available
            visibleAppsMissingIcons.removeAll(nowAvailable)
            // Trigger a redraw to show the newly loaded icons
            invalidate()
        }
    }
    
    /**
     * Public method to notify that icons may have been updated.
     * This should be called when icons are set/updated externally.
     */
    fun onIconsUpdated() {
        checkAndInvalidateIfIconsLoaded()
    }

    private fun drawEdgeEffects(canvas: Canvas, width: Int, height: Int) {
        // EdgeEffect is disabled for now - using only bounce effect for overscroll
        // The glow effect was causing unwanted transformations to other UI elements
        var needsInvalidate = false

        topEdgeEffect?.let { effect ->
            if (!effect.isFinished) {
                needsInvalidate = true
            }
        }

        bottomEdgeEffect?.let { effect ->
            if (!effect.isFinished) {
                needsInvalidate = true
            }
        }

        if (needsInvalidate) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawDebugPath(canvas: Canvas, w: Float, h: Float) {
        val provider = PathShapeRegistry.getProvider(config.pathShape)
        val path = Path()

        for (i in 0..100) {
            val t = i / 100f
            val point = provider.getPointOnPath(t, config.startPoint, config.endPoint, config)
            val screenX = point.x * w
            val screenY = (1 - point.y) * h // Flip Y since screen Y is top-down

            if (i == 0) {
                path.moveTo(screenX, screenY)
            } else {
                path.lineTo(screenX, screenY)
            }
        }

        canvas.drawPath(path, debugPaint)
    }

    private fun drawAppsOnPath(canvas: Canvas, w: Float, h: Float) {
        val apps = getDisplayedApps()
        //Log.d("RainbowPath", "drawAppsOnPath: ${apps.size} apps, appList size: ${appList.size}")
        if (apps.isEmpty()) return

        val provider = PathShapeRegistry.getProvider(config.pathShape)
        val iconSizePx = config.appIconSize * w

        // Apply overscroll offset to create bounce effect
        val bounceOffset = overscrollDistance * config.appSpacing

        apps.forEachIndexed { index, thing ->
            // Calculate position on path with bounce offset
            val baseT = index * config.appSpacing + scrollOffset + bounceOffset
            val t = baseT.coerceIn(0f, 1f)

            // Skip if outside visible range
            if (baseT < -0.1f || baseT > 1.1f) return@forEachIndexed

            val point = provider.getPointOnPath(t, config.startPoint, config.endPoint, config)
            val screenX = point.x * w
            val screenY = (1 - point.y) * h

            if (index < 3) {
                //Log.d("RainbowPath", "App $index: baseT=$baseT, t=$t, point=(${point.x}, ${point.y}), screen=($screenX, $screenY)")
            }

            // Create rect for this app
            val rect = RectF(
                screenX - iconSizePx / 2,
                screenY - iconSizePx / 2,
                screenX + iconSizePx / 2,
                screenY + iconSizePx / 2
            )

            drawnApps.add(DrawnAppInfo(index, rect, t))

            if (thing.folderName != null) {
                // Draw folder with multiple app icons
                val folderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#55FFFFFF")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(screenX, screenY, iconSizePx / 2, folderPaint)
                
                // Draw up to 4 app icons in a 2x2 grid
                for (i in 0..1) {
                    for (j in 0..1) {
                        if (i * 2 + j >= thing.apps.size) break
                        val app = thing.apps[i * 2 + j]
                        visiblePackageNames.add(app.packageName)
                        val icon = icons[app.packageName]?.toBitmap()
                        if (icon != null) {
                            val iconRect = RectF(
                                screenX - iconSizePx / 2 + iconSizePx * j / 2,
                                screenY - iconSizePx / 2 + iconSizePx * i / 2,
                                screenX + iconSizePx * j / 2,
                                screenY + iconSizePx * i / 2
                            )
                            canvas.drawBitmap(icon, null, iconRect, iconPaint)
                        } else {
                            // Track that this visible app is missing its icon
                            visibleAppsMissingIcons.add(app.packageName)
                            // Fallback: draw colored circle
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = try {
                                    if (app.color.isNotEmpty()) app.color.toInt() else Color.GRAY
                                } catch (e: Exception) {
                                    Color.GRAY
                                }
                                style = Paint.Style.FILL
                            }
                            val iconRect = RectF(
                                screenX - iconSizePx / 2 + iconSizePx * j / 2,
                                screenY - iconSizePx / 2 + iconSizePx * i / 2,
                                screenX + iconSizePx * j / 2,
                                screenY + iconSizePx * i / 2
                            )
                            canvas.drawCircle(iconRect.centerX(), iconRect.centerY(), iconSizePx / 4, paint)
                        }
                    }
                }
                
                // Draw folder favorite indicator
                if (thing.favorite == true) {
                    canvas.drawCircle(
                        rect.left + iconSizePx / 8,
                        rect.bottom - iconSizePx / 8,
                        iconSizePx / 10,
                        favoriteIndicatorPaint
                    )
                }
            } else {
                // Draw single app icon
                val app = thing.apps.first()
                visiblePackageNames.add(app.packageName)
                val icon = icons[app.packageName]?.toBitmap()
                if (icon != null) {
                    canvas.drawBitmap(icon, null, rect, iconPaint)
                } else {
                    // Track that this visible app is missing its icon
                    visibleAppsMissingIcons.add(app.packageName)
                    // Fallback: draw colored circle
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = try {
                            if (app.color.isNotEmpty()) app.color.toInt() else Color.GRAY
                        } catch (e: Exception) {
                            Color.GRAY
                        }
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(screenX, screenY, iconSizePx / 2, paint)
                }

                // Draw indicators
                if (app.hasShortcuts) {
                    canvas.drawCircle(
                        rect.right - iconSizePx / 8,
                        rect.bottom - iconSizePx / 8,
                        iconSizePx / 10,
                        indicatorPaint
                    )
                }

                if (app.favorite) {
                    canvas.drawCircle(
                        rect.left + iconSizePx / 8,
                        rect.bottom - iconSizePx / 8,
                        iconSizePx / 10,
                        favoriteIndicatorPaint
                    )
                }
            }

            // Draw app/folder name
            if (config.showAppNames) {
                val name = getNameOfApp(thing)
                // Calculate text position with offset (multiplier increased for more range)
                // Offset is relative to icon size, ranging from -1 to 1, multiplied by 3 for larger offset range
                val textOffsetX = config.appNameOffsetX * iconSizePx * 3f
                val textOffsetY = config.appNameOffsetY * iconSizePx * 3f
                // For left alignment, start text from left edge + offset
                val textX = screenX - iconSizePx / 2 + textOffsetX
                // For vertical centering, adjust Y position to text baseline
                val textMetrics = textPaint.fontMetrics
                val textCenterY = screenY + textOffsetY - (textMetrics.ascent + textMetrics.descent) / 2f

                // Draw based on style
                when (config.appNameStyle) {
                    AppNameStyle.BORDERED -> {
                        // Draw border (stroke) first
                        canvas.drawText(name, textX, textCenterY, textBorderPaint)
                        // Draw fill text on top
                        canvas.drawText(name, textX, textCenterY, textPaint)
                    }
                    AppNameStyle.PLAIN, AppNameStyle.SHADOW -> {
                        // Shadow is already applied to textPaint for SHADOW style
                        canvas.drawText(name, textX, textCenterY, textPaint)
                    }
                }
            }
        }
    }

    private fun drawLetterIndex(canvas: Canvas, w: Float, h: Float) {
        // Reverse the letter order so it matches the visual flow on the path
        // (path starts at bottom with t≈0, so first letters should be at bottom of index too)
        val letters = when (config.appSortOrder) {
            AppSortOrder.ASCENDING -> letterPositions.keys.sortedDescending()
            AppSortOrder.DESCENDING -> letterPositions.keys.sorted()
        }
        if (letters.isEmpty()) return

        val indexWidth = config.letterIndexSize * w * 2
        val padding = config.letterIndexPadding * w

        val left = if (config.letterIndexPosition == LetterIndexPosition.RIGHT) {
            w - indexWidth - padding
        } else {
            padding
        }

        val letterHeight = h / letters.size.coerceAtLeast(1)

        // Calculate touchable area - always extends to screen edge with configurable padding from letters
        val paddingFromLetters = config.letterIndexPanFromLetters * w

        val touchableLeft = if (config.letterIndexPosition == LetterIndexPosition.RIGHT) {
            // Right side: extend inward from letters by padding amount
            (left - paddingFromLetters).coerceAtLeast(0f)
        } else {
            // Left side: always start at screen edge
            0f
        }

        val touchableRight = if (config.letterIndexPosition == LetterIndexPosition.RIGHT) {
            // Right side: always extend to screen edge
            w
        } else {
            // Left side: extend outward from letters by padding amount
            (left + indexWidth + paddingFromLetters).coerceAtMost(w)
        }

        letterIndexRect = RectF(touchableLeft, 0f, touchableRight, h)

        // Draw background when user is actively touching the letter index OR when forced (e.g., in settings)
        if (isInLetterIndex || showLetterIndexBackground) {
            canvas.drawRoundRect(letterIndexRect!!, 16f, 16f, letterIndexBgPaint)
        }

        letters.forEachIndexed { index, letter ->
            val y = letterHeight * index + letterHeight / 2 + letterPaint.textSize / 3
            canvas.drawText(
                letter.toString(),
                left + indexWidth / 2,
                y,
                letterPaint
            )
        }
    }

    private fun drawFavoritesButton(canvas: Canvas, w: Float, h: Float) {
        val buttonSize = config.favButtonSize * w
        val centerX = config.favButtonPosition.x * w
        val centerY = (1 - config.favButtonPosition.y) * h

        canvas.drawCircle(centerX, centerY, buttonSize / 2, favButtonPaint)

        // Draw back arrow icon when in folder, otherwise draw star icon
        val iconDrawable = if (inFolder) {
            AppCompatResources.getDrawable(context, R.drawable.ic_baseline_arrow_back_24)
        } else {
            AppCompatResources.getDrawable(
                context,
                if (onlyFavorites) R.drawable.favorite_filled else R.drawable.favorite
            )
        }
        iconDrawable?.let {
            it.setTint(if (inFolder) Color.WHITE else if (onlyFavorites) Color.RED else Color.WHITE)
            val iconSize = buttonSize * 0.6f
            it.setBounds(
                (centerX - iconSize / 2).toInt(),
                (centerY - iconSize / 2).toInt(),
                (centerX + iconSize / 2).toInt(),
                (centerY + iconSize / 2).toInt()
            )
            it.draw(canvas)
        }
    }

    private fun drawSearchButton(canvas: Canvas, w: Float, h: Float) {
        val buttonSize = config.searchButtonSize * w
        val centerX = config.searchButtonPosition.x * w
        val centerY = (1 - config.searchButtonPosition.y) * h

        canvas.drawCircle(centerX, centerY, buttonSize / 2, searchButtonPaint)

        // Draw search icon
        val iconDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_search_50)
        iconDrawable?.let {
            it.setTint(Color.WHITE)
            val iconSize = buttonSize * 0.6f
            it.setBounds(
                (centerX - iconSize / 2).toInt(),
                (centerY - iconSize / 2).toInt(),
                (centerX + iconSize / 2).toInt(),
                (centerY + iconSize / 2).toInt()
            )
            it.draw(canvas)
        }
    }

    private fun drawShortcutsPopup(canvas: Canvas, w: Float, h: Float) {
        val appInfo = drawnApps.find { it.index == shortcutsAppIndex } ?: return
        val iconSizePx = config.appIconSize * w

        val popupWidth = iconSizePx * 3
        val itemHeight = shortcutTextPaint.textSize * 2
        val popupHeight = itemHeight * shortcuts.size

        val popupLeft = (appInfo.rect.centerX() - popupWidth / 2).coerceIn(0f, w - popupWidth)
        val popupTop = (appInfo.rect.top - popupHeight - 8).coerceAtLeast(0f)

        // Draw background
        val popupRect = RectF(popupLeft, popupTop, popupLeft + popupWidth, popupTop + popupHeight)
        canvas.drawRoundRect(popupRect, 16f, 16f, shortcutBgPaint)

        // Draw shortcuts
        shortcuts.forEachIndexed { index, label ->
            val itemTop = popupTop + itemHeight * index
            val itemRect = RectF(popupLeft, itemTop, popupLeft + popupWidth, itemTop + itemHeight)
            shortcutRects.add(itemRect)

            canvas.drawText(
                label,
                popupLeft + popupWidth / 2,
                itemTop + itemHeight / 2 + shortcutTextPaint.textSize / 3,
                shortcutTextPaint
            )
        }
    }

    private fun drawBigLetterIndicator(canvas: Canvas, w: Float, h: Float) {
        val letter = currentLetterIndexLetter ?: return

        // Set text size to be large and bold
        val textSize = w * 0.5f  // % of screen width
        bigLetterPaint.textSize = textSize

        // Calculate text dimensions
        val letterStr = letter.toString()
        val textWidth = bigLetterPaint.measureText(letterStr)
        val textMetrics = bigLetterPaint.fontMetrics
        val textHeight = textMetrics.descent - textMetrics.ascent

        // Position in upper left corner with padding
        val padding = w * 0.05f
        val bgPadding = w * 0.02f
        val left = padding + bgPadding
        val top = padding - textMetrics.ascent + bgPadding

        // Draw background rectangle with equal padding on all sides
        val bgRect = RectF(
            left - bgPadding,
            top + textMetrics.ascent - bgPadding,
            left + textWidth + bgPadding,
            top + textMetrics.descent + bgPadding
        )
        canvas.drawRoundRect(bgRect, 16f, 16f, bigLetterBgPaint)

        // Draw the letter
        canvas.drawText(letterStr, left, top, bigLetterPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionName = when (event.action) {
            MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
            MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
            MotionEvent.ACTION_UP -> "ACTION_UP"
            MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
            else -> "ACTION_${event.action}"
        }
        val timestamp = System.currentTimeMillis()
        Log.d("RainbowPathTouch", "onTouchEvent: $actionName at (${event.x}, ${event.y}), timestamp=$timestamp, isDragging=$isDragging, longPressTriggered=$longPressTriggered, touchedAppIndex=$touchedAppIndex, touchStartedInLetterIndex=$touchStartedInLetterIndex, shortcutsAppIndex=$shortcutsAppIndex")
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("RainbowPathTouch", "ACTION_DOWN: Starting touch at (${event.x}, ${event.y})")
                touchStartX = event.x
                touchStartY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = false
                initialScrollOffset = 0f
                // Stop any ongoing fling
                if (isFling) {
                    Log.d("RainbowPathTouch", "ACTION_DOWN: Stopping ongoing fling")
                    removeCallbacks(flingRunnable)
                    isFling = false
                    eventListener?.onFlingEnded()
                }
                scrollVelocity = 0f
                longPressTriggered = false
                touchedAppIndex = null

                // Check if touch is in letter index
                letterIndexRect?.let {
                    if (it.contains(event.x, event.y)) {
                        Log.d("RainbowPathTouch", "ACTION_DOWN: Touch in letter index")
                        isInLetterIndex = true
                        touchStartedInLetterIndex = true
                        handleLetterIndexTouch(event.y)
                        return true
                    }
                }
                touchStartedInLetterIndex = false

                // Check shortcuts popup
                if (shortcutsAppIndex != null) {
                    Log.d("RainbowPathTouch", "ACTION_DOWN: Shortcuts popup is open, checking if touch is on shortcut")
                    shortcutRects.forEachIndexed { index, rect ->
                        if (rect.contains(event.x, event.y)) {
                            Log.d("RainbowPathTouch", "ACTION_DOWN: Touch on shortcut $index")
                            eventListener?.onShortcutClicked(index)
                            hideShortcuts()
                            return true
                        }
                    }
                    // Touch outside popup - close it
                    Log.d("RainbowPathTouch", "ACTION_DOWN: Touch outside shortcuts popup, closing it")
                    hideShortcuts()
                } else {
                    // Check if touch is on an app and start long press countdown
                    // Calculate directly to avoid stale drawnApps data
                    val touchedIndex = getAppIndexAtTouchPosition(event.x, event.y)
                    if (touchedIndex != null) {
                        Log.d("RainbowPathTouch", "ACTION_DOWN: Touch on app at index $touchedIndex, starting long press countdown")
                        touchedAppIndex = touchedIndex
                        eventListener?.onLongPressStart(touchedIndex)
                    } else {
                        Log.d("RainbowPathTouch", "ACTION_DOWN: Touch not on any app")
                    }
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isInLetterIndex) {
                    Log.d("RainbowPathTouch", "ACTION_MOVE: In letter index")
                    handleLetterIndexTouch(event.y)
                    return true
                }

                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                // Detect drag if finger moves past 10 pixels from initial touch down
                val distanceFromStart = sqrt(
                    (event.x - touchStartX) * (event.x - touchStartX) +
                    (event.y - touchStartY) * (event.y - touchStartY)
                )

                if (!isDragging && distanceFromStart > 30f) {
                    Log.d("RainbowPathTouch", "ACTION_MOVE: Starting drag, distanceFromStart=$distanceFromStart")
                    isDragging = true
                    initialScrollOffset = scrollOffset  // Save current scroll position
                    // Cancel long press if user starts dragging
                    if (!longPressTriggered && touchedAppIndex != null) {
                        Log.d("RainbowPathTouch", "ACTION_MOVE: Cancelling long press due to drag")
                        touchedAppIndex = null
                    }
                }

                if (isDragging) {
                    // Calculate total distance from touch start to current position
                    val totalDistance = sqrt(
                        (event.x - touchStartX) * (event.x - touchStartX) +
                        (event.y - touchStartY) * (event.y - touchStartY)
                    )
                    
                    // Use Y direction as boolean: if moved up (event.y < touchStartY), scroll positive
                    val scrollDirection = if (event.y < touchStartY) 1f else -1f
                    
                    // Calculate scroll amount based on total distance
                    val scrollAmount = (totalDistance / (height * config.appSpacing * 10)) * config.scrollSensitivity
                    
                    val apps = getDisplayedApps()

                    if (apps.isNotEmpty()) {
                        // Allow scrolling one app space before first app (maxScroll = config.appSpacing)
                        // and three app spaces after last app appears at end of path (t=1.0)
                        // Last app is at index (apps.size - 1), so:
                        // baseT = (apps.size - 1) * spacing + minScroll should equal 1.0 + 3*spacing
                        // minScroll = 1.0 + 3*spacing - (apps.size - 1)*spacing = 1.0 - (apps.size - 4)*spacing
                        // But we want to scroll 3 app spaces PAST the end, so we need MORE negative scrolling
                        val maxScroll = config.appSpacing
                        val minScroll = 1.0f - (apps.size + 2) * config.appSpacing

                        // Ensure valid range (for very few apps, minScroll might be > maxScroll)
                        val validMinScroll = minScroll.coerceAtMost(maxScroll)
                        val validMaxScroll = maxScroll.coerceAtLeast(minScroll)

                        // Set scroll offset based on initial position + distance * direction
                        val newScrollOffset = initialScrollOffset + scrollAmount * scrollDirection
                        val scrollDelta = newScrollOffset - scrollOffset

                        // Check if we're at boundaries
                        val atTop = scrollOffset >= validMaxScroll
                        val atBottom = scrollOffset <= validMinScroll

                        if ((atTop && scrollDelta > 0) || (atBottom && scrollDelta < 0)) {
                            // Apply overscroll with resistance
                            val resistance = 0.3f
                            overscrollDistance += scrollDelta * resistance
                            overscrollDistance = overscrollDistance.coerceIn(-maxOverscrollDistance, maxOverscrollDistance)

                            // Trigger edge effect
                            val pullAmount = abs(scrollDelta) * 2f
                            if (atTop && scrollDelta > 0) {
                                topEdgeEffect?.onPull(pullAmount, 0.5f)
                            } else if (atBottom && scrollDelta < 0) {
                                bottomEdgeEffect?.onPull(pullAmount, 0.5f)
                            }

                            // Reset velocity when hitting boundary
                            scrollVelocity = 0f
                        } else {
                            // Normal scrolling
                            scrollOffset = newScrollOffset.coerceIn(validMinScroll, validMaxScroll)
                            // Update folder scroll position if in folder (this saves to prefs)
                            updateFolderScrollPosition()

                            // Track velocity - use actual finger movement speed without multiplier
                            // Fling will continue at this natural velocity and decelerate with friction
                            scrollVelocity = scrollDelta

                            Log.d("RainbowPath", "Tracking velocity: scrollDelta=$scrollDelta, velocity=$scrollVelocity")

                            // Release overscroll if moving away from boundary
                            if (overscrollDistance != 0f) {
                                overscrollDistance *= 0.7f
                                if (abs(overscrollDistance) < 0.001f) {
                                    overscrollDistance = 0f
                                }
                            }
                        }
                    }

                    invalidate()
                }

                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                Log.d("RainbowPathTouch", "ACTION_UP: isDragging=$isDragging, longPressTriggered=$longPressTriggered, touchStartedInLetterIndex=$touchStartedInLetterIndex, touchedAppIndex=$touchedAppIndex")
                isInLetterIndex = false
                currentLetterIndexLetter = null  // Reset letter tracking

                // Release edge effects
                topEdgeEffect?.onRelease()
                bottomEdgeEffect?.onRelease()

                // Animate overscroll back to normal
                if (overscrollDistance != 0f) {
                    animateOverscrollRelease()
                }

                if (!isDragging) {
                    Log.d("RainbowPathTouch", "ACTION_UP: Not dragging, checking for tap")
                    // Only process taps if long press wasn't triggered and touch didn't start in letter index
                    if (!longPressTriggered && !touchStartedInLetterIndex) {
                        Log.d("RainbowPathTouch", "ACTION_UP: Conditions met for tap processing (longPressTriggered=false, touchStartedInLetterIndex=false)")
                        // Check for tap on favorites button
                        val w = width.toFloat()
                        val h = height.toFloat()
                        val buttonSize = config.favButtonSize * w
                        val centerX = config.favButtonPosition.x * w
                        val centerY = (1 - config.favButtonPosition.y) * h

                        val dist = sqrt(
                            (event.x - centerX) * (event.x - centerX) +
                            (event.y - centerY) * (event.y - centerY)
                        )

                        if (dist < buttonSize / 2) {
                            Log.d("RainbowPathTouch", "ACTION_UP: Tap on favorites button")
                            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            if (inFolder) {
                                eventListener?.onBackButtonPressed()
                            } else {
                                eventListener?.onFavoritesToggled()
                            }
                            return true
                        }

                        // Check for tap on search button
                        val searchButtonSize = config.searchButtonSize * w
                        val searchCenterX = config.searchButtonPosition.x * w
                        val searchCenterY = (1 - config.searchButtonPosition.y) * h

                        val searchDist = sqrt(
                            (event.x - searchCenterX) * (event.x - searchCenterX) +
                            (event.y - searchCenterY) * (event.y - searchCenterY)
                        )

                        if (searchDist < searchButtonSize / 2) {
                            Log.d("RainbowPathTouch", "ACTION_UP: Tap on search button")
                            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            eventListener?.onSearchButtonClicked()
                            return true
                        }

                        // Check for tap on app - calculate directly from touch coordinates
                        // to avoid stale drawnApps data
                        val clickedAppIndex = getAppIndexAtTouchPosition(event.x, event.y)
                        if (clickedAppIndex != null) {
                            Log.d("RainbowPathTouch", "ACTION_UP: Tap on app at index $clickedAppIndex, calling onAppClicked")
                            if (eventListener == null) {
                                Log.e("RainbowPathTouch", "ACTION_UP: ERROR - eventListener is NULL! Cannot call onAppClicked")
                            } else {
                                Log.d("RainbowPathTouch", "ACTION_UP: eventListener is not null, calling onAppClicked($clickedAppIndex)")
                                eventListener?.onAppClicked(clickedAppIndex)
                            }
                            return true
                        } else {
                            Log.d("RainbowPathTouch", "ACTION_UP: Tap not on any app (clickedAppIndex=null)")
                        }
                    } else {
                        Log.d("RainbowPathTouch", "ACTION_UP: Tap processing BLOCKED - longPressTriggered=$longPressTriggered, touchStartedInLetterIndex=$touchStartedInLetterIndex")
                    }
                } else {
                    Log.d("RainbowPathTouch", "ACTION_UP: Was dragging, checking for fling")
                    // Start fling if velocity is high enough
                    if (abs(scrollVelocity) > minFlingVelocity) {
                        Log.d("RainbowPath", "Starting fling with velocity: $scrollVelocity")
                        isFling = true
                        eventListener?.onFlingStarted()
                        // Start the fling animation loop
                        postOnAnimation(flingRunnable)
                    } else {
                        Log.d("RainbowPath", "Velocity too low for fling: $scrollVelocity (min: $minFlingVelocity)")
                        // Save state when drag ends without fling
                        saveScrollStateToPrefs()
                    }
                }

                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                Log.d("RainbowPathTouch", "ACTION_CANCEL: Touch cancelled, resetting touch state")
                // Reset all touch state when touch is cancelled (e.g., when app goes to background)
                resetTouchState()
                
                // Animate overscroll back to normal
                if (overscrollDistance != 0f) {
                    animateOverscrollRelease()
                }
                
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * Calculate valid scroll limits based on current app list
     * Returns a Pair of (validMinScroll, validMaxScroll)
     */
    private fun getValidScrollLimits(): Pair<Float, Float> {
        val apps = getDisplayedApps()
        if (apps.isEmpty()) {
            return Pair(0f, 0f)
        }
        
        // Allow scrolling one app space before first app (maxScroll = config.appSpacing)
        // and three app spaces after last app appears at end of path (t=1.0)
        // Last app is at index (apps.size - 1), so:
        // baseT = (apps.size - 1) * spacing + minScroll should equal 1.0 + 3*spacing
        // minScroll = 1.0 + 3*spacing - (apps.size - 1)*spacing = 1.0 - (apps.size - 4)*spacing
        // But we want to scroll 3 app spaces PAST the end, so we need MORE negative scrolling
        val maxScroll = config.appSpacing
        val minScroll = 1.0f - (apps.size + 2) * config.appSpacing

        // Ensure valid range (for very few apps, minScroll might be > maxScroll)
        val validMinScroll = minScroll.coerceAtMost(maxScroll)
        val validMaxScroll = maxScroll.coerceAtLeast(minScroll)
        
        return Pair(validMinScroll, validMaxScroll)
    }

    private fun handleLetterIndexTouch(y: Float) {
        // Use same reversed order as visual display
        val letters = when (config.appSortOrder) {
            AppSortOrder.ASCENDING -> letterPositions.keys.sortedDescending()
            AppSortOrder.DESCENDING -> letterPositions.keys.sorted()
        }
        if (letters.isEmpty()) return

        val letterHeight = height / letters.size
        val index = (y / letterHeight).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[index]

        letterPositions[letter]?.let { appIndex ->
            // Calculate desired scroll position
            // Add an offset to ensure the first app with this letter is fully visible
            // The offset brings the app up a bit so it's not cut off at the bottom
            val baseScrollOffset = -appIndex * config.appSpacing
            val visibilityOffset = config.appSpacing * 1f // Offset to ensure app is fully visible
            val desiredScrollOffset = baseScrollOffset + visibilityOffset
            
            // Apply scroll limits to prevent overscrolling
            val (validMinScroll, validMaxScroll) = getValidScrollLimits()
            scrollOffset = desiredScrollOffset.coerceIn(validMinScroll, validMaxScroll)

            // Only trigger haptic feedback if the letter changed
            if (currentLetterIndexLetter != letter) {
                currentLetterIndexLetter = letter
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }

            invalidate()
        }
    }

    fun getAppIndexAtPosition(x: Float, y: Float): Int? {
        // Iterate in reverse order to check topmost (last-drawn) apps first
        drawnApps.asReversed().forEach { drawnApp ->
            if (drawnApp.rect.contains(x, y)) {
                return drawnApp.index
            }
        }
        return null
    }

    /**
     * Calculate which app is at the given touch position by recalculating positions
     * This avoids using potentially stale drawnApps data
     */
    private fun getAppIndexAtTouchPosition(x: Float, y: Float): Int? {
        val apps = getDisplayedApps()
        if (apps.isEmpty()) return null

        val w = width.toFloat()
        val h = height.toFloat()
        val provider = PathShapeRegistry.getProvider(config.pathShape)
        val iconSizePx = config.appIconSize * w
        val bounceOffset = overscrollDistance * config.appSpacing

        // Check apps in reverse order (last drawn = topmost)
        // This matches the visual order when apps overlap
        for (index in apps.size - 1 downTo 0) {
            val thing = apps[index]
            val baseT = index * config.appSpacing + scrollOffset + bounceOffset
            val t = baseT.coerceIn(0f, 1f)

            // Skip if outside visible range
            if (baseT < -0.1f || baseT > 1.1f) continue

            val point = provider.getPointOnPath(t, config.startPoint, config.endPoint, config)
            val screenX = point.x * w
            val screenY = (1 - point.y) * h

            // Create rect for this app
            val rect = RectF(
                screenX - iconSizePx / 2,
                screenY - iconSizePx / 2,
                screenX + iconSizePx / 2,
                screenY + iconSizePx / 2
            )

            if (rect.contains(x, y)) {
                val appName = getNameOfApp(thing)
                val pkg = if (thing.apps.isNotEmpty()) thing.apps.first().packageName else "N/A"
                Log.d("RainbowPath", "getAppIndexAtTouchPosition: Touch detected at index $index: '$appName' ($pkg)")
                return index
            }
        }
        return null
    }

    fun scrollToApp(index: Int) {
        // When scrolling to first app (index 0), use maxScroll to show it fully visible
        // For other apps, calculate normal scroll position
        val desiredScrollOffset = if (index == 0) {
            config.appSpacing
        } else {
            -index * config.appSpacing
        }
        
        // Apply scroll limits to prevent overscrolling
        val (validMinScroll, validMaxScroll) = getValidScrollLimits()
        scrollOffset = desiredScrollOffset.coerceIn(validMinScroll, validMaxScroll)
        
        invalidate()
    }

    fun scrollToLetter(letter: Char) {
        letterPositions[letter.uppercaseChar()]?.let { appIndex ->
            scrollToApp(appIndex)
        }
    }

    fun triggerLongPress() {
        Log.d("RainbowPathTouch", "RainbowPathView.triggerLongPress: CALLED, touchedAppIndex=$touchedAppIndex, isDragging=$isDragging")
        // Called by fragment when long press countdown completes
        // When inFolder is true, all items are apps (not folders), so allow long press
        touchedAppIndex?.let { appIndex ->
            if (!isDragging) {
                Log.d("RainbowPathTouch", "RainbowPathView.triggerLongPress: Setting longPressTriggered=true, calling onAppLongPressed($appIndex)")
                longPressTriggered = true
                eventListener?.onAppLongPressed(appIndex)
            } else {
                Log.d("RainbowPathTouch", "RainbowPathView.triggerLongPress: BLOCKED - isDragging=true")
            }
        } ?: run {
            Log.d("RainbowPathTouch", "RainbowPathView.triggerLongPress: BLOCKED - touchedAppIndex is null")
        }
    }
}
