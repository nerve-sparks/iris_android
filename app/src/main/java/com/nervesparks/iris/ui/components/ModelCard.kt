package com.nervesparks.iris.ui.components

import android.app.DownloadManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) }
    var showDeletedMessage by remember { mutableStateOf(false) }
    var isDefaultModel by remember { mutableStateOf(viewModel.defaultModelName.value == modelName) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            showDeletedMessage = true
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
            Row (horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                if (modelName == viewModel.loadedModelName.value) {
                    Text(color = Color.Green, text = "Active Model", fontSize = 12.sp)
                }
                if(modelName == viewModel.defaultModelName.value){
                    Text(color = Color.LightGray, text = "Default", fontSize = 12.sp)
                }
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

                            if (showDeleteConfirmation) {
                                AlertDialog(
                                    textContentColor = Color.LightGray,
                                    containerColor =  Color(0xFF233340),
                                    modifier = Modifier.background(shape = RoundedCornerShape(8.dp), color = Color(0xFF233340)),
                                    onDismissRequest = { showDeleteConfirmation = false },
                                    title = { Text("Confirm Deletion", color = Color.White) },
                                    text = { Text("Are you sure you want to delete this model? The app will restart after deletion.") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (modelName == viewModel.loadedModelName.value) {
                                                    viewModel.setDefaultModelName("")
                                                }
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
                                                isDeleted = true
                                                viewModel.refresh = true
                                            },
                                            colors = ButtonDefaults.buttonColors(Color(0xFFb91c1c))
                                        ) {
                                            Text("Delete")
                                        }
                                    },
                                    dismissButton = {
                                        Button(
                                            colors = ButtonDefaults.buttonColors(Color.Black),
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

            if (showDeletedMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Model Deleted",
                    color = Color.Red,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (modelName == viewModel.loadedModelName.value){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val context = LocalContext.current
                    RadioButton(
                        selected = (modelName==viewModel.defaultModelName.value),
                        onClick = {
                            viewModel.setDefaultModelName(modelName)
                            Toast.makeText(
                                context,
                                "$modelName set as default model",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.Green,
                            unselectedColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set as Default Model",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

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
