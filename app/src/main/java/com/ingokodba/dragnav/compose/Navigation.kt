package com.ingokodba.dragnav.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ingokodba.dragnav.MainActivity
import com.ingokodba.dragnav.navigation.NavRoute

/**
 * Navigation graph for the app
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    mainActivity: MainActivity,
    currentLayout: MainActivity.Companion.Layouts,
    onLayoutChange: (MainActivity.Companion.Layouts) -> Unit,
    startDestination: String = NavRoute.Main.route
) {
    // Store nav controller reference in MainActivity for programmatic navigation
    LaunchedEffect(navController) {
        mainActivity.navController = navController
    }
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoute.Main.route) {
            MainScreenContent(
                mainActivity = mainActivity,
                onNavigateToActivities = {
                    navController.navigate(NavRoute.Activities.route)
                    onLayoutChange(MainActivity.Companion.Layouts.LAYOUT_ACTIVITIES)
                },
                onNavigateToSearch = {
                    navController.navigate(NavRoute.Search.route)
                    onLayoutChange(MainActivity.Companion.Layouts.LAYOUT_SEARCH)
                },
                onNavigateToActions = {
                    navController.navigate(NavRoute.Actions.route)
                    onLayoutChange(MainActivity.Companion.Layouts.LAYOUT_ACTIONS)
                }
            )
        }
        
        composable(NavRoute.Search.route) {
            SearchScreenContent(
                mainActivity = mainActivity,
                onNavigateBack = {
                    navController.popBackStack()
                    onLayoutChange(MainActivity.Companion.Layouts.LAYOUT_MAIN)
                }
            )
        }
        
        composable(NavRoute.Activities.route) {
            ActivitiesScreenContent(
                mainActivity = mainActivity,
                onNavigateBack = {
                    navController.popBackStack()
                    onLayoutChange(MainActivity.Companion.Layouts.LAYOUT_MAIN)
                }
            )
        }
        
        composable(NavRoute.Actions.route) {
            ActionsScreenContent(
                mainActivity = mainActivity,
                onNavigateBack = {
                    navController.popBackStack()
                    onLayoutChange(MainActivity.Companion.Layouts.LAYOUT_MAIN)
                }
            )
        }
    }
}

@Composable
fun MainScreenContent(
    mainActivity: MainActivity,
    onNavigateToActivities: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Will be replaced with actual main fragment composable
    // For now, use AndroidView to wrap existing fragments
    MainFragmentComposable(
        mainActivity = mainActivity,
        modifier = modifier
    )
}

@Composable
fun SearchScreenContent(
    mainActivity: MainActivity,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Will be replaced with actual search fragment composable
    SearchFragmentComposable(
        mainActivity = mainActivity,
        modifier = modifier
    )
}

@Composable
fun ActivitiesScreenContent(
    mainActivity: MainActivity,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Will be replaced with actual activities fragment composable
    ActivitiesFragmentComposable(
        mainActivity = mainActivity,
        modifier = modifier
    )
}

@Composable
fun ActionsScreenContent(
    mainActivity: MainActivity,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Will be replaced with actual actions fragment composable
    ActionsFragmentComposable(
        mainActivity = mainActivity,
        modifier = modifier
    )
}

