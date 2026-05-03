package com.ingokodba.dragnav.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.UiDesignEnum
import com.ingokodba.dragnav.ViewModel
import com.ingokodba.dragnav.navigation.NavRoute

/**
 * Main navigation component for the app
 * Routes to different screens based on UI design mode and current navigation state
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    mainActivity: MainActivityCompose,
    viewModel: ViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Main.route,
        modifier = modifier
    ) {
        // Main screen - switches between different UI modes
        composable(NavRoute.Main.route) {
            when (mainActivity.uiDesignMode) {
                UiDesignEnum.CIRCLE -> {
                    CircleScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.CIRCLE_RIGHT_HAND -> {
                    CircleScreen(mainActivity, viewModel = viewModel, rightHanded = true)
                }
                UiDesignEnum.CIRCLE_LEFT_HAND -> {
                    CircleScreen(mainActivity, viewModel = viewModel, leftHanded = true)
                }
                UiDesignEnum.KEYPAD -> {
                    KeypadScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.RAINBOW_PATH -> {
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
            }
        }

        // Search screen
        composable(NavRoute.Search.route) {
            SearchScreen(mainActivity, viewModel)
        }

        // Activities (app list) screen
        composable(NavRoute.Activities.route) {
            ActivitiesScreen(mainActivity, viewModel)
        }

        // Actions screen
        composable(NavRoute.Actions.route) {
            ActionsScreen(mainActivity, viewModel)
        }

        // Settings screens — no transition animations
        composable(
            NavRoute.Settings.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SettingsScreen(mainActivity, navController)
        }

        composable(
            NavRoute.SettingsCircle.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            CircleSettingsScreen(mainActivity, navController)
        }

        composable(
            NavRoute.SettingsRainbow.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            RainbowSettingsScreen(mainActivity, navController)
        }

        composable(
            NavRoute.SettingsGeneral.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            GeneralSettingsScreen(mainActivity, navController)
        }

        composable(
            NavRoute.SettingsHelp.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            HelpSettingsScreen(mainActivity, navController)
        }

        composable(
            NavRoute.SettingsNotifications.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            NotificationSettingsScreen(mainActivity, navController)
        }

        composable(
            NavRoute.SettingsExcludedApps.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            ExcludedAppsScreen(mainActivity, navController)
        }

        composable(
            NavRoute.SettingsHiddenApps.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            HiddenAppsScreen(mainActivity, navController)
        }
    }
}

/**
 * Helper function to get the start destination based on preferences
 * Currently always returns Main, but could be extended to remember last screen
 */
fun getStartDestination(activity: MainActivityCompose): String {
    return NavRoute.Main.route
}
