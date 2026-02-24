package com.nervesparks.iris.ui.components

import android.app.DownloadManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.MatrixBg
import com.nervesparks.iris.ui.MatrixGreen
import com.nervesparks.iris.ui.MatrixGreenBright
import com.nervesparks.iris.ui.MatrixGreenDark
import com.nervesparks.iris.ui.MatrixGreenMid
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
            .border(1.dp, MatrixGreenDark, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MatrixGreenDark.copy(alpha = 0.15f),
            contentColor = MatrixGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                if (modelName == viewModel.loadedModelName.value) {
                    Text(
                        text = "● ACTIVE",
                        color = MatrixGreenBright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (modelName == viewModel.defaultModelName.value) {
                    Text(
                        text = "[DEFAULT]",
                        color = MatrixGreenMid,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = modelName,
                color = MatrixGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val fullUrl = if (downloadLink != "") downloadLink
                    else "https://huggingface.co/${viewModel.userGivenModel}/resolve/main/${modelName}?download=true"

                if (!showDeletedMessage) {
                    Downloadable.Button(
                        viewModel, dm,
                        Downloadable(modelName, source = Uri.parse(fullUrl), destination = File(extFilesDir, modelName))
                    )
                }

                Spacer(modifier = Modifier.padding(5.dp))

                if (showDeleteButton) {
                    File(extFilesDir, modelName).let { downloadable ->
                        if (downloadable.exists()) {
                            Button(
                                onClick = { showDeleteConfirmation = true },
                                colors = ButtonDefaults.buttonColors(Color(0xFF1a0000)),
                            ) {
                                Text(text = "✕ DELETE", color = Color(0xFFFF4444), fontFamily = FontFamily.Monospace)
                            }

                            if (showDeleteConfirmation) {
                                AlertDialog(
                                    textContentColor = MatrixGreen,
                                    containerColor = MatrixBg,
                                    modifier = Modifier
                                        .background(shape = RoundedCornerShape(8.dp), color = MatrixBg)
                                        .border(1.dp, MatrixGreenDark, RoundedCornerShape(8.dp)),
                                    onDismissRequest = { showDeleteConfirmation = false },
                                    title = { Text("! CONFIRM_DELETE", color = MatrixGreenBright, fontFamily = FontFamily.Monospace) },
                                    text = { Text("Delete this model? App will restart after deletion.", color = MatrixGreenMid, fontFamily = FontFamily.Monospace) },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (modelName == viewModel.loadedModelName.value) viewModel.setDefaultModelName("")
                                                coroutineScope.launch { viewModel.unload() }
                                                File(extFilesDir, modelName).delete()
                                                viewModel.showModal = false
                                                if (modelName == viewModel.loadedModelName.value) {
                                                    viewModel.newShowModal = true
                                                    showDeleteConfirmation = false
                                                    viewModel.loadedModelName.value = ""
                                                }
                                                isDeleted = true
                                                viewModel.refresh = true
                                            },
                                            colors = ButtonDefaults.buttonColors(Color(0xFF1a0000))
                                        ) { Text("DELETE", color = Color(0xFFFF4444), fontFamily = FontFamily.Monospace) }
                                    },
                                    dismissButton = {
                                        Button(
                                            colors = ButtonDefaults.buttonColors(MatrixGreenDark),
                                            onClick = { showDeleteConfirmation = false }
                                        ) { Text("CANCEL", color = MatrixGreen, fontFamily = FontFamily.Monospace) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showDeletedMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "✕ MODEL DELETED", color = Color(0xFFFF4444), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (modelName == viewModel.loadedModelName.value) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val context = LocalContext.current
                    RadioButton(
                        selected = (modelName == viewModel.defaultModelName.value),
                        onClick = {
                            viewModel.setDefaultModelName(modelName)
                            Toast.makeText(context, "$modelName set as default", Toast.LENGTH_SHORT).show()
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MatrixGreenBright,
                            unselectedColor = MatrixGreenDark
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Set as Default", color = MatrixGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            File(extFilesDir, modelName).let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (formatFileSize(File(extFilesDir, modelName).length()) != "0 Bytes")
                        "SIZE: ${formatFileSize(File(extFilesDir, modelName).length())}"
                    else "STATUS: NOT_DOWNLOADED",
                    color = MatrixGreenMid,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
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
        else    -> String.format("%d Bytes", size)
    }
}
