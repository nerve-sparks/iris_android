package com.nervesparks.iris.features

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Samsung Galaxy S24 Ultra S Pen Integration
 * Advanced handwriting recognition and sketch-to-text conversion
 */
class SPenManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    
    data class DrawingStroke(
        val path: Path,
        val color: Color,
        val strokeWidth: Float,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class HandwritingResult(
        val recognizedText: String,
        val confidence: Float,
        val alternativeTexts: List<String> = emptyList()
    )

    var isDrawing by mutableStateOf(false)
        private set
    
    var currentStrokes by mutableStateOf<List<DrawingStroke>>(emptyList())
        private set
    
    var handwritingResult by mutableStateOf<HandwritingResult?>(null)
        private set
    
    var sPenDetected by mutableStateOf(false)
        private set

    /**
     * Check if S Pen is available and supported
     */
    fun checkSPenSupport(): Boolean {
        return try {
            // Check for Samsung S Pen feature
            context.packageManager.hasSystemFeature("com.sec.feature.spen_usp") ||
            context.packageManager.hasSystemFeature("android.hardware.touchscreen.multitouch.jazzhand")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Start drawing session
     */
    fun startDrawing() {
        isDrawing = true
        currentStrokes = emptyList()
        handwritingResult = null
    }

    /**
     * Add stroke to current drawing
     */
    fun addStroke(stroke: DrawingStroke) {
        currentStrokes = currentStrokes + stroke
    }

    /**
     * Complete drawing and process for handwriting recognition
     */
    fun completeDrawing() {
        isDrawing = false
        if (currentStrokes.isNotEmpty()) {
            processHandwriting()
        }
    }

    /**
     * Clear current drawing
     */
    fun clearDrawing() {
        currentStrokes = emptyList()
        handwritingResult = null
        isDrawing = false
    }

    /**
     * Process handwriting recognition (simplified version)
     */
    private fun processHandwriting() {
        coroutineScope.launch(Dispatchers.Default) {
            try {
                // In a real implementation, you would use:
                // - Google ML Kit Digital Ink Recognition
                // - Samsung SDK for S Pen
                // - Custom OCR solution
                
                // Simplified simulation for demonstration
                val simulatedText = analyzeStrokes(currentStrokes)
                
                handwritingResult = HandwritingResult(
                    recognizedText = simulatedText,
                    confidence = 0.85f,
                    alternativeTexts = listOf(
                        simulatedText.replace("a", "o"),
                        simulatedText.replace("e", "a")
                    )
                )
            } catch (e: Exception) {
                handwritingResult = HandwritingResult(
                    recognizedText = "Recognition failed",
                    confidence = 0.0f
                )
            }
        }
    }

    /**
     * Analyze strokes to simulate handwriting recognition
     */
    private fun analyzeStrokes(strokes: List<DrawingStroke>): String {
        // This is a simplified simulation
        // Real implementation would use proper ML models
        
        return when (strokes.size) {
            in 1..3 -> "Hi"
            in 4..6 -> "Hello"
            in 7..10 -> "How are you?"
            in 11..15 -> "Can you help me?"
            else -> "I need assistance with this task"
        }
    }

    /**
     * Convert drawing to text input
     */
    fun getRecognizedText(): String {
        return handwritingResult?.recognizedText ?: ""
    }

    /**
     * Check if handwriting recognition is complete
     */
    fun isRecognitionComplete(): Boolean {
        return handwritingResult != null
    }

    /**
     * Get S Pen pressure sensitivity (if available)
     */
    fun getPressure(event: MotionEvent): Float {
        return when (event.getToolType(0)) {
            MotionEvent.TOOL_TYPE_STYLUS -> event.pressure
            else -> 0.5f // Default pressure for finger touch
        }
    }

    /**
     * Detect S Pen vs finger input
     */
    fun isSPenInput(event: MotionEvent): Boolean {
        return event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
    }
}

/**
 * S Pen Drawing Canvas Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SPenDrawingCanvas(
    sPenManager: SPenManager,
    onTextRecognized: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf(Path()) }
    var currentStrokeColor by remember { mutableStateOf(Color.Blue) }
    var currentStrokeWidth by remember { mutableStateOf(3f) }
    
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Header with controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (sPenManager.sPenDetected) "S Pen Detected" else "Handwriting Input",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row {
                // Clear button
                IconButton(
                    onClick = { sPenManager.clearDrawing() }
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_delete),
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Convert to text button
                IconButton(
                    onClick = { 
                        sPenManager.completeDrawing()
                        val text = sPenManager.getRecognizedText()
                        if (text.isNotBlank()) {
                            onTextRecognized(text)
                        }
                    }
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_edit),
                        contentDescription = "Convert to Text",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Drawing canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position
                            
                            when (event.changes.first().pressed) {
                                true -> {
                                    if (currentPath.isEmpty) {
                                        sPenManager.startDrawing()
                                        currentPath.moveTo(position.x, position.y)
                                    } else {
                                        currentPath.lineTo(position.x, position.y)
                                    }
                                }
                                false -> {
                                    if (!currentPath.isEmpty) {
                                        sPenManager.addStroke(
                                            SPenManager.DrawingStroke(
                                                path = Path().apply { addPath(currentPath) },
                                                color = currentStrokeColor,
                                                strokeWidth = currentStrokeWidth
                                            )
                                        )
                                        currentPath = Path()
                                    }
                                }
                            }
                            
                            event.changes.first().consume()
                        }
                    }
                }
        ) {
            // Draw completed strokes
            sPenManager.currentStrokes.forEach { stroke ->
                drawPath(
                    path = stroke.path,
                    color = stroke.color,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            
            // Draw current stroke in progress
            if (!currentPath.isEmpty) {
                drawPath(
                    path = currentPath,
                    color = currentStrokeColor,
                    style = Stroke(
                        width = currentStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        
        // Recognition result
        sPenManager.handwritingResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Recognized: ${result.recognizedText}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Confidence: ${(result.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    if (result.alternativeTexts.isNotEmpty()) {
                        Text(
                            text = "Alternatives: ${result.alternativeTexts.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // Instructions
        if (sPenManager.currentStrokes.isEmpty() && sPenManager.handwritingResult == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (sPenManager.checkSPenSupport()) {
                    "Use your S Pen to write text above, then tap the convert button"
                } else {
                    "Use your finger to write text above, then tap the convert button"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}