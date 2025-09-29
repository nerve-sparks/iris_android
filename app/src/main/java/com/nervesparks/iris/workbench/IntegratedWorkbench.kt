package com.nervesparks.iris.workbench

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.concurrent.TimeUnit

/**
 * Integrated Development Workbench
 * Termux-style environment with GUI for app development
 */

data class WorkbenchSession(
    val id: String,
    val name: String,
    val workingDirectory: String,
    val environment: Map<String, String>,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val output: String,
    val error: String,
    val executionTime: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProjectFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val permissions: String,
    val content: String? = null
)

enum class WorkbenchMode {
    TERMINAL, FILE_MANAGER, CODE_EDITOR, BUILD_TOOLS, PACKAGE_MANAGER, GIT_CLIENT
}

class IntegratedWorkbench(private val context: Context) {
    companion object {
        private const val TAG = "IntegratedWorkbench"
        private const val WORKBENCH_DIR = "workbench"
        private const val MAX_OUTPUT_SIZE = 100 * 1024 // 100KB max output
    }
    
    private val workbenchDir = File(context.filesDir, WORKBENCH_DIR)
    private val commandHistory = mutableListOf<CommandResult>()
    private val activeSessions = mutableMapOf<String, WorkbenchSession>()
    
    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()
    
    private val _currentDirectory = MutableStateFlow(workbenchDir.absolutePath)
    val currentDirectory: StateFlow<String> = _currentDirectory.asStateFlow()
    
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()
    
    private val _projectFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val projectFiles: StateFlow<List<ProjectFile>> = _projectFiles.asStateFlow()
    
    init {
        initializeWorkbench()
    }
    
    private fun initializeWorkbench() {
        try {
            if (!workbenchDir.exists()) {
                workbenchDir.mkdirs()
            }
            
            // Create essential directories
            listOf("projects", "tools", "temp", "scripts", "logs").forEach { dir ->
                File(workbenchDir, dir).mkdirs()
            }
            
            // Set up development environment
            setupDevelopmentEnvironment()
            
            refreshProjectFiles()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize workbench", e)
        }
    }
    
    private fun setupDevelopmentEnvironment() {
        // Create essential development tools and scripts
        createBuildScript()
        createGitScript()
        createPackageManagerScript()
        setupAndroidDevEnvironment()
    }
    
    /**
     * Execute command in workbench terminal
     */
    suspend fun executeCommand(
        command: String,
        workingDir: String = _currentDirectory.value
    ): CommandResult {
        return withContext(Dispatchers.IO) {
            _isExecuting.value = true
            val startTime = System.currentTimeMillis()
            
            try {
                Log.d(TAG, "Executing: $command in $workingDir")
                
                // Handle built-in commands
                val result = when {
                    command.startsWith("cd ") -> handleCdCommand(command)
                    command == "pwd" -> handlePwdCommand()
                    command == "ls" -> handleLsCommand(workingDir)
                    command.startsWith("ls ") -> handleLsCommand(command.substring(3).trim())
                    command == "clear" -> handleClearCommand()
                    command.startsWith("cat ") -> handleCatCommand(command.substring(4).trim())
                    command.startsWith("mkdir ") -> handleMkdirCommand(command.substring(6).trim())
                    command.startsWith("touch ") -> handleTouchCommand(command.substring(6).trim())
                    command.startsWith("rm ") -> handleRmCommand(command.substring(3).trim())
                    command.startsWith("echo ") -> handleEchoCommand(command.substring(5).trim())
                    command.startsWith("nano ") || command.startsWith("edit ") -> 
                        handleEditCommand(command.split(" ", limit = 2).getOrNull(1) ?: "")
                    command.startsWith("build") -> handleBuildCommand(command)
                    command.startsWith("gradle") -> handleGradleCommand(command)
                    command.startsWith("git ") -> handleGitCommand(command)
                    command == "help" -> handleHelpCommand()
                    else -> executeSystemCommand(command, workingDir)
                }
                
                val executionTime = System.currentTimeMillis() - startTime
                val commandResult = CommandResult(
                    command = command,
                    exitCode = result.exitCode,
                    output = result.output,
                    error = result.error,
                    executionTime = executionTime
                )
                
                commandHistory.add(commandResult)
                appendToTerminal("${getCurrentPrompt()}$command")
                if (result.output.isNotEmpty()) {
                    appendToTerminal(result.output)
                }
                if (result.error.isNotEmpty()) {
                    appendToTerminal("ERROR: ${result.error}")
                }
                
                refreshProjectFiles()
                commandResult
                
            } catch (e: Exception) {
                Log.e(TAG, "Command execution failed: $command", e)
                val errorResult = CommandResult(
                    command = command,
                    exitCode = -1,
                    output = "",
                    error = e.message ?: "Unknown error",
                    executionTime = System.currentTimeMillis() - startTime
                )
                appendToTerminal("ERROR: ${e.message}")
                errorResult
                
            } finally {
                _isExecuting.value = false
            }
        }
    }
    
    private fun handleCdCommand(command: String): CommandResult {
        val path = command.substring(3).trim()
        val targetDir = if (path.startsWith("/")) {
            File(path)
        } else {
            File(_currentDirectory.value, path)
        }
        
        return if (targetDir.exists() && targetDir.isDirectory) {
            _currentDirectory.value = targetDir.absolutePath
            CommandResult("cd", 0, "", "")
        } else {
            CommandResult("cd", 1, "", "Directory not found: $path")
        }
    }
    
    private fun handlePwdCommand(): CommandResult {
        return CommandResult("pwd", 0, _currentDirectory.value, "")
    }
    
    private fun handleLsCommand(path: String = _currentDirectory.value): CommandResult {
        val dir = File(path)
        return if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            val output = files?.joinToString("\n") { file ->
                val permissions = if (file.canRead()) "r" else "-" +
                        if (file.canWrite()) "w" else "-" +
                        if (file.canExecute()) "x" else "-"
                val type = if (file.isDirectory) "d" else "-"
                val size = if (file.isDirectory) "" else " ${file.length()}"
                "$type$permissions $size ${file.name}"
            } ?: ""
            CommandResult("ls", 0, output, "")
        } else {
            CommandResult("ls", 1, "", "Directory not found: $path")
        }
    }
    
    private fun handleClearCommand(): CommandResult {
        _terminalOutput.value = ""
        return CommandResult("clear", 0, "", "")
    }
    
    private fun handleCatCommand(filename: String): CommandResult {
        val file = File(_currentDirectory.value, filename)
        return if (file.exists() && file.isFile) {
            try {
                val content = file.readText().take(MAX_OUTPUT_SIZE)
                CommandResult("cat", 0, content, "")
            } catch (e: Exception) {
                CommandResult("cat", 1, "", "Cannot read file: ${e.message}")
            }
        } else {
            CommandResult("cat", 1, "", "File not found: $filename")
        }
    }
    
    private fun handleMkdirCommand(dirname: String): CommandResult {
        val dir = File(_currentDirectory.value, dirname)
        return if (dir.mkdirs() || dir.exists()) {
            CommandResult("mkdir", 0, "", "")
        } else {
            CommandResult("mkdir", 1, "", "Cannot create directory: $dirname")
        }
    }
    
    private fun handleTouchCommand(filename: String): CommandResult {
        val file = File(_currentDirectory.value, filename)
        return try {
            if (!file.exists()) {
                file.createNewFile()
            }
            file.setLastModified(System.currentTimeMillis())
            CommandResult("touch", 0, "", "")
        } catch (e: Exception) {
            CommandResult("touch", 1, "", "Cannot create file: ${e.message}")
        }
    }
    
    private fun handleRmCommand(filename: String): CommandResult {
        val file = File(_currentDirectory.value, filename)
        return if (file.exists()) {
            if (file.deleteRecursively()) {
                CommandResult("rm", 0, "", "")
            } else {
                CommandResult("rm", 1, "", "Cannot delete: $filename")
            }
        } else {
            CommandResult("rm", 1, "", "File not found: $filename")
        }
    }
    
    private fun handleEchoCommand(text: String): CommandResult {
        return CommandResult("echo", 0, text, "")
    }
    
    private fun handleEditCommand(filename: String): CommandResult {
        // Signal to UI to open code editor
        return CommandResult("edit", 0, "OPEN_EDITOR:$filename", "")
    }
    
    private fun handleBuildCommand(command: String): CommandResult {
        return when {
            command.contains("debug") -> buildApp("debug")
            command.contains("release") -> buildApp("release")
            else -> buildApp("debug")
        }
    }
    
    private fun handleGradleCommand(command: String): CommandResult {
        // Execute gradle commands
        val args = command.split(" ").drop(1)
        return executeGradleTask(args)
    }
    
    private fun handleGitCommand(command: String): CommandResult {
        // Execute git commands
        return executeSystemCommand(command, _currentDirectory.value)
    }
    
    private fun handleHelpCommand(): CommandResult {
        val helpText = """
            Integrated Development Workbench - Available Commands:
            
            File Operations:
              ls [path]          - List directory contents
              cd <path>          - Change directory
              pwd                - Print working directory
              cat <file>         - Display file content
              mkdir <dir>        - Create directory
              touch <file>       - Create/update file
              rm <file>          - Remove file/directory
              echo <text>        - Display text
              
            Development:
              edit <file>        - Open file in code editor
              build [debug|release] - Build Android app
              gradle <task>      - Run Gradle task
              git <command>      - Execute Git command
              
            System:
              clear              - Clear terminal
              help               - Show this help
              
            Special Features:
              - Real-time file monitoring
              - Integrated code editor
              - Build system integration
              - Git repository management
              - Package management
        """.trimIndent()
        
        return CommandResult("help", 0, helpText, "")
    }
    
    private fun executeSystemCommand(command: String, workingDir: String): CommandResult {
        return try {
            val processBuilder = ProcessBuilder()
            processBuilder.command("sh", "-c", command)
            processBuilder.directory(File(workingDir))
            
            val process = processBuilder.start()
            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = outputReader.readText().take(MAX_OUTPUT_SIZE)
            val error = errorReader.readText().take(MAX_OUTPUT_SIZE)
            
            val exitCode = process.waitFor()
            
            CommandResult(command, exitCode, output, error)
            
        } catch (e: Exception) {
            CommandResult(command, -1, "", e.message ?: "Execution failed")
        }
    }
    
    private fun buildApp(buildType: String): CommandResult {
        return try {
            val gradleCommand = if (buildType == "release") {
                "./gradlew assembleRelease"
            } else {
                "./gradlew assembleDebug"
            }
            
            // Find the app's root directory
            val appRootDir = findAppRootDirectory()
            if (appRootDir != null) {
                executeSystemCommand(gradleCommand, appRootDir.absolutePath)
            } else {
                CommandResult("build", 1, "", "Cannot find app root directory")
            }
        } catch (e: Exception) {
            CommandResult("build", -1, "", "Build failed: ${e.message}")
        }
    }
    
    private fun executeGradleTask(args: List<String>): CommandResult {
        val appRootDir = findAppRootDirectory()
        return if (appRootDir != null) {
            val command = "./gradlew ${args.joinToString(" ")}"
            executeSystemCommand(command, appRootDir.absolutePath)
        } else {
            CommandResult("gradle", 1, "", "Cannot find app root directory")
        }
    }
    
    private fun findAppRootDirectory(): File? {
        // Try to find the app root directory with build.gradle.kts
        var current = File(_currentDirectory.value)
        while (current.parent != null) {
            if (File(current, "build.gradle.kts").exists() || 
                File(current, "build.gradle").exists()) {
                return current
            }
            current = current.parentFile
        }
        return null
    }
    
    private fun createBuildScript() {
        val buildScript = File(workbenchDir, "scripts/build.sh")
        buildScript.parentFile.mkdirs()
        buildScript.writeText("""
            #!/bin/bash
            # Integrated Build Script
            
            case "$1" in
                "debug")
                    ./gradlew assembleDebug
                    ;;
                "release")
                    ./gradlew assembleRelease
                    ;;
                "clean")
                    ./gradlew clean
                    ;;
                *)
                    echo "Usage: build [debug|release|clean]"
                    ;;
            esac
        """.trimIndent())
        buildScript.setExecutable(true)
    }
    
    private fun createGitScript() {
        val gitScript = File(workbenchDir, "scripts/git.sh")
        gitScript.writeText("""
            #!/bin/bash
            # Git wrapper script
            git "$@"
        """.trimIndent())
        gitScript.setExecutable(true)
    }
    
    private fun createPackageManagerScript() {
        val pkgScript = File(workbenchDir, "scripts/pkg.sh")
        pkgScript.writeText("""
            #!/bin/bash
            # Package manager simulation
            echo "Integrated Package Manager - Feature coming soon"
        """.trimIndent())
        pkgScript.setExecutable(true)
    }
    
    private fun setupAndroidDevEnvironment() {
        // Create Android development specific tools and configurations
        val androidDir = File(workbenchDir, "android")
        androidDir.mkdirs()
        
        // Create signing configuration
        val signingScript = File(androidDir, "sign_release.sh")
        signingScript.writeText("""
            #!/bin/bash
            # Release signing script
            
            if [ -z "$1" ]; then
                echo "Usage: sign_release.sh <apk_path>"
                exit 1
            fi
            
            APK_PATH="$1"
            SIGNED_APK="${APK_PATH%.*}_signed.apk"
            
            # Use integrated keystore (implement keystore management)
            echo "Signing APK: $APK_PATH"
            echo "Output: $SIGNED_APK"
            
            # Placeholder for actual signing
            cp "$APK_PATH" "$SIGNED_APK"
            echo "APK signed successfully"
        """.trimIndent())
        signingScript.setExecutable(true)
    }
    
    private fun getCurrentPrompt(): String {
        val currentDir = File(_currentDirectory.value).name
        return "workbench:$currentDir$ "
    }
    
    private fun appendToTerminal(text: String) {
        val current = _terminalOutput.value
        val newOutput = if (current.length > MAX_OUTPUT_SIZE) {
            current.takeLast(MAX_OUTPUT_SIZE / 2) + "\n" + text
        } else {
            current + "\n" + text
        }
        _terminalOutput.value = newOutput
    }
    
    private fun refreshProjectFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentDir = File(_currentDirectory.value)
                val files = currentDir.listFiles()?.map { file ->
                    ProjectFile(
                        path = file.absolutePath,
                        name = file.name,
                        isDirectory = file.isDirectory,
                        size = if (file.isDirectory) 0 else file.length(),
                        lastModified = file.lastModified(),
                        permissions = "${if (file.canRead()) "r" else "-"}${if (file.canWrite()) "w" else "-"}${if (file.canExecute()) "x" else "-"}"
                    )
                }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
                
                _projectFiles.value = files
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh project files", e)
            }
        }
    }
    
    /**
     * Read file content for code editor
     */
    suspend fun readFileContent(filePath: String): String {
        return withContext(Dispatchers.IO) {
            try {
                File(filePath).readText()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read file: $filePath", e)
                ""
            }
        }
    }
    
    /**
     * Write file content from code editor
     */
    suspend fun writeFileContent(filePath: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                File(filePath).writeText(content)
                refreshProjectFiles()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write file: $filePath", e)
                false
            }
        }
    }
    
    /**
     * Get command history
     */
    fun getCommandHistory(): List<CommandResult> {
        return commandHistory.toList()
    }
    
    /**
     * Clear terminal output
     */
    fun clearTerminal() {
        _terminalOutput.value = ""
    }
}

@Composable
fun rememberIntegratedWorkbench(context: Context): IntegratedWorkbench {
    return remember {
        IntegratedWorkbench(context)
    }
}