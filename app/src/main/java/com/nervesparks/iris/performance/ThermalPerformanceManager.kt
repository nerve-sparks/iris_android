package com.nervesparks.iris.performance

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Thermal management and sustained performance for Samsung Galaxy S24 Ultra
 * Auto-adjusts threads and performance during long generations
 */

class ThermalPerformanceManager(
    private val context: Context,
    private val activity: Activity
) {
    companion object {
        private const val TAG = "ThermalPerformance"
        private const val THERMAL_THROTTLE_TEMP = 42.0f // Celsius
        private const val CRITICAL_TEMP = 48.0f
        private const val THERMAL_CHECK_INTERVAL = 2000L // 2 seconds
        
        // Samsung S24 Ultra optimized thread limits
        private const val MAX_THREADS_NORMAL = 8
        private const val MAX_THREADS_WARM = 6
        private const val MAX_THREADS_HOT = 4
        private const val MAX_THREADS_CRITICAL = 2
    }
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var sustainedPerformanceSession: Any? = null
    private var thermalMonitorJob: Job? = null
    
    private val _thermalState = MutableStateFlow(ThermalState())
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    data class ThermalState(
        val temperature: Float = 0f,
        val batteryLevel: Int = 100,
        val isThrottling: Boolean = false,
        val recommendedThreads: Int = MAX_THREADS_NORMAL,
        val performanceMode: PerformanceMode = PerformanceMode.NORMAL
    )
    
    data class PerformanceMetrics(
        val tokensPerSecond: Double = 0.0,
        val batteryPercent: Int = 100,
        val temperature: Float = 0f,
        val cpuUsage: Float = 0f,
        val memoryUsage: Long = 0L,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class PerformanceMode {
        NORMAL, POWER_SAVE, SUSTAINED, THROTTLED
    }
    
    /**
     * Enable sustained performance mode for long generations
     */
    fun enableSustainedPerformance() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (activity.window != null) {
                    activity.window.setSustainedPerformanceMode(true)
                    Log.d(TAG, "Sustained performance mode enabled")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not enable sustained performance mode", e)
        }
    }
    
    /**
     * Disable sustained performance mode
     */
    fun disableSustainedPerformance() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (activity.window != null) {
                    activity.window.setSustainedPerformanceMode(false)
                    Log.d(TAG, "Sustained performance mode disabled")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not disable sustained performance mode", e)
        }
    }
    
    /**
     * Start thermal monitoring
     */
    fun startThermalMonitoring() {
        thermalMonitorJob?.cancel()
        thermalMonitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                updateThermalState()
                updatePerformanceMetrics()
                delay(THERMAL_CHECK_INTERVAL)
            }
        }
        Log.d(TAG, "Thermal monitoring started")
    }
    
    /**
     * Stop thermal monitoring
     */
    fun stopThermalMonitoring() {
        thermalMonitorJob?.cancel()
        thermalMonitorJob = null
        Log.d(TAG, "Thermal monitoring stopped")
    }
    
    private suspend fun updateThermalState() {
        try {
            val temperature = getCpuTemperature()
            val batteryLevel = getBatteryLevel()
            
            val recommendedThreads = when {
                temperature >= CRITICAL_TEMP -> MAX_THREADS_CRITICAL
                temperature >= THERMAL_THROTTLE_TEMP + 4 -> MAX_THREADS_HOT
                temperature >= THERMAL_THROTTLE_TEMP -> MAX_THREADS_WARM
                else -> MAX_THREADS_NORMAL
            }
            
            val performanceMode = when {
                temperature >= CRITICAL_TEMP -> PerformanceMode.THROTTLED
                temperature >= THERMAL_THROTTLE_TEMP -> PerformanceMode.POWER_SAVE
                batteryLevel < 20 -> PerformanceMode.POWER_SAVE
                else -> PerformanceMode.NORMAL
            }
            
            val isThrottling = temperature >= THERMAL_THROTTLE_TEMP
            
            _thermalState.value = ThermalState(
                temperature = temperature,
                batteryLevel = batteryLevel,
                isThrottling = isThrottling,
                recommendedThreads = recommendedThreads,
                performanceMode = performanceMode
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "Error updating thermal state", e)
        }
    }
    
    private suspend fun updatePerformanceMetrics() {
        withContext(Dispatchers.IO) {
            try {
                val metrics = PerformanceMetrics(
                    tokensPerSecond = getCurrentTokenRate(),
                    batteryPercent = getBatteryLevel(),
                    temperature = getCpuTemperature(),
                    cpuUsage = getCpuUsage(),
                    memoryUsage = getMemoryUsage(),
                    timestamp = System.currentTimeMillis()
                )
                
                _performanceMetrics.value = metrics
            } catch (e: Exception) {
                Log.w(TAG, "Error updating performance metrics", e)
            }
        }
    }
    
    private fun getCpuTemperature(): Float {
        return try {
            // Try multiple thermal zone files (Samsung specific paths)
            val thermalPaths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp", 
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/virtual/thermal/thermal_zone1/temp"
            )
            
            for (path in thermalPaths) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val tempStr = file.readText().trim()
                    val temp = tempStr.toFloat()
                    // Convert from millidegrees to degrees Celsius
                    return if (temp > 1000) temp / 1000f else temp
                }
            }
            25.0f // Default room temperature
        } catch (e: Exception) {
            Log.w(TAG, "Could not read CPU temperature", e)
            25.0f
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            val batteryFile = File("/sys/class/power_supply/battery/capacity")
            if (batteryFile.exists() && batteryFile.canRead()) {
                batteryFile.readText().trim().toInt()
            } else {
                100 // Default full battery
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read battery level", e)
            100
        }
    }
    
    private fun getCpuUsage(): Float {
        return try {
            val statFile = File("/proc/stat")
            if (statFile.exists() && statFile.canRead()) {
                val lines = statFile.readLines()
                if (lines.isNotEmpty()) {
                    val cpuLine = lines[0]
                    val values = cpuLine.split(" ").drop(2).map { it.toLongOrNull() ?: 0L }
                    if (values.size >= 4) {
                        val idle = values[3]
                        val total = values.sum()
                        return if (total > 0) ((total - idle) * 100.0f / total) else 0f
                    }
                }
            }
            0f
        } catch (e: Exception) {
            Log.w(TAG, "Could not read CPU usage", e)
            0f
        }
    }
    
    private fun getMemoryUsage(): Long {
        return try {
            val memInfoFile = File("/proc/meminfo")
            if (memInfoFile.exists() && memInfoFile.canRead()) {
                val lines = memInfoFile.readLines()
                val totalMem = lines.find { it.startsWith("MemTotal:") }
                    ?.split("\\s+".toRegex())?.get(1)?.toLong() ?: 0L
                val availableMem = lines.find { it.startsWith("MemAvailable:") }
                    ?.split("\\s+".toRegex())?.get(1)?.toLong() ?: 0L
                totalMem - availableMem
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read memory usage", e)
            0L
        }
    }
    
    private fun getCurrentTokenRate(): Double {
        // This would be updated by the streaming token manager
        return 0.0
    }
    
    fun updateTokenRate(tokensPerSecond: Double) {
        val current = _performanceMetrics.value
        _performanceMetrics.value = current.copy(
            tokensPerSecond = tokensPerSecond,
            timestamp = System.currentTimeMillis()
        )
    }
}

@Composable
fun rememberThermalPerformanceManager(
    context: Context,
    activity: Activity
): ThermalPerformanceManager {
    return remember {
        ThermalPerformanceManager(context, activity)
    }
}