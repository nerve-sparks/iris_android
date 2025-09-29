package com.nervesparks.iris.benchmark

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.nervesparks.iris.performance.ThermalPerformanceManager
import com.nervesparks.iris.streaming.StreamingTokenManager

/**
 * Benchmark overlay showing performance metrics
 * Battery % + Temperature + Tokens/sec like ChatGPT
 */

@Composable
fun BenchmarkOverlay(
    thermalManager: ThermalPerformanceManager,
    streamingManager: StreamingTokenManager,
    isVisible: Boolean = true,
    position: BenchmarkPosition = BenchmarkPosition.TOP_RIGHT
) {
    if (!isVisible) return
    
    val thermalState by thermalManager.thermalState.collectAsState()
    val performanceMetrics by thermalManager.performanceMetrics.collectAsState()
    val streamingStats = streamingManager.getStreamingStats()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = when (position) {
            BenchmarkPosition.TOP_LEFT -> Alignment.TopStart
            BenchmarkPosition.TOP_RIGHT -> Alignment.TopEnd
            BenchmarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
            BenchmarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            BenchmarkCard(
                batteryLevel = thermalState.batteryLevel,
                temperature = thermalState.temperature,
                tokensPerSecond = streamingStats.tokensPerSecond,
                isThrottling = thermalState.isThrottling,
                cpuUsage = performanceMetrics.cpuUsage,
                memoryUsage = performanceMetrics.memoryUsage,
                totalTokens = streamingStats.totalTokens
            )
        }
    }
}

@Composable
private fun BenchmarkCard(
    batteryLevel: Int,
    temperature: Float,
    tokensPerSecond: Double,
    isThrottling: Boolean,
    cpuUsage: Float,
    memoryUsage: Long,
    totalTokens: Long
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .background(
                Color(0xFF1A1A1A).copy(alpha = 0.95f),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isThrottling) Color(0xFFFF6B6B) else Color(0xFF4ECDC4).copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header
            Text(
                text = "Performance",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
            )
            
            Divider(color = Color(0xFF333333), thickness = 0.5.dp)
            
            // Battery Level
            BenchmarkMetric(
                label = "Battery",
                value = "${batteryLevel}%",
                color = getBatteryColor(batteryLevel),
                icon = getBatteryIcon(batteryLevel)
            )
            
            // Temperature
            BenchmarkMetric(
                label = "Temp",
                value = "${temperature.toInt()}°C",
                color = getTemperatureColor(temperature),
                icon = getTemperatureIcon(temperature)
            )
            
            // Tokens per second
            BenchmarkMetric(
                label = "Speed",
                value = "${String.format("%.1f", tokensPerSecond)} t/s",
                color = getSpeedColor(tokensPerSecond),
                icon = "⚡"
            )
            
            // CPU Usage
            BenchmarkMetric(
                label = "CPU",
                value = "${cpuUsage.toInt()}%",
                color = getCpuColor(cpuUsage),
                icon = "🔥"
            )
            
            // Memory Usage (in MB)
            BenchmarkMetric(
                label = "RAM",
                value = "${memoryUsage / 1024}MB",
                color = getMemoryColor(memoryUsage),
                icon = "💾"
            )
            
            // Total tokens generated
            if (totalTokens > 0) {
                BenchmarkMetric(
                    label = "Total",
                    value = "${totalTokens}t",
                    color = Color(0xFFB0B0B0),
                    icon = "📊"
                )
            }
            
            // Thermal throttling warning
            if (isThrottling) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "🌡️",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Throttling",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchmarkMetric(
    label: String,
    value: String,
    color: Color,
    icon: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFB0B0B0)
                )
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// Color helpers
private fun getBatteryColor(batteryLevel: Int): Color {
    return when {
        batteryLevel > 50 -> Color(0xFF4CAF50)  // Green
        batteryLevel > 20 -> Color(0xFFFF9800)  // Orange  
        else -> Color(0xFFFF5722)               // Red
    }
}

private fun getTemperatureColor(temperature: Float): Color {
    return when {
        temperature < 35 -> Color(0xFF2196F3)   // Blue (cool)
        temperature < 42 -> Color(0xFF4CAF50)   // Green (normal)
        temperature < 48 -> Color(0xFFFF9800)   // Orange (warm)
        else -> Color(0xFFFF5722)               // Red (hot)
    }
}

private fun getSpeedColor(tokensPerSecond: Double): Color {
    return when {
        tokensPerSecond > 20 -> Color(0xFF4CAF50)   // Green (fast)
        tokensPerSecond > 10 -> Color(0xFFFF9800)   // Orange (medium)
        tokensPerSecond > 0 -> Color(0xFFFFEB3B)    // Yellow (slow)
        else -> Color(0xFF9E9E9E)                   // Gray (stopped)
    }
}

private fun getCpuColor(cpuUsage: Float): Color {
    return when {
        cpuUsage < 50 -> Color(0xFF4CAF50)      // Green
        cpuUsage < 80 -> Color(0xFFFF9800)      // Orange
        else -> Color(0xFFFF5722)               // Red
    }
}

private fun getMemoryColor(memoryUsage: Long): Color {
    val memoryMB = memoryUsage / 1024
    return when {
        memoryMB < 1000 -> Color(0xFF4CAF50)    // Green
        memoryMB < 2000 -> Color(0xFFFF9800)    // Orange
        else -> Color(0xFFFF5722)               // Red
    }
}

// Icon helpers
private fun getBatteryIcon(batteryLevel: Int): String {
    return when {
        batteryLevel > 75 -> "🔋"
        batteryLevel > 50 -> "🔋"
        batteryLevel > 25 -> "🪫"
        else -> "🪫"
    }
}

private fun getTemperatureIcon(temperature: Float): String {
    return when {
        temperature < 35 -> "❄️"
        temperature < 45 -> "🌡️"
        else -> "🔥"
    }
}

enum class BenchmarkPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * Composable state for benchmark overlay visibility
 */
@Composable
fun rememberBenchmarkOverlayState(): MutableState<Boolean> {
    return rememberSaveable { mutableStateOf(false) }
}

/**
 * Toggle benchmark overlay
 */
@Composable
fun BenchmarkToggleButton(
    isVisible: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = { isVisible.value = !isVisible.value },
        modifier = modifier
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(
                id = if (isVisible.value) 
                    android.R.drawable.ic_menu_view 
                else 
                    android.R.drawable.ic_menu_info_details
            ),
            contentDescription = if (isVisible.value) "Hide Benchmark" else "Show Benchmark",
            tint = if (isVisible.value) Color(0xFF4ECDC4) else Color(0xFFB0B0B0)
        )
    }
}