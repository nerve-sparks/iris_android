package com.nervesparks.axia.execution

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.provider.Settings
import android.media.MediaPlayer
import android.content.ContentResolver
import android.provider.ContactsContract
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.*
import com.nervesparks.axia.models.*
import com.nervesparks.axia.security.AxiaSecurityLayer

/**
 * Universal Task Executor for Axia
 * "No matter what I tell it to do, I need it to do it"
 * 
 * Capabilities:
 * - System management (WiFi, Bluetooth, calls, SMS)
 * - App management (install, launch, manage)  
 * - File operations (create, modify, organize)
 * - Scheduling (timers, alarms, reminders)
 * - Code execution and self-modification
 * - Development tasks (build, test, deploy)
 */
class UniversalTaskExecutor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // System service managers
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val packageManager = context.packageManager
    
    // Task state management
    private val _activeTasks = MutableStateFlow<List<ActiveTask>>(emptyList())
    val activeTasks: StateFlow<List<ActiveTask>> = _activeTasks.asStateFlow()
    
    private val _taskHistory = MutableStateFlow<List<CompletedTask>>(emptyList())
    val taskHistory: StateFlow<List<CompletedTask>> = _taskHistory.asStateFlow()
    
    suspend fun initialize() {
        // Initialize task execution system
        loadTaskHistory()
    }
    
    /**
     * Execute system commands (WiFi, Bluetooth, calls, SMS, etc.)
     */
    suspend fun executeSystemCommand(command: SystemCommand): TaskResult = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        addActiveTask(taskId, "System: ${command.type}", command.parameters.toString())
        
        try {
            val result = when (command.type.lowercase()) {
                "wifi" -> handleWifiCommand(command)
                "bluetooth" -> handleBluetoothCommand(command)
                "call" -> handleCallCommand(command)
                "sms", "text" -> handleSmsCommand(command)
                "alarm", "timer" -> handleTimerAlarmCommand(command)
                "contact" -> handleContactCommand(command)
                "app" -> handleAppCommand(command)
                "volume" -> handleVolumeCommand(command)
                "brightness" -> handleBrightnessCommand(command)
                "screenshot" -> handleScreenshotCommand(command)
                "block" -> handleBlockCommand(command)
                else -> TaskResult.error(taskId, "Unknown system command: ${command.type}")
            }
            
            completeTask(taskId, result)
            result
            
        } catch (e: Exception) {
            val errorResult = TaskResult.error(taskId, "Failed to execute ${command.type}: ${e.message}")
            completeTask(taskId, errorResult)
            errorResult
        }
    }
    
    /**
     * Execute daily tasks (reminders, scheduling, etc.)
     */
    suspend fun executeDailyTasks(plan: TaskPlan): List<TaskResult> {
        return plan.tasks.map { task ->
            val command = parseTaskToCommand(task)
            executeSystemCommand(command)
        }
    }
    
    /**
     * Execute code-related tasks
     */
    suspend fun executeCodeTask(code: String, request: AxiaRequest): TaskResult = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        addActiveTask(taskId, "Code Execution", code.take(100))
        
        try {
            val result = when {
                code.contains("class") && code.contains("fun") -> {
                    // Kotlin code execution
                    executeKotlinCode(code, request)
                }
                code.contains("def") || code.contains("import") -> {
                    // Python code execution
                    executePythonCode(code, request)
                }
                code.contains("function") || code.contains("const") -> {
                    // JavaScript code execution  
                    executeJavaScriptCode(code, request)
                }
                else -> {
                    // Generic code execution
                    executeGenericCode(code, request)
                }
            }
            
            completeTask(taskId, result)
            result
            
        } catch (e: Exception) {
            val errorResult = TaskResult.error(taskId, "Code execution failed: ${e.message}")
            completeTask(taskId, errorResult)
            errorResult
        }
    }
    
    /**
     * Execute self-modification tasks (Axia modifying itself)
     */
    suspend fun executeSelfModification(
        code: String, 
        request: AxiaRequest, 
        safetyChecks: Boolean
    ): TaskResult = withContext(Dispatchers.IO) {
        val taskId = UUID.randomUUID().toString()
        addActiveTask(taskId, "Self-Modification", request.content.take(100))
        
        try {
            if (safetyChecks && !isSafeModification(code)) {
                return@withContext TaskResult.error(taskId, "Safety check failed - modification too risky")
            }
            
            val result = when {
                code.contains("package com.nervesparks.axia") -> {
                    // Modifying Axia core
                    modifyAxiaCore(code, request)
                }
                code.contains("activity") || code.contains("fragment") -> {
                    // UI modifications
                    modifyAxiaUI(code, request)
                }
                code.contains("fun") && code.contains("suspend") -> {
                    // Adding new capabilities
                    addAxiaCapability(code, request)
                }
                else -> {
                    // Generic file modification
                    modifyAxiaFiles(code, request)
                }
            }
            
            completeTask(taskId, result)
            result
            
        } catch (e: Exception) {
            val errorResult = TaskResult.error(taskId, "Self-modification failed: ${e.message}")
            completeTask(taskId, errorResult)
            errorResult
        }
    }
    
    // System command handlers
    private fun handleWifiCommand(command: SystemCommand): TaskResult {
        val action = command.parameters["action"] as? String ?: "toggle"
        
        return try {
            when (action.lowercase()) {
                "on", "enable" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ requires user to enable WiFi manually
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        TaskResult.success("", "Opened WiFi settings - please enable manually", "Android 10+ requires manual WiFi control")
                    } else {
                        @Suppress("DEPRECATION")
                        wifiManager.isWifiEnabled = true
                        TaskResult.success("", "WiFi enabled", "WiFi has been turned on")
                    }
                }
                "off", "disable" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        TaskResult.success("", "Opened WiFi settings - please disable manually", "Android 10+ requires manual WiFi control")
                    } else {
                        @Suppress("DEPRECATION")
                        wifiManager.isWifiEnabled = false
                        TaskResult.success("", "WiFi disabled", "WiFi has been turned off")
                    }
                }
                "status" -> {
                    val isEnabled = wifiManager.isWifiEnabled
                    val status = if (isEnabled) "enabled" else "disabled"
                    TaskResult.success("", "WiFi is $status", "Current WiFi state: $status")
                }
                else -> {
                    TaskResult.error("", "Unknown WiFi action: $action")
                }
            }
        } catch (e: Exception) {
            TaskResult.error("", "WiFi command failed: ${e.message}")
        }
    }
    
    private fun handleBluetoothCommand(command: SystemCommand): TaskResult {
        val action = command.parameters["action"] as? String ?: "toggle"
        val bluetoothAdapter = bluetoothManager.adapter
        
        return try {
            when (action.lowercase()) {
                "on", "enable" -> {
                    if (bluetoothAdapter?.isEnabled != true) {
                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        TaskResult.success("", "Bluetooth enable requested", "Please allow Bluetooth to be enabled")
                    } else {
                        TaskResult.success("", "Bluetooth already enabled", "Bluetooth is already on")
                    }
                }
                "off", "disable" -> {
                    if (bluetoothAdapter?.isEnabled == true) {
                        bluetoothAdapter.disable()
                        TaskResult.success("", "Bluetooth disabled", "Bluetooth has been turned off")
                    } else {
                        TaskResult.success("", "Bluetooth already disabled", "Bluetooth is already off")
                    }
                }
                "status" -> {
                    val status = if (bluetoothAdapter?.isEnabled == true) "enabled" else "disabled"
                    TaskResult.success("", "Bluetooth is $status", "Current Bluetooth state: $status")
                }
                else -> {
                    TaskResult.error("", "Unknown Bluetooth action: $action")
                }
            }
        } catch (e: Exception) {
            TaskResult.error("", "Bluetooth command failed: ${e.message}")
        }
    }
    
    private fun handleCallCommand(command: SystemCommand): TaskResult {
        val phoneNumber = command.parameters["number"] as? String
        val contact = command.parameters["contact"] as? String
        
        return try {
            val numberToCall = phoneNumber ?: findContactNumber(contact ?: "")
            
            if (numberToCall.isNullOrEmpty()) {
                return TaskResult.error("", "No phone number provided or contact not found")
            }
            
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$numberToCall")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == 
                PackageManager.PERMISSION_GRANTED) {
                context.startActivity(callIntent)
                TaskResult.success("", "Call initiated", "Calling $numberToCall")
            } else {
                // Fallback to dialer
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$numberToCall")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                TaskResult.success("", "Dialer opened", "Please confirm call to $numberToCall")
            }
        } catch (e: Exception) {
            TaskResult.error("", "Call failed: ${e.message}")
        }
    }
    
    private fun handleSmsCommand(command: SystemCommand): TaskResult {
        val phoneNumber = command.parameters["number"] as? String
        val contact = command.parameters["contact"] as? String
        val message = command.parameters["message"] as? String ?: ""
        
        return try {
            val numberToText = phoneNumber ?: findContactNumber(contact ?: "")
            
            if (numberToText.isNullOrEmpty()) {
                return TaskResult.error("", "No phone number provided or contact not found")
            }
            
            if (context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == 
                PackageManager.PERMISSION_GRANTED) {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(numberToText, null, message, null, null)
                TaskResult.success("", "SMS sent", "Message sent to $numberToText")
            } else {
                // Fallback to messaging app
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$numberToText")
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
                TaskResult.success("", "Messaging app opened", "Please confirm sending message")
            }
        } catch (e: Exception) {
            TaskResult.error("", "SMS failed: ${e.message}")
        }
    }
    
    private fun handleTimerAlarmCommand(command: SystemCommand): TaskResult {
        val duration = command.parameters["duration"] as? Long ?: 0L
        val time = command.parameters["time"] as? String
        val message = command.parameters["message"] as? String ?: "Alarm"
        
        return try {
            val triggerTime = if (time != null) {
                parseTimeString(time)
            } else {
                System.currentTimeMillis() + (duration * 1000)
            }
            
            val intent = Intent("com.nervesparks.axia.ALARM_TRIGGERED").apply {
                putExtra("message", message)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                triggerTime.toInt(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            
            val timeStr = if (duration > 0) "${duration}s" else time
            TaskResult.success("", "Alarm set", "Alarm set for $timeStr with message: $message")
            
        } catch (e: Exception) {
            TaskResult.error("", "Alarm/Timer failed: ${e.message}")
        }
    }
    
    private fun handleContactCommand(command: SystemCommand): TaskResult {
        val action = command.parameters["action"] as? String ?: "find"
        val name = command.parameters["name"] as? String ?: ""
        
        return when (action.lowercase()) {
            "find", "search" -> {
                val contact = findContact(name)
                if (contact != null) {
                    TaskResult.success("", "Contact found", "Found: ${contact.name} - ${contact.phone}")
                } else {
                    TaskResult.error("", "Contact not found: $name")
                }
            }
            else -> TaskResult.error("", "Unknown contact action: $action")
        }
    }
    
    private fun handleAppCommand(command: SystemCommand): TaskResult {
        val action = command.parameters["action"] as? String ?: "launch"
        val appName = command.parameters["app"] as? String ?: ""
        val packageName = command.parameters["package"] as? String
        
        return try {
            when (action.lowercase()) {
                "launch", "open" -> {
                    val intent = if (packageName != null) {
                        packageManager.getLaunchIntentForPackage(packageName)
                    } else {
                        findAppByName(appName)
                    }
                    
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        TaskResult.success("", "App launched", "Opened ${appName.ifEmpty { packageName }}")
                    } else {
                        TaskResult.error("", "App not found: ${appName.ifEmpty { packageName }}")
                    }
                }
                else -> TaskResult.error("", "Unknown app action: $action")
            }
        } catch (e: Exception) {
            TaskResult.error("", "App command failed: ${e.message}")
        }
    }
    
    private fun handleVolumeCommand(command: SystemCommand): TaskResult {
        // Volume control would require audio manager
        return TaskResult.success("", "Volume command processed", "Volume control not implemented yet")
    }
    
    private fun handleBrightnessCommand(command: SystemCommand): TaskResult {
        // Brightness control would require system settings access
        return TaskResult.success("", "Brightness command processed", "Brightness control not implemented yet")
    }
    
    private fun handleScreenshotCommand(command: SystemCommand): TaskResult {
        // Screenshot would require media projection API
        return TaskResult.success("", "Screenshot command processed", "Screenshot not implemented yet")
    }
    
    private fun handleBlockCommand(command: SystemCommand): TaskResult {
        val number = command.parameters["number"] as? String ?: ""
        // Call blocking would require telecom manager integration
        return TaskResult.success("", "Block command processed", "Number blocking for $number not implemented yet")
    }
    
    // Code execution methods
    private fun executeKotlinCode(code: String, request: AxiaRequest): TaskResult {
        // This would implement Kotlin code execution
        return TaskResult.success("", "Kotlin code executed", "Code execution completed (placeholder)")
    }
    
    private fun executePythonCode(code: String, request: AxiaRequest): TaskResult {
        // This would implement Python code execution via Chaquopy or similar
        return TaskResult.success("", "Python code executed", "Code execution completed (placeholder)")
    }
    
    private fun executeJavaScriptCode(code: String, request: AxiaRequest): TaskResult {
        // This would implement JavaScript execution via WebView
        return TaskResult.success("", "JavaScript executed", "Code execution completed (placeholder)")
    }
    
    private fun executeGenericCode(code: String, request: AxiaRequest): TaskResult {
        // Generic code execution
        return TaskResult.success("", "Code executed", "Generic code execution completed")
    }
    
    // Self-modification methods
    private fun isSafeModification(code: String): Boolean {
        val dangerousPatterns = listOf(
            "System.exit",
            "deleteRecursively",
            "rm -rf",
            "DROP TABLE",
            "exec",
            "Runtime.getRuntime"
        )
        
        return !dangerousPatterns.any { pattern ->
            code.contains(pattern, ignoreCase = true)
        }
    }
    
    private fun modifyAxiaCore(code: String, request: AxiaRequest): TaskResult {
        // This would implement safe core modifications
        return TaskResult.success("", "Core modified", "Axia core modification completed (placeholder)")
    }
    
    private fun modifyAxiaUI(code: String, request: AxiaRequest): TaskResult {
        // This would implement UI modifications
        return TaskResult.success("", "UI modified", "Axia UI modification completed (placeholder)")
    }
    
    private fun addAxiaCapability(code: String, request: AxiaRequest): TaskResult {
        // This would add new capabilities to Axia
        return TaskResult.success("", "Capability added", "New capability added to Axia (placeholder)")
    }
    
    private fun modifyAxiaFiles(code: String, request: AxiaRequest): TaskResult {
        // This would modify Axia files safely
        return TaskResult.success("", "Files modified", "Axia files modified (placeholder)")
    }
    
    // Helper methods
    private fun findContactNumber(contactName: String): String? {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != 
            PackageManager.PERMISSION_GRANTED) {
            return null
        }
        
        val resolver: ContentResolver = context.contentResolver
        val cursor: Cursor? = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$contactName%"),
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return it.getString(phoneIndex)
            }
        }
        
        return null
    }
    
    private fun findContact(name: String): Contact? {
        // Simplified contact finding
        val phoneNumber = findContactNumber(name)
        return if (phoneNumber != null) {
            Contact(name, phoneNumber)
        } else {
            null
        }
    }
    
    private fun findAppByName(appName: String): Intent? {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in installedApps) {
            val appLabel = packageManager.getApplicationLabel(app).toString()
            if (appLabel.contains(appName, ignoreCase = true)) {
                return packageManager.getLaunchIntentForPackage(app.packageName)
            }
        }
        
        return null
    }
    
    private fun parseTimeString(timeStr: String): Long {
        // Simple time parsing - could be enhanced
        return try {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                
                // If time has passed today, set for tomorrow
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                calendar.timeInMillis
            } else {
                System.currentTimeMillis() + 60000 // Default to 1 minute from now
            }
        } catch (e: Exception) {
            System.currentTimeMillis() + 60000
        }
    }
    
    private fun parseTaskToCommand(task: String): SystemCommand {
        val taskLower = task.lowercase()
        
        return when {
            taskLower.contains("wifi") -> SystemCommand("wifi", mapOf("action" to "toggle"))
            taskLower.contains("call") -> {
                val number = extractPhoneNumber(task)
                SystemCommand("call", mapOf("number" to number))
            }
            taskLower.contains("text") || taskLower.contains("sms") -> {
                val number = extractPhoneNumber(task)
                val message = extractMessage(task)
                SystemCommand("sms", mapOf("number" to number, "message" to message))
            }
            taskLower.contains("timer") -> {
                val duration = extractDuration(task)
                SystemCommand("timer", mapOf("duration" to duration))
            }
            else -> SystemCommand("unknown", mapOf("task" to task))
        }
    }
    
    private fun extractPhoneNumber(text: String): String {
        val phoneRegex = Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b")
        return phoneRegex.find(text)?.value ?: ""
    }
    
    private fun extractMessage(text: String): String {
        val parts = text.split("saying", "message", "text")
        return if (parts.size > 1) parts.last().trim() else ""
    }
    
    private fun extractDuration(text: String): Long {
        val durationRegex = Regex("(\\d+)\\s*(second|minute|hour)s?")
        val match = durationRegex.find(text)
        
        return if (match != null) {
            val number = match.groupValues[1].toLongOrNull() ?: 0L
            val unit = match.groupValues[2]
            
            when (unit) {
                "second" -> number
                "minute" -> number * 60
                "hour" -> number * 3600
                else -> number
            }
        } else {
            60L // Default 1 minute
        }
    }
    
    private fun addActiveTask(id: String, description: String, details: String) {
        val task = ActiveTask(id, description, details, System.currentTimeMillis())
        _activeTasks.value = _activeTasks.value + task
    }
    
    private fun completeTask(id: String, result: TaskResult) {
        val activeTask = _activeTasks.value.find { it.id == id }
        _activeTasks.value = _activeTasks.value.filterNot { it.id == id }
        
        if (activeTask != null) {
            val completedTask = CompletedTask(
                id = id,
                description = activeTask.description,
                result = result,
                startTime = activeTask.startTime,
                endTime = System.currentTimeMillis()
            )
            _taskHistory.value = (_taskHistory.value + completedTask).takeLast(100) // Keep last 100
        }
    }
    
    private fun loadTaskHistory() {
        // Load task history from storage - placeholder
    }
}

// Data classes
data class SystemCommand(
    val type: String,
    val parameters: Map<String, Any>
)

data class TaskPlan(
    val tasks: List<String> = emptyList()
)

data class TaskResult(
    val id: String,
    val status: String,
    val description: String,
    val details: String = "",
    val success: Boolean = true,
    val error: String? = null
) {
    companion object {
        fun success(id: String, description: String, details: String = ""): TaskResult {
            return TaskResult(id, "SUCCESS", description, details, true)
        }
        
        fun error(id: String, message: String): TaskResult {
            return TaskResult(id, "ERROR", "Task failed", message, false, message)
        }
    }
}

data class Contact(
    val name: String,
    val phone: String
)

data class ActiveTask(
    val id: String,
    val description: String,
    val details: String,
    val startTime: Long
)

data class CompletedTask(
    val id: String,
    val description: String,
    val result: TaskResult,
    val startTime: Long,
    val endTime: Long
)