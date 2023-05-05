package com.ingokodba.dragnav

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import com.example.dragnav.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KFunction1

/**
 * TODO: document your custom view class.
 */
class RecycleScroller : View {

    private lateinit var trackPaint: Paint
    private var thumbY = 0f
    private var touchDownY = 0f
    var touchDown = false
    var drawThumb = false
    private var thumbHalfHidden = true
    private var thumbHeight = 100

    private var thumbRect = Rect(0, 0, width, 0)
    private var callback: KFunction1<Int, Unit>? = null
    /**
     * In the example view, this drawable is drawn above the text.
     */
    var thumbDrawable: Drawable? = context.getDrawable(R.drawable.thumb)

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    fun setCallback(callbackArg: KFunction1<Int, Unit>){
        callback = callbackArg
    }

    fun precentageToScroll(precentage: Int){
        //val precentage = (thumbY-(thumbHeight/2f))/((height-thumbHeight/2f)-(thumbHeight/2f))*100
        if(!touchDown) {
            thumbY = precentage * (height - thumbHeight) / 100 + (thumbHeight / 2f)
            invalidate()
        }
        //thumbY = precentage
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

        // Set up a default TextPaint object
        trackPaint = TextPaint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
        }

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements()
    }

    private fun invalidateTextPaintAndMeasurements() {
        trackPaint.let {
            it.color = Color.BLUE
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        thumbHeight = (width*2f).toInt()

        /*trackPaint.let{
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), it)
        }*/
        if(drawThumb){
            thumbDrawable?.let{
                thumbRect.top = (thumbY-thumbHeight/2f).toInt()
                thumbRect.bottom = (thumbY+thumbHeight/2f).toInt()
                //Rect(0, (thumbY-thumbHeight/2f).toInt(), width, (thumbY+thumbHeight/2f).toInt())
                canvas.drawBitmap(it.toBitmap(), null, thumbRect, null)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        thumbRect = Rect(0, 0, measuredWidth, 0)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_DOWN){
            touchDownY = event.y
            touchDown = true
            thumbHalfHidden = false

            return true
        }
        if(event?.action == MotionEvent.ACTION_MOVE){
            if(abs(event.y-touchDownY) > 10){
                //Log.d("ingo", "drawThumb true")
                drawThumb = true
                var tmpY = event.y
                tmpY = min(tmpY, (height-thumbHeight/2f).toFloat())
                tmpY = max(tmpY, (thumbHeight/2f))
                thumbY = tmpY
                val precentage = (thumbY-(thumbHeight/2f))/((height-thumbHeight/2f)-(thumbHeight/2f))*100
                callback?.let { it(precentage.toInt()) }
                postInvalidate()
            }
            return true
        }

        if(event?.action == MotionEvent.ACTION_UP){
            drawThumb = false
            touchDown = false
            thumbHalfHidden = true
            postInvalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}