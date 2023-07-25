package com.ingokodba.dragnav

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama

class ViewModel : ViewModel() {

    // The internal MutableLiveData that stores the status of the most recent request
    private val _popis_aplikacija = MutableLiveData<MutableList<AppInfo>>()
    private val _popis_aplikacija_filtered = MutableLiveData<MutableList<AppInfo>>()
    private var _icons = MutableLiveData<MutableMap<String, Drawable?>>()

    // The external immutable LiveData for the request status
    val appsList: LiveData<MutableList<AppInfo>> = _popis_aplikacija
    val appsListFiltered: LiveData<MutableList<AppInfo>> = _popis_aplikacija_filtered
    var icons: LiveData<MutableMap<String, Drawable?>> = _icons

    lateinit var currentMenu: KrugSAplikacijama
    var currentMenuId: Int = -1
    var trenutnoPrikazanaPolja: List<KrugSAplikacijama> = listOf()
    var max_subcounter:Int = -1
    var stack:MutableList<Pair<Int,Int>> = mutableListOf()
    var highestId = -1
    var no_draw_position:Int = -1
    var lastEnteredIntent: KrugSAplikacijama? = null
    var editMode:Boolean = false
    var addNewAppMode:Boolean = false
    var editSelected:Int = -1
    var pocetnaId:Int = -1
    var sviKrugovi:MutableList<KrugSAplikacijama> = mutableListOf()

    var currentlyLoadingApps: MutableList<String> = mutableListOf()

    fun initialize(){
        _icons.postValue(mutableMapOf())
        _popis_aplikacija.postValue(mutableListOf())
        trenutnoPrikazanaPolja = listOf()
        sviKrugovi = mutableListOf()
        currentMenuId = -1
    }

    /**
     * Call getMarsPhotos() on init so we can display status immediately.
     */
    init {
        initialize()
    }

    fun setFilteredApps(apps: MutableList<AppInfo>){
        _popis_aplikacija_filtered.postValue(apps)
    }

    fun addApps(apps: MutableList<AppInfo>){
        var concatenatedApps: MutableList<AppInfo> = _popis_aplikacija.value!!.plus(apps).toMutableList()
        concatenatedApps.sortBy { it.label.lowercase() }
        _popis_aplikacija.postValue(concatenatedApps)
        _popis_aplikacija_filtered.postValue(concatenatedApps)
        for(app in concatenatedApps){
            //Log.d("ingo", app.label + "->" + app.packageName)
        }
    }

    fun removeApp(app: AppInfo){
        _popis_aplikacija.value!!.remove(app)
    }
}