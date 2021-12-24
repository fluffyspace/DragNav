package com.example.dragnav

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dragnav.modeli.AppInfo
import kotlin.random.Random

class NewRAdapterViewModel  : ViewModel() {

    // The internal MutableLiveData that stores the status of the most recent request
    private val _popis_aplikacija = MutableListLiveData<AppInfo>()
    private var _icons = MutableLiveData<MutableMap<String, Drawable?>>()

    // The external immutable LiveData for the request status
    val appsList: LiveData<List<AppInfo>> = _popis_aplikacija
    var icons: LiveData<MutableMap<String, Drawable?>> = _icons

    /**
     * Call getMarsPhotos() on init so we can display status immediately.
     */
    init {
        _icons.value = mutableMapOf()
        _popis_aplikacija.clear()
    }

    fun addApps(apps: MutableList<AppInfo>){
        _popis_aplikacija.addAll(apps)
        _popis_aplikacija.sortBy { it.label.lowercase() }
    }


}