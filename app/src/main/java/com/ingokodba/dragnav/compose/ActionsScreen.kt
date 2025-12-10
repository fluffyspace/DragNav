package com.ingokodba.dragnav.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ingokodba.dragnav.MainActivity
import com.ingokodba.dragnav.MainActivityCompose
import com.ingokodba.dragnav.ViewModel
import com.ingokodba.dragnav.modeli.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen(
    mainActivity: MainActivityCompose,
    viewModel: ViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Define available actions
    val actions by remember {
        mutableStateOf(
            listOf(
                Action(
                    title = "Send Last Image",
                    description = "Send the last captured image",
                    type = MainActivity.Companion.ActionTypes.ACTION_SEND_TO_APP
                ),
                Action(
                    title = "Timer",
                    description = "Set a timer",
                    type = MainActivity.Companion.ActionTypes.ACTION_SEND_TO_APP
                )
            )
        )
    }

    // Handle back button
    BackHandler {
        mainActivity.navController?.popBackStack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Actions",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Actions list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(actions) { action ->
                ActionItem(
                    action = action,
                    onClick = {
                        // TODO: Implement action execution
                        // For now, just show a toast/snackbar
                        mainActivity.navController?.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun ActionItem(
    action: Action,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.DarkGray,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
