package com.ingokodba.dragnav.rainbow

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.ingokodba.dragnav.EncapsulatedAppInfoWithFolder
import com.ingokodba.dragnav.SearchFragment
import kotlin.math.abs
import kotlin.math.min

/**
 * Glassy search overlay with floating search bar and app list
 */
class SearchOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        setWillNotDraw(false) // Ensure onDraw is called
        setBackgroundColor(Color.parseColor("#CC000000")) // Set background color (80% opacity black)
    }

    private var allApps: List<EncapsulatedAppInfoWithFolder> = emptyList()
    private var filteredApps: List<EncapsulatedAppInfoWithFolder> = emptyList()
    private var icons: Map<String, Drawable?> = emptyMap()
    
    private var searchBar: EditText? = null
    private var isVisible = false
    
    // Drawing constants
    private val searchBarHeight = 60f
    private val searchBarPadding = 24f
    private val searchBarCornerRadius = 30f
    private val itemHeight = 80f
    private val itemPadding = 16f
    private val iconSize = 56f
    private val maxVisibleItems = 8
    
    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC000000") // 80% opacity black
        style = Paint.Style.FILL
    }
    
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6FFFFFF") // 90% opacity white for glass effect
        style = Paint.Style.FILL
    }
    
    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF") // 50% opacity white border
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK // Black text for white/glassy background
        textSize = 40f
        textAlign = Paint.Align.LEFT
    }
    
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Touch handling
    private var scrollOffset = 0f
    private var lastTouchY = 0f
    private var isScrolling = false
    private var touchedItemIndex: Int? = null
    private var longPressTriggered = false
    
    // Keyboard detection
    private var lastHeight = 0
    private var keyboardVisible = false
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var keyboardDetectionEnabled = false
    private var keyboardDetectionStartTime = 0L
    private val keyboardDetectionDelay = 500L // 500ms delay before detecting keyboard state
    
    // Item rects for touch detection
    private data class ItemRect(val index: Int, val rect: RectF)
    private val itemRects = mutableListOf<ItemRect>()
    
    interface SearchOverlayListener {
        fun onAppClicked(app: EncapsulatedAppInfoWithFolder)
        fun onAppLongPressed(app: EncapsulatedAppInfoWithFolder)
        fun onDismiss()
    }
    
    private var listener: SearchOverlayListener? = null
    
    fun setListener(listener: SearchOverlayListener) {
        this.listener = listener
    }
    
    fun setApps(apps: List<EncapsulatedAppInfoWithFolder>) {
        allApps = apps
        filteredApps = apps
        scrollOffset = 0f
        invalidate()
    }
    
    fun setIcons(icons: Map<String, Drawable?>) {
        this.icons = icons
        invalidate()
    }
    
    fun show() {
        isVisible = true
        visibility = VISIBLE
        bringToFront() // Bring overlay to front
        requestFocus() // Request focus for the overlay
        
        // Disable keyboard detection initially and set start time
        keyboardDetectionEnabled = false
        keyboardDetectionStartTime = System.currentTimeMillis()
        lastHeight = 0 // Reset height tracking
        keyboardVisible = false
        
        // Wait for layout to complete before setting up search bar
        post {
            // Setup search bar if not already done
            if (searchBar == null && width > 0 && height > 0) {
                setupSearchBar()
            }
            
            invalidate() // Force redraw
            
            searchBar?.requestFocus()
            postDelayed({
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchBar, InputMethodManager.SHOW_IMPLICIT)
                // Mark keyboard as visible after showing it
                postDelayed({
                    keyboardVisible = true
                    lastHeight = height // Initialize height after keyboard appears
                }, 200) // Small delay to let keyboard appear
            }, 100) // Small delay to ensure layout is complete
        }
    }
    
    fun hide() {
        isVisible = false
        visibility = GONE
        keyboardVisible = false
        keyboardDetectionEnabled = false // Disable detection when hiding
        searchBar?.clearFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchBar?.windowToken, 0)
        searchBar?.setText("")
        filteredApps = allApps
        scrollOffset = 0f
        invalidate()
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupSearchBar()
        setupKeyboardListener()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeKeyboardListener()
    }
    
    private fun setupKeyboardListener() {
        removeKeyboardListener()
        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            // Only detect keyboard state after delay period
            if (!keyboardDetectionEnabled) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - keyboardDetectionStartTime >= keyboardDetectionDelay) {
                    keyboardDetectionEnabled = true
                } else {
                    // Still in delay period, just update height but don't detect
                    lastHeight = height
                    return@OnGlobalLayoutListener
                }
            }
            
            val currentHeight = height
            if (lastHeight > 0) {
                val heightDiff = lastHeight - currentHeight
                // Keyboard is visible if height decreased significantly (more than 200dp)
                val wasKeyboardVisible = keyboardVisible
                keyboardVisible = heightDiff > 200
                
                // If keyboard was visible and now it's not, close overlay
                if (wasKeyboardVisible && !keyboardVisible && isVisible && keyboardDetectionEnabled) {
                    listener?.onDismiss()
                }
            }
            lastHeight = currentHeight
        }
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }
    
    private fun removeKeyboardListener() {
        layoutListener?.let {
            viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        layoutListener = null
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (searchBar == null && w > 0 && h > 0) {
            setupSearchBar()
        }
    }
    
    private fun setupSearchBar() {
        if (searchBar != null) return
        if (width == 0 || height == 0) return // Wait for layout
        
        val searchBarWidth = (width * 0.8f).toInt().coerceAtMost((resources.displayMetrics.widthPixels * 0.8f).toInt())
        
        searchBar = EditText(context).apply {
            hint = "Search apps and folders..."
            setHintTextColor(Color.parseColor("#80000000")) // Dark hint text for white background
            setTextColor(Color.BLACK) // Black text for white background
            textSize = 16f
            background = null
            setPadding(
                (searchBarPadding + 24).toInt(), // More horizontal padding
                (searchBarHeight / 2 - 20).toInt(),
                (searchBarPadding + 24).toInt(), // More horizontal padding
                (searchBarHeight / 2 - 20).toInt()
            )
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s?.toString() ?: ""
                    performSearch(query)
                }
            })
            
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Select first result if available
                    if (filteredApps.isNotEmpty()) {
                        listener?.onAppClicked(filteredApps[0])
                        return@setOnEditorActionListener true
                    }
                }
                false
            }
        }
        
        val params = LayoutParams(
            searchBarWidth,
            searchBarHeight.toInt()
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            topMargin = (searchBarPadding * 2).toInt()
        }
        addView(searchBar, params)
    }
    
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            filteredApps = allApps
        } else {
            filteredApps = searchApps(query)
        }
        scrollOffset = 0f
        invalidate()
    }
    
    private fun searchApps(query: String): List<EncapsulatedAppInfoWithFolder> {
        val results = mutableListOf<Pair<Int, EncapsulatedAppInfoWithFolder>>()
        val queryLower = query.lowercase()
        
        for (item in allApps) {
            if (item.folderName != null) {
                // Search folder name using similar algorithm to app search
                item.folderName?.let{ folderName ->
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
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isVisible || width == 0 || height == 0) return
        
        val w = width.toFloat()
        val h = height.toFloat()
        
        // Draw search bar background (glassy effect) - positioned at top center
        val searchBarWidth = (w * 0.8f).coerceAtMost(resources.displayMetrics.widthPixels * 0.8f)
        val searchBarLeft = (w - searchBarWidth) / 2
        val searchBarTop = searchBarPadding * 2
        val searchBarRight = searchBarLeft + searchBarWidth
        val searchBarBottom = searchBarTop + searchBarHeight
        
        val searchBarRect = RectF(searchBarLeft, searchBarTop, searchBarRight, searchBarBottom)
        canvas.drawRoundRect(searchBarRect, searchBarCornerRadius, searchBarCornerRadius, glassPaint)
        canvas.drawRoundRect(searchBarRect, searchBarCornerRadius, searchBarCornerRadius, glassBorderPaint)
        
        // Draw app list
        drawAppList(canvas, w, h, searchBarBottom + searchBarPadding * 2)
    }
    
    private fun drawAppList(canvas: Canvas, w: Float, h: Float, startY: Float) {
        itemRects.clear()
        
        val visibleHeight = h - startY - searchBarPadding
        val maxItems = min(maxVisibleItems, (visibleHeight / itemHeight).toInt())
        
        val startIndex = (scrollOffset / itemHeight).toInt().coerceAtLeast(0)
        val endIndex = min(startIndex + maxItems + 1, filteredApps.size)
        
        var currentY = startY - (scrollOffset % itemHeight)
        
        for (i in startIndex until endIndex) {
            if (i >= filteredApps.size) break
            if (currentY + itemHeight < startY) {
                currentY += itemHeight
                continue
            }
            if (currentY > h) break
            
            val item = filteredApps[i]
            // Add more padding to items
            val itemHorizontalPadding = itemPadding + 16f
            val itemVerticalPadding = 8f
            val itemRect = RectF(
                itemHorizontalPadding,
                currentY + itemVerticalPadding,
                w - itemHorizontalPadding,
                currentY + itemHeight - itemVerticalPadding
            )
            
            itemRects.add(ItemRect(i, itemRect))
            
            // Draw item background (glassy effect)
            canvas.drawRoundRect(itemRect, 16f, 16f, glassPaint)
            canvas.drawRoundRect(itemRect, 16f, 16f, glassBorderPaint)
            
            // Draw icon with padding
            val iconLeft = itemHorizontalPadding + 16f
            val iconTop = currentY + itemVerticalPadding + (itemHeight - itemVerticalPadding * 2 - iconSize) / 2
            val iconRight = iconLeft + iconSize
            val iconBottom = iconTop + iconSize
            
            if (item.folderName != null) {
                // Draw folder icon (multiple small icons)
                val folderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#55FFFFFF")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(
                    iconLeft + iconSize / 2,
                    iconTop + iconSize / 2,
                    iconSize / 2,
                    folderPaint
                )
            } else if (item.apps.isNotEmpty()) {
                val app = item.apps.first()
                val icon = icons[app.packageName]?.toBitmap()
                if (icon != null) {
                    val iconRect = RectF(iconLeft, iconTop, iconRight, iconBottom)
                    canvas.drawBitmap(icon, null, iconRect, iconPaint)
                } else {
                    // Fallback: colored circle
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = try {
                            if (app.color.isNotEmpty()) app.color.toInt() else Color.GRAY
                        } catch (e: Exception) {
                            Color.GRAY
                        }
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(
                        iconLeft + iconSize / 2,
                        iconTop + iconSize / 2,
                        iconSize / 2,
                        paint
                    )
                }
            }
            
            // Draw text with padding
            val textX = iconRight + 20f
            val textY = currentY + itemVerticalPadding + (itemHeight - itemVerticalPadding * 2) / 2 + itemTextPaint.textSize / 3
            val text = if (item.folderName != null) {
                item.folderName ?: ""
            } else if (item.apps.isNotEmpty()) {
                item.apps.first().label
            } else {
                ""
            }
            canvas.drawText(text, textX, textY, itemTextPaint)
            
            currentY += itemHeight
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                isScrolling = false
                longPressTriggered = false
                touchedItemIndex = null
                
                // Check if touch is on an item
                for (itemRect in itemRects) {
                    if (itemRect.rect.contains(event.x, event.y)) {
                        touchedItemIndex = itemRect.index
                        // Start long press countdown
                        postDelayed({
                            if (touchedItemIndex == itemRect.index && !isScrolling && !longPressTriggered) {
                                longPressTriggered = true
                                if (itemRect.index < filteredApps.size) {
                                    listener?.onAppLongPressed(filteredApps[itemRect.index])
                                }
                            }
                        }, 500)
                        return true
                    }
                }
                
                // Check if touch is outside search bar and items - dismiss
                val searchBarTop = searchBarPadding * 2
                val searchBarBottom = searchBarTop + searchBarHeight
                val searchBarWidth = (width * 0.8f).coerceAtMost(resources.displayMetrics.widthPixels * 0.8f)
                val searchBarLeft = (width - searchBarWidth) / 2
                val searchBarRight = searchBarLeft + searchBarWidth
                
                val isInSearchBar = event.y >= searchBarTop && event.y <= searchBarBottom &&
                                   event.x >= searchBarLeft && event.x <= searchBarRight
                
                // Check if touch is on any item (using the actual drawn rects)
                val isOnItem = itemRects.any { it.rect.contains(event.x, event.y) }
                
                if (!isInSearchBar && !isOnItem) {
                    listener?.onDismiss()
                    return true
                }
                
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - lastTouchY
                
                if (abs(dy) > 10f) {
                    isScrolling = true
                    touchedItemIndex = null
                    
                    // Scroll the list
                    val maxScroll = (filteredApps.size - maxVisibleItems).coerceAtLeast(0) * itemHeight
                    scrollOffset = (scrollOffset - dy).coerceIn(0f, maxScroll.toFloat())
                    invalidate()
                }
                
                lastTouchY = event.y
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (!isScrolling && !longPressTriggered && touchedItemIndex != null) {
                    // Click on item
                    if (touchedItemIndex!! < filteredApps.size) {
                        listener?.onAppClicked(filteredApps[touchedItemIndex!!])
                    }
                }
                
                isScrolling = false
                touchedItemIndex = null
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isVisible) {
            listener?.onDismiss()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

