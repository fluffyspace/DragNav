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
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates
import com.ingokodba.dragnav.modeli.MiddleButtonStates.*
import kotlin.math.floor


class PhoneKeypadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs){

    private val text_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border_paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pressed_paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var size = 320
    private var size_width = 320

    private var pressed_button:Int = -1
    //val calldrawable = AppCompatResources.getDrawable(context, R.drawable.ic_outline_info_75)

    var text_size = 18f
    var view: View = this

    init {
        text_paint.color = Color.WHITE
        text_paint.textAlign = Paint.Align.CENTER
        text_paint.style = Paint.Style.FILL
        text_paint.textSize = text_size
        text_paint.setShadowLayer(4.0f, 1.0f, 2.0f, Color.BLACK)

        border_paint.color = Color.WHITE
        border_paint.style = Paint.Style.STROKE
        border_paint.strokeWidth = 5f

        pressed_paint.color = Color.parseColor("#55000000")
        pressed_paint.style = Paint.Style.FILL
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
        text_size = size/9f
        text_paint.textSize = text_size

        setMeasuredDimension(size_width, size)
    }

    override fun onDraw(canvas: Canvas) {
        // call the super method to keep any drawing from the parent side.
        super.onDraw(canvas)
        // settings

        drawPolja(canvas)
    }

    fun diram(broj: Int){
        mEventListener?.onEventOccurred(broj)
    }

    private var mEventListener: MainFragmentTipke.IMyEventListener? = null

    fun setEventListener(mEventListener: MainFragmentTipke.IMyEventListener?) {
        this.mEventListener = mEventListener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN) {
            val point = Point(event.x.toInt(), event.y.toInt())
            val pressed_rect_index = brojevi_rects.indexOfFirst { it.contains(point) }
            if (pressed_rect_index != -1) {
                Log.d("ingo", "diram $pressed_rect_index")
                pressed_button = pressed_rect_index
                invalidate()
                diram(pressed_rect_index)
            }
        } else if(event.action == MotionEvent.ACTION_UP){
            pressed_button = -1
            invalidate()
        }
        return true
    }

    var brojevi_rects: MutableList<Rect> = mutableListOf()

    private fun drawPolja(canvas: Canvas){
        brojevi_rects.clear()
        canvas.apply {
            val polje_width = size_width / 3f
            val polje_height = size / 4f
            drawText("0", (polje_width/2)+polje_width*1, (polje_height/2)+polje_height*3+text_paint.textSize/2, text_paint)
            val rect_zero = Rect((polje_width*1).toInt(),
                (polje_height*3).toInt(), (polje_width*(2)).toInt(),
                (polje_height*(4)).toInt())
            brojevi_rects.add(rect_zero)
            if(pressed_button == 0){
                drawRect(rect_zero, pressed_paint)
            }
            drawText("C", (polje_width/2)+polje_width*2, (polje_height/2)+polje_height*3+text_paint.textSize/2, text_paint)
            val rect_c = Rect((polje_width*2).toInt(),
                (polje_height*3).toInt(), (polje_width*(3)).toInt(),
                (polje_height*(4)).toInt())
            if(pressed_button == 10){
                drawRect(rect_c, pressed_paint)
            }
            for(i in 0..2){
                for(j in 0..2){
                    val broj = i+1+j*3
                    drawText(broj.toString(), (polje_width/2)+polje_width*i, (polje_height/2)+polje_height*j+text_paint.textSize/2, text_paint)
                    val rect = Rect((polje_width*j).toInt(),
                        (polje_height*i).toInt(), (polje_width*(j+1)).toInt(),
                        (polje_height*(i+1)).toInt())
                    if(pressed_button == i*3+j+1){
                        drawRect(rect, pressed_paint)
                        Log.d("ingo", "crtam ${i*3+j+1}")
                    }
                    brojevi_rects.add(rect)
                }
            }
            brojevi_rects.add(rect_c)
            drawLine(polje_width, 0f, polje_width, size.toFloat(), border_paint)
            drawLine(polje_width*2, 0f, polje_width*2, size.toFloat(), border_paint)
            drawLine(0f, polje_height, size_width.toFloat(), polje_height, border_paint)
            drawLine(0f, polje_height*2, size_width.toFloat(), polje_height*2, border_paint)
            drawLine(0f, polje_height*3, size_width.toFloat(), polje_height*3, border_paint)
        }
    }
}