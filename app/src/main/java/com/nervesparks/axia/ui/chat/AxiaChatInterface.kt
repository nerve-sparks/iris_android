package com.nervesparks.axia.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import com.nervesparks.axia.models.*
import com.nervesparks.axia.MainViewModel
import com.nervesparks.axia.core.*

/**
 * ChatGPT-Style Interface for Axia
 * Features:
 * - Mode selection (Agent, Research, Development, etc.)
 * - Conversation search and management
 * - Project isolation
 * - Model switching
 * - Side panel with functions
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxiaChatInterface(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentInput by viewModel.currentInput.collectAsState()
    
    var selectedMode by remember { mutableStateOf(AxiaChatMode.ASSISTANT) }
    var showSidePanel by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<ChatProject?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Side Panel (when shown)
        AnimatedVisibility(
            visible = showSidePanel,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            AxiaSidePanel(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
                selectedProject = selectedProject,
                onProjectSelected = { selectedProject = it },
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onNewProject = { /* Create new project */ },
                onImportProject = { /* Import existing project */ },
                modifier = Modifier.width(320.dp)
            )
        }
        
        // Main Chat Interface
        Column(
            modifier = Modifier
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
            // Chat Header
            AxiaChatHeader(
                selectedMode = selectedMode,
                selectedProject = selectedProject,
                onModeClick = { selectedMode = it },
                onMenuClick = onMenuClick,
                onSidePanelToggle = { showSidePanel = !showSidePanel },
                showSidePanel = showSidePanel
            )
            
            // Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    AxiaWelcomeScreen(
                        selectedMode = selectedMode,
                        onModeSelected = { selectedMode = it },
                        onQuickAction = { action ->
                            viewModel.sendMessage(action)
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        reverseLayout = true
                    ) {
                        // Loading indicator
                        if (isLoading) {
                            item {
                                AxiaTypingIndicator()
                            }
                        }
                        
                        // Messages (reversed for bottom-up display)
                        items(messages.reversed()) { message ->
                            AxiaMessageBubble(
                                message = message,
                                selectedMode = selectedMode
                            )
                        }
                    }
                }
                
                // Mode indicator overlay
                AxiaModeIndicator(
                    selectedMode = selectedMode,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
            
            // Input Area
            AxiaInputArea(
                input = currentInput,
                onInputChange = viewModel::updateInput,
                onSend = { 
                    if (currentInput.isNotEmpty()) {
                        viewModel.sendMessage(currentInput)
                    }
                },
                isLoading = isLoading,
                selectedMode = selectedMode,
                onModeChange = { selectedMode = it }
            )
        }
    }
}

@Composable
private fun AxiaChatHeader(
    selectedMode: AxiaChatMode,
    selectedProject: ChatProject?,
    onModeClick: (AxiaChatMode) -> Unit,
    onMenuClick: () -> Unit,
    onSidePanelToggle: () -> Unit,
    showSidePanel: Boolean
) {
    Surface(
        color = Color(0xFF161B22).copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu button
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color(0xFFE6EDF3)
                    )
                }
                
                // Mode indicator (clickable)
                Surface(
                    onClick = { /* Show mode selector */ },
                    color = selectedMode.color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, selectedMode.color.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedMode.icon,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedMode.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = selectedMode.color,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                
                // Project indicator (if selected)
                selectedProject?.let { project ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF21262D),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "📁 ${project.name}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF8B949E)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Side panel toggle
            IconButton(onClick = onSidePanelToggle) {
                Icon(
                    if (showSidePanel) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                    contentDescription = "Toggle Side Panel",
                    tint = Color(0xFFE6EDF3)
                )
            }
        }
    }
}

@Composable
private fun AxiaSidePanel(
    selectedMode: AxiaChatMode,
    onModeSelected: (AxiaChatMode) -> Unit,
    selectedProject: ChatProject?,
    onProjectSelected: (ChatProject?) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNewProject: () -> Unit,
    onImportProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF161B22),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search conversations
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = "Search conversations...",
                        color = Color(0xFF656D76)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF656D76)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF58A6FF),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color(0xFFE6EDF3),
                    unfocusedTextColor = Color(0xFFE6EDF3)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mode Selection
            Text(
                text = "Axia Modes",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AxiaChatMode.values()) { mode ->
                    AxiaModeCard(
                        mode = mode,
                        isSelected = selectedMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Projects Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Row {
                    IconButton(onClick = onNewProject) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Project",
                            tint = Color(0xFF58A6FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onImportProject) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = "Import Project",
                            tint = Color(0xFF58A6FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Project list (placeholder)
            Text(
                text = "No projects yet",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF656D76)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AxiaModeCard(
    mode: AxiaChatMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                mode.color.copy(alpha = 0.15f)
            } else {
                Color(0xFF21262D)
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, mode.color.copy(alpha = 0.5f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mode.icon,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isSelected) mode.color else Color(0xFFE6EDF3),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF8B949E)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AxiaWelcomeScreen(
    selectedMode: AxiaChatMode,
    onModeSelected: (AxiaChatMode) -> Unit,
    onQuickAction: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Axia logo/avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF1976D2)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Hi! I'm Axia",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.Bold
            )
        )
        
        Text(
            text = selectedMode.welcomeMessage,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color(0xFF8B949E)
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Quick action suggestions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(selectedMode.quickActions) { action ->
                Card(
                    onClick = { onQuickAction(action.prompt) },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF21262D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action.icon,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFFE6EDF3),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF8B949E)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AxiaModeIndicator(
    selectedMode: AxiaChatMode,
    modifier: Modifier = Modifier
) {
    Surface(
        color = selectedMode.color.copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedMode.icon,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = selectedMode.shortName,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun AxiaMessageBubble(
    message: ChatMessage,
    selectedMode: AxiaChatMode
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        if (!message.isUser) {
            // AI avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        selectedMode.color.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedMode.icon,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Message bubble
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    Color(0xFF58A6FF)
                } else {
                    Color(0xFF21262D)
                }
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (message.isUser) Color.White else Color(0xFFE6EDF3)
                    )
                )
                
                // Message metadata
                if (!message.isUser && (message.tokensPerSecond > 0 || message.isAutonomous)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (message.tokensPerSecond > 0) {
                            Text(
                                text = "${message.tokensPerSecond.toInt()} tok/s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF656D76)
                                )
                            )
                        }
                        if (message.isAutonomous) {
                            Text(
                                text = "🤖 Autonomous",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF7EE787)
                                )
                            )
                        }
                    }
                }
            }
        }
        
        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            
            // User avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color(0xFF58A6FF).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AxiaTypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Color(0xFF58A6FF).copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF21262D)
            ),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFF8B949E).copy(alpha = alpha),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun AxiaInputArea(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    selectedMode: AxiaChatMode,
    onModeChange: (AxiaChatMode) -> Unit
) {
    Surface(
        color = Color(0xFF161B22).copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Quick mode switcher
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AxiaChatMode.values().take(4)) { mode ->
                    FilterChip(
                        onClick = { onModeChange(mode) },
                        label = {
                            Text(
                                text = "${mode.icon} ${mode.shortName}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedMode == mode,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = mode.color,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF21262D),
                            labelColor = Color(0xFF8B949E)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Input field with send button
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = selectedMode.inputPlaceholder,
                            color = Color(0xFF656D76)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedMode.color,
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = selectedMode.color,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Data classes and enums
enum class AxiaChatMode(
    val title: String,
    val shortName: String,
    val icon: String,
    val description: String,
    val color: Color,
    val welcomeMessage: String,
    val inputPlaceholder: String,
    val quickActions: List<QuickAction>
) {
    ASSISTANT(
        title = "AI Assistant",
        shortName = "Assistant",
        icon = "🤝",
        description = "General help and conversation",
        color = Color(0xFF58A6FF),
        welcomeMessage = "I'm here to help with anything you need. Ask me questions, get suggestions, or just chat!",
        inputPlaceholder = "Ask me anything...",
        quickActions = listOf(
            QuickAction("⏰", "Set Timer", "Set a 25-minute timer", "Set a timer for 25 minutes"),
            QuickAction("📱", "Call Contact", "Make a phone call", "Call John"),
            QuickAction("📝", "Send Text", "Send a text message", "Text Sarah that I'm running late"),
            QuickAction("🔍", "Quick Search", "Search for information", "What's the weather today?")
        )
    ),
    
    AUTONOMOUS_AGENT(
        title = "Autonomous Agent",
        shortName = "Agent",
        icon = "🤖",
        description = "AI that takes actions and controls your phone",
        color = Color(0xFF7EE787),
        welcomeMessage = "I can autonomously control your device and execute tasks. Tell me what you want done, and I'll handle it!",
        inputPlaceholder = "What should I do for you?",
        quickActions = listOf(
            QuickAction("📶", "WiFi Control", "Toggle WiFi settings", "Turn off WiFi and enable mobile data"),
            QuickAction("📞", "Auto Call", "Make calls automatically", "Call my doctor and ask for appointment availability"),
            QuickAction("🏠", "Smart Home", "Control smart devices", "Turn off all lights and lock the front door"),
            QuickAction("📧", "Email Management", "Handle emails", "Reply to all urgent emails from today")
        )
    ),
    
    DEEP_RESEARCH(
        title = "Deep Research",
        shortName = "Research",
        icon = "🔬",
        description = "Comprehensive research with web search",
        color = Color(0xFFFF9800),
        welcomeMessage = "I'll conduct deep research using multiple sources to give you comprehensive answers with citations.",
        inputPlaceholder = "What would you like me to research?",
        quickActions = listOf(
            QuickAction("🔬", "Tech Research", "Research latest technology", "Research latest AI developments in 2025"),
            QuickAction("📊", "Market Analysis", "Analyze market trends", "Analyze the current state of the mobile app market"),
            QuickAction("🏥", "Health Info", "Research health topics", "Research the benefits and risks of intermittent fasting"),
            QuickAction("💼", "Business Intel", "Business research", "Research competitors in the fintech space")
        )
    ),
    
    DEVELOPMENT(
        title = "Development Mode",
        shortName = "Dev",
        icon = "👨‍💻",
        description = "Android development and coding assistance",
        color = Color(0xFF4CAF50),
        welcomeMessage = "I'm your Android development partner. I can write code, debug issues, suggest architectures, and help build apps!",
        inputPlaceholder = "What do you want to build or fix?",
        quickActions = listOf(
            QuickAction("📱", "New App", "Create a new Android app", "Create a new expense tracking app with Material Design"),
            QuickAction("🐛", "Debug Code", "Fix bugs and issues", "Debug this crash in my MainActivity"),
            QuickAction("🏗️", "Architecture", "Design app architecture", "Design MVVM architecture for my social media app"),
            QuickAction("🚀", "Optimize", "Improve performance", "Optimize my app for better battery usage")
        )
    ),
    
    SYSTEM_CONTROL(
        title = "System Control",
        shortName = "System",
        icon = "⚙️",
        description = "Direct phone and system management",
        color = Color(0xFFE91E63),
        welcomeMessage = "I can directly control your phone's systems, settings, and functions. No limitations!",
        inputPlaceholder = "What system function do you need?",
        quickActions = listOf(
            QuickAction("📡", "Network", "Manage network settings", "Switch to 5G and forget old WiFi networks"),
            QuickAction("🔋", "Battery", "Optimize battery", "Enable battery saver and close background apps"),
            QuickAction("🔧", "Settings", "Change system settings", "Enable dark mode and increase font size"),
            QuickAction("🧹", "Cleanup", "Clean system", "Clear cache, organize files, and free up storage")
        )
    ),
    
    IMAGE_GENERATION(
        title = "Image Generation",
        shortName = "Images",
        icon = "🎨",
        description = "Local AI image creation and editing",
        color = Color(0xFF9C27B0),
        welcomeMessage = "I can generate images locally on your device using AI. No internet required for creating artwork!",
        inputPlaceholder = "Describe the image you want to create...",
        quickActions = listOf(
            QuickAction("🎨", "Create Art", "Generate artistic images", "Create a cyberpunk cityscape at night"),
            QuickAction("👤", "Portrait", "Generate portraits", "Create a professional headshot of a business person"),
            QuickAction("🏞️", "Landscape", "Generate landscapes", "Create a serene mountain lake at sunrise"),
            QuickAction("🏠", "Architecture", "Generate buildings", "Design a modern minimalist house exterior")
        )
    )
}

data class QuickAction(
    val icon: String,
    val title: String,
    val description: String,
    val prompt: String
)

data class ChatProject(
    val id: String,
    val name: String,
    val type: ProjectType,
    val createdAt: Long,
    val lastModified: Long,
    val description: String
)

enum class ProjectType {
    ANDROID_APP, WEB_PROJECT, RESEARCH, GENERAL, SYSTEM_AUTOMATION
}