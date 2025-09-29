package com.nervesparks.iris.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.*
import com.nervesparks.iris.data.ChatMessage
import java.util.*

/**
 * Message action system similar to ChatGPT/Copilot
 * Copy / Regenerate / Share / Pin actions
 */

enum class MessageAction(
    val displayName: String,
    val icon: String,
    val requiresUserMessage: Boolean = false
) {
    COPY("Copy", "📋"),
    REGENERATE("Regenerate", "🔄", requiresUserMessage = false),  // Works on AI messages
    SHARE("Share", "🔗"),
    PIN("Pin", "📌"),
    UNPIN("Unpin", "📌"),
    DELETE("Delete", "🗑️");
    
    companion object {
        fun getAvailableActions(message: ChatMessage, isPinned: Boolean): List<MessageAction> {
            return buildList {
                add(COPY)
                if (!message.isFromUser) {
                    add(REGENERATE)
                }
                add(SHARE)
                if (isPinned) {
                    add(UNPIN)
                } else {
                    add(PIN)
                }
                add(DELETE)
            }
        }
    }
}

class MessageActionManager(private val context: Context) {
    
    private val pinnedMessages = mutableStateSetOf<String>()
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    fun executeAction(
        action: MessageAction,
        message: ChatMessage,
        onRegenerate: (String) -> Unit = {},
        onDelete: (String) -> Unit = {}
    ) {
        when (action) {
            MessageAction.COPY -> copyMessage(message)
            MessageAction.REGENERATE -> regenerateMessage(message, onRegenerate)
            MessageAction.SHARE -> shareMessage(message)
            MessageAction.PIN -> pinMessage(message.id)
            MessageAction.UNPIN -> unpinMessage(message.id)
            MessageAction.DELETE -> deleteMessage(message, onDelete)
        }
    }
    
    private fun copyMessage(message: ChatMessage) {
        val clip = ClipData.newPlainText("Chat Message", message.message)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun regenerateMessage(message: ChatMessage, onRegenerate: (String) -> Unit) {
        if (!message.isFromUser) {
            onRegenerate(message.id)
            Toast.makeText(context, "Regenerating response...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareMessage(message: ChatMessage) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "From AI Assistant:\n\n${message.message}")
            putExtra(Intent.EXTRA_SUBJECT, "AI Chat Response")
        }
        
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share message"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share message", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun pinMessage(messageId: String) {
        pinnedMessages.add(messageId)
        Toast.makeText(context, "Message pinned", Toast.LENGTH_SHORT).show()
    }
    
    private fun unpinMessage(messageId: String) {
        pinnedMessages.remove(messageId)
        Toast.makeText(context, "Message unpinned", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteMessage(message: ChatMessage, onDelete: (String) -> Unit) {
        onDelete(message.id)
        pinnedMessages.remove(message.id)
        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
    }
    
    fun isMessagePinned(messageId: String): Boolean {
        return pinnedMessages.contains(messageId)
    }
    
    fun getPinnedMessages(): Set<String> {
        return pinnedMessages.toSet()
    }
    
    fun clearPinnedMessages() {
        pinnedMessages.clear()
    }
}

/**
 * Composable for message action dropdown menu
 */
@Composable
fun MessageActionMenu(
    message: ChatMessage,
    isVisible: Boolean,
    actionManager: MessageActionManager,
    onDismiss: () -> Unit,
    onRegenerate: (String) -> Unit = {},
    onDelete: (String) -> Unit = {}
) {
    if (isVisible) {
        val isPinned = actionManager.isMessagePinned(message.id)
        val availableActions = MessageAction.getAvailableActions(message, isPinned)
        
        // Implementation would use DropdownMenu or custom overlay
        // This is the logic structure
        availableActions.forEach { action ->
            // Menu item for each action
            // onClick: actionManager.executeAction(action, message, onRegenerate, onDelete)
        }
    }
}