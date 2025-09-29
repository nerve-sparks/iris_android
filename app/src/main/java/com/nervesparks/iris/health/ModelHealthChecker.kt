package com.nervesparks.iris.health

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Model health check system for GGUF files and MLC models
 * Provides friendly error messages and model validation
 */

data class ModelHealthStatus(
    val isHealthy: Boolean,
    val modelType: ModelType,
    val modelName: String,
    val version: String,
    val architecture: String,
    val parameterCount: String,
    val quantization: String,
    val contextLength: Int,
    val issues: List<HealthIssue>,
    val lastChecked: Long = System.currentTimeMillis()
)

data class HealthIssue(
    val severity: IssueSeverity,
    val code: String,
    val message: String,
    val suggestion: String
)

enum class IssueSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

enum class ModelType {
    GGUF, MLC, UNKNOWN
}

class ModelHealthChecker(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelHealthChecker"
        private const val GGUF_MAGIC = 0x46554747 // "GGUF" in little-endian
        private const val MLC_CONFIG_FILE = "mlc-chat-config.json"
        private const val HEALTH_CHECK_TIMEOUT = 10000L // 10 seconds
    }
    
    private val _healthStatus = MutableStateFlow<ModelHealthStatus?>(null)
    val healthStatus: StateFlow<ModelHealthStatus?> = _healthStatus.asStateFlow()
    
    /**
     * Perform comprehensive health check on a model file
     */
    suspend fun checkModelHealth(modelPath: String): ModelHealthStatus {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(modelPath)
                if (!file.exists()) {
                    return@withContext createErrorStatus(
                        "MODEL_NOT_FOUND",
                        "Model file not found: ${file.name}",
                        "Please ensure the model file exists and is accessible."
                    )
                }
                
                if (!file.canRead()) {
                    return@withContext createErrorStatus(
                        "MODEL_NO_READ_PERMISSION",
                        "Cannot read model file: ${file.name}",
                        "Check file permissions and storage access."
                    )
                }
                
                val modelType = detectModelType(file)
                when (modelType) {
                    ModelType.GGUF -> checkGGUFModel(file)
                    ModelType.MLC -> checkMLCModel(file)
                    ModelType.UNKNOWN -> createErrorStatus(
                        "MODEL_TYPE_UNKNOWN",
                        "Unknown model format: ${file.name}",
                        "Supported formats: GGUF (.gguf), MLC (.mlc)"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking model health", e)
                createErrorStatus(
                    "HEALTH_CHECK_FAILED",
                    "Failed to check model health: ${e.message}",
                    "Try restarting the app or re-downloading the model."
                )
            }
        }.also { status ->
            _healthStatus.value = status
        }
    }
    
    /**
     * Check GGUF model health
     */
    private suspend fun checkGGUFModel(file: File): ModelHealthStatus {
        return withTimeout(HEALTH_CHECK_TIMEOUT) {
            val issues = mutableListOf<HealthIssue>()
            var modelName = file.nameWithoutExtension
            var version = "Unknown"
            var architecture = "Unknown"
            var parameterCount = "Unknown"
            var quantization = "Unknown"
            var contextLength = 2048
            
            RandomAccessFile(file, "r").use { raf ->
                // Check GGUF magic number
                val magic = raf.readInt()
                if (Integer.reverseBytes(magic) != GGUF_MAGIC) {
                    issues.add(HealthIssue(
                        IssueSeverity.CRITICAL,
                        "INVALID_GGUF_MAGIC",
                        "Invalid GGUF file format",
                        "File may be corrupted. Re-download the model."
                    ))
                    return@withTimeout createHealthStatus(false, ModelType.GGUF, modelName, version, 
                        architecture, parameterCount, quantization, contextLength, issues)
                }
                
                // Read GGUF version
                val ggufVersion = Integer.reverseBytes(raf.readInt())
                if (ggufVersion < 2) {
                    issues.add(HealthIssue(
                        IssueSeverity.WARNING,
                        "OLD_GGUF_VERSION",
                        "Old GGUF version: $ggufVersion",
                        "Consider using GGUF v3 or later for better performance."
                    ))
                }
                
                // Read tensor count and metadata count
                val tensorCount = java.lang.Long.reverseBytes(raf.readLong())
                val metadataCount = java.lang.Long.reverseBytes(raf.readLong())
                
                if (tensorCount <= 0) {
                    issues.add(HealthIssue(
                        IssueSeverity.CRITICAL,
                        "NO_TENSORS",
                        "Model has no tensors",
                        "Model file may be corrupted."
                    ))
                }
                
                // Try to extract model metadata
                try {
                    // This is a simplified metadata extraction
                    // In production, you'd need proper GGUF parsing
                    modelName = extractModelName(file.name)
                    parameterCount = estimateParameterCount(file.length())
                    quantization = extractQuantization(file.name)
                    contextLength = estimateContextLength(file.length())
                    
                } catch (e: Exception) {
                    issues.add(HealthIssue(
                        IssueSeverity.WARNING,
                        "METADATA_READ_ERROR",
                        "Could not read model metadata",
                        "Model may still work but some features may be limited."
                    ))
                }
            }
            
            // File size validation
            val fileSizeMB = file.length() / (1024 * 1024)
            if (fileSizeMB < 100) {
                issues.add(HealthIssue(
                    IssueSeverity.WARNING,
                    "SMALL_MODEL_SIZE",
                    "Model size is unusually small: ${fileSizeMB}MB",
                    "Very small models may have limited capabilities."
                ))
            }
            
            createHealthStatus(
                issues.none { it.severity == IssueSeverity.CRITICAL },
                ModelType.GGUF,
                modelName,
                version,
                architecture,
                parameterCount,
                quantization,
                contextLength,
                issues
            )
        }
    }
    
    /**
     * Check MLC model health
     */
    private suspend fun checkMLCModel(file: File): ModelHealthStatus {
        val issues = mutableListOf<HealthIssue>()
        var modelName = file.nameWithoutExtension
        var version = "Unknown"
        var architecture = "Unknown"
        var parameterCount = "Unknown"
        var quantization = "Unknown"
        var contextLength = 2048
        
        // Check for MLC config file
        val configFile = File(file.parent, MLC_CONFIG_FILE)
        if (!configFile.exists()) {
            issues.add(HealthIssue(
                IssueSeverity.ERROR,
                "MLC_CONFIG_MISSING",
                "MLC configuration file missing",
                "Ensure mlc-chat-config.json is present with the model."
            ))
        } else {
            try {
                val configContent = configFile.readText()
                // Parse basic config info
                if (configContent.contains("conv_template")) {
                    // Extract conv_template and other metadata
                    modelName = extractFromConfig(configContent, "model_name", modelName)
                    architecture = extractFromConfig(configContent, "model_type", architecture)
                } else {
                    issues.add(HealthIssue(
                        IssueSeverity.WARNING,
                        "MISSING_CONV_TEMPLATE",
                        "Missing conversation template",
                        "Model may not format conversations correctly."
                    ))
                }
            } catch (e: Exception) {
                issues.add(HealthIssue(
                    IssueSeverity.ERROR,
                    "CONFIG_PARSE_ERROR",
                    "Could not parse MLC config",
                    "Check if the configuration file is valid JSON."
                ))
            }
        }
        
        return createHealthStatus(
            issues.none { it.severity == IssueSeverity.CRITICAL },
            ModelType.MLC,
            modelName,
            version,
            architecture,
            parameterCount,
            quantization,
            contextLength,
            issues
        )
    }
    
    private fun detectModelType(file: File): ModelType {
        return when {
            file.extension.lowercase() == "gguf" -> ModelType.GGUF
            file.extension.lowercase() == "mlc" || 
            File(file.parent, MLC_CONFIG_FILE).exists() -> ModelType.MLC
            else -> ModelType.UNKNOWN
        }
    }
    
    private fun extractModelName(fileName: String): String {
        // Extract model name from common naming patterns
        return fileName.split("-").first().split("_").first().split(".").first()
    }
    
    private fun estimateParameterCount(fileSize: Long): String {
        val sizeMB = fileSize / (1024 * 1024)
        return when {
            sizeMB < 500 -> "~1B parameters"
            sizeMB < 2000 -> "~3B parameters"
            sizeMB < 4000 -> "~7B parameters"
            sizeMB < 8000 -> "~13B parameters"
            sizeMB < 16000 -> "~30B parameters"
            else -> "~70B+ parameters"
        }
    }
    
    private fun extractQuantization(fileName: String): String {
        return when {
            fileName.contains("q4_0", ignoreCase = true) -> "Q4_0"
            fileName.contains("q4_1", ignoreCase = true) -> "Q4_1"
            fileName.contains("q5_0", ignoreCase = true) -> "Q5_0"
            fileName.contains("q5_1", ignoreCase = true) -> "Q5_1"
            fileName.contains("q8_0", ignoreCase = true) -> "Q8_0"
            fileName.contains("f16", ignoreCase = true) -> "F16"
            fileName.contains("f32", ignoreCase = true) -> "F32"
            else -> "Unknown"
        }
    }
    
    private fun estimateContextLength(fileSize: Long): Int {
        val sizeMB = fileSize / (1024 * 1024)
        return when {
            sizeMB < 1000 -> 2048
            sizeMB < 4000 -> 4096
            sizeMB < 8000 -> 8192
            else -> 16384
        }
    }
    
    private fun extractFromConfig(configContent: String, key: String, defaultValue: String): String {
        return try {
            val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            regex.find(configContent)?.groupValues?.get(1) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    private fun createHealthStatus(
        isHealthy: Boolean,
        modelType: ModelType,
        modelName: String,
        version: String,
        architecture: String,
        parameterCount: String,
        quantization: String,
        contextLength: Int,
        issues: List<HealthIssue>
    ): ModelHealthStatus {
        return ModelHealthStatus(
            isHealthy = isHealthy,
            modelType = modelType,
            modelName = modelName,
            version = version,
            architecture = architecture,
            parameterCount = parameterCount,
            quantization = quantization,
            contextLength = contextLength,
            issues = issues
        )
    }
    
    private fun createErrorStatus(code: String, message: String, suggestion: String): ModelHealthStatus {
        return ModelHealthStatus(
            isHealthy = false,
            modelType = ModelType.UNKNOWN,
            modelName = "Unknown",
            version = "Unknown",
            architecture = "Unknown",
            parameterCount = "Unknown",
            quantization = "Unknown",
            contextLength = 0,
            issues = listOf(HealthIssue(IssueSeverity.CRITICAL, code, message, suggestion))
        )
    }
}

@Composable
fun rememberModelHealthChecker(context: Context): ModelHealthChecker {
    return remember {
        ModelHealthChecker(context)
    }
}