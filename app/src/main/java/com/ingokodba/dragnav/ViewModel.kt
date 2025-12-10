package com.ingokodba.dragnav

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ingokodba.dragnav.compose.AppNotification
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.RainbowMapa
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class DisplayMode {
    ALL_APPS,        // Show all apps (no folders), including apps that are in folders
    FAVORITES_ONLY   // Show only favorite apps and favorite folders
}

class ViewModel : ViewModel() {

    // The internal MutableLiveData that stores the status of the most recent request (kept for backward compatibility)
    private val _popis_aplikacija = MutableLiveData<MutableList<AppInfo>>()
    private val _rainbow_mape = MutableLiveData<MutableList<RainbowMapa>>()
    private var _icons = MutableLiveData<MutableMap<String, Drawable?>>()
    private val _notifications = MutableLiveData<List<AppNotification>>()

    // New Flow-based state
    private val _appsListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _rainbowMapeFlow = MutableStateFlow<List<RainbowMapa>>(emptyList())
    private val _iconsFlow = MutableStateFlow<Map<String, Drawable?>>(emptyMap())
    private val _notificationsFlow = MutableStateFlow<List<AppNotification>>(emptyList())
    private val _displayModeFlow = MutableStateFlow(DisplayMode.ALL_APPS)

    // The external immutable LiveData for the request status (kept for backward compatibility)
    val appsList: LiveData<MutableList<AppInfo>> = _popis_aplikacija
    val rainbowMape: LiveData<MutableList<RainbowMapa>> = _rainbow_mape
    var rainbowFiltered: MutableList<EncapsulatedAppInfoWithFolder> = mutableListOf()
    var rainbowAll: MutableList<EncapsulatedAppInfoWithFolder> = mutableListOf()
    var icons: LiveData<MutableMap<String, Drawable?>> = _icons
    val notifications: LiveData<List<AppNotification>> = _notifications

    // New Flow-based public APIs
    val appsListFlow: StateFlow<List<AppInfo>> = _appsListFlow.asStateFlow()
    val rainbowMapeFlow: StateFlow<List<RainbowMapa>> = _rainbowMapeFlow.asStateFlow()
    val iconsFlow: StateFlow<Map<String, Drawable?>> = _iconsFlow.asStateFlow()
    val notificationsFlow: StateFlow<List<AppNotification>> = _notificationsFlow.asStateFlow()

    // Computed flows
    val rainbowFilteredFlow: StateFlow<List<EncapsulatedAppInfoWithFolder>> = combine(
        _appsListFlow,
        _rainbowMapeFlow,
        _displayModeFlow
    ) { apps, folders, mode ->
        computeRainbowFiltered(apps, folders, mode)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val rainbowAllFlow: StateFlow<List<EncapsulatedAppInfoWithFolder>> = combine(
        _appsListFlow,
        _rainbowMapeFlow
    ) { apps, folders ->
        computeRainbowAll(apps, folders)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
        _icons.value = mutableMapOf()
        _popis_aplikacija.value = mutableListOf()
        _rainbow_mape.value = mutableListOf()
        _notifications.value = emptyList()
        trenutnoPrikazanaPolja = listOf()
        sviKrugovi = mutableListOf()
        currentMenuId = -1

        // Initialize flows
        _appsListFlow.value = emptyList()
        _rainbowMapeFlow.value = emptyList()
        _iconsFlow.value = emptyMap()
        _notificationsFlow.value = emptyList()
        _displayModeFlow.value = DisplayMode.ALL_APPS
    }

    /**
     * Initialize database flows to automatically sync with database changes
     */
    fun initializeDatabaseFlows(
        appsFlow: Flow<List<AppInfo>>,
        foldersFlow: Flow<List<RainbowMapa>>
    ) {
        viewModelScope.launch {
            appsFlow.collect { apps ->
                _appsListFlow.value = apps
                // Keep LiveData in sync for backward compatibility
                _popis_aplikacija.postValue(apps.toMutableList())
            }
        }

        viewModelScope.launch {
            foldersFlow.collect { folders ->
                _rainbowMapeFlow.value = folders
                // Keep LiveData in sync for backward compatibility
                _rainbow_mape.postValue(folders.toMutableList())
            }
        }
    }

    /**
     * Compute filtered apps based on display mode
     */
    private fun computeRainbowFiltered(
        apps: List<AppInfo>,
        folders: List<RainbowMapa>,
        mode: DisplayMode
    ): List<EncapsulatedAppInfoWithFolder> {
        Log.d("ViewModel", "computeRainbowFiltered called with mode=$mode")
        Log.d("ViewModel", "apps has ${apps.size} apps")
        Log.d("ViewModel", "folders has ${folders.size} folders")

        val favCount = apps.count { it.favorite }
        Log.d("ViewModel", "Found $favCount favorite apps in appsList")

        val filtered = when (mode) {
            DisplayMode.FAVORITES_ONLY -> {
                val favoriteApps = apps
                    .filter { it.favorite }
                    .map { EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite) }

                val favoriteFolders = folders
                    .filter { it.favorite }
                    .map { EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite) }

                Log.d("ViewModel", "FAVORITES_ONLY: ${favoriteApps.size} favorite apps, ${favoriteFolders.size} favorite folders")

                (favoriteApps + favoriteFolders).sortedBy {
                    if (it.folderName != null) {
                        it.folderName?.lowercase() ?: ""
                    } else {
                        it.apps.firstOrNull()?.label?.lowercase() ?: ""
                    }
                }
            }
            DisplayMode.ALL_APPS -> {
                val allApps = apps.map {
                    EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)
                }

                Log.d("ViewModel", "ALL_APPS: ${allApps.size} total apps")

                allApps.sortedBy {
                    if (it.apps.isNotEmpty()) it.apps.first().label.lowercase() else ""
                }
            }
        }

        Log.d("ViewModel", "computeRainbowFiltered returning ${filtered.size} items")
        return filtered
    }

    /**
     * Compute all apps including folders
     */
    private fun computeRainbowAll(
        apps: List<AppInfo>,
        folders: List<RainbowMapa>
    ): List<EncapsulatedAppInfoWithFolder> {
        val allApps = apps.map {
            EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)
        }
        val allFolders = folders.map {
            EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)
        }

        return (allApps + allFolders).sortedBy {
            if (it.folderName == null && it.apps.isNotEmpty()) {
                it.apps.first().label.lowercase()
            } else {
                it.folderName?.lowercase() ?: ""
            }
        }
    }

    /**
     * Set display mode (all apps or favorites only)
     */
    fun setDisplayMode(mode: DisplayMode) {
        _displayModeFlow.value = mode
    }

    var modeDistanceAccumulated: Int = 0
    var modeDistanceAccumulatedFavorites: Int = 0

    /**
     * Call getMarsPhotos() on init so we can display status immediately.
     */
    init {
        initialize()
    }

    fun setRainbowFilteredValues(list: MutableList<EncapsulatedAppInfoWithFolder>){
        rainbowFiltered = list
    }

    fun updateRainbowAll(){
        val filtered = appsList.value!!.map{EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)}.toMutableList().apply {
            addAll(rainbowMape.value!!.map{EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite)})
            sortBy { if(it.folderName == null && it.apps.isNotEmpty()) it.apps.first().label.lowercase() else it.folderName?.lowercase() ?: "" }
        }
        rainbowAll = filtered
    }

    fun updateRainbowFiltered(displayMode: DisplayMode){
        Log.d("ViewModel", "updateRainbowFiltered called with displayMode=$displayMode")
        Log.d("ViewModel", "appsList.value has ${appsList.value?.size ?: 0} apps")
        Log.d("ViewModel", "rainbowMape.value has ${rainbowMape.value?.size ?: 0} folders")

        // Count favorite apps for debugging
        val favCount = appsList.value?.count { it.favorite } ?: 0
        Log.d("ViewModel", "Found $favCount favorite apps in appsList")
        appsList.value?.filter { it.favorite }?.forEach { app ->
            Log.d("ViewModel", "  Favorite app: ${app.label} (${app.packageName}), favorite=${app.favorite}")
        }

        val filtered = when (displayMode) {
            DisplayMode.FAVORITES_ONLY -> {
                // Show only favorite apps AND favorite folders
                val favoriteApps = appsList.value!!
                    .filter { it.favorite }
                    .map { EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite) }

                val favoriteFolders = rainbowMape.value!!
                    .filter { it.favorite }
                    .map { EncapsulatedAppInfoWithFolder(it.apps, it.folderName, it.favorite) }

                Log.d("ViewModel", "FAVORITES_ONLY: ${favoriteApps.size} favorite apps, ${favoriteFolders.size} favorite folders")

                (favoriteApps + favoriteFolders).toMutableList().apply {
                    sortBy {
                        if (it.folderName != null) {
                            it.folderName?.lowercase() ?: ""
                        } else {
                            it.apps.firstOrNull()?.label?.lowercase() ?: ""
                        }
                    }
                }
            }
            DisplayMode.ALL_APPS -> {
                // Show ALL apps (including apps that are in folders), but don't show folders
                val allApps = appsList.value!!.map {
                    EncapsulatedAppInfoWithFolder(listOf(it), null, it.favorite)
                }.toMutableList()

                Log.d("ViewModel", "ALL_APPS: ${allApps.size} total apps")
                val favInAll = allApps.count { it.favorite == true }
                Log.d("ViewModel", "ALL_APPS: $favInAll apps have favorite=true")

                allApps.apply {
                    sortBy { if(it.apps.isNotEmpty()) it.apps.first().label.lowercase() else "" }
                }
            }
        }

        rainbowFiltered = filtered
        Log.d("ViewModel", "rainbowFiltered set to ${filtered.size} items")
        Log.d("ingo22", Gson().toJson(filtered.map{sveit -> sveit.apps.map{appit -> appit.label}}))
    }

    // Deprecated: Use updateRainbowFiltered(DisplayMode) instead
    @Deprecated("Use updateRainbowFiltered(DisplayMode) instead", ReplaceWith("updateRainbowFiltered(if (onlyfavorites) DisplayMode.FAVORITES_ONLY else DisplayMode.ALL_APPS)"))
    fun updateRainbowFiltered(onlyfavorites: Boolean){
        updateRainbowFiltered(if (onlyfavorites) DisplayMode.FAVORITES_ONLY else DisplayMode.ALL_APPS)
    }

    fun curateAppsInFolders(apps: List<AppInfo>): List<AppInfo>{
        val newapps = apps.toMutableList()
        for(rainbowMapa in rainbowMape.value!!){
            for(app in rainbowMapa.apps){
                newapps.remove(newapps.find{it.packageName == app.packageName})
                //Log.d("ingo", "removing $app")
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
        var concatenatedApps: MutableList<AppInfo> = _popis_aplikacija.value!!.plus(apps).filter{it.packageName != "com.ingokodba.dragnav"}.toMutableList()
        concatenatedApps.sortBy { it.label.lowercase() }
        _popis_aplikacija.postValue(concatenatedApps)
        for(app in concatenatedApps){
            Log.d("ingo", app.label + "->" + app.packageName)
        }
    }

    fun clearApps(){
        _popis_aplikacija.value = mutableListOf()
    }

    fun removeApp(app: AppInfo){
        _popis_aplikacija.value!!.remove(app)
    }

    fun setIcons(icons: MutableMap<String, Drawable?>){
        _icons.value = icons
        _iconsFlow.value = icons
    }

    fun updateNotifications(notifications: List<AppNotification>) {
        _notifications.postValue(notifications)
        _notificationsFlow.value = notifications
    }
}