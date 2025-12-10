package com.ingokodba.dragnav

import android.content.res.Resources

/**
 * Backward compatibility stub for old fragments that reference MainActivity constants.
 * This allows MainActivity.kt to be safely deleted while keeping old fragments compilable.
 */
class MainActivity {
    companion object {
        lateinit var resources2: Resources

        const val ACTION_CANCEL = -1
        const val ACTION_LAUNCH = -2
        const val ACTION_ADD = -3
        const val ACTION_HOME = -4
        const val ACTION_ADD_APP = -5
        const val ACTION_APPINFO = "appinfo"
        const val ACTION_ADD_PRECAC = "add_shortcut"
        const val MENU_UNDEFINED = -1
        const val MENU_APPLICATION_OR_FOLDER = 0
        const val MENU_SHORTCUT = 1
        const val MENU_ACTION = 2

        enum class Layouts {
            LAYOUT_MAIN, LAYOUT_SEARCH, LAYOUT_ACTIVITIES, LAYOUT_SETTINGS, LAYOUT_ACTIONS
        }

        enum class ActionTypes {
            ACTION_SEND_TO_APP
        }

        const val ACTION_IMPORT = 1
        const val ACTION_EXPORT = 2

        @Deprecated("Use MainActivityCompose.getInstance() instead", ReplaceWith("MainActivityCompose.getInstance()"))
        fun getInstance(): MainActivityCompose? {
            return MainActivityCompose.getInstance()
        }
    }

    // Stub methods for backward compatibility with old fragments
    // These are never called in MainActivityCompose but are needed for compilation
    var shortcutPopup: android.widget.PopupWindow? = null
    var addingNewAppEvent: com.ingokodba.dragnav.modeli.MessageEvent? = null
    lateinit var uiDesignMode: UiDesignEnum
    var gcolor: Int = android.graphics.Color.GRAY

    // Stub mainFragment with refreshCurrentMenu method for CustomDialogFragment compatibility
    interface MainFragmentStub {
        fun refreshCurrentMenu()
    }

    var mainFragment: MainFragmentStub? = object : MainFragmentStub {
        override fun refreshCurrentMenu() {
            // No-op stub
        }
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun showLayout(layout: Layouts) {
        // No-op: navigation is handled by Compose in MainActivityCompose
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun runAction(action: com.ingokodba.dragnav.modeli.Action) {
        // No-op: actions are handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun openShortcutsMenu(chipId: Int) {
        // No-op: shortcuts menu is handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun startColorpicker() {
        getInstance()?.startColorpicker()
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun databaseUpdateItem(item: com.ingokodba.dragnav.modeli.KrugSAplikacijama) {
        // No-op: database updates are handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun refreshCurrentMenu() {
        // No-op: menu refresh is handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun showMyDialog(selectedId: Int) {
        // No-op: dialogs are handled by Compose in MainActivityCompose
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun deleteSelectedItem(selectedId: Int) {
        // No-op: deletion is handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun openAddMenu() {
        // No-op: add menu is handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun getPreferences(mode: Int): android.content.SharedPreferences {
        // Return the default shared preferences for this activity
        val instance = getInstance()
        return instance!!.getPreferences(android.content.Context.MODE_PRIVATE)
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun addNewApp(event: com.ingokodba.dragnav.modeli.MessageEvent?) {
        // No-op: adding apps is handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun startShortcut(item: com.ingokodba.dragnav.modeli.KrugSAplikacijama) {
        // No-op: shortcuts are handled in Compose screens
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun onMessageEvent(event: com.ingokodba.dragnav.modeli.MessageEvent) {
        // No-op: Direct callbacks are used instead of event bus in MainActivityCompose
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun openFolderNameMenu(view: android.view.View, editing: Boolean, name: String, showColor: Boolean, callback: (String) -> Unit) {
        getInstance()?.openFolderNameMenu(view, editing, name, showColor, callback)
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun rainbowMapaInsertItem(mapa: com.ingokodba.dragnav.modeli.RainbowMapa) {
        getInstance()?.rainbowMapaInsertItem(mapa)
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun rainbowMapaUpdateItem(mapa: com.ingokodba.dragnav.modeli.RainbowMapa) {
        getInstance()?.rainbowMapaUpdateItem(mapa)
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun rainbowMapaDeleteItem(mapa: com.ingokodba.dragnav.modeli.RainbowMapa) {
        getInstance()?.rainbowMapaDeleteItem(mapa)
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun showDialogWithActions(actions: List<com.ingokodba.dragnav.ShortcutAction>, handler: OnShortcutClick, view: android.view.View) {
        getInstance()?.showDialogWithActions(actions, handler, view)
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun saveAppInfo(app: com.ingokodba.dragnav.modeli.AppInfo) {
        getInstance()?.saveAppInfo(app)
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun isAppAlreadyInMap(app: com.ingokodba.dragnav.modeli.AppInfo): Boolean {
        return getInstance()?.isAppAlreadyInMap(app) ?: false
    }

    @Deprecated("Legacy method - not used in MainActivityCompose")
    fun getShortcutFromPackage(packageName: String): List<android.content.pm.ShortcutInfo> {
        return getInstance()?.getShortcutFromPackage(packageName) ?: emptyList()
    }
}

enum class DialogStates {
    APP_SHORTCUTS, ADDING_TO_FOLDER, FOLDER_OPTIONS
}

interface OnShortcutClick {
    fun onShortcutClick(index: Int)
}
