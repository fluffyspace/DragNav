package com.example.dragnav

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.graphics.contains
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.example.dragnav.modeli.MeniJednoPolje
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CircleView(context: Context, attrs: AttributeSet) : View(context, attrs){

    companion object{

        val MIDDLE_BUTTON_HIDE = 0
        val MIDDLE_BUTTON_EDIT = 1
        val MIDDLE_BUTTON_CLOSE = 2
        val MIDDLE_BUTTON_CLOSE_INSIDE = 3
        val MIDDLE_BUTTON_HOME = 4
    }
    private var middleButtonState:Int = MIDDLE_BUTTON_HIDE
    // Paint object for coloring and styling
    private val text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val empty_circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val close_inside_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yellow_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thick_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Some colors for the face background, eyes and mouth.
    private var yellowColor = Color.YELLOW
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
    private var hovered_over:Int = -1
    lateinit var radapter:RAdapter

    var detectSize = 100
    var editMode:Boolean = false

    var mShowText:Boolean
    var textPos:Int
    var yellow:Int = -1
    private var amIHomeVar = true

    fun setStepSize(size:Float){
        step_size = Math.PI*size // s 0.225 ih je 9 na ekranu, s 0.25 ih je 8
        if(step_size < 0.1) step_size = 0.1
        invalidate()
    }

    fun yellowIt(id:Int){
        yellow = id
        changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
        invalidate()
    }

    fun deyellowAll(){
        yellow = -1
        amIHome(amIHomeVar)
        if(editMode){
            changeMiddleButtonState(MIDDLE_BUTTON_EDIT)
        } else if(amIHomeVar) {
            changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
        } else {
            changeMiddleButtonState(MIDDLE_BUTTON_HOME)
        }
        invalidate()
    }

    fun setPosDontDraw(position:Int){
        no_draw_position = position
    }

    fun setTextList(list:List<MeniJednoPolje>){
        app_list = list
        hovered_over = -1
        invalidate()
    }

    fun setColorList(list:List<String>){
        color_list = list
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
        text_paint.color = Color.WHITE
        text_paint.textAlign = Paint.Align.CENTER
        text_paint.style = Paint.Style.FILL
        text_paint.textSize = 60F;
        text_paint.setShadowLayer(4.0f, 1.0f, 2.0f, Color.BLACK);

        close_inside_paint.color = closeInsideColor
        close_inside_paint.style = Paint.Style.FILL_AND_STROKE
        close_inside_paint.strokeWidth = borderWidth

        empty_circle_paint.color = Color.parseColor("#FBB8AC")
        empty_circle_paint.style = Paint.Style.STROKE
        empty_circle_paint.strokeWidth = borderWidth
        empty_circle_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);

        circle_paint.color = Color.parseColor("#FBB8AC")
        circle_paint.style = Paint.Style.FILL
        circle_paint.strokeWidth = borderWidth
        circle_paint.setShadowLayer(1.0f, 1.0f, 2.0f, Color.BLACK);

        yellow_paint.color = yellowColor
        yellow_paint.style = Paint.Style.FILL
        yellow_paint.strokeWidth = borderWidth

        thick_paint.color = middleButtonColor
        thick_paint.style = Paint.Style.STROKE
        thick_paint.strokeWidth = 20f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 1
        size = Math.min(measuredWidth, measuredHeight)
        // 2
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        // call the super method to keep any drawing from the parent side.
        super.onDraw(canvas)

        drawTexts(canvas)
        when(middleButtonState){
            MIDDLE_BUTTON_EDIT -> drawEditButton(canvas)
            MIDDLE_BUTTON_CLOSE -> drawCloseButton(canvas)
            MIDDLE_BUTTON_CLOSE_INSIDE -> drawCloseButton(canvas, true)
            MIDDLE_BUTTON_HOME -> drawHomeButton(canvas)
        }
        //drawEditButton(canvas)
        //drawCloseButton(canvas)
    }

    private var mEventListener: IMyEventListener? = null

    fun setEventListener(mEventListener: IMyEventListener?) {
        this.mEventListener = mEventListener
    }

    fun amIHome(ami:Boolean){
        amIHomeVar=ami
        if(ami) changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
    }

    interface IMyEventListener {
        fun onEventOccurred(event: MotionEvent, counter:Int)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //Log.d("ingo", event.action.toString())
        var rect: Rect = Rect((event.x-detectSize).toInt(), (event.y-detectSize).toInt(), (event.x+detectSize).toInt(), (event.y+detectSize).toInt())
        var counter = 0
        var found = false
        for(text_point in text_points){
            if(rect.contains(text_point)){
                mEventListener?.onEventOccurred(event, counter)
                found = true
                if(hovered_over != counter) {
                    hovered_over = counter
                    invalidate()
                }
                break
            }
            counter++
        }
        var sredina:Boolean = false
        if (rect.contains(size / 2, size / 2)) {
            sredina = true
        }
        if(!found || event.action == MotionEvent.ACTION_UP) {
            hovered_over = -1
            invalidate()
        }
        /*if(!found && event.action == MotionEvent.ACTION_DOWN){
            if(sredina){
                if(!editMode && middleButtonState != MIDDLE_BUTTON_HOME){
                    return false
                }
            } else {
                return false
            }
        }*/
        if (sredina) {
            if(editMode) mEventListener?.onEventOccurred(event, MainActivity.ACTION_ADD)
            if(middleButtonState == MIDDLE_BUTTON_HOME) mEventListener?.onEventOccurred(event, MainActivity.ACTION_HOME)
        }
        if(event.action == MotionEvent.ACTION_MOVE) {
            if (sredina) {
                if (middleButtonState == MIDDLE_BUTTON_CLOSE) changeMiddleButtonState(
                    MIDDLE_BUTTON_CLOSE_INSIDE
                )
            } else {
                if (middleButtonState == MIDDLE_BUTTON_CLOSE_INSIDE) changeMiddleButtonState(
                    MIDDLE_BUTTON_CLOSE
                )
            }
        }
        if(!editMode){
            if(found){
                changeMiddleButtonState(MIDDLE_BUTTON_CLOSE)
            }
            if(event.action == MotionEvent.ACTION_UP){
                if(amIHomeVar) {
                    changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
                } else {
                    changeMiddleButtonState(MIDDLE_BUTTON_HOME)
                }
                // provjeri ako je miš na X, ako je onda ne pozovi meventlistener
                if(sredina){
                    mEventListener?.onEventOccurred(event, MainActivity.ACTION_CANCEL)
                } else {
                    mEventListener?.onEventOccurred(event, MainActivity.ACTION_LAUNCH)
                }
                Log.d("ingo", "sakrij")
            }
        }/* else {
            if(event.action == MotionEvent.ACTION_DOWN && found){
                Log.d("ingo", "editmode " + yellow)
                if(yellow == -1) {
                    yellowIt(counter)
                } else {
                    deyellowAll()
                }
            }
        }*/
        return true
    }

    fun changeMiddleButtonState(state:Int){
        middleButtonState = state
        invalidate()
    }

    private fun drawHomeButton(canvas: Canvas){
        canvas.apply {
            drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
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
            drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
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
                drawCircle(size / 2f, size / 2f, detectSize.toFloat(), close_inside_paint)
                drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            } else {
                drawCircle(size / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
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
            drawCircle(size / 2f, size / 2f, radius, empty_circle_paint)
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
                val draw_pointF = PointF( (((size / 2f)+x).toFloat()), (((size / 2f)+y).toFloat()) )
                val draw_point = Point( draw_pointF.x.toInt(), draw_pointF.y.toInt() )

                var text:String
                if(counter >= app_list.size){
                    text = ""
                    drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), empty_circle_paint)
                } else {
                    text = app_list[counter].text
                    circle_paint.color = Color.GRAY
                    if(app_list[counter].nextIntent != "") {
                        Log.d("ingo", "pokusavam boju " + color_list[counter])
                        try {
                            circle_paint.color = color_list[counter].toInt()
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                        val bitmap: Bitmap? =
                            radapter.icons[app_list[counter].nextIntent]?.toBitmap()
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