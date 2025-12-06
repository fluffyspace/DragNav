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
}

