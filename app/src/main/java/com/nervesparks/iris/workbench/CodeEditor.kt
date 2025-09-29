package com.nervesparks.iris.workbench

import android.util.Log
import androidx.compose.runtime.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.nio.file.*

/**
 * Advanced Code Editor for the Workbench
 * Features syntax highlighting, auto-completion, and real-time collaboration
 */

data class EditorFile(
    val path: String,
    val name: String,
    val content: String,
    val language: CodeLanguage,
    val lastModified: Long,
    val hasUnsavedChanges: Boolean = false,
    val cursorPosition: Int = 0,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0
)

enum class CodeLanguage(val displayName: String, val extensions: List<String>) {
    KOTLIN("Kotlin", listOf("kt", "kts")),
    JAVA("Java", listOf("java")),
    XML("XML", listOf("xml")),
    JSON("JSON", listOf("json")),
    GRADLE("Gradle", listOf("gradle", "gradle.kts")),
    MARKDOWN("Markdown", listOf("md", "markdown")),
    YAML("YAML", listOf("yml", "yaml")),
    PROPERTIES("Properties", listOf("properties")),
    SHELL("Shell", listOf("sh", "bash")),
    PYTHON("Python", listOf("py")),
    JAVASCRIPT("JavaScript", listOf("js")),
    CSS("CSS", listOf("css")),
    HTML("HTML", listOf("html", "htm")),
    PLAIN_TEXT("Plain Text", listOf("txt"));
    
    companion object {
        fun fromExtension(extension: String): CodeLanguage {
            return values().find { lang ->
                lang.extensions.any { it.equals(extension, ignoreCase = true) }
            } ?: PLAIN_TEXT
        }
        
        fun fromFileName(fileName: String): CodeLanguage {
            val extension = fileName.substringAfterLast('.', "")
            return fromExtension(extension)
        }
    }
}

data class SyntaxHighlight(
    val start: Int,
    val end: Int,
    val type: HighlightType,
    val color: Long
)

enum class HighlightType {
    KEYWORD, STRING, COMMENT, NUMBER, FUNCTION, CLASS, VARIABLE, OPERATOR
}

data class AutoCompleteItem(
    val text: String,
    val description: String,
    val insertText: String,
    val type: CompletionType
)

enum class CompletionType {
    KEYWORD, FUNCTION, CLASS, VARIABLE, SNIPPET, IMPORT
}

class CodeEditor {
    companion object {
        private const val TAG = "CodeEditor"
    }
    
    private val _openFiles = MutableStateFlow<List<EditorFile>>(emptyList())
    val openFiles: StateFlow<List<EditorFile>> = _openFiles.asStateFlow()
    
    private val _activeFileIndex = MutableStateFlow(-1)
    val activeFileIndex: StateFlow<Int> = _activeFileIndex.asStateFlow()
    
    private val _syntaxHighlights = MutableStateFlow<List<SyntaxHighlight>>(emptyList())
    val syntaxHighlights: StateFlow<List<SyntaxHighlight>> = _syntaxHighlights.asStateFlow()
    
    private val _autoCompleteItems = MutableStateFlow<List<AutoCompleteItem>>(emptyList())
    val autoCompleteItems: StateFlow<List<AutoCompleteItem>> = _autoCompleteItems.asStateFlow()
    
    private val _isAutoCompleteVisible = MutableStateFlow(false)
    val isAutoCompleteVisible: StateFlow<Boolean> = _isAutoCompleteVisible.asStateFlow()
    
    /**
     * Open a file in the editor
     */
    suspend fun openFile(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists() || !file.isFile) {
                    Log.w(TAG, "File does not exist: $filePath")
                    return@withContext false
                }
                
                val content = file.readText()
                val language = CodeLanguage.fromFileName(file.name)
                
                val editorFile = EditorFile(
                    path = filePath,
                    name = file.name,
                    content = content,
                    language = language,
                    lastModified = file.lastModified()
                )
                
                val currentFiles = _openFiles.value.toMutableList()
                val existingIndex = currentFiles.indexOfFirst { it.path == filePath }
                
                if (existingIndex >= 0) {
                    // File already open, just switch to it
                    _activeFileIndex.value = existingIndex
                } else {
                    // Add new file
                    currentFiles.add(editorFile)
                    _openFiles.value = currentFiles
                    _activeFileIndex.value = currentFiles.size - 1
                }
                
                // Generate syntax highlighting
                generateSyntaxHighlighting(editorFile)
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open file: $filePath", e)
                false
            }
        }
    }
    
    /**
     * Save the active file
     */
    suspend fun saveActiveFile(): Boolean {
        return withContext(Dispatchers.IO) {
            val activeFile = getActiveFile() ?: return@withContext false
            
            try {
                File(activeFile.path).writeText(activeFile.content)
                
                // Update file in the list
                val currentFiles = _openFiles.value.toMutableList()
                val index = _activeFileIndex.value
                if (index >= 0 && index < currentFiles.size) {
                    currentFiles[index] = activeFile.copy(
                        hasUnsavedChanges = false,
                        lastModified = System.currentTimeMillis()
                    )
                    _openFiles.value = currentFiles
                }
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save file: ${activeFile.path}", e)
                false
            }
        }
    }
    
    /**
     * Update content of the active file
     */
    fun updateActiveFileContent(newContent: String, cursorPosition: Int = 0) {
        val activeFile = getActiveFile() ?: return
        val currentFiles = _openFiles.value.toMutableList()
        val index = _activeFileIndex.value
        
        if (index >= 0 && index < currentFiles.size) {
            currentFiles[index] = activeFile.copy(
                content = newContent,
                hasUnsavedChanges = true,
                cursorPosition = cursorPosition
            )
            _openFiles.value = currentFiles
            
            // Update syntax highlighting
            CoroutineScope(Dispatchers.Default).launch {
                generateSyntaxHighlighting(currentFiles[index])
            }
            
            // Trigger auto-complete if needed
            if (shouldShowAutoComplete(newContent, cursorPosition)) {
                CoroutineScope(Dispatchers.Default).launch {
                    generateAutoCompleteItems(newContent, cursorPosition)
                }
            } else {
                _isAutoCompleteVisible.value = false
            }
        }
    }
    
    /**
     * Close a file
     */
    fun closeFile(index: Int) {
        val currentFiles = _openFiles.value.toMutableList()
        if (index >= 0 && index < currentFiles.size) {
            currentFiles.removeAt(index)
            _openFiles.value = currentFiles
            
            // Adjust active file index
            when {
                currentFiles.isEmpty() -> _activeFileIndex.value = -1
                index <= _activeFileIndex.value -> {
                    _activeFileIndex.value = maxOf(0, _activeFileIndex.value - 1)
                }
            }
        }
    }
    
    /**
     * Switch to a specific file
     */
    fun switchToFile(index: Int) {
        if (index >= 0 && index < _openFiles.value.size) {
            _activeFileIndex.value = index
            val activeFile = getActiveFile()
            if (activeFile != null) {
                CoroutineScope(Dispatchers.Default).launch {
                    generateSyntaxHighlighting(activeFile)
                }
            }
        }
    }
    
    /**
     * Get the currently active file
     */
    fun getActiveFile(): EditorFile? {
        val index = _activeFileIndex.value
        val files = _openFiles.value
        return if (index >= 0 && index < files.size) files[index] else null
    }
    
    /**
     * Generate syntax highlighting for a file
     */
    private suspend fun generateSyntaxHighlighting(file: EditorFile) {
        withContext(Dispatchers.Default) {
            val highlights = when (file.language) {
                CodeLanguage.KOTLIN -> generateKotlinHighlighting(file.content)
                CodeLanguage.JAVA -> generateJavaHighlighting(file.content)
                CodeLanguage.XML -> generateXmlHighlighting(file.content)
                CodeLanguage.JSON -> generateJsonHighlighting(file.content)
                else -> emptyList()
            }
            _syntaxHighlights.value = highlights
        }
    }
    
    private fun generateKotlinHighlighting(content: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        val keywords = listOf(
            "class", "fun", "val", "var", "if", "else", "when", "for", "while",
            "try", "catch", "finally", "throw", "return", "break", "continue",
            "import", "package", "private", "public", "internal", "protected",
            "override", "abstract", "open", "final", "companion", "object",
            "interface", "enum", "data", "sealed", "suspend", "inline"
        )
        
        // Highlight keywords
        keywords.forEach { keyword ->
            val regex = "\\b$keyword\\b".toRegex()
            regex.findAll(content).forEach { match ->
                highlights.add(SyntaxHighlight(
                    start = match.range.first,
                    end = match.range.last + 1,
                    type = HighlightType.KEYWORD,
                    color = 0xFF58A6FF // Blue
                ))
            }
        }
        
        // Highlight strings
        val stringRegex = "\"([^\"\\\\]|\\\\.)*\"".toRegex()
        stringRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.STRING,
                color = 0xFF7EE787 // Green
            ))
        }
        
        // Highlight comments
        val commentRegex = "//.*$".toRegex(RegexOption.MULTILINE)
        commentRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.COMMENT,
                color = 0xFF8B949E // Gray
            ))
        }
        
        // Highlight block comments
        val blockCommentRegex = "/\\*.*?\\*/".toRegex(RegexOption.DOT_MATCHES_ALL)
        blockCommentRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.COMMENT,
                color = 0xFF8B949E // Gray
            ))
        }
        
        // Highlight numbers
        val numberRegex = "\\b\\d+(\\.\\d+)?\\b".toRegex()
        numberRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.NUMBER,
                color = 0xFFFFA657 // Orange
            ))
        }
        
        return highlights.sortedBy { it.start }
    }
    
    private fun generateJavaHighlighting(content: String): List<SyntaxHighlight> {
        // Similar to Kotlin but with Java keywords
        val highlights = mutableListOf<SyntaxHighlight>()
        
        val keywords = listOf(
            "class", "interface", "enum", "extends", "implements", "import", "package",
            "public", "private", "protected", "static", "final", "abstract", "synchronized",
            "void", "int", "long", "float", "double", "boolean", "char", "byte", "short",
            "if", "else", "switch", "case", "default", "for", "while", "do", "break",
            "continue", "try", "catch", "finally", "throw", "throws", "return", "new",
            "this", "super", "null", "true", "false"
        )
        
        keywords.forEach { keyword ->
            val regex = "\\b$keyword\\b".toRegex()
            regex.findAll(content).forEach { match ->
                highlights.add(SyntaxHighlight(
                    start = match.range.first,
                    end = match.range.last + 1,
                    type = HighlightType.KEYWORD,
                    color = 0xFF58A6FF
                ))
            }
        }
        
        return highlights.sortedBy { it.start }
    }
    
    private fun generateXmlHighlighting(content: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        // Highlight XML tags
        val tagRegex = "<[^>]+>".toRegex()
        tagRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.KEYWORD,
                color = 0xFF58A6FF
            ))
        }
        
        // Highlight attribute values
        val attrRegex = "=\"[^\"]*\"".toRegex()
        attrRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first + 1,
                end = match.range.last,
                type = HighlightType.STRING,
                color = 0xFF7EE787
            ))
        }
        
        return highlights.sortedBy { it.start }
    }
    
    private fun generateJsonHighlighting(content: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        // Highlight JSON strings (keys and values)
        val stringRegex = "\"[^\"]*\"".toRegex()
        stringRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.STRING,
                color = 0xFF7EE787
            ))
        }
        
        // Highlight JSON numbers
        val numberRegex = "\\b\\d+(\\.\\d+)?\\b".toRegex()
        numberRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.NUMBER,
                color = 0xFFFFA657
            ))
        }
        
        // Highlight JSON booleans and null
        val boolNullRegex = "\\b(true|false|null)\\b".toRegex()
        boolNullRegex.findAll(content).forEach { match ->
            highlights.add(SyntaxHighlight(
                start = match.range.first,
                end = match.range.last + 1,
                type = HighlightType.KEYWORD,
                color = 0xFF58A6FF
            ))
        }
        
        return highlights.sortedBy { it.start }
    }
    
    /**
     * Check if auto-complete should be shown
     */
    private fun shouldShowAutoComplete(content: String, cursorPosition: Int): Boolean {
        if (cursorPosition == 0) return false
        
        val char = content.getOrNull(cursorPosition - 1)
        return when (char) {
            '.', ':', ' ' -> true
            else -> {
                // Show if typing a word
                val wordStart = findWordStart(content, cursorPosition)
                cursorPosition - wordStart >= 2
            }
        }
    }
    
    /**
     * Generate auto-complete items
     */
    private suspend fun generateAutoCompleteItems(content: String, cursorPosition: Int) {
        withContext(Dispatchers.Default) {
            val activeFile = getActiveFile() ?: return@withContext
            
            val items = when (activeFile.language) {
                CodeLanguage.KOTLIN -> generateKotlinAutoComplete(content, cursorPosition)
                CodeLanguage.JAVA -> generateJavaAutoComplete(content, cursorPosition)
                else -> emptyList()
            }
            
            _autoCompleteItems.value = items
            _isAutoCompleteVisible.value = items.isNotEmpty()
        }
    }
    
    private fun generateKotlinAutoComplete(content: String, cursorPosition: Int): List<AutoCompleteItem> {
        val items = mutableListOf<AutoCompleteItem>()
        
        // Add Kotlin keywords
        val keywords = listOf(
            "class", "fun", "val", "var", "if", "else", "when", "for", "while",
            "try", "catch", "finally", "throw", "return", "break", "continue",
            "import", "package", "private", "public", "internal", "protected",
            "override", "abstract", "open", "final", "companion", "object"
        )
        
        keywords.forEach { keyword ->
            items.add(AutoCompleteItem(
                text = keyword,
                description = "Kotlin keyword",
                insertText = keyword,
                type = CompletionType.KEYWORD
            ))
        }
        
        // Add common Android/Compose items if applicable
        if (content.contains("androidx.compose") || content.contains("@Composable")) {
            val composeItems = listOf(
                AutoCompleteItem("@Composable", "Compose function annotation", "@Composable", CompletionType.SNIPPET),
                AutoCompleteItem("remember", "Remember value in composition", "remember", CompletionType.FUNCTION),
                AutoCompleteItem("LaunchedEffect", "Launch coroutine in composition", "LaunchedEffect", CompletionType.FUNCTION),
                AutoCompleteItem("collectAsStateWithLifecycle", "Collect flow as state", "collectAsStateWithLifecycle()", CompletionType.FUNCTION)
            )
            items.addAll(composeItems)
        }
        
        return items.take(10) // Limit to 10 items
    }
    
    private fun generateJavaAutoComplete(content: String, cursorPosition: Int): List<AutoCompleteItem> {
        val items = mutableListOf<AutoCompleteItem>()
        
        // Add Java keywords
        val keywords = listOf(
            "class", "interface", "enum", "extends", "implements",
            "public", "private", "protected", "static", "final",
            "if", "else", "switch", "for", "while", "try", "catch"
        )
        
        keywords.forEach { keyword ->
            items.add(AutoCompleteItem(
                text = keyword,
                description = "Java keyword",
                insertText = keyword,
                type = CompletionType.KEYWORD
            ))
        }
        
        return items.take(10)
    }
    
    private fun findWordStart(content: String, position: Int): Int {
        var start = position - 1
        while (start > 0 && content[start].isLetterOrDigit()) {
            start--
        }
        return start + 1
    }
    
    /**
     * Insert auto-complete item
     */
    fun insertAutoCompleteItem(item: AutoCompleteItem, cursorPosition: Int) {
        val activeFile = getActiveFile() ?: return
        val content = activeFile.content
        
        val wordStart = findWordStart(content, cursorPosition)
        val newContent = content.substring(0, wordStart) +
                item.insertText +
                content.substring(cursorPosition)
        
        val newCursorPosition = wordStart + item.insertText.length
        updateActiveFileContent(newContent, newCursorPosition)
        
        _isAutoCompleteVisible.value = false
    }
    
    /**
     * Hide auto-complete
     */
    fun hideAutoComplete() {
        _isAutoCompleteVisible.value = false
    }
}

@Composable
fun rememberCodeEditor(): CodeEditor {
    return remember { CodeEditor() }
}