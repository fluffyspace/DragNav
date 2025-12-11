package com.ingokodba.dragnav

import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates

/**
 * Event listener interface for circle view touch events.
 * Moved from MainFragment during Compose migration.
 */
interface ICircleViewEventListener {
    fun onEventOccurred(event: MotionEvent, counter: Int, current: Int)
}

interface UiComponent{
    fun amIHome(ami:Boolean?)
    var amIHomeVar: Boolean
    fun deselectAll()
    fun setColorList(list:List<String>)
    fun setKrugSAplikacijamaList(list:List<KrugSAplikacijama>)
    fun setAppList(list:List<AppInfo>)
    fun setPosDontDraw(position:Int)
    var editMode: Boolean
    fun changeMiddleButtonState(state: MiddleButtonStates)
    fun invalidate()
    fun setEventListener(mEventListener: ICircleViewEventListener?)
    var icons: MutableMap<String, Drawable?>
    var addAppMode: Boolean
    fun selectPolje(id:Int)
    fun updateDesign()
    var view: View
}