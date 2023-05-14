package com.ingokodba.dragnav

import android.content.Context
import android.content.pm.ShortcutInfo
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
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MiddleButtonStates
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


class RightHandCircleView(context: Context, attrs: AttributeSet) : View(context, attrs){

    private var middleButtonState:MiddleButtonStates = MIDDLE_BUTTON_HIDE

    // Paint object for coloring and styling
    private val text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val smaller_text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shortcuts_text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val empty_circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thickLine = Paint(Paint.ANTI_ALIAS_FLAG)
    val clear_boja = Paint(Paint.ANTI_ALIAS_FLAG)
    //private val yellow_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thick_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val black_stroke_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shortcut_indicator_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Some colors for the face background, eyes and mouth.
    private var yellowColor = Color.YELLOW
    private var middleButtonColor = Color.WHITE
    private var closeInsideColor = Color.parseColor("#aaff0000")
    // View size in pixels
    private var size_width = 320
    private var size_height = 320
    private var app_list:List<AppInfo> = listOf()
    private var color_list:List<String> = listOf()
    private var new_letter_apps:MutableList<Int> = mutableListOf()
    private var no_draw_position:Int = -1
    private var polja_points:MutableList<Point> = mutableListOf()
    private var hovered_over:Int = -1
    var questiondrawable: Bitmap? = null
    //lateinit var radapter:RAdapter
    var bitmap: Bitmap? = null

    var gcolor = Color.parseColor("#FBB8AC")
    var draw_circles = true
    var draw_icons = true
    var border_width = 4f
    var text_size = 18f
    var smaller_text_size = 18f
    var transparency = 0.8f
    var shadow_toggle = true
    var show_app_names = true
    var showBigCircle = false
    var view: View = this

    var shortcuts: List<ShortcutInfo> = listOf()
    var shortcuts_app: Int? = null
    var shortcuts_rects: MutableList<Rect> = mutableListOf()

    var quickSwipeAngles: MutableList<Float> = mutableListOf()
    var quickSwipeEntered: Boolean = false

    var highestIconAngleDrawn: Double = 0.0

    var drawn_apps: MutableList<DrawnApp> = mutableListOf()
    var rect: Rect? = null

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
    var onlyfavorites: Boolean = false

    var detectSizeDivider = 1.0
    var detectSize = 0
    var offset: Float = 0f
    var last_abc: Char = '0'
    var new_abc: Char = '0'
    var app_index: Int = 0
    val queued_texts: MutableList<QueuedText> = mutableListOf()

    var clickIgnored = false
    var clickProcessed = false

    var sredina_processed = false
    var favcirclerect: Rect? = null
    var limit: Double = 0.0

    fun setColor(agcolor:String){
        try {
            gcolor = agcolor.toInt()
            Log.d("ingo", "color set to " + agcolor)
            invalidate()
        } catch (e:NumberFormatException){
            e.printStackTrace()
        }
    }

    fun setStepSize(size:Float){
        step_size = Math.PI*size // s 0.225 ih je 9 na ekranu, s 0.25 ih je 8
        if(step_size < 0.1) step_size = 0.1
        invalidate()
    }

    fun selectPolje(id:Int){
        selected = id
        changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
        invalidate()
    }

    fun showShortcuts(app_i: Int, shortcuts: List<ShortcutInfo>){
        this.shortcuts = shortcuts
        this.shortcuts_app = app_i
        clickProcessed = true
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

    fun setPosDontDraw(position:Int){
        no_draw_position = position
        polja_points.clear()
        Log.d("ingo", "setPosDontDraw " + position)
    }

    fun setKrugSAplikacijamaList(list:List<AppInfo>){
        app_list = list
        new_letter_apps.clear()
        var firstLetter: Char = '0'
        var counter = 0
        for(app in app_list){
            if(app.label[0].uppercaseChar() > firstLetter){
                firstLetter = app.label[0].uppercaseChar()
                new_letter_apps.add(counter)
                Log.d("ingo", "${app.label} $counter $firstLetter")
            }
            counter++
        }

        hovered_over = -1
        invalidate()
        //Log.d("ingo", "circleviewb setTextList " + list.map{it.text})
    }

    fun setColorList(list:List<String>){
        color_list = list
    }

    init {
        questiondrawable = AppCompatResources.getDrawable(context, R.drawable.baseline_question_mark_24)?.toBitmap()
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
        shortcut_indicator_paint.style = Paint.Style.FILL
        shortcut_indicator_paint.color = Color.RED
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

        drawPolja(canvas)
        //drawEditButton(canvas)
        //drawCloseButton(canvas)
    }

    private var mEventListener: MainFragmentRainbow.IMyEventListener? = null

    fun setEventListener(mEventListener: MainFragmentRainbow.IMyEventListener?) {
        this.mEventListener = mEventListener
    }

    fun lala(){
        Log.d("ingo", "invalidatedd")
        invalidate()
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

    fun checkForQuickMove(point: Point){
        val distance = Math.sqrt((point.y - size_height).toDouble().pow(2) + (point.x - size_width).toDouble().pow(2) )
        Log.d("ingo", "distance $distance $radius $detectSize")
        if(distance > radius*1.1f+detectSize){
            quickSwipeEntered = true
            var angle = -Math.PI/2-atan2((point.y-size_height).toDouble(),
                (point.x-size_width).toDouble()
            )
            var click = 0
            clickProcessed = true
            for(i in quickSwipeAngles.reversed().indices){
                if(angle > quickSwipeAngles[i]){
                    click = i
                }
            }
            val firstByThatLetter = app_list.firstOrNull{it.label[0].uppercaseChar() == app_list.distinctBy { it.label[0].uppercaseChar() }[click].label[0].uppercaseChar()}
                ?: return

            Log.d("ingo", "angle $angle ${firstByThatLetter.label} $click $quickSwipeAngles")
            // saznati s kulko moramo pomnožiti
            moveDistancedAccumulated = -(app_list.indexOf(firstByThatLetter)*step_size*250).toInt()
            Log.d("ingo", "lol $moveDistancedAccumulated $limit")

            if ((moveDistancedAccumulated) / 500f - Math.PI / 2 < limit) {
                //Log.d("ingo", "zaustavljam ${offset-Math.PI/2} ${limit}")
                //offset = (moveDistancedAccumulated+moveDistance)/500f
                moveDistancedAccumulated = ((limit + Math.PI / 2) * 500f).toInt()
            }

            invalidate()
            Log.d("ingo", "lol $moveDistancedAccumulated ${app_list.indexOf(firstByThatLetter)}")
        }
    }

    fun startOpenShortcutCountdown(){
        mEventListener?.onEventOccurred(EventTypes.START_COUNTDOWN, 2)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN){
            touchStart = Point(event.x.toInt(), event.y.toInt())
            // only if inside app
            startOpenShortcutCountdown()
            // check if clicked on fav
            if(shortcuts_app != null) {
                for (i in shortcuts_rects.indices) {
                    if (shortcuts_rects[i].contains(touchStart)) {
                        mEventListener?.onEventOccurred(EventTypes.OPEN_SHORTCUT, i)
                        clickProcessed = true
                    }
                }
                shortcuts = mutableListOf()
                shortcuts_app = null
            }
            if(favcirclerect != null && favcirclerect!!.contains(touchStart)){
                mEventListener?.onEventOccurred(EventTypes.TOGGLE_FAVORITES, 1)
            }
            checkForQuickMove(touchStart)
        } else if(event.action == MotionEvent.ACTION_MOVE){
            if(!clickProcessed){
                val change = (event.y - touchStart.y).toInt() - (event.x - touchStart.x).toInt()
                limit = -(app_list.size/2)*step_size - step_size*2
                if(abs(change) > detectSize/2f) mEventListener?.onEventOccurred(EventTypes.STOP_COUNTDOWN, 2)
                if(limit < -Math.PI/2 && abs(change) > detectSize/4f) {
                    // we're moving
                    moveDistance = change
                    if(abs(moveDistance) > detectSize/4) clickIgnored = true
                    if (moveDistance + moveDistancedAccumulated > 0) {
                        moveDistance = -moveDistancedAccumulated
                    } else if ((moveDistance + moveDistancedAccumulated) / 500f - Math.PI / 2 < limit) {
                        //Log.d("ingo", "zaustavljam ${offset-Math.PI/2} ${limit}")
                        //offset = (moveDistancedAccumulated+moveDistance)/500f
                        moveDistance = ((limit + Math.PI / 2) * 500f - moveDistancedAccumulated).toInt()
                    }
                }
            } else if(quickSwipeEntered){
                checkForQuickMove(Point(event.x.toInt(), event.y.toInt()))
            }
            //Log.d("ingo", "ne zaustavljam ${(moveDistance+moveDistancedAccumulated)/500f-Math.PI/2} ${limit}")
            invalidate()
        } else if(event.action == MotionEvent.ACTION_UP){
            Log.d("ingo", "moveDistance $moveDistance $moveDistancedAccumulated $step_size")
            if(!clickProcessed && !clickIgnored && abs(event.x - touchStart.x) + abs(event.y - touchStart.y) < detectSize/4){
                val app_i = getAppIndexImIn()
                if(app_i != null) mEventListener?.onEventOccurred(EventTypes.OPEN_APP, app_i!!)
            }
            mEventListener?.onEventOccurred(EventTypes.STOP_COUNTDOWN, 2)
            moveDistancedAccumulated += moveDistance
            moveDistance = 0
            clickIgnored = false
            clickProcessed = false
            quickSwipeEntered = false
        }
        return true
    }

    var step_size:Double = Math.PI*0.05
    var radius = size_width/1.3f
    private fun drawPolja(canvas: Canvas){
        queued_texts.clear()
        radius = size_width/1.35f
        if(overrideStep != null) step_size = overrideStep!!.toDouble()
        detectSizeDivider = 10.0
        detectSize = if(overrideDetectSize != null) overrideDetectSize!!.toInt() else 50
        quickSwipeAngles.clear()
        polja_points.clear()
        drawn_apps.clear()
        shortcuts_rects.clear()
        highestIconAngleDrawn = 0.0
        val inner_radius2 = (if(overrideDistance != null) overrideDistance!! else 0.87f)
        canvas.apply {

            // draw inner button
            var xx = (sin(Math.PI*5f/4f) * radius*inner_radius2/2.5f).toFloat()
            var yy = (cos(Math.PI*5f/4f) * radius*inner_radius2/2.5f).toFloat()
            var draw_pointF = PointF( ((size_width+xx)), ((size_height+yy)) )
            /*val rect = Rect(
                (draw_pointF.x - detectSize).toInt(),
                (draw_pointF.y - detectSize).toInt(),
                (draw_pointF.x + detectSize).toInt(),
                (draw_pointF.y + detectSize).toInt()
            )*/
            drawCircle(draw_pointF.x, draw_pointF.y, radius*inner_radius2/6f, circle_paint)

            val drawable: Drawable? = AppCompatResources.getDrawable(context, if(onlyfavorites) R.drawable.star_fill else R.drawable.star_empty)
            drawable?.setTint(Color.BLACK)
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
            Log.d("ingo", "racunamo " + (app_list.size/2)*step_size + ", $offset, $moveDistancedAccumulated")

            var counter = 0
            var offsetModulated = offset%step_size
            var offsetDivided = offset/step_size

            val offset2 = offset+(step_size/2f).toFloat()
            val offsetDivided2 = offset2/step_size
            val offsetModulated2 = offset2%step_size

            last_abc = '0'
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

            var lineRadius = radius*1.15f
            val rectf = RectF(size_width-lineRadius, size_height-lineRadius, size_width+lineRadius, size_height+lineRadius)
            drawn_apps.sortBy { it.startTrig }
            if(drawn_apps.size == 0) return

            val appsGroupedByLetters = app_list.distinctBy { it.label[0].uppercaseChar() }
            val divider = 90f/appsGroupedByLetters.size
            for(i in appsGroupedByLetters.indices){
                quickSwipeAngles.add(Math.toRadians((divider*i).toDouble()).toFloat())
                empty_circle_paint.strokeWidth = border_width*(drawn_apps.filter { it.letter.uppercaseChar() == appsGroupedByLetters[i].label[0].uppercaseChar() }.size.toFloat()/drawn_apps.size.toFloat()).toFloat()
                drawArc(rectf,
                    (-90-divider*i), -divider/1.2f, false, empty_circle_paint)
                val point = myDraw(Math.toRadians(180+divider*i+divider/3.0), radius*1.3f)
                canvas.drawText(appsGroupedByLetters[i].label[0].uppercaseChar().toString(), point.x, point.y+text_size/2, text_paint)
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
                                shortcuts[i].shortLabel as String,
                                textxy.x,
                                textxy.y,
                                shortcuts_text_paint
                            )

                            shortcuts_rects.add(recttmp)
                            drawRect(recttmp, black_stroke_paint)
                        }
                    } else {

                    }
                }
            }
            /*
            val appsGroupedByLetters = drawn_apps.distinctBy { it.letter.uppercaseChar() }

            for(i in appsGroupedByLetters.indices){
                var startAngle: Float = 0f
                var stopAngle: Float = 0f
                var nostart = false
                var nostop = false

                startAngle = Math.toDegrees(drawn_apps.first { it.letter.uppercaseChar() == appsGroupedByLetters[i].letter.uppercaseChar() }.startTrig - Math.PI/2f).toFloat()
                stopAngle = Math.toDegrees(drawn_apps.last { it.letter.uppercaseChar() == appsGroupedByLetters[i].letter.uppercaseChar() }.startTrig - Math.PI/2f).toFloat()
                if(i == 0){
                    if(drawn_apps[0].app_index > app_list.indexOfFirst{it.label[0].uppercaseChar() == drawn_apps[0].letter.uppercaseChar()}){
                        startAngle = 90f
                        nostart = true
                    }
                }
                if(i == appsGroupedByLetters.size-1){
                    if(drawn_apps.last { it.letter.uppercaseChar() == appsGroupedByLetters[i].letter.uppercaseChar() }.app_index != app_list.indexOfLast { it.label[0].uppercaseChar() == appsGroupedByLetters[i].letter.uppercaseChar() }){
                        stopAngle = 180f
                        nostop = true
                    }
                }
                if(stopAngle > 180f) stopAngle = 180f
                if(startAngle > 180f) startAngle = 180f
                if(stopAngle < 90f) stopAngle = 90f
                if(startAngle < 90f) startAngle = 90f

                drawArc(rectf,
                    360-startAngle, (startAngle-stopAngle), false, empty_circle_paint)

                val middleAngle = startAngle + (stopAngle-startAngle)/2
                Log.d("ingo", "triang middleangle $middleAngle")

                val point = myDraw(Math.toRadians(middleAngle.toDouble()) + Math.PI/2, radius*1.3f)
                if(!nostart) {
                    val pointstart =
                        myDraw(Math.toRadians(startAngle.toDouble()) + Math.PI / 2, lineRadius)
                    drawCircle(pointstart.x, pointstart.y, border_width*2f, circle_paint)
                }
                if(!nostop) {
                    val pointent =
                        myDraw(Math.toRadians(stopAngle.toDouble()) + Math.PI / 2, lineRadius)
                    drawCircle(pointent.x, pointent.y, border_width * 2f, circle_paint)
                }

                canvas.drawText(appsGroupedByLetters[i].letter.uppercaseChar().toString(), point.x, point.y+text_size/2, text_paint)

            }*/

            /*for(line_index in 0 until drawn_apps.size){
                //var xx = (sin(alphabetLines[line_index].startTrig)*radius).toFloat()
                //var yy = (cos(alphabetLines[line_index].startTrig)*radius).toFloat()

                var startAngle: Float = Math.toDegrees(drawn_apps[line_index].startTrig - Math.PI/2f).toFloat()
                var stopAngle: Float = if(line_index != drawn_apps.size-1 ) Math.toDegrees(drawn_apps[line_index+1].startTrig - Math.PI/2f).toFloat() else 180f

                if(line_index == 0 && drawn_apps[0].app_index != 0){
                    if(app_list[drawn_apps[0].app_index-1].label[0].uppercaseChar() == drawn_apps[0].letter){
                        startAngle = 90f
                    }
                } else if(line_index == drawn_apps.size-1 && app_list.size-1 == drawn_apps.last().app_index){
                    stopAngle = Math.toDegrees(highestIconAngleDrawn - Math.PI/2).toFloat()
                }
                Log.d("ingo", "triang " + drawn_apps[line_index].letter + " " + startAngle + " " + stopAngle)

                /*drawLine(
                    size_width.toFloat(), size_height.toFloat(),
                    size_width+xx, size_height+yy, empty_circle_paint
                )*/
                drawArc(rectf,
                    360-startAngle-1, (startAngle-stopAngle)+1, true, empty_circle_paint)

                val middleAngle = startAngle + (stopAngle-startAngle)/2
                Log.d("ingo", "triang middleangle $middleAngle")
                val point = myDrawCircle(Math.toRadians(middleAngle.toDouble()) + Math.PI/2, radius*1.2f, 10f, this)
                drawCircle(point.x, point.y, 10f, empty_circle_paint)
            }
            Log.d("ingo", "triang ______________________")*/
        }
    }

    fun drawApp2(index: Int, triang: Double, radiusScale: Float, canvas: Canvas){
        //Log.d("ingo", "aplikacija " + app_list[index].label + " " + index)
        bitmap = icons[app_list[index].packageName]?.toBitmap()
        //Log.d("ingo", "crtam " + Gson().toJson(bitmap))

            if(triang > highestIconAngleDrawn) highestIconAngleDrawn = triang

            var xx = (sin(triang) * radius*radiusScale).toFloat()
            var yy = (cos(triang) * radius*radiusScale).toFloat()
            var draw_pointF = PointF( ((size_width+xx).toFloat()), ((size_height+yy).toFloat()) )
            val rect = Rect(
                (draw_pointF.x - detectSize).toInt(),
                (draw_pointF.y - detectSize).toInt(),
                (draw_pointF.x + detectSize).toInt(),
                (draw_pointF.y + detectSize).toInt()
            )
            drawn_apps.add(DrawnApp(triang, app_list[index].label[0], index, rect))

            if(smaller_text_size != 0f){
                queued_texts.add(QueuedText(app_list[index].label, draw_pointF.x, draw_pointF.y+detectSize+smaller_text_size))
            }
            if(app_list[index].hasShortcuts) {
                canvas.drawCircle(
                    rect.right.toFloat(),
                    rect.bottom.toFloat(), detectSize / 8f, shortcut_indicator_paint
                )
            }
        if(bitmap != null) {
            canvas.drawBitmap(
                bitmap!!, null, rect, null
            )
        } else {
            if(questiondrawable != null) canvas.drawBitmap(
                questiondrawable!!, null, rect, null
            )
            /*val firstletter = app_list[index].label[0].uppercaseChar()

            val i: Int = alphabet_lines.indexOfFirst{it.letter == firstletter}
            if(i != -1){
                if(triang < alphabetLines[i].startTrig) alphabetLines[i].startTrig = triang
            } else {
                alphabetLines.add(AlphabetLine(triang, firstletter, index))
            }*/

            /*if(alphabetLines.last().letter == firstletter && alphabetLines.last().startTrig > triang){
                // ovaj umjesto onoga
                alphabetLines.last().startTrig = triang
            }

            if(alphabetLines.size == 0 || new_letter_apps.contains(index)){
                alphabetLines.add(AlphabetLine(triang, app_list[index].label[0].uppercaseChar(), index))
                //Log.d("ingo", "triang $triang")
            } else if(alphabetLines.size == 1 && alphabetLines[0].startTrig > triang){
                if(app_list[index].label[0].uppercaseChar() == alphabetLines[0].letter) {
                    alphabetLines[0].startTrig = triang
                } else {
                    alphabetLines.add(0, AlphabetLine(triang, app_list[index].label[0].uppercaseChar(), index))
                }
            }*/

            /*if(new_letter_apps.contains(index)){
                val xxx = (sin(triang) * radius*1.2f).toFloat()
                val yyy = (cos(triang) * radius*1.2f).toFloat()
                val adrawPointF = PointF( ((size_width+xxx).toFloat()), ((size_height+yyy).toFloat()) )
                canvas.drawText(app_list[index].label[0].uppercaseChar().toString(), adrawPointF.x, adrawPointF.y+text_size/2, text_paint)
            }*/
        }

    }

    fun myDraw(triang: Double, distance: Float): PointF{
        val xxx = (sin(triang) * distance).toFloat()
        val yyy = (cos(triang) * distance).toFloat()
        return PointF( ((size_width+xxx).toFloat()), ((size_height+yyy).toFloat()) )
    }
}

class DrawnApp(startTrig: Double, letter: Char, app_index: Int, rect: Rect){
    var startTrig: Double = startTrig
    var letter: Char = letter
    var app_index: Int = app_index
    var rect: Rect = rect
}
