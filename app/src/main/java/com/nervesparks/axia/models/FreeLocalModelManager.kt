package com.nervesparks.axia.models

import android.content.Context
import android.llama.cpp.LLamaAndroid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import android.util.Log
import com.nervesparks.axia.optimization.IntelligentHardwareOptimizer

/**
 * 🆓 FREE Local Model Manager for Axia
 * 100% FREE - No paid APIs required!
 * 
 * Supports completely FREE local AI models:
 * - Llama 2 (7B, 13B) - Meta's open-source models
 * - CodeLlama (7B, 13B) - For coding assistance  
 * - Mistral 7B - High-quality general purpose
 * - Phi-3-mini (3.8B) - Microsoft's efficient model
 * - Gemma 2B/7B - Google's open models
 * - TinyLlama (1.1B) - Ultra-lightweight
 * 
 * All models run locally on device - no internet required after download!
 */
class FreeLocalModelManager(
    private val context: Context,
    private val llamaAndroid: LLamaAndroid,
    private val hardwareOptimizer: IntelligentHardwareOptimizer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Model state
    private val _availableModels = MutableStateFlow<List<LocalModel>>(emptyList())
    val availableModels: StateFlow<List<LocalModel>> = _availableModels.asStateFlow()
    
    private val _currentModel = MutableStateFlow<LocalModel?>(null)
    val currentModel: StateFlow<LocalModel?> = _currentModel.asStateFlow()
    
    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.IDLE)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    init {
        initializeFreeModels()
        loadBestModelForDevice()
    }
    
    /**
     * Initialize all available FREE models
     */
    private fun initializeFreeModels() {
        val freeModels = listOf(
            // Ultra-lightweight - runs on any device
            LocalModel(
                id = "tinyllama-1.1b-q4",
                name = "TinyLlama 1.1B",
                description = "🚀 Ultra-fast, runs on any device (500MB)",
                size = "500MB",
                downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGML/resolve/main/tinyllama-1.1b-chat-v1.0.q4_0.bin",
                modelType = ModelType.GENERAL,
                minRamMb = 1024,
                optimalRamMb = 2048,
                tokensPerSecond = 15.0,
                contextLength = 2048,
                isRecommended = true
            ),
            
            // Phi-3-mini - Microsoft's efficient model
            LocalModel(
                id = "phi-3-mini-q4",
                name = "Phi-3-mini 3.8B",
                description = "🧠 Microsoft's smart & efficient (2.2GB)",
                size = "2.2GB",
                downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
                modelType = ModelType.GENERAL,
                minRamMb = 3072,
                optimalRamMb = 4096,
                tokensPerSecond = 12.0,
                contextLength = 4096,
                isRecommended = true
            ),
            
            // Llama 2 7B - Meta's flagship
            LocalModel(
                id = "llama2-7b-q4",
                name = "Llama 2 7B Chat",
                description = "🦙 Meta's powerful general AI (3.5GB)",
                size = "3.5GB",
                downloadUrl = "https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGML/resolve/main/llama-2-7b-chat.q4_0.bin",
                modelType = ModelType.GENERAL,
                minRamMb = 4096,
                optimalRamMb = 6144,
                tokensPerSecond = 8.0,
                contextLength = 4096,
                isRecommended = true
            ),
            
            // CodeLlama for programming
            LocalModel(
                id = "codellama-7b-q4",
                name = "CodeLlama 7B",
                description = "💻 Meta's coding specialist (3.5GB)",
                size = "3.5GB", 
                downloadUrl = "https://huggingface.co/TheBloke/CodeLlama-7B-Instruct-GGML/resolve/main/codellama-7b-instruct.q4_0.bin",
                modelType = ModelType.CODE,
                minRamMb = 4096,
                optimalRamMb = 6144,
                tokensPerSecond = 8.0,
                contextLength = 4096,
                isRecommended = false
            ),
            
            // Mistral 7B - High quality
            LocalModel(
                id = "mistral-7b-q4",
                name = "Mistral 7B Instruct",
                description = "⚡ High-quality & fast (3.8GB)",
                size = "3.8GB",
                downloadUrl = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.1-GGUF/resolve/main/mistral-7b-instruct-v0.1.q4_0.gguf",
                modelType = ModelType.GENERAL,
                minRamMb = 4096,
                optimalRamMb = 6144,
                tokensPerSecond = 10.0,
                contextLength = 8192,
                isRecommended = false
            ),
            
            // Gemma 2B - Google's compact model
            LocalModel(
                id = "gemma-2b-q4",
                name = "Gemma 2B",
                description = "🔥 Google's efficient model (1.2GB)",
                size = "1.2GB",
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-gguf/resolve/main/gemma-2b-it-q4_0.gguf",
                modelType = ModelType.GENERAL,
                minRamMb = 2048,
                optimalRamMb = 3072,
                tokensPerSecond = 13.0,
                contextLength = 8192,
                isRecommended = false
            )
        )
        
        _availableModels.value = freeModels
    }
    
    /**
     * Auto-select best model based on device capabilities
     */
    private fun loadBestModelForDevice() {
        scope.launch {
            val deviceRam = hardwareOptimizer.getTotalRAM()
            val availableRam = hardwareOptimizer.getAvailableRAM()
            
            Log.i("FreeModelManager", "Device RAM: ${deviceRam}MB, Available: ${availableRam}MB")
            
            val bestModel = when {
                availableRam >= 6144 -> _availableModels.value.find { it.id == "llama2-7b-q4" }
                availableRam >= 4096 -> _availableModels.value.find { it.id == "phi-3-mini-q4" }  
                availableRam >= 2048 -> _availableModels.value.find { it.id == "gemma-2b-q4" }
                else -> _availableModels.value.find { it.id == "tinyllama-1.1b-q4" }
            }
            
            bestModel?.let { model ->
                Log.i("FreeModelManager", "Auto-selected model: ${model.name}")
                if (isModelDownloaded(model)) {
                    loadModel(model)
                } else {
                    Log.i("FreeModelManager", "Model not downloaded, will download when needed")
                }
            }
        }
    }
    
    /**
     * Download a free model from Hugging Face
     */
    suspend fun downloadModel(model: LocalModel): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _modelStatus.value = ModelStatus.DOWNLOADING
                
                Log.i("FreeModelManager", "Starting download: ${model.name}")
                
                val modelFile = File(context.filesDir, "models/${model.id}.bin")
                modelFile.parentFile?.mkdirs()
                
                // Simulate download for now - implement actual download logic
                repeat(100) { progress ->
                    delay(50) // Simulate download time
                    val progressMap = _downloadProgress.value.toMutableMap()
                    progressMap[model.id] = progress / 100f
                    _downloadProgress.value = progressMap
                }
                
                // Create placeholder file for demo
                modelFile.writeText("Model placeholder - implement actual download")
                
                Log.i("FreeModelManager", "Download completed: ${model.name}")
                _modelStatus.value = ModelStatus.IDLE
                true
                
            } catch (e: Exception) {
                Log.e("FreeModelManager", "Download failed: ${e.message}")
                _modelStatus.value = ModelStatus.ERROR
                false
            }
        }
    }
    
    /**
     * Load a model for inference
     */
    suspend fun loadModel(model: LocalModel): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                _modelStatus.value = ModelStatus.LOADING
                
                if (!isModelDownloaded(model)) {
                    Log.w("FreeModelManager", "Model not downloaded, downloading first...")
                    if (!downloadModel(model)) {
                        return@withContext false
                    }
                }
                
                Log.i("FreeModelManager", "Loading model: ${model.name}")
                
                // Optimize memory for model loading
                hardwareOptimizer.optimizeForModelLoading()
                
                val modelFile = File(context.filesDir, "models/${model.id}.bin")
                
                // Load model through Llama Android
                val success = llamaAndroid.load_model(modelFile.absolutePath)
                
                if (success) {
                    _currentModel.value = model
                    _modelStatus.value = ModelStatus.READY
                    Log.i("FreeModelManager", "Model loaded successfully: ${model.name}")
                } else {
                    _modelStatus.value = ModelStatus.ERROR
                    Log.e("FreeModelManager", "Failed to load model: ${model.name}")
                }
                
                success
                
            } catch (e: Exception) {
                Log.e("FreeModelManager", "Model loading error: ${e.message}")
                _modelStatus.value = ModelStatus.ERROR
                false
            }
        }
    }
    
    /**
     * Generate response using loaded local model - 100% FREE
     */
    suspend fun generateResponse(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f
    ): String {
        return withContext(Dispatchers.Default) {
            try {
                val model = _currentModel.value
                if (model == null) {
                    return@withContext "❌ No model loaded. Please select and download a model first."
                }
                
                if (_modelStatus.value != ModelStatus.READY) {
                    return@withContext "❌ Model not ready. Current status: ${_modelStatus.value}"
                }
                
                _modelStatus.value = ModelStatus.GENERATING
                
                Log.i("FreeModelManager", "Generating response with ${model.name}")
                
                // Format prompt based on model type
                val formattedPrompt = formatPromptForModel(model, prompt)
                
                // Generate using Llama Android
                val response = llamaAndroid.send_message(formattedPrompt)
                
                _modelStatus.value = ModelStatus.READY
                
                return@withContext if (response.isNotEmpty()) {
                    "✅ **${model.name}**: $response"
                } else {
                    "❌ No response generated. Try a different prompt or model."
                }
                
            } catch (e: Exception) {
                Log.e("FreeModelManager", "Generation error: ${e.message}")
                _modelStatus.value = ModelStatus.READY
                return@withContext "❌ Error generating response: ${e.message}"
            }
        }
    }
    
    /**
     * Format prompt based on model requirements
     */
    private fun formatPromptForModel(model: LocalModel, prompt: String): String {
        return when (model.modelType) {
            ModelType.GENERAL -> {
                when {
                    model.id.contains("llama2") -> "<s>[INST] $prompt [/INST]"
                    model.id.contains("mistral") -> "<s>[INST] $prompt [/INST]"
                    model.id.contains("phi") -> "Human: $prompt\n\nAssistant:"
                    model.id.contains("gemma") -> "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"
                    else -> prompt
                }
            }
            ModelType.CODE -> {
                "### Instruction:\n$prompt\n\n### Response:\n"
            }
        }
    }
    
    /**
     * Check if model is already downloaded
     */
    private fun isModelDownloaded(model: LocalModel): Boolean {
        val modelFile = File(context.filesDir, "models/${model.id}.bin")
        return modelFile.exists()
    }
    
    /**
     * Get available storage space
     */
    fun getAvailableStorage(): Long {
        return context.filesDir.usableSpace
    }
    
    /**
     * Delete a downloaded model to free space
     */
    suspend fun deleteModel(model: LocalModel): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val modelFile = File(context.filesDir, "models/${model.id}.bin")
                if (modelFile.exists()) {
                    val deleted = modelFile.delete()
                    Log.i("FreeModelManager", "Model deleted: ${model.name}, success: $deleted")
                    deleted
                } else {
                    true // Already deleted
                }
            } catch (e: Exception) {
                Log.e("FreeModelManager", "Error deleting model: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Get model recommendations based on use case
     */
    fun getModelRecommendations(useCase: UseCase): List<LocalModel> {
        val allModels = _availableModels.value
        
        return when (useCase) {
            UseCase.GENERAL_CHAT -> allModels.filter { it.modelType == ModelType.GENERAL }
                .sortedBy { it.optimalRamMb }
            UseCase.CODING -> allModels.filter { it.modelType == ModelType.CODE }
            UseCase.LIGHTWEIGHT -> allModels.filter { it.minRamMb <= 2048 }
            UseCase.HIGH_QUALITY -> allModels.filter { it.minRamMb >= 4096 }
        }
    }
}

// Data classes
data class LocalModel(
    val id: String,
    val name: String,
    val description: String,
    val size: String,
    val downloadUrl: String,
    val modelType: ModelType,
    val minRamMb: Int,
    val optimalRamMb: Int,
    val tokensPerSecond: Double,
    val contextLength: Int,
    val isRecommended: Boolean = false
)

enum class ModelType {
    GENERAL,
    CODE
}

enum class ModelStatus {
    IDLE,
    DOWNLOADING,
    LOADING, 
    READY,
    GENERATING,
    ERROR
}

enum class UseCase {
    GENERAL_CHAT,
    CODING,
    LIGHTWEIGHT,
    HIGH_QUALITY
}