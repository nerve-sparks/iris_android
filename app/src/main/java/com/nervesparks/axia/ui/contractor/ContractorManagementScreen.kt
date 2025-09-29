package com.nervesparks.axia.ui.contractor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nervesparks.axia.contractor.*

/**
 * Contractor API Management UI for Axia
 * 
 * FEATURES:
 * - Add/remove contractor API keys
 * - View usage statistics and costs
 * - Toggle between local and cloud models
 * - Security settings for API keys
 * - Real-time cost tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorManagementScreen(
    contractorManager: ContractorApiManager,
    modifier: Modifier = Modifier
) {
    val contractorStatus by contractorManager.contractorStatus.collectAsStateWithLifecycle()
    val availableContractors by contractorManager.availableContractors.collectAsStateWithLifecycle()
    val usageStats by contractorManager.usageStats.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedContractor by remember { mutableStateOf<ContractorProvider?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CloudQueue,
                    contentDescription = "Contractor APIs",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Contractor API System",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when (contractorStatus) {
                        ContractorStatus.FREE_ONLY -> "🆓 100% Free Local Models Only"
                        ContractorStatus.CONTRACTORS_AVAILABLE -> "🚀 Local + Cloud APIs Available"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status Banner
        ContractorStatusBanner(contractorStatus)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Available Contractors
        Text(
            text = "Available Contractors",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(availableContractors) { contractor ->
                ContractorProviderCard(
                    contractor = contractor,
                    hasApiKey = contractorManager.getApiKey(contractor) != null,
                    usage = usageStats[contractor.name],
                    onAddKey = { 
                        selectedContractor = contractor
                        showAddDialog = true
                    },
                    onRemoveKey = { 
                        contractorManager.removeContractorKey(contractor)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    
    // Add API Key Dialog
    if (showAddDialog && selectedContractor != null) {
        AddApiKeyDialog(
            contractor = selectedContractor!!,
            onDismiss = { 
                showAddDialog = false
                selectedContractor = null
            },
            onConfirm = { apiKey ->
                contractorManager.storeContractorKey(selectedContractor!!, apiKey)
                showAddDialog = false
                selectedContractor = null
            }
        )
    }
}

@Composable
fun ContractorStatusBanner(status: ContractorStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                ContractorStatus.FREE_ONLY -> MaterialTheme.colorScheme.surface
                ContractorStatus.CONTRACTORS_AVAILABLE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (status) {
                    ContractorStatus.FREE_ONLY -> Icons.Default.LockOpen
                    ContractorStatus.CONTRACTORS_AVAILABLE -> Icons.Default.CloudDone
                },
                contentDescription = null,
                tint = when (status) {
                    ContractorStatus.FREE_ONLY -> MaterialTheme.colorScheme.onSurface
                    ContractorStatus.CONTRACTORS_AVAILABLE -> Color(0xFF4CAF50)
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (status) {
                        ContractorStatus.FREE_ONLY -> "Free Local Mode"
                        ContractorStatus.CONTRACTORS_AVAILABLE -> "Contractors Enabled"
                    },
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = when (status) {
                        ContractorStatus.FREE_ONLY -> "Using only local AI models. Add contractor keys to access cloud APIs."
                        ContractorStatus.CONTRACTORS_AVAILABLE -> "Cloud APIs available. You pay providers directly with your own keys."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorProviderCard(
    contractor: ContractorProvider,
    hasApiKey: Boolean,
    usage: ContractorUsage?,
    onAddKey: () -> Unit,
    onRemoveKey: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasApiKey) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contractor.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Text(
                        text = contractor.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (contractor.costInfo.isNotEmpty()) {
                        Text(
                            text = "💰 ${contractor.costInfo}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Status indicator
                Icon(
                    if (hasApiKey) Icons.Default.CheckCircle else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (hasApiKey) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Models
            if (contractor.models.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Models: ${contractor.models.take(3).joinToString(", ")}${if (contractor.models.size > 3) "..." else ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Usage stats (if key is configured)
            if (hasApiKey && usage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Requests: ${usage.totalRequests}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Est. Cost: $${String.format("%.3f", usage.estimatedTotalCost)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (hasApiKey) {
                    OutlinedButton(
                        onClick = onRemoveKey,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                } else {
                    Button(
                        onClick = onAddKey
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Key")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApiKeyDialog(
    contractor: ContractorProvider,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(false) }
    
    // Validate API key format
    LaunchedEffect(apiKey) {
        isValid = ContractorApiManager(context).validateApiKey(contractor, apiKey)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add ${contractor.displayName} API Key")
        },
        text = {
            Column {
                Text(
                    text = contractor.setupInstructions,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { 
                        Text(
                            when (contractor) {
                                ContractorProvider.OPENAI -> "sk-..."
                                ContractorProvider.ANTHROPIC -> "sk-ant-..."
                                ContractorProvider.HUGGING_FACE -> "hf_..."
                                else -> "Enter your API key"
                            }
                        )
                    },
                    isError = apiKey.isNotEmpty() && !isValid,
                    supportingText = {
                        if (apiKey.isNotEmpty() && !isValid) {
                            Text(
                                "Invalid API key format",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Keys are encrypted and stored securely",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(apiKey) },
                enabled = isValid
            ) {
                Text("Add Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}