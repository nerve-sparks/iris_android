package com.nervesparks.axia

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.content.getSystemService
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.File

// Axia imports
import com.nervesparks.axia.memory.*
import com.nervesparks.axia.autonomous.*
import com.nervesparks.axia.monitoring.*
import com.nervesparks.axia.generation.*
import com.nervesparks.axia.ui.agent.*
import com.nervesparks.axia.ui.monitoring.*
import com.nervesparks.axia.ui.generation.*
import com.nervesparks.axia.ui.chat.*
import com.nervesparks.axia.ui.theme.*
import com.nervesparks.axia.data.*

/**
 * Main Activity for Axia - Advanced Autonomous AI Assistant
 * Features: Advanced memory management, autonomous operation, system control,
 * local image generation, performance monitoring, and unrestricted AI capabilities
 */

class AxiaViewModelFactory(
    private val llamaAndroid: LLamaAndroid,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val memoryManager: AxiaMemoryManager,
    private val autonomousAgent: AutonomousAgent,
    private val performanceTracker: PerformanceTracker,
    private val imageGenerator: ImageGenerator
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MainViewModel::class.java -> MainViewModel(
                llamaAndroid, 
                userPreferencesRepository, 
                memoryManager, 
                autonomousAgent, 
                performanceTracker
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

class MainActivity : ComponentActivity() {

    private val downloadManager by lazy { getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { getSystemService<ClipboardManager>()!! }

    private lateinit var viewModel: MainViewModel
    private lateinit var memoryManager: AxiaMemoryManager
    private lateinit var autonomousAgent: AutonomousAgent
    private lateinit var performanceTracker: PerformanceTracker
    private lateinit var imageGenerator: ImageGenerator

    // Axia brand colors for sophisticated dark theme
    private val axiaDarkGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0A0E1A), // Deep space blue
            Color(0xFF1A1F2E), // Midnight blue
            Color(0xFF2D1B3D)  // Dark purple accent
        )
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        setupAxiaOptimizations()
        initializeAxiaComponents()
        
        setContent {
            AxiaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    AxiaMainInterface()
                }
            }
        }
    }

    private fun setupAxiaOptimizations() {
        // Advanced performance settings for Axia
        window.apply {
            // Keep screen on for continuous operation
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Optimize for performance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = 
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        
        // Enhanced StrictMode for AI operations
        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectFileUriExposure()
                .detectCleartextNetwork()
                .detectNonSdkApiUsage()
                .penaltyLog()
                .build()
        )
        
        // Set status bar for dark theme
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Set navigation bar color
        window.navigationBarColor = Color(0xFF0A0E1A).toArgb()
        window.statusBarColor = Color.Transparent.toArgb()
    }

    private fun initializeAxiaComponents() {
        // Initialize advanced memory management
        memoryManager = AxiaMemoryManager(this)
        
        // Initialize performance monitoring
        performanceTracker = PerformanceTracker(this)
        
        // Initialize image generation
        imageGenerator = ImageGenerator(this)
        
        // Initialize autonomous agent
        autonomousAgent = AutonomousAgent(
            context = this,
            memoryManager = memoryManager,
            performanceTracker = performanceTracker
        )
        
        // Initialize repositories
        val userPrefsRepo = UserPreferencesRepository.getInstance(applicationContext)
        val llamaAndroid = LLamaAndroid.instance()
        
        // Create view model with all Axia components
        val viewModelFactory = AxiaViewModelFactory(
            llamaAndroid, 
            userPrefsRepo, 
            memoryManager, 
            autonomousAgent, 
            performanceTracker, 
            imageGenerator
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        
        // Start performance monitoring
        performanceTracker.startMonitoring()
        
        // Initialize memory management
        memoryManager.initializeMemorySystem()
    }

    @Composable
    private fun AxiaMainInterface() {
        var selectedTab by remember { mutableStateOf(AxiaTab.CHAT) }
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Main navigation drawer
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AxiaNavigationDrawer(
                    selectedTab = selectedTab,
                    onTabSelected = { 
                        selectedTab = it
                        scope.launch { drawerState.close() }
                    },
                    autonomousAgent = autonomousAgent,
                    performanceTracker = performanceTracker
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(axiaDarkGradient)
            ) {
                // Main content based on selected tab
                when (selectedTab) {
                    AxiaTab.CHAT -> {
                        AxiaChatInterface(
                            viewModel = viewModel,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    }
                    AxiaTab.AUTONOMOUS_AGENT -> {
                        AutonomousAgentUI(
                            autonomousAgent = autonomousAgent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AxiaTab.PERFORMANCE -> {
                        PerformanceDashboard(
                            performanceTracker = performanceTracker,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AxiaTab.IMAGE_GENERATION -> {
                        ImageGenerationUI(
                            imageGenerator = imageGenerator,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AxiaTab.MEMORY_MANAGER -> {
                        MemoryManagerUI(
                            memoryManager = memoryManager,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AxiaTab.SYSTEM_CONTROL -> {
                        SystemControlUI(
                            autonomousAgent = autonomousAgent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Floating Action Button for quick autonomous commands
                if (selectedTab == AxiaTab.CHAT) {
                    AxiaFloatingActionButton(
                        autonomousAgent = autonomousAgent,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun AxiaNavigationDrawer(
        selectedTab: AxiaTab,
        onTabSelected: (AxiaTab) -> Unit,
        autonomousAgent: AutonomousAgent,
        performanceTracker: PerformanceTracker
    ) {
        val agentState by autonomousAgent.agentState.collectAsState()
        val performanceMetrics by performanceTracker.performanceMetrics.collectAsState()
        
        ModalDrawerSheet(
            modifier = Modifier.width(320.dp),
            drawerContainerColor = Color(0xFF161B22)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Axia Header
                AxiaHeader(agentState, performanceMetrics)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Navigation items
                AxiaTab.values().forEach { tab ->
                    AxiaNavigationItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { onTabSelected(tab) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Quick stats
                AxiaQuickStats(performanceMetrics, agentState)
            }
        }
    }

    @Composable
    private fun AxiaHeader(
        agentState: AgentState,
        performanceMetrics: PerformanceMetrics
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Axia logo/avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (agentState.isActive) {
                                Brush.radialGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF1976D2))
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(Color(0xFF656D76), Color(0xFF30363D))
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Axia",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color(0xFFE6EDF3),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Autonomous AI Assistant",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF8B949E)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(
                    label = if (agentState.isActive) "ACTIVE" else "STANDBY",
                    color = if (agentState.isActive) Color(0xFF4CAF50) else Color(0xFF656D76)
                )
                
                StatusChip(
                    label = "${(performanceMetrics.ramUsage.usagePercent).toInt()}% RAM",
                    color = when {
                        performanceMetrics.ramUsage.usagePercent > 90 -> Color(0xFFFF5722)
                        performanceMetrics.ramUsage.usagePercent > 75 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }
                )
            }
        }
    }

    @Composable
    private fun StatusChip(label: String, color: Color) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    @Composable
    private fun AxiaNavigationItem(
        tab: AxiaTab,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            color = if (isSelected) {
                Color(0xFF58A6FF).copy(alpha = 0.15f)
            } else {
                Color.Transparent
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.icon,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isSelected) {
                            Color(0xFF58A6FF)
                        } else {
                            Color(0xFFE6EDF3)
                        },
                        fontWeight = if (isSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                )
            }
        }
    }

    @Composable
    private fun AxiaQuickStats(
        performanceMetrics: PerformanceMetrics,
        agentState: AgentState
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF21262D)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "⚡ System Status",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                QuickStatRow(
                    label = "CPU",
                    value = "${performanceMetrics.cpuUsagePercent.usagePercent.toInt()}%"
                )
                QuickStatRow(
                    label = "RAM",
                    value = "${performanceMetrics.ramUsage.availableRAM / (1024 * 1024 * 1024)}GB"
                )
                QuickStatRow(
                    label = "Tasks",
                    value = "${agentState.activeTasksCount}"
                )
            }
        }
    }

    @Composable
    private fun QuickStatRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8B949E)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF58A6FF),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun AxiaFloatingActionButton(
        autonomousAgent: AutonomousAgent,
        modifier: Modifier = Modifier
    ) {
        val agentState by autonomousAgent.agentState.collectAsState()
        
        FloatingActionButton(
            onClick = {
                if (agentState.isActive) {
                    autonomousAgent.pauseAgent()
                } else {
                    autonomousAgent.activateAgent()
                }
            },
            containerColor = if (agentState.isActive) {
                Color(0xFFFF5722)
            } else {
                Color(0xFF4CAF50)
            },
            modifier = modifier
        ) {
            Text(
                text = if (agentState.isActive) "⏸️" else "▶️",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    // Placeholder composables (to be implemented)
    @Composable private fun AxiaChatInterface(viewModel: MainViewModel, onMenuClick: () -> Unit) {}
    @Composable private fun MemoryManagerUI(memoryManager: AxiaMemoryManager, modifier: Modifier) {}
    @Composable private fun SystemControlUI(autonomousAgent: AutonomousAgent, modifier: Modifier) {}

    override fun onDestroy() {
        super.onDestroy()
        // Clean shutdown of Axia components
        performanceTracker.stopMonitoring()
        memoryManager.cleanup()
        autonomousAgent.shutdown()
    }
}

enum class AxiaTab(val title: String, val icon: String) {
    CHAT("Chat", "💬"),
    AUTONOMOUS_AGENT("Autonomous Agent", "🤖"),
    PERFORMANCE("Performance", "📊"),
    IMAGE_GENERATION("Image Generation", "🎨"),
    MEMORY_MANAGER("Memory Manager", "🧠"),
    SYSTEM_CONTROL("System Control", "⚙️")
}