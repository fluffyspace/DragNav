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
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MiddleButtonStates
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


class RightHandCircleView(context: Context, attrs: AttributeSet) : View(context, attrs){

    private var middleButtonState:MiddleButtonStates = MIDDLE_BUTTON_HIDE

    // Paint object for coloring and styling
    private val text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val empty_circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val close_inside_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val clear_boja = Paint(Paint.ANTI_ALIAS_FLAG)
    //private val yellow_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thick_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val semi_transparent_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Some colors for the face background, eyes and mouth.
    private var yellowColor = Color.YELLOW
    private var middleButtonColor = Color.WHITE
    private var closeInsideColor = Color.parseColor("#aaff0000")
    // View size in pixels
    private var size = 320
    private var size_width = 320
    private var app_list:List<AppInfo> = listOf()
    private var color_list:List<String> = listOf()
    private var no_draw_position:Int = -1
    private var polja_points:MutableList<Point> = mutableListOf()
    private var hovered_over:Int = -1
    val infodrawable = AppCompatResources.getDrawable(context, R.drawable.ic_outline_info_75)
    val adddrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_add_50)
    //lateinit var radapter:RAdapter
    var bitmap: Bitmap? = null

    var gcolor = Color.parseColor("#FBB8AC")
    var draw_circles = true
    var draw_icons = true
    var border_width = 4f
    var text_size = 18f
    var transparency = 0.8f
    var shadow_toggle = true
    var show_app_names = true
    var showBigCircle = false
    var view: View = this

    var drawn_apps: MutableList<Pair<Int, Rect>> = mutableListOf()
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

    var detectSizeDivider = 1.0
    var detectSize = 0

    var sredina_processed = false

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
        hovered_over = -1
        invalidate()
        //Log.d("ingo", "circleviewb setTextList " + list.map{it.text})
    }

    fun setColorList(list:List<String>){
        color_list = list
    }

    init {
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
        text_paint.setShadowLayer(4.0f, 1.0f, 2.0f, Color.BLACK);
        semi_transparent_paint.color = Color.parseColor("#88000000")
        semi_transparent_paint.style = Paint.Style.FILL
        close_inside_paint.color = closeInsideColor
        close_inside_paint.style = Paint.Style.FILL_AND_STROKE
        empty_circle_paint.style = Paint.Style.STROKE
        circle_paint.style = Paint.Style.FILL
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
        size = measuredWidth
        if(size < measuredWidth){
            size_width = measuredWidth
        } else {
            size_width = size
        }
        detectSize = if(overrideDetectSize != null) {
            overrideDetectSize!!.toInt()
        } else {
            (size / detectSizeDivider).toInt()
        }
        // 2
        setMeasuredDimension(size_width, size)
    }

    override fun onDraw(canvas: Canvas) {
        // call the super method to keep any drawing from the parent side.
        super.onDraw(canvas)
        // settings
        empty_circle_paint.color = gcolor
        circle_paint.color = gcolor
        if(shadow_toggle) {
            empty_circle_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK)
            circle_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);
            //thick_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);
        } else {
            empty_circle_paint.setShadowLayer(0.0f, 1.0f, 2.0f, Color.BLACK)
            circle_paint.setShadowLayer(0.0f, 1.0f, 2.0f, Color.BLACK);
            thick_paint.setShadowLayer(0.0f, 1.0f, 2.0f, Color.BLACK);
        }
        semi_transparent_paint.strokeWidth = border_width
        close_inside_paint.strokeWidth = border_width
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
        text_size = PreferenceManager.getDefaultSharedPreferences(context).getString(MySettingsFragment.UI_TEXT_SIZE, "30")!!.toFloat()
        transparency = PreferenceManager.getDefaultSharedPreferences(context).getString(MySettingsFragment.UI_TRANSPARENCY, "1")!!.toFloat()
        text_paint.textSize = text_size;
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

    fun setAppList(list:List<AppInfo>){

    }

    val abc = listOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN){
            touchStart = Point(event.x.toInt(), event.y.toInt())
        } else if(event.action == MotionEvent.ACTION_MOVE){
            moveDistance = (event.y - touchStart.y).toInt() - (event.x - touchStart.x).toInt()
            if(moveDistance+moveDistancedAccumulated > 0) {
                moveDistance = -moveDistancedAccumulated
            }
            invalidate()
        } else if(event.action == MotionEvent.ACTION_UP){
            Log.d("ingo", "moveDistance $moveDistance $detectSize")
            if(abs(moveDistance) < detectSize/4){
                for(draw_app in drawn_apps){
                    if(draw_app.second.contains(touchStart)){
                        mEventListener?.onEventOccurred(event, draw_app.first)
                        break
                    }
                }
            }
            moveDistancedAccumulated += moveDistance
            moveDistance = 0
        }
        return true
    }

    var step_size:Double = Math.PI*0.05

    private fun drawPolja(canvas: Canvas){
        detectSizeDivider = 10.0
        detectSize = if(overrideDetectSize != null) overrideDetectSize!!.toInt() else 50
        polja_points.clear()
        drawn_apps.clear()
        canvas.apply {
            val radius = size/1.2f
            //if (showBigCircle) drawCircle(size.toFloat(), size.toFloat(), radius, empty_circle_paint)

            var currentStepValue: Double = Math.PI
            var offset = (moveDistancedAccumulated+moveDistance)/500f

            //drawText((moveDistancedAccumulated+moveDistance).toString(), (size/2).toFloat(), 50F, text_paint)

            var counter = 0
            var offsetModulated = offset%step_size
            var offsetDivided = offset/step_size
            var x: Double
            var y: Double
            var draw_pointF: PointF
            while(currentStepValue.compareTo(Math.PI) >= 0 && currentStepValue.compareTo(Math.PI*2f) < 0) {
                currentStepValue += step_size
                x = sin(currentStepValue+offsetModulated) * radius
                y = cos(currentStepValue+offsetModulated) * radius
                draw_pointF = PointF( ((size+x).toFloat()), ((size+y).toFloat()) )
                val abc_index = ((counter-offsetDivided.toInt())*2)
                if(abc_index >= app_list.size) break
                //if(abc_index >= abc.size) break
                /*drawCircle(
                    draw_pointF.x,
                    draw_pointF.y,
                    detectSize.toFloat(), empty_circle_paint
                )*/
                drawApp(abc_index, draw_pointF.x, draw_pointF.y, this)
                //drawText(abc_index.toString(), draw_pointF.x, draw_pointF.y+text_size/2, text_paint)
                counter++
            }
            counter = 0
            currentStepValue = Math.PI
            offset += (step_size/2f).toFloat()
            offsetDivided = offset/step_size
            offsetModulated = offset%step_size
            val inner_radius = radius * (if(overrideDistance != null) overrideDistance!!.toDouble() else 0.87)
            while(currentStepValue.compareTo(Math.PI) >= 0 && currentStepValue.compareTo(Math.PI*2f) < 0) {
                currentStepValue += step_size
                x = sin(currentStepValue+offsetModulated) * inner_radius
                y = cos(currentStepValue+offsetModulated) * inner_radius
                draw_pointF = PointF( ((size+x).toFloat()), ((size+y).toFloat()) )
                val abc_index = ((counter-offsetDivided.toInt())*2+1)
                if(abc_index >= app_list.size) break
                //if(abc_index >= abc.size) break
                /*drawCircle(
                    draw_pointF.x,
                    draw_pointF.y,
                    detectSize.toFloat(), empty_circle_paint
                )*/
                //drawText(abc_index.toString(), draw_pointF.x, draw_pointF.y+text_size/2, text_paint)
                drawApp(abc_index, draw_pointF.x, draw_pointF.y, this)
                counter++
            }
        }

    }

    fun drawApp(index: Int, x: Float, y: Float, canvas: Canvas){
        bitmap = icons[app_list[index].packageName]?.toBitmap()
        if(bitmap != null) {
            rect = Rect(
                (x - detectSize).toInt(),
                (y - detectSize).toInt(),
                (x + detectSize).toInt(),
                (y + detectSize).toInt()
            )
            drawn_apps.add(Pair(index, rect!!))
            canvas.drawBitmap(
                bitmap!!, null, rect!!, null
            )
        }
    }
}