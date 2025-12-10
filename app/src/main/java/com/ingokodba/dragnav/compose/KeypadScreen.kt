package com.ingokodba.dragnav.compose

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ingokodba.dragnav.*

/**
 * Compose screen for Keypad UI mode (KEYPAD).
 * Wraps the custom PhoneKeypadView and provides 3-digit code input interface.
 *
 * @param mainActivity Reference to MainActivityCompose
 * @param modifier Optional Modifier for the root composable
 * @param viewModel Shared ViewModel containing app data
 */
@Composable
fun KeypadScreen(
    mainActivity: MainActivityCompose,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = viewModel()
) {
    val context = LocalContext.current

    // Collect app data from ViewModel
    val appsList by viewModel.appsListFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // UI state
    var currentInput by remember { mutableStateOf("") }
    var displayText by remember { mutableStateOf("---") }

    // View reference
    val keypadViewRef = remember { mutableStateOf<PhoneKeypadView?>(null) }

    // Function to handle number input
    fun handleNumberInput(number: Int) {
        Log.d("KeypadScreen", "Number pressed: $number")

        if (number == 10) {
            // Clear button (number 10 represents clear)
            currentInput = ""
            displayText = "---"
        } else {
            // Add digit
            currentInput += number

            // Update display based on input length
            when (currentInput.length) {
                1 -> displayText = "$number"
                2 -> displayText = currentInput
                3 -> {
                    // Launch app
                    val id = currentInput.toIntOrNull()
                    if (id != null && id < appsList.size) {
                        val app = appsList[id]
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    }
                    // Reset
                    currentInput = ""
                    displayText = "---"
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Display text at top
        Text(
            text = displayText,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )

        // Phone keypad view
        AndroidView(
            factory = { ctx ->
                PhoneKeypadView(ctx).apply {
                    setEventListener(object : MainFragmentTipke.IMyEventListener {
                        override fun onEventOccurred(number: Int) {
                            handleNumberInput(number)
                        }
                    })
                    keypadViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
