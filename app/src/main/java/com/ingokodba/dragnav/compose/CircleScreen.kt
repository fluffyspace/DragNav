package com.ingokodba.dragnav.compose

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dragnav.R
import com.ingokodba.dragnav.*
import com.ingokodba.dragnav.modeli.KrugSAplikacijama
import com.ingokodba.dragnav.modeli.MiddleButtonStates

/**
 * CircleScreen - Compose wrapper for the circular navigation UI mode
 * Supports CIRCLE, CIRCLE_RIGHT_HAND, and CIRCLE_LEFT_HAND variants
 */
@Composable
fun CircleScreen(
    mainActivity: MainActivityCompose,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = viewModel(),
    rightHanded: Boolean = false,
    leftHanded: Boolean = false
) {
    val context = LocalContext.current

    // Collect Flow-based state
    val appsList by viewModel.appsListFlow.collectAsStateWithLifecycle()
    val icons by viewModel.iconsFlow.collectAsStateWithLifecycle()

    // Local UI state
    var selectedText by remember { mutableStateOf(context.getString(R.string.home)) }
    var showAddMenu by remember { mutableStateOf(false) }

    // View references (no recomposition triggers)
    val circleViewRef = remember { mutableStateOf<CircleView?>(null) }
    val bottomMenuViewRef = remember { mutableStateOf<BottomMenuView?>(null) }

    // Helper functions
    fun getPolje(id: Int): KrugSAplikacijama? {
        return viewModel.sviKrugovi.find { it.id == id }
    }

    fun getSubPolja(id: Int): MutableList<KrugSAplikacijama> {
        val lista: MutableList<KrugSAplikacijama> = mutableListOf()
        val polje1 = getPolje(id)
        if (polje1 != null) {
            for (polje2 in viewModel.sviKrugovi) {
                if (polje1.polja!!.contains(polje2.id)) {
                    lista.add(polje2)
                }
            }
            Log.d("CircleScreen", "getSubPolja of $id is ${lista.map { it.text }}")
            return lista
        }
        return mutableListOf()
    }

    fun prikaziPoljaKruga(idKruga: Int, selected: Int) {
        var precaci: MutableList<KrugSAplikacijama> = mutableListOf()
        val trenutnoPolje = getPolje(idKruga)
        val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        if (launcherApps.hasShortcutHostPermission()) {
            val precaci_info = trenutnoPolje?.let {
                mainActivity.getShortcutFromPackage(it.nextIntent)
            }
            precaci = precaci_info?.map {
                KrugSAplikacijama(
                    id = 0,
                    text = it.shortLabel.toString(),
                    nextIntent = it.`package`,
                    nextId = it.id,
                    shortcut = true
                )
            } as MutableList<KrugSAplikacijama>
        }

        val isHome = idKruga == viewModel.pocetnaId
        circleViewRef.value?.amIHome(isHome)

        Log.d("CircleScreen", "prikazi prečace ${precaci.map { "${it.text}->${it.nextIntent}->${it.nextId}" }}")
        var polja = getSubPolja(idKruga)

        if (trenutnoPolje?.nextIntent != "") {
            precaci.add(
                KrugSAplikacijama(
                    id = 0,
                    text = "App info",
                    nextIntent = MainActivity.ACTION_APPINFO,
                    nextId = trenutnoPolje!!.nextIntent
                )
            )
        } else {
            val amIHomeVar = circleViewRef.value?.amIHomeVar ?: true
            if (!((amIHomeVar && precaci.size + polja.size >= 8) || (!amIHomeVar && precaci.size + polja.size >= 7))) {
                polja.add(
                    KrugSAplikacijama(
                        id = 0,
                        text = "",
                        nextIntent = MainActivity.ACTION_ADD_PRECAC,
                        nextId = trenutnoPolje!!.nextIntent
                    )
                )
            }
        }

        viewModel.trenutnoPrikazanaPolja = precaci + polja
        circleViewRef.value?.setColorList(
            IntArray(precaci.size) { Color.WHITE }.map { it.toString() } + polja.map { it.color }
        )
        circleViewRef.value?.setKrugSAplikacijamaList(viewModel.trenutnoPrikazanaPolja)
        Log.d("CircleScreen", "currentSubmenuList ${viewModel.trenutnoPrikazanaPolja.map { it.text }}")
        circleViewRef.value?.setPosDontDraw(selected)
        viewModel.no_draw_position = selected
        viewModel.max_subcounter = viewModel.trenutnoPrikazanaPolja.size
    }

    fun prebaciMeni(id: Int, counter: Int, nostack: Boolean = false): KrugSAplikacijama? {
        val polje = getPolje(id)
        Log.d("CircleScreen", "prebaciMeni $id")
        if (polje != null) {
            viewModel.currentMenu = polje
            viewModel.currentMenuId = id
            prikaziPoljaKruga(polje.id, counter)

            selectedText = polje.text
            if (!nostack) {
                viewModel.stack.add(Pair(viewModel.currentMenu.id, counter))
                Log.d("CircleScreen", "adding ${viewModel.currentMenu.text} to viewModel.stack.")
            }
            return polje
        } else {
            Log.d("CircleScreen", "prebaciMeni null!")
        }
        return null
    }

    fun goToHome() {
        Log.d("CircleScreen", "pocetna ${viewModel.pocetnaId}")
        viewModel.stack.clear()
        prebaciMeni(viewModel.pocetnaId, -1)
        selectedText = context.getString(R.string.home)
    }

    fun deYellowAll() {
        circleViewRef.value?.deselectAll()
        viewModel.editSelected = -1
        bottomMenuViewRef.value?.selectedId = -1
        bottomMenuViewRef.value?.invalidate()
    }

    fun toggleEditMode() {
        viewModel.editMode = !viewModel.editMode
        circleViewRef.value?.editMode = viewModel.editMode
        circleViewRef.value?.changeMiddleButtonState(MiddleButtonStates.MIDDLE_BUTTON_EDIT)
        circleViewRef.value?.invalidate()

        if (viewModel.editMode) {
            bottomMenuViewRef.value?.editMode = true
            bottomMenuViewRef.value?.postInvalidate()
        } else {
            deYellowAll()
            bottomMenuViewRef.value?.editMode = false
            bottomMenuViewRef.value?.postInvalidate()
        }
    }

    fun circleViewToggleAddAppMode(yesOrNo: Int = -1) {
        if (yesOrNo != -1) {
            viewModel.addNewAppMode = yesOrNo != 0
        } else {
            viewModel.addNewAppMode = !viewModel.addNewAppMode
        }
        circleViewRef.value?.addAppMode = viewModel.addNewAppMode
        if (viewModel.addNewAppMode) {
            circleViewRef.value?.changeMiddleButtonState(MiddleButtonStates.MIDDLE_BUTTON_CHECK)
            showAddMenu = true
        } else {
            circleViewRef.value?.amIHome(null)
            showAddMenu = false
        }
    }

    fun maxElementsPresent(): Boolean {
        val currentSizeWithoutPlusButton = viewModel.trenutnoPrikazanaPolja.size -
            if (viewModel.trenutnoPrikazanaPolja.find { it.nextIntent == MainActivity.ACTION_ADD_PRECAC } != null) 1 else 0
        val amIHomeVar = circleViewRef.value?.amIHomeVar ?: true
        return (amIHomeVar && currentSizeWithoutPlusButton >= 8) ||
               (!amIHomeVar && currentSizeWithoutPlusButton >= 7)
    }

    fun addNew() {
        Log.d("CircleScreen", "it's add")
        if (maxElementsPresent()) {
            Toast.makeText(context, "Max. elements present.", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("CircleScreen", "openaddmenu")
            mainActivity.openAddMenu()
        }
    }

    fun enterSelected() {
        if (viewModel.editSelected == -1) return
        prebaciMeni(viewModel.trenutnoPrikazanaPolja[viewModel.editSelected].id, viewModel.editSelected)
        deYellowAll()
    }

    fun launchLastEntered() {
        if (viewModel.lastEnteredIntent == null) return
        if (!viewModel.lastEnteredIntent?.shortcut!!) {
            Log.d("CircleScreen", "launchLastEntered app_or_folder ${viewModel.lastEnteredIntent}")
            val launchIntent: Intent? = viewModel.lastEnteredIntent?.let {
                context.packageManager.getLaunchIntentForPackage(it.nextIntent)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                if (viewModel.lastEnteredIntent!!.nextIntent == MainActivity.ACTION_APPINFO) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", viewModel.lastEnteredIntent!!.nextId, null)
                    intent.data = uri
                    context.startActivity(intent)
                } else if (viewModel.lastEnteredIntent!!.nextIntent == MainActivity.ACTION_ADD_PRECAC) {
                    addNew()
                    viewModel.lastEnteredIntent = null
                    return
                }
            }
        } else {
            Log.d("CircleScreen", "launchLastEntered shortcut ${viewModel.lastEnteredIntent}")
            viewModel.lastEnteredIntent?.let { mainActivity.startShortcut(it) }
        }
        viewModel.lastEnteredIntent = null
        goToHome()
    }

    fun touched(event: MotionEvent, redniBrojPolja: Int, no_draw_position: Int) {
        Log.d("CircleScreen", "touched ${event.action} $redniBrojPolja $no_draw_position")
        bottomMenuViewRef.value?.collapse()

        var sublist_counter = redniBrojPolja
        if (no_draw_position >= 0 && no_draw_position <= redniBrojPolja) {
            sublist_counter++
            Log.d("CircleScreen", "sublist_counter povecan")
        }

        when (redniBrojPolja) {
            MainActivity.ACTION_ADD_APP -> {
                mainActivity.addNewApp(mainActivity.addingNewAppEvent)
                mainActivity.addingNewAppEvent = null
                circleViewToggleAddAppMode(0)
                return
            }
            MainActivity.ACTION_ADD -> {
                addNew()
                return
            }
            MainActivity.ACTION_CANCEL, MainActivity.ACTION_HOME -> {
                goToHome()
                return
            }
        }

        if (event.action == MotionEvent.ACTION_UP && !viewModel.editMode && !viewModel.addNewAppMode) {
            Log.d("CircleScreen", "action up")
            if (redniBrojPolja == MainActivity.ACTION_LAUNCH) {
                Log.d("CircleScreen", "launch")
                launchLastEntered()
            }
            return
        }

        if (!viewModel.editMode) {
            if (viewModel.trenutnoPrikazanaPolja[redniBrojPolja].nextIntent != MainActivity.ACTION_ADD_PRECAC) {
                selectedText = viewModel.trenutnoPrikazanaPolja[redniBrojPolja].text
            }
            if (viewModel.trenutnoPrikazanaPolja[redniBrojPolja].nextIntent == "") { // folder
                Log.d("CircleScreen", "nextIntent je prazan")
                viewModel.lastEnteredIntent = null
                prebaciMeni(viewModel.trenutnoPrikazanaPolja[redniBrojPolja].id, sublist_counter)
            } else {
                viewModel.lastEnteredIntent = viewModel.trenutnoPrikazanaPolja[redniBrojPolja]
                if (!viewModel.trenutnoPrikazanaPolja[redniBrojPolja].shortcut) {
                    prebaciMeni(viewModel.trenutnoPrikazanaPolja[redniBrojPolja].id, sublist_counter)
                }
            }
        } else if (viewModel.editMode && event.action == MotionEvent.ACTION_DOWN) {
            Log.d("CircleScreen", "elseif viewModel.editMode down")
            if (redniBrojPolja >= 0) {
                Log.d("CircleScreen", "well its true")
                if (viewModel.editSelected == -1 || viewModel.editSelected != redniBrojPolja) {
                    viewModel.editSelected = redniBrojPolja
                    bottomMenuViewRef.value?.selectedId = viewModel.editSelected
                    bottomMenuViewRef.value?.invalidate()
                    circleViewRef.value?.selectPolje(redniBrojPolja)
                } else {
                    if (viewModel.editSelected == redniBrojPolja) {
                        deYellowAll()
                    }
                }
            }
        }
    }

    fun touched2(event: MotionEvent, counter: Int) {
        if (counter >= 0) {
            when (counter) {
                0 -> addNew()
                1 -> mainActivity.navigateToActivities()
                2 -> toggleEditMode()
                3 -> mainActivity.navigateToSearch()
                4 -> mainActivity.navigateToSettings()
                5 -> bottomMenuViewRef.value?.collapse()
            }
        } else {
            if (counter != -4 && viewModel.editSelected == -1) {
                Toast.makeText(context, "Nothing selected.", Toast.LENGTH_SHORT).show()
                return
            }
            when (counter) {
                -1 -> mainActivity.showMyDialog(viewModel.editSelected)
                -2 -> mainActivity.showDeleteConfirmDialog(viewModel.editSelected)
                -3 -> enterSelected()
                -4 -> toggleEditMode()
            }
        }
    }

    fun refreshCurrentMenu() {
        circleViewRef.value?.updateDesign()
        circleViewRef.value?.invalidate()
        prebaciMeni(viewModel.currentMenuId, viewModel.no_draw_position)
        Log.d("CircleScreen", "current menu refreshed")
    }

    // Main UI
    Box(modifier = modifier.fillMaxSize()) {
        // CircleView wrapper
        AndroidView(
            factory = { ctx ->
                CircleView(ctx).apply {
                    circleViewRef.value = this

                    // Attach event listener ONCE in factory
                    setEventListener(object : ICircleViewEventListener {
                        override fun onEventOccurred(
                            event: MotionEvent,
                            counter: Int,
                            current: Int
                        ) {
                            touched(event, counter, current)
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom menu (only for CIRCLE mode, not right/left hand variants)
        if (!rightHanded && !leftHanded) {
            AndroidView(
                factory = { ctx ->
                    BottomMenuView(ctx).apply {
                        bottomMenuViewRef.value = this

                        updateTexts(
                            listOf(
                                ctx.getString(R.string.rename),
                                ctx.getString(R.string.delete),
                                ctx.getString(R.string.enter),
                                ctx.getString(R.string.cancel)
                            )
                        )

                        setEventListener(object : BottomMenuView.IMyOtherEventListener {
                            override fun onEventOccurred(event: MotionEvent, counter: Int) {
                                touched2(event, counter)
                            }
                        })
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }

        // Selected text display
        Text(
            text = selectedText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            style = MaterialTheme.typography.titleLarge
        )

        // Add/Cancel buttons when in add mode
        if (showAddMenu) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = {
                    mainActivity.addingNewAppEvent = null
                    circleViewToggleAddAppMode(0)
                }) {
                    Text(context.getString(R.string.cancel))
                }
                Button(onClick = {
                    mainActivity.addNewApp(mainActivity.addingNewAppEvent)
                    mainActivity.addingNewAppEvent = null
                    circleViewToggleAddAppMode(0)
                }) {
                    Text(context.getString(R.string.add))
                }
            }
        }

        // Edit mode controls for right/left hand variants
        if (rightHanded || leftHanded) {
            Column(
                modifier = Modifier
                    .align(if (rightHanded) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel.editMode) {
                    IconButton(onClick = { mainActivity.showMyDialog(viewModel.editSelected) }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_edit_24),
                            contentDescription = "Rename"
                        )
                    }
                    IconButton(onClick = { mainActivity.showDeleteConfirmDialog(viewModel.editSelected) }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_delete_24),
                            contentDescription = "Delete"
                        )
                    }
                    IconButton(onClick = { enterSelected() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_arrow_forward_24),
                            contentDescription = "Enter"
                        )
                    }
                    IconButton(onClick = { toggleEditMode() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_close_50),
                            contentDescription = "Cancel"
                        )
                    }
                } else {
                    IconButton(onClick = { mainActivity.navigateToSearch() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_search_50),
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = { mainActivity.navigateToActivities() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_list_100),
                            contentDescription = "List"
                        )
                    }
                    IconButton(onClick = { toggleEditMode() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_edit_24),
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = { mainActivity.navigateToSettings() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_baseline_settings_100),
                            contentDescription = "Settings"
                        )
                    }
                }
            }
        }
    }

    // Lifecycle effects
    LaunchedEffect(icons) {
        if (icons.isNotEmpty()) {
            circleViewRef.value?.icons = icons.toMutableMap()
            Log.d("CircleScreen", "icons loaded: ${icons.size}")
        }
    }

    LaunchedEffect(viewModel.addNewAppMode) {
        circleViewRef.value?.addAppMode = viewModel.addNewAppMode
        if (viewModel.addNewAppMode) {
            circleViewRef.value?.changeMiddleButtonState(MiddleButtonStates.MIDDLE_BUTTON_CHECK)
            showAddMenu = true
        } else {
            circleViewRef.value?.amIHome(null)
            showAddMenu = false
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.currentMenuId == -1) {
            goToHome()
        } else {
            refreshCurrentMenu()
        }
    }

    // Handle back button
    androidx.activity.compose.BackHandler {
        if (viewModel.currentMenuId != viewModel.pocetnaId) {
            goToHome()
        } else if (viewModel.editMode) {
            toggleEditMode()
        } else {
            mainActivity.finish()
        }
    }

    // Expose interface methods to MainActivity
    DisposableEffect(Unit) {
        val refreshMenuFn = fun() {
            circleViewRef.value?.updateDesign()
            circleViewRef.value?.invalidate()
            prebaciMeni(viewModel.currentMenuId, viewModel.no_draw_position)
            Log.d("CircleScreen", "current menu refreshed")
        }

        val toggleAddModeFn = fun(yesOrNo: Int) {
            if (yesOrNo != -1) {
                viewModel.addNewAppMode = yesOrNo != 0
            } else {
                viewModel.addNewAppMode = !viewModel.addNewAppMode
            }
            circleViewRef.value?.addAppMode = viewModel.addNewAppMode
            if (viewModel.addNewAppMode) {
                circleViewRef.value?.changeMiddleButtonState(MiddleButtonStates.MIDDLE_BUTTON_CHECK)
                showAddMenu = true
            } else {
                circleViewRef.value?.amIHome(null)
                showAddMenu = false
            }
        }

        mainActivity.circleScreenCallbacks = object : CircleScreenCallbacks {
            override fun iconsUpdated() {
                circleViewRef.value?.icons = viewModel.icons.value!!
            }

            override fun selectedItemDeleted() {
                deYellowAll()
                refreshMenuFn()
            }

            override fun refreshCurrentMenu() {
                refreshMenuFn()
            }

            override fun circleViewToggleAddAppMode(yesOrNo: Int) {
                toggleAddModeFn(yesOrNo)
            }
        }

        onDispose {
            mainActivity.circleScreenCallbacks = null
        }
    }
}

/**
 * Interface for MainActivity to call CircleScreen methods
 */
interface CircleScreenCallbacks {
    fun iconsUpdated()
    fun selectedItemDeleted()
    fun refreshCurrentMenu()
    fun circleViewToggleAddAppMode(yesOrNo: Int = -1)
}
