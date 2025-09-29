package com.nervesparks.axia.ui.generation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import com.nervesparks.axia.generation.*
import com.nervesparks.axia.models.*

/**
 * Local AI Image Generation Interface for Axia
 * Provides complete local image generation capabilities
 */

@Composable
fun ImageGenerationUI(
    imageGenerator: ImageGenerator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isGenerating by imageGenerator.isGenerating.collectAsState()
    val generationProgress by imageGenerator.generationProgress.collectAsState()
    val generatedImages by imageGenerator.generatedImages.collectAsState()
    val availableModels by imageGenerator.availableModels.collectAsState()
    
    var selectedModel by remember { mutableStateOf<ImageModel?>(null) }
    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var generationSettings by remember { mutableStateOf(ImageGenerationSettings()) }
    
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
        // Header
        ImageGenerationHeader()
        
        // Model Selection & Status
        ModelSelectionCard(
            availableModels = availableModels,
            selectedModel = selectedModel,
            onModelSelected = { selectedModel = it }
        )
        
        // Generation Interface
        if (isGenerating) {
            GenerationProgressCard(
                progress = generationProgress,
                onCancel = { imageGenerator.cancelGeneration() }
            )
        } else {
            GenerationInputCard(
                prompt = prompt,
                onPromptChange = { prompt = it },
                negativePrompt = negativePrompt,
                onNegativePromptChange = { negativePrompt = it },
                settings = generationSettings,
                onSettingsChange = { generationSettings = it },
                onGenerate = {
                    scope.launch {
                        selectedModel?.let { model ->
                            imageGenerator.generateImage(
                                model = model,
                                prompt = prompt,
                                negativePrompt = negativePrompt.takeIf { it.isNotEmpty() },
                                settings = generationSettings
                            )
                        }
                    }
                },
                canGenerate = selectedModel != null && prompt.isNotEmpty()
            )
        }
        
        // Generated Images Gallery
        GeneratedImagesGallery(
            images = generatedImages,
            onImageClick = { /* Handle image click */ },
            onImageShare = { /* Handle image share */ },
            onImageDelete = { imageGenerator.deleteImage(it) }
        )
    }
}

@Composable
private fun ImageGenerationHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF161B22).copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🎨 Local AI Image Generation",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold
                )
            )
            
            Text(
                text = "Generate images locally with AI - No internet required",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF8B949E)
                )
            )
        }
    }
}

@Composable
private fun ModelSelectionCard(
    availableModels: List<ImageModel>,
    selectedModel: ImageModel?,
    onModelSelected: (ImageModel) -> Unit
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
            Text(
                text = "🤖 Select AI Model",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (availableModels.isEmpty()) {
                ModelDownloadPrompt()
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableModels) { model ->
                        ModelCard(
                            model = model,
                            isSelected = selectedModel == model,
                            onClick = { onModelSelected(model) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelDownloadPrompt() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📥 No AI models installed",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color(0xFF8B949E)
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { /* Handle model download */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF58A6FF)
            )
        ) {
            Text(
                text = "Download Stable Diffusion",
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "~2.5GB download • Runs locally without internet",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF656D76)
            )
        )
    }
}

@Composable
private fun ModelCard(
    model: ImageModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF58A6FF).copy(alpha = 0.2f)
            } else {
                Color(0xFF21262D)
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF58A6FF))
        } else {
            null
        },
        modifier = Modifier.width(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8B949E)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${model.sizeGB}GB",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF656D76)
                    )
                )
                
                if (model.isDownloaded) {
                    Text(
                        text = "✅ Ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF4CAF50)
                        )
                    )
                } else {
                    Text(
                        text = "📥 Download",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF58A6FF)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerationProgressCard(
    progress: GenerationProgress,
    onCancel: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎨 Generating Image...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDA3633)
                    ),
                    modifier = Modifier.size(width = 80.dp, height = 32.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = progress.percentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF58A6FF),
                trackColor = Color(0xFF21262D)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = progress.currentStep,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF8B949E)
                    )
                )
                
                Text(
                    text = "${(progress.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF58A6FF),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            if (progress.estimatedTimeRemaining > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "⏱️ ${formatTimeRemaining(progress.estimatedTimeRemaining)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF656D76)
                    )
                )
            }
        }
    }
}

@Composable
private fun GenerationInputCard(
    prompt: String,
    onPromptChange: (String) -> Unit,
    negativePrompt: String,
    onNegativePromptChange: (String) -> Unit,
    settings: ImageGenerationSettings,
    onSettingsChange: (ImageGenerationSettings) -> Unit,
    onGenerate: () -> Unit,
    canGenerate: Boolean
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
            // Prompt Input
            Text(
                text = "✨ Prompt",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = "Describe what you want to generate...",
                        color = Color(0xFF656D76)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF58A6FF),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color(0xFFE6EDF3),
                    unfocusedTextColor = Color(0xFFE6EDF3)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Negative Prompt Input
            Text(
                text = "🚫 Negative Prompt (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = negativePrompt,
                onValueChange = onNegativePromptChange,
                placeholder = {
                    Text(
                        text = "What to avoid in the image...",
                        color = Color(0xFF656D76)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF58A6FF),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color(0xFFE6EDF3),
                    unfocusedTextColor = Color(0xFFE6EDF3)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Generation Settings
            GenerationSettingsSection(
                settings = settings,
                onSettingsChange = onSettingsChange
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Generate Button
            Button(
                onClick = onGenerate,
                enabled = canGenerate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636),
                    disabledContainerColor = Color(0xFF30363D)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎨",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Image",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerationSettingsSection(
    settings: ImageGenerationSettings,
    onSettingsChange: (ImageGenerationSettings) -> Unit
) {
    Column {
        Text(
            text = "⚙️ Generation Settings",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Image Size
        SettingSlider(
            label = "Image Size",
            value = settings.size.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(size = it.toInt())) },
            valueRange = 256f..1024f,
            steps = 7,
            valueFormatter = { "${it.toInt()}x${it.toInt()}" }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Steps
        SettingSlider(
            label = "Quality Steps",
            value = settings.steps.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(steps = it.toInt())) },
            valueRange = 20f..100f,
            steps = 15,
            valueFormatter = { "${it.toInt()} steps" }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // CFG Scale
        SettingSlider(
            label = "Guidance Scale",
            value = settings.guidanceScale,
            onValueChange = { onSettingsChange(settings.copy(guidanceScale = it)) },
            valueRange = 1f..20f,
            steps = 38,
            valueFormatter = { "%.1f".format(it) }
        )
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF8B949E)
            ),
            modifier = Modifier.width(120.dp)
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58A6FF),
                activeTrackColor = Color(0xFF58A6FF),
                inactiveTrackColor = Color(0xFF30363D)
            ),
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = valueFormatter(value),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF58A6FF),
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.width(80.dp)
        )
    }
}

@Composable
private fun GeneratedImagesGallery(
    images: List<GeneratedImage>,
    onImageClick: (GeneratedImage) -> Unit,
    onImageShare: (GeneratedImage) -> Unit,
    onImageDelete: (GeneratedImage) -> Unit
) {
    if (images.isEmpty()) return
    
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
            Text(
                text = "🖼️ Generated Images (${images.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(400.dp)
            ) {
                items(images) { image ->
                    GeneratedImageCard(
                        image = image,
                        onClick = { onImageClick(image) },
                        onShare = { onImageShare(image) },
                        onDelete = { onImageDelete(image) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratedImageCard(
    image: GeneratedImage,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        ),
        modifier = Modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image
            Image(
                bitmap = image.bitmap.asImageBitmap(),
                contentDescription = image.prompt,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Actions overlay
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            CircleShape
                        )
                ) {
                    Text(
                        text = "📤",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            CircleShape
                        )
                ) {
                    Text(
                        text = "🗑️",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Generation info
            if (image.generationTime > 0) {
                Text(
                    text = "${image.generationTime}s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                )
            }
        }
    }
}

private fun formatTimeRemaining(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}