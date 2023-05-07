package com.ingokodba.dragnav

import android.graphics.drawable.Drawable
import android.view.View
import androidx.fragment.app.Fragment
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates

interface MainFragmentInterface{
    fun iconsUpdated()
    fun goToPocetna()
    fun updateStuff()
    fun refreshCurrentMenu()
    fun toggleEditMode()
    fun selectedItemDeleted()
    var fragment: Fragment
}