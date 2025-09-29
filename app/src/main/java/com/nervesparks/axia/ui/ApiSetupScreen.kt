package com.nervesparks.axia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSetupScreen(
    secureApiManager: com.nervesparks.axia.config.SecureApiManager,
    onSetupComplete: () -> Unit
) {
    var openaiKey by remember { mutableStateOf("") }
    var anthropicKey by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var validationResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🤖 Axia API Setup",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Configure your AI provider API keys securely",
            style = MaterialTheme.typography.bodyMedium
        )
        
        // OpenAI API Key
        OutlinedTextField(
            value = openaiKey,
            onValueChange = { openaiKey = it },
            label = { Text("OpenAI API Key") },
            placeholder = { Text("sk-proj-...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = validationResults["openai"]?.startsWith("Invalid") == true
        )
        
        if (validationResults["openai"] != null) {
            Text(
                text = validationResults["openai"]!!,
                color = if (validationResults["openai"]!!.startsWith("Valid")) 
                    MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Anthropic API Key
        OutlinedTextField(
            value = anthropicKey,
            onValueChange = { anthropicKey = it },
            label = { Text("Anthropic API Key") },
            placeholder = { Text("sk-ant-api03-...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = validationResults["anthropic"]?.startsWith("Invalid") == true
        )
        
        if (validationResults["anthropic"] != null) {
            Text(
                text = validationResults["anthropic"]!!,
                color = if (validationResults["anthropic"]!!.startsWith("Valid")) 
                    MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Button(
            onClick = {
                scope.launch {
                    isValidating = true
                    val results = mutableMapOf<String, String>()
                    
                    if (openaiKey.isNotEmpty()) {
                        val result = secureApiManager.validateApiKey("openai", openaiKey)
                        results["openai"] = result.message
                        if (result.isValid) {
                            secureApiManager.setOpenAiKey(openaiKey)
                        }
                    }
                    
                    if (anthropicKey.isNotEmpty()) {
                        val result = secureApiManager.validateApiKey("anthropic", anthropicKey)
                        results["anthropic"] = result.message
                        if (result.isValid) {
                            secureApiManager.setAnthropicKey(anthropicKey)
                        }
                    }
                    
                    validationResults = results
                    isValidating = false
                    
                    if (results.values.all { it.startsWith("Valid") }) {
                        onSetupComplete()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isValidating && (openaiKey.isNotEmpty() || anthropicKey.isNotEmpty())
        ) {
            if (isValidating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validating...")
            } else {
                Text("Validate & Save Keys")
            }
        }
        
        // Help Text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📋 How to get API keys:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Text(
                    text = "• OpenAI: Visit platform.openai.com → API keys",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "• Anthropic: Visit console.anthropic.com → API keys",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "• Keys are stored securely with hardware encryption",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}