package com.ingokodba.dragnav.navigation

/**
 * Navigation routes for the app using sealed classes for type-safe navigation
 */
sealed class NavRoute(val route: String) {
    object Main : NavRoute("main")
    object Settings : NavRoute("settings")
    object SettingsRainbow : NavRoute("settings/rainbow")
    object SettingsGeneral : NavRoute("settings/general")
    object SettingsHelp : NavRoute("settings/help")
    object SettingsNotifications : NavRoute("settings/notifications")
    object SettingsExcludedApps : NavRoute("settings/notifications/excluded")
    object SettingsHiddenApps : NavRoute("settings/hidden_apps")
}

