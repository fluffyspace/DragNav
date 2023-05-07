package com.ingokodba.dragnav

import android.graphics.drawable.Drawable
import android.view.View
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates

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
    fun setEventListener(mEventListener: MainFragment.IMyEventListener?)
    var icons: MutableMap<String, Drawable?>
    var addAppMode: Boolean
    fun selectPolje(id:Int)
    fun updateDesign()
    var view: View
}