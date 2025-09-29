package com.nervesparks.axia

import android.llama.cpp.LLamaAndroid
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File

// Axia imports
import com.nervesparks.axia.memory.*
import com.nervesparks.axia.autonomous.*
import com.nervesparks.axia.monitoring.*
import com.nervesparks.axia.data.*
import com.nervesparks.axia.models.*

/**
 * Enhanced MainViewModel for Axia
 * Integrates advanced memory management, autonomous operation, and performance monitoring
 */
class MainViewModel(
    private val llamaAndroid: LLamaAndroid,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val memoryManager: AxiaMemoryManager,
    private val autonomousAgent: AutonomousAgent,
    private val performanceTracker: PerformanceTracker
) : ViewModel() {

    // Chat state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    // Model management
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel.asStateFlow()

    private val _modelLoadingProgress = MutableStateFlow(0f)
    val modelLoadingProgress: StateFlow<Float> = _modelLoadingProgress.asStateFlow()

    // Axia-specific state
    private val _contextLength = MutableStateFlow(0)
    val contextLength: StateFlow<Int> = _contextLength.asStateFlow()

    private val _tokensPerSecond = MutableStateFlow(0.0)
    val tokensPerSecond: StateFlow<Double> = _tokensPerSecond.asStateFlow()

    private val _memoryUsage = MutableStateFlow(0L)
    val memoryUsage: StateFlow<Long> = _memoryUsage.asStateFlow()

    private val _autonomousMode = MutableStateFlow(false)
    val autonomousMode: StateFlow<Boolean> = _autonomousMode.asStateFlow()

    // Enhanced conversation context with memory management
    private val _conversationContext = MutableStateFlow<ConversationContext?>(null)
    val conversationContext: StateFlow<ConversationContext?> = _conversationContext.asStateFlow()

    init {
        initializeAxiaViewModel()
        startPerformanceMonitoring()
        loadConversationContext()
    }

    private fun initializeAxiaViewModel() {
        viewModelScope.launch {
            // Initialize memory-optimized chat
            memoryManager.memoryState.collect { memoryState ->
                _memoryUsage.value = memoryState.usedRAM
                
                // Adjust AI processing based on memory availability
                adjustAIProcessingForMemory(memoryState)
            }
        }

        viewModelScope.launch {
            // Monitor autonomous agent state
            autonomousAgent.agentState.collect { agentState ->
                _autonomousMode.value = agentState.isActive
                
                // Process autonomous responses
                if (agentState.hasNewResponse) {
                    processAutonomousResponse(agentState.lastResponse)
                }
            }
        }
    }

    private fun startPerformanceMonitoring() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                
                // Update performance metrics
                performanceTracker.performanceMetrics.value.let { metrics ->
                    _tokensPerSecond.value = performanceTracker.aiMetrics.value.tokensPerSecond
                    _contextLength.value = performanceTracker.aiMetrics.value.contextLength
                }
                
                // Check for context drift and manage memory
                checkContextHealth()
            }
        }
    }

    private fun loadConversationContext() {
        viewModelScope.launch {
            val context = memoryManager.loadConversationContext()
            _conversationContext.value = context
            
            // Restore previous messages if context exists
            context?.let { ctx ->
                _messages.value = ctx.messages
                _contextLength.value = ctx.totalTokens
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Add user message
                val userMessage = ChatMessage(
                    content = message,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + userMessage
                
                // Check if message should trigger autonomous mode
                if (shouldTriggerAutonomousMode(message)) {
                    handleAutonomousRequest(message)
                } else {
                    handleRegularChat(message)
                }
                
                // Update conversation context
                updateConversationContext()
                
            } catch (e: Exception) {
                handleError("Failed to send message: ${e.message}")
            } finally {
                _isLoading.value = false
                _currentInput.value = ""
            }
        }
    }

    private suspend fun handleRegularChat(message: String) {
        // Use memory-optimized processing
        val optimizedPrompt = memoryManager.optimizePromptForMemory(message, _messages.value)
        
        // Generate response using LLaMA with performance monitoring
        val startTime = System.currentTimeMillis()
        val response = generateLlamaResponse(optimizedPrompt)
        val endTime = System.currentTimeMillis()
        
        // Calculate performance metrics
        val tokens = response.length / 4 // Rough estimation
        val timeSeconds = (endTime - startTime) / 1000.0
        _tokensPerSecond.value = tokens / timeSeconds
        
        // Add AI response
        val aiMessage = ChatMessage(
            content = response,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            tokensPerSecond = _tokensPerSecond.value
        )
        _messages.value = _messages.value + aiMessage
    }

    private suspend fun handleAutonomousRequest(message: String) {
        // Route to autonomous agent
        val response = autonomousAgent.processCommand(message)
        
        val aiMessage = ChatMessage(
            content = response,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isAutonomous = true
        )
        _messages.value = _messages.value + aiMessage
        
        // If autonomous action was triggered, show status
        if (autonomousAgent.agentState.value.isActive) {
            val statusMessage = ChatMessage(
                content = "🤖 Autonomous mode activated. I'm now executing your request...",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                isSystem = true
            )
            _messages.value = _messages.value + statusMessage
        }
    }

    private suspend fun generateLlamaResponse(prompt: String): String {
        return try {
            // Use enhanced LLaMA generation with memory constraints
            val maxTokens = memoryManager.calculateOptimalTokens()
            
            llamaAndroid.send(prompt).also { response ->
                // Monitor for hallucination and drift
                val driftScore = performanceTracker.calculateDriftScore(response, _messages.value)
                if (driftScore > 0.8) {
                    // Context drift detected, refresh context
                    refreshConversationContext()
                }
            }
        } catch (e: Exception) {
            "I apologize, but I encountered an error while processing your request. Please try again."
        }
    }

    fun loadModel(modelPath: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _modelLoadingProgress.value = 0f
                
                // Load model with memory optimization
                memoryManager.prepareForModelLoading()
                
                // Simulate loading progress
                for (i in 1..10) {
                    delay(200)
                    _modelLoadingProgress.value = i / 10f
                }
                
                llamaAndroid.load(modelPath)
                _selectedModel.value = File(modelPath).name
                
                // Optimize memory after model loading
                memoryManager.optimizeAfterModelLoading()
                
                // Add system message
                val systemMessage = ChatMessage(
                    content = "⚡ Axia is now ready with enhanced capabilities. I can help you with autonomous tasks, system control, image generation, and more!",
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    isSystem = true
                )
                _messages.value = listOf(systemMessage)
                
            } catch (e: Exception) {
                handleError("Failed to load model: ${e.message}")
            } finally {
                _isLoading.value = false
                _modelLoadingProgress.value = 0f
            }
        }
    }

    fun loadExistingModels(directory: File) {
        val models = directory.listFiles()?.filter { 
            it.extension.lowercase() == "gguf" 
        }?.map { 
            it.absolutePath 
        } ?: emptyList()
        
        _availableModels.value = models
    }

    fun updateInput(input: String) {
        _currentInput.value = input
    }

    fun clearChat() {
        _messages.value = emptyList()
        _contextLength.value = 0
        memoryManager.clearConversationContext()
        _conversationContext.value = null
    }

    fun toggleAutonomousMode() {
        if (_autonomousMode.value) {
            autonomousAgent.pauseAgent()
        } else {
            autonomousAgent.activateAgent()
        }
    }

    // Enhanced helper methods for Axia
    private fun shouldTriggerAutonomousMode(message: String): Boolean {
        val autonomousKeywords = listOf(
            "control", "manage", "execute", "run command", "system", 
            "autonomous", "automatically", "wifi", "bluetooth", "call", 
            "sms", "screenshot", "app", "install", "download"
        )
        
        return autonomousKeywords.any { keyword ->
            message.lowercase().contains(keyword)
        }
    }

    private suspend fun processAutonomousResponse(response: String) {
        val aiMessage = ChatMessage(
            content = response,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isAutonomous = true
        )
        _messages.value = _messages.value + aiMessage
    }

    private suspend fun adjustAIProcessingForMemory(memoryState: MemoryState) {
        val memoryPressure = memoryState.usedRAM.toDouble() / memoryState.totalRAM
        
        // Adjust processing based on memory pressure
        when {
            memoryPressure > 0.9 -> {
                // Critical memory - move to swap
                memoryManager.moveToSwap(priority = SwapPriority.HIGH)
            }
            memoryPressure > 0.7 -> {
                // High memory - optimize context
                memoryManager.optimizeContext()
            }
        }
    }

    private suspend fun checkContextHealth() {
        val aiMetrics = performanceTracker.aiMetrics.value
        
        // Check for context drift
        if (aiMetrics.contextualDrift > 0.8 || aiMetrics.semanticDrift > 0.7) {
            refreshConversationContext()
        }
        
        // Check for hallucination risk
        if (aiMetrics.hallucinationRisk > 0.3) {
            // Reduce context length and refresh
            _messages.value = _messages.value.takeLast(10)
            updateConversationContext()
        }
    }

    private suspend fun refreshConversationContext() {
        val currentMessages = _messages.value
        val optimizedContext = memoryManager.optimizeConversationContext(currentMessages)
        
        _conversationContext.value = optimizedContext
        _contextLength.value = optimizedContext.totalTokens
    }

    private suspend fun updateConversationContext() {
        val context = ConversationContext(
            messages = _messages.value,
            totalTokens = _contextLength.value,
            lastUpdated = System.currentTimeMillis()
        )
        
        _conversationContext.value = context
        memoryManager.saveConversationContext(context)
    }

    private fun handleError(error: String) {
        val errorMessage = ChatMessage(
            content = "❌ $error",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isSystem = true
        )
        _messages.value = _messages.value + errorMessage
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup Axia components
        viewModelScope.launch {
            memoryManager.cleanup()
            autonomousAgent.shutdown()
            performanceTracker.stopMonitoring()
        }
    }
}

// Data classes for enhanced functionality
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val tokensPerSecond: Double = 0.0,
    val isAutonomous: Boolean = false,
    val isSystem: Boolean = false
)

data class ConversationContext(
    val messages: List<ChatMessage>,
    val totalTokens: Int,
    val lastUpdated: Long
)