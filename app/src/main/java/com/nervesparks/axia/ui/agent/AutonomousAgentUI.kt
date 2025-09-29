package com.nervesparks.axia.ui.agent

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import com.nervesparks.axia.autonomous.*
import com.nervesparks.axia.models.*

/**
 * Autonomous Agent Control Interface for Axia
 * Provides full control and monitoring of the autonomous AI agent
 */

@Composable
fun AutonomousAgentUI(
    autonomousAgent: AutonomousAgent,
    modifier: Modifier = Modifier
) {
    val agentState by autonomousAgent.agentState.collectAsState()
    val activeCommands by autonomousAgent.activeCommands.collectAsState()
    val agentCapabilities by autonomousAgent.capabilities.collectAsState()
    
    var selectedMode by remember { mutableStateOf(AgentMode.ASSISTANT) }
    var autonomyLevel by remember { mutableStateOf(AutonomyLevel.CONTROLLED) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0E1A),
                        Color(0xFF1A1F2E)
                    )
                )
            )
    ) {
        // Agent Status Header
        AgentStatusHeader(
            agentState = agentState,
            autonomousAgent = autonomousAgent
        )
        
        // Agent Control Panel
        AgentControlPanel(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            autonomyLevel = autonomyLevel,
            onAutonomyLevelChanged = { autonomyLevel = it },
            autonomousAgent = autonomousAgent
        )
        
        // Main Content based on selected mode
        when (selectedMode) {
            AgentMode.ASSISTANT -> {
                AssistantMode(
                    autonomousAgent = autonomousAgent,
                    agentState = agentState,
                    activeCommands = activeCommands
                )
            }
            AgentMode.AUTONOMOUS -> {
                AutonomousMode(
                    autonomousAgent = autonomousAgent,
                    agentState = agentState,
                    activeCommands = activeCommands
                )
            }
            AgentMode.SYSTEM_CONTROL -> {
                SystemControlMode(
                    autonomousAgent = autonomousAgent,
                    capabilities = agentCapabilities
                )
            }
            AgentMode.TERMINAL -> {
                TerminalMode(
                    autonomousAgent = autonomousAgent
                )
            }
            AgentMode.MONITORING -> {
                MonitoringMode(
                    autonomousAgent = autonomousAgent,
                    agentState = agentState
                )
            }
        }
    }
}

@Composable
private fun AgentStatusHeader(
    agentState: AgentState,
    autonomousAgent: AutonomousAgent
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF161B22).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Agent Identity
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated agent avatar
                AgentAvatar(agentState.isActive)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "⚡ Axia Autonomous Agent",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color(0xFFE6EDF3),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Text(
                        text = getStatusDescription(agentState),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = getStatusColor(agentState)
                        )
                    )
                }
            }
            
            // Quick Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AgentQuickAction(
                    icon = if (agentState.isActive) "⏸️" else "▶️",
                    label = if (agentState.isActive) "Pause" else "Activate",
                    onClick = {
                        if (agentState.isActive) {
                            autonomousAgent.pauseAgent()
                        } else {
                            autonomousAgent.activateAgent()
                        }
                    }
                )
                
                AgentQuickAction(
                    icon = "🔄",
                    label = "Reset",
                    onClick = { autonomousAgent.resetAgent() }
                )
                
                AgentQuickAction(
                    icon = "⚙️",
                    label = "Config",
                    onClick = { /* Open configuration */ }
                )
            }
        }
    }
}

@Composable
private fun AgentAvatar(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        )
    )
    
    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Color(0xFF58A6FF).copy(alpha = glowAlpha * 0.3f),
                        CircleShape
                    )
                    .rotate(rotation)
            )
        }
        
        // Agent avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isActive) {
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF58A6FF),
                                Color(0xFF1F6FEB)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF656D76),
                                Color(0xFF30363D)
                            )
                        )
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun AgentQuickAction(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(width = 60.dp, height = 40.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AgentControlPanel(
    selectedMode: AgentMode,
    onModeSelected: (AgentMode) -> Unit,
    autonomyLevel: AutonomyLevel,
    onAutonomyLevelChanged: (AutonomyLevel) -> Unit,
    autonomousAgent: AutonomousAgent
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Mode Selection
            Text(
                text = "🎯 Agent Mode",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AgentMode.values()) { mode ->
                    ModeChip(
                        mode = mode,
                        isSelected = selectedMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Autonomy Level Control
            Text(
                text = "⚡ Autonomy Level",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AutonomyLevelSlider(
                autonomyLevel = autonomyLevel,
                onAutonomyLevelChanged = onAutonomyLevelChanged,
                onApply = { autonomousAgent.setAutonomyLevel(it) }
            )
        }
    }
}

@Composable
private fun ModeChip(
    mode: AgentMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = "${mode.icon} ${mode.title}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
        },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF58A6FF),
            selectedLabelColor = Color(0xFF0D1117),
            containerColor = Color(0xFF21262D),
            labelColor = Color(0xFF8B949E)
        )
    )
}

@Composable
private fun AutonomyLevelSlider(
    autonomyLevel: AutonomyLevel,
    onAutonomyLevelChanged: (AutonomyLevel) -> Unit,
    onApply: (AutonomyLevel) -> Unit
) {
    val levels = AutonomyLevel.values()
    val currentIndex = levels.indexOf(autonomyLevel)
    
    Column {
        // Slider
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { newValue ->
                val newLevel = levels[newValue.toInt().coerceIn(0, levels.size - 1)]
                onAutonomyLevelChanged(newLevel)
            },
            valueRange = 0f..(levels.size - 1).toFloat(),
            steps = levels.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58A6FF),
                activeTrackColor = Color(0xFF58A6FF),
                inactiveTrackColor = Color(0xFF30363D)
            )
        )
        
        // Level descriptions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            levels.forEach { level ->
                Text(
                    text = level.shortName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (level == autonomyLevel) {
                            Color(0xFF58A6FF)
                        } else {
                            Color(0xFF656D76)
                        },
                        fontWeight = if (level == autonomyLevel) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Current level description
        Text(
            text = autonomyLevel.description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF8B949E)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Apply button
        Button(
            onClick = { onApply(autonomyLevel) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF238636)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Apply Autonomy Level",
                color = Color.White
            )
        }
    }
}

@Composable
private fun AssistantMode(
    autonomousAgent: AutonomousAgent,
    agentState: AgentState,
    activeCommands: List<AgentCommand>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Assistant Actions
        item {
            QuickAssistantActions(autonomousAgent)
        }
        
        // Active Commands
        item {
            ActiveCommandsCard(activeCommands)
        }
        
        // Assistant Capabilities
        item {
            AssistantCapabilitiesCard()
        }
    }
}

@Composable
private fun AutonomousMode(
    autonomousAgent: AutonomousAgent,
    agentState: AgentState,
    activeCommands: List<AgentCommand>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Autonomous Task Queue
        item {
            AutonomousTaskQueue(autonomousAgent, activeCommands)
        }
        
        // Learning & Adaptation
        item {
            LearningAdaptationCard(agentState)
        }
        
        // Autonomous Metrics
        item {
            AutonomousMetricsCard(agentState)
        }
    }
}

@Composable
private fun SystemControlMode(
    autonomousAgent: AutonomousAgent,
    capabilities: AgentCapabilities
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // System Control Panel
        item {
            SystemControlPanel(autonomousAgent)
        }
        
        // Device Control
        item {
            DeviceControlCard(autonomousAgent)
        }
        
        // Network Control
        item {
            NetworkControlCard(autonomousAgent)
        }
        
        // App Management
        item {
            AppManagementCard(autonomousAgent)
        }
    }
}

@Composable
private fun TerminalMode(
    autonomousAgent: AutonomousAgent
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Terminal Interface
        TerminalInterface(autonomousAgent)
    }
}

@Composable
private fun MonitoringMode(
    autonomousAgent: AutonomousAgent,
    agentState: AgentState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Agent Performance Metrics
        item {
            AgentPerformanceCard(agentState)
        }
        
        // Decision Analytics
        item {
            DecisionAnalyticsCard(agentState)
        }
        
        // Learning Progress
        item {
            LearningProgressCard(agentState)
        }
    }
}

// Placeholder implementations for complex components
@Composable private fun QuickAssistantActions(autonomousAgent: AutonomousAgent) {}
@Composable private fun ActiveCommandsCard(activeCommands: List<AgentCommand>) {}
@Composable private fun AssistantCapabilitiesCard() {}
@Composable private fun AutonomousTaskQueue(autonomousAgent: AutonomousAgent, activeCommands: List<AgentCommand>) {}
@Composable private fun LearningAdaptationCard(agentState: AgentState) {}
@Composable private fun AutonomousMetricsCard(agentState: AgentState) {}
@Composable private fun SystemControlPanel(autonomousAgent: AutonomousAgent) {}
@Composable private fun DeviceControlCard(autonomousAgent: AutonomousAgent) {}
@Composable private fun NetworkControlCard(autonomousAgent: AutonomousAgent) {}
@Composable private fun AppManagementCard(autonomousAgent: AutonomousAgent) {}
@Composable private fun TerminalInterface(autonomousAgent: AutonomousAgent) {}
@Composable private fun AgentPerformanceCard(agentState: AgentState) {}
@Composable private fun DecisionAnalyticsCard(agentState: AgentState) {}
@Composable private fun LearningProgressCard(agentState: AgentState) {}

// Helper functions
private fun getStatusDescription(agentState: AgentState): String {
    return when {
        agentState.isActive && agentState.isLearning -> "Active • Learning Mode"
        agentState.isActive -> "Active • Standby"
        agentState.isPaused -> "Paused"
        else -> "Inactive"
    }
}

private fun getStatusColor(agentState: AgentState): Color {
    return when {
        agentState.isActive && agentState.isLearning -> Color(0xFF7EE787)
        agentState.isActive -> Color(0xFF58A6FF)
        agentState.isPaused -> Color(0xFFFF9800)
        else -> Color(0xFF656D76)
    }
}

// Enums
enum class AgentMode(val title: String, val icon: String) {
    ASSISTANT("Assistant", "🤝"),
    AUTONOMOUS("Autonomous", "🧠"),
    SYSTEM_CONTROL("System", "⚙️"),
    TERMINAL("Terminal", "💻"),
    MONITORING("Monitoring", "📊")
}

enum class AutonomyLevel(val title: String, val shortName: String, val description: String) {
    CONTROLLED("Controlled", "Control", "Agent requires explicit permission for all actions"),
    SUPERVISED("Supervised", "Super", "Agent can perform safe actions autonomously"),
    AUTONOMOUS("Autonomous", "Auto", "Agent operates independently with minimal oversight"),
    UNRESTRICTED("Unrestricted", "Full", "Complete autonomy - agent has full system access")
}