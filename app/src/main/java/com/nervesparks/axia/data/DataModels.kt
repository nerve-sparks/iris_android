package com.nervesparks.axia.data

import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable

/**
 * Core Data Models for Axia
 * Shared data structures used across the application
 */

@Immutable
@Serializable
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val conversationId: String,
    val tokensPerSecond: Double = 0.0,
    val isAutonomous: Boolean = false,
    val metadata: MessageMetadata? = null
)

@Serializable
data class MessageMetadata(
    val modelUsed: String? = null,
    val tokensUsed: Int = 0,
    val responseTime: Long = 0,
    val cost: Double = 0.0,
    val isEncrypted: Boolean = false,
    val attachments: List<String> = emptyList()
)

@Serializable
data class ResearchResults(
    val query: String,
    val summary: String,
    val sources: List<ResearchSource>,
    val keyPoints: List<String>,
    val confidence: Float,
    val timestamp: Long,
    val isEncrypted: Boolean = false
)

@Serializable  
data class ResearchSource(
    val url: String,
    val title: String,
    val snippet: String,
    val relevanceScore: Float,
    val isReliable: Boolean = true
)

@Serializable
data class AutonomousTask(
    val id: String,
    val description: String,
    val type: TaskType,
    val priority: TaskPriority,
    val status: TaskStatus,
    val createdAt: Long,
    val completedAt: Long? = null,
    val result: String? = null,
    val error: String? = null
)

@Serializable
data class DeviceAction(
    val id: String,
    val type: ActionType,
    val parameters: Map<String, String>,
    val description: String,
    val requiresConfirmation: Boolean = false,
    val isReversible: Boolean = false
)

enum class TaskType {
    PHONE_CALL, TEXT_MESSAGE, EMAIL, WEB_SEARCH, APP_CONTROL, 
    SYSTEM_SETTING, FILE_OPERATION, RESEARCH, DEVELOPMENT, IMAGE_GENERATION
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
}

enum class ActionType {
    CALL, SMS, EMAIL, WIFI_TOGGLE, BLUETOOTH_TOGGLE, VOLUME_CHANGE,
    APP_LAUNCH, WEB_NAVIGATE, FILE_CREATE, FILE_DELETE, SETTING_CHANGE
}

// UI State Models
@Immutable
data class UIState(
    val isLoading: Boolean = false,
    val currentMode: AxiaMode = AxiaMode.ASSISTANT,
    val activeConversationId: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val performanceMetrics: PerformanceMetrics? = null
)

@Immutable
data class PerformanceMetrics(
    val cpuUsage: Float,
    val memoryUsage: Float,
    val temperature: Float,
    val batteryLevel: Int,
    val networkLatency: Long,
    val modelResponseTime: Long
)

enum class AxiaMode {
    ASSISTANT, AUTONOMOUS_AGENT, DEEP_RESEARCH, DEVELOPMENT, SYSTEM_CONTROL, IMAGE_GENERATION
}

enum class ConnectionStatus {
    CONNECTED, CONNECTING, DISCONNECTED, ERROR
}