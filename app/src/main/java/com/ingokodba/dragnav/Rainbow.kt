package com.ingokodba.dragnav

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.contains
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.google.gson.Gson
import com.ingokodba.dragnav.CircleView.Companion.colorToHex
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MiddleButtonStates
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class Rainbow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs){

    private var middleButtonState:MiddleButtonStates = MIDDLE_BUTTON_HIDE

    // Paint object for coloring and styling
    private val text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val smaller_text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shortcuts_text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val no_icon_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val empty_circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thickLine = Paint(Paint.ANTI_ALIAS_FLAG)
    val clear_boja = Paint(Paint.ANTI_ALIAS_FLAG)
    //private val yellow_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thick_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val black_stroke_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shortcut_indicator_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val favorite_indicator_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Some colors for the face background, eyes and mouth.
    private var yellowColor = Color.YELLOW
    private var middleButtonColor = Color.WHITE
    private var closeInsideColor = Color.parseColor("#aaff0000")
    // View size in pixels
    private var size_width = 320
    private var size_height = 320
    private var app_list:List<EncapsulatedAppInfoWithFolder> = listOf()
    private var color_list:List<String> = listOf()
    var questiondrawable: Bitmap? = null
    var homeDrawable: Bitmap? = null
    var bitmap: Bitmap? = null

    var gcolor = Color.parseColor("#FBB8AC")
    var draw_circles = true
    var draw_icons = true
    var color_on_primary = false
    var border_width = 4f
    var text_size = 18f
    var smaller_text_size = 18f
    var transparency = 0.8f
    var shadow_toggle = true
    var show_app_names = true
    var showBigCircle = false
    var view: View = this

    var shortcuts: List<String> = listOf()
    var shortcuts_app: Int? = null
    var shortcuts_rects: MutableList<Rect> = mutableListOf()

    var quickSwipeAngles: MutableList<Float> = mutableListOf()
    var quickSwipeEntered: Boolean = false
    var currentQuickSwipeLetter: Char? = null

    var highestIconAngleDrawn: Double = 0.0

    var drawn_apps: MutableList<DrawnApp> = mutableListOf()

    var inFolder: Boolean = false
    var editMode:Boolean = false
    var addAppMode:Boolean = false
    var icons:MutableMap<String, Drawable?> = mutableMapOf()

    var mShowText:Boolean
    var textPos:Int
    var selected:Int = -1
    var amIHomeVar: Boolean = true

    var overrideDetectSize: Float? = null
    var overrideDistance: Float? = null
    var overrideStep: Float? = null
    var overrideRadius: Float? = null
    var overrideArcPosition: Float? = null
    var onlyfavorites: Boolean = false

    var detectSizeDivider = 1.0
    var detectSize = 0
    var offset: Float = 0f
    var app_index: Int = 0
    val queued_texts: MutableList<QueuedText> = mutableListOf()

    var clickIgnored = false
    var clickProcessed = false

    var favcirclerect: Rect? = null
    var limit: Double = 0.0

    var flingMinVelocity = 20f
    var flingMaxVelocity = 500f
    var flingFriction = 2
    var flingValue = 0f
    var flingValueAccumulated = 0f
    var flingOn = false

    var lastTouchPoints: MutableList<Point> = mutableListOf()
    var hasMoved = false
    var leftOrRight: Boolean = true

    fun limit(number: Int): Boolean{
        limit = -(app_list.size/2)*step_size - step_size*2
        //Log.d("ingo", "limit $limit number $number divided ${(number) / 500f - Math.PI / 2}")
        if(limit < -Math.PI/2) {
            return (number > 0 || (number) / 500f - Math.PI / 2 < limit)
        }
        return true
    }
    fun flingUpdate(){
        if(!flingOn) return
        //Log.d("ingo", "fling update $flingValue $flingFriction")
        flingValue -= if (flingValue > 0) flingFriction else -flingFriction
        if(abs(flingValue) <= flingFriction || limit((flingValueAccumulated + flingValue + moveDistancedAccumulated).toInt())){
            finishFling()
            return
        }
        // moveSpeed is already directionally adjusted for left-handed layouts on line 493,
        // so flingValue already has the correct sign - don't negate it again
        flingValueAccumulated += flingValue
        moveAction(flingValueAccumulated.toInt())
    }

    fun finishFling(){
        flingOn = false
        moveDistancedAccumulated += flingValueAccumulated.toInt()
        resetClicks()
    }

    fun setColor(agcolor:String){
        try {
            gcolor = agcolor.toInt()
            Log.d("ingo", "color set to " + agcolor)
            invalidate()
        } catch (e:NumberFormatException){
            e.printStackTrace()
        }
    }

    fun showShortcuts(app_i: Int, shortcuts: List<String>){
        this.shortcuts = shortcuts
        this.shortcuts_app = app_i
        clickIgnored = true
        invalidate()
    }

    fun deselectAll(){
        selected = -1
        amIHome(amIHomeVar)
        if(editMode){
            //changeMiddleButtonState(MIDDLE_BUTTON_EDIT)
        } else if(amIHomeVar) {
            changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
        } else {
            changeMiddleButtonState(MIDDLE_BUTTON_HOME)
        }
        invalidate()
    }

    fun getFirstLetterOfApp(app: EncapsulatedAppInfoWithFolder): Char{
        val name: String = try {
            if(app.folderName != null && app.folderName!!.isNotEmpty()) {
                app.folderName!!
            } else if(app.apps.isNotEmpty()) {
                try {
                    val firstApp = app.apps.first()
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

    fun getNameOfApp(app: EncapsulatedAppInfoWithFolder): String{
        return try {
            if(app.folderName != null && app.folderName!!.isNotEmpty()) {
                app.folderName!!
            } else if(app.apps.isNotEmpty()) {
                try {
                    val firstApp = app.apps.first()
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

    fun setAppInfoList(list:List<EncapsulatedAppInfoWithFolder>){
        app_list = list
        var firstLetter = '0'
        var counter = 0
        for(app in app_list){
            val firstLetterOfApp = getFirstLetterOfApp(app)
            if(firstLetterOfApp > firstLetter){
                firstLetter = firstLetterOfApp
                //Log.d("ingo", "${Gson().toJson(app)} $counter $firstLetter")
            }
            counter++
        }
        invalidate()
    }

    init {
        questiondrawable = AppCompatResources.getDrawable(context, R.drawable.baseline_question_mark_24)?.toBitmap()
        homeDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_home_100)?.toBitmap()
        updateDesign()
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircleView,
            0, 0).apply {
            try {
                mShowText = getBoolean(R.styleable.CircleView_showText, false)
                textPos = getInteger(R.styleable.CircleView_labelPosition, 0)
            } finally {
                recycle()
            }
        }
        text_paint.color = Color.WHITE
        text_paint.textAlign = Paint.Align.CENTER
        text_paint.style = Paint.Style.FILL
        smaller_text_paint.color = Color.WHITE
        smaller_text_paint.textAlign = Paint.Align.CENTER
        smaller_text_paint.style = Paint.Style.FILL
        smaller_text_paint.setShadowLayer(4.0f, 1.0f, 2.0f, Color.BLACK);
        shortcuts_text_paint.color = Color.BLACK
        shortcuts_text_paint.textAlign = Paint.Align.CENTER
        shortcuts_text_paint.style = Paint.Style.FILL
        //shortcuts_text_paint.setShadowLayer(4.0f, 1.0f, 2.0f, Color.BLACK);
        black_stroke_paint.color = Color.BLACK
        black_stroke_paint.style = Paint.Style.STROKE
        black_stroke_paint.strokeWidth = 5f
        thickLine.color = closeInsideColor
        thickLine.style = Paint.Style.FILL_AND_STROKE
        empty_circle_paint.style = Paint.Style.STROKE
        circle_paint.style = Paint.Style.FILL
        no_icon_paint.style = Paint.Style.FILL
        shortcut_indicator_paint.style = Paint.Style.FILL
        shortcut_indicator_paint.color = Color.RED
        favorite_indicator_paint.style = Paint.Style.FILL
        favorite_indicator_paint.color = Color.parseColor("#61b8ed")
        thick_paint.color = middleButtonColor
        thick_paint.style = Paint.Style.STROKE
        thick_paint.strokeWidth = 20f
        clear_boja.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        //overrideDistance = PreferenceManager.getDefaultSharedPreferences(context).getFloat("distance", 0f)//.let { if(it != 0f) it else null }
        //overrideDetectSize = PreferenceManager.getDefaultSharedPreferences(context).getFloat("detectSize", 0f)//.let { if(it != 0f) it else null }
        Log.d("ingo", "overrideDistance $overrideDistance")
        Log.d("ingo", "overrideDetectSize $overrideDetectSize")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 1
        size_width = measuredWidth
        size_height = measuredHeight
    }

    override fun onDraw(canvas: Canvas) {
        // call the super method to keep any drawing from the parent side.
        super.onDraw(canvas)
        // settings
        empty_circle_paint.color = gcolor
        circle_paint.color = gcolor
        if(shadow_toggle) {
            empty_circle_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK)
            //circle_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);
            //thick_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);
        } else {
            empty_circle_paint.setShadowLayer(0.0f, 1.0f, 2.0f, Color.BLACK)
            circle_paint.setShadowLayer(0.0f, 1.0f, 2.0f, Color.BLACK);
            thick_paint.setShadowLayer(0.0f, 1.0f, 2.0f, Color.BLACK);
        }
        black_stroke_paint.strokeWidth = border_width
        thickLine.strokeWidth = border_width
        empty_circle_paint.strokeWidth = border_width
        circle_paint.strokeWidth = border_width
        no_icon_paint.strokeWidth = border_width

        drawPolja(canvas)
        //drawEditButton(canvas)
        //drawCloseButton(canvas)
    }

    private var mEventListener: MainFragmentRainbow.IMyEventListener? = null

    fun setEventListener(mEventListener: MainFragmentRainbow.IMyEventListener?) {
        this.mEventListener = mEventListener
    }

    fun amIHome(ami:Boolean?){
        if(ami != null) amIHomeVar=ami
        if(!addAppMode) {
            if (editMode) {
                //changeMiddleButtonState(MIDDLE_BUTTON_EDIT)
            } else if (amIHomeVar) {
                changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
            } else {
                changeMiddleButtonState(MIDDLE_BUTTON_HOME)
            }
        }
    }

    var touchStart: Point = Point(0, 0)
    var moveDistance = 0
    var moveDistancedAccumulated = 0
    var startedMoving: Boolean = false

    fun changeMiddleButtonState(state:MiddleButtonStates){
        middleButtonState = state
        invalidate()
    }

    fun getAppIndexImIn(): Int?{
        for(draw_app in drawn_apps){
            if(draw_app.rect.contains(touchStart)){
                return draw_app.app_index
            }
        }
        return null
    }

    fun updateDesign(){
        val gcolor = PreferenceManager.getDefaultSharedPreferences(context).getString(MySettingsFragment.UI_COLOR, "-1")
        if (gcolor != null) {
            setColor(gcolor)
        }
        val circles = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MySettingsFragment.UI_CIRCLES_TOGGLE, true)
        draw_circles = circles
        val icons = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MySettingsFragment.UI_ICONS_TOGGLE, true)
        draw_icons = icons
        val shadow = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MySettingsFragment.UI_SHADOW_TOGGLE, true)
        shadow_toggle = shadow
        show_app_names = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MySettingsFragment.UI_SHOW_APP_NAMES, true)
        showBigCircle = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MySettingsFragment.UI_BIG_CIRCLE, true)
        text_size = PreferenceManager.getDefaultSharedPreferences(context).getString(MySettingsFragment.UI_TEXT_SIZE, "50")!!.toFloat()
        color_on_primary = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(MySettingsFragment.UI_COLOR_ON_PRIMARY, false)
        smaller_text_size = PreferenceManager.getDefaultSharedPreferences(context).getString(MySettingsFragment.UI_SMALLER_TEXT_SIZE, "30")!!.toFloat()
        transparency = PreferenceManager.getDefaultSharedPreferences(context).getString(MySettingsFragment.UI_TRANSPARENCY, "1")!!.toFloat()
        text_paint.textSize = text_size;
        smaller_text_paint.textSize = smaller_text_size;
        shortcuts_text_paint.textSize = smaller_text_size;
        val border_width1 = PreferenceManager.getDefaultSharedPreferences(context).getString(MySettingsFragment.UI_BORDER_WIDTH, "4")
        try {
            if (border_width1 != null) {
                border_width = border_width1.toFloat()
            }
        } catch(e:NumberFormatException){
            border_width = 4f
            e.printStackTrace()
        }
    }

    fun quickMove(point: Point){
        if(app_list.size == drawn_apps.size){
            moveDistancedAccumulated = 0
            invalidate()
            return
        }
        limit = -(app_list.size/2)*step_size - step_size*2
        var angle = -Math.PI/2-atan2((point.y-size_height).toDouble(),
            (point.x - if(leftOrRight) size_width else 0).toDouble()
        )
        if(!leftOrRight) angle = -angle
        var click = 0
        clickProcessed = true
        for(i in quickSwipeAngles.reversed().indices){
            if(angle > quickSwipeAngles[i]){
                click = i
            }
        }
        val firstByThatLetter = app_list.firstOrNull{getFirstLetterOfApp(it) == getFirstLetterOfApp(app_list.distinctBy { getFirstLetterOfApp(it) }[click])}
            ?: return

        // Update current letter for quick swipe indicator
        currentQuickSwipeLetter = getFirstLetterOfApp(firstByThatLetter).uppercaseChar()

        Log.d("ingo", "angle $angle ${Gson().toJson(firstByThatLetter)} $click $quickSwipeAngles")
        // saznati s kulko moramo pomnožiti
        val futureFoveDistancedAccumulated = -(app_list.indexOf(firstByThatLetter)*step_size*250).toInt()
        Log.d("ingo", "lol $moveDistancedAccumulated $limit")

        // prešli smo preko
        if ((futureFoveDistancedAccumulated) / 500f - Math.PI / 2 < limit) {
            moveDistancedAccumulated = ((limit + Math.PI / 2) * 500f).toInt()
        } else {
            moveDistancedAccumulated = futureFoveDistancedAccumulated
        }

        invalidate()
        Log.d("ingo", "lol $moveDistancedAccumulated ${app_list.indexOf(firstByThatLetter)}")
    }

    fun checkForQuickMove(point: Point){
        val distance = Math.sqrt((point.y - size_height).toDouble().pow(2) + (point.x - if(leftOrRight) size_width else 0).toDouble().pow(2) )
        Log.d("ingo", "distance $distance ${radius*1.15f} $detectSize")
        if(distance >= radius*1.15f){
            quickSwipeEntered = true
            quickMove(point)
        }
    }

    fun isTouchOnApp(): Boolean {
        for(draw_app in drawn_apps){
            if(draw_app.rect.contains(touchStart)){
                return true
            }
        }
        return false
    }

    fun startOpenShortcutCountdown(){
        if(isTouchOnApp()) {
            mEventListener?.onEventOccurred(EventTypes.START_COUNTDOWN, 2)
        }
        // Don't trigger sliders toggle from Rainbow view - only top_touch_area should do that
    }

    fun moveAction(change: Int){
        limit = -(app_list.size/2)*step_size - step_size*2
        if(!hasMoved && abs(change) > detectSize/2f){
            mEventListener?.onEventOccurred(EventTypes.STOP_COUNTDOWN, 2)
            hasMoved = true
        }
        if(limit < -Math.PI/2 && (startedMoving || abs(change) > detectSize/4f)) {
            // we're moving
            startedMoving = true
            Log.d("ingo", "we're moving")
            moveDistance = change
            if(abs(moveDistance) > detectSize/4) clickIgnored = true
            if (moveDistance + moveDistancedAccumulated > 0) {
                moveDistance = -moveDistancedAccumulated
            } else if ((moveDistance + moveDistancedAccumulated) / 500f - Math.PI / 2 < limit) {
                //Log.d("ingo", "zaustavljam ${offset-Math.PI/2} ${limit}")
                //offset = (moveDistancedAccumulated+moveDistance)/500f
                moveDistance = ((limit + Math.PI / 2) * 500f - moveDistancedAccumulated).toInt()
            }
            invalidate()
        } else {
            Log.e("ingo", "we're NOT moving")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN){
            if(flingOn) finishFling()
            lastTouchPoints.add(Point(event.x.toInt(), event.y.toInt()))
            touchStart = Point(event.x.toInt(), event.y.toInt())
            // only if inside app
            startOpenShortcutCountdown()
            // if clicked on opened shortcut menu
            if(shortcuts_app != null) {
                for (i in shortcuts_rects.indices) {
                    if (shortcuts_rects[i].contains(touchStart)) {
                        //mEventListener?.onEventOccurred(EventTypes.OPEN_SHORTCUT, i)
                        clickProcessed = true
                    }
                }
                shortcuts = mutableListOf()
                shortcuts_app = null
                invalidate()
            // check if clicked on fav
            } else if(favcirclerect != null && favcirclerect!!.contains(touchStart)){
                mEventListener?.onEventOccurred(EventTypes.TOGGLE_FAVORITES, 1)
            } else {
                checkForQuickMove(touchStart)
            }
        } else if(event.action == MotionEvent.ACTION_MOVE){
            if(quickSwipeEntered) {
                quickMove(Point(event.x.toInt(), event.y.toInt()))
            } else if(!clickProcessed){
                val change = if(leftOrRight){
                    (event.y - touchStart.y).toInt() - (event.x - touchStart.x).toInt()
                } else {
                    (event.y - touchStart.y).toInt() - (touchStart.x - event.x).toInt()
                }
                lastTouchPoints.add(Point(event.x.toInt(), event.y.toInt()))
                // Limit stored touch points to prevent memory growth
                if (lastTouchPoints.size > 10) {
                    lastTouchPoints.removeAt(0)
                }
                moveAction(change)
            }
            //Log.d("ingo", "ne zaustavljam ${(moveDistance+moveDistancedAccumulated)/500f-Math.PI/2} ${limit}")

        } else if(event.action == MotionEvent.ACTION_UP){
            startedMoving = false
            //Log.d("ingo", "moveDistance $moveDistance $moveDistancedAccumulated $step_size")
            if(!clickProcessed && !clickIgnored && abs(event.x - touchStart.x) + abs(event.y - touchStart.y) < detectSize/4){
                val app_i = getAppIndexImIn()
                if(app_i != null) mEventListener?.onEventOccurred(EventTypes.OPEN_APP, app_i)
            }
            if(!quickSwipeEntered && lastTouchPoints.size > 3){
                val moveSpeed = if(leftOrRight) {
                    ((event.y - lastTouchPoints[lastTouchPoints.size-4].y).toInt() - (event.x - lastTouchPoints[lastTouchPoints.size-4].x).toInt())/3
                } else {
                    -((event.y - lastTouchPoints[lastTouchPoints.size-4].y).toInt() - (lastTouchPoints[lastTouchPoints.size-4].x - event.x).toInt())/3
                }
                Log.d("ingo", "moveSpeed " + moveSpeed)
                if(abs(moveSpeed) > flingMinVelocity){
                    flingOn = true
                    flingValue = moveSpeed.toFloat()
                    if(abs(moveSpeed) >= flingMaxVelocity){
                        flingValue = if(flingValue > 0) flingMaxVelocity else -flingMaxVelocity
                    }
                    mEventListener?.onEventOccurred(EventTypes.START_FLING, 0)
                }
            }
            mEventListener?.onEventOccurred(EventTypes.STOP_COUNTDOWN, 2)
            moveDistancedAccumulated += moveDistance
            resetClicks()
        }
        return true
    }

    fun resetClicks(){
        moveDistance = 0
        clickIgnored = false
        clickProcessed = false
        quickSwipeEntered = false
        currentQuickSwipeLetter = null
        hasMoved = false
        flingValueAccumulated = 0f
        lastTouchPoints.clear()
    }

    var step_size:Double = Math.PI*0.05
    var radius = size_width/1.3f
    var arcPositionOffset: Float = 0f
    private fun drawPolja(canvas: Canvas){
        queued_texts.clear()
        val radiusMultiplier = overrideRadius ?: 1f
        radius = size_width / 1.35f * radiusMultiplier
        arcPositionOffset = (overrideArcPosition ?: 0f) * size_width
        if(overrideStep != null) step_size = overrideStep!!.toDouble()
        detectSizeDivider = 10.0
        detectSize = if(overrideDetectSize != null) overrideDetectSize!!.toInt() else 50
        quickSwipeAngles.clear()
        drawn_apps.clear()
        shortcuts_rects.clear()
        highestIconAngleDrawn = 0.0
        val inner_radius2 = (if(overrideDistance != null) overrideDistance!! else 0.87f)
        canvas.apply {
            // draw inner button
            val xx = (sin(Math.PI*5f/4f) * radius*inner_radius2/2.5f).toFloat()
            val yy = (cos(Math.PI*5f/4f) * radius*inner_radius2/2.5f).toFloat()
            val draw_pointF = PointF( (if(leftOrRight) (size_width+xx) else -xx), ((size_height+yy)) )
            /*val rect = Rect(
                (draw_pointF.x - detectSize).toInt(),
                (draw_pointF.y - detectSize).toInt(),
                (draw_pointF.x + detectSize).toInt(),
                (draw_pointF.y + detectSize).toInt()
            )*/
            drawCircle(draw_pointF.x, draw_pointF.y, radius*inner_radius2/6f, circle_paint)

            val drawable: Drawable? = AppCompatResources.getDrawable(context, if(inFolder) R.drawable.ic_baseline_arrow_back_24 else if(onlyfavorites) R.drawable.star_fill else R.drawable.star_empty)
            drawable?.setTint(if(!color_on_primary) Color.BLACK else Color.WHITE)
            val bitmap: Bitmap? = drawable?.toBitmap()
            if (bitmap != null) {
                drawBitmap(
                    bitmap, null, Rect(
                        (draw_pointF.x - radius*inner_radius2/6f / 2f).toInt(),
                        (draw_pointF.y - radius*inner_radius2/6f / 2f).toInt(),
                        (draw_pointF.x + radius*inner_radius2/6f / 2f).toInt(),
                        (draw_pointF.y + radius*inner_radius2/6f / 2f).toInt(),
                    ), thick_paint
                )
            }
            val rect2 = Rect(
                (draw_pointF.x - radius*inner_radius2/6f).toInt(),
                (draw_pointF.y - radius*inner_radius2/6f).toInt(),
                (draw_pointF.x + radius*inner_radius2/6f).toInt(),
                (draw_pointF.y + radius*inner_radius2/6f).toInt(),
            )
            favcirclerect = rect2
            //if (showBigCircle) drawCircle(size.toFloat(), size.toFloat(), radius, empty_circle_paint)

            var currentStepValue: Double = Math.PI
            offset = (moveDistancedAccumulated+moveDistance)/500f

            //drawText((moveDistancedAccumulated+moveDistance).toString(), (size/2).toFloat(), 50F, text_paint)
            //Log.d("ingo", "racunamo " + (app_list.size/2)*step_size + ", $offset, $moveDistancedAccumulated")

            var counter = 0
            val offsetModulated = offset%step_size
            val offsetDivided = offset/step_size

            val offset2 = offset+(step_size/2f).toFloat()
            val offsetDivided2 = offset2/step_size
            val offsetModulated2 = offset2%step_size

            while(currentStepValue.compareTo(Math.PI) >= 0 && currentStepValue.compareTo(Math.PI*(3f/2f)) < 0) {
                var drawn = false
                currentStepValue += step_size

                app_index = ((counter-offsetDivided.toInt())*2)
                if(app_index >= 0 && app_index < app_list.size){
                    //Log.d("ingo", "break $app_index")
                    drawApp2(app_index, currentStepValue+offsetModulated, 1f, this)
                    drawn = true
                }

                app_index = ((counter-offsetDivided2.toInt())*2+1)
                if(app_index >= 0 && app_index < app_list.size){
                    //Log.d("ingo", "break $app_index")
                    drawApp2(app_index, currentStepValue+offsetModulated2, inner_radius2, this)
                    drawn = true
                }
                counter++
                if(!drawn) break
            }

            val lineRadius = radius*1.15f
            val rectf = RectF(if(leftOrRight) size_width-lineRadius else -lineRadius, size_height-lineRadius, if(leftOrRight) size_width+lineRadius else lineRadius, size_height+lineRadius)
            drawn_apps.sortBy { it.startTrig }
            if(drawn_apps.size == 0) return

            val appsGroupedByLetter = app_list.distinctBy { getFirstLetterOfApp(it) }
            val divider = 90f/appsGroupedByLetter.size
            val drawnAppsCountByLetter = drawn_apps.groupingBy { it.letter.uppercaseChar() }.eachCount()
            for(i in appsGroupedByLetter.indices){
                quickSwipeAngles.add(Math.toRadians((divider*i).toDouble()).toFloat())
                //val first_drawn_app = drawn_apps.first().letter.uppercaseChar() == appsGroupedByLetter[i].label[0].uppercaseChar()
                empty_circle_paint.strokeWidth = border_width + border_width*4*(drawn_apps.filter { it.letter.uppercaseChar() == getFirstLetterOfApp(appsGroupedByLetter[i]) }.size.toFloat()/drawn_apps.size.toFloat())
                if(leftOrRight){
                    drawArc(rectf,
                        (-90-divider*i), -divider/1.2f, false, empty_circle_paint)
                } else {
                    drawArc(rectf,
                        (-90+divider*i), divider/1.2f, false, empty_circle_paint)
                }

                val point = myDraw(Math.toRadians(180+divider*i+divider/3.0), radius*1.3f)
                canvas.drawText(getFirstLetterOfApp(appsGroupedByLetter[i]).toString(), point.x, point.y+text_size/2, text_paint)
            }

            for(queued_text in queued_texts){
                drawText(queued_text.text, queued_text.x, queued_text.y, smaller_text_paint)
            }

            if(shortcuts_app != null){
                val shortcut_app_rect =
                    drawn_apps.firstOrNull { it.app_index == shortcuts_app }?.rect
                if (shortcut_app_rect != null) {
                    val rect_height = smaller_text_size * 5
                    val center =
                        shortcut_app_rect.right - (shortcut_app_rect.right - shortcut_app_rect.left) / 2
                    if (shortcuts.isNotEmpty()) {
                        //drawCircle(size_width/2f, size_height/2f, 100f, circle_paint)
                        val shortcuts_rect = Rect(
                            center - 200,
                            (shortcut_app_rect.top - (shortcuts.size) * rect_height).toInt(),
                            center + 200,
                            shortcut_app_rect.top
                        )
                        drawRect(shortcuts_rect, circle_paint)
                        for (i in shortcuts.indices) {
                            val recttmp = Rect(
                                shortcuts_rect.left,
                                (shortcut_app_rect.top - i * rect_height - rect_height).toInt(),
                                shortcuts_rect.right,
                                (shortcut_app_rect.top - i * rect_height).toInt()
                            )
                            val textxy = PointF(
                                center.toFloat(),
                                recttmp.bottom - rect_height / 2f + smaller_text_size / 2f
                            )
                            drawText(
                                shortcuts[i] as String,
                                textxy.x,
                                textxy.y,
                                shortcuts_text_paint
                            )

                            shortcuts_rects.add(recttmp)
                            drawRect(recttmp, black_stroke_paint)
                        }
                    }
                }
            }
        }
    }

    fun drawApp2(index: Int, triang: Double, radiusScale: Float, canvas: Canvas){
        //Log.d("ingo", "aplikacija " + app_list[index].label + " " + index)

        //Log.d("ingo", "crtam " + Gson().toJson(bitmap))

        if(triang > highestIconAngleDrawn) highestIconAngleDrawn = triang

        var xx = (sin(triang) * radius*radiusScale).toFloat()
        var yy = (cos(triang) * radius*radiusScale).toFloat()
        var draw_pointF = PointF( (if(leftOrRight) (size_width+xx) else -xx), ((size_height+yy).toFloat()) )
        val rect = Rect(
            (draw_pointF.x - detectSize).toInt(),
            (draw_pointF.y - detectSize).toInt(),
            (draw_pointF.x + detectSize).toInt(),
            (draw_pointF.y + detectSize).toInt()
        )
        drawn_apps.add(DrawnApp(triang, getFirstLetterOfApp(app_list[index]), index, rect))

        if(smaller_text_size != 0f && show_app_names){
            queued_texts.add(QueuedText(getNameOfApp(app_list[index]), draw_pointF.x, draw_pointF.y+detectSize+smaller_text_size))
        }
        if(app_list[index].folderName == null && app_list[index].apps.first().hasShortcuts) {
            canvas.drawCircle(
                rect.right.toFloat(),
                rect.bottom.toFloat(), detectSize / 8f, shortcut_indicator_paint
            )
        }
        if((app_list[index].folderName == null && app_list[index].apps.first().favorite) || (app_list[index].folderName != null && app_list[index].favorite == true)) {
            canvas.drawCircle(
                rect.left.toFloat(),
                rect.bottom.toFloat(), detectSize / 8f, favorite_indicator_paint
            )
        }
        if(app_list[index].folderName != null)
        {
            no_icon_paint.color = Color.parseColor("#55FFFFFF")
            canvas.drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), no_icon_paint)
            for(i in 0..1){
                for(j in 0..1){
                    //Log.d("ingo", "i: $i j: $j")
                    if(i*2 + j >= app_list[index].apps.size) break
                    val packageName = app_list[index].apps[i*2+j].packageName
                    val drawable = icons[packageName]
                    bitmap = drawable?.toBitmap()
                    Log.d("ingo", "Folder icon for $packageName: drawable=${drawable != null}, bitmap=${bitmap != null}, draw_icons=$draw_icons, icons map size=${icons.size}, icons identity=${System.identityHashCode(icons)}")
                    if(bitmap != null && draw_icons) {
                        canvas.drawBitmap(
                            bitmap!!, null, Rect(
                                (draw_pointF.x - detectSize + detectSize * j).toInt(),
                                (draw_pointF.y - detectSize + detectSize * i).toInt(),
                                (draw_pointF.x + detectSize * j).toInt(),
                                (draw_pointF.y + detectSize * i).toInt()
                            ), null
                        )
                    } else {
                        val boja = app_list[index].apps[i*2+j].color
                        Log.d("ingo", "boja za ${app_list[index].apps.first().label} je $boja")
                        try {
                            //val transcolor = colorToHex(Color.valueOf(app_list[index].color.toInt())) + transparenthex
                            if (boja != ""){
                                val boja = Color.valueOf(boja.toInt())
                                Log.d("ingo", "transparent " + boja + " " + "#FF" + colorToHex(boja))
                                no_icon_paint.color = Color.parseColor("#FF" + colorToHex(boja))
                                //Log.d("ingo", "boja je ${no_icon_paint.color}")
                            } else {
                                no_icon_paint.color = Color.parseColor("#55000000")
                                //Log.d("ingo", "boja! je ${no_icon_paint.color}")
                            }
                        } catch (e: NumberFormatException ){
                            no_icon_paint.color = Color.parseColor("#55000000")
                            //Log.d("ingo", "boja nemoguće za dešifrirati1")
                        } catch (e: IllegalArgumentException ){
                            no_icon_paint.color = Color.parseColor("#55000000")
                            //Log.d("ingo", "boja nemoguće za dešifrirati2")
                        }
                        val rec = Rect(
                            (draw_pointF.x - detectSize + detectSize * j).toInt(),
                            (draw_pointF.y - detectSize + detectSize * i).toInt(),
                            (draw_pointF.x + detectSize * j).toInt(),
                            (draw_pointF.y + detectSize * i).toInt()
                        )
                        canvas.drawCircle(rec.exactCenterX(), rec.exactCenterY(), detectSize/2f, no_icon_paint)
                    }
                }
            }
        } else {
            bitmap = if(app_list[index].folderName == null) icons[app_list[index].apps.first().packageName]?.toBitmap() else null
            if(bitmap != null && draw_icons) {
                canvas.drawBitmap(
                    bitmap!!, null, rect, null
                )
            } else {
                Log.d("ingo", "boja za ${app_list[index].apps.first().label} je ${app_list[index].apps.first().color}")
                try {
                    //val transcolor = colorToHex(Color.valueOf(app_list[index].color.toInt())) + transparenthex
                    if (app_list[index].apps.first().color != ""){
                        val boja = Color.valueOf(app_list[index].apps.first().color.toInt())
                        //Log.d("ingo", "transparent " + boja + " " + "#FF" + colorToHex(boja))
                        no_icon_paint.color = Color.parseColor("#FF" + colorToHex(boja))
                        //Log.d("ingo", "boja je ${no_icon_paint.color}")
                    } else {
                        no_icon_paint.color = Color.parseColor("#55000000")
                        //Log.d("ingo", "boja! je ${no_icon_paint.color}")
                    }
                } catch (e: NumberFormatException ){
                    no_icon_paint.color = Color.parseColor("#55000000")
                    //Log.d("ingo", "boja nemoguće za dešifrirati1")
                } catch (e: IllegalArgumentException ){
                    no_icon_paint.color = Color.parseColor("#55000000")
                    //Log.d("ingo", "boja nemoguće za dešifrirati2")
                }
                canvas.drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), no_icon_paint)
            }
        }
    }

    fun myDraw(triang: Double, distance: Float): PointF{
        val xxx = (sin(triang) * distance).toFloat()
        val yyy = (cos(triang) * distance).toFloat()
        return PointF( (if(leftOrRight) (size_width+xxx) else -xxx), ((size_height+yyy).toFloat()) )
    }
}

class DrawnApp(startTrig: Double, letter: Char, app_index: Int, rect: Rect){
    var startTrig: Double = startTrig
    var letter: Char = letter
    var app_index: Int = app_index
    var rect: Rect = rect
}

class EncapsulatedAppInfoWithFolder(apps: List<AppInfo>, folderName: String?, favorite: Boolean?){
    var apps: List<AppInfo> = apps
    var folderName: String? = folderName
    var favorite: Boolean? = favorite
}