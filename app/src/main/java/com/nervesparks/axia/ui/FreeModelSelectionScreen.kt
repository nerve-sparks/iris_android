package com.nervesparks.axia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.nervesparks.axia.models.FreeLocalModelManager
import com.nervesparks.axia.models.LocalModel
import com.nervesparks.axia.models.ModelStatus
import com.nervesparks.axia.models.UseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeModelSelectionScreen(
    freeModelManager: FreeLocalModelManager,
    onModelSelected: (LocalModel) -> Unit,
    onBack: () -> Unit = {}
) {
    val availableModels by freeModelManager.availableModels.collectAsState()
    val currentModel by freeModelManager.currentModel.collectAsState() 
    val modelStatus by freeModelManager.modelStatus.collectAsState()
    val downloadProgress by freeModelManager.downloadProgress.collectAsState()
    
    val scope = rememberCoroutineScope()
    var selectedUseCase by remember { mutableStateOf(UseCase.GENERAL_CHAT) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🆓 100% FREE AI Models",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "No subscriptions, no API keys, completely free!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Use Case Selector
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "What will you use Axia for?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedUseCase = UseCase.GENERAL_CHAT },
                        label = { Text("💬 Chat") },
                        selected = selectedUseCase == UseCase.GENERAL_CHAT
                    )
                    FilterChip(
                        onClick = { selectedUseCase = UseCase.CODING },
                        label = { Text("💻 Coding") },
                        selected = selectedUseCase == UseCase.CODING
                    )
                    FilterChip(
                        onClick = { selectedUseCase = UseCase.LIGHTWEIGHT },
                        label = { Text("🚀 Fast") },
                        selected = selectedUseCase == UseCase.LIGHTWEIGHT
                    )
                    FilterChip(
                        onClick = { selectedUseCase = UseCase.HIGH_QUALITY },
                        label = { Text("🏆 Quality") },
                        selected = selectedUseCase == UseCase.HIGH_QUALITY
                    )
                }
            }
        }
        
        // Storage Info
        StorageInfoCard(freeModelManager)
        
        // Model List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val recommendedModels = freeModelManager.getModelRecommendations(selectedUseCase)
            
            items(recommendedModels) { model ->
                FreeModelCard(
                    model = model,
                    isSelected = currentModel?.id == model.id,
                    isDownloaded = isModelDownloaded(model), // You'd implement this
                    downloadProgress = downloadProgress[model.id] ?: 0f,
                    onDownload = {
                        scope.launch {
                            freeModelManager.downloadModel(model)
                        }
                    },
                    onSelect = {
                        scope.launch {
                            if (freeModelManager.loadModel(model)) {
                                onModelSelected(model)
                            }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            freeModelManager.deleteModel(model)
                        }
                    }
                )
            }
        }
        
        // Current Status
        if (modelStatus != ModelStatus.IDLE) {
            StatusCard(modelStatus, currentModel)
        }
    }
}

@Composable
fun FreeModelCard(
    model: LocalModel,
    isSelected: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Model specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📦 ${model.size}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "🧠 ${model.minRamMb}MB RAM",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "⚡ ${model.tokensPerSecond.toInt()} tok/s",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    !isDownloaded && downloadProgress == 0f -> {
                        OutlinedButton(
                            onClick = onDownload,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                    downloadProgress > 0f && downloadProgress < 1f -> {
                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    isDownloaded -> {
                        Button(
                            onClick = onSelect,
                            modifier = Modifier.weight(1f),
                            enabled = !isSelected
                        ) {
                            Text(if (isSelected) "✅ Active" else "🚀 Use Model")
                        }
                        OutlinedButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageInfoCard(freeModelManager: FreeLocalModelManager) {
    val availableStorage = remember { freeModelManager.getAvailableStorage() }
    val availableGB = availableStorage / 1_000_000_000
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📱 Available Storage",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${availableGB}GB free",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusCard(status: ModelStatus, currentModel: LocalModel?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                ModelStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                ModelStatus.READY -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (status) {
                ModelStatus.DOWNLOADING -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                ModelStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                ModelStatus.GENERATING -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                ModelStatus.READY -> Text("✅", style = MaterialTheme.typography.titleMedium)
                ModelStatus.ERROR -> Text("❌", style = MaterialTheme.typography.titleMedium)
                else -> Text("💤", style = MaterialTheme.typography.titleMedium)
            }
            
            Column {
                Text(
                    text = when (status) {
                        ModelStatus.DOWNLOADING -> "Downloading model..."
                        ModelStatus.LOADING -> "Loading model into memory..."
                        ModelStatus.READY -> "Model ready: ${currentModel?.name ?: "Unknown"}"
                        ModelStatus.GENERATING -> "Generating response..."
                        ModelStatus.ERROR -> "Error with model"
                        else -> "No model loaded"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (status == ModelStatus.READY && currentModel != null) {
                    Text(
                        text = "🚀 ${currentModel.tokensPerSecond.toInt()} tokens/sec",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper function - implement based on your file system
private fun isModelDownloaded(model: LocalModel): Boolean {
    // Implement actual file check
    return false
}