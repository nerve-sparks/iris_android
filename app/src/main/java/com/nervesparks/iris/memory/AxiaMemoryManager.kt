package com.nervesparks.iris.memory

import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced Memory Management System for Axia
 * Handles RAM/Swap optimization with AI-first priority
 */

@Singleton
class AxiaMemoryManager @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Memory tracking
    private val _memoryStatus = MutableStateFlow(MemoryStatus())
    val memoryStatus: StateFlow<MemoryStatus> = _memoryStatus.asStateFlow()
    
    // AI Core (Always in RAM)
    private val aiCoreMemory = ConcurrentHashMap<String, Any>()
    
    // Swap Bridge for non-critical functions
    private val swapBridge = SwapBridge(context)
    
    // Memory zones
    private val criticalMemory = mutableSetOf<String>()
    private val swapableMemory = mutableSetOf<String>()
    
    init {
        startMemoryMonitoring()
        initializeCriticalMemory()
    }
    
    private fun initializeCriticalMemory() {
        // AI Core components that MUST stay in RAM
        criticalMemory.addAll(listOf(
            "ai_model",
            "context_window", 
            "active_conversation",
            "token_processor",
            "inference_engine",
            "memory_context",
            "user_preferences"
        ))
        
        // Components that can use swap
        swapableMemory.addAll(listOf(
            "knowledge_base",
            "file_operations",
            "build_tools",
            "project_templates",
            "download_cache",
            "search_index",
            "log_history"
        ))
    }
    
    private fun startMemoryMonitoring() {
        scope.launch {
            while (true) {
                updateMemoryStatus()
                optimizeMemoryAllocation()
                delay(1000) // Update every second
            }
        }
    }
    
    private suspend fun updateMemoryStatus() {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalRAM = getTotalRAM()
        val availableRAM = memInfo.availMem
        val usedRAM = totalRAM - availableRAM
        val swapInfo = getSwapInfo()
        
        val status = MemoryStatus(
            totalRAM = totalRAM,
            usedRAM = usedRAM,
            availableRAM = availableRAM,
            totalSwap = swapInfo.total,
            usedSwap = swapInfo.used,
            availableSwap = swapInfo.available,
            aiMemoryUsage = getAICoreMemoryUsage(),
            swapBridgeUsage = swapBridge.getUsage(),
            memoryPressure = calculateMemoryPressure(availableRAM, totalRAM),
            isLowMemory = memInfo.lowMemory,
            threshold = memInfo.threshold
        )
        
        _memoryStatus.value = status
    }
    
    private suspend fun optimizeMemoryAllocation() {
        val status = _memoryStatus.value
        
        when {
            status.memoryPressure > 0.8 -> {
                // Critical memory pressure - move more to swap
                moveToSwap(SwapPriority.LOW_PRIORITY)
                triggerGarbageCollection()
            }
            status.memoryPressure > 0.6 -> {
                // Moderate pressure - selective swap
                moveToSwap(SwapPriority.MEDIUM_PRIORITY)
            }
            status.memoryPressure < 0.3 -> {
                // Plenty of RAM - can move things back
                moveFromSwap(SwapPriority.HIGH_PRIORITY)
            }
        }
    }
    
    /**
     * Store data with intelligent RAM/Swap allocation
     */
    suspend fun store(key: String, data: Any, priority: MemoryPriority = MemoryPriority.NORMAL): Boolean {
        return when (priority) {
            MemoryPriority.CRITICAL -> {
                // AI Core - always in RAM
                aiCoreMemory[key] = data
                criticalMemory.add(key)
                true
            }
            MemoryPriority.HIGH -> {
                // Prefer RAM but can swap if needed
                if (_memoryStatus.value.memoryPressure < 0.7) {
                    aiCoreMemory[key] = data
                    true
                } else {
                    swapBridge.store(key, data)
                }
            }
            MemoryPriority.NORMAL, MemoryPriority.LOW -> {
                // Use swap bridge
                swapBridge.store(key, data)
            }
        }
    }
    
    /**
     * Retrieve data from RAM or Swap
     */
    suspend fun retrieve(key: String): Any? {
        return aiCoreMemory[key] ?: swapBridge.retrieve(key)
    }
    
    /**
     * Force data to stay in RAM (AI Core protection)
     */
    suspend fun lockInRAM(key: String): Boolean {
        val data = retrieve(key) ?: return false
        aiCoreMemory[key] = data
        criticalMemory.add(key)
        swapBridge.remove(key)
        return true
    }
    
    /**
     * Move specific data to swap
     */
    suspend fun moveToSwap(key: String): Boolean {
        val data = aiCoreMemory[key] ?: return false
        if (key in criticalMemory) return false // Protect critical memory
        
        val stored = swapBridge.store(key, data)
        if (stored) {
            aiCoreMemory.remove(key)
        }
        return stored
    }
    
    private suspend fun moveToSwap(priority: SwapPriority) {
        val candidates = aiCoreMemory.keys.filter { key ->
            key !in criticalMemory && when (priority) {
                SwapPriority.LOW_PRIORITY -> key in swapableMemory
                SwapPriority.MEDIUM_PRIORITY -> key in swapableMemory || key.startsWith("cache_")
                SwapPriority.HIGH_PRIORITY -> !key.startsWith("ai_") && !key.startsWith("context_")
            }
        }
        
        candidates.take(5).forEach { key ->
            moveToSwap(key)
        }
    }
    
    private suspend fun moveFromSwap(priority: SwapPriority) {
        val candidates = swapBridge.getKeys().filter { key ->
            when (priority) {
                SwapPriority.HIGH_PRIORITY -> key.startsWith("active_") || key.startsWith("current_")
                SwapPriority.MEDIUM_PRIORITY -> !key.startsWith("cache_")
                SwapPriority.LOW_PRIORITY -> key.startsWith("ui_")
            }
        }
        
        candidates.take(3).forEach { key ->
            val data = swapBridge.retrieve(key)
            if (data != null) {
                aiCoreMemory[key] = data
                swapBridge.remove(key)
            }
        }
    }
    
    private fun getTotalRAM(): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (e: Exception) {
            Runtime.getRuntime().maxMemory()
        }
    }
    
    private fun getSwapInfo(): SwapInfo {
        return try {
            val memInfo = File("/proc/meminfo").readText()
            val swapTotal = memInfo.lines().find { it.startsWith("SwapTotal:") }
                ?.split("\\s+".toRegex())?.get(1)?.toLongOrNull() ?: 0L
            val swapFree = memInfo.lines().find { it.startsWith("SwapFree:") }
                ?.split("\\s+".toRegex())?.get(1)?.toLongOrNull() ?: 0L
            
            SwapInfo(
                total = swapTotal * 1024, // Convert KB to bytes
                used = (swapTotal - swapFree) * 1024,
                available = swapFree * 1024
            )
        } catch (e: Exception) {
            SwapInfo(0, 0, 0)
        }
    }
    
    private fun getAICoreMemoryUsage(): Long {
        return aiCoreMemory.values.sumOf { data ->
            when (data) {
                is String -> data.length * 2L // UTF-16
                is ByteArray -> data.size.toLong()
                is List<*> -> data.size * 8L // Rough estimate
                is Map<*, *> -> data.size * 16L
                else -> 64L // Default object overhead
            }
        }
    }
    
    private fun calculateMemoryPressure(available: Long, total: Long): Float {
        return 1f - (available.toFloat() / total.toFloat())
    }
    
    private fun triggerGarbageCollection() {
        System.gc()
        System.runFinalization()
    }
    
    /**
     * Get current memory allocation for AI Core
     */
    fun getAICoreAllocation(): Map<String, Long> {
        return criticalMemory.associateWith { key ->
            when (val data = aiCoreMemory[key]) {
                is String -> data.length * 2L
                is ByteArray -> data.size.toLong()
                is List<*> -> data.size * 8L
                is Map<*, *> -> data.size * 16L
                else -> 64L
            }
        }
    }
    
    /**
     * Set maximum dataset size (in GB)
     */
    suspend fun setMaxDatasetSize(gigabytes: Int) {
        val bytesLimit = gigabytes * 1024L * 1024L * 1024L
        store("dataset_size_limit", bytesLimit, MemoryPriority.HIGH)
        swapBridge.setMaxSize(bytesLimit)
    }
    
    fun cleanup() {
        scope.cancel()
        swapBridge.cleanup()
    }
}

/**
 * Swap Bridge for non-critical functions
 */
class SwapBridge(private val context: Context) {
    private val swapDir = File(context.cacheDir, "axia_swap")
    private val swapIndex = ConcurrentHashMap<String, SwapEntry>()
    private var maxSize = 10L * 1024L * 1024L * 1024L // 10GB default
    
    init {
        swapDir.mkdirs()
    }
    
    suspend fun store(key: String, data: Any): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(swapDir, "${key}.swap")
            val serialized = serializeData(data)
            
            file.writeBytes(serialized)
            swapIndex[key] = SwapEntry(
                file = file,
                size = serialized.size.toLong(),
                timestamp = System.currentTimeMillis(),
                accessCount = 0
            )
            
            cleanupIfNeeded()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun retrieve(key: String): Any? = withContext(Dispatchers.IO) {
        try {
            val entry = swapIndex[key] ?: return@withContext null
            val data = deserializeData(entry.file.readBytes())
            
            // Update access statistics
            swapIndex[key] = entry.copy(
                accessCount = entry.accessCount + 1,
                timestamp = System.currentTimeMillis()
            )
            
            data
        } catch (e: Exception) {
            null
        }
    }
    
    fun remove(key: String): Boolean {
        val entry = swapIndex.remove(key) ?: return false
        return entry.file.delete()
    }
    
    fun getKeys(): Set<String> = swapIndex.keys.toSet()
    
    fun getUsage(): Long = swapIndex.values.sumOf { it.size }
    
    fun setMaxSize(bytes: Long) {
        maxSize = bytes
    }
    
    private fun cleanupIfNeeded() {
        val currentUsage = getUsage()
        if (currentUsage > maxSize) {
            // Remove least recently used items
            val sortedEntries = swapIndex.entries.sortedBy { it.value.timestamp }
            var bytesToRemove = currentUsage - (maxSize * 0.8).toLong() // Clean to 80%
            
            for ((key, entry) in sortedEntries) {
                if (bytesToRemove <= 0) break
                remove(key)
                bytesToRemove -= entry.size
            }
        }
    }
    
    private fun serializeData(data: Any): ByteArray {
        return when (data) {
            is String -> data.toByteArray(Charsets.UTF_8)
            is ByteArray -> data
            else -> data.toString().toByteArray(Charsets.UTF_8)
        }
    }
    
    private fun deserializeData(bytes: ByteArray): String {
        return String(bytes, Charsets.UTF_8)
    }
    
    fun cleanup() {
        swapDir.deleteRecursively()
        swapIndex.clear()
    }
}

// Data classes
data class MemoryStatus(
    val totalRAM: Long = 0,
    val usedRAM: Long = 0,
    val availableRAM: Long = 0,
    val totalSwap: Long = 0,
    val usedSwap: Long = 0,
    val availableSwap: Long = 0,
    val aiMemoryUsage: Long = 0,
    val swapBridgeUsage: Long = 0,
    val memoryPressure: Float = 0f,
    val isLowMemory: Boolean = false,
    val threshold: Long = 0
)

data class SwapInfo(
    val total: Long,
    val used: Long,
    val available: Long
)

data class SwapEntry(
    val file: File,
    val size: Long,
    val timestamp: Long,
    val accessCount: Int
)

enum class MemoryPriority {
    CRITICAL,  // AI Core - always in RAM
    HIGH,      // Prefer RAM
    NORMAL,    // Balanced
    LOW        // Prefer swap
}

enum class SwapPriority {
    LOW_PRIORITY,    // Only least important items
    MEDIUM_PRIORITY, // More aggressive swapping
    HIGH_PRIORITY    // Most aggressive
}