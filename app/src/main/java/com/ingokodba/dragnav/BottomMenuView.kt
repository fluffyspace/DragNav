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
import androidx.preference.PreferenceManager
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.MeniJednoPolje


class BottomMenuView(context: Context, attrs: AttributeSet) : View(context, attrs){

    companion object{
        val BUTTONS_HIDDEN = 0
        val BUTTONS_SHOWN = 1
        val BUTTONS_EDIT = 2
    }
    var buttonsState:Int = BUTTONS_HIDDEN
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
    var small_screen:Boolean = false
    var selectedId:Int = -1
    //lateinit var radapter:RAdapter

    val drawables:List<Drawable?> = listOf(
        context.getDrawable(R.drawable.ic_baseline_add_50),
        context.getDrawable(R.drawable.ic_baseline_list_100),
        context.getDrawable(R.drawable.ic_baseline_edit_24),
        context.getDrawable(R.drawable.ic_baseline_search_80),
        context.getDrawable(R.drawable.ic_baseline_settings_100),
    )
    val expand_icon:Drawable? = context.getDrawable(R.drawable.ic_baseline_menu_24)
    val collapse_icon:Drawable? = context.getDrawable(R.drawable.ic_baseline_close_50)
    var edit_texts:List<String> = listOf("Rename", "Delete", "Enter", "Cancel")
    var edit_drawables:List<Drawable?> = listOf(
        context.getDrawable(R.drawable.ic_baseline_drive_file_rename_outline_24),
        context.getDrawable(R.drawable.ic_baseline_delete_24),
        context.getDrawable(R.drawable.ic_baseline_arrow_forward_24),
        context.getDrawable(R.drawable.ic_baseline_close_50),
    )

    var detectSize = 110f
    var editDetectSize = 110f
    var padding = 50f
    var editMode:Boolean = false
    var icons:MutableMap<String, Drawable?> = mutableMapOf()

    var yellow:Int = -1

    var cx = size/2f
    var cy = size/2f

    var search_button_cx:Float = 0f
    var search_button_cy:Float = 0f
    var def_height:Int = -1
    var radius = 300
    var global_height:Int = -1
    var global_width:Int = -1
    var overriden:Boolean = false
    var overridenRadius = -1
    var overridenDetectSize = -1f
    var overridenEditDetectSize = -1f

    fun setStepSize(size:Float){
        step_size = Math.PI*size // s 0.225 ih je 9 na ekranu, s 0.25 ih je 8
        if(step_size < 0.1) step_size = 0.1
        invalidate()
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BottomMenuView,
            0, 0).apply {
            try {
                def_height = getInteger(R.styleable.BottomMenuView_pxheight, -1)
                radius = getInteger(R.styleable.BottomMenuView_radius, 100)
                detectSize = getInteger(R.styleable.BottomMenuView_detectSize, 110).toFloat()
                overriden = getBoolean(R.styleable.BottomMenuView_override, false)
                Log.d("ingo", "def_height " + def_height)
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
        buttonsState = BUTTONS_HIDDEN
        global_height = if(def_height != -1) {
            def_height
        } else {
            measuredHeight
        }
        global_width = measuredWidth

        cx = measuredWidth/2f
        cy = global_height/2f
        if(!overriden) {
            Log.d(
                "ingo",
                "small_screen " + small_screen + " global_height " + global_height + " global_width " + global_width
            )
            //small_screen = measuredWidth/measuredHeight > 1.7
            var duzina_visina_omjer: Float = global_width / global_height.toFloat()
            Log.d(
                "ingo",
                "duzina_visina_omjer " + duzina_visina_omjer + " global_height " + global_height + " (global_width/1.434f).toInt() " + (global_width / 1.434f).toInt()
            )
            if (duzina_visina_omjer < 1.434f) global_height = (global_width / 1.434f).toInt()
            duzina_visina_omjer = global_width / global_height.toFloat()
            //if(overriden && )
            small_screen = duzina_visina_omjer > 2.7
            updateSmallScreenPreference()
            if (!small_screen) {
                detectSize = (global_height / 5.88).toFloat()
                editDetectSize = (global_width / 10).toFloat()
                radius = global_height / 2
            } else {
                detectSize = (global_width / 15).toFloat()
                editDetectSize = detectSize*1.2f
                global_height = (detectSize*2+padding*2).toInt()
            }
            updateOverridens()
            text_paint.textSize = editDetectSize / 2;
            padding = editDetectSize / 2
            // 2

        }
        size = global_height
        setMeasuredDimension(measuredWidth, global_height)
    }

    fun collapse(){
        buttonsState = BUTTONS_HIDDEN
        invalidate()
        Log.d("ingo", "collapse")
    }

    fun drawBackground(canvas:Canvas){
        canvas.apply {
            drawRect(0f, 0f, global_width.toFloat(), global_height.toFloat(), semi_transparent_paint)
        }
    }

    fun updateSmallScreenPreference() {
        val oneline = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(MySettingsFragment.UI_ONELINE, false)
        small_screen = oneline
        hovered_over = -1
        Log.d("ingo", "config small_screen to " + oneline)
    }

    fun updateOverridens(){
        if(overridenRadius != -1) radius = overridenRadius
        if(overridenDetectSize != -1f) detectSize = overridenDetectSize
        if(overridenEditDetectSize != -1f) editDetectSize = overridenEditDetectSize
    }


    override fun onDraw(canvas: Canvas) {
        // call the super method to keep any drawing from the parent side.
        super.onDraw(canvas)

        val ccy = (size-detectSize-padding)
        search_button_cx = cx
        search_button_cy = ccy
        if(editMode){
            drawEditButtons(canvas)
            Log.d("ingo", "bottommenuview crta editmode")
        } else if(small_screen) {
            drawAllButtons(canvas)
            buttonsState = BUTTONS_SHOWN
        } else {
            when (buttonsState) {
                BUTTONS_SHOWN -> {
                    drawAllButtons(canvas)
                    drawMenuButton(canvas, true)
                }
                BUTTONS_HIDDEN -> drawMenuButton(canvas)
            }
            Log.d("ingo", "bottommenuview crta " + buttonsState)
        }
    }

    fun drawAllButtons(canvas:Canvas){
        if(!small_screen) {
            text_points.clear()
            drawBackground(canvas)
            canvas.apply {
                var current: Double = 0.0
                var counter = 0
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
                text_points.clear()
                val ccy = (size-detectSize-padding)
                var counter = 0
                val itemn = drawables.size+1
                val width = (drawables.size*detectSize*2+((drawables.size-1)*padding))/2
                for(drawable in drawables) {
                    val offset_n = counter*detectSize*2+counter*padding
                    val draw_pointF = PointF(cx+width-detectSize-offset_n, ccy)
                    drawCircleBitmapButton(canvas, draw_pointF.x, draw_pointF.y, detectSize, drawable, text_points)
                    counter++
                }
            }
        }
    }

    fun drawCircleBitmapButton(canvas:Canvas, cx:Float, cy:Float, radius:Float, drawable:Drawable?, points:MutableList<Point>?, shaded:Boolean=false){
        canvas.apply {
            if (shaded) {
                drawCircle(cx, cy, radius, hover_circle_paint)
            } else {
                drawCircle(cx, cy, radius, circle_paint)
            }
            drawCircle(cx, cy, radius, circle_border_paint)
            points?.add(Point(cx.toInt(), cy.toInt()))
            val bitmap: Bitmap? = drawable?.apply { setTint(Color.BLACK) }?.toBitmap()
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

    fun updateTexts(lista:List<String>){
        edit_texts = lista
    }

    fun drawMenuButton(canvas:Canvas, opened:Boolean=false){
        canvas.apply {
            val ccy = (size-detectSize-padding)

            drawCircleBitmapButton(canvas, cx, ccy, detectSize, (if (opened) collapse_icon else expand_icon), null)

            /*drawCircle(cx, ccy, detectSize, circle_paint)
            drawCircle(cx, ccy, detectSize, circle_border_paint)
            val bitmap: Bitmap? = (if (opened) collapse_icon else expand_icon)?.toBitmap()
            if (bitmap != null) {
                drawBitmap(bitmap, null, Rect((cx - detectSize / 2).toInt(), (ccy - detectSize/ 2).toInt(), (cx + detectSize/2).toInt(), (ccy + detectSize / 2).toInt()), thick_paint)
            }*/
        }
    }

    fun drawEditButtons(canvas:Canvas){
        canvas.apply {
            val ccy = (global_height-padding-editDetectSize)
            var counter = 0
            val offset = (edit_texts.size*editDetectSize*2+((edit_texts.size-1)*padding))/2

            for(edit_text in edit_texts) {
                val offset_n = counter*editDetectSize*2+counter*padding+editDetectSize
                drawCircleBitmapButton(canvas, cx-offset+offset_n, ccy, editDetectSize, edit_drawables[counter], edit_points, (selectedId == -1 && counter != edit_texts.size-1))
                /*drawCircle(cx-offset+offset_n, ccy, editDetectSize, if(selectedId == -1 && counter != edit_texts.size-1) hover_circle_paint else circle_paint)
                drawCircle(cx-offset+offset_n, ccy, editDetectSize, circle_border_paint)
                drawText(edit_text, cx-offset+offset_n, ccy + text_paint.textSize/2, text_paint)
                edit_points.add(Point((cx-offset+offset_n).toInt(), ccy.toInt()))*/
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
        var consumed = false
        if(!editMode) {
            val rect = Rect(
                (event.x - detectSize).toInt(),
                (event.y - detectSize).toInt(),
                (event.x + detectSize).toInt(),
                (event.y + detectSize).toInt()
            )
            if(!small_screen) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (rect.contains(search_button_cx.toInt(), search_button_cy.toInt())) {
                        if(buttonsState == BUTTONS_SHOWN){
                            buttonsState = BUTTONS_HIDDEN
                        } else if(buttonsState == BUTTONS_HIDDEN){
                            buttonsState = BUTTONS_SHOWN
                        }
                        consumed = true
                        invalidate()
                    }
                }
                if (buttonsState == BUTTONS_SHOWN) {
                    var counter = 0
                    var found = false
                    for (text_point in text_points) {
                        if (rect.contains(text_point)) {
                            Log.d("ingo", "" + counter.toString())
                            //if(counter >= no_draw_position) counter++
                            //mEventListener?.onEventOccurred(event, counter, no_draw_position)
                            found = true
                            consumed = true
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
                        Log.d("ingo", "hovered over to -1")
                        invalidate()
                    }
                    if (event.action == MotionEvent.ACTION_UP && hovered_over != -1) {
                        mEventListener?.onEventOccurred(event, counter)
                        buttonsState = BUTTONS_HIDDEN
                        invalidate()
                    }
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    //buttonsState = BUTTONS_HIDDEN
                    //invalidate()
                }
            } else {
                // small_screen true
                Log.d("ingo", "in")
                if (event.action == MotionEvent.ACTION_DOWN && buttonsState == BUTTONS_HIDDEN) {
                    if (rect.contains(search_button_cx.toInt(), search_button_cy.toInt())) {
                        buttonsState = BUTTONS_SHOWN
                        consumed = true
                        Log.d("ingo", "buttons shown")
                        invalidate()
                    }
                    Log.d("ingo", "down hidden")
                } else if (event.action == MotionEvent.ACTION_DOWN && buttonsState == BUTTONS_SHOWN) {
                    Log.d("ingo", "down shown")
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
                            consumed = true
                            found = true
                            break
                        }
                        counter++
                    }
                    if (event.action == MotionEvent.ACTION_DOWN && hovered_over != -1) {
                        Log.d("ingo", "šaljem " + counter)
                        mEventListener?.onEventOccurred(event, counter)
                        buttonsState = BUTTONS_HIDDEN
                        invalidate()
                        consumed = true
                    }
                }
            }
        } else {
            val rect = Rect(
                (event.x - editDetectSize).toInt(),
                (event.y - editDetectSize).toInt(),
                (event.x + editDetectSize).toInt(),
                (event.y + editDetectSize).toInt()
            )
            var counter = 0
            var found = false
            for (edit_point in edit_points) {
                if (rect.contains(edit_point)) {
                    Log.d("ingo", counter.toString())
                    //if(counter >= no_draw_position) counter++
                    //mEventListener?.onEventOccurred(event, counter, no_draw_position)
                    found = true
                    consumed = true
                    break
                }
                counter++
            }
            if (event.action == MotionEvent.ACTION_DOWN && found) {
                mEventListener?.onEventOccurred(event, -counter-1)
            }
        }
        return consumed
    }

}