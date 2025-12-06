package com.ingokodba.dragnav.compose

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.ingokodba.dragnav.MainActivity
import com.ingokodba.dragnav.MainFragment
import com.ingokodba.dragnav.MainFragmentRainbow
import com.ingokodba.dragnav.MainFragmentTipke
import com.ingokodba.dragnav.rainbow.MainFragmentRainbowPath
import com.ingokodba.dragnav.SearchFragment
import com.ingokodba.dragnav.ActivitiesFragment
import com.ingokodba.dragnav.ActionsFragment
import com.ingokodba.dragnav.UiDesignEnum

/**
 * Composable wrapper for MainFragment based on UI design mode
 */
@Composable
fun MainFragmentComposable(
    mainActivity: MainActivity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fragmentContainerView by remember { mutableStateOf<FragmentContainerView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = android.view.View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                fragmentContainerView = this
            }
        },
        modifier = modifier,
        update = { view ->
            if (fragmentContainerView != null && mainActivity.supportFragmentManager.findFragmentById(view.id) == null) {
                // Create appropriate fragment based on UI design mode
                val fragment = when (mainActivity.uiDesignMode) {
                    UiDesignEnum.CIRCLE, 
                    UiDesignEnum.CIRCLE_RIGHT_HAND, 
                    UiDesignEnum.CIRCLE_LEFT_HAND -> MainFragment()
                    UiDesignEnum.RAINBOW_RIGHT -> MainFragmentRainbow(true)
                    UiDesignEnum.RAINBOW_LEFT -> MainFragmentRainbow(false)
                    UiDesignEnum.KEYPAD -> MainFragmentTipke()
                    UiDesignEnum.RAINBOW_PATH -> MainFragmentRainbowPath()
                }
                
                // Add fragment to container
                mainActivity.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitNow()
                
                // Update mainActivity reference
                mainActivity.mainFragment = fragment as com.ingokodba.dragnav.MainFragmentInterface
                
                // Initialize fragment after creation (defer to ensure view is ready)
                view.post {
                    fragment.iconsUpdated()
                    fragment.goToHome()
                }
            }
        }
    )
}

/**
 * Composable wrapper for SearchFragment
 */
@Composable
fun SearchFragmentComposable(
    mainActivity: MainActivity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fragmentContainerView by remember { mutableStateOf<FragmentContainerView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = android.view.View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                fragmentContainerView = this
            }
        },
        modifier = modifier,
        update = { view ->
            if (fragmentContainerView != null && mainActivity.supportFragmentManager.findFragmentById(view.id) == null) {
                val fragment = SearchFragment()
                mainActivity.searchFragment = fragment
                
                mainActivity.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitNow()
            }
        }
    )
}

/**
 * Composable wrapper for ActivitiesFragment
 */
@Composable
fun ActivitiesFragmentComposable(
    mainActivity: MainActivity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fragmentContainerView by remember { mutableStateOf<FragmentContainerView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = android.view.View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                fragmentContainerView = this
            }
        },
        modifier = modifier,
        update = { view ->
            if (fragmentContainerView != null && mainActivity.supportFragmentManager.findFragmentById(view.id) == null) {
                val fragment = ActivitiesFragment(mainActivity.uiDesignMode)
                mainActivity.activitiesFragment = fragment
                
                mainActivity.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitNow()
            }
        }
    )
}

/**
 * Composable wrapper for ActionsFragment
 */
@Composable
fun ActionsFragmentComposable(
    mainActivity: MainActivity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fragmentContainerView by remember { mutableStateOf<FragmentContainerView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = android.view.View.generateViewId()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                fragmentContainerView = this
            }
        },
        modifier = modifier,
        update = { view ->
            if (fragmentContainerView != null && mainActivity.supportFragmentManager.findFragmentById(view.id) == null) {
                val fragment = ActionsFragment()
                mainActivity.actionsFragment = fragment
                
                mainActivity.supportFragmentManager.beginTransaction()
                    .replace(view.id, fragment)
                    .commitNow()
            }
        }
    )
}

