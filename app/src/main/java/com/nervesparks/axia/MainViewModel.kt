package com.nervesparks.axia

import android.llama.cpp.LLamaAndroid
import android.util.Log
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
import com.nervesparks.axia.core.AxiaIntelligenceCore
import com.nervesparks.axia.execution.AutonomousTaskExecutor
import com.nervesparks.axia.security.AxiaSecurityLayer
import com.nervesparks.axia.generation.LocalImageGenerator
import com.nervesparks.axia.models.FreeLocalModelManager

// Contractor system imports
import com.nervesparks.axia.contractor.ContractorApiManager
import com.nervesparks.axia.contractor.ContractorStatus
import com.nervesparks.axia.contractor.huggingface.HuggingFaceIntegration
import com.nervesparks.axia.contractor.automation.WorkAutomationSystem

/**
 * Enhanced MainViewModel for Axia - CONTRACTOR API VERSION
 * 
 * CONTRACTOR MODEL:
 * - Local AI models = 100% FREE (TinyLlama, Phi-3, Llama, etc.)
 * - Cloud APIs = User provides their own API keys (they pay directly)
 * - Extensive Hugging Face integration for downloads & inference
 * - Complete work automation with autonomous phone control
 * 
 * FEATURES:
 * ✅ Secure API key management with AES-256 encryption
 * ✅ Real-time cost tracking and usage analytics
 * ✅ Seamless switching between local and cloud models
 * ✅ Extensive Hugging Face model downloads and inference
 * ✅ Advanced work automation (phone, email, calendar, etc.)
 * ✅ Multi-provider support (OpenAI, Anthropic, Google AI, etc.)
 */
class MainViewModel(
    private val llamaAndroid: LLamaAndroid,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val memoryManager: AxiaMemoryManager,
    private val performanceTracker: PerformanceTracker,
    private val hardwareOptimizer: IntelligentHardwareOptimizer,
    private val context: android.content.Context
) : ViewModel() {

    // Core Axia systems - CONTRACTOR VERSION
    private val securityLayer = AxiaSecurityLayer(context)
    private val conversationMemory = ConversationMemory(context, securityLayer)  
    private val autonomousExecutor = AutonomousTaskExecutor(context)
    private val imageGenerator = LocalImageGenerator(context, hardwareOptimizer)
    private val freeModelManager = FreeLocalModelManager(context, llamaAndroid, hardwareOptimizer)
    
    // NEW: Contractor API System
    private val contractorManager = ContractorApiManager(context)
    private val huggingFaceIntegration = HuggingFaceIntegration(context, contractorManager)
    private val workAutomation = WorkAutomationSystem(context, contractorManager, huggingFaceIntegration)
    
    // Intelligence core with FREE + CONTRACTOR models
    private val axiaCore = AxiaIntelligenceCore(
        context = context,
        freeModelManager = freeModelManager,
        memoryManager = memoryManager,
        hardwareOptimizer = hardwareOptimizer,
        contractorManager = contractorManager,
        huggingFaceIntegration = huggingFaceIntegration
    )

    // Chat state
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    // Model management - FREE local + CONTRACTOR cloud models  
    private val _availableModels = MutableStateFlow(
        listOf(
            // LOCAL MODELS (100% FREE)
            "🆓 TinyLlama 1.1B (Ultra-fast, FREE)",
            "🆓 Phi-3-mini 3.8B (Smart & efficient, FREE)",
            "🆓 Llama 2 7B (Powerful, FREE)", 
            "🆓 CodeLlama 7B (Programming, FREE)",
            "🆓 Mistral 7B (High-quality, FREE)",
            "🆓 Gemma 2B (Google's model, FREE)",
            
            // CONTRACTOR MODELS (User pays directly)
            "🔑 GPT-4o (OpenAI, contractor)",
            "🔑 Claude 3.5 Sonnet (Anthropic, contractor)",
            "🔑 Gemini Pro (Google AI, contractor)",
            "🔑 Command R+ (Cohere, contractor)",
            "🤗 HuggingFace Models (Download any model)"
        )
    )
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _selectedModel = MutableStateFlow("🆓 TinyLlama 1.1B (Ultra-fast, FREE)")
    val selectedModel: StateFlow<String?> = _selectedModel.asStateFlow()

    // NEW: Contractor system state
    private val _contractorStatus = MutableStateFlow<ContractorStatus>(ContractorStatus.FREE_ONLY)
    val contractorStatus: StateFlow<ContractorStatus> = _contractorStatus.asStateFlow()
    
    private val _huggingFaceModels = MutableStateFlow<List<String>>(emptyList())
    val huggingFaceModels: StateFlow<List<String>> = _huggingFaceModels.asStateFlow()
    
    private val _automationCapabilities = MutableStateFlow<List<String>>(emptyList())
    val automationCapabilities: StateFlow<List<String>> = _automationCapabilities.asStateFlow()

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
        initializeContractorSystem()
        startPerformanceMonitoring()
        loadConversationContext()
    }
    
    private fun initializeContractorSystem() {
        viewModelScope.launch {
            // Monitor contractor status
            contractorManager.contractorStatus.collect { status ->
                _contractorStatus.value = status
                updateAvailableModels(status)
            }
        }
        
        viewModelScope.launch {
            // Monitor HuggingFace models
            huggingFaceIntegration.availableModels.collect { models ->
                _huggingFaceModels.value = models.map { "🤗 ${it.name} (${it.author})" }
            }
        }
        
        viewModelScope.launch {
            // Monitor automation capabilities  
            workAutomation.automationCapabilities.collect { capabilities ->
                _automationCapabilities.value = capabilities.map { 
                    "${getCategoryEmoji(it.category)} ${it.name}" 
                }
            }
        }
    }
    
    private fun updateAvailableModels(status: ContractorStatus) {
        val baseModels = listOf(
            "🆓 TinyLlama 1.1B (Ultra-fast, FREE)",
            "🆓 Phi-3-mini 3.8B (Smart & efficient, FREE)",
            "🆓 Llama 2 7B (Powerful, FREE)", 
            "🆓 CodeLlama 7B (Programming, FREE)",
            "🆓 Mistral 7B (High-quality, FREE)",
            "🆓 Gemma 2B (Google's model, FREE)"
        )
        
        val contractorModels = if (status == ContractorStatus.CONTRACTORS_AVAILABLE) {
            listOf(
                "🔑 GPT-4o (OpenAI, contractor)",
                "🔑 GPT-4o-mini (OpenAI, contractor)", 
                "🔑 Claude 3.5 Sonnet (Anthropic, contractor)",
                "🔑 Claude 3 Haiku (Anthropic, contractor)",
                "🔑 Gemini 1.5 Pro (Google AI, contractor)",
                "🔑 Command R+ (Cohere, contractor)"
            )
        } else {
            emptyList()
        }
        
        val huggingFaceOption = listOf("🤗 Browse HuggingFace Models")
        
        _availableModels.value = baseModels + contractorModels + huggingFaceOption
    }
    
    private fun getCategoryEmoji(category: com.nervesparks.axia.contractor.automation.AutomationCategory): String {
        return when (category) {
            com.nervesparks.axia.contractor.automation.AutomationCategory.COMMUNICATION -> "📱"
            com.nervesparks.axia.contractor.automation.AutomationCategory.PRODUCTIVITY -> "📋"
            com.nervesparks.axia.contractor.automation.AutomationCategory.SYSTEM -> "⚙️"
            com.nervesparks.axia.contractor.automation.AutomationCategory.RESEARCH -> "🔍"
            com.nervesparks.axia.contractor.automation.AutomationCategory.MARKETING -> "📈"
            com.nervesparks.axia.contractor.automation.AutomationCategory.ADVANCED -> "🚀"
        }
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

    /**
     * Send message using FREE local AI models only - no API costs!
     */
    fun sendMessage(message: String) {
        if (message.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Store user message with encryption
                val userMessage = ChatMessage(
                    content = message,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + userMessage
                conversationMemory.storeMessage(userMessage)
                
                // Check if autonomous action is requested
                if (shouldTriggerAutonomousMode(message)) {
                    handleAutonomousRequest(message)
                } else {
                    // Generate response using FREE local model
                    val response = freeModelManager.generateResponse(
                        prompt = message,
                        maxTokens = 512,
                        temperature = 0.7f
                    )
                    
                    // Store AI response
                    val aiMessage = ChatMessage(
                        content = response,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + aiMessage
                    conversationMemory.storeMessage(aiMessage)
                }
                
                // Update conversation context
                updateConversationContext()
                
            } catch (e: Exception) {
                handleError("❌ Error: ${e.message}\n\n💡 Tip: Make sure a local model is downloaded and loaded!")
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

    /**
     * Enhanced message sending with complete Axia integration
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Create user message with encryption
                val userMessage = ChatMessage(
                    content = message,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + userMessage

                // Store in encrypted memory
                conversationMemory.storeMessage(
                    conversationId = getCurrentConversationId(),
                    content = message,
                    isUser = true
                )

                // Determine response strategy using Axia Intelligence Core
                val response = axiaCore.processUserInput(
                    input = message,
                    context = conversationMemory.generateConversationContext(getCurrentConversationId())
                )

                when (response.type) {
                    com.nervesparks.axia.core.ResponseType.AUTONOMOUS_ACTION -> {
                        handleAutonomousTask(response.content)
                    }
                    com.nervesparks.axia.core.ResponseType.IMAGE_GENERATION -> {
                        handleImageGeneration(response.content)
                    }
                    com.nervesparks.axia.core.ResponseType.RESEARCH -> {
                        handleResearchRequest(response.content)
                    }
                    com.nervesparks.axia.core.ResponseType.DEVELOPMENT -> {
                        handleDevelopmentTask(response.content)
                    }
                    else -> {
                        handleNormalConversation(response.content)
                    }
                }

            } catch (e: Exception) {
                addErrorMessage("Error processing message: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle autonomous task execution
     */
    private suspend fun handleAutonomousTask(taskDescription: String) {
        val result = autonomousTaskExecutor.executeTask(
            description = taskDescription,
            context = getConversationContext(),
            requireConfirmation = false
        )

        val responseMessage = ChatMessage(
            content = if (result.isSuccess) {
                "✅ Task completed: ${result.message}"
            } else {
                "❌ Task failed: ${result.error}"
            },
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isAutonomous = true
        )

        _messages.value = _messages.value + responseMessage
        
        // Store encrypted response
        conversationMemory.storeMessage(
            conversationId = getCurrentConversationId(),
            content = responseMessage.content,
            isUser = false,
            metadata = com.nervesparks.axia.memory.MessageMetadata(isAutonomous = true)
        )
    }

    /**
     * Handle image generation requests
     */
    private suspend fun handleImageGeneration(prompt: String) {
        val result = localImageGenerator.generateImage(
            prompt = prompt,
            style = com.nervesparks.axia.generation.ImageStyle.REALISTIC
        )

        val responseMessage = if (result.isSuccess && result is com.nervesparks.axia.generation.ImageGenerationResult.Success) {
            ChatMessage(
                content = "🎨 Generated image: $prompt\n📁 Saved to: ${result.imagePath}",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
        } else {
            ChatMessage(
                content = "❌ Image generation failed: ${(result as com.nervesparks.axia.generation.ImageGenerationResult.Error).message}",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
        }

        _messages.value = _messages.value + responseMessage
        conversationMemory.storeMessage(
            conversationId = getCurrentConversationId(),
            content = responseMessage.content,
            isUser = false
        )
    }

    /**
     * Handle research requests
     */
    private suspend fun handleResearchRequest(query: String) {
        val response = axiaCore.conductResearch(query)
        
        val responseMessage = ChatMessage(
            content = response,
            isUser = false,
            timestamp = System.currentTimeMillis()
        )

        _messages.value = _messages.value + responseMessage
        conversationMemory.storeMessage(
            conversationId = getCurrentConversationId(),
            content = response,
            isUser = false
        )
    }

    /**
     * Handle development tasks
     */
    private suspend fun handleDevelopmentTask(taskDescription: String) {
        val response = axiaCore.assistDevelopment(taskDescription)
        
        val responseMessage = ChatMessage(
            content = response,
            isUser = false,
            timestamp = System.currentTimeMillis()
        )

        _messages.value = _messages.value + responseMessage
        conversationMemory.storeMessage(
            conversationId = getCurrentConversationId(),
            content = response,
            isUser = false
        )
    }

    /**
     * Handle normal conversation
     */
    private suspend fun handleNormalConversation(prompt: String) {
        // Use cloud model for response
        val result = cloudModelManager.sendInference(
            prompt = prompt,
            maxTokens = 2048,
            systemPrompt = "You are Axia, a helpful AI assistant with autonomous capabilities."
        )

        val responseMessage = when (result) {
            is InferenceResult.Success -> {
                ChatMessage(
                    content = result.content,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    tokensPerSecond = calculateTokensPerSecond(result.tokensUsed, result.responseTime)
                )
            }
            is InferenceResult.Error -> {
                ChatMessage(
                    content = "❌ Error: ${result.message}",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            }
        }

        _messages.value = _messages.value + responseMessage
        conversationMemory.storeMessage(
            conversationId = getCurrentConversationId(),
            content = responseMessage.content,
            isUser = false,
            metadata = com.nervesparks.axia.memory.MessageMetadata(
                tokensUsed = if (result is InferenceResult.Success) result.tokensUsed else 0,
                responseTime = if (result is InferenceResult.Success) result.responseTime else 0
            )
        )
    }

    /**
     * Execute phone control actions
     */
    fun executePhoneAction(action: String, parameters: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            try {
                val result = when (action) {
                    "call" -> autonomousTaskExecutor.makePhoneCall(
                        contactName = parameters["contact"],
                        phoneNumber = parameters["number"],
                        context = parameters["context"] ?: ""
                    )
                    "sms" -> autonomousTaskExecutor.sendTextMessage(
                        contactName = parameters["contact"],
                        phoneNumber = parameters["number"],
                        messageContent = parameters["message"],
                        context = parameters["context"] ?: ""
                    )
                    "wifi" -> autonomousTaskExecutor.controlSystemSetting(
                        "wifi", 
                        parameters["action"] ?: "toggle"
                    )
                    "bluetooth" -> autonomousTaskExecutor.controlSystemSetting(
                        "bluetooth", 
                        parameters["action"] ?: "toggle"
                    )
                    "volume" -> autonomousTaskExecutor.controlSystemSetting(
                        "volume", 
                        "set", 
                        parameters["level"]
                    )
                    "app" -> autonomousTaskExecutor.launchApplication(
                        parameters["name"] ?: ""
                    )
                    "web" -> autonomousTaskExecutor.openWebsite(
                        parameters["url"] ?: ""
                    )
                    else -> com.nervesparks.axia.execution.TaskExecutionResult.failure("Unknown action: $action")
                }

                val message = ChatMessage(
                    content = if (result.isSuccess) "✅ ${result.message}" else "❌ ${result.error}",
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    isAutonomous = true
                )

                _messages.value = _messages.value + message
            } catch (e: Exception) {
                addErrorMessage("Action execution failed: ${e.message}")
            }
        }
    }

    /**
     * Generate image with AI
     */
    fun generateImage(prompt: String, style: String = "realistic") {
        viewModelScope.launch {
            try {
                val imageStyle = when (style.lowercase()) {
                    "artistic" -> com.nervesparks.axia.generation.ImageStyle.ARTISTIC
                    "cartoon" -> com.nervesparks.axia.generation.ImageStyle.CARTOON
                    "anime" -> com.nervesparks.axia.generation.ImageStyle.ANIME
                    else -> com.nervesparks.axia.generation.ImageStyle.REALISTIC
                }

                val result = localImageGenerator.generateImage(
                    prompt = prompt,
                    style = imageStyle,
                    aspectRatio = com.nervesparks.axia.generation.AspectRatio.SQUARE
                )

                val message = if (result.isSuccess && result is com.nervesparks.axia.generation.ImageGenerationResult.Success) {
                    ChatMessage(
                        content = "🎨 Generated: $prompt\n⏱️ ${result.generationTime}ms\n📁 ${result.imagePath}",
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    ChatMessage(
                        content = "❌ Generation failed: ${(result as com.nervesparks.axia.generation.ImageGenerationResult.Error).message}",
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                }

                _messages.value = _messages.value + message
            } catch (e: Exception) {
                addErrorMessage("Image generation failed: ${e.message}")
            }
        }
    }

    /**
     * Switch AI model
     */
    fun switchModel(modelId: String) {
        viewModelScope.launch {
            cloudModelManager.selectModel(modelId)
            addSystemMessage("Switched to model: $modelId")
        }
    }

    /**
     * Get security status
     */
    fun getSecurityStatus(): Flow<com.nervesparks.axia.security.SecurityReport> = flow {
        emit(securityLayer.getSecurityReport())
    }

    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics() = performanceTracker.performanceMetrics

    // Private helper methods

    private fun getCurrentConversationId(): String {
        // For now, use a single conversation. In production, manage multiple conversations
        return "main_conversation"
    }

    private fun getConversationContext(): String {
        return _messages.value.takeLast(5).joinToString("\n") { 
            "${if (it.isUser) "User" else "Axia"}: ${it.content}" 
        }
    }

    private fun calculateTokensPerSecond(tokens: Int, timeMs: Long): Double {
        return if (timeMs > 0) (tokens * 1000.0) / timeMs else 0.0
    }

    private fun addSystemMessage(message: String) {
        val systemMessage = ChatMessage(
            content = "ℹ️ $message",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isSystem = true
        )
        _messages.value = _messages.value + systemMessage
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup Axia components
        viewModelScope.launch {
            memoryManager.cleanup()
            performanceTracker.stopMonitoring()
            // Additional cleanup for new components
        }
    }
    
    /**
     * Test security system functionality
     */
    suspend fun testSecurity(): Boolean {
        return try {
            val testData = "Test security data"
            val encrypted = securityLayer.encryptData(testData.toByteArray())
            val decrypted = securityLayer.decryptData(encrypted)
            String(decrypted) == testData
        } catch (e: Exception) {
            logError("Security test failed", e)
            false
        }
    }
    
    /**
     * Test memory system functionality
     */
    suspend fun testMemory(): Boolean {
        return try {
            val testMessage = ChatMessage(
                content = "Test memory message",
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
            conversationMemory.storeMessage(testMessage)
            val retrieved = conversationMemory.getConversationMessages("test-conversation", 1)
            retrieved.isNotEmpty()
        } catch (e: Exception) {
            logError("Memory test failed", e)
            false
        }
    }
    
    /**
     * Log error with detailed information
     */
    private fun logError(message: String, error: Throwable) {
        Log.e("MainViewModel", "$message: ${error.message}")
        error.printStackTrace()
    }
    
    // ==============================================
    // CONTRACTOR SYSTEM ACCESS METHODS
    // ==============================================
    
    /**
     * Get contractor API manager
     */
    fun getContractorManager(): ContractorApiManager {
        return contractorManager
    }
    
    /**
     * Get HuggingFace integration
     */
    fun getHuggingFaceIntegration(): HuggingFaceIntegration {
        return huggingFaceIntegration
    }
    
    /**
     * Get work automation system
     */
    fun getWorkAutomationSystem(): WorkAutomationSystem {
        return workAutomation
    }
    
    /**
     * Execute work automation task
     */
    fun executeWorkAutomation(taskType: String, parameters: Map<String, Any>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val result = when (taskType) {
                    "phone_call" -> workAutomation.executeAutomationTask(
                        "phone_control_make_call", 
                        parameters
                    )
                    "send_sms" -> workAutomation.executeAutomationTask(
                        "phone_control_send_sms",
                        parameters
                    )
                    "create_calendar_event" -> workAutomation.executeAutomationTask(
                        "calendar_management_create_event",
                        parameters
                    )
                    "send_email" -> workAutomation.executeAutomationTask(
                        "email_automation_send_email",
                        parameters
                    )
                    "organize_files" -> workAutomation.executeAutomationTask(
                        "file_management_organize_files",
                        parameters
                    )
                    else -> com.nervesparks.axia.contractor.automation.AutomationResult.error("Unknown automation task: $taskType")
                }
                
                val message = ChatMessage(
                    content = if (result.isSuccess) 
                        "🤖 Automation completed: ${result.message}" 
                    else 
                        "❌ Automation failed: ${result.error}",
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    isSystem = true
                )
                
                _messages.value = _messages.value + message
                
            } catch (e: Exception) {
                addErrorMessage("Work automation failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Download HuggingFace model
     */
    fun downloadHuggingFaceModel(modelId: String) {
        viewModelScope.launch {
            try {
                huggingFaceIntegration.downloadModel(modelId).collect { progress ->
                    val message = ChatMessage(
                        content = "🤗 Downloading ${modelId}: ${progress.percentage}%",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        isSystem = true
                    )
                    
                    _messages.value = _messages.value + message
                }
            } catch (e: Exception) {
                addErrorMessage("Model download failed: ${e.message}")
            }
        }
    }
    
    /**
     * Use contractor API for inference
     */
    fun useContractorModel(modelName: String, prompt: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Determine provider and model from selection
                val (provider, modelId) = parseContractorModel(modelName)
                
                if (provider == "huggingface") {
                    val result = huggingFaceIntegration.runInference(modelId, prompt)
                    
                    val message = ChatMessage(
                        content = result.output ?: result.error ?: "No response",
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    _messages.value = _messages.value + message
                } else {
                    // Handle other contractor APIs (OpenAI, Anthropic, etc.)
                    addErrorMessage("Direct contractor API calls not yet implemented for $provider")
                }
                
            } catch (e: Exception) {
                addErrorMessage("Contractor model inference failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Parse contractor model selection
     */
    private fun parseContractorModel(modelName: String): Pair<String, String> {
        return when {
            modelName.contains("GPT-4o") -> "openai" to "gpt-4o"
            modelName.contains("Claude") -> "anthropic" to "claude-3-5-sonnet"
            modelName.contains("Gemini") -> "google" to "gemini-1.5-pro"
            modelName.contains("HuggingFace") -> "huggingface" to "meta-llama/Llama-3.2-3B"
            else -> "local" to modelName
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