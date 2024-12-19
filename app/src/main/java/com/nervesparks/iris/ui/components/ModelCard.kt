package com.nervesparks.iris.ui.components

import android.app.DownloadManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ModelCard(
    modelName: String,
    viewModel: MainViewModel,
    dm: DownloadManager,
    extFilesDir: File,
    downloadLink: String,
    showDeleteButton: Boolean
) {
    // State for showing the confirmation dialog and whether the model is deleted
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) } // Track deletion status
    var showDeletedMessage by remember { mutableStateOf(false) } // Track showing deleted message

    // Recompose Downloadable.Button after 1 second delay
    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            showDeletedMessage = true
            // Wait for 1 second before showing the button again
            kotlinx.coroutines.delay(1000)
            showDeletedMessage = false
            isDeleted = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xff0f172a),
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (modelName == viewModel.loadedModelName.value) {
                Text(color = Color.Green, text = "Currently Active", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = modelName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val fullUrl = if (downloadLink != "") {
                    downloadLink
                } else {
                    "https://huggingface.co/${viewModel.userGivenModel}/resolve/main/${modelName}?download=true"
                }

                // If model is not deleted, show Downloadable.Button
                if (!showDeletedMessage) {
                    Downloadable.Button(
                        viewModel,
                        dm,
                        Downloadable(
                            modelName,
                            source = Uri.parse(fullUrl),
                            destination = File(extFilesDir, modelName)
                        )
                    )
                }

                Spacer(modifier = Modifier.padding(5.dp))

                if (showDeleteButton) {
                    File(extFilesDir, modelName).let { downloadable ->
                        if (downloadable.exists()) {
                            Button(
                                onClick = { showDeleteConfirmation = true },
                                colors = ButtonDefaults.buttonColors(Color(0xFFb91c1c)),
                            ) {
                                Text(text = "Delete", color = Color.White)
                            }

                            // Confirmation Dialog
                            if (showDeleteConfirmation) {
                                AlertDialog(
                                    textContentColor = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    titleContentColor = Color.White,
                                    containerColor = Color(0xFF233340),
                                    onDismissRequest = { showDeleteConfirmation = false },
                                    title = { Text("Confirm Deletion") },
                                    text = { Text("Are you sure you want to delete this model? The app will restart after deletion.") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch { viewModel.unload() }
                                                File(extFilesDir, modelName).delete()
                                                viewModel.showModal = false
                                                if (modelName == viewModel.loadedModelName.value) {
                                                    viewModel.newShowModal = true
                                                    showDeleteConfirmation = false
                                                }
                                                if (modelName == viewModel.loadedModelName.value) {
                                                    viewModel.loadedModelName.value = ""
                                                }
                                                // Mark model as deleted to show the updated state
                                                isDeleted = true // Update deletion state
                                                viewModel.refresh = true
                                            },
                                            colors = ButtonDefaults.buttonColors(Color(0xFFb91c1c))
                                        ) {
                                            Text("Delete")
                                        }
                                    },
                                    dismissButton = {
                                        Button(
                                            colors = ButtonDefaults.buttonColors(Color.DarkGray),
                                            onClick = { showDeleteConfirmation = false }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Show "Model Deleted" message for 1 second after deletion
            if (showDeletedMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Model Deleted",
                    color = Color.Red,
                    fontSize = 15.sp
                )
            }

            // Add file size information if model exists
            File(extFilesDir, modelName).let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (formatFileSize(File(extFilesDir, modelName).length()) != "0 Bytes") {
                        "Size: ${formatFileSize(File(extFilesDir, modelName).length())}"
                    } else {
                        "Not Downloaded"
                    },
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> String.format("%d Bytes", size)
    }
}


