package com.nervesparks.iris.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import kotlinx.coroutines.launch

data class BenchmarkState(
    val isRunning: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val results: List<String> = emptyList(),
    val error: String? = null
)

@Composable
fun BenchMarkScreen(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var state by remember { mutableStateOf(BenchmarkState()) }
    var tokensPerSecond by remember { mutableStateOf(0.0) }

    val deviceInfo = buildDeviceInfo(viewModel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "Benchmark Information",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Device Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                deviceInfo.lines().forEach { line ->
                    Text(line, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
        val context = LocalContext.current
        // Benchmark Button

        androidx.compose.material3.Button(
            modifier =Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB).copy(alpha = 1.0f),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 3.dp
            ),
            onClick = {
                if(viewModel.loadedModelName.value == ""){

                    Toast.makeText(context, "Load A Model First", Toast.LENGTH_SHORT).show()
                }
                else{
                state = state.copy(showConfirmDialog = true) }},
            enabled = !state.isRunning,
        )
        {
            Text(if (state.isRunning) "Benchmarking..." else "Start Benchmark", color = Color.White)
        }

        // Progress Indicator
        if (state.isRunning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    "Benchmarking in progress...",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Results Section
        if (state.results.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Benchmark Results",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    state.results.forEach { result ->
                        Text(
                            result,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Token Per Second Speed Display
        Text(
            text = if (viewModel.tokensPerSecondsFinal > 0) {
                "Tokens per second: %.2f".format(viewModel.tokensPerSecondsFinal)
            } else {
                "Calculating tokens per second..."
            },
            style = MaterialTheme.typography.body1,
            color = Color.Green,
            modifier = Modifier.padding(16.dp)
        )

        // Error Display
        state.error?.let { error ->
            Text(
                error,
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Confirmation Dialog
    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                state = state.copy(showConfirmDialog = false)
            },
            title = { Text("Benchmarking Notice") },
            text = { Text("This process will 30 seconds to 1 minute. Do you want to continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        state = state.copy(
                            showConfirmDialog = false,
                            isRunning = true,
                            results = emptyList(),
                            error = null
                        )
                        scope.launch {
                            try {
                                viewModel.myCustomBenchmark()

                                // Update tokens per second after benchmarking
                                state = state.copy(
                                    results = viewModel.tokensList.toList() // Fetch tokens collected
                                )
                            } catch (e: Exception) {
                                state = state.copy(
                                    error = "Error: ${e.message}"
                                )
                            } finally {
                                state = state.copy(isRunning = false)
                            }
                        }
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state = state.copy(showConfirmDialog = false)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}



private fun buildDeviceInfo(viewModel: MainViewModel): String {
    return buildString {
        append("Device: ${Build.MODEL}\n")
        append("Android: ${Build.VERSION.RELEASE}\n")
        append("Processor: ${Build.HARDWARE}\n")
        append("Available Threads: ${Runtime.getRuntime().availableProcessors()}\n")
        append("Current Model: ${viewModel.loadedModelName.value ?: "N/A"}\n")
        append("User Threads: ${viewModel.user_thread}")
    }
}
