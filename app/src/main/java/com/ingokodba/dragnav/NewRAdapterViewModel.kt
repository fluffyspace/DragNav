package com.ingokodba.dragnav

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ingokodba.dragnav.baza.AppDatabase
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MeniJednoPolje
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewRAdapterViewModel : ViewModel() {

    // The internal MutableLiveData that stores the status of the most recent request
    private val _popis_aplikacija = MutableLiveData<MutableList<AppInfo>>()
    private var _icons = MutableLiveData<MutableMap<String, Drawable?>>()

    // The external immutable LiveData for the request status
    val appsList: LiveData<MutableList<AppInfo>> = _popis_aplikacija
    var icons: LiveData<MutableMap<String, Drawable?>> = _icons

    var lastTextViewEnteredCounter:Int = -1
    lateinit var currentMenu: MeniJednoPolje
    var currentMenuId: Int = -1
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

    fun initialize(){
        _icons.postValue(mutableMapOf())
        _popis_aplikacija.postValue(mutableListOf())
        currentSubmenuList = listOf()
        listaMenija = mutableListOf()
        currentMenuId = -1
    }

    /**
     * Call getMarsPhotos() on init so we can display status immediately.
     */
    init {
        initialize()
    }

    fun addApps(apps: MutableList<AppInfo>){
        var concatenated: MutableList<AppInfo> = _popis_aplikacija.value!!.plus(apps).toMutableList()
        concatenated.sortBy { it.label.lowercase() }
        _popis_aplikacija.postValue(concatenated)
        for(app in concatenated){
        Log.d("ingo", "" + app.label + "->" + app.packageName)
        }
    }

    fun removeApp(app: AppInfo){
        _popis_aplikacija.value!!.remove(app)
    }
}