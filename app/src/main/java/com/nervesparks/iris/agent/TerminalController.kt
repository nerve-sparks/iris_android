package com.nervesparks.iris.agent

import android.content.Context
import com.nervesparks.iris.development.DevelopmentEnvironment
import com.nervesparks.iris.development.CommandResult
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Terminal Controller for Direct AI Control
 * Allows AI to execute terminal commands directly
 */

@Singleton
class TerminalController @Inject constructor(
    private val context: Context
) {
    private val developmentEnvironment = DevelopmentEnvironment(context)
    private var currentSession = developmentEnvironment.createTerminalSession("ai_direct")
    
    /**
     * Execute command directly for AI
     */
    suspend fun executeCommand(command: String): CommandResult {
        return currentSession.executeCommand(command)
    }
    
    /**
     * Get current working directory
     */
    fun getCurrentDirectory(): String {
        return currentSession.getCurrentDirectory()
    }
    
    /**
     * Change directory
     */
    suspend fun changeDirectory(path: String): CommandResult {
        return executeCommand("cd $path")
    }
    
    /**
     * List files in current directory
     */
    suspend fun listFiles(): CommandResult {
        return executeCommand("ls -la")
    }
    
    /**
     * Create file with content
     */
    suspend fun createFile(filename: String, content: String): CommandResult {
        return executeCommand("echo '$content' > '$filename'")
    }
    
    /**
     * Read file content
     */
    suspend fun readFile(filename: String): CommandResult {
        return executeCommand("cat '$filename'")
    }
    
    /**
     * Execute build command
     */
    suspend fun buildProject(): CommandResult {
        return executeCommand("./gradlew build")
    }
    
    /**
     * Run tests
     */
    suspend fun runTests(): CommandResult {
        return executeCommand("./gradlew test")
    }
    
    /**
     * Install APK
     */
    suspend fun installApp(): CommandResult {
        return executeCommand("./gradlew installDebug")
    }
    
    /**
     * Git operations
     */
    suspend fun gitStatus(): CommandResult {
        return executeCommand("git status")
    }
    
    suspend fun gitAdd(files: String = "."): CommandResult {
        return executeCommand("git add $files")
    }
    
    suspend fun gitCommit(message: String): CommandResult {
        return executeCommand("git commit -m '$message'")
    }
    
    suspend fun gitPush(): CommandResult {
        return executeCommand("git push")
    }
    
    fun cleanup() {
        currentSession.close()
    }
}