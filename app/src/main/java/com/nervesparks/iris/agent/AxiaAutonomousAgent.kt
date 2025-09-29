package com.nervesparks.iris.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autonomous AI Agent for Axia
 * Direct phone control and automation
 */

@Singleton
class AxiaAutonomousAgent @Inject constructor(
    private val context: Context,
    private val terminalController: TerminalController,
    private val memoryManager: com.nervesparks.iris.memory.AxiaMemoryManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Agent state
    private val _agentState = MutableStateFlow(AgentState.IDLE)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()
    
    // Command queue
    private val commandQueue = Channel<AgentCommand>(Channel.UNLIMITED)
    
    // Accessibility service reference
    private var accessibilityService: AxiaAccessibilityService? = null
    
    // Action executor
    private val actionExecutor = ActionExecutor(context)
    
    init {
        startCommandProcessor()
        requestAccessibilityPermission()
    }
    
    private fun startCommandProcessor() {
        scope.launch {
            commandQueue.consumeAsFlow().collect { command ->
                processCommand(command)
            }
        }
    }
    
    /**
     * Execute user command autonomously
     */
    suspend fun executeCommand(userInput: String): AgentResponse {
        _agentState.value = AgentState.THINKING
        
        // Parse intent from user input
        val intent = parseUserIntent(userInput)
        
        return when (intent.type) {
            IntentType.PHONE_CONTROL -> executePhoneControl(intent)
            IntentType.TERMINAL_COMMAND -> executeTerminalCommand(intent)
            IntentType.APP_AUTOMATION -> executeAppAutomation(intent)
            IntentType.FILE_OPERATION -> executeFileOperation(intent)
            IntentType.SYSTEM_SETTING -> executeSystemSetting(intent)
            IntentType.INFORMATION_QUERY -> executeInformationQuery(intent)
            IntentType.DEVELOPMENT_TASK -> executeDevelopmentTask(intent)
            else -> AgentResponse.error("I don't understand that command yet.")
        }
    }
    
    private suspend fun executePhoneControl(intent: AgentIntent): AgentResponse {
        _agentState.value = AgentState.EXECUTING
        
        return try {
            when (intent.action) {
                "call" -> {
                    val phoneNumber = intent.parameters["phone"] as? String
                    if (phoneNumber != null) {
                        actionExecutor.makeCall(phoneNumber)
                        AgentResponse.success("Calling $phoneNumber")
                    } else {
                        AgentResponse.error("Please provide a phone number")
                    }
                }
                "text", "sms" -> {
                    val number = intent.parameters["phone"] as? String
                    val message = intent.parameters["message"] as? String
                    if (number != null && message != null) {
                        actionExecutor.sendText(number, message)
                        AgentResponse.success("Sent text to $number")
                    } else {
                        AgentResponse.error("Please provide phone number and message")
                    }
                }
                "open_app" -> {
                    val appName = intent.parameters["app"] as? String
                    if (appName != null) {
                        actionExecutor.openApp(appName)
                        AgentResponse.success("Opened $appName")
                    } else {
                        AgentResponse.error("Please specify which app to open")
                    }
                }
                else -> AgentResponse.error("Phone control action not supported: ${intent.action}")
            }
        } catch (e: Exception) {
            AgentResponse.error("Failed to execute phone control: ${e.message}")
        } finally {
            _agentState.value = AgentState.IDLE
        }
    }
    
    private suspend fun executeTerminalCommand(intent: AgentIntent): AgentResponse {
        _agentState.value = AgentState.EXECUTING
        
        val command = intent.parameters["command"] as? String
        if (command == null) {
            _agentState.value = AgentState.IDLE
            return AgentResponse.error("No command specified")
        }
        
        return try {
            val result = terminalController.executeCommand(command)
            _agentState.value = AgentState.IDLE
            AgentResponse.success(result.output, result.error.takeIf { it.isNotEmpty() })
        } catch (e: Exception) {
            _agentState.value = AgentState.IDLE
            AgentResponse.error("Terminal command failed: ${e.message}")
        }
    }
    
    private suspend fun executeAppAutomation(intent: AgentIntent): AgentResponse {
        _agentState.value = AgentState.EXECUTING
        
        val service = accessibilityService
        if (service == null) {
            _agentState.value = AgentState.IDLE
            return AgentResponse.error("Accessibility service not enabled. Please enable it in Settings > Accessibility > Axia")
        }
        
        return try {
            when (intent.action) {
                "click" -> {
                    val text = intent.parameters["text"] as? String
                    if (text != null) {
                        service.clickByText(text)
                        AgentResponse.success("Clicked on '$text'")
                    } else {
                        AgentResponse.error("Please specify what to click")
                    }
                }
                "type" -> {
                    val text = intent.parameters["text"] as? String
                    if (text != null) {
                        service.typeText(text)
                        AgentResponse.success("Typed '$text'")
                    } else {
                        AgentResponse.error("Please specify what to type")
                    }
                }
                "scroll" -> {
                    val direction = intent.parameters["direction"] as? String ?: "down"
                    service.scroll(direction)
                    AgentResponse.success("Scrolled $direction")
                }
                "back" -> {
                    service.goBack()
                    AgentResponse.success("Pressed back button")
                }
                "home" -> {
                    service.goHome()
                    AgentResponse.success("Went to home screen")
                }
                else -> AgentResponse.error("App automation action not supported: ${intent.action}")
            }
        } catch (e: Exception) {
            AgentResponse.error("App automation failed: ${e.message}")
        } finally {
            _agentState.value = AgentState.IDLE
        }
    }
    
    private suspend fun executeFileOperation(intent: AgentIntent): AgentResponse {
        _agentState.value = AgentState.EXECUTING
        
        return try {
            when (intent.action) {
                "create" -> {
                    val filename = intent.parameters["filename"] as? String
                    val content = intent.parameters["content"] as? String ?: ""
                    if (filename != null) {
                        terminalController.executeCommand("echo '$content' > '$filename'")
                        AgentResponse.success("Created file: $filename")
                    } else {
                        AgentResponse.error("Please specify filename")
                    }
                }
                "read" -> {
                    val filename = intent.parameters["filename"] as? String
                    if (filename != null) {
                        val result = terminalController.executeCommand("cat '$filename'")
                        AgentResponse.success("File contents:\n${result.output}")
                    } else {
                        AgentResponse.error("Please specify filename")
                    }
                }
                "delete" -> {
                    val filename = intent.parameters["filename"] as? String
                    if (filename != null) {
                        terminalController.executeCommand("rm '$filename'")
                        AgentResponse.success("Deleted file: $filename")
                    } else {
                        AgentResponse.error("Please specify filename")
                    }
                }
                else -> AgentResponse.error("File operation not supported: ${intent.action}")
            }
        } catch (e: Exception) {
            AgentResponse.error("File operation failed: ${e.message}")
        } finally {
            _agentState.value = AgentState.IDLE
        }
    }
    
    private suspend fun executeSystemSetting(intent: AgentIntent): AgentResponse {
        _agentState.value = AgentState.EXECUTING
        
        return try {
            when (intent.action) {
                "wifi" -> {
                    val enable = intent.parameters["enable"] as? Boolean ?: true
                    actionExecutor.toggleWifi(enable)
                    AgentResponse.success("WiFi ${if (enable) "enabled" else "disabled"}")
                }
                "bluetooth" -> {
                    val enable = intent.parameters["enable"] as? Boolean ?: true
                    actionExecutor.toggleBluetooth(enable)
                    AgentResponse.success("Bluetooth ${if (enable) "enabled" else "disabled"}")
                }
                "brightness" -> {
                    val level = intent.parameters["level"] as? Int ?: 50
                    actionExecutor.setBrightness(level)
                    AgentResponse.success("Set brightness to $level%")
                }
                "volume" -> {
                    val level = intent.parameters["level"] as? Int ?: 50
                    actionExecutor.setVolume(level)
                    AgentResponse.success("Set volume to $level%")
                }
                else -> AgentResponse.error("System setting not supported: ${intent.action}")
            }
        } catch (e: Exception) {
            AgentResponse.error("System setting failed: ${e.message}")
        } finally {
            _agentState.value = AgentState.IDLE
        }
    }
    
    private suspend fun executeInformationQuery(intent: AgentIntent): AgentResponse {
        // This would integrate with the AI model for information queries
        return AgentResponse.success("Information query processing not implemented yet")
    }
    
    private suspend fun executeDevelopmentTask(intent: AgentIntent): AgentResponse {
        _agentState.value = AgentState.EXECUTING
        
        return try {
            when (intent.action) {
                "build" -> {
                    val result = terminalController.executeCommand("./gradlew build")
                    AgentResponse.success("Build completed:\n${result.output}")
                }
                "test" -> {
                    val result = terminalController.executeCommand("./gradlew test")
                    AgentResponse.success("Tests completed:\n${result.output}")
                }
                "deploy" -> {
                    val result = terminalController.executeCommand("./gradlew installDebug")
                    AgentResponse.success("App deployed:\n${result.output}")
                }
                else -> AgentResponse.error("Development task not supported: ${intent.action}")
            }
        } catch (e: Exception) {
            AgentResponse.error("Development task failed: ${e.message}")
        } finally {
            _agentState.value = AgentState.IDLE
        }
    }
    
    private fun parseUserIntent(input: String): AgentIntent {
        val lowerInput = input.lowercase()
        
        return when {
            // Phone control patterns
            lowerInput.contains("call") && lowerInput.matches(".*\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b.*".toRegex()) -> {
                val phoneRegex = "\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b".toRegex()
                val phone = phoneRegex.find(input)?.value
                AgentIntent(IntentType.PHONE_CONTROL, "call", mapOf("phone" to phone))
            }
            
            lowerInput.contains("text") || lowerInput.contains("sms") -> {
                AgentIntent(IntentType.PHONE_CONTROL, "text", extractTextMessageParams(input))
            }
            
            lowerInput.startsWith("open ") -> {
                val app = input.substring(5).trim()
                AgentIntent(IntentType.PHONE_CONTROL, "open_app", mapOf("app" to app))
            }
            
            // Terminal commands
            lowerInput.startsWith("run ") || lowerInput.startsWith("execute ") -> {
                val command = if (lowerInput.startsWith("run ")) input.substring(4) else input.substring(8)
                AgentIntent(IntentType.TERMINAL_COMMAND, "execute", mapOf("command" to command))
            }
            
            // App automation
            lowerInput.contains("click on ") -> {
                val text = input.substringAfter("click on ").trim()
                AgentIntent(IntentType.APP_AUTOMATION, "click", mapOf("text" to text))
            }
            
            lowerInput.contains("type ") -> {
                val text = input.substringAfter("type ").trim().removeSurrounding("\"")
                AgentIntent(IntentType.APP_AUTOMATION, "type", mapOf("text" to text))
            }
            
            // File operations
            lowerInput.startsWith("create file ") -> {
                val filename = input.substring(12).trim()
                AgentIntent(IntentType.FILE_OPERATION, "create", mapOf("filename" to filename))
            }
            
            lowerInput.startsWith("read file ") -> {
                val filename = input.substring(10).trim()
                AgentIntent(IntentType.FILE_OPERATION, "read", mapOf("filename" to filename))
            }
            
            // System settings
            lowerInput.contains("turn on wifi") || lowerInput.contains("enable wifi") -> {
                AgentIntent(IntentType.SYSTEM_SETTING, "wifi", mapOf("enable" to true))
            }
            
            lowerInput.contains("turn off wifi") || lowerInput.contains("disable wifi") -> {
                AgentIntent(IntentType.SYSTEM_SETTING, "wifi", mapOf("enable" to false))
            }
            
            // Development tasks
            lowerInput.contains("build") && (lowerInput.contains("app") || lowerInput.contains("project")) -> {
                AgentIntent(IntentType.DEVELOPMENT_TASK, "build", emptyMap())
            }
            
            else -> AgentIntent(IntentType.UNKNOWN, "unknown", emptyMap())
        }
    }
    
    private fun extractTextMessageParams(input: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        // Extract phone number
        val phoneRegex = "\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b".toRegex()
        phoneRegex.find(input)?.let { params["phone"] = it.value }
        
        // Extract message (text in quotes or after "saying" or "message")
        val messageRegex = "\"([^\"]+)\"|saying (.+)|message (.+)".toRegex()
        messageRegex.find(input)?.let { match ->
            params["message"] = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
        }
        
        return params
    }
    
    private fun requestAccessibilityPermission() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // Permission request failed
        }
    }
    
    fun setAccessibilityService(service: AxiaAccessibilityService) {
        accessibilityService = service
    }
    
    fun cleanup() {
        scope.cancel()
        commandQueue.close()
    }
}

/**
 * Accessibility Service for UI automation
 */
class AxiaAccessibilityService : AccessibilityService() {
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            notificationTimeout = 100
        }
        
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }
    
    override fun onInterrupt() {
        // Service interrupted
    }
    
    fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNode = findNodeByText(rootNode, text)
        return targetNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }
    
    fun typeText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val editTextNode = findEditTextNode(rootNode)
        
        if (editTextNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            return editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        
        return false
    }
    
    fun scroll(direction: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(rootNode)
        
        return when (direction.lowercase()) {
            "up" -> scrollableNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true
            "down" -> scrollableNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true
            else -> false
        }
    }
    
    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let { findNodeByText(it, text) }?.let { return it }
        }
        
        return null
    }
    
    private fun findEditTextNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText") {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let { findEditTextNode(it) }?.let { return it }
        }
        
        return null
    }
    
    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let { findScrollableNode(it) }?.let { return it }
        }
        
        return null
    }
}

// Data classes
data class AgentIntent(
    val type: IntentType,
    val action: String,
    val parameters: Map<String, Any?>
)

data class AgentResponse(
    val success: Boolean,
    val message: String,
    val error: String? = null,
    val data: Any? = null
) {
    companion object {
        fun success(message: String, data: Any? = null) = AgentResponse(true, message, null, data)
        fun error(message: String) = AgentResponse(false, "", message)
    }
}

data class AgentCommand(
    val id: String,
    val intent: AgentIntent,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AgentState {
    IDLE, THINKING, EXECUTING, ERROR
}

enum class IntentType {
    PHONE_CONTROL,
    TERMINAL_COMMAND,
    APP_AUTOMATION,
    FILE_OPERATION,
    SYSTEM_SETTING,
    INFORMATION_QUERY,
    DEVELOPMENT_TASK,
    UNKNOWN
}