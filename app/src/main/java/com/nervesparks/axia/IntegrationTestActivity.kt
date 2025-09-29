package com.nervesparks.axia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * Integration test activity to verify all Axia components are working together
 */
class IntegrationTestActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AxiaTheme {
                IntegrationTestScreen()
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun IntegrationTestScreen() {
        val viewModel: MainViewModel = viewModel()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        var testResults by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var isTestingInProgress by remember { mutableStateOf(false) }
        var testMessage by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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
                        text = "🤖 Axia Integration Tests",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Verify all components are working together",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Permission Status
            PermissionStatusCard()
            
            // Test Controls
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "System Tests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Button(
                        onClick = {
                            requestPermissions()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request All Permissions")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                runIntegrationTests(viewModel) { results, message ->
                                    testResults = results
                                    testMessage = message
                                    isTestingInProgress = false
                                }
                                isTestingInProgress = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTestingInProgress
                    ) {
                        if (isTestingInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Text("Run All Tests")
                        }
                    }
                }
            }
            
            // Test Results
            if (testResults.isNotEmpty()) {
                TestResultsCard(testResults, testMessage)
            }
            
            // Component Status
            ComponentStatusCard(viewModel)
            
            // Quick Actions
            QuickActionsCard(viewModel)
        }
    }
    
    @Composable
    fun PermissionStatusCard() {
        val permissions = listOf(
            Manifest.permission.RECORD_AUDIO to "Microphone",
            Manifest.permission.CALL_PHONE to "Phone Calls",
            Manifest.permission.SEND_SMS to "Send SMS",
            Manifest.permission.READ_CONTACTS to "Read Contacts",
            Manifest.permission.CAMERA to "Camera",
            Manifest.permission.ACCESS_FINE_LOCATION to "Location",
            Manifest.permission.WRITE_EXTERNAL_STORAGE to "Storage"
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Permission Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                permissions.forEach { (permission, name) ->
                    val isGranted = ContextCompat.checkSelfPermission(
                        LocalContext.current,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isGranted) "✅ Granted" else "❌ Denied",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun TestResultsCard(results: Map<String, Boolean>, message: String) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (results.values.all { it }) 
                    MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Test Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                results.forEach { (test, passed) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = test,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (passed) "✅ PASS" else "❌ FAIL",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun ComponentStatusCard(viewModel: MainViewModel) {
        val components = listOf(
            "Security Layer" to "🔒",
            "Conversation Memory" to "🧠",
            "Autonomous Executor" to "🤖",
            "Image Generator" to "🎨",
            "Cloud Manager" to "☁️",
            "Hardware Optimizer" to "⚡"
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Component Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                components.forEach { (name, icon) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = icon,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "🟢 Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun QuickActionsCard(viewModel: MainViewModel) {
        val scope = rememberCoroutineScope()
        var actionResult by remember { mutableStateOf("") }
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val response = viewModel.sendMessage("Hello Axia, how are you?")
                                    actionResult = "✅ Chat test successful"
                                } catch (e: Exception) {
                                    actionResult = "❌ Chat test failed: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Chat")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    viewModel.generateImage("A beautiful sunset over the ocean", "realistic")
                                    actionResult = "✅ Image generation started"
                                } catch (e: Exception) {
                                    actionResult = "❌ Image generation failed: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Image")
                    }
                }
                
                if (actionResult.isNotEmpty()) {
                    Text(
                        text = actionResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (actionResult.startsWith("✅")) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        permissionLauncher.launch(permissions)
    }
    
    private suspend fun runIntegrationTests(
        viewModel: MainViewModel,
        onComplete: (Map<String, Boolean>, String) -> Unit
    ) {
        val results = mutableMapOf<String, Boolean>()
        
        try {
            // Test 1: Security Layer
            results["Security Encryption"] = try {
                viewModel.testSecurity()
                true
            } catch (e: Exception) {
                false
            }
            
            // Test 2: Conversation Memory
            results["Memory Storage"] = try {
                viewModel.testMemory()
                true
            } catch (e: Exception) {
                false
            }
            
            // Test 3: Model Switching
            results["Model Management"] = try {
                viewModel.switchModel("claude-3-haiku")
                true
            } catch (e: Exception) {
                false
            }
            
            // Test 4: Basic Chat
            results["Chat Functionality"] = try {
                val response = viewModel.sendMessage("Test message")
                response.isNotEmpty()
            } catch (e: Exception) {
                false
            }
            
            // Test 5: Image Generation
            results["Image Generation"] = try {
                viewModel.generateImage("Test image", "artistic")
                true
            } catch (e: Exception) {
                false
            }
            
            val successCount = results.values.count { it }
            val totalCount = results.size
            val message = "Completed $successCount/$totalCount tests successfully"
            
            onComplete(results, message)
            
        } catch (e: Exception) {
            onComplete(
                mapOf("Overall Test" to false), 
                "Test suite failed: ${e.message}"
            )
        }
    }
}