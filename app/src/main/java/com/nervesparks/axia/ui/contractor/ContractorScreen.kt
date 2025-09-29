package com.nervesparks.axia.ui.contractor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.nervesparks.axia.contractor.*
import com.nervesparks.axia.huggingface.*

/**
 * Contractor API Management Screen for Axia
 * "Your personal AI contractors - you provide the keys, we handle the rest"
 * 
 * Features:
 * ✅ Add/manage API keys for multiple providers
 * ✅ Extensive Hugging Face integration
 * ✅ Autonomous task execution
 * ✅ Real-time usage tracking
 * ✅ Performance metrics
 * ✅ Work automation workflows
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorScreen(
    viewModel: ContractorViewModel = viewModel(),
    onNavigateToWorkflow: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    val availableContractors by viewModel.availableContractors.collectAsState()
    val activeContractors by viewModel.activeContractors.collectAsState()
    val contractorMetrics by viewModel.contractorMetrics.collectAsState()
    val isExecutingTasks by viewModel.isExecutingTasks.collectAsState()
    val usageSummary by viewModel.usageSummary.collectAsState()
    
    // UI State
    var selectedTab by remember { mutableStateOf(0) }
    var showAddApiKeyDialog by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf<ContractorProvider?>(null) }
    
    val tabs = listOf("Contractors", "Hugging Face", "Analytics", "Workflows")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
    ) {
        // Header
        ContractorHeader(
            activeContractors = activeContractors.size,
            isExecuting = isExecutingTasks,
            usageSummary = usageSummary
        )
        
        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        // Content
        when (selectedTab) {
            0 -> ContractorsTab(
                contractors = availableContractors,
                activeContractors = activeContractors,
                metrics = contractorMetrics,
                onAddApiKey = { provider ->
                    selectedProvider = provider
                    showAddApiKeyDialog = true
                },
                onRemoveApiKey = { providerId ->
                    scope.launch {
                        viewModel.removeContractorApiKey(providerId)
                    }
                }
            )
            
            1 -> HuggingFaceTab(
                huggingFaceManager = viewModel.huggingFaceManager,
                onExecuteTask = { task ->
                    scope.launch {
                        viewModel.executeAutonomousTask(task)
                    }
                }
            )
            
            2 -> AnalyticsTab(
                metrics = contractorMetrics,
                usageSummary = usageSummary
            )
            
            3 -> WorkflowsTab(
                onCreateWorkflow = onNavigateToWorkflow,
                onExecuteWorkflow = { workflow ->
                    scope.launch {
                        viewModel.executeWorkflow(workflow)
                    }
                }
            )
        }
    }
    
    // Add API Key Dialog
    if (showAddApiKeyDialog && selectedProvider != null) {
        AddApiKeyDialog(
            provider = selectedProvider!!,
            onConfirm = { provider, apiKey, config ->
                scope.launch {
                    viewModel.addContractorApiKey(provider.id, apiKey, config)
                    showAddApiKeyDialog = false
                    selectedProvider = null
                }
            },
            onDismiss = {
                showAddApiKeyDialog = false
                selectedProvider = null
            }
        )
    }
}

@Composable
private fun ContractorHeader(
    activeContractors: Int,
    isExecuting: Boolean,
    usageSummary: ContractorUsageSummary?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🤖 AI Contractors",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Your personal AI workforce - you provide the keys, we handle everything else",
                fontSize = 14.sp,
                color = Color(0xFFB0B0C4),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Active Contractors
                StatusCard(
                    title = "Active",
                    value = activeContractors.toString(),
                    icon = Icons.Default.Groups,
                    color = if (activeContractors > 0) Color(0xFF4CAF50) else Color(0xFF757575)
                )
                
                // Execution Status
                StatusCard(
                    title = "Status", 
                    value = if (isExecuting) "Working" else "Ready",
                    icon = if (isExecuting) Icons.Default.AutoMode else Icons.Default.CheckCircle,
                    color = if (isExecuting) Color(0xFFFF9800) else Color(0xFF4CAF50)
                )
                
                // Total Requests
                StatusCard(
                    title = "Requests",
                    value = usageSummary?.totalRequests?.toString() ?: "0",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        ),
        modifier = Modifier.size(width = 100.dp, height = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFFB0B0C4)
            )
        }
    }
}

@Composable
private fun ContractorsTab(
    contractors: List<ContractorProvider>,
    activeContractors: Set<String>,
    metrics: Map<String, ContractorMetrics>,
    onAddApiKey: (ContractorProvider) -> Unit,
    onRemoveApiKey: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Available AI Contractors",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(contractors) { contractor ->
            ContractorCard(
                contractor = contractor,
                isActive = activeContractors.contains(contractor.id),
                metrics = metrics[contractor.id],
                onAddApiKey = { onAddApiKey(contractor) },
                onRemoveApiKey = { onRemoveApiKey(contractor.id) }
            )
        }
    }
}

@Composable
private fun ContractorCard(
    contractor: ContractorProvider,
    isActive: Boolean,
    metrics: ContractorMetrics?,
    onAddApiKey: () -> Unit,
    onRemoveApiKey: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contractor.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = contractor.description,
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0C4)
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive) Color(0xFF4CAF50) else Color(0xFF757575)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Capabilities
            Text(
                text = "Capabilities:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB0B0C4)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(contractor.capabilities) { capability ->
                    Chip(
                        onClick = { },
                        label = { Text(capability.name.lowercase().replace('_', ' ')) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.2f),
                            labelColor = Color(0xFF2196F3)
                        )
                    )
                }
            }
            
            // Work Productivity Features
            if (contractor.workProductivityFeatures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Work Automation:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB0B0C4)
                )
                contractor.workProductivityFeatures.take(3).forEach { feature ->
                    Text(
                        text = "• $feature",
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Metrics (if active)
            if (isActive && metrics != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem("Requests", metrics.totalRequests.toString())
                    MetricItem("Success", "${(metrics.successRate * 100).toInt()}%")
                    MetricItem("Avg Time", "${metrics.averageResponseTime.toInt()}ms")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Cost and Setup Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = contractor.costEstimate,
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = contractor.setupComplexity.description,
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0C4)
                    )
                }
                
                // Action Button
                if (isActive) {
                    Button(
                        onClick = onRemoveApiKey,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE57373)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Remove", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onAddApiKey,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Add Key", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFB0B0C4)
        )
    }
}

@Composable
private fun HuggingFaceTab(
    huggingFaceManager: HuggingFaceManager,
    onExecuteTask: (AutonomousTask) -> Unit
) {
    val availableModels by huggingFaceManager.availableModels.collectAsState()
    val downloadedModels by huggingFaceManager.downloadedModels.collectAsState()
    val recommendations by huggingFaceManager.modelRecommendations.collectAsState()
    
    var selectedSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf("Models", "Downloads", "Recommendations")
    
    Column {
        // Sub-tab navigation
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }
        
        // Content based on sub-tab
        when (selectedSubTab) {
            0 -> HuggingFaceModelsContent(availableModels, huggingFaceManager)
            1 -> HuggingFaceDownloadsContent(downloadedModels, huggingFaceManager)
            2 -> HuggingFaceRecommendationsContent(recommendations, onExecuteTask)
        }
    }
}

@Composable
private fun HuggingFaceModelsContent(
    models: List<HuggingFaceModel>,
    huggingFaceManager: HuggingFaceManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🤗 Hugging Face Models",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = "Extensive integration with the world's largest AI model hub",
            fontSize = 14.sp,
            color = Color(0xFFB0B0C4),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (models.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFFB0B0C4),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Search for Models",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Use the search function to find models for your tasks",
                        fontSize = 14.sp,
                        color = Color(0xFFB0B0C4),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(models) { model ->
                    HuggingFaceModelCard(model, huggingFaceManager)
                }
            }
        }
    }
}

@Composable
private fun HuggingFaceModelCard(
    model: HuggingFaceModel,
    huggingFaceManager: HuggingFaceManager
) {
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.modelName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "by ${model.author}",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0C4)
                    )
                    
                    if (model.downloads != null && model.downloads > 0) {
                        Text(
                            text = "${model.downloads} downloads",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                if (model.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tags
            if (model.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(model.tags.take(4)) { tag ->
                        Chip(
                            onClick = { },
                            label = { Text(tag, fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.2f),
                                labelColor = Color(0xFF2196F3)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!model.isDownloaded) {
                    Button(
                        onClick = {
                            scope.launch {
                                huggingFaceManager.downloadModel(model.modelId)
                                    .collect { /* Handle progress */ }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            // Use model for inference
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Use Model", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HuggingFaceDownloadsContent(
    downloadedModels: List<HuggingFaceModel>,
    huggingFaceManager: HuggingFaceManager
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Downloaded Models (${downloadedModels.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        if (downloadedModels.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A2E)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFFB0B0C4),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Downloaded Models",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Download models to use them offline",
                            fontSize = 14.sp,
                            color = Color(0xFFB0B0C4),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } else {
            items(downloadedModels) { model ->
                HuggingFaceModelCard(model, huggingFaceManager)
            }
        }
    }
}

@Composable
private fun HuggingFaceRecommendationsContent(
    recommendations: List<ModelRecommendation>,
    onExecuteTask: (AutonomousTask) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Recommended Models",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(recommendations) { recommendation ->
            RecommendationCard(recommendation, onExecuteTask)
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: ModelRecommendation,
    onExecuteTask: (AutonomousTask) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.task,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = recommendation.modelId,
                        fontSize = 12.sp,
                        color = Color(0xFFB0B0C4)
                    )
                    Text(
                        text = recommendation.reason,
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Performance score
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.size(60.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${(recommendation.performanceScore * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Score",
                            fontSize = 8.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTab(
    metrics: Map<String, ContractorMetrics>,
    usageSummary: ContractorUsageSummary?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Analytics & Performance",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        if (usageSummary != null) {
            item {
                AnalyticsSummaryCard(usageSummary)
            }
        }
        
        if (metrics.isNotEmpty()) {
            items(metrics.toList()) { (providerId, metric) ->
                AnalyticsCard(providerId, metric)
            }
        }
    }
}

@Composable
private fun AnalyticsSummaryCard(summary: ContractorUsageSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Overall Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric("Total Requests", summary.totalRequests.toString())
                SummaryMetric("Success Rate", "${(summary.totalSuccessfulRequests * 100 / maxOf(summary.totalRequests, 1))}%")
                SummaryMetric("Avg Time", "${summary.averageResponseTime.toInt()}ms")
                SummaryMetric("Total Tokens", summary.totalTokensUsed.toString())
            }
            
            if (summary.topPerformingContractor != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Top Performer: ${summary.topPerformingContractor}",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFB0B0C4),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnalyticsCard(providerId: String, metrics: ContractorMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = providerId.replaceFirstChar { it.uppercase() },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("Requests", metrics.totalRequests.toString())
                MetricItem("Success", "${(metrics.successRate * 100).toInt()}%")
                MetricItem("Avg Time", "${metrics.averageResponseTime.toInt()}ms")
                MetricItem("Tokens", metrics.totalTokensUsed.toString())
            }
        }
    }
}

@Composable
private fun WorkflowsTab(
    onCreateWorkflow: () -> Unit,
    onExecuteWorkflow: (AutonomousWorkflow) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFB0B0C4),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Autonomous Workflows",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = "Create complex multi-step AI workflows",
            fontSize = 14.sp,
            color = Color(0xFFB0B0C4),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateWorkflow,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Workflow")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddApiKeyDialog(
    provider: ContractorProvider,
    onConfirm: (ContractorProvider, String, Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(false) }
    
    // Validate API key format
    LaunchedEffect(apiKey) {
        isValid = when (provider.id) {
            "openai" -> apiKey.startsWith("sk-") && apiKey.length > 20
            "anthropic" -> apiKey.startsWith("sk-ant-") && apiKey.length > 30
            "huggingface" -> apiKey.startsWith("hf_") && apiKey.length > 20
            "google" -> apiKey.length > 20
            "together" -> apiKey.length > 30
            else -> apiKey.length > 10
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add ${provider.name} API Key")
        },
        text = {
            Column {
                Text(
                    text = provider.description,
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0C4)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { 
                        Text(when (provider.id) {
                            "openai" -> "sk-..."
                            "anthropic" -> "sk-ant-..."
                            "huggingface" -> "hf_..."
                            else -> "Enter API key..."
                        })
                    },
                    isError = apiKey.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (apiKey.isNotEmpty() && !isValid) {
                    Text(
                        text = "Invalid API key format",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Get your API key from: ${when (provider.id) {
                        "openai" -> "platform.openai.com"
                        "anthropic" -> "console.anthropic.com"
                        "huggingface" -> "huggingface.co/settings/tokens"
                        "google" -> "makersuite.google.com"
                        "together" -> "api.together.xyz"
                        else -> "provider's website"
                    }}",
                    fontSize = 11.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(provider, apiKey, emptyMap()) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}