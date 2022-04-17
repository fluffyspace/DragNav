package com.ingokodba.dragnav

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MeniJednoPolje

class NewRAdapterViewModel  : ViewModel() {

    // The internal MutableLiveData that stores the status of the most recent request
    private val _popis_aplikacija = MutableListLiveData<AppInfo>()
    private var _icons = MutableLiveData<MutableMap<String, Drawable?>>()

    // The external immutable LiveData for the request status
    val appsList: LiveData<List<AppInfo>> = _popis_aplikacija
    var icons: LiveData<MutableMap<String, Drawable?>> = _icons

    var lastTextViewEnteredCounter:Int = -1
    lateinit var currentMenu: MeniJednoPolje
    var currentSubmenuList: List<MeniJednoPolje> = listOf()
    var max_subcounter:Int = -1
    var stack:MutableList<Pair<Int,Int>> = mutableListOf()
    var highestId = -1
    var selected_global:Int = -1
    var lastEnteredIntent: MeniJednoPolje? = null
    var editMode:Boolean = false
    var addNewAppMode:Boolean = false
    var editSelected:Int = -1
    var pocetnaId:Int = -1
    var listaMenija:MutableList<MeniJednoPolje> = mutableListOf()

    /**
     * Call getMarsPhotos() on init so we can display status immediately.
     */
    init {
        _icons.postValue(mutableMapOf())
        _popis_aplikacija.clear()
    }

    fun addApps(apps: MutableList<AppInfo>){
        _popis_aplikacija.addAll(apps)
        _popis_aplikacija.sortBy { it.label.lowercase() }
    }

    fun removeApp(app: AppInfo){
        _popis_aplikacija.remove(app)
    }
}