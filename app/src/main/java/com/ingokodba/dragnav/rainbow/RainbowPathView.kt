package com.ingokodba.dragnav.rainbow

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.example.dragnav.R
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

    // Configuration
    var config: PathConfig = PathConfig()
        set(value) {
            field = value
            invalidate()
        }

    // App data
    private var appList: List<AppInfo> = emptyList()
    var icons: MutableMap<String, Drawable?> = mutableMapOf()
    var onlyFavorites: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // Scroll state
    private var scrollOffset: Float = 0f
    private var scrollVelocity: Float = 0f
    private var lastTouchY: Float = 0f
    private var lastTouchX: Float = 0f
    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var isDragging: Boolean = false
    private var isFling: Boolean = false

    // Fling physics
    private val flingFriction = 0.92f
    private val minFlingVelocity = 2f

    // Drawn app tracking for touch detection
    private data class DrawnAppInfo(
        val index: Int,
        val rect: RectF,
        val pathPosition: Float
    )
    private val drawnApps = mutableListOf<DrawnAppInfo>()

    // Letter index for fast scrolling
    private val letterPositions = mutableMapOf<Char, Int>()
    private var letterIndexRect: RectF? = null
    private var isInLetterIndex = false

    // Shortcuts
    private var shortcutsAppIndex: Int? = null
    var shortcuts: List<String> = emptyList()
    private val shortcutRects = mutableListOf<RectF>()

    // Event listener
    private var eventListener: EventListener? = null

    // Paints
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 1f, 2f, Color.BLACK)
    }
    private val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }
    private val favButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Debug mode
    var debugMode = true

    interface EventListener {
        fun onAppClicked(appIndex: Int)
        fun onAppLongPressed(appIndex: Int)
        fun onShortcutClicked(shortcutIndex: Int)
        fun onFavoritesToggled()
        fun onFlingStarted()
        fun onFlingEnded()
    }

    fun setEventListener(listener: EventListener) {
        eventListener = listener
    }

    fun setApps(apps: List<AppInfo>) {
        Log.d("RainbowPath", "setApps called with ${apps.size} apps")
        appList = apps
        updateLetterPositions()
        invalidate()
    }

    private fun getDisplayedApps(): List<AppInfo> {
        return if (onlyFavorites) {
            appList.filter { it.favorite }
        } else {
            appList
        }
    }

    private fun updateLetterPositions() {
        letterPositions.clear()
        val apps = getDisplayedApps()
        apps.forEachIndexed { index, app ->
            if (app.label.isNotEmpty()) {
                val letter = app.label[0].uppercaseChar()
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
        scrollOffset += scrollVelocity

        // Clamp scroll
        val apps = getDisplayedApps()
        if (apps.isNotEmpty()) {
            val maxScroll = (apps.size - 1) * config.appSpacing
            scrollOffset = scrollOffset.coerceIn(-maxScroll.coerceAtLeast(0f), 0f)
        }

        if (abs(scrollVelocity) < minFlingVelocity) {
            isFling = false
            eventListener?.onFlingEnded()
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawnApps.clear()
        shortcutRects.clear()

        val w = width.toFloat()
        val h = height.toFloat()

        // Update text sizes based on config
        textPaint.textSize = config.appNameSize * resources.displayMetrics.scaledDensity
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

        // Draw shortcuts popup if active
        if (shortcutsAppIndex != null && shortcuts.isNotEmpty()) {
            drawShortcutsPopup(canvas, w, h)
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
        Log.d("RainbowPath", "drawAppsOnPath: ${apps.size} apps, appList size: ${appList.size}")
        if (apps.isEmpty()) return

        val provider = PathShapeRegistry.getProvider(config.pathShape)
        val iconSizePx = config.appIconSize * w

        apps.forEachIndexed { index, app ->
            // Calculate position on path
            val baseT = index * config.appSpacing + scrollOffset
            val t = baseT.coerceIn(0f, 1f)

            // Skip if outside visible range
            if (baseT < -0.1f || baseT > 1.1f) return@forEachIndexed

            val point = provider.getPointOnPath(t, config.startPoint, config.endPoint, config)
            val screenX = point.x * w
            val screenY = (1 - point.y) * h

            if (index < 3) {
                Log.d("RainbowPath", "App $index: baseT=$baseT, t=$t, point=(${point.x}, ${point.y}), screen=($screenX, $screenY)")
            }

            // Create rect for this app
            val rect = RectF(
                screenX - iconSizePx / 2,
                screenY - iconSizePx / 2,
                screenX + iconSizePx / 2,
                screenY + iconSizePx / 2
            )

            drawnApps.add(DrawnAppInfo(index, rect, t))

            // Draw app icon
            val icon = icons[app.packageName]?.toBitmap()
            if (icon != null) {
                canvas.drawBitmap(icon, null, rect, iconPaint)
            } else {
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

            // Draw app name
            if (config.showAppNames) {
                canvas.drawText(
                    app.label,
                    screenX,
                    screenY + iconSizePx / 2 + textPaint.textSize + 4,
                    textPaint
                )
            }
        }
    }

    private fun drawLetterIndex(canvas: Canvas, w: Float, h: Float) {
        val letters = letterPositions.keys.sorted()
        if (letters.isEmpty()) return

        val indexWidth = config.letterIndexSize * w * 2
        val padding = config.letterIndexPadding * w

        val left = if (config.letterIndexPosition == LetterIndexPosition.RIGHT) {
            w - indexWidth - padding
        } else {
            padding
        }

        val letterHeight = h / letters.size.coerceAtLeast(1)
        letterIndexRect = RectF(left, 0f, left + indexWidth, h)

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

        // Draw star icon
        val starDrawable = AppCompatResources.getDrawable(
            context,
            if (onlyFavorites) R.drawable.star_fill else R.drawable.star_empty
        )
        starDrawable?.let {
            it.setTint(if (onlyFavorites) Color.YELLOW else Color.WHITE)
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = false
                isFling = false
                scrollVelocity = 0f

                // Check if touch is in letter index
                letterIndexRect?.let {
                    if (it.contains(event.x, event.y)) {
                        isInLetterIndex = true
                        handleLetterIndexTouch(event.y)
                        return true
                    }
                }

                // Check shortcuts popup
                if (shortcutsAppIndex != null) {
                    shortcutRects.forEachIndexed { index, rect ->
                        if (rect.contains(event.x, event.y)) {
                            eventListener?.onShortcutClicked(index)
                            hideShortcuts()
                            return true
                        }
                    }
                    // Touch outside popup - close it
                    hideShortcuts()
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isInLetterIndex) {
                    handleLetterIndexTouch(event.y)
                    return true
                }

                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                if (!isDragging && (abs(dx) > 10 || abs(dy) > 10)) {
                    isDragging = true
                }

                if (isDragging) {
                    // Scroll based on movement along path direction
                    val scrollDelta = (-dy + dx) / (height * config.appSpacing * 10)
                    scrollOffset += scrollDelta
                    scrollVelocity = scrollDelta * 10

                    // Clamp scroll
                    val apps = getDisplayedApps()
                    if (apps.isNotEmpty()) {
                        val maxScroll = (apps.size - 1) * config.appSpacing
                        scrollOffset = scrollOffset.coerceIn(-maxScroll.coerceAtLeast(0f), 0f)
                    }

                    invalidate()
                }

                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                isInLetterIndex = false

                if (!isDragging) {
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
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        eventListener?.onFavoritesToggled()
                        return true
                    }

                    // Check for tap on app
                    drawnApps.forEach { drawnApp ->
                        if (drawnApp.rect.contains(event.x, event.y)) {
                            eventListener?.onAppClicked(drawnApp.index)
                            return true
                        }
                    }
                } else {
                    // Start fling if velocity is high enough
                    if (abs(scrollVelocity) > minFlingVelocity) {
                        isFling = true
                        eventListener?.onFlingStarted()
                    }
                }

                isDragging = false
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun handleLetterIndexTouch(y: Float) {
        val letters = letterPositions.keys.sorted()
        if (letters.isEmpty()) return

        val letterHeight = height / letters.size
        val index = (y / letterHeight).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[index]

        letterPositions[letter]?.let { appIndex ->
            scrollOffset = -appIndex * config.appSpacing
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            invalidate()
        }
    }

    fun getAppIndexAtPosition(x: Float, y: Float): Int? {
        drawnApps.forEach { drawnApp ->
            if (drawnApp.rect.contains(x, y)) {
                return drawnApp.index
            }
        }
        return null
    }

    fun scrollToApp(index: Int) {
        scrollOffset = -index * config.appSpacing
        invalidate()
    }

    fun scrollToLetter(letter: Char) {
        letterPositions[letter.uppercaseChar()]?.let { appIndex ->
            scrollToApp(appIndex)
        }
    }
}
