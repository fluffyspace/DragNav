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


class CircleView(context: Context, attrs: AttributeSet) : View(context, attrs){

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
    private var step_size:Double = Math.PI*0.25
    private var app_list:List<KrugSAplikacijama> = listOf()
    private var color_list:List<String> = listOf()
    private var no_draw_position:Int = -1
    private var polja_points:MutableList<Point> = mutableListOf()
    private var hovered_over:Int = -1
    val infodrawable = AppCompatResources.getDrawable(context, R.drawable.ic_outline_info_75)
    val adddrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_add_50)
    //lateinit var radapter:RAdapter

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

    var detectSize = 100
    var editMode:Boolean = false
    var addAppMode:Boolean = false
    var icons:MutableMap<String, Drawable?> = mutableMapOf()

    var mShowText:Boolean
    var textPos:Int
    var selected:Int = -1
    var amIHomeVar: Boolean = true

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

    fun setKrugSAplikacijamaList(list:List<KrugSAplikacijama>){
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
        detectSize = (size / 10).toInt()
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
        when(middleButtonState){
            //MIDDLE_BUTTON_EDIT -> drawEditButton(canvas)
            MIDDLE_BUTTON_CLOSE -> drawCloseButton(canvas)
            MIDDLE_BUTTON_CLOSE_INSIDE -> drawCloseButton(canvas, true)
            MIDDLE_BUTTON_HOME -> drawHomeButton(canvas)
            MIDDLE_BUTTON_CHECK -> drawCheckButton(canvas)
        }
        //drawEditButton(canvas)
        //drawCloseButton(canvas)
    }

    private var mEventListener: MainFragment.IMyEventListener? = null

    fun setEventListener(mEventListener: MainFragment.IMyEventListener?) {
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //Log.d("ingo", event.action.toString())
        val rect = Rect((event.x-detectSize).toInt(), (event.y-detectSize).toInt(), (event.x+detectSize).toInt(), (event.y+detectSize).toInt())
        var redniBrojPolja = 0
        var found = false
        var processed = false
        for(polje_point in polja_points){
            if(rect.contains(polje_point)){
                if(redniBrojPolja == app_list.size){
                    mEventListener?.onEventOccurred(event, MainActivity.ACTION_ADD, no_draw_position)
                } else {
                    mEventListener?.onEventOccurred(event, redniBrojPolja, no_draw_position)
                    found = true
                }
                break
            }
            redniBrojPolja++
        }
        if(!found || event.action == MotionEvent.ACTION_UP) {
            hovered_over = -1
            invalidate()
        }
        var sredina = false

        if (rect.contains(size_width / 2, size / 2)) {
            sredina = true
        }
        if (sredina && !sredina_processed) {
            if(editMode) {
                //mEventListener?.onEventOccurred(event, MainActivity.ACTION_ADD, no_draw_position)
                //Log.d("ingo", "editmode sredina yes")
            }
            if(addAppMode){
                mEventListener?.onEventOccurred(event, MainActivity.ACTION_ADD_APP, no_draw_position)
                processed = true
                sredina_processed = true
            } else if(middleButtonState == MIDDLE_BUTTON_HOME){
                Log.d("ingo", "sredina home")
                mEventListener?.onEventOccurred(event, MainActivity.ACTION_HOME, no_draw_position)
                processed = true
                sredina_processed = true
            }
        }
        if(event.action == MotionEvent.ACTION_MOVE && !addAppMode) {
            if (sredina) {
                if (middleButtonState == MIDDLE_BUTTON_CLOSE) {
                    changeMiddleButtonState(MIDDLE_BUTTON_CLOSE_INSIDE)
                    processed = true
                }
            } else {
                if (middleButtonState == MIDDLE_BUTTON_CLOSE_INSIDE) {
                    changeMiddleButtonState(MIDDLE_BUTTON_CLOSE)
                    processed = true
                }
            }
        }
        if(!editMode && !addAppMode){
            if(found){
                changeMiddleButtonState(MIDDLE_BUTTON_CLOSE)
            }
            if(event.action == MotionEvent.ACTION_UP){
                Log.d("ingo", "sredina debouncer reset")
                if(amIHomeVar) {
                    changeMiddleButtonState(MIDDLE_BUTTON_HIDE)
                } else {
                    changeMiddleButtonState(MIDDLE_BUTTON_HOME)
                }
                if (sredina && !sredina_processed) {
                    sredina_processed = true
                    Log.d("ingo", "sredina cancel")
                    mEventListener?.onEventOccurred(
                        event,
                        MainActivity.ACTION_CANCEL, no_draw_position
                    )
                } else {
                    mEventListener?.onEventOccurred(
                        event,
                        MainActivity.ACTION_LAUNCH, no_draw_position
                    )
                }
            }
        }
        if(event.action == MotionEvent.ACTION_UP){
            sredina_processed = false
        }
        return (found || processed)
    }

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

    private fun drawCheckButton(canvas: Canvas) {
        canvas.apply {
            drawCircle(size_width / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            val drawable: Drawable? = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_check_50)
            drawable?.setTint(Color.WHITE)
            val bitmap: Bitmap? = drawable?.toBitmap()
            if (bitmap != null) {
                drawBitmap(
                    bitmap, null, Rect(
                        (size_width / 2f - detectSize / 2).toInt(),
                        (size / 2f - detectSize / 2).toInt(),
                        (size_width / 2f + detectSize / 2).toInt(),
                        (size / 2f + detectSize / 2).toInt()
                    ), thick_paint
                )
            }
        }
    }

    private fun drawHomeButton(canvas: Canvas){
        canvas.apply {
            drawCircle(size_width / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            val drawable: Drawable? = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_home_100)
            drawable?.setTint(Color.WHITE)
            val bitmap: Bitmap? = drawable?.toBitmap()
            if (bitmap != null) {
                drawBitmap(
                    bitmap, null, Rect(
                        (size / 2f - detectSize / 2).toInt(),
                        (size / 2f - detectSize / 2).toInt(),
                        (size / 2f + detectSize / 2).toInt(),
                        (size / 2f + detectSize / 2).toInt()
                    ), thick_paint
                )
            }
            //drawText("Home", size/2f, size/2f+20, text_paint)
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
            drawCircle(size_width / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            var plus_margin = detectSize/2
            thick_paint.strokeWidth = 15f
            var center = (size / 2f)
            drawLine(center, center+(-detectSize+plus_margin).toFloat(), center, center+(detectSize-plus_margin).toFloat(), thick_paint)
            drawLine(center+(-detectSize+plus_margin).toFloat(),center, center+(detectSize-plus_margin).toFloat(), center, thick_paint)
        }
    }

    private fun drawCloseButton(canvas: Canvas, inside: Boolean = false){
        canvas.apply {
            if(inside){
                drawCircle(size_width / 2f, size / 2f, detectSize.toFloat(), close_inside_paint)
                drawCircle(size_width / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            } else {
                drawCircle(size_width / 2f, size / 2f, detectSize.toFloat(), empty_circle_paint)
            }
            var plus_margin = detectSize/1.5
            thick_paint.strokeWidth = 12f
            var center = (size / 2f)
            drawLine((size_width/2f)+(-detectSize+plus_margin).toFloat(), center+(-detectSize+plus_margin).toFloat(), (size_width/2f)+(detectSize-plus_margin).toFloat(), center+(detectSize-plus_margin).toFloat(), thick_paint)
            drawLine((size_width/2f)+(-detectSize+plus_margin).toFloat(), center+(+detectSize-plus_margin).toFloat(), (size_width/2f)+(detectSize-plus_margin).toFloat(), center-(detectSize-plus_margin).toFloat(), thick_paint)
        }
    }

    fun setAppList(list:List<AppInfo>){

    }

    fun colorToHex(color: Color): String? {
        var hex = (
            "%02x%02x%02x").format(
            floor(color.red()*255).toInt(),
            floor(color.green()*255).toInt(),
            floor(color.blue()*255).toInt()
        )
        return hex.uppercase()
    }

    private fun drawPolja(canvas: Canvas){
        val queued_texts: MutableList<QueuedText> = mutableListOf()
        val hexes = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        val transparenthex = hexes[floor((transparency/1.0)*(hexes.size-1)).toInt()] + "" + hexes[floor((transparency/1.0)*(hexes.size-1)).toInt()]
        Log.d("ingo", "transparent ${transparenthex}")
        polja_points.clear()
        canvas.apply {
            val radius = size / 3f
            var currentStepValue = 0.0
            val cx = size_width/2f
            val cy = size/2f
            if(showBigCircle) drawCircle(cx, cy, radius, empty_circle_paint)
            var counter = 0
            var over_no_draw_position = false
            while(currentStepValue < Math.PI*2){
                if(!over_no_draw_position && counter == no_draw_position){
                    currentStepValue += step_size
                    over_no_draw_position = true
                    continue
                }
                val x = Math.sin(currentStepValue)*radius
                val y = Math.cos(currentStepValue)*radius
                val draw_pointF = PointF( ((cx+x).toFloat()), ((cy+y).toFloat()) )
                val draw_point = Point( draw_pointF.x.toInt(), draw_pointF.y.toInt() )
                var text:String
                // rade se rupe za krugove
                if((counter >= app_list.size && draw_circles) || counter < app_list.size ) {
                    drawCircle(
                        draw_pointF.x,
                        draw_pointF.y,
                        detectSize.toFloat(), clear_boja
                    )
                }
                if(counter >= app_list.size){
                    // crtaju se prazni krugovi
                    text = ""
                    if (draw_circles) drawCircle(
                        draw_pointF.x,
                        draw_pointF.y,
                        detectSize.toFloat(),
                        empty_circle_paint
                    )
                } else {
                    text = app_list[counter].text
                    Log.d("ingo", "boja za ${app_list[counter].text} je ${app_list[counter].color}")
                    try {
                        //val transcolor = colorToHex(Color.valueOf(app_list[counter].color.toInt())) + transparenthex
                        if (app_list[counter].color != ""){
                            val boja = Color.valueOf(app_list[counter].color.toInt())
                            Log.d("ingo", "transparent " + boja + " " + "#" + transparenthex + colorToHex(boja))
                            circle_paint.color = Color.parseColor("#" + transparenthex + colorToHex(boja))
                        } else {
                            circle_paint.color = Color.parseColor("#55000000")
                        }
                        Log.d("ingo", "boja je ${circle_paint.color}")
                    } catch (e: NumberFormatException ){
                        circle_paint.color = Color.parseColor("#55000000")
                        Log.d("ingo", "boja nemoguće za dešifrirati1")
                    } catch (e: IllegalArgumentException ){
                        circle_paint.color = Color.parseColor("#55000000")
                        Log.d("ingo", "boja nemoguće za dešifrirati2")
                    }
                    if(app_list[counter].nextIntent != "") {
                        // app
                        var bitmap: Bitmap?
                        var scale = 1.0
                        if(app_list[counter].nextIntent == MainActivity.ACTION_APPINFO){
                            drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), semi_transparent_paint)
                            bitmap = infodrawable?.toBitmap()
                        } else if(app_list[counter].nextIntent == MainActivity.ACTION_ADD_PRECAC){
                            drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), semi_transparent_paint)
                            bitmap = adddrawable?.apply { setTint(Color.WHITE) }?.toBitmap()
                            scale = 0.5
                        } else {
                            //Log.d("ingo", "pokusavam boju " + color_list[counter])
                            try {
                                circle_paint.color = color_list[counter].toInt()
                            } catch (e: NumberFormatException) {
                                e.printStackTrace()
                            }
                            bitmap = icons[app_list[counter].nextIntent]?.toBitmap()
                            //Log.d("ingo", "applist[" + counter + "].nextIntent = " + app_list[counter].nextIntent )
                        }
                        if (draw_icons && bitmap != null) {
                            drawBitmap(
                                bitmap, null, Rect(
                                    (draw_pointF.x - detectSize*scale).toInt(),
                                    (draw_pointF.y - detectSize*scale).toInt(),
                                    (draw_pointF.x + detectSize*scale).toInt(),
                                    (draw_pointF.y + detectSize*scale).toInt()
                                ), null
                            )
                            if(show_app_names && !app_list[counter].shortcut){
                                queued_texts.add(QueuedText(text, draw_pointF.x, draw_pointF.y+detectSize+text_size))
                                //drawText(text, draw_pointF.x, draw_pointF.y+detectSize+text_size, text_paint)
                            }
                        } else {
                            drawCircle(
                                draw_pointF.x,
                                draw_pointF.y,
                                detectSize.toFloat(),
                                circle_paint
                            )
                            queued_texts.add(QueuedText(text,draw_pointF.x, draw_pointF.y+(text_size/2)))
                            //drawText(text, draw_pointF.x, draw_pointF.y+(text_size/2).toFloat(), text_paint)
                        }
                        if(app_list[counter].shortcut){
                            // prebojaj polje zato što je shortcut pa ide tekst preko ikone
                            drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), semi_transparent_paint)
                            //if(draw_circles) drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), empty_circle_paint)
                            queued_texts.add(QueuedText(text, draw_pointF.x, draw_pointF.y+(text_size/2)))
                            //drawText(text, draw_pointF.x, draw_pointF.y+(text_size/2).toFloat(), text_paint)
                        }
                    } else {
                        // folder
                        drawCircle(
                            draw_pointF.x,
                            draw_pointF.y,
                            detectSize.toFloat(),
                            circle_paint
                        )
                        queued_texts.add(QueuedText(text,draw_pointF.x, draw_pointF.y+(text_size/2)-5))
                        //drawText(text, draw_pointF.x, draw_pointF.y+(text_size/2)-5, text_paint)
                    }
                    polja_points.add(draw_point)
                }
                if(counter == selected){
                    //drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), yellow_paint)
                    drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), thick_paint)
                }

                if(counter == hovered_over){
                    //drawCircle(draw_pointF.x, draw_pointF.y, detectSize.toFloat(), thick_paint)
                }

                //drawText(text, draw_pointF.x, draw_pointF.y+20, text_paint)
                currentStepValue += step_size
                counter++
            }
            for(queued_text in queued_texts){
                drawText(queued_text.text, queued_text.x, queued_text.y, text_paint)
            }
            queued_texts.clear()
        }
    }
}

class QueuedText(text: String, x: Float, y: Float){
    val text = text
    val x = x
    val y = y
}