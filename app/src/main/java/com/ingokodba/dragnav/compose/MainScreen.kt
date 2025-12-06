package com.ingokodba.dragnav.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ingokodba.dragnav.navigation.NavRoute

/**
 * Main screen composable that hosts the navigation
 */
@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // This will be replaced with actual navigation content
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Main Screen - Navigation will be set up here")
    }
}

