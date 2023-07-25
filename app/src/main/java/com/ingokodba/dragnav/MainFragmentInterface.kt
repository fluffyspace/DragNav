package com.ingokodba.dragnav

import androidx.fragment.app.Fragment

interface MainFragmentInterface{
    fun iconsUpdated()
    fun goToHome()
    fun refreshCurrentMenu()
    fun toggleEditMode()
    fun selectedItemDeleted()
    var fragment: Fragment
}