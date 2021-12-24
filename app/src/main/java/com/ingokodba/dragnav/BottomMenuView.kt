package com.ingokodba.dragnav

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.contains
import androidx.core.graphics.drawable.toBitmap
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.MeniJednoPolje


class BottomMenuView(context: Context, attrs: AttributeSet) : View(context, attrs){

    companion object{
        val BUTTONS_HIDDEN = 0
        val BUTTONS_SHOWN = 1
        val BUTTONS_EDIT = 2
    }
    private var buttonsState:Int = BUTTONS_HIDDEN
    var editState:Boolean = false
    // Paint object for coloring and styling
    private val text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hover_circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circle_border_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //private val yellow_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thick_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val semi_transparent_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Some colors for the face background, eyes and mouth.
    private var middleButtonColor = Color.WHITE
    private var closeInsideColor = Color.parseColor("#aaff0000")
    // Face border width in pixels
    private var borderWidth = 4.0f
    // View size in pixels
    private var size = 320
    private var step_size:Double = Math.PI*0.25
    private var app_list:List<MeniJednoPolje> = listOf()
    private var color_list:List<String> = listOf()
    private var no_draw_position:Int = -1
    private var text_points:MutableList<Point> = mutableListOf()
    private var edit_points:MutableList<Point> = mutableListOf()
    private var hovered_over:Int = -1
    var small_screen:Boolean = true
    var selectedId:Int = -1
    //lateinit var radapter:RAdapter

    val drawables:List<Drawable?> = listOf(
        context.getDrawable(R.drawable.ic_baseline_home_100),
        context.getDrawable(R.drawable.ic_baseline_list_100),
        context.getDrawable(R.drawable.ic_baseline_edit_24),
        context.getDrawable(R.drawable.ic_baseline_search_80),
        context.getDrawable(R.drawable.ic_baseline_settings_100),
    )
    val expand_icon:Drawable? = context.getDrawable(R.drawable.ic_baseline_add_50)
    val collapse_icon:Drawable? = context.getDrawable(R.drawable.ic_baseline_close_50)
    val edit_texts:List<String> = listOf("Rename", "Delete", "Enter", "Cancel")

    var detectSize = 110f
    var editMode:Boolean = false
    var icons:MutableMap<String, Drawable?> = mutableMapOf()

    var mShowText:Boolean
    var textPos:Int
    var yellow:Int = -1
    var amIHomeVar = true

    var cx = size/2f
    var cy = size/2f

    var search_button_cx:Float = 0f
    var search_button_cy:Float = 0f

    fun setStepSize(size:Float){
        step_size = Math.PI*size // s 0.225 ih je 9 na ekranu, s 0.25 ih je 8
        if(step_size < 0.1) step_size = 0.1
        invalidate()
    }

    init {
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
        text_paint.color = Color.BLACK
        text_paint.textAlign = Paint.Align.CENTER
        text_paint.style = Paint.Style.FILL
        text_paint.textSize = 50F;
        text_paint.setShadowLayer(4.0f, 1.0f, 2.0f, Color.WHITE);

        semi_transparent_paint.color = Color.parseColor("#88000000")
        semi_transparent_paint.style = Paint.Style.FILL
        semi_transparent_paint.strokeWidth = borderWidth

        circle_paint.color = Color.parseColor("#ffffff")
        circle_paint.style = Paint.Style.FILL
        circle_paint.strokeWidth = borderWidth

        hover_circle_paint.color = Color.parseColor("#cccccc")
        hover_circle_paint.style = Paint.Style.FILL
        hover_circle_paint.strokeWidth = borderWidth

        circle_border_paint.color = Color.parseColor("#BB000000")
        circle_border_paint.style = Paint.Style.STROKE
        circle_border_paint.strokeWidth = borderWidth
        circle_border_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);


        thick_paint.color = middleButtonColor
        thick_paint.style = Paint.Style.STROKE
        thick_paint.strokeWidth = 20f
        thick_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 1
        size = measuredHeight
        cx = measuredWidth/2f
        cy = measuredHeight/2f
        // 2
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    fun collapse(){

    }

    override fun onDraw(canvas: Canvas) {
        // call the super method to keep any drawing from the parent side.
        super.onDraw(canvas)

        //drawTexts(canvas)
        if(small_screen) detectSize = 70f
        if(editMode){
            drawEditButtons(canvas)
            Log.d("ingo", "bottommenuview crta editmode")
        } else {
            when (buttonsState) {
                BUTTONS_SHOWN -> {
                    if(!small_screen) drawMenuButton(canvas, true)
                    drawAllButtons(canvas)
                }
                BUTTONS_HIDDEN -> drawMenuButton(canvas)
            }
            Log.d("ingo", "bottommenuview crta " + buttonsState)
        }
        //drawEditButton(canvas)
        //drawCloseButton(canvas)
    }

    fun drawAllButtons(canvas:Canvas){
        if(!small_screen) {
            text_points.clear()
            canvas.apply {
                var current: Double = 0.0
                var counter = 0
                val radius = 300
                while (current <= Math.PI) {
                    val x = Math.cos(current) * radius
                    val y = Math.sin(current) * radius
                    val draw_pointF = PointF(((cx + x).toFloat()), ((search_button_cy - y).toFloat()))
                    drawCircleBitmapButton(canvas, draw_pointF.x, draw_pointF.y, detectSize, drawables[counter], text_points,
                        hovered_over == counter
                    )
                    current += step_size
                    counter++
                }
            }
        } else {
            canvas.apply {
                val padding = 50f
                val ccy = (size-detectSize-padding)
                search_button_cx = cx
                search_button_cy = ccy
                var counter = 0
                val itemn = drawables.size+1
                val width = (drawables.size*detectSize*2+((drawables.size-1)*padding))/2
                for(drawable in drawables) {
                    val offset_n = counter*detectSize*2+counter*padding
                    val draw_pointF = PointF(cx-width+detectSize+offset_n, ccy)

                    drawCircleBitmapButton(canvas, draw_pointF.x, draw_pointF.y, detectSize, drawable, text_points)
                    counter++
                }
            }
        }
    }

    fun drawCircleBitmapButton(canvas:Canvas, cx:Float, cy:Float, radius:Float, drawable:Drawable?, points:MutableList<Point>, shaded:Boolean=false){
        canvas.apply {
            if (shaded) {
                drawCircle(cx, cy, detectSize, hover_circle_paint)
            } else {
                drawCircle(cx, cy, detectSize, circle_paint)
            }
            drawCircle(cx, cy, radius, circle_border_paint)
            points.add(Point(cx.toInt(), cy.toInt()))
            val bitmap: Bitmap? = drawable?.toBitmap()
            if (bitmap != null) {
                drawBitmap(
                    bitmap,
                    null,
                    Rect(
                        (cx - radius / 2).toInt(),
                        (cy - radius / 2).toInt(),
                        (cx + radius / 2).toInt(),
                        (cy + radius / 2).toInt()
                    ),
                    thick_paint
                )
            }
        }
    }

    fun drawMenuButton(canvas:Canvas, opened:Boolean=false){
        canvas.apply {
            val padding = 50f
            val ccy = (size-detectSize-padding)
            search_button_cx = cx
            search_button_cy = ccy
            drawCircle(cx, ccy, detectSize, circle_paint)
            drawCircle(cx, ccy, detectSize, circle_border_paint)
            val bitmap: Bitmap? = (if (opened) collapse_icon else expand_icon)?.toBitmap()
            if (bitmap != null) {
                drawBitmap(bitmap, null, Rect((cx - detectSize / 2).toInt(), (ccy - detectSize/ 2).toInt(), (cx + detectSize/2).toInt(), (ccy + detectSize / 2).toInt()), thick_paint)
            }
        }
    }

    fun drawEditButtons(canvas:Canvas){
        canvas.apply {
            val padding = 50f
            val ccy = (size-detectSize-padding)
            search_button_cx = cx
            search_button_cy = ccy
            var counter = 0
            val offset = (edit_texts.size*detectSize*2+(edit_texts.size-1*padding))/2
            for(edit_text in edit_texts) {
                val offset_n = counter*detectSize*2+counter*padding
                drawCircle(cx-offset+offset_n, ccy, detectSize, if(selectedId == -1 && counter != edit_texts.size-1) hover_circle_paint else circle_paint)
                drawCircle(cx-offset+offset_n, ccy, detectSize, circle_border_paint)
                drawText(edit_text, cx-offset+offset_n, ccy + 20, text_paint)
                edit_points.add(Point((cx-offset+offset_n).toInt(), ccy.toInt()))
                counter++
            }
        }
    }

    private var mEventListener: IMyOtherEventListener? = null

    fun setEventListener(mEventListener: IMyOtherEventListener?) {
        this.mEventListener = mEventListener
    }

    interface IMyOtherEventListener {
        fun onEventOccurred(event: MotionEvent, counter:Int)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rect = Rect(
            (event.x - detectSize).toInt(),
            (event.y - detectSize).toInt(),
            (event.x + detectSize).toInt(),
            (event.y + detectSize).toInt()
        )
        if(!editMode) {
            if(!small_screen) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (rect.contains(search_button_cx.toInt(), search_button_cy.toInt())) {
                        buttonsState = BUTTONS_SHOWN
                        invalidate()
                    }
                }
                if (buttonsState == BUTTONS_SHOWN) {
                    var counter = 0
                    var found = false
                    for (text_point in text_points) {
                        if (rect.contains(text_point)) {
                            Log.d("ingo", counter.toString())
                            //if(counter >= no_draw_position) counter++
                            //mEventListener?.onEventOccurred(event, counter, no_draw_position)
                            found = true
                            if (hovered_over != counter) {
                                hovered_over = counter
                                invalidate()
                            }
                            break
                        }
                        counter++
                    }
                    if (!found && hovered_over != -1) {
                        hovered_over = -1
                        invalidate()
                    }
                    if (event.action == MotionEvent.ACTION_UP && hovered_over != -1) {
                        mEventListener?.onEventOccurred(event, counter)
                    }
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    buttonsState = BUTTONS_HIDDEN
                    invalidate()
                }
            } else {
                // small_screen true
                Log.d("ingo", "in")
                if (event.action == MotionEvent.ACTION_DOWN && buttonsState == BUTTONS_HIDDEN) {
                    if (rect.contains(search_button_cx.toInt(), search_button_cy.toInt())) {
                        buttonsState = BUTTONS_SHOWN
                        Log.d("ingo", "buttons shown")
                        invalidate()
                    }
                } else if (event.action == MotionEvent.ACTION_DOWN && buttonsState == BUTTONS_SHOWN) {
                    var counter = 0
                    var found = false
                    for (text_point in text_points) {
                        if (rect.contains(text_point)) {
                            Log.d("ingo", counter.toString())
                            //if(counter >= no_draw_position) counter++
                            //mEventListener?.onEventOccurred(event, counter, no_draw_position)
                            if (hovered_over != counter) {
                                hovered_over = counter
                                invalidate()
                            }
                            found = true
                            break
                        }
                        counter++
                    }
                    if (event.action == MotionEvent.ACTION_DOWN && hovered_over != -1) {
                        Log.d("ingo", "")
                        mEventListener?.onEventOccurred(event, counter)
                        buttonsState = BUTTONS_HIDDEN
                        invalidate()
                    }
                }
            }
        } else {
            var counter = 0
            var found = false
            for (edit_point in edit_points) {
                if (rect.contains(edit_point)) {
                    Log.d("ingo", counter.toString())
                    //if(counter >= no_draw_position) counter++
                    //mEventListener?.onEventOccurred(event, counter, no_draw_position)
                    found = true
                    break
                }
                counter++
            }
            if (event.action == MotionEvent.ACTION_DOWN && found) {
                mEventListener?.onEventOccurred(event, -counter-1)
            }
        }
        return true
    }

    private fun drawHomeButton(canvas: Canvas){
        canvas.apply {
            //drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            drawText("Home", size/2f, size/2f+20, text_paint)
            /*
                var plus_margin = 50
                val boxsize = 30
                val squish_vertical:Float = 0.7f
                thick_paint.strokeWidth = 15f
                val c = (size / 2f)
                val path = Path()
                path.moveTo(c-boxsize, c-boxsize*squish_vertical)
                path.lineTo(c+boxsize, c-boxsize*squish_vertical)
                path.lineTo(c+boxsize, c+boxsize*squish_vertical)
                path.lineTo(c-boxsize, c+boxsize*squish_vertical)
                path.lineTo(c-boxsize, c-boxsize*squish_vertical)
                path.lineTo(c, (c-boxsize*2))
                path.lineTo(c+boxsize, c-boxsize*squish_vertical)
                drawPath(path, thick_paint)*/
        }
    }

    private fun drawEditButton(canvas: Canvas){
        canvas.apply {
            //drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            var plus_margin = 50
            thick_paint.strokeWidth = 15f
            var center = (size / 2f)
            drawLine(center, center+(-detectSize+plus_margin).toFloat(), center, center+(detectSize-plus_margin).toFloat(), thick_paint)
            drawLine(center+(-detectSize+plus_margin).toFloat(),center, center+(detectSize-plus_margin).toFloat(), center, thick_paint)
        }
    }

    private fun drawCloseButton(canvas: Canvas, inside: Boolean = false){
        canvas.apply {
            if(inside){
                //drawCircle(size / 2f, size / 2f, detectSize.toFloat(), close_inside_paint)
                //drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            } else {
                //drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            }
            var plus_margin = 60
            thick_paint.strokeWidth = 15f
            var center = (size / 2f)
            drawLine(center+(-detectSize+plus_margin).toFloat(), center+(-detectSize+plus_margin).toFloat(), center+(detectSize-plus_margin).toFloat(), center+(detectSize-plus_margin).toFloat(), thick_paint)
            drawLine(center+(-detectSize+plus_margin).toFloat(), center+(+detectSize-plus_margin).toFloat(), center+(detectSize-plus_margin).toFloat(), center-(detectSize-plus_margin).toFloat(), thick_paint)
        }
    }

    private fun drawTexts(canvas: Canvas){

        text_points.clear()
        canvas.apply {

            val radius = size / 3f
            var current:Double = 0.0
            val cx = size/2f
            val cy = size/2f
            //drawCircle(cx, cy, radius, empty_circle_paint)
            var counter = 0
            var over_no_draw_position = false
            while(current < Math.PI*2){
                if(!over_no_draw_position && counter == no_draw_position){
                    current += step_size
                    over_no_draw_position = true
                    continue
                }
                val x = Math.sin(current)*radius
                val y = Math.cos(current)*radius
                val draw_pointF = PointF( ((cx+x).toFloat()), ((cy+y).toFloat()) )
                val draw_point = Point( draw_pointF.x.toInt(), draw_pointF.y.toInt() )
                var text:String
                if(counter >= app_list.size){
                    text = ""
                    //drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), empty_circle_paint)
                } else {
                    text = app_list[counter].text
                    circle_paint.color = Color.parseColor("#55000000")
                    if(app_list[counter].nextIntent != "") {
                        //Log.d("ingo", "pokusavam boju " + color_list[counter])
                        try {
                            circle_paint.color = color_list[counter].toInt()
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                        val bitmap: Bitmap? = icons[app_list[counter].nextIntent]?.toBitmap()//Bitmap.createBitmap(5, 5, Bitmap.Config.RGB_565)
                        //radapter.icons[app_list[counter].nextIntent]?.toBitmap()
                        if (bitmap != null) {
                            drawBitmap(
                                bitmap, null, Rect(
                                    (draw_pointF.x - detectSize).toInt(),
                                    (draw_pointF.y - detectSize).toInt(),
                                    (draw_pointF.x + detectSize).toInt(),
                                    (draw_pointF.y + detectSize).toInt()
                                ), null
                            )
                        } else {
                            drawCircle(
                                draw_pointF.x,
                                draw_pointF.y,
                                detectSize.toFloat(),
                                circle_paint
                            )
                            drawText(text, draw_pointF.x, draw_pointF.y+20, text_paint)
                        }
                        if(app_list[counter].shortcut){
                            drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), semi_transparent_paint)
                            //drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), empty_circle_paint)
                            drawText(text, draw_pointF.x, draw_pointF.y+20, text_paint)
                        }
                    } else {
                        drawCircle(
                            draw_pointF.x,
                            draw_pointF.y,
                            detectSize.toFloat(),
                            circle_paint
                        )
                        drawText(text, draw_pointF.x, draw_pointF.y+20, text_paint)
                    }
                    text_points.add(draw_point)
                }
                if(counter == yellow){
                    //drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), yellow_paint)
                    drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), thick_paint)
                }

                if(counter == hovered_over){
                    //drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), thick_paint)
                }


                //drawText(text, draw_pointF.x, draw_pointF.y+20, text_paint)
                current += step_size
                counter++
            }
        }
    }
}