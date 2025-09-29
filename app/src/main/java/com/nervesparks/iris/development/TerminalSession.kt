package com.nervesparks.iris.development

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Terminal Session for Development Environment
 * Manages individual terminal instances with state
 */

class TerminalSession(
    val sessionId: String,
    initialDirectory: String,
    private val environment: DevelopmentEnvironment
) {
    private var currentDirectory = initialDirectory
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Terminal output buffer
    private val _output = MutableSharedFlow<TerminalOutput>()
    val output: SharedFlow<TerminalOutput> = _output.asSharedFlow()
    
    // Command execution state
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()
    
    // Session environment variables
    private val environmentVars = mutableMapOf<String, String>().apply {
        put("HOME", initialDirectory)
        put("PATH", "/system/bin:/system/xbin:$initialDirectory/tools")
        put("PWD", currentDirectory)
        put("USER", "iris_dev")
        put("SHELL", "/bin/sh")
    }
    
    init {
        // Send welcome message
        scope.launch {
            _output.emit(
                TerminalOutput(
                    type = OutputType.SYSTEM,
                    content = "Iris Development Terminal v1.0\n" +
                            "Session: $sessionId\n" +
                            "Working Directory: $currentDirectory\n" +
                            "Type 'help' for available commands.\n",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Execute command in this session
     */
    suspend fun executeCommand(command: String): CommandResult {
        if (_isExecuting.value) {
            return CommandResult(false, "", "Another command is already running", -1)
        }
        
        _isExecuting.value = true
        
        try {
            // Echo command
            _output.emit(
                TerminalOutput(
                    type = OutputType.COMMAND,
                    content = "$ $command",
                    timestamp = System.currentTimeMillis()
                )
            )
            
            // Handle directory change locally
            if (command.startsWith("cd ")) {
                val result = handleDirectoryChange(command)
                emitCommandResult(result)
                return result
            }
            
            // Execute through environment
            val result = environment.executeCommand(command, currentDirectory)
            
            // Update environment variables if needed
            if (result.success && command == "pwd") {
                environmentVars["PWD"] = result.output.trim()
            }
            
            emitCommandResult(result)
            return result
            
        } finally {
            _isExecuting.value = false
        }
    }
    
    private fun handleDirectoryChange(command: String): CommandResult {
        val target = command.substring(3).trim()
        
        val newDir = when {
            target.isEmpty() -> environmentVars["HOME"] ?: currentDirectory
            target == "~" -> environmentVars["HOME"] ?: currentDirectory
            target.startsWith("/") -> target
            target == ".." -> File(currentDirectory).parent ?: currentDirectory
            target == "." -> currentDirectory
            else -> File(currentDirectory, target).absolutePath
        }
        
        return if (File(newDir).exists() && File(newDir).isDirectory) {
            currentDirectory = newDir
            environmentVars["PWD"] = newDir
            CommandResult(true, "", "", 0)
        } else {
            CommandResult(false, "", "cd: no such file or directory: $target", 1)
        }
    }
    
    private suspend fun emitCommandResult(result: CommandResult) {
        if (result.output.isNotEmpty()) {
            _output.emit(
                TerminalOutput(
                    type = OutputType.STDOUT,
                    content = result.output,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        if (result.error.isNotEmpty()) {
            _output.emit(
                TerminalOutput(
                    type = OutputType.STDERR,
                    content = result.error,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    /**
     * Send signal to running process (like Ctrl+C)
     */
    suspend fun sendSignal(signal: Signal) {
        when (signal) {
            Signal.INTERRUPT -> {
                _output.emit(
                    TerminalOutput(
                        type = OutputType.SYSTEM,
                        content = "^C\n",
                        timestamp = System.currentTimeMillis()
                    )
                )
                // Cancel current execution
                _isExecuting.value = false
            }
            Signal.EOF -> {
                _output.emit(
                    TerminalOutput(
                        type = OutputType.SYSTEM,
                        content = "^D\n",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    /**
     * Get current working directory
     */
    fun getCurrentDirectory(): String = currentDirectory
    
    /**
     * Get environment variable
     */
    fun getEnvironmentVariable(name: String): String? = environmentVars[name]
    
    /**
     * Set environment variable
     */
    fun setEnvironmentVariable(name: String, value: String) {
        environmentVars[name] = value
    }
    
    /**
     * Get all environment variables
     */
    fun getEnvironmentVariables(): Map<String, String> = environmentVars.toMap()
    
    /**
     * Get session prompt
     */
    fun getPrompt(): String {
        val user = environmentVars["USER"] ?: "iris_dev"
        val currentDir = File(currentDirectory).name
        return "$user@iris:$currentDir$ "
    }
    
    /**
     * Clear terminal output
     */
    suspend fun clear() {
        _output.emit(
            TerminalOutput(
                type = OutputType.CLEAR,
                content = "",
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Close session and cleanup
     */
    fun close() {
        scope.cancel()
    }
}

/**
 * Terminal output data
 */
data class TerminalOutput(
    val type: OutputType,
    val content: String,
    val timestamp: Long
)

enum class OutputType {
    COMMAND,    // User command echo
    STDOUT,     // Standard output
    STDERR,     // Error output
    SYSTEM,     // System messages
    CLEAR       // Clear screen
}

enum class Signal {
    INTERRUPT,  // Ctrl+C
    EOF         // Ctrl+D
}