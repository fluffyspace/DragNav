package com.ingokodba.dragnav

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.RainbowMapa

class ViewModel : ViewModel() {

    // The internal MutableLiveData that stores the status of the most recent request
    private val _popis_aplikacija = MutableLiveData<MutableList<AppInfo>>()
    private val _rainbow_mape = MutableLiveData<MutableList<RainbowMapa>>()
    private val _rainbow_all = MutableLiveData<MutableList<EncapsulatedAppInfoWithFolder>>()
    private var _icons = MutableLiveData<MutableMap<String, Drawable?>>()

    // The external immutable LiveData for the request status
    val appsList: LiveData<MutableList<AppInfo>> = _popis_aplikacija
    val rainbowMape: LiveData<MutableList<RainbowMapa>> = _rainbow_mape
    val rainbowAll: LiveData<MutableList<EncapsulatedAppInfoWithFolder>> = _rainbow_all
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
    var favorites: Boolean = false

    var currentlyLoadingApps: MutableList<String> = mutableListOf()

    fun initialize(){
        _icons.value = mutableMapOf()
        _popis_aplikacija.value = mutableListOf()
        _rainbow_mape.value = mutableListOf()
        _rainbow_all.value = mutableListOf()
        trenutnoPrikazanaPolja = listOf()
        sviKrugovi = mutableListOf()
        currentMenuId = -1
    }

    var modeDistanceAccumulated: Int = 0
    var modeDistanceAccumulatedFavorites: Int = 0

    /**
     * Call getMarsPhotos() on init so we can display status immediately.
     */
    init {
        initialize()
    }

    fun setRainbowAll(list: MutableList<EncapsulatedAppInfoWithFolder>){
        _rainbow_all.value = list
    }

    fun updateRainbowAll(onlyfavorites: Boolean){
        val sve = getApps().filter{if(onlyfavorites) it.favorite else true }.map{EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)}.toMutableList().apply {
            addAll(rainbowMape.value!!.filter{if(onlyfavorites) it.favorite else true }.map{EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)})
            sortBy { if(it.folderName == null) it.apps.first().label.lowercase() else it.folderName!!.lowercase() }
        }
        _rainbow_all.value = sve
        Log.d("ingo22", Gson().toJson(sve.map{sveit -> sveit.apps.map{appit -> appit.label}}))
    }

    fun getApps(): List<AppInfo>{
        return curateAppsInFolders(if(!favorites) {
            appsList.value!!
        } else {
            appsList.value!!.filter { it.favorite }
        })
    }

    fun curateAppsInFolders(apps: List<AppInfo>): List<AppInfo>{
        val newapps = apps.toMutableList()
        for(rainbowMapa in rainbowMape.value!!){
            for(app in rainbowMapa.apps){
                newapps.remove(newapps.find{it.packageName == app.packageName})
                Log.d("ingo", "removing $app")
            }
        }
        return newapps
    }

    fun addRainbowMape(mape: MutableList<RainbowMapa>){
        Log.d("ingo", "mape $mape")
        if(mape.size != 0) _rainbow_mape.value = (_rainbow_mape.value!!.plus(mape).toMutableList())
        Log.d("ingo", "rainbowmape ${_rainbow_mape.value}")
    }

    fun updateRainbowMapa(mapa: RainbowMapa){
        val index = _rainbow_mape.value!!.indexOfFirst { it.id == mapa.id }
        _rainbow_mape.value = (_rainbow_mape.value!!.filter{it.id != mapa.id}.toMutableList().apply { add(index, mapa) }.toMutableList())
    }

    fun deleteRainbowMapa(mapa: RainbowMapa){
        _rainbow_mape.value = (_rainbow_mape.value!!.filter{it.id != mapa.id}.toMutableList())
    }

    fun addApps(apps: MutableList<AppInfo>){
        var concatenatedApps: MutableList<AppInfo> = _popis_aplikacija.value!!.plus(apps).toMutableList()
        concatenatedApps.sortBy { it.label.lowercase() }
        _popis_aplikacija.postValue(concatenatedApps)
        for(app in concatenatedApps){
            //Log.d("ingo", app.label + "->" + app.packageName)
        }
    }

    fun clearApps(){
        _popis_aplikacija.value = mutableListOf()
    }

    fun removeApp(app: AppInfo){
        _popis_aplikacija.value!!.remove(app)
    }
}