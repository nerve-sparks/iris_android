package com.nervesparks.axia.models

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import com.nervesparks.axia.optimization.IntelligentHardwareOptimizer

/**
 * Cloud Model Manager for Axia
 * Supports multiple cloud providers with intelligent model selection
 * - OpenAI: GPT-4o, GPT-4o-mini, GPT-4.1, GPT-3.5-turbo
 * - Anthropic: Claude 3.5 Sonnet, Claude 3 Haiku, Claude 3 Opus
 * - Google: Gemini Ultra, Gemini Pro, Gemini Flash
 * - Local models: Managed by hardware optimizer for 6GB limit
 */
class CloudModelManager(
    private val context: Context,
    private val hardwareOptimizer: IntelligentHardwareOptimizer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // HTTP client with extended timeouts for model inference
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()
    
    // Model state
    private val _availableModels = MutableStateFlow<List<CloudModel>>(emptyList())
    val availableModels: StateFlow<List<CloudModel>> = _availableModels.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<CloudModel?>(null)
    val selectedModel: StateFlow<CloudModel?> = _selectedModel.asStateFlow()
    
    private val _modelPerformance = MutableStateFlow<Map<String, ModelPerformance>>(emptyMap())
    val modelPerformance: StateFlow<Map<String, ModelPerformance>> = _modelPerformance.asStateFlow()
    
    private val _inferenceStatus = MutableStateFlow<InferenceStatus>(InferenceStatus.IDLE)
    val inferenceStatus: StateFlow<InferenceStatus> = _inferenceStatus.asStateFlow()
    
    // API Configuration
    private var openaiApiKey: String? = null
    private var anthropicApiKey: String? = null
    private var googleApiKey: String? = null
    
    // Add secure API manager
    private lateinit var secureApiManager: com.nervesparks.axia.config.SecureApiManager
    
    // Error tracking
    private val _apiErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val apiErrors: StateFlow<Map<String, String>> = _apiErrors.asStateFlow()
    
    fun initialize(secureApiManager: com.nervesparks.axia.config.SecureApiManager) {
        this.secureApiManager = secureApiManager
        loadApiKeys()
    }
    
    private fun loadApiKeys() {
        openaiApiKey = secureApiManager.getOpenAiKey()
        anthropicApiKey = secureApiManager.getAnthropicKey()
    }
    
    init {
        // Initialize available models
        initializeAvailableModels()
        
        // Start performance monitoring
        startPerformanceMonitoring()
    }
    
    /**
     * Initialize available cloud models
     */
    private fun initializeAvailableModels() {
        val models = mutableListOf<CloudModel>()
        
        // OpenAI Models
        models.addAll(listOf(
            CloudModel(
                id = "gpt-4o",
                name = "GPT-4o",
                provider = ModelProvider.OPENAI,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.VISION, ModelCapability.FUNCTION_CALLING),
                contextWindow = 128000,
                maxOutputTokens = 4096,
                costPer1kTokens = 0.005,
                speed = ModelSpeed.FAST,
                quality = ModelQuality.EXCELLENT
            ),
            CloudModel(
                id = "gpt-4o-mini",
                name = "GPT-4o Mini",
                provider = ModelProvider.OPENAI,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.VISION),
                contextWindow = 128000,
                maxOutputTokens = 16384,
                costPer1kTokens = 0.00015,
                speed = ModelSpeed.VERY_FAST,
                quality = ModelQuality.GOOD
            ),
            CloudModel(
                id = "gpt-4-turbo",
                name = "GPT-4 Turbo",
                provider = ModelProvider.OPENAI,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.VISION, ModelCapability.FUNCTION_CALLING),
                contextWindow = 128000,
                maxOutputTokens = 4096,
                costPer1kTokens = 0.01,
                speed = ModelSpeed.MEDIUM,
                quality = ModelQuality.EXCELLENT
            ),
            CloudModel(
                id = "gpt-3.5-turbo",
                name = "GPT-3.5 Turbo",
                provider = ModelProvider.OPENAI,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE),
                contextWindow = 16384,
                maxOutputTokens = 4096,
                costPer1kTokens = 0.0005,
                speed = ModelSpeed.VERY_FAST,
                quality = ModelQuality.GOOD
            )
        ))
        
        // Anthropic Models
        models.addAll(listOf(
            CloudModel(
                id = "claude-3-5-sonnet-20241022",
                name = "Claude 3.5 Sonnet",
                provider = ModelProvider.ANTHROPIC,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.VISION, ModelCapability.ANALYSIS),
                contextWindow = 200000,
                maxOutputTokens = 8192,
                costPer1kTokens = 0.003,
                speed = ModelSpeed.FAST,
                quality = ModelQuality.EXCELLENT
            ),
            CloudModel(
                id = "claude-3-opus-20240229",
                name = "Claude 3 Opus",
                provider = ModelProvider.ANTHROPIC,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.VISION, ModelCapability.ANALYSIS),
                contextWindow = 200000,
                maxOutputTokens = 4096,
                costPer1kTokens = 0.015,
                speed = ModelSpeed.SLOW,
                quality = ModelQuality.EXCEPTIONAL
            ),
            CloudModel(
                id = "claude-3-haiku-20240307",
                name = "Claude 3 Haiku",
                provider = ModelProvider.ANTHROPIC,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE),
                contextWindow = 200000,
                maxOutputTokens = 4096,
                costPer1kTokens = 0.00025,
                speed = ModelSpeed.VERY_FAST,
                quality = ModelQuality.GOOD
            )
        ))
        
        // Google Models
        models.addAll(listOf(
            CloudModel(
                id = "gemini-1.5-pro",
                name = "Gemini 1.5 Pro",
                provider = ModelProvider.GOOGLE,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.VISION, ModelCapability.AUDIO),
                contextWindow = 2000000,
                maxOutputTokens = 8192,
                costPer1kTokens = 0.00125,
                speed = ModelSpeed.FAST,
                quality = ModelQuality.EXCELLENT
            ),
            CloudModel(
                id = "gemini-1.5-flash",
                name = "Gemini 1.5 Flash",
                provider = ModelProvider.GOOGLE,
                capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.VISION),
                contextWindow = 1000000,
                maxOutputTokens = 8192,
                costPer1kTokens = 0.000075,
                speed = ModelSpeed.VERY_FAST,
                quality = ModelQuality.GOOD
            )
        ))
        
        // Local Models (managed by hardware optimizer)
        models.addAll(getLocalModels())
        
        _availableModels.value = models
        
        // Select default model based on hardware capabilities
        selectOptimalDefaultModel()
    }
    
    /**
     * Get local models based on hardware constraints
     */
    private fun getLocalModels(): List<CloudModel> {
        val localModels = mutableListOf<CloudModel>()
        
        // Check available RAM for local models (6GB limit as requested)
        val availableRAM = hardwareOptimizer.getAvailableRAM()
        val maxModelSize = minOf(availableRAM, 6L * 1024 * 1024 * 1024) // 6GB limit
        
        if (maxModelSize >= 3L * 1024 * 1024 * 1024) { // 3GB available
            localModels.add(
                CloudModel(
                    id = "local-llama-3b",
                    name = "Llama 3B (Local)",
                    provider = ModelProvider.LOCAL,
                    capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE),
                    contextWindow = 4096,
                    maxOutputTokens = 2048,
                    costPer1kTokens = 0.0, // Free - local
                    speed = ModelSpeed.FAST,
                    quality = ModelQuality.GOOD,
                    memoryRequirement = 3L * 1024 * 1024 * 1024
                )
            )
        }
        
        if (maxModelSize >= 1L * 1024 * 1024 * 1024) { // 1GB available
            localModels.add(
                CloudModel(
                    id = "local-llama-1b",
                    name = "Llama 1B (Local)",
                    provider = ModelProvider.LOCAL,
                    capabilities = listOf(ModelCapability.TEXT),
                    contextWindow = 2048,
                    maxOutputTokens = 1024,
                    costPer1kTokens = 0.0,
                    speed = ModelSpeed.VERY_FAST,
                    quality = ModelQuality.MODERATE,
                    memoryRequirement = 1L * 1024 * 1024 * 1024
                )
            )
        }
        
        if (maxModelSize >= 512L * 1024 * 1024) { // 512MB available
            localModels.add(
                CloudModel(
                    id = "local-llama-500m",
                    name = "Llama 500M (Local)",
                    provider = ModelProvider.LOCAL,
                    capabilities = listOf(ModelCapability.TEXT),
                    contextWindow = 1024,
                    maxOutputTokens = 512,
                    costPer1kTokens = 0.0,
                    speed = ModelSpeed.VERY_FAST,
                    quality = ModelQuality.BASIC,
                    memoryRequirement = 512L * 1024 * 1024
                )
            )
        }
        
        return localModels
    }
    
    /**
     * Select optimal default model based on hardware and requirements
     */
    private fun selectOptimalDefaultModel() {
        val models = _availableModels.value
        
        // Prefer local models if sufficient hardware, otherwise cloud
        val localModel = models.find { it.provider == ModelProvider.LOCAL && it.name.contains("3B") }
        val defaultModel = localModel ?: models.find { it.id == "gpt-4o-mini" } ?: models.firstOrNull()
        
        _selectedModel.value = defaultModel
    }
    
    /**
     * Set API keys for cloud providers
     */
    fun setApiKeys(
        openaiKey: String? = null,
        anthropicKey: String? = null,
        googleKey: String? = null
    ) {
        openaiApiKey = openaiKey
        anthropicApiKey = anthropicKey
        googleApiKey = googleKey
    }
    
    /**
     * Select a specific model for inference
     */
    fun selectModel(modelId: String) {
        val model = _availableModels.value.find { it.id == modelId }
        if (model != null) {
            // Check if local model fits in memory
            if (model.provider == ModelProvider.LOCAL) {
                val availableRAM = hardwareOptimizer.getAvailableRAM()
                if (model.memoryRequirement > availableRAM) {
                    // Try to optimize memory first
                    scope.launch {
                        hardwareOptimizer.optimizeMemoryForModel(model.memoryRequirement)
                        _selectedModel.value = model
                    }
                    return
                }
            }
            
            _selectedModel.value = model
        }
    }
    
    /**
     * Intelligent model selection based on task type and requirements
     */
    suspend fun selectOptimalModel(
        taskType: TaskType,
        complexity: TaskComplexity,
        requiresVision: Boolean = false,
        requiresFunctionCalling: Boolean = false,
        maxCostPer1k: Double = 0.01,
        preferLocal: Boolean = false
    ): CloudModel? {
        val models = _availableModels.value
        
        // Filter models based on requirements
        val suitableModels = models.filter { model ->
            // Check capabilities
            val hasRequiredCapabilities = when (taskType) {
                TaskType.CODE_GENERATION -> model.capabilities.contains(ModelCapability.CODE)
                TaskType.TEXT_ANALYSIS -> model.capabilities.contains(ModelCapability.TEXT)
                TaskType.VISION -> model.capabilities.contains(ModelCapability.VISION)
                TaskType.GENERAL -> true
            }
            
            val hasVision = !requiresVision || model.capabilities.contains(ModelCapability.VISION)
            val hasFunctionCalling = !requiresFunctionCalling || model.capabilities.contains(ModelCapability.FUNCTION_CALLING)
            val isAffordable = model.costPer1kTokens <= maxCostPer1k
            val isLocal = model.provider == ModelProvider.LOCAL
            val meetsLocalPreference = !preferLocal || isLocal
            
            hasRequiredCapabilities && hasVision && hasFunctionCalling && isAffordable && meetsLocalPreference
        }
        
        // Sort by quality and speed, prefer local if requested
        val bestModel = suitableModels.sortedWith(compareByDescending<CloudModel> { 
            if (preferLocal && it.provider == ModelProvider.LOCAL) 10 else 0 
        }.thenByDescending { 
            it.quality.score 
        }.thenByDescending { 
            it.speed.score 
        }).firstOrNull()
        
        bestModel?.let { selectModel(it.id) }
        return bestModel
    }
    
    /**
     * Send inference request to selected model
     */
    suspend fun sendInference(
        prompt: String,
        maxTokens: Int = 2048,
        temperature: Float = 0.7f,
        systemPrompt: String? = null,
        images: List<String>? = null
    ): InferenceResult {
        val model = _selectedModel.value
            ?: return InferenceResult.error("No model selected")
        
        _inferenceStatus.value = InferenceStatus.PROCESSING
        
        return try {
            val result = when (model.provider) {
                ModelProvider.OPENAI -> sendOpenAIInference(model, prompt, maxTokens, temperature, systemPrompt, images)
                ModelProvider.ANTHROPIC -> sendAnthropicInference(model, prompt, maxTokens, temperature, systemPrompt, images)
                ModelProvider.GOOGLE -> sendGoogleInference(model, prompt, maxTokens, temperature, systemPrompt, images)
                ModelProvider.LOCAL -> sendLocalInference(model, prompt, maxTokens, temperature, systemPrompt)
            }
            
            // Update performance metrics
            updatePerformanceMetrics(model.id, result)
            
            _inferenceStatus.value = InferenceStatus.IDLE
            result
            
        } catch (e: Exception) {
            _inferenceStatus.value = InferenceStatus.ERROR
            InferenceResult.error("Inference failed: ${e.message}")
        }
    }
    
    /**
     * Send request to OpenAI API
     */
    private suspend fun sendOpenAIInference(
        model: CloudModel,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        systemPrompt: String?,
        images: List<String>?
    ): InferenceResult = withContext(Dispatchers.IO) {
        
        if (openaiApiKey == null) {
            return@withContext InferenceResult.error("OpenAI API key not configured")
        }
        
        val messages = JSONArray()
        
        // Add system prompt if provided
        if (systemPrompt != null) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        
        // Add user message with optional images
        val userMessage = JSONObject().apply {
            put("role", "user")
            
            if (images.isNullOrEmpty()) {
                put("content", prompt)
            } else {
                val contentArray = JSONArray()
                contentArray.put(JSONObject().apply {
                    put("type", "text")
                    put("text", prompt)
                })
                
                images.forEach { imageUrl ->
                    contentArray.put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", imageUrl)
                        })
                    })
                }
                
                put("content", contentArray)
            }
        }
        
        messages.put(userMessage)
        
        val requestBody = JSONObject().apply {
            put("model", model.id)
            put("messages", messages)
            put("max_tokens", maxTokens)
            put("temperature", temperature)
        }
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $openaiApiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val startTime = System.currentTimeMillis()
        
        try {
            val response = client.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                return@withContext InferenceResult.error("API error: ${response.code}")
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext InferenceResult.error("Empty response")
            
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            
            if (choices.length() == 0) {
                return@withContext InferenceResult.error("No response choices")
            }
            
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")
            
            val usage = jsonResponse.optJSONObject("usage")
            val tokensUsed = usage?.optInt("total_tokens", 0) ?: 0
            val cost = tokensUsed * model.costPer1kTokens / 1000.0
            
            InferenceResult.success(
                content = content,
                tokensUsed = tokensUsed,
                responseTime = responseTime,
                cost = cost,
                modelId = model.id
            )
            
        } catch (e: Exception) {
            InferenceResult.error("Request failed: ${e.message}")
        }
    }
    
    /**
     * Send request to Anthropic API
     */
    private suspend fun sendAnthropicInference(
        model: CloudModel,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        systemPrompt: String?,
        images: List<String>?
    ): InferenceResult = withContext(Dispatchers.IO) {
        
        if (anthropicApiKey == null) {
            return@withContext InferenceResult.error("Anthropic API key not configured")
        }
        
        val messages = JSONArray()
        val userContent = JSONArray()
        
        userContent.put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })
        
        // Add images if provided
        images?.forEach { imageUrl ->
            userContent.put(JSONObject().apply {
                put("type", "image")
                put("source", JSONObject().apply {
                    put("type", "base64")
                    put("media_type", "image/jpeg")
                    put("data", imageUrl) // Should be base64 encoded
                })
            })
        }
        
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userContent)
        })
        
        val requestBody = JSONObject().apply {
            put("model", model.id)
            put("max_tokens", maxTokens)
            put("temperature", temperature)
            put("messages", messages)
            if (systemPrompt != null) {
                put("system", systemPrompt)
            }
        }
        
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", anthropicApiKey!!)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val startTime = System.currentTimeMillis()
        
        try {
            val response = client.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                return@withContext InferenceResult.error("API error: ${response.code}")
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext InferenceResult.error("Empty response")
            
            val jsonResponse = JSONObject(responseBody)
            val content = jsonResponse.getJSONArray("content")
            val text = content.getJSONObject(0).getString("text")
            
            val usage = jsonResponse.optJSONObject("usage")
            val tokensUsed = usage?.optInt("input_tokens", 0)?.plus(usage.optInt("output_tokens", 0)) ?: 0
            val cost = tokensUsed * model.costPer1kTokens / 1000.0
            
            InferenceResult.success(
                content = text,
                tokensUsed = tokensUsed,
                responseTime = responseTime,
                cost = cost,
                modelId = model.id
            )
            
        } catch (e: Exception) {
            InferenceResult.error("Request failed: ${e.message}")
        }
    }
    
    /**
     * Send request to Google API
     */
    private suspend fun sendGoogleInference(
        model: CloudModel,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        systemPrompt: String?,
        images: List<String>?
    ): InferenceResult = withContext(Dispatchers.IO) {
        
        if (googleApiKey == null) {
            return@withContext InferenceResult.error("Google API key not configured")
        }
        
        val parts = JSONArray()
        parts.put(JSONObject().apply { put("text", prompt) })
        
        // Add images if provided
        images?.forEach { imageUrl ->
            parts.put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", imageUrl) // Should be base64 encoded
                })
            })
        }
        
        val contents = JSONArray()
        contents.put(JSONObject().apply {
            put("parts", parts)
        })
        
        val requestBody = JSONObject().apply {
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", maxTokens)
                put("temperature", temperature)
            })
            if (systemPrompt != null) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
            }
        }
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${model.id}:generateContent?key=$googleApiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val startTime = System.currentTimeMillis()
        
        try {
            val response = client.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime
            
            if (!response.isSuccessful) {
                return@withContext InferenceResult.error("API error: ${response.code}")
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext InferenceResult.error("Empty response")
            
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            
            val usage = jsonResponse.optJSONObject("usageMetadata")
            val tokensUsed = usage?.optInt("totalTokenCount", 0) ?: 0
            val cost = tokensUsed * model.costPer1kTokens / 1000.0
            
            InferenceResult.success(
                content = content,
                tokensUsed = tokensUsed,
                responseTime = responseTime,
                cost = cost,
                modelId = model.id
            )
            
        } catch (e: Exception) {
            InferenceResult.error("Request failed: ${e.message}")
        }
    }
    
    /**
     * Send request to local model
     */
    private suspend fun sendLocalInference(
        model: CloudModel,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        systemPrompt: String?
    ): InferenceResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        try {
            // This would integrate with your local LLaMA implementation
            val fullPrompt = if (systemPrompt != null) {
                "$systemPrompt\n\nUser: $prompt\nAssistant:"
            } else {
                "User: $prompt\nAssistant:"
            }
            
            // Call local model (placeholder - implement with actual LLaMA integration)
            val response = hardwareOptimizer.generateWithLocalModel(fullPrompt, maxTokens, temperature)
            val responseTime = System.currentTimeMillis() - startTime
            
            InferenceResult.success(
                content = response,
                tokensUsed = response.split(" ").size, // Rough estimate
                responseTime = responseTime,
                cost = 0.0, // Free for local
                modelId = model.id
            )
            
        } catch (e: Exception) {
            InferenceResult.error("Local inference failed: ${e.message}")
        }
    }
    
    /**
     * Update performance metrics for a model
     */
    private fun updatePerformanceMetrics(modelId: String, result: InferenceResult) {
        if (result is InferenceResult.Success) {
            val current = _modelPerformance.value[modelId] ?: ModelPerformance(modelId)
            val updated = current.copy(
                totalRequests = current.totalRequests + 1,
                totalTokens = current.totalTokens + result.tokensUsed,
                totalCost = current.totalCost + result.cost,
                averageResponseTime = (current.averageResponseTime * current.totalRequests + result.responseTime) / (current.totalRequests + 1),
                lastUsed = System.currentTimeMillis()
            )
            
            _modelPerformance.value = _modelPerformance.value + (modelId to updated)
        }
    }
    
    /**
     * Start performance monitoring
     */
    private fun startPerformanceMonitoring() {
        scope.launch {
            while (true) {
                delay(60000) // Check every minute
                
                // Clean up old performance data
                val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
                _modelPerformance.value = _modelPerformance.value.filterValues { 
                    it.lastUsed > cutoffTime 
                }
            }
        }
    }
    
    /**
     * Get model recommendations based on task and constraints
     */
    fun getModelRecommendations(
        taskType: TaskType,
        budget: Double = 0.01,
        preferLocal: Boolean = false
    ): List<CloudModel> {
        return _availableModels.value
            .filter { it.costPer1kTokens <= budget }
            .filter { model ->
                when (taskType) {
                    TaskType.CODE_GENERATION -> model.capabilities.contains(ModelCapability.CODE)
                    TaskType.TEXT_ANALYSIS -> model.capabilities.contains(ModelCapability.TEXT)
                    TaskType.VISION -> model.capabilities.contains(ModelCapability.VISION)
                    TaskType.GENERAL -> true
                }
            }
            .sortedWith(compareByDescending<CloudModel> { 
                if (preferLocal && it.provider == ModelProvider.LOCAL) 10 else 0 
            }.thenByDescending { 
                it.quality.score 
            }.thenBy { 
                it.costPer1kTokens 
            })
            .take(5)
    }
}

// Data classes and enums
data class CloudModel(
    val id: String,
    val name: String,
    val provider: ModelProvider,
    val capabilities: List<ModelCapability>,
    val contextWindow: Int,
    val maxOutputTokens: Int,
    val costPer1kTokens: Double,
    val speed: ModelSpeed,
    val quality: ModelQuality,
    val memoryRequirement: Long = 0, // For local models
    val isAvailable: Boolean = true
)

enum class ModelProvider {
    OPENAI, ANTHROPIC, GOOGLE, LOCAL
}

enum class ModelCapability {
    TEXT, CODE, VISION, AUDIO, FUNCTION_CALLING, ANALYSIS
}

enum class ModelSpeed(val score: Int) {
    VERY_FAST(5), FAST(4), MEDIUM(3), SLOW(2), VERY_SLOW(1)
}

enum class ModelQuality(val score: Int) {
    EXCEPTIONAL(5), EXCELLENT(4), GOOD(3), MODERATE(2), BASIC(1)
}

enum class TaskType {
    CODE_GENERATION, TEXT_ANALYSIS, VISION, GENERAL
}

enum class TaskComplexity {
    SIMPLE, MEDIUM, COMPLEX
}

enum class InferenceStatus {
    IDLE, PROCESSING, ERROR
}

data class ModelPerformance(
    val modelId: String,
    val totalRequests: Int = 0,
    val totalTokens: Int = 0,
    val totalCost: Double = 0.0,
    val averageResponseTime: Long = 0,
    val lastUsed: Long = System.currentTimeMillis()
)

sealed class InferenceResult {
    data class Success(
        val content: String,
        val tokensUsed: Int,
        val responseTime: Long,
        val cost: Double,
        val modelId: String
    ) : InferenceResult()
    
    data class Error(val message: String) : InferenceResult()
    
    companion object {
        fun success(content: String, tokensUsed: Int, responseTime: Long, cost: Double, modelId: String) =
            Success(content, tokensUsed, responseTime, cost, modelId)
        fun error(message: String) = Error(message)
    }
}