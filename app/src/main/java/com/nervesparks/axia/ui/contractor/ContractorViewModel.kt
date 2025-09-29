package com.nervesparks.axia.ui.contractor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.nervesparks.axia.contractor.ContractorApiManager
import com.nervesparks.axia.huggingface.HuggingFaceManager
import com.nervesparks.axia.security.SecureStorageManager

/**
 * ViewModel for Contractor API Management
 * Coordinates between ContractorApiManager and HuggingFaceManager
 * Provides unified interface for the UI
 */
class ContractorViewModel(application: Application) : AndroidViewModel(application) {
    
    private val secureStorage = SecureStorageManager(application)
    private val contractorManager = ContractorApiManager(application, secureStorage)
    val huggingFaceManager = HuggingFaceManager(application)
    
    // Expose contractor manager state
    val availableContractors = contractorManager.availableContractors
    val activeContractors = contractorManager.activeContractors
    val contractorMetrics = contractorManager.contractorMetrics
    val isExecutingTasks = contractorManager.isExecutingTasks
    val taskQueue = contractorManager.taskQueue
    
    // Combined usage summary
    private val _usageSummary = MutableStateFlow<ContractorUsageSummary?>(null)
    val usageSummary: StateFlow<ContractorUsageSummary?> = _usageSummary.asStateFlow()
    
    init {
        // Initialize with any existing API keys
        loadExistingApiKeys()
        
        // Update usage summary when metrics change
        viewModelScope.launch {
            contractorMetrics.collect { metrics ->
                _usageSummary.value = contractorManager.getContractorUsageSummary()
            }
        }
    }
    
    /**
     * Add contractor API key
     */
    suspend fun addContractorApiKey(
        providerId: String,
        apiKey: String,
        additionalConfig: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return contractorManager.addContractorApiKey(providerId, apiKey, additionalConfig).also { result ->
            if (result.isSuccess && providerId == "huggingface") {
                // Also set in HuggingFaceManager
                huggingFaceManager.setApiKey(apiKey)
            }
        }
    }
    
    /**
     * Remove contractor API key
     */
    suspend fun removeContractorApiKey(providerId: String): Result<Unit> {
        return contractorManager.removeContractorApiKey(providerId)
    }
    
    /**
     * Execute autonomous task
     */
    suspend fun executeAutonomousTask(task: AutonomousTask): TaskExecutionResult {
        return contractorManager.executeAutonomousTask(task)
    }
    
    /**
     * Execute workflow using HuggingFace
     */
    suspend fun executeWorkflow(workflow: AutonomousWorkflow): Flow<WorkflowProgress> {
        return huggingFaceManager.executeWorkflow(workflow)
    }
    
    /**
     * Queue task for autonomous execution
     */
    fun queueAutonomousTask(task: AutonomousTask) {
        contractorManager.queueAutonomousTask(task)
    }
    
    /**
     * Load any existing API keys from secure storage
     */
    private fun loadExistingApiKeys() {
        viewModelScope.launch {
            val configuredProviders = secureStorage.getConfiguredProviders()
            
            configuredProviders.forEach { providerId ->
                val apiKey = secureStorage.getApiKey(providerId)
                if (apiKey != null) {
                    // Test and activate the contractor
                    contractorManager.addContractorApiKey(providerId, apiKey)
                    
                    // Special handling for HuggingFace
                    if (providerId == "huggingface") {
                        huggingFaceManager.setApiKey(apiKey)
                    }
                }
            }
        }
    }
}

// Import the data classes from the contractor package
import com.nervesparks.axia.contractor.ContractorProvider
import com.nervesparks.axia.contractor.ContractorMetrics
import com.nervesparks.axia.contractor.ContractorUsageSummary
import com.nervesparks.axia.contractor.AutonomousTask
import com.nervesparks.axia.contractor.TaskExecutionResult
import com.nervesparks.axia.huggingface.AutonomousWorkflow
import com.nervesparks.axia.huggingface.WorkflowProgress