package com.nervesparks.iris.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.SemanticColors
import com.nervesparks.iris.ui.theme.ModernIconButton
import com.nervesparks.iris.ui.theme.PrimaryButton
import com.nervesparks.iris.ui.theme.SecondaryButton
import com.nervesparks.iris.ui.theme.ThemedModalSurface
import com.nervesparks.iris.ui.theme.ThemedModalCard
import java.io.File

@Composable
fun ModelSelectionModal(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToModels: () -> Unit = {},
    isForBenchmark: Boolean = false
) {
    val context = LocalContext.current
    val extFilesDir = context.getExternalFilesDir(null)

    // Get available models and check which ones exist
    val availableModels = remember(extFilesDir) {
        extFilesDir?.let { viewModel.getAvailableModels(it) } ?: emptyList()
    }

    var showReasoningOnly by remember { mutableStateOf(false) }
    val filteredModels = remember(availableModels, showReasoningOnly) {
        if (showReasoningOnly) {
            availableModels.filter { it["supportsReasoning"] == "true" }
        } else {
            availableModels
        }
    }
    
    // Track selected model
    var selectedModel by remember { mutableStateOf("") }
    
    // Check if any models are downloaded
    val hasDownloadedModels = filteredModels.isNotEmpty()
    
    // File picker for local model import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Handle the selected file
            val fileName = context.contentResolver.getFileName(selectedUri)
            if (fileName?.endsWith(".gguf") == true) {
                // Copy file to app's external files directory
                extFilesDir?.let { dir ->
                    val destFile = File(dir, fileName)
                    try {
                        context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        // Refresh the model list
                        viewModel.refresh = true
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        ThemedModalSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.defaultPadding)
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ComponentStyles.largePadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Model Selection",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    ModernIconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))
                
                // Action buttons for model management
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
                ) {
                    // Download Models button
                    SecondaryButton(
                        onClick = onNavigateToModels,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(ComponentStyles.smallIconSize)
                        )
                        Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
                        Text("Download Models")
                    }
                    
                    // Import Local Model button
                    SecondaryButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(ComponentStyles.smallIconSize)
                        )
                        Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
                        Text("Import Model")
                    }
                }
                
                Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))
                
                // Status indicator
                if (!hasDownloadedModels) {
                    // No models downloaded
                    ThemedModalCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ComponentStyles.defaultPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(ComponentStyles.defaultIconSize)
                            )
                            Spacer(modifier = Modifier.width(ComponentStyles.defaultSpacing))
                            Text(
                                text = "No models downloaded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))
                    
                    Text(
                        text = "Download models from HuggingFace or import local GGUF files to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(ComponentStyles.largePadding))
                    
                    PrimaryButton(
                        onClick = onNavigateToModels,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go to Models")
                    }
                } else {
                    // Models available
                    Text(
                        text = "Select a model to load:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reasoning only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
                        Switch(checked = showReasoningOnly, onCheckedChange = { showReasoningOnly = it })
                    }

                    Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))

                    // Model list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
                    ) {
                        items(items = filteredModels, key = { it["name"] ?: it.hashCode().toString() }) { model ->
                            val modelName = model["name"] ?: ""
                            val isSelected = modelName == selectedModel
                            val isCurrentModel = modelName == viewModel.loadedModelName.value
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedModel = modelName
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isCurrentModel -> MaterialTheme.colorScheme.primaryContainer
                                        isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                shape = ComponentStyles.smallCardShape,
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isSelected) ComponentStyles.largeElevation else ComponentStyles.smallElevation
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(ComponentStyles.defaultPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = modelName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = when {
                                                    isCurrentModel -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (model["supportsReasoning"] == "true") {
                                                Spacer(modifier = Modifier.width(ComponentStyles.smallSpacing))
                                                Text(
                                                    text = "Reasoning",
                                                    color = Color.Black,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier
                                                        .background(SemanticColors.Warning, ComponentStyles.smallCardShape)
                                                        .padding(horizontal = ComponentStyles.smallPadding, vertical = ComponentStyles.smallPadding / 2)
                                                )
                                            }
                                        }

                                        if (isCurrentModel) {
                                            Text(
                                                text = "Currently loaded",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(ComponentStyles.defaultIconSize)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(ComponentStyles.defaultPadding))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ComponentStyles.defaultSpacing)
                    ) {
                        SecondaryButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        PrimaryButton(
                            onClick = {
                                if (selectedModel.isNotEmpty() && extFilesDir != null) {
                                    if (isForBenchmark) {
                                        viewModel.runBenchmarkWithModel(selectedModel, extFilesDir)
                                    } else {
                                        viewModel.loadModelByName(selectedModel, extFilesDir)
                                    }
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedModel.isNotEmpty()
                        ) {
                            Text(if (isForBenchmark) "Run Benchmark" else "Load Model")
                        }
                    }
                }
            }
        }
    }
}

// Extension function to get file name from URI
private fun android.content.ContentResolver.getFileName(uri: Uri): String? {
    return when (uri.scheme) {
        "content" -> {
            val cursor = query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        it.getString(displayNameIndex)
                    } else null
                } else null
            }
        }
        "file" -> uri.lastPathSegment
        else -> null
    }
} 