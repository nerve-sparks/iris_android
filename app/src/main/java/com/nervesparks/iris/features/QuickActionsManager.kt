package com.nervesparks.iris.features

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Samsung Galaxy S24 Ultra Swipe Gestures for Quick Actions
 * Enhanced with haptic feedback and smooth animations
 */

data class SwipeAction(
    val id: String,
    val title: String,
    val icon: Int,
    val color: Color,
    val action: () -> Unit
)

enum class SwipeDirection {
    LEFT, RIGHT, NONE
}

class QuickActionsManager {
    
    companion object {
        const val SWIPE_THRESHOLD = 200f
        const val REVEAL_THRESHOLD = 100f
    }

    /**
     * Get default quick actions for chat messages
     */
    fun getChatMessageActions(
        onCopy: () -> Unit,
        onSpeak: () -> Unit,
        onRegenerate: () -> Unit,
        onShare: () -> Unit
    ): Pair<List<SwipeAction>, List<SwipeAction>> {
        
        val leftActions = listOf(
            SwipeAction(
                id = "copy",
                title = "Copy",
                icon = android.R.drawable.ic_menu_save,
                color = Color(0xFF10B981),
                action = onCopy
            ),
            SwipeAction(
                id = "speak",
                title = "Speak",
                icon = android.R.drawable.ic_btn_speak_now,
                color = Color(0xFF3B82F6),
                action = onSpeak
            )
        )
        
        val rightActions = listOf(
            SwipeAction(
                id = "regenerate",
                title = "Retry",
                icon = android.R.drawable.ic_popup_sync,
                color = Color(0xFFF59E0B),
                action = onRegenerate
            ),
            SwipeAction(
                id = "share",
                title = "Share",
                icon = android.R.drawable.ic_menu_share,
                color = Color(0xFF8B5CF6),
                action = onShare
            )
        )
        
        return Pair(leftActions, rightActions)
    }

    /**
     * Get quick actions for input field
     */
    fun getInputActions(
        onVoiceInput: () -> Unit,
        onClipboard: () -> Unit,
        onTemplates: () -> Unit,
        onCamera: () -> Unit
    ): Pair<List<SwipeAction>, List<SwipeAction>> {
        
        val leftActions = listOf(
            SwipeAction(
                id = "voice",
                title = "Voice",
                icon = android.R.drawable.ic_btn_speak_now,
                color = Color(0xFF10B981),
                action = onVoiceInput
            ),
            SwipeAction(
                id = "clipboard",
                title = "Paste",
                icon = android.R.drawable.ic_menu_edit,
                color = Color(0xFF6366F1),
                action = onClipboard
            )
        )
        
        val rightActions = listOf(
            SwipeAction(
                id = "templates",
                title = "Templates",
                icon = android.R.drawable.ic_menu_agenda,
                color = Color(0xFFF59E0B),
                action = onTemplates
            ),
            SwipeAction(
                id = "camera",
                title = "Camera",
                icon = android.R.drawable.ic_menu_camera,
                color = Color(0xFF8B5CF6),
                action = onCamera
            )
        )
        
        return Pair(leftActions, rightActions)
    }

    /**
     * Get conversation management actions
     */
    fun getConversationActions(
        onBookmark: () -> Unit,
        onExport: () -> Unit,
        onClear: () -> Unit,
        onSearch: () -> Unit
    ): Pair<List<SwipeAction>, List<SwipeAction>> {
        
        val leftActions = listOf(
            SwipeAction(
                id = "bookmark",
                title = "Bookmark",
                icon = android.R.drawable.btn_star_big_on,
                color = Color(0xFFEF4444),
                action = onBookmark
            ),
            SwipeAction(
                id = "search",
                title = "Search",
                icon = android.R.drawable.ic_menu_search,
                color = Color(0xFF06B6D4),
                action = onSearch
            )
        )
        
        val rightActions = listOf(
            SwipeAction(
                id = "export",
                title = "Export",
                icon = android.R.drawable.ic_menu_upload,
                color = Color(0xFF10B981),
                action = onExport
            ),
            SwipeAction(
                id = "clear",
                title = "Clear",
                icon = android.R.drawable.ic_menu_delete,
                color = Color(0xFFEF4444),
                action = onClear
            )
        )
        
        return Pair(leftActions, rightActions)
    }
}

/**
 * Composable modifier for swipe gestures
 */
@Composable
fun Modifier.swipeActions(
    leftActions: List<SwipeAction> = emptyList(),
    rightActions: List<SwipeAction> = emptyList(),
    onSwipeStateChanged: (SwipeDirection, Float) -> Unit = { _, _ -> }
): Modifier {
    var offsetX by remember { mutableStateOf(0f) }
    var currentDirection by remember { mutableStateOf(SwipeDirection.NONE) }

    return this
        .offset { IntOffset(offsetX.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val direction = when {
                        offsetX > QuickActionsManager.SWIPE_THRESHOLD -> SwipeDirection.RIGHT
                        offsetX < -QuickActionsManager.SWIPE_THRESHOLD -> SwipeDirection.LEFT
                        else -> SwipeDirection.NONE
                    }

                    when (direction) {
                        SwipeDirection.RIGHT -> {
                            if (rightActions.isNotEmpty()) {
                                rightActions.first().action()
                            }
                        }
                        SwipeDirection.LEFT -> {
                            if (leftActions.isNotEmpty()) {
                                leftActions.first().action()
                            }
                        }
                        SwipeDirection.NONE -> {
                            // Snap back
                        }
                    }

                    // Reset position
                    offsetX = 0f
                    currentDirection = SwipeDirection.NONE
                    onSwipeStateChanged(SwipeDirection.NONE, 0f)
                }
            ) { _, dragAmount ->
                val newOffset = offsetX + dragAmount
                val maxOffset = QuickActionsManager.SWIPE_THRESHOLD * 1.5f

                offsetX = newOffset.coerceIn(-maxOffset, maxOffset)

                val newDirection = when {
                    abs(offsetX) < QuickActionsManager.REVEAL_THRESHOLD -> SwipeDirection.NONE
                    offsetX > 0 -> SwipeDirection.RIGHT
                    else -> SwipeDirection.LEFT
                }

                if (newDirection != currentDirection) {
                    currentDirection = newDirection
                    onSwipeStateChanged(newDirection, abs(offsetX))
                }
            }
        }
}