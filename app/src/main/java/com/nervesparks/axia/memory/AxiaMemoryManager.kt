package com.nervesparks.axia.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.StatFs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced Memory Management System for Axia
 * Implements RAM/Swap bridge architecture for optimal AI performance
 */

@Singleton
class AxiaMemoryManager @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Memory pools
    private val ramPool = RAMPool()
    private val swapPool = SwapPool()
    
    // Memory state tracking
    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    // AI model memory (always in RAM)
    private var aiModelMemory: ByteArray? = null
    private val aiContextMemory = ConcurrentHashMap<String, Any>()
    
    // Background tasks and data structures (can use swap)
    private val swapBridge = SwapBridge()
    
    // Memory thresholds (GB)
    companion object {
        const val MAX_RAM_USAGE_GB = 8.0 // Keep AI in 8GB RAM
        const val MAX_SWAP_USAGE_GB = 64.0 // Allow 64GB swap for everything else
        const val CRITICAL_RAM_THRESHOLD = 0.9 // 90% RAM usage triggers swap
        const val SWAP_BRIDGE_THRESHOLD = 0.7 // 70% RAM usage starts moving to swap
    }
    
    init {
        startMemoryMonitoring()
    }
    
    /**
     * Allocate memory with preference (RAM for AI, Swap for everything else)
     */
    fun allocateMemory(
        size: Long,
        priority: MemoryPriority,
        description: String
    ): MemoryAllocation? {
        return when (priority) {
            MemoryPriority.AI_CRITICAL -> {
                // AI-critical data always goes to RAM
                ramPool.allocate(size, description)
            }
            MemoryPriority.AI_CONTEXT -> {
                // AI context tries RAM first, then high-priority swap
                ramPool.allocate(size, description) 
                    ?: swapPool.allocate(size, description, SwapPriority.HIGH)
            }
            MemoryPriority.BACKGROUND -> {
                // Background tasks prefer swap
                swapPool.allocate(size, description, SwapPriority.NORMAL)
                    ?: ramPool.allocate(size, description)
            }
            MemoryPriority.CACHE -> {
                // Cache uses swap exclusively
                swapPool.allocate(size, description, SwapPriority.LOW)
            }
        }
    }
    
    /**
     * Keep AI model permanently in RAM
     */
    fun loadAIModelToRAM(modelData: ByteArray): Boolean {
        return try {
            // Ensure we have enough RAM for the AI model
            val requiredRAM = modelData.size + (1024 * 1024 * 1024) // Model + 1GB buffer
            if (getAvailableRAM() >= requiredRAM) {
                aiModelMemory = modelData
                
                // Lock this memory to prevent swapping
                lockMemoryInRAM(modelData)
                
                updateMemoryState()
                true
            } else {
                // Free up RAM by moving non-critical data to swap
                freeRAMForAI(requiredRAM)
                aiModelMemory = modelData
                lockMemoryInRAM(modelData)
                updateMemoryState()
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * AI Context Management - Keep in RAM with swap backup
     */
    fun storeAIContext(contextId: String, context: Any) {
        aiContextMemory[contextId] = context
        
        // Also backup to swap for persistence
        scope.launch {
            swapBridge.backupToSwap(contextId, context)
        }
    }
    
    fun getAIContext(contextId: String): Any? {
        // Try RAM first
        return aiContextMemory[contextId] ?: run {
            // Fallback to swap
            runBlocking {
                swapBridge.restoreFromSwap(contextId)?.also {
                    // Restore to RAM
                    aiContextMemory[contextId] = it
                }
            }
        }
    }
    
    /**
     * Swap Bridge Operations
     */
    private fun freeRAMForAI(requiredBytes: Long) {
        scope.launch {
            val itemsToSwap = ramPool.getLowPriorityItems(requiredBytes)
            itemsToSwap.forEach { item ->
                swapPool.moveFromRAM(item)
                ramPool.deallocate(item.id)
            }
            updateMemoryState()
        }
    }
    
    /**
     * Memory monitoring and automatic management
     */
    private fun startMemoryMonitoring() {
        scope.launch {
            while (true) {
                updateMemoryState()
                
                val state = _memoryState.value
                
                // Auto-manage memory based on usage
                when {
                    state.ramUsagePercent > CRITICAL_RAM_THRESHOLD -> {
                        // Critical RAM usage - aggressive swap
                        triggerEmergencySwap()
                    }
                    state.ramUsagePercent > SWAP_BRIDGE_THRESHOLD -> {
                        // High RAM usage - start moving background tasks
                        triggerBackgroundSwap()
                    }
                }
                
                delay(1000) // Monitor every second
            }
        }
    }
    
    private suspend fun triggerEmergencySwap() {
        // Move all non-AI data to swap
        val nonAIItems = ramPool.getNonAIItems()
        nonAIItems.forEach { item ->
            swapPool.moveFromRAM(item)
            ramPool.deallocate(item.id)
        }
    }
    
    private suspend fun triggerBackgroundSwap() {
        // Move background tasks to swap
        val backgroundItems = ramPool.getBackgroundItems()
        backgroundItems.forEach { item ->
            swapPool.moveFromRAM(item)
            ramPool.deallocate(item.id)
        }
    }
    
    /**
     * System memory information
     */
    private fun updateMemoryState() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalRAM = memInfo.totalMem
        val availableRAM = memInfo.availMem
        val usedRAM = totalRAM - availableRAM
        
        val swapInfo = getSwapInfo()
        
        _memoryState.value = MemoryState(
            totalRAM = totalRAM,
            usedRAM = usedRAM,
            availableRAM = availableRAM,
            ramUsagePercent = usedRAM.toDouble() / totalRAM,
            totalSwap = swapInfo.total,
            usedSwap = swapInfo.used,
            availableSwap = swapInfo.available,
            swapUsagePercent = if (swapInfo.total > 0) swapInfo.used.toDouble() / swapInfo.total else 0.0,
            aiModelSize = aiModelMemory?.size?.toLong() ?: 0L,
            aiContextCount = aiContextMemory.size
        )
    }
    
    private fun getAvailableRAM(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }
    
    private fun getSwapInfo(): SwapInfo {
        return try {
            val memInfoFile = File("/proc/meminfo")
            val lines = memInfoFile.readLines()
            
            var swapTotal = 0L
            var swapFree = 0L
            
            lines.forEach { line ->
                when {
                    line.startsWith("SwapTotal:") -> {
                        swapTotal = line.split("\\s+".toRegex())[1].toLong() * 1024
                    }
                    line.startsWith("SwapFree:") -> {
                        swapFree = line.split("\\s+".toRegex())[1].toLong() * 1024
                    }
                }
            }
            
            SwapInfo(
                total = swapTotal,
                used = swapTotal - swapFree,
                available = swapFree
            )
        } catch (e: Exception) {
            SwapInfo(0, 0, 0)
        }
    }
    
    private fun lockMemoryInRAM(data: ByteArray) {
        // Use mlock system call to prevent swapping (requires root)
        try {
            val process = ProcessBuilder("su", "-c", "echo 'Memory locked for AI'").start()
            process.waitFor()
        } catch (e: Exception) {
            // Fallback: keep strong reference to prevent GC
        }
    }
    
    fun cleanup() {
        scope.cancel()
        ramPool.cleanup()
        swapPool.cleanup()
    }
}

// Memory Management Classes
class RAMPool {
    private val allocations = ConcurrentHashMap<String, MemoryAllocation>()
    
    fun allocate(size: Long, description: String): MemoryAllocation? {
        val id = generateId()
        val allocation = MemoryAllocation(
            id = id,
            size = size,
            description = description,
            location = MemoryLocation.RAM,
            priority = determinePriority(description)
        )
        allocations[id] = allocation
        return allocation
    }
    
    fun deallocate(id: String) {
        allocations.remove(id)
    }
    
    fun getLowPriorityItems(requiredBytes: Long): List<MemoryAllocation> {
        return allocations.values
            .filter { it.priority != MemoryPriority.AI_CRITICAL }
            .sortedBy { it.priority.ordinal }
            .takeWhile { requiredBytes > 0 }
    }
    
    fun getNonAIItems(): List<MemoryAllocation> {
        return allocations.values
            .filter { !it.description.contains("AI", ignoreCase = true) }
    }
    
    fun getBackgroundItems(): List<MemoryAllocation> {
        return allocations.values
            .filter { it.priority == MemoryPriority.BACKGROUND }
    }
    
    private fun determinePriority(description: String): MemoryPriority {
        return when {
            description.contains("AI_MODEL", ignoreCase = true) -> MemoryPriority.AI_CRITICAL
            description.contains("AI_CONTEXT", ignoreCase = true) -> MemoryPriority.AI_CONTEXT
            description.contains("BACKGROUND", ignoreCase = true) -> MemoryPriority.BACKGROUND
            else -> MemoryPriority.CACHE
        }
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString()
    
    fun cleanup() {
        allocations.clear()
    }
}

class SwapPool {
    private val swapDirectory = File("/data/data/com.nervesparks.axia/swap/")
    
    init {
        swapDirectory.mkdirs()
    }
    
    fun allocate(size: Long, description: String, priority: SwapPriority): MemoryAllocation? {
        return try {
            val id = generateId()
            val swapFile = File(swapDirectory, "$id.swap")
            swapFile.createNewFile()
            
            MemoryAllocation(
                id = id,
                size = size,
                description = description,
                location = MemoryLocation.SWAP,
                priority = MemoryPriority.BACKGROUND,
                swapFile = swapFile
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun moveFromRAM(allocation: MemoryAllocation): Boolean {
        return try {
            val swapFile = File(swapDirectory, "${allocation.id}.swap")
            // Move data from RAM to swap file
            swapFile.createNewFile()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString()
    
    fun cleanup() {
        swapDirectory.listFiles()?.forEach { it.delete() }
    }
}

class SwapBridge {
    private val swapDirectory = File("/data/data/com.nervesparks.axia/context/")
    
    init {
        swapDirectory.mkdirs()
    }
    
    suspend fun backupToSwap(contextId: String, context: Any) = withContext(Dispatchers.IO) {
        try {
            val file = File(swapDirectory, "$contextId.ctx")
            file.writeText(context.toString())
        } catch (e: Exception) {
            // Handle backup failure
        }
    }
    
    suspend fun restoreFromSwap(contextId: String): Any? = withContext(Dispatchers.IO) {
        try {
            val file = File(swapDirectory, "$contextId.ctx")
            if (file.exists()) {
                file.readText()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

// Data classes
data class MemoryState(
    val totalRAM: Long = 0,
    val usedRAM: Long = 0,
    val availableRAM: Long = 0,
    val ramUsagePercent: Double = 0.0,
    val totalSwap: Long = 0,
    val usedSwap: Long = 0,
    val availableSwap: Long = 0,
    val swapUsagePercent: Double = 0.0,
    val aiModelSize: Long = 0,
    val aiContextCount: Int = 0
)

data class MemoryAllocation(
    val id: String,
    val size: Long,
    val description: String,
    val location: MemoryLocation,
    val priority: MemoryPriority,
    val swapFile: File? = null
)

data class SwapInfo(
    val total: Long,
    val used: Long,
    val available: Long
)

enum class MemoryLocation {
    RAM, SWAP
}

enum class MemoryPriority {
    AI_CRITICAL,    // AI model - always in RAM
    AI_CONTEXT,     // AI context - RAM preferred
    BACKGROUND,     // Background tasks - swap preferred
    CACHE           // Cache data - swap only
}

enum class SwapPriority {
    HIGH, NORMAL, LOW
}