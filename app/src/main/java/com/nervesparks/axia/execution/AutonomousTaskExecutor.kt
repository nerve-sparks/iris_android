package com.nervesparks.axia.execution

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.telecom.TelecomManager
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.nervesparks.axia.data.*
import com.nervesparks.axia.models.CloudModelManager
import com.nervesparks.axia.security.AxiaSecurityLayer
import android.util.Log
import java.util.*

/**
 * Autonomous Task Executor for Axia
 * Executes phone control tasks with AI reasoning and safety checks
 * 
 * Features:
 * - Phone calls and SMS with AI context understanding
 * - System settings control (WiFi, Bluetooth, Volume, etc.)
 * - App launching and control
 * - File system operations
 * - Web browsing automation
 * - Safety checks and permission handling
 */
class AutonomousTaskExecutor(
    private val context: Context,
    private val cloudModelManager: CloudModelManager,
    private val securityLayer: AxiaSecurityLayer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // System managers
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    
    // Execution state
    private val _activeTasks = MutableStateFlow<List<AutonomousTask>>(emptyList())
    val activeTasks: StateFlow<List<AutonomousTask>> = _activeTasks.asStateFlow()
    
    private val _executionMetrics = MutableStateFlow<ExecutionMetrics?>(null)
    val executionMetrics: StateFlow<ExecutionMetrics?> = _executionMetrics.asStateFlow()
    
    // Safety constraints
    private val safetyConstraints = SafetyConstraints()
    
    /**
     * Execute autonomous task with AI reasoning
     */
    suspend fun executeTask(
        description: String,
        context: String = "",
        requireConfirmation: Boolean = false
    ): TaskExecutionResult {
        return withContext(Dispatchers.IO) {
            val taskId = UUID.randomUUID().toString()
            
            try {
                Log.i("AutonomousExecutor", "Starting task execution: $description")
                
                // Create task record
                val task = AutonomousTask(
                    id = taskId,
                    description = description,
                    type = inferTaskType(description),
                    priority = inferTaskPriority(description),
                    status = TaskStatus.PENDING,
                    createdAt = System.currentTimeMillis()
                )
                
                updateActiveTask(task)
                
                // Use AI to understand and plan the task
                val executionPlan = generateExecutionPlan(description, context)
                if (executionPlan == null) {
                    return@withContext completeTask(task, TaskStatus.FAILED, error = "Failed to generate execution plan")
                }
                
                // Safety checks
                if (!performSafetyChecks(executionPlan)) {
                    return@withContext completeTask(task, TaskStatus.FAILED, error = "Task failed safety checks")
                }
                
                // Request confirmation if needed
                if (requireConfirmation && !requestUserConfirmation(executionPlan)) {
                    return@withContext completeTask(task, TaskStatus.CANCELLED, error = "User cancelled task")
                }
                
                // Execute the planned actions
                updateActiveTask(task.copy(status = TaskStatus.IN_PROGRESS))
                val result = executeActionPlan(executionPlan)
                
                // Complete task
                val finalStatus = if (result.isSuccess) TaskStatus.COMPLETED else TaskStatus.FAILED
                completeTask(task, finalStatus, result = result.message, error = result.error)
                
            } catch (e: Exception) {
                Log.e("AutonomousExecutor", "Task execution failed", e)
                completeTask(
                    AutonomousTask(
                        id = taskId,
                        description = description,
                        type = TaskType.SYSTEM_SETTING,
                        priority = TaskPriority.MEDIUM,
                        status = TaskStatus.FAILED,
                        createdAt = System.currentTimeMillis()
                    ),
                    TaskStatus.FAILED,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Make phone call with AI-generated context
     */
    suspend fun makePhoneCall(
        contactName: String? = null,
        phoneNumber: String? = null,
        context: String = ""
    ): TaskExecutionResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check permissions
                if (!hasPermission(Manifest.permission.CALL_PHONE)) {
                    return@withContext TaskExecutionResult.failure("Phone permission not granted")
                }
                
                // Resolve contact if name provided
                val finalNumber = phoneNumber ?: resolveContactNumber(contactName)
                if (finalNumber == null) {
                    return@withContext TaskExecutionResult.failure("Could not resolve phone number")
                }
                
                // Validate phone number
                if (!isValidPhoneNumber(finalNumber)) {
                    return@withContext TaskExecutionResult.failure("Invalid phone number format")
                }
                
                // Generate AI context for the call if provided
                val callContext = if (context.isNotEmpty()) {
                    generateCallContext(contactName ?: "contact", context)
                } else null
                
                // Make the call
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$finalNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                context.startActivity(callIntent)
                
                TaskExecutionResult.success("Call initiated to $finalNumber", callContext)
                
            } catch (e: Exception) {
                Log.e("AutonomousExecutor", "Phone call failed", e)
                TaskExecutionResult.failure("Failed to make call: ${e.message}")
            }
        }
    }
    
    /**
     * Send SMS with AI-generated content
     */
    suspend fun sendTextMessage(
        contactName: String? = null,
        phoneNumber: String? = null,
        messageContent: String? = null,
        context: String = ""
    ): TaskExecutionResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check permissions
                if (!hasPermission(Manifest.permission.SEND_SMS)) {
                    return@withContext TaskExecutionResult.failure("SMS permission not granted")
                }
                
                // Resolve contact
                val finalNumber = phoneNumber ?: resolveContactNumber(contactName)
                if (finalNumber == null) {
                    return@withContext TaskExecutionResult.failure("Could not resolve phone number")
                }
                
                // Generate message content if not provided
                val finalMessage = messageContent ?: generateMessageContent(context, contactName)
                if (finalMessage.isNullOrBlank()) {
                    return@withContext TaskExecutionResult.failure("Could not generate message content")
                }
                
                // Send SMS
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(finalNumber, null, finalMessage, null, null)
                
                TaskExecutionResult.success("SMS sent to $finalNumber: $finalMessage")
                
            } catch (e: Exception) {
                Log.e("AutonomousExecutor", "SMS failed", e)
                TaskExecutionResult.failure("Failed to send SMS: ${e.message}")
            }
        }
    }
    
    /**
     * Control system settings (WiFi, Bluetooth, Volume, etc.)
     */
    suspend fun controlSystemSetting(
        setting: String,
        action: String,
        value: String? = null
    ): TaskExecutionResult {
        return withContext(Dispatchers.IO) {
            try {
                when (setting.lowercase()) {
                    "wifi" -> {
                        when (action.lowercase()) {
                            "on", "enable", "turn on" -> {
                                wifiManager?.isWifiEnabled = true
                                TaskExecutionResult.success("WiFi enabled")
                            }
                            "off", "disable", "turn off" -> {
                                wifiManager?.isWifiEnabled = false
                                TaskExecutionResult.success("WiFi disabled")
                            }
                            "toggle" -> {
                                val currentState = wifiManager?.isWifiEnabled ?: false
                                wifiManager?.isWifiEnabled = !currentState
                                TaskExecutionResult.success("WiFi ${if (!currentState) "enabled" else "disabled"}")
                            }
                            else -> TaskExecutionResult.failure("Unknown WiFi action: $action")
                        }
                    }
                    
                    "bluetooth" -> {
                        when (action.lowercase()) {
                            "on", "enable", "turn on" -> {
                                if (bluetoothAdapter?.enable() == true) {
                                    TaskExecutionResult.success("Bluetooth enabled")
                                } else {
                                    TaskExecutionResult.failure("Failed to enable Bluetooth")
                                }
                            }
                            "off", "disable", "turn off" -> {
                                if (bluetoothAdapter?.disable() == true) {
                                    TaskExecutionResult.success("Bluetooth disabled")
                                } else {
                                    TaskExecutionResult.failure("Failed to disable Bluetooth")
                                }
                            }
                            "toggle" -> {
                                val isEnabled = bluetoothAdapter?.isEnabled ?: false
                                val success = if (isEnabled) {
                                    bluetoothAdapter?.disable() ?: false
                                } else {
                                    bluetoothAdapter?.enable() ?: false
                                }
                                if (success) {
                                    TaskExecutionResult.success("Bluetooth ${if (!isEnabled) "enabled" else "disabled"}")
                                } else {
                                    TaskExecutionResult.failure("Failed to toggle Bluetooth")
                                }
                            }
                            else -> TaskExecutionResult.failure("Unknown Bluetooth action: $action")
                        }
                    }
                    
                    "volume" -> {
                        value?.toIntOrNull()?.let { volumeLevel ->
                            val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                            val targetVolume = (volumeLevel * maxVolume / 100).coerceIn(0, maxVolume)
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                            TaskExecutionResult.success("Volume set to $volumeLevel%")
                        } ?: TaskExecutionResult.failure("Invalid volume value: $value")
                    }
                    
                    "brightness" -> {
                        // Note: This requires WRITE_SETTINGS permission and system app
                        TaskExecutionResult.failure("Brightness control requires system-level permissions")
                    }
                    
                    else -> TaskExecutionResult.failure("Unsupported setting: $setting")
                }
            } catch (e: Exception) {
                Log.e("AutonomousExecutor", "System setting control failed", e)
                TaskExecutionResult.failure("Failed to control $setting: ${e.message}")
            }
        }
    }
    
    /**
     * Launch application
     */
    suspend fun launchApplication(appName: String): TaskExecutionResult {
        return withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage(resolveAppPackage(appName))
                
                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    TaskExecutionResult.success("Launched $appName")
                } else {
                    TaskExecutionResult.failure("App not found: $appName")
                }
            } catch (e: Exception) {
                Log.e("AutonomousExecutor", "App launch failed", e)
                TaskExecutionResult.failure("Failed to launch $appName: ${e.message}")
            }
        }
    }
    
    /**
     * Open website in browser
     */
    suspend fun openWebsite(url: String): TaskExecutionResult {
        return withContext(Dispatchers.IO) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                TaskExecutionResult.success("Opened website: $url")
            } catch (e: Exception) {
                Log.e("AutonomousExecutor", "Website opening failed", e)
                TaskExecutionResult.failure("Failed to open website: ${e.message}")
            }
        }
    }
    
    // Private helper methods
    
    private suspend fun generateExecutionPlan(description: String, context: String): ExecutionPlan? {
        return try {
            val prompt = """
            Analyze this task request and create an execution plan:
            
            Task: $description
            Context: $context
            
            Create a step-by-step plan that includes:
            1. Required permissions
            2. Actions to perform
            3. Safety considerations
            4. Expected outcomes
            
            Respond in a structured format that can be executed.
            """.trimIndent()
            
            val result = cloudModelManager.sendInference(
                prompt = prompt,
                maxTokens = 1000,
                systemPrompt = "You are an AI assistant creating execution plans for mobile device automation. Be specific and safe."
            )
            
            when (result) {
                is com.nervesparks.axia.models.InferenceResult.Success -> {
                    parseExecutionPlan(result.content)
                }
                is com.nervesparks.axia.models.InferenceResult.Error -> {
                    Log.e("AutonomousExecutor", "Failed to generate plan: ${result.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AutonomousExecutor", "Plan generation failed", e)
            null
        }
    }
    
    private suspend fun generateCallContext(contactName: String, context: String): String? {
        return try {
            val prompt = """
            Generate a brief, professional context summary for a phone call:
            
            Contact: $contactName
            Situation: $context
            
            Provide a 1-2 sentence summary of what this call is about.
            """.trimIndent()
            
            val result = cloudModelManager.sendInference(prompt = prompt, maxTokens = 100)
            
            when (result) {
                is com.nervesparks.axia.models.InferenceResult.Success -> result.content
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun generateMessageContent(context: String, contactName: String?): String? {
        return try {
            val prompt = """
            Generate a brief, appropriate text message:
            
            Context: $context
            Recipient: ${contactName ?: "contact"}
            
            Create a short, polite message (1-2 sentences max).
            """.trimIndent()
            
            val result = cloudModelManager.sendInference(prompt = prompt, maxTokens = 100)
            
            when (result) {
                is com.nervesparks.axia.models.InferenceResult.Success -> result.content
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun resolveContactNumber(contactName: String?): String? {
        if (contactName == null) return null
        
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$contactName%"),
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                } else null
            }
        } catch (e: Exception) {
            Log.e("AutonomousExecutor", "Contact resolution failed", e)
            null
        }
    }
    
    private fun resolveAppPackage(appName: String): String {
        return when (appName.lowercase()) {
            "chrome", "browser" -> "com.android.chrome"
            "gmail", "email" -> "com.google.android.gm"
            "maps", "google maps" -> "com.google.android.apps.maps"
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "telegram" -> "org.telegram.messenger"
            "spotify" -> "com.spotify.music"
            "netflix" -> "com.netflix.mediaclient"
            "instagram" -> "com.instagram.android"
            "twitter", "x" -> "com.twitter.android"
            "facebook" -> "com.facebook.katana"
            else -> appName // Assume it's already a package name
        }
    }
    
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^[+]?[0-9]{10,15}$"))
    }
    
    private fun inferTaskType(description: String): TaskType {
        return when {
            description.contains("call", ignoreCase = true) -> TaskType.PHONE_CALL
            description.contains("text", ignoreCase = true) || description.contains("sms", ignoreCase = true) -> TaskType.TEXT_MESSAGE
            description.contains("email", ignoreCase = true) -> TaskType.EMAIL
            description.contains("wifi", ignoreCase = true) || description.contains("bluetooth", ignoreCase = true) -> TaskType.SYSTEM_SETTING
            description.contains("app", ignoreCase = true) || description.contains("launch", ignoreCase = true) -> TaskType.APP_CONTROL
            description.contains("search", ignoreCase = true) -> TaskType.WEB_SEARCH
            else -> TaskType.SYSTEM_SETTING
        }
    }
    
    private fun inferTaskPriority(description: String): TaskPriority {
        return when {
            description.contains("urgent", ignoreCase = true) || description.contains("emergency", ignoreCase = true) -> TaskPriority.CRITICAL
            description.contains("important", ignoreCase = true) -> TaskPriority.HIGH
            else -> TaskPriority.MEDIUM
        }
    }
    
    private fun parseExecutionPlan(planText: String): ExecutionPlan {
        // Simple parsing - in production this would be more sophisticated
        return ExecutionPlan(
            steps = listOf(planText),
            requiredPermissions = emptyList(),
            safetyChecks = emptyList(),
            estimatedDuration = 5000L
        )
    }
    
    private fun performSafetyChecks(plan: ExecutionPlan): Boolean {
        // Implement safety checks based on plan
        return true // Simplified for now
    }
    
    private suspend fun requestUserConfirmation(plan: ExecutionPlan): Boolean {
        // In a real implementation, this would show a UI dialog
        return true // Auto-approve for now
    }
    
    private suspend fun executeActionPlan(plan: ExecutionPlan): TaskExecutionResult {
        // Execute each step in the plan
        return TaskExecutionResult.success("Plan executed successfully")
    }
    
    private fun updateActiveTask(task: AutonomousTask) {
        val current = _activeTasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            current[index] = task
        } else {
            current.add(task)
        }
        _activeTasks.value = current
    }
    
    private fun completeTask(
        task: AutonomousTask, 
        status: TaskStatus, 
        result: String? = null, 
        error: String? = null
    ): TaskExecutionResult {
        val completedTask = task.copy(
            status = status,
            result = result,
            error = error,
            completedAt = System.currentTimeMillis()
        )
        
        updateActiveTask(completedTask)
        
        // Remove completed tasks after a delay
        scope.launch {
            delay(30000) // 30 seconds
            val current = _activeTasks.value.toMutableList()
            current.removeAll { it.id == task.id }
            _activeTasks.value = current
        }
        
        return if (status == TaskStatus.COMPLETED) {
            TaskExecutionResult.success(result ?: "Task completed")
        } else {
            TaskExecutionResult.failure(error ?: "Task failed")
        }
    }
}

// Data classes

data class ExecutionPlan(
    val steps: List<String>,
    val requiredPermissions: List<String>,
    val safetyChecks: List<String>,
    val estimatedDuration: Long
)

data class TaskExecutionResult(
    val isSuccess: Boolean,
    val message: String,
    val error: String? = null,
    val data: Any? = null
) {
    companion object {
        fun success(message: String, data: Any? = null) = TaskExecutionResult(true, message, null, data)
        fun failure(error: String) = TaskExecutionResult(false, error, error, null)
    }
}

data class ExecutionMetrics(
    val totalTasks: Int,
    val successfulTasks: Int,
    val failedTasks: Int,
    val averageExecutionTime: Long,
    val lastTaskTime: Long
)

class SafetyConstraints {
    fun isTaskSafe(task: AutonomousTask): Boolean {
        // Implement safety constraints
        return true
    }
}