package com.nervesparks.iris.data.repository.impl

import android.llama.cpp.LLamaAndroid
import android.util.Log
import com.nervesparks.iris.data.HuggingFaceApiService
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.exceptions.ModelNotFoundException
import com.nervesparks.iris.data.exceptions.InvalidModelException
import com.nervesparks.iris.data.exceptions.ValidationException
import com.nervesparks.iris.data.exceptions.NetworkException
import com.nervesparks.iris.data.repository.ModelRepository
import com.nervesparks.iris.data.repository.ModelConfiguration
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.File

import javax.inject.Inject

/**
 * Implementation of ModelRepository for local model management
 */
class ModelRepositoryImpl @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val huggingFaceApiService: HuggingFaceApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val moshi: Moshi
) : ModelRepository {
    
    private val tag = "ModelRepositoryImpl"
    private var currentLoadedModel: String = ""
    private var cachedModels: List<Map<String, String>>? = null
    private fun curatedDefaultModels(): List<Map<String, String>> = listOf(
        mapOf(
            "name" to "deepcogito_cogito-v1-preview-llama-3B-Q4_K_M.gguf",
            "source" to "https://huggingface.co/bartowski/deepcogito_cogito-v1-preview-llama-3B-GGUF/resolve/main/deepcogito_cogito-v1-preview-llama-3B-Q4_K_M.gguf?download=true",
            "destination" to "deepcogito_cogito-v1-preview-llama-3B-Q4_K_M.gguf",
            "supportsReasoning" to "true",
            "chatTemplate" to "COGITO"
        ),
        mapOf(
            "name" to "LGAI-EXAONE_EXAONE-Deep-2.4B-Q4_K_M.gguf",
            "source" to "https://huggingface.co/bartowski/LGAI-EXAONE_EXAONE-Deep-2.4B-GGUF/resolve/main/LGAI-EXAONE_EXAONE-Deep-2.4B-Q4_K_M.gguf?download=true",
            "destination" to "LGAI-EXAONE_EXAONE-Deep-2.4B-Q4_K_M.gguf",
            "supportsReasoning" to "true",
            "chatTemplate" to "EXAONE"
        ),
        mapOf(
            "name" to "NousResearch_DeepHermes-3-Llama-3-3B-Preview-Q4_K_M.gguf",
            "source" to "https://huggingface.co/bartowski/NousResearch_DeepHermes-3-Llama-3-3B-Preview-GGUF/resolve/main/NousResearch_DeepHermes-3-Llama-3-3B-Preview-Q4_K_M.gguf?download=true",
            "destination" to "NousResearch_DeepHermes-3-Llama-3-3B-Preview-Q4_K_M.gguf",
            "supportsReasoning" to "false",
            "chatTemplate" to "DEEPHERMES"
        ),
        mapOf(
            "name" to "Qwen_Qwen3-0.6B-Q4_K_M.gguf",
            "source" to "https://huggingface.co/bartowski/Qwen_Qwen3-0.6B-GGUF/resolve/main/Qwen_Qwen3-0.6B-Q4_K_M.gguf?download=true",
            "destination" to "Qwen_Qwen3-0.6B-Q4_K_M.gguf",
            "supportsReasoning" to "false",
            "chatTemplate" to "QWEN3"
        ),
        mapOf(
            "name" to "Qwen_Qwen3-4B-Thinking-2507-Q4_K_M.gguf",
            "source" to "https://huggingface.co/bartowski/Qwen_Qwen3-4B-Thinking-2507-GGUF/resolve/main/Qwen_Qwen3-4B-Thinking-2507-Q4_K_M.gguf?download=true",
            "destination" to "Qwen_Qwen3-4B-Thinking-2507-Q4_K_M.gguf",
            "supportsReasoning" to "true",
            "chatTemplate" to "QWEN3"
        ),
        mapOf(
            "name" to "google_gemma-3n-E2B-it-Q4_K_M.gguf",
            "source" to "https://huggingface.co/bartowski/google_gemma-3n-E2B-it-GGUF/resolve/main/google_gemma-3n-E2B-it-Q4_K_M.gguf?download=true",
            "destination" to "google_gemma-3n-E2B-it-Q4_K_M.gguf",
            "supportsReasoning" to "false",
            "supportsVision" to "true",
            "chatTemplate" to "GEMMA"
        ),
        mapOf(
            "name" to "Llama-3.2-3B-Instruct-Q4_K_L.gguf",
            "source" to "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true",
            "destination" to "Llama-3.2-3B-Instruct-Q4_K_L.gguf",
            "supportsReasoning" to "false"
        ),
        mapOf(
            "name" to "Llama-3.2-1B-Instruct-Q6_K_L.gguf",
            "source" to "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true",
            "destination" to "Llama-3.2-1B-Instruct-Q6_K_L.gguf",
            "supportsReasoning" to "false"
        ),
        mapOf(
            "name" to "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf",
            "source" to "https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true",
            "destination" to "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf",
            "supportsReasoning" to "false"
        ),
        mapOf(
            "name" to "NemoTron-1.5B-Q4_K_M.gguf",
            "source" to "https://huggingface.co/bartowski/nvidia_OpenReasoning-Nemotron-1.5B-GGUF/resolve/main/nvidia_OpenReasoning-Nemotron-1.5B-Q4_K_M.gguf?download=true",
            "destination" to "NemoTron-1.5B-Q4_K_M.gguf",
            "supportsReasoning" to "true"
        )
    )

    private data class CachedModel(
        val name: String,
        val source: String,
        val destination: String,
        val supportsReasoning: Boolean = false
    )

    override suspend fun refreshAvailableModels(): List<Map<String, String>> {
        return try {
            // Validate input parameters
            val searchQuery = "gguf"
            if (searchQuery.isBlank()) {
                throw ValidationException("Search query cannot be blank")
            }
            
            val token = userPreferencesRepository.getHuggingFaceToken().takeIf { it.isNotEmpty() }
            val models = huggingFaceApiService.searchModels(searchQuery, token)
            val mapped = models.flatMap { info ->
                info.siblings.filter { it.filename.endsWith(".gguf") }.map { file ->
                    val source = "https://huggingface.co/${info.id}/resolve/main/${file.filename}?download=true"
                    mapOf(
                        "name" to file.filename,
                        "source" to source,
                        "destination" to file.filename,
                        "supportsReasoning" to "false"
                    )
                }
            }
            
            // Curated default models (centralized)
            val defaultModels = curatedDefaultModels()
            
            // Merge curated defaults with remote search results, de-duplicated by name
            val allModels = (defaultModels + mapped)
                .asReversed()
                .distinctBy { it["name"] }
                .asReversed()
            cachedModels = allModels

            // persist cache
            val listType = Types.newParameterizedType(List::class.java, CachedModel::class.java)
            val adapter = moshi.adapter<List<CachedModel>>(listType)
            val cacheData = allModels.map {
                CachedModel(
                    it["name"] ?: "",
                    it["source"] ?: "",
                    it["destination"] ?: "",
                    it["supportsReasoning"]?.toBoolean() ?: false
                )
            }
            userPreferencesRepository.setCachedModels(adapter.toJson(cacheData))

            allModels
        } catch (e: NetworkException) {
            Log.e(tag, "Network error refreshing model catalogue", e)
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing model catalogue", e)
            // Return curated default models even if API fails
            curatedDefaultModels()
        }
    }

    override suspend fun getAvailableModels(directory: File): List<Map<String, String>> {
        return try {
            if (cachedModels == null) {
                val json = userPreferencesRepository.getCachedModels()
                if (json.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, CachedModel::class.java)
                    val adapter = moshi.adapter<List<CachedModel>>(listType)
                    cachedModels = adapter.fromJson(json)?.map { model ->
                        mapOf(
                            "name" to model.name,
                            "source" to model.source,
                            "destination" to model.destination,
                            "supportsReasoning" to model.supportsReasoning.toString()
                        )
                    }
                }
            }
            if (cachedModels == null) {
                refreshAvailableModels()
            }

            (cachedModels ?: emptyList()).filter { model ->
                val destinationPath = File(directory, model["destination"].orEmpty())
                destinationPath.exists()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting available models", e)
            emptyList()
        }
    }
    
    override suspend fun loadModel(modelPath: String): Result<Unit> {
        return try {
            // Validate input parameters
            if (modelPath.isBlank()) {
                throw ValidationException("Model path cannot be blank")
            }
            
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                throw ModelNotFoundException(modelFile.name)
            }
            
            if (!modelFile.canRead()) {
                throw InvalidModelException(modelFile.name, Exception("File is not readable"))
            }
            
            Log.d(tag, "Loading model: $modelPath")
            llamaAndroid.load(
                modelPath,
                userThreads = 2, // Default thread count
                topK = 40, // Default top-k
                topP = 0.9f, // Default top-p
                temp = 0.7f // Default temperature
            )
            currentLoadedModel = modelFile.name
            Log.i(tag, "Successfully loaded model: $currentLoadedModel")
            Result.success(Unit)
        } catch (e: ModelNotFoundException) {
            Log.e(tag, "Model not found: $modelPath", e)
            Result.failure(e)
        } catch (e: InvalidModelException) {
            Log.e(tag, "Invalid model: $modelPath", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Error loading model: $modelPath", e)
            Result.failure(e)
        }
    }
    
    override suspend fun loadModelByName(modelName: String, directory: File): Result<Unit> {
        return try {
            // Validate input parameters
            if (modelName.isBlank()) {
                throw IllegalArgumentException("Model name cannot be blank")
            }
            
            if (!directory.exists()) {
                throw IllegalArgumentException("Directory does not exist: ${directory.absolutePath}")
            }
            
            if (!directory.isDirectory) {
                throw IllegalArgumentException("Path is not a directory: ${directory.absolutePath}")
            }
            
            if (!directory.canRead()) {
                throw IllegalArgumentException("Directory is not readable: ${directory.absolutePath}")
            }
            
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                loadModel(modelFile.absolutePath)
            } else {
                Log.e(tag, "Model file not found: ${modelFile.absolutePath}")
                Result.failure(Exception("Model file not found: $modelName"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading model by name: $modelName", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getLoadedModelName(): String {
        return currentLoadedModel
    }
    
    override suspend fun setLoadedModelName(modelName: String) {
        currentLoadedModel = modelName
    }

    override suspend fun getModelConfiguration(modelName: String): ModelConfiguration {
        return try {
            userPreferencesRepository.getModelConfiguration(modelName)
        } catch (e: Exception) {
            Log.e(tag, "Error getting model configuration for $modelName", e)
            ModelConfiguration()
        }
    }

    override suspend fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        try {
            userPreferencesRepository.saveModelConfiguration(modelName, config)
            Log.d(tag, "Saving configuration for model: $modelName")
        } catch (e: Exception) {
            Log.e(tag, "Error saving model configuration for $modelName", e)
        }
    }
    
    override suspend fun modelExists(modelName: String, directory: File): Boolean {
        return try {
            val modelFile = File(directory, modelName)
            modelFile.exists()
        } catch (e: Exception) {
            Log.e(tag, "Error checking if model exists: $modelName", e)
            false
        }
    }
    
    override suspend fun getModelFileSize(modelName: String, directory: File): Long {
        return try {
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                modelFile.length()
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting model file size: $modelName", e)
            0L
        }
    }
    
    override suspend fun deleteModel(modelName: String, directory: File): Result<Unit> {
        return try {
            val modelFile = File(directory, modelName)
            if (modelFile.exists()) {
                val deleted = modelFile.delete()
                if (deleted) {
                    Log.i(tag, "Successfully deleted model: $modelName")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete model file"))
                }
            } else {
                Result.failure(Exception("Model file not found"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting model: $modelName", e)
            Result.failure(e)
        }
    }
} 
