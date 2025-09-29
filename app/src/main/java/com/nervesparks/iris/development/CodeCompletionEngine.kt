package com.nervesparks.iris.development

/**
 * Code Completion Engine
 * Provides intelligent code completion for development environment
 */

class CodeCompletionEngine {
    
    private val kotlinKeywords = listOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
        "companion", "const", "constructor", "continue", "crossinline", "data",
        "delegate", "do", "else", "enum", "expect", "external", "false", "final",
        "finally", "for", "fun", "get", "if", "import", "in", "infix", "init",
        "inline", "inner", "interface", "internal", "is", "lateinit", "noinline",
        "null", "object", "open", "operator", "out", "override", "package",
        "private", "protected", "public", "reified", "return", "sealed", "set",
        "super", "suspend", "tailrec", "this", "throw", "true", "try", "typealias",
        "typeof", "val", "var", "vararg", "when", "where", "while"
    )
    
    private val javaKeywords = listOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new",
        "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
        "transient", "try", "void", "volatile", "while"
    )
    
    private val commonAndroidImports = mapOf(
        "Context" to "android.content.Context",
        "Activity" to "android.app.Activity",
        "Fragment" to "androidx.fragment.app.Fragment",
        "Bundle" to "android.os.Bundle",
        "Intent" to "android.content.Intent",
        "View" to "android.view.View",
        "ViewGroup" to "android.view.ViewGroup",
        "TextView" to "android.widget.TextView",
        "Button" to "android.widget.Button",
        "ImageView" to "android.widget.ImageView",
        "RecyclerView" to "androidx.recyclerview.widget.RecyclerView",
        "Composable" to "androidx.compose.runtime.Composable",
        "LazyColumn" to "androidx.compose.foundation.lazy.LazyColumn",
        "Text" to "androidx.compose.material3.Text",
        "Button" to "androidx.compose.material3.Button"
    )
    
    private val kotlinStandardLibrary = mapOf(
        "listOf" to "Creates a new read-only list",
        "mutableListOf" to "Creates a new mutable list",
        "mapOf" to "Creates a new read-only map",
        "mutableMapOf" to "Creates a new mutable map",
        "setOf" to "Creates a new read-only set",
        "mutableSetOf" to "Creates a new mutable set",
        "println" to "Prints a line to the standard output",
        "print" to "Prints to the standard output",
        "require" to "Throws IllegalArgumentException if condition is false",
        "check" to "Throws IllegalStateException if condition is false",
        "TODO" to "Always throws NotImplementedError",
        "runBlocking" to "Runs a coroutine blocking the current thread",
        "launch" to "Launches a new coroutine",
        "async" to "Creates a coroutine and returns its future result"
    )
    
    suspend fun getCompletions(filePath: String, content: String, cursorPosition: Int): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()
        
        // Get the word being typed
        val wordStart = findWordStart(content, cursorPosition)
        val wordEnd = findWordEnd(content, cursorPosition)
        val currentWord = content.substring(wordStart, wordEnd)
        
        // Get file extension to determine language
        val fileExtension = filePath.substringAfterLast(".")
        
        when (fileExtension) {
            "kt" -> {
                completions.addAll(getKotlinCompletions(content, currentWord, cursorPosition))
            }
            "java" -> {
                completions.addAll(getJavaCompletions(content, currentWord, cursorPosition))
            }
            "xml" -> {
                completions.addAll(getXmlCompletions(content, currentWord, cursorPosition))
            }
            "gradle" -> {
                completions.addAll(getGradleCompletions(content, currentWord, cursorPosition))
            }
        }
        
        return completions.filter { 
            it.label.startsWith(currentWord, ignoreCase = true) 
        }.sortedBy { it.label }
    }
    
    private fun getKotlinCompletions(content: String, currentWord: String, cursorPosition: Int): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()
        
        // Keywords
        kotlinKeywords.forEach { keyword ->
            completions.add(
                CompletionItem(
                    label = keyword,
                    detail = "Kotlin keyword",
                    insertText = keyword,
                    kind = CompletionKind.KEYWORD
                )
            )
        }
        
        // Standard library functions
        kotlinStandardLibrary.forEach { (function, description) ->
            completions.add(
                CompletionItem(
                    label = function,
                    detail = description,
                    insertText = "$function()",
                    kind = CompletionKind.METHOD
                )
            )
        }
        
        // Android imports
        commonAndroidImports.forEach { (className, fullImport) ->
            completions.add(
                CompletionItem(
                    label = className,
                    detail = fullImport,
                    insertText = className,
                    kind = CompletionKind.CLASS
                )
            )
        }
        
        // Context-aware completions
        if (isInFunctionContext(content, cursorPosition)) {
            completions.addAll(getFunctionCompletions())
        }
        
        if (isInClassContext(content, cursorPosition)) {
            completions.addAll(getClassCompletions())
        }
        
        // Compose-specific completions
        if (isInComposeContext(content)) {
            completions.addAll(getComposeCompletions())
        }
        
        return completions
    }
    
    private fun getJavaCompletions(content: String, currentWord: String, cursorPosition: Int): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()
        
        // Java keywords
        javaKeywords.forEach { keyword ->
            completions.add(
                CompletionItem(
                    label = keyword,
                    detail = "Java keyword",
                    insertText = keyword,
                    kind = CompletionKind.KEYWORD
                )
            )
        }
        
        // Common Java patterns
        completions.addAll(getJavaPatterns())
        
        return completions
    }
    
    private fun getXmlCompletions(content: String, currentWord: String, cursorPosition: Int): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()
        
        // Android XML attributes
        val androidAttributes = listOf(
            "android:layout_width", "android:layout_height", "android:text",
            "android:textSize", "android:textColor", "android:background",
            "android:padding", "android:margin", "android:gravity",
            "android:orientation", "android:visibility", "android:id"
        )
        
        androidAttributes.forEach { attr ->
            completions.add(
                CompletionItem(
                    label = attr,
                    detail = "Android XML attribute",
                    insertText = "$attr=\"\"",
                    kind = CompletionKind.VARIABLE
                )
            )
        }
        
        return completions
    }
    
    private fun getGradleCompletions(content: String, currentWord: String, cursorPosition: Int): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()
        
        // Gradle DSL
        val gradleDsl = mapOf(
            "apply" to "apply plugin: ''",
            "dependencies" to "dependencies {\n    \n}",
            "android" to "android {\n    \n}",
            "implementation" to "implementation ''",
            "testImplementation" to "testImplementation ''",
            "androidTestImplementation" to "androidTestImplementation ''"
        )
        
        gradleDsl.forEach { (keyword, template) ->
            completions.add(
                CompletionItem(
                    label = keyword,
                    detail = "Gradle DSL",
                    insertText = template,
                    kind = CompletionKind.SNIPPET
                )
            )
        }
        
        return completions
    }
    
    private fun getFunctionCompletions(): List<CompletionItem> {
        return listOf(
            CompletionItem(
                label = "override",
                detail = "Override function",
                insertText = "override fun ${1:functionName}(): ${2:ReturnType} {\n    ${3:// TODO}\n}",
                kind = CompletionKind.SNIPPET
            ),
            CompletionItem(
                label = "suspend",
                detail = "Suspend function",
                insertText = "suspend fun ${1:functionName}(): ${2:ReturnType} {\n    ${3:// TODO}\n}",
                kind = CompletionKind.SNIPPET
            )
        )
    }
    
    private fun getClassCompletions(): List<CompletionItem> {
        return listOf(
            CompletionItem(
                label = "data class",
                detail = "Data class",
                insertText = "data class ${1:ClassName}(${2:properties})",
                kind = CompletionKind.SNIPPET
            ),
            CompletionItem(
                label = "companion object",
                detail = "Companion object",
                insertText = "companion object {\n    ${1:// TODO}\n}",
                kind = CompletionKind.SNIPPET
            )
        )
    }
    
    private fun getComposeCompletions(): List<CompletionItem> {
        return listOf(
            CompletionItem(
                label = "@Composable",
                detail = "Composable function",
                insertText = "@Composable\nfun ${1:FunctionName}() {\n    ${2:// TODO}\n}",
                kind = CompletionKind.SNIPPET
            ),
            CompletionItem(
                label = "LazyColumn",
                detail = "Lazy scrollable column",
                insertText = "LazyColumn {\n    items(${1:items}) { ${2:item} ->\n        ${3:// TODO}\n    }\n}",
                kind = CompletionKind.SNIPPET
            ),
            CompletionItem(
                label = "remember",
                detail = "Remember state",
                insertText = "remember { ${1:mutableStateOf(${2:initialValue})} }",
                kind = CompletionKind.SNIPPET
            )
        )
    }
    
    private fun getJavaPatterns(): List<CompletionItem> {
        return listOf(
            CompletionItem(
                label = "main",
                detail = "Main method",
                insertText = "public static void main(String[] args) {\n    ${1:// TODO}\n}",
                kind = CompletionKind.SNIPPET
            ),
            CompletionItem(
                label = "sout",
                detail = "System.out.println",
                insertText = "System.out.println(${1:});",
                kind = CompletionKind.SNIPPET
            )
        )
    }
    
    private fun findWordStart(content: String, position: Int): Int {
        var start = position
        while (start > 0 && (content[start - 1].isLetterOrDigit() || content[start - 1] == '_')) {
            start--
        }
        return start
    }
    
    private fun findWordEnd(content: String, position: Int): Int {
        var end = position
        while (end < content.length && (content[end].isLetterOrDigit() || content[end] == '_')) {
            end++
        }
        return end
    }
    
    private fun isInFunctionContext(content: String, cursorPosition: Int): Boolean {
        val beforeCursor = content.substring(0, cursorPosition)
        return beforeCursor.contains("fun ") && !beforeCursor.contains("class ")
    }
    
    private fun isInClassContext(content: String, cursorPosition: Int): Boolean {
        val beforeCursor = content.substring(0, cursorPosition)
        return beforeCursor.contains("class ") || beforeCursor.contains("object ")
    }
    
    private fun isInComposeContext(content: String): Boolean {
        return content.contains("@Composable") || content.contains("androidx.compose")
    }
}