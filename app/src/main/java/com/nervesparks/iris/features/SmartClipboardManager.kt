package com.nervesparks.iris.features

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Samsung Galaxy S24 Ultra Smart Clipboard Integration
 * Analyzes copied text and provides intelligent suggestions
 */
class SmartClipboardManager(
    private val context: Context,
    private val clipboardManager: ClipboardManager,
    private val coroutineScope: CoroutineScope
) {
    var lastClipboardText by mutableStateOf("")
        private set
    
    var clipboardSuggestions by mutableStateOf<List<ClipboardSuggestion>>(emptyList())
        private set
    
    var showClipboardHelper by mutableStateOf(false)
        private set

    data class ClipboardSuggestion(
        val type: SuggestionType,
        val title: String,
        val prompt: String,
        val icon: Int
    )

    enum class SuggestionType {
        SUMMARIZE,
        EXPLAIN,
        TRANSLATE,
        IMPROVE,
        CODE_REVIEW,
        EMAIL_REPLY,
        GRAMMAR_CHECK,
        SIMPLIFY
    }

    /**
     * Check clipboard for changes and analyze content
     */
    fun checkClipboard() {
        coroutineScope.launch(Dispatchers.Main) {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                if (text.isNotEmpty() && text != lastClipboardText && text.length > 10) {
                    lastClipboardText = text
                    analyzeClipboardContent(text)
                    showClipboardHelper = true
                }
            }
        }
    }

    /**
     * Analyze clipboard content and generate smart suggestions
     */
    private fun analyzeClipboardContent(text: String) {
        val suggestions = mutableListOf<ClipboardSuggestion>()

        // Detect content type and generate appropriate suggestions
        when {
            text.contains("@") && text.contains(".") -> {
                // Looks like an email
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.EMAIL_REPLY,
                        "Draft Reply",
                        "Help me write a professional reply to this email:\n\n$text",
                        android.R.drawable.ic_dialog_email
                    )
                )
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.SUMMARIZE,
                        "Summarize Email",
                        "Summarize the key points of this email:\n\n$text",
                        android.R.drawable.ic_menu_info_details
                    )
                )
            }

            text.matches(Regex(".*\\b(class|function|def|public|private|protected|var|let|const)\\b.*", RegexOption.IGNORE_CASE)) -> {
                // Looks like code
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.CODE_REVIEW,
                        "Review Code",
                        "Review this code and suggest improvements:\n\n```\n$text\n```",
                        android.R.drawable.ic_menu_edit
                    )
                )
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.EXPLAIN,
                        "Explain Code",
                        "Explain what this code does in simple terms:\n\n```\n$text\n```",
                        android.R.drawable.ic_menu_help
                    )
                )
            }

            text.length > 200 -> {
                // Long text - offer summarization
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.SUMMARIZE,
                        "Summarize Text",
                        "Summarize this text in 3-4 key points:\n\n$text",
                        android.R.drawable.ic_menu_info_details
                    )
                )
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.SIMPLIFY,
                        "Simplify Text",
                        "Rewrite this text to be simpler and easier to understand:\n\n$text",
                        android.R.drawable.ic_menu_edit
                    )
                )
            }

            else -> {
                // General text suggestions
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.EXPLAIN,
                        "Explain This",
                        "Explain this in detail:\n\n$text",
                        android.R.drawable.ic_menu_help
                    )
                )
                suggestions.add(
                    ClipboardSuggestion(
                        SuggestionType.IMPROVE,
                        "Improve Writing",
                        "Improve the writing and grammar of this text:\n\n$text",
                        android.R.drawable.ic_menu_edit
                    )
                )
            }
        }

        // Always add grammar check for text content
        if (text.length > 20) {
            suggestions.add(
                ClipboardSuggestion(
                    SuggestionType.GRAMMAR_CHECK,
                    "Grammar Check",
                    "Check the grammar and spelling of this text:\n\n$text",
                    android.R.drawable.ic_menu_edit
                )
            )
        }

        clipboardSuggestions = suggestions
    }

    fun dismissClipboardHelper() {
        showClipboardHelper = false
    }

    fun clearSuggestions() {
        clipboardSuggestions = emptyList()
        showClipboardHelper = false
    }
}