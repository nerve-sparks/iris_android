package com.nervesparks.iris.ui.sheets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.nervesparks.iris.profiles.PerformanceProfile
import com.nervesparks.iris.profiles.PerformanceProfileManager

/**
 * Advanced parameters bottom sheet with rememberSaveable state
 * Quick profile switching similar to ChatGPT/Copilot
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametersBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    profileManager: PerformanceProfileManager,
    onParametersChanged: (PerformanceProfile, CustomParams?) -> Unit
) {
    val context = LocalContext.current
    val currentProfile by profileManager.currentProfile.collectAsState(initial = PerformanceProfile.BALANCED)
    val customParams by profileManager.customParameters.collectAsState(initial = PerformanceProfileManager.CustomParameters(0.7f, 0.9f, 6, 4096))
    
    // Saveable state for UI components
    var selectedProfile by rememberSaveable { mutableStateOf(currentProfile) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var customTemp by rememberSaveable { mutableFloatStateOf(customParams.temperature) }
    var customTopP by rememberSaveable { mutableFloatStateOf(customParams.topP) }
    var customThreads by rememberSaveable { mutableIntStateOf(customParams.threads) }
    var customContext by rememberSaveable { mutableIntStateOf(customParams.contextLength) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF0D1117),
            contentColor = Color(0xFFE6EDF3),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            Color(0xFF30363D),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Performance Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6EDF3)
                            )
                        )
                        
                        TextButton(
                            onClick = { showAdvanced = !showAdvanced }
                        ) {
                            Text(
                                text = if (showAdvanced) "Simple" else "Advanced",
                                color = Color(0xFF58A6FF)
                            )
                        }
                    }
                }
                
                item {
                    // Performance Profiles
                    Text(
                        text = "Performance Profiles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFE6EDF3),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PerformanceProfile.values().forEach { profile ->
                            ProfileCard(
                                profile = profile,
                                isSelected = selectedProfile == profile,
                                onClick = {
                                    selectedProfile = profile
                                    onParametersChanged(profile, null)
                                }
                            )
                        }
                    }
                }
                
                if (showAdvanced) {
                    item {
                        Divider(
                            color = Color(0xFF30363D),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Text(
                            text = "Advanced Parameters",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFE6EDF3),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    // Temperature
                    item {
                        ParameterSlider(
                            label = "Temperature",
                            value = customTemp,
                            onValueChange = { 
                                customTemp = it
                                onParametersChanged(
                                    selectedProfile,
                                    CustomParams(customTemp, customTopP, customThreads, customContext)
                                )
                            },
                            valueRange = 0.1f..2.0f,
                            description = "Controls randomness (0.1 = focused, 2.0 = creative)"
                        )
                    }
                    
                    // Top-P
                    item {
                        ParameterSlider(
                            label = "Top-P",
                            value = customTopP,
                            onValueChange = { 
                                customTopP = it
                                onParametersChanged(
                                    selectedProfile,
                                    CustomParams(customTemp, customTopP, customThreads, customContext)
                                )
                            },
                            valueRange = 0.1f..1.0f,
                            description = "Nucleus sampling (0.1 = narrow, 1.0 = broad)"
                        )
                    }
                    
                    // Thread Count
                    item {
                        ParameterIntSlider(
                            label = "Thread Count",
                            value = customThreads,
                            onValueChange = { 
                                customThreads = it
                                onParametersChanged(
                                    selectedProfile,
                                    CustomParams(customTemp, customTopP, customThreads, customContext)
                                )
                            },
                            valueRange = 1..12,
                            description = "CPU threads (higher = faster, more battery drain)"
                        )
                    }
                    
                    // Context Length
                    item {
                        ParameterIntSlider(
                            label = "Context Length",
                            value = customContext,
                            onValueChange = { 
                                customContext = it
                                onParametersChanged(
                                    selectedProfile,
                                    CustomParams(customTemp, customTopP, customThreads, customContext)
                                )
                            },
                            valueRange = 512..16384,
                            step = 512,
                            description = "Memory length (higher = remembers more, uses more RAM)"
                        )
                    }
                    
                    // Reset to defaults
                    item {
                        OutlinedButton(
                            onClick = {
                                customTemp = selectedProfile.temperature
                                customTopP = selectedProfile.topP
                                customThreads = selectedProfile.threads
                                customContext = selectedProfile.contextLength
                                onParametersChanged(selectedProfile, null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF58A6FF)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Text("Reset to Profile Defaults")
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: PerformanceProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1F2937) else Color(0xFF0F172A)
        ),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF58A6FF)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color(0xFF58A6FF) else Color(0xFFE6EDF3)
                        )
                    )
                    Text(
                        text = profile.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF8B949E)
                        )
                    )
                }
                
                if (isSelected) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(android.R.drawable.checkbox_on_background),
                        contentDescription = "Selected",
                        tint = Color(0xFF58A6FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Profile specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileSpec("Temp", String.format("%.1f", profile.temperature))
                ProfileSpec("Top-P", String.format("%.1f", profile.topP))
                ProfileSpec("Threads", "${profile.threads}")
                ProfileSpec("Context", "${profile.contextLength}")
            }
        }
    }
}

@Composable
private fun ProfileSpec(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6EDF3)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF8B949E)
            )
        )
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF58A6FF),
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58A6FF),
                activeTrackColor = Color(0xFF58A6FF),
                inactiveTrackColor = Color(0xFF30363D)
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF8B949E)
            )
        )
    }
}

@Composable
private fun ParameterIntSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    step: Int = 1,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF58A6FF),
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange((it / step).toInt() * step) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first) / step - 1,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58A6FF),
                activeTrackColor = Color(0xFF58A6FF),
                inactiveTrackColor = Color(0xFF30363D)
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF8B949E)
            )
        )
    }
}

data class CustomParams(
    val temperature: Float,
    val topP: Float,
    val threads: Int,
    val contextLength: Int
)