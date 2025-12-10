package com.ingokodba.dragnav.compose

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
                    // TODO: CircleScreen(mainActivity, viewModel)
                    // Placeholder until CircleScreen is implemented
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.CIRCLE_RIGHT_HAND -> {
                    // TODO: CircleScreen(mainActivity, viewModel, rightHanded = true)
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.CIRCLE_LEFT_HAND -> {
                    // TODO: CircleScreen(mainActivity, viewModel, leftHanded = true)
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.RAINBOW_RIGHT -> {
                    // TODO: RainbowScreen(mainActivity, viewModel, rightSide = true)
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.RAINBOW_LEFT -> {
                    // TODO: RainbowScreen(mainActivity, viewModel, rightSide = false)
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.KEYPAD -> {
                    // TODO: KeypadScreen(mainActivity, viewModel)
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
                UiDesignEnum.RAINBOW_PATH -> {
                    RainbowPathScreen(mainActivity, viewModel = viewModel)
                }
            }
        }

        // Search screen
        composable(NavRoute.Search.route) {
            // TODO: SearchScreen(mainActivity, viewModel, navController)
            // Placeholder until SearchScreen is implemented
            RainbowPathScreen(mainActivity, viewModel = viewModel)
        }

        // Activities (app list) screen
        composable(NavRoute.Activities.route) {
            // TODO: ActivitiesScreen(mainActivity, viewModel, navController)
            RainbowPathScreen(mainActivity, viewModel = viewModel)
        }

        // Actions screen
        composable(NavRoute.Actions.route) {
            // TODO: ActionsScreen(mainActivity, viewModel, navController)
            RainbowPathScreen(mainActivity, viewModel = viewModel)
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
