package com.nervesparks.iris.ui.knowledge

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.nervesparks.iris.knowledge.*

/**
 * Knowledge Base Management UI
 * Download and manage specialized datasets
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    knowledgeManager: KnowledgeBaseManager,
    dao: KnowledgeDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val datasets by dao.getAllDatasets().collectAsState(initial = emptyList())
    val downloadProgress by knowledgeManager.downloadProgress.collectAsState()
    val trainingProgress by knowledgeManager.trainingProgress.collectAsState()
    
    var showAddDataset by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<KnowledgeCategory?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_revert),
                    contentDescription = "Back",
                    tint = Color(0xFFE6EDF3)
                )
            }
            
            Text(
                text = "Knowledge Base",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold
                )
            )
            
            IconButton(onClick = { showAddDataset = true }) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_add),
                    contentDescription = "Add Dataset",
                    tint = Color(0xFF58A6FF)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category Filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    selected = selectedCategory == null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF58A6FF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF21262D),
                        labelColor = Color(0xFFE6EDF3)
                    )
                )
            }
            
            items(KnowledgeCategory.values()) { category ->
                FilterChip(
                    onClick = { selectedCategory = if (selectedCategory == category) null else category },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category.icon)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(category.displayName)
                        }
                    },
                    selected = selectedCategory == category,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF58A6FF),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF21262D),
                        labelColor = Color(0xFFE6EDF3)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dataset List
        val filteredDatasets = if (selectedCategory != null) {
            datasets.filter { it.category == selectedCategory }
        } else {
            datasets
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredDatasets.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Datasets Found",
                        description = "Add specialized datasets to enhance AI knowledge",
                        onAddClick = { showAddDataset = true }
                    )
                }
            } else {
                items(filteredDatasets) { dataset ->
                    DatasetCard(
                        dataset = dataset,
                        downloadProgress = downloadProgress[dataset.id],
                        trainingProgress = trainingProgress[dataset.id],
                        onToggleEnabled = { enabled ->
                            // Toggle dataset enabled state
                        },
                        onDelete = {
                            // Delete dataset
                        },
                        onRetrain = {
                            // Retrain dataset
                        }
                    )
                }
            }
        }
    }
    
    // Add Dataset Dialog
    if (showAddDataset) {
        AddDatasetDialog(
            onDismiss = { showAddDataset = false },
            onAdd = { dataset ->
                // Add and download dataset
                showAddDataset = false
            }
        )
    }
}

@Composable
private fun DatasetCard(
    dataset: KnowledgeDataset,
    downloadProgress: Float?,
    trainingProgress: Float?,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onRetrain: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        ),
        border = BorderStroke(
            1.dp,
            if (dataset.isEnabled) Color(0xFF58A6FF).copy(alpha = 0.3f) else Color(0xFF30363D)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dataset.category.icon,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = dataset.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFE6EDF3),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = dataset.category.displayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8B949E)
                            )
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator
                    StatusIndicator(
                        downloadStatus = dataset.downloadStatus,
                        trainingStatus = dataset.trainingStatus
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Enable/Disable Switch
                    Switch(
                        checked = dataset.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF58A6FF),
                            checkedTrackColor = Color(0xFF58A6FF).copy(alpha = 0.5f)
                        )
                    )
                }
            }
            
            // Progress bars
            downloadProgress?.let { progress ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloading...",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF58A6FF)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8B949E))
                    )
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF58A6FF)
                )
            }
            
            trainingProgress?.let { progress ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Training...",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8B949E))
                    )
                }
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50)
                )
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = dataset.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE6EDF3)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Dataset details
                    DatasetDetails(dataset)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (dataset.trainingStatus == TrainingStatus.COMPLETED) {
                            OutlinedButton(
                                onClick = onRetrain,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF58A6FF)
                                )
                            ) {
                                Text("Retrain")
                            }
                        }
                        
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF6B6B)
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    downloadStatus: DownloadStatus,
    trainingStatus: TrainingStatus
) {
    val (color, icon) = when {
        downloadStatus == DownloadStatus.DOWNLOADING -> Color(0xFF58A6FF) to "⬇️"
        trainingStatus == TrainingStatus.PROCESSING -> Color(0xFFFF9800) to "⚙️"
        trainingStatus == TrainingStatus.COMPLETED -> Color(0xFF4CAF50) to "✅"
        downloadStatus == DownloadStatus.FAILED || trainingStatus == TrainingStatus.FAILED -> Color(0xFFFF5722) to "❌"
        else -> Color(0xFF8B949E) to "⏳"
    }
    
    Text(
        text = icon,
        style = MaterialTheme.typography.titleMedium,
        color = color
    )
}

@Composable
private fun DatasetDetails(dataset: KnowledgeDataset) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DetailRow("Size", "${dataset.fileSize / 1024 / 1024} MB")
        DetailRow("Weight", "${dataset.weightMultiplier}x")
        DetailRow("Priority", dataset.priority.toString())
        if (dataset.downloadedAt > 0) {
            DetailRow("Downloaded", java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(dataset.downloadedAt)))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF8B949E)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📚",
                style = MaterialTheme.typography.displayMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF8B949E)
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58A6FF)
                )
            ) {
                Text("Add Dataset")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDatasetDialog(
    onDismiss: () -> Unit,
    onAdd: (KnowledgeDataset) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(KnowledgeCategory.GENERAL) }
    var weight by remember { mutableStateOf(1.0f) }
    var priority by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B22),
        title = {
            Text(
                text = "Add Knowledge Dataset",
                color = Color(0xFFE6EDF3)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Dataset Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3),
                        focusedLabelColor = Color(0xFF58A6FF),
                        unfocusedLabelColor = Color(0xFF8B949E),
                        focusedBorderColor = Color(0xFF58A6FF),
                        unfocusedBorderColor = Color(0xFF30363D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = { Text("Source URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3),
                        focusedLabelColor = Color(0xFF58A6FF),
                        unfocusedLabelColor = Color(0xFF8B949E),
                        focusedBorderColor = Color(0xFF58A6FF),
                        unfocusedBorderColor = Color(0xFF30363D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3),
                        focusedLabelColor = Color(0xFF58A6FF),
                        unfocusedLabelColor = Color(0xFF8B949E),
                        focusedBorderColor = Color(0xFF58A6FF),
                        unfocusedBorderColor = Color(0xFF30363D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF8B949E)
                    )
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(KnowledgeCategory.values()) { cat ->
                        FilterChip(
                            onClick = { category = cat },
                            label = { 
                                Text("${cat.icon} ${cat.displayName}")
                            },
                            selected = category == cat,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF58A6FF),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF21262D),
                                labelColor = Color(0xFFE6EDF3)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dataset = KnowledgeDataset(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        category = category,
                        description = description,
                        sourceUrl = sourceUrl,
                        filePath = "",
                        fileSize = 0,
                        checksum = "",
                        downloadStatus = DownloadStatus.PENDING,
                        trainingStatus = TrainingStatus.PENDING,
                        weightMultiplier = weight,
                        priority = priority
                    )
                    onAdd(dataset)
                },
                enabled = name.isNotBlank() && sourceUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58A6FF))
            ) {
                Text("Add & Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8B949E))
            }
        }
    )
}