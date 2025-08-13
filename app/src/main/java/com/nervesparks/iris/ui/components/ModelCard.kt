package com.nervesparks.iris.ui.components

import android.app.DownloadManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.ThemedWarningButton
import com.nervesparks.iris.ui.theme.SecondaryButton
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModelCard(
    modelName: String,
    supportsReasoning: Boolean = false,
    supportsVision: Boolean = false,
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
    var showMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

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
            .padding(vertical = ComponentStyles.smallPadding)
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            )
            .shadow(
                elevation = ComponentStyles.defaultElevation,
                shape = ComponentStyles.smallCardShape
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.defaultElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.smallPadding)
        ) {
            Row (horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()){
                if (modelName == viewModel.loadedModelName.value) {
                    Text(color = MaterialTheme.colorScheme.primary, text = "Active Model", fontSize = 12.sp)
                }
                if(modelName == viewModel.defaultModelName.value){
                    Text(color = MaterialTheme.colorScheme.onSurfaceVariant, text = "Default", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = modelName, style = MaterialTheme.typography.titleSmall)
                if (supportsReasoning) {
                    Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
                    Text(
                        text = "Reasoning",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, ComponentStyles.smallCardShape)
                            .padding(horizontal = ComponentStyles.smallPadding, vertical = ComponentStyles.smallPadding)
                    )
                }
                if (supportsVision) {
                    Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
                    Text(
                        text = "Vision",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary, ComponentStyles.smallCardShape)
                            .padding(horizontal = ComponentStyles.smallPadding, vertical = ComponentStyles.smallPadding)
                    )
                }
                Spacer(Modifier.weight(1f))
                // Backend badge
                Text(
                    text = viewModel.currentBackend,
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.tertiary, ComponentStyles.smallCardShape)
                        .padding(horizontal = ComponentStyles.smallPadding, vertical = ComponentStyles.smallPadding)
                )
            }
            Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val fullUrl = if (downloadLink.isNotEmpty()) {
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

                Spacer(modifier = Modifier.padding(ComponentStyles.smallPadding))

                if (showDeleteButton) {
                    File(extFilesDir, modelName).let { downloadable ->
                        if (downloadable.exists()) {
                            ThemedWarningButton(
                                onClick = { showDeleteConfirmation = true }
                            ) {
                                Text(text = "Delete", color = MaterialTheme.colorScheme.onError)
                            }

                            if (showDeleteConfirmation) {
                                androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteConfirmation = false }) {
                                    com.nervesparks.iris.ui.theme.ThemedModalCard {
                                        Column(Modifier.padding(ComponentStyles.defaultPadding)) {
                                            Text("Confirm Deletion", style = MaterialTheme.typography.titleMedium)
                                            Spacer(Modifier.height(ComponentStyles.smallPadding))
                                            Text("Are you sure you want to delete this model? The app will restart after deletion.")
                                            Spacer(Modifier.height(ComponentStyles.defaultPadding))
                                            Row(horizontalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)) {
                                                SecondaryButton(onClick = { showDeleteConfirmation = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                                                ThemedWarningButton(onClick = {
                                                    if (modelName == viewModel.loadedModelName.value) { viewModel.setDefaultModelName("") }
                                                    coroutineScope.launch { viewModel.unload() }
                                                    File(extFilesDir, modelName).delete()
                                                    viewModel.showModal = true
                                                    if (modelName == viewModel.loadedModelName.value) { viewModel.loadedModelName.value = "" }
                                                    isDeleted = true
                                                    viewModel.refresh = true
                                                }, modifier = Modifier.weight(1f)) { Text("Delete") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Long-press menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy name") },
                    onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(modelName))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (modelName == viewModel.defaultModelName.value) "Unset default" else "Set default") },
                    onClick = {
                        if (modelName == viewModel.defaultModelName.value) {
                            viewModel.setDefaultModelName("")
                        } else {
                            viewModel.setDefaultModelName(modelName)
                        }
                        showMenu = false
                    }
                )
            }

            if (showDeletedMessage) {
                Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                Text(
                    text = "Model Deleted",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
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
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
                    Text(
                        text = "Set as Default Model",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }

            File(extFilesDir, modelName).let {
                Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
                Text(
                    text = if (formatFileSize(File(extFilesDir, modelName).length()) != "0 Bytes") {
                        "Size: ${formatFileSize(File(extFilesDir, modelName).length())}"
                    } else {
                        "Not Downloaded"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
