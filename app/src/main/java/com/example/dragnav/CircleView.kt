package com.example.dragnav

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.contains


class CircleView(context: Context, attrs: AttributeSet) : View(context, attrs){

    // Paint object for coloring and styling
    private val text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circle_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Some colors for the face background, eyes and mouth.
    private var faceColor = Color.YELLOW
    private var eyesColor = Color.BLACK
    private var mouthColor = Color.BLACK
    private var borderColor = Color.BLACK
    // Face border width in pixels
    private var borderWidth = 4.0f
    // View size in pixels
    private var size = 320
    private var step_size:Double = Math.PI*0.25
    private var text_list:List<String> = listOf()
    private var no_draw_position:Int = -1
    private var text_points:MutableList<Point> = mutableListOf()
    var detectSize = 100

    var mShowText:Boolean
    var textPos:Int

    fun setStepSize(size:Float){
        step_size = Math.PI*size // s 0.225 ih je 9 na ekranu, s 0.25 ih je 8
        if(step_size < 0.1) step_size = 0.1
        invalidate()
    }

    fun setPosDontDraw(position:Int){
        no_draw_position = position
        invalidate()
    }

    fun setTextList(list:List<String>){
        text_list = list
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
        circle_paint.color = borderColor
        circle_paint.style = Paint.Style.STROKE
        circle_paint.strokeWidth = borderWidth
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
        //drawFaceBackground(canvas)
        //drawEyes(canvas)
        //drawMouth(canvas)
    }

    private fun drawFaceBackground(canvas: Canvas) {
        // 1
        text_paint.color = faceColor
        text_paint.style = Paint.Style.FILL

        // 2
        val radius = size / 2f

        // 3
        canvas.drawCircle(size / 2f, size / 2f, radius, text_paint)

        // 4


        // 5
        canvas.drawCircle(size / 2f, size / 2f, radius - borderWidth / 2f, text_paint)
    }

    private var mEventListener: IMyEventListener? = null

    fun setEventListener(mEventListener: IMyEventListener?) {
        this.mEventListener = mEventListener
    }

    interface IMyEventListener {
        fun onEventOccurred(counter:Int)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        var rect: Rect = Rect((event.x-detectSize).toInt(), (event.y-detectSize).toInt(), (event.x+detectSize).toInt(), (event.y+detectSize).toInt())
        var counter = 0
        for(text_point in text_points){
            if(rect.contains(text_point)){
                mEventListener?.onEventOccurred(counter)
                break
            }
            counter++
        }

        return super.onTouchEvent(event)
    }

    private fun drawTexts(canvas: Canvas){
        text_paint.color = eyesColor

        text_paint.setTextSize(60F);
        text_points.clear()
        canvas.apply {

            var radius = size / 3f
            var current:Double = 0.0
            text_paint.style = Paint.Style.STROKE
            text_paint.textAlign = Paint.Align.CENTER
            drawCircle(size / 2f, size / 2f, radius, text_paint)
            text_paint.style = Paint.Style.FILL
            var counter = 0
            while(current < Math.PI*2){
                if(counter == no_draw_position){
                    current += step_size
                    continue
                }
                var x = Math.sin(current)*radius
                var y = Math.cos(current)*radius
                var text:String
                if(counter >= text_list.size){
                    text = "-"
                } else {
                    text = text_list[counter]
                }
                var draw_pointF = PointF( (((size / 2f)+x).toFloat()), (((size / 2f)+y).toFloat()) )
                var draw_point = Point( draw_pointF.x.toInt(), draw_pointF.y.toInt() )
                drawText(text, draw_pointF.x, draw_pointF.y+20, text_paint)
                drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), circle_paint)
                text_points.add(draw_point)
                current += step_size
                counter++
            }
        }
    }

    private fun drawEyes(canvas: Canvas) {
        // 1
        text_paint.color = eyesColor
        text_paint.style = Paint.Style.FILL

        // 2
        val leftEyeRect = RectF(size * 0.32f, size * 0.23f, size * 0.43f, size * 0.50f)

        canvas.drawOval(leftEyeRect, text_paint)

        // 3
        val rightEyeRect = RectF(size * 0.57f, size * 0.23f, size * 0.68f, size * 0.50f)

        canvas.drawOval(rightEyeRect, text_paint)
    }

    private fun drawMouth(canvas: Canvas) {
        val mouthPath = Path()
        // 1
        mouthPath.moveTo(size * 0.22f, size * 0.7f)
        // 2
        mouthPath.quadTo(size * 0.50f, size * 0.80f, size * 0.78f, size * 0.70f)
        // 3
        mouthPath.quadTo(size * 0.50f, size * 0.90f, size * 0.22f, size * 0.70f)
        // 4
        text_paint.color = mouthColor
        text_paint.style = Paint.Style.FILL
        // 5
        canvas.drawPath(mouthPath, text_paint)
    }
}