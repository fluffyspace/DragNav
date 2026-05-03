package com.ingokodba.dragnav.navigation

/**
 * Navigation routes for the app using sealed classes for type-safe navigation
 */
sealed class NavRoute(val route: String) {
    object Main : NavRoute("main")
    object Search : NavRoute("search")
    object Activities : NavRoute("activities")
    object Actions : NavRoute("actions")
    object Settings : NavRoute("settings")
    object SettingsCircle : NavRoute("settings/circle")
    object SettingsRainbow : NavRoute("settings/rainbow")
    object SettingsGeneral : NavRoute("settings/general")
    object SettingsHelp : NavRoute("settings/help")
    object SettingsNotifications : NavRoute("settings/notifications")
    object SettingsExcludedApps : NavRoute("settings/notifications/excluded")
}

