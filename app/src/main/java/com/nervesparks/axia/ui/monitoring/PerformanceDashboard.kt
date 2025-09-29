package com.nervesparks.axia.ui.monitoring

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.nervesparks.axia.monitoring.*
import kotlin.math.*

/**
 * Live Performance Dashboard for Axia
 * Real-time monitoring of all system and AI metrics
 */

@Composable
fun PerformanceDashboard(
    performanceTracker: PerformanceTracker,
    modifier: Modifier = Modifier
) {
    val performanceMetrics by performanceTracker.performanceMetrics.collectAsState()
    val aiMetrics by performanceTracker.aiMetrics.collectAsState()
    
    var selectedTab by remember { mutableStateOf(DashboardTab.OVERVIEW) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Dashboard Header
        DashboardHeader()
        
        // Tab Navigation
        DashboardTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
        
        // Content based on selected tab
        when (selectedTab) {
            DashboardTab.OVERVIEW -> {
                OverviewDashboard(performanceMetrics, aiMetrics)
            }
            DashboardTab.AI_METRICS -> {
                AIMetricsDashboard(aiMetrics)
            }
            DashboardTab.SYSTEM -> {
                SystemDashboard(performanceMetrics)
            }
            DashboardTab.MEMORY -> {
                MemoryDashboard(performanceMetrics)
            }
            DashboardTab.PERFORMANCE -> {
                PerformanceDashboard(performanceMetrics, aiMetrics)
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Surface(
        color = Color(0xFF161B22),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "⚡ Axia Performance Monitor",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Live System & AI Metrics",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF8B949E)
                    )
                )
            }
            
            // Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun DashboardTabs(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color(0xFF161B22),
        contentColor = Color(0xFFE6EDF3),
        edgePadding = 16.dp
    ) {
        DashboardTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = "${tab.icon} ${tab.title}",
                        color = if (selectedTab == tab) Color(0xFF58A6FF) else Color(0xFF8B949E)
                    )
                }
            )
        }
    }
}

@Composable
private fun OverviewDashboard(
    performanceMetrics: PerformanceMetrics,
    aiMetrics: AIMetrics
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Critical Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CriticalMetricCard(
                    title = "AI Status",
                    value = "ACTIVE",
                    subtitle = "${aiMetrics.tokensPerSecond.toInt()} tok/s",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                
                CriticalMetricCard(
                    title = "Memory",
                    value = "${(performanceMetrics.ramUsage.usagePercent).toInt()}%",
                    subtitle = "${performanceMetrics.ramUsage.availableRAM / (1024 * 1024 * 1024)}GB free",
                    color = getMemoryColor(performanceMetrics.ramUsage.usagePercent),
                    modifier = Modifier.weight(1f)
                )
                
                CriticalMetricCard(
                    title = "Context",
                    value = "${aiMetrics.contextLength}",
                    subtitle = "${(aiMetrics.contextUtilization * 100).toInt()}% used",
                    color = Color(0xFF58A6FF),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Real-time Performance Graph
        item {
            PerformanceGraphCard(performanceMetrics, aiMetrics)
        }
        
        // Drift & Quality Monitoring
        item {
            DriftMonitoringCard(aiMetrics)
        }
        
        // Quick System Stats
        item {
            SystemStatsCard(performanceMetrics)
        }
    }
}

@Composable
private fun AIMetricsDashboard(aiMetrics: AIMetrics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Token Performance
        item {
            TokenPerformanceCard(aiMetrics)
        }
        
        // Context Management
        item {
            ContextManagementCard(aiMetrics)
        }
        
        // Model Quality Metrics
        item {
            ModelQualityCard(aiMetrics)
        }
        
        // Drift Detection
        item {
            DriftDetectionCard(aiMetrics)
        }
    }
}

@Composable
private fun CriticalMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF8B949E)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF656D76)
                )
            )
        }
    }
}

@Composable
private fun PerformanceGraphCard(
    performanceMetrics: PerformanceMetrics,
    aiMetrics: AIMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🔥 Real-time Performance",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Real-time performance visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color(0xFF0D1117),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                // Performance graph visualization would go here
                PerformanceGraph(performanceMetrics, aiMetrics)
            }
        }
    }
}

@Composable
private fun PerformanceGraph(
    performanceMetrics: PerformanceMetrics,
    aiMetrics: AIMetrics
) {
    // Animated performance graph
    val cpuUsage = performanceMetrics.cpuUsagePercent.usagePercent.toFloat()
    val memoryUsage = performanceMetrics.ramUsage.usagePercent.toFloat()
    val tokenRate = (aiMetrics.tokensPerSecond / 100).toFloat().coerceAtMost(1f)
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        PerformanceBar(
            label = "CPU",
            value = cpuUsage / 100f,
            color = Color(0xFFFF9800),
            unit = "${cpuUsage.toInt()}%"
        )
        
        PerformanceBar(
            label = "RAM", 
            value = memoryUsage / 100f,
            color = Color(0xFF2196F3),
            unit = "${memoryUsage.toInt()}%"
        )
        
        PerformanceBar(
            label = "Tokens/s",
            value = tokenRate,
            color = Color(0xFF4CAF50),
            unit = "${aiMetrics.tokensPerSecond.toInt()}"
        )
    }
}

@Composable
private fun PerformanceBar(
    label: String,
    value: Float,
    color: Color,
    unit: String
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(1000)
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF8B949E),
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.width(80.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(
                    Color(0xFF21262D),
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedValue.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        ),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
        
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall.copy(
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .width(60.dp)
                .padding(start = 12.dp)
        )
    }
}

@Composable
private fun DriftMonitoringCard(aiMetrics: AIMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🎯 Context Quality Monitor",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Drift indicators
            DriftIndicator(
                label = "Semantic Drift",
                value = aiMetrics.semanticDrift,
                threshold = 0.7,
                format = "%.3f"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DriftIndicator(
                label = "Context Drift", 
                value = aiMetrics.contextualDrift,
                threshold = 0.8,
                format = "%.3f"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DriftIndicator(
                label = "Hallucination Risk",
                value = aiMetrics.hallucinationRisk,
                threshold = 0.3,
                format = "%.1f%%",
                multiplier = 100.0
            )
        }
    }
}

@Composable
private fun DriftIndicator(
    label: String,
    value: Double,
    threshold: Double,
    format: String,
    multiplier: Double = 1.0
) {
    val displayValue = value * multiplier
    val isWarning = value > threshold
    val color = if (isWarning) Color(0xFFFF6B6B) else Color(0xFF4CAF50)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFE6EDF3)
            )
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = format.format(displayValue),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            
            if (isWarning) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TokenPerformanceCard(aiMetrics: AIMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🚀 Token Performance",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Tokens/Second",
                    value = "${aiMetrics.tokensPerSecond.toInt()}",
                    color = Color(0xFF58A6FF)
                )
                
                MetricItem(
                    label = "Total Processed",
                    value = "${aiMetrics.totalTokensProcessed / 1000}K",
                    color = Color(0xFF7EE787)
                )
                
                MetricItem(
                    label = "Efficiency",
                    value = "${(aiMetrics.tokenEfficiency * 100).toInt()}%",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun ContextManagementCard(aiMetrics: AIMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🧠 Context Management",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Context utilization bar
            val contextUtilization = aiMetrics.contextUtilization.toFloat()
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Context Utilization",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF8B949E)
                        )
                    )
                    Text(
                        text = "${(contextUtilization * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF58A6FF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = contextUtilization,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF58A6FF),
                    trackColor = Color(0xFF21262D)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Length",
                    value = "${aiMetrics.contextLength}",
                    color = Color(0xFF58A6FF)
                )
                
                MetricItem(
                    label = "Coherence",
                    value = "${(aiMetrics.contextCoherence * 100).toInt()}%",
                    color = Color(0xFF4CAF50)
                )
                
                MetricItem(
                    label = "Memory",
                    value = "${aiMetrics.contextMemoryUsage / (1024 * 1024)}MB",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF8B949E)
            )
        )
    }
}

@Composable 
private fun ModelQualityCard(aiMetrics: AIMetrics) {
    // Implementation for model quality metrics
}

@Composable
private fun DriftDetectionCard(aiMetrics: AIMetrics) {
    // Implementation for detailed drift detection
}

@Composable
private fun SystemDashboard(performanceMetrics: PerformanceMetrics) {
    // Implementation for system dashboard
}

@Composable
private fun MemoryDashboard(performanceMetrics: PerformanceMetrics) {
    // Implementation for memory dashboard
}

@Composable
private fun SystemStatsCard(performanceMetrics: PerformanceMetrics) {
    // Implementation for system stats
}

private fun getMemoryColor(usagePercent: Double): Color {
    return when {
        usagePercent > 90 -> Color(0xFFFF5722) // Critical
        usagePercent > 75 -> Color(0xFFFF9800) // Warning
        else -> Color(0xFF4CAF50) // Good
    }
}

enum class DashboardTab(val title: String, val icon: String) {
    OVERVIEW("Overview", "📊"),
    AI_METRICS("AI Metrics", "🤖"),
    SYSTEM("System", "⚙️"),
    MEMORY("Memory", "🧠"),
    PERFORMANCE("Performance", "⚡")
}