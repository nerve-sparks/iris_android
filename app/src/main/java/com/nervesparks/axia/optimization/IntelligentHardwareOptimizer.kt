package com.nervesparks.axia.optimization

import android.content.Context
import android.app.ActivityManager
import android.os.Build
import android.os.Debug
import android.hardware.Sensor
import android.hardware.SensorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.nervesparks.axia.models.*
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.*

/**
 * Intelligent Hardware Optimization for Axia
 * Automatically determines optimal model configuration based on device capabilities
 * 
 * Galaxy S24 Ultra Optimization:
 * - Snapdragon 8 Gen 3 AI acceleration
 * - 12GB RAM intelligent allocation
 * - Multiple small models vs single large model decisions
 * - Real-time performance adjustment
 */
class IntelligentHardwareOptimizer(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Hardware profile detection
    private val _hardwareProfile = MutableStateFlow<HardwareProfile?>(null)
    val hardwareProfile: StateFlow<HardwareProfile?> = _hardwareProfile.asStateFlow()
    
    private val _optimalConfiguration = MutableStateFlow<OptimalModelConfiguration?>(null)
    val optimalConfiguration: StateFlow<OptimalModelConfiguration?> = _optimalConfiguration.asStateFlow()
    
    private val _performanceMetrics = MutableStateFlow<PerformanceMetrics?>(null)
    val performanceMetrics: StateFlow<PerformanceMetrics?> = _performanceMetrics.asStateFlow()
    
    suspend fun initialize() {
        detectHardwareCapabilities()
        calculateOptimalConfiguration()
        startPerformanceMonitoring()
    }
    
    /**
     * Detect device hardware capabilities
     */
    private suspend fun detectHardwareCapabilities() = withContext(Dispatchers.IO) {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val profile = HardwareProfile(
            deviceModel = Build.MODEL,
            chipset = detectChipset(),
            totalRAM = getTotalRAM(),
            availableRAM = memInfo.availMem,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuFrequency = getCPUMaxFrequency(),
            hasNPU = hasNeuralProcessingUnit(),
            aiAcceleration = detectAIAcceleration(),
            thermalProfile = detectThermalProfile(),
            batteryCapacity = getBatteryCapacity(),
            storageType = detectStorageType(),
            androidVersion = Build.VERSION.SDK_INT
        )
        
        _hardwareProfile.value = profile
    }
    
    /**
     * Calculate optimal model configuration based on hardware
     */
    private suspend fun calculateOptimalConfiguration() {
        val hardware = _hardwareProfile.value ?: return
        
        val config = when {
            // Galaxy S24 Ultra or similar flagship
            hardware.totalRAM >= 12_000_000_000L && hardware.hasNPU -> {
                calculateFlagshipConfiguration(hardware)
            }
            // High-end device (8-12GB RAM)
            hardware.totalRAM >= 8_000_000_000L -> {
                calculateHighEndConfiguration(hardware)
            }
            // Mid-range device (6-8GB RAM)
            hardware.totalRAM >= 6_000_000_000L -> {
                calculateMidRangeConfiguration(hardware)
            }
            // Budget device (<6GB RAM)
            else -> {
                calculateBudgetConfiguration(hardware)
            }
        }
        
        _optimalConfiguration.value = config
    }
    
    private fun calculateFlagshipConfiguration(hardware: HardwareProfile): OptimalModelConfiguration {
        // Galaxy S24 Ultra optimal setup
        return OptimalModelConfiguration(
            strategy = ModelStrategy.FUSION_MULTI_MODEL,
            models = listOf(
                ModelSpec(
                    name = "code-specialist",
                    type = ModelType.CODE_GENERATION,
                    size = ModelSize.MEDIUM_3B,
                    ramAllocation = 3_000_000_000L, // 3GB
                    priority = ModelPriority.HIGH
                ),
                ModelSpec(
                    name = "general-assistant",
                    type = ModelType.GENERAL_CONVERSATION,
                    size = ModelSize.SMALL_1B,
                    ramAllocation = 1_500_000_000L, // 1.5GB
                    priority = ModelPriority.MEDIUM
                ),
                ModelSpec(
                    name = "reasoning-engine",
                    type = ModelType.COMPLEX_REASONING,
                    size = ModelSize.MEDIUM_3B,
                    ramAllocation = 3_000_000_000L, // 3GB
                    priority = ModelPriority.MEDIUM
                ),
                ModelSpec(
                    name = "research-analyzer",
                    type = ModelType.RESEARCH_SYNTHESIS,
                    size = ModelSize.SMALL_500M,
                    ramAllocation = 500_000_000L, // 500MB
                    priority = ModelPriority.LOW
                ),
                ModelSpec(
                    name = "quick-responder",
                    type = ModelType.QUICK_TASKS,
                    size = ModelSize.TINY_200M,
                    ramAllocation = 200_000_000L, // 200MB
                    priority = ModelPriority.HIGH
                )
            ),
            totalRAMUsage = 8_200_000_000L, // 8.2GB total
            swapUsage = 4_000_000_000L, // 4GB swap for background
            useAIAcceleration = true,
            enableModelFusion = true,
            dynamicLoading = true,
            thermalThrottling = true
        )
    }
    
    private fun calculateHighEndConfiguration(hardware: HardwareProfile): OptimalModelConfiguration {
        // 8-12GB RAM devices
        return OptimalModelConfiguration(
            strategy = ModelStrategy.DUAL_MODEL,
            models = listOf(
                ModelSpec(
                    name = "primary-model",
                    type = ModelType.GENERAL_CODE,
                    size = ModelSize.MEDIUM_3B,
                    ramAllocation = 4_000_000_000L, // 4GB
                    priority = ModelPriority.HIGH
                ),
                ModelSpec(
                    name = "quick-assistant",
                    type = ModelType.QUICK_TASKS,
                    size = ModelSize.SMALL_500M,
                    ramAllocation = 800_000_000L, // 800MB
                    priority = ModelPriority.MEDIUM
                )
            ),
            totalRAMUsage = 4_800_000_000L,
            swapUsage = 2_000_000_000L,
            useAIAcceleration = hardware.hasNPU,
            enableModelFusion = false,
            dynamicLoading = true,
            thermalThrottling = true
        )
    }
    
    private fun calculateMidRangeConfiguration(hardware: HardwareProfile): OptimalModelConfiguration {
        // 6-8GB RAM devices
        return OptimalModelConfiguration(
            strategy = ModelStrategy.SINGLE_OPTIMIZED,
            models = listOf(
                ModelSpec(
                    name = "unified-model",
                    type = ModelType.GENERAL_PURPOSE,
                    size = ModelSize.SMALL_1B,
                    ramAllocation = 2_500_000_000L, // 2.5GB
                    priority = ModelPriority.HIGH
                )
            ),
            totalRAMUsage = 2_500_000_000L,
            swapUsage = 1_000_000_000L,
            useAIAcceleration = false,
            enableModelFusion = false,
            dynamicLoading = true,
            thermalThrottling = true
        )
    }
    
    private fun calculateBudgetConfiguration(hardware: HardwareProfile): OptimalModelConfiguration {
        // <6GB RAM devices
        return OptimalModelConfiguration(
            strategy = ModelStrategy.LIGHTWEIGHT,
            models = listOf(
                ModelSpec(
                    name = "lightweight-model",
                    type = ModelType.BASIC_ASSISTANT,
                    size = ModelSize.TINY_200M,
                    ramAllocation = 1_000_000_000L, // 1GB
                    priority = ModelPriority.HIGH
                )
            ),
            totalRAMUsage = 1_000_000_000L,
            swapUsage = 500_000_000L,
            useAIAcceleration = false,
            enableModelFusion = false,
            dynamicLoading = false,
            thermalThrottling = true
        )
    }
    
    /**
     * Dynamic performance monitoring and adjustment
     */
    private fun startPerformanceMonitoring() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(5000) // Monitor every 5 seconds
                
                val metrics = collectPerformanceMetrics()
                _performanceMetrics.value = metrics
                
                // Adjust configuration if needed
                if (shouldAdjustConfiguration(metrics)) {
                    adjustConfiguration(metrics)
                }
            }
        }
    }
    
    private suspend fun collectPerformanceMetrics(): PerformanceMetrics {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        return PerformanceMetrics(
            cpuUsage = getCPUUsage(),
            ramUsage = (getTotalRAM() - memInfo.availMem).toDouble() / getTotalRAM(),
            temperature = getDeviceTemperature(),
            batteryLevel = getBatteryLevel(),
            thermalState = getThermalState(),
            aiProcessingLoad = getAIProcessingLoad(),
            modelSwitchCount = getModelSwitchCount(),
            averageResponseTime = getAverageResponseTime(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun shouldAdjustConfiguration(metrics: PerformanceMetrics): Boolean {
        return metrics.temperature > 65.0 || // Too hot
               metrics.ramUsage > 0.9 ||      // RAM pressure
               metrics.cpuUsage > 80.0 ||     // CPU overload
               metrics.averageResponseTime > 5000 // Too slow
    }
    
    private suspend fun adjustConfiguration(metrics: PerformanceMetrics) {
        val currentConfig = _optimalConfiguration.value ?: return
        val hardware = _hardwareProfile.value ?: return
        
        val adjustedConfig = when {
            metrics.temperature > 70.0 -> {
                // Thermal throttling - reduce to smaller models
                currentConfig.copy(
                    thermalThrottling = true,
                    models = currentConfig.models.map { model ->
                        model.copy(
                            size = when (model.size) {
                                ModelSize.LARGE_7B -> ModelSize.MEDIUM_3B
                                ModelSize.MEDIUM_3B -> ModelSize.SMALL_1B
                                ModelSize.SMALL_1B -> ModelSize.SMALL_500M
                                ModelSize.SMALL_500M -> ModelSize.TINY_200M
                                else -> model.size
                            }
                        )
                    }
                )
            }
            metrics.ramUsage > 0.85 -> {
                // RAM pressure - offload to swap
                currentConfig.copy(
                    swapUsage = currentConfig.swapUsage + 1_000_000_000L, // Add 1GB swap
                    models = currentConfig.models.map { model ->
                        if (model.priority == ModelPriority.LOW) {
                            model.copy(ramAllocation = model.ramAllocation / 2)
                        } else model
                    }
                )
            }
            metrics.averageResponseTime > 3000 -> {
                // Performance issue - switch to faster model
                if (currentConfig.strategy != ModelStrategy.FUSION_MULTI_MODEL) {
                    calculateFlagshipConfiguration(hardware)
                } else {
                    currentConfig
                }
            }
            else -> currentConfig
        }
        
        _optimalConfiguration.value = adjustedConfig
    }
    
    // Hardware detection utilities
    private fun detectChipset(): String {
        return when {
            Build.HARDWARE.contains("qcom") -> "Qualcomm Snapdragon"
            Build.HARDWARE.contains("exynos") -> "Samsung Exynos"
            Build.HARDWARE.contains("mt") -> "MediaTek"
            else -> "Unknown"
        }
    }
    
    private fun getTotalRAM(): Long {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (e: Exception) {
            4_000_000_000L // Default 4GB
        }
    }
    
    private fun getCPUMaxFrequency(): Long {
        return try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (file.exists()) {
                file.readText().trim().toLong() * 1000 // Convert to Hz
            } else {
                2_000_000_000L // Default 2GHz
            }
        } catch (e: Exception) {
            2_000_000_000L
        }
    }
    
    private fun hasNeuralProcessingUnit(): Boolean {
        return when {
            Build.MODEL.contains("SM-S928") -> true // S24 Ultra
            Build.MODEL.contains("SM-S926") -> true // S24+
            Build.MODEL.contains("SM-S921") -> true // S24
            detectChipset().contains("Snapdragon") && Build.VERSION.SDK_INT >= 29 -> true
            else -> false
        }
    }
    
    private fun detectAIAcceleration(): AIAcceleration {
        return when {
            hasNeuralProcessingUnit() -> AIAcceleration.NPU
            Build.VERSION.SDK_INT >= 27 -> AIAcceleration.NNAPI
            else -> AIAcceleration.NONE
        }
    }
    
    private fun detectThermalProfile(): ThermalProfile {
        return when {
            Build.MODEL.contains("SM-S928") -> ThermalProfile.FLAGSHIP // S24 Ultra
            getTotalRAM() > 8_000_000_000L -> ThermalProfile.HIGH_END
            getTotalRAM() > 6_000_000_000L -> ThermalProfile.MID_RANGE
            else -> ThermalProfile.BUDGET
        }
    }
    
    private fun getBatteryCapacity(): Int {
        // This would integrate with BatteryManager for actual capacity
        return when {
            Build.MODEL.contains("SM-S928") -> 5000 // S24 Ultra
            else -> 4000 // Default
        }
    }
    
    private fun detectStorageType(): StorageType {
        return if (Build.VERSION.SDK_INT >= 26) {
            StorageType.UFS_3_1 // Modern Android devices
        } else {
            StorageType.EMMC
        }
    }
    
    // Performance monitoring utilities
    private fun getCPUUsage(): Double {
        return try {
            val file = RandomAccessFile("/proc/stat", "r")
            val line = file.readLine()
            file.close()
            
            val parts = line.split("\\s+".toRegex())
            val idle = parts[4].toLong()
            val total = parts.drop(1).take(4).sumOf { it.toLong() }
            
            ((total - idle).toDouble() / total) * 100
        } catch (e: Exception) {
            50.0 // Default
        }
    }
    
    private fun getDeviceTemperature(): Double {
        return try {
            val tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            // This would need sensor listener implementation
            35.0 // Placeholder
        } catch (e: Exception) {
            35.0
        }
    }
    
    private fun getBatteryLevel(): Int {
        // This would integrate with BatteryManager
        return 85 // Placeholder
    }
    
    private fun getThermalState(): ThermalState {
        return when {
            getDeviceTemperature() > 70 -> ThermalState.CRITICAL
            getDeviceTemperature() > 60 -> ThermalState.SEVERE
            getDeviceTemperature() > 45 -> ThermalState.MODERATE
            else -> ThermalState.NORMAL
        }
    }
    
    private fun getAIProcessingLoad(): Double = 0.0 // Placeholder
    private fun getModelSwitchCount(): Int = 0 // Placeholder
    private fun getAverageResponseTime(): Long = 1500L // Placeholder
    
    /**
     * Get available RAM for model loading
     */
    fun getAvailableRAM(): Long {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxHeapSize()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        
        // Calculate available memory
        val availableHeapMemory = maxMemory - (totalMemory - freeMemory)
        
        // Get system memory info
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        // Available system RAM (considering Android's memory management)
        val availableSystemMemory = memInfo.availMem
        
        // Use conservative estimate (60% of available system memory for model loading)
        val conservativeEstimate = (availableSystemMemory * 0.6).toLong()
        
        return minOf(availableHeapMemory, conservativeEstimate)
    }
    
    /**
     * Optimize memory for specific model loading
     */
    suspend fun optimizeMemoryForModel(requiredMemory: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Force garbage collection
                System.gc()
                
                // Clear model cache if needed
                clearModelCache()
                
                // Reduce background processes
                reduceBackgroundProcesses()
                
                // Check if enough memory is now available
                val availableMemory = getAvailableRAM()
                
                if (availableMemory >= requiredMemory) {
                    true
                } else {
                    // Try aggressive optimization
                    aggressiveMemoryOptimization()
                    getAvailableRAM() >= requiredMemory
                }
                
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Generate response with local model (placeholder for actual implementation)
     */
    suspend fun generateWithLocalModel(
        prompt: String, 
        maxTokens: Int, 
        temperature: Float
    ): String {
        return withContext(Dispatchers.Default) {
            // This would integrate with your actual LLaMA implementation
            // For now, return a placeholder response
            try {
                // Check if we have enough resources
                val cpuUsage = getCurrentCPUUsage()
                val memoryUsage = getCurrentMemoryUsage()
                val temperature_celsius = getCurrentTemperature()
                
                if (cpuUsage > 90 || memoryUsage > 85 || temperature_celsius > 80) {
                    // Throttle generation for device protection
                    delay(1000)
                }
                
                // Simulate local model generation
                // In real implementation, this would call LLamaAndroid
                "Generated response using local model with optimized parameters. " +
                "CPU: ${cpuUsage}%, Memory: ${memoryUsage}%, Temp: ${temperature_celsius}°C"
                
            } catch (e: Exception) {
                "Error generating with local model: ${e.message}"
            }
        }
    }
    
    /**
     * Get optimal model configuration based on current hardware state
     */
    suspend fun getOptimalModelConfiguration(): ModelConfiguration {
        val availableRAM = getAvailableRAM()
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val temperature = getCurrentTemperature()
        val batteryLevel = getCurrentBatteryLevel()
        
        return when {
            // High-end configuration (Galaxy S24 Ultra optimal)
            availableRAM >= 6L * 1024 * 1024 * 1024 && temperature < 70 && batteryLevel > 30 -> {
                ModelConfiguration(
                    modelSize = ModelSize.MEDIUM_3B,
                    contextWindow = 4096,
                    maxTokens = 2048,
                    threads = minOf(cpuCores, 6),
                    batchSize = 8,
                    useGPU = true,
                    precisionMode = PrecisionMode.FP16,
                    description = "High-performance configuration for optimal experience"
                )
            }
            
            // Balanced configuration
            availableRAM >= 3L * 1024 * 1024 * 1024 && temperature < 75 && batteryLevel > 20 -> {
                ModelConfiguration(
                    modelSize = ModelSize.SMALL_1B,
                    contextWindow = 2048,
                    maxTokens = 1024,
                    threads = minOf(cpuCores, 4),
                    batchSize = 4,
                    useGPU = true,
                    precisionMode = PrecisionMode.FP16,
                    description = "Balanced configuration for good performance and efficiency"
                )
            }
            
            // Efficiency configuration
            availableRAM >= 1L * 1024 * 1024 * 1024 -> {
                ModelConfiguration(
                    modelSize = ModelSize.SMALL_500M,
                    contextWindow = 1024,
                    maxTokens = 512,
                    threads = minOf(cpuCores, 2),
                    batchSize = 2,
                    useGPU = false,
                    precisionMode = PrecisionMode.INT8,
                    description = "Efficiency configuration for resource-constrained scenarios"
                )
            }
            
            // Minimal configuration
            else -> {
                ModelConfiguration(
                    modelSize = ModelSize.TINY_200M,
                    contextWindow = 512,
                    maxTokens = 256,
                    threads = 1,
                    batchSize = 1,
                    useGPU = false,
                    precisionMode = PrecisionMode.INT4,
                    description = "Minimal configuration for basic functionality"
                )
            }
        }
    }
    
    /**
     * Monitor and adjust model performance in real-time
     */
    suspend fun monitorAndAdjustPerformance() {
        while (true) {
            delay(5000) // Check every 5 seconds
            
            val currentTemp = getCurrentTemperature()
            val currentMemory = getCurrentMemoryUsage()
            val currentCPU = getCurrentCPUUsage()
            val batteryLevel = getCurrentBatteryLevel()
            
            // Thermal throttling
            if (currentTemp > 85) {
                reduceModelPerformance("High temperature detected")
            } else if (currentTemp > 80) {
                moderateModelPerformance("Elevated temperature")
            } else if (currentTemp < 70) {
                restoreModelPerformance("Temperature normal")
            }
            
            // Memory pressure handling
            if (currentMemory > 90) {
                aggressiveMemoryOptimization()
            } else if (currentMemory > 80) {
                moderateMemoryOptimization()
            }
            
            // Battery optimization
            if (batteryLevel < 15) {
                enablePowerSavingMode()
            } else if (batteryLevel < 30) {
                enableBatteryOptimizedMode()
            }
            
            // Update optimization metrics
            updateOptimizationMetrics()
        }
    }
    
    // Private helper methods for new functionality
    private fun clearModelCache() {
        // Clear any cached models to free memory
    }
    
    private fun reduceBackgroundProcesses() {
        // Reduce non-essential background processes
    }
    
    private fun aggressiveMemoryOptimization() {
        System.gc()
        System.runFinalization()
        // Additional aggressive memory cleanup
    }
    
    private fun moderateMemoryOptimization() {
        System.gc()
        // Moderate memory cleanup
    }
    
    private fun reduceModelPerformance(reason: String) {
        // Reduce model inference speed/quality to protect hardware
    }
    
    private fun moderateModelPerformance(reason: String) {
        // Moderate performance reduction
    }
    
    private fun restoreModelPerformance(reason: String) {
        // Restore full performance
    }
    
    private fun enablePowerSavingMode() {
        // Enable aggressive power saving
    }
    
    private fun enableBatteryOptimizedMode() {
        // Enable battery optimized mode
    }
    
    private fun updateOptimizationMetrics() {
        // Update metrics
    }
}

// Additional data classes for new functionality
data class ModelConfiguration(
    val modelSize: ModelSize,
    val contextWindow: Int,
    val maxTokens: Int,
    val threads: Int,
    val batchSize: Int,
    val useGPU: Boolean,
    val precisionMode: PrecisionMode,
    val description: String
)

enum class PrecisionMode {
    FP32, FP16, INT8, INT4
}
    val deviceModel: String,
    val chipset: String,
    val totalRAM: Long,
    val availableRAM: Long,
    val cpuCores: Int,
    val cpuFrequency: Long,
    val hasNPU: Boolean,
    val aiAcceleration: AIAcceleration,
    val thermalProfile: ThermalProfile,
    val batteryCapacity: Int,
    val storageType: StorageType,
    val androidVersion: Int
)

data class OptimalModelConfiguration(
    val strategy: ModelStrategy,
    val models: List<ModelSpec>,
    val totalRAMUsage: Long,
    val swapUsage: Long,
    val useAIAcceleration: Boolean,
    val enableModelFusion: Boolean,
    val dynamicLoading: Boolean,
    val thermalThrottling: Boolean
)

data class ModelSpec(
    val name: String,
    val type: ModelType,
    val size: ModelSize,
    val ramAllocation: Long,
    val priority: ModelPriority
)

data class PerformanceMetrics(
    val cpuUsage: Double,
    val ramUsage: Double,
    val temperature: Double,
    val batteryLevel: Int,
    val thermalState: ThermalState,
    val aiProcessingLoad: Double,
    val modelSwitchCount: Int,
    val averageResponseTime: Long,
    val timestamp: Long
)

enum class ModelStrategy {
    FUSION_MULTI_MODEL,    // Multiple specialized models working together
    DUAL_MODEL,            // Primary + quick assistant
    SINGLE_OPTIMIZED,      // One optimized model
    LIGHTWEIGHT            // Minimal resource usage
}

enum class ModelType {
    CODE_GENERATION,
    GENERAL_CONVERSATION,
    COMPLEX_REASONING,
    RESEARCH_SYNTHESIS,
    QUICK_TASKS,
    GENERAL_CODE,
    GENERAL_PURPOSE,
    BASIC_ASSISTANT
}

enum class ModelSize {
    LARGE_7B,      // 7+ billion parameters
    MEDIUM_3B,     // 3 billion parameters  
    SMALL_1B,      // 1 billion parameters
    SMALL_500M,    // 500 million parameters
    TINY_200M      // 200 million parameters
}

enum class ModelPriority {
    HIGH, MEDIUM, LOW
}

enum class AIAcceleration {
    NPU,      // Neural Processing Unit
    NNAPI,    // Android Neural Networks API
    NONE      // CPU only
}

enum class ThermalProfile {
    FLAGSHIP,   // Excellent cooling
    HIGH_END,   // Good cooling
    MID_RANGE,  // Average cooling
    BUDGET      // Limited cooling
}

enum class StorageType {
    UFS_3_1,    // Modern fast storage
    UFS_2_1,    // Fast storage
    EMMC        // Older storage
}

enum class ThermalState {
    NORMAL,     // <45°C
    MODERATE,   // 45-60°C
    SEVERE,     // 60-70°C
    CRITICAL    // >70°C
}