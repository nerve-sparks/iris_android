package com.nervesparks.iris.development

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-App Development Environment
 * Termux-style CLI/GUI hybrid for development within the app
 */

@Singleton
class DevelopmentEnvironment @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Development workspace directory
    private val devWorkspace: File = File(context.filesDir, "dev_workspace")
    
    // Active terminal sessions
    private val terminalSessions = ConcurrentHashMap<String, TerminalSession>()
    
    // Command history
    private val _commandHistory = MutableStateFlow<List<CommandHistoryEntry>>(emptyList())
    val commandHistory: StateFlow<List<CommandHistoryEntry>> = _commandHistory.asStateFlow()
    
    // File system watcher
    private val _fileSystemChanges = MutableSharedFlow<FileSystemEvent>()
    val fileSystemChanges: SharedFlow<FileSystemEvent> = _fileSystemChanges.asSharedFlow()
    
    // Build system integration
    private val buildSystem = BuildSystemManager(context)
    
    // Code completion engine
    private val codeCompletion = CodeCompletionEngine()
    
    init {
        initializeWorkspace()
    }
    
    private fun initializeWorkspace() {
        scope.launch {
            // Create workspace structure
            devWorkspace.mkdirs()
            
            // Initialize project directories
            val projectDirs = listOf(
                "projects",
                "tools",
                "templates",
                "scripts",
                "builds",
                "logs"
            )
            
            projectDirs.forEach { dir ->
                File(devWorkspace, dir).mkdirs()
            }
            
            // Copy development tools
            copyDevelopmentTools()
            
            // Initialize version control
            initializeGit()
        }
    }
    
    private suspend fun copyDevelopmentTools() {
        val tools = mapOf(
            "gradle-wrapper.jar" to "tools/gradle/",
            "kotlin-compiler.jar" to "tools/kotlin/",
            "build-tools.jar" to "tools/build/"
        )
        
        tools.forEach { (source, destination) ->
            try {
                val destDir = File(devWorkspace, destination)
                destDir.mkdirs()
                
                context.assets.open("dev_tools/$source").use { input ->
                    File(destDir, source).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Tool not found, that's okay
            }
        }
    }
    
    private suspend fun initializeGit() {
        executeCommand("git init", devWorkspace.absolutePath)
        executeCommand("git config user.name 'Iris Dev'", devWorkspace.absolutePath)
        executeCommand("git config user.email 'dev@iris.local'", devWorkspace.absolutePath)
    }
    
    /**
     * Create a new terminal session
     */
    fun createTerminalSession(sessionId: String, initialDirectory: String = devWorkspace.absolutePath): TerminalSession {
        val session = TerminalSession(sessionId, initialDirectory, this)
        terminalSessions[sessionId] = session
        return session
    }
    
    /**
     * Get existing terminal session
     */
    fun getTerminalSession(sessionId: String): TerminalSession? {
        return terminalSessions[sessionId]
    }
    
    /**
     * Execute command in development environment
     */
    suspend fun executeCommand(command: String, workingDirectory: String): CommandResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = when {
                command.startsWith("cd ") -> handleCdCommand(command, workingDirectory)
                command.startsWith("mkdir ") -> handleMkdirCommand(command, workingDirectory)
                command.startsWith("ls") -> handleLsCommand(command, workingDirectory)
                command.startsWith("cat ") -> handleCatCommand(command, workingDirectory)
                command.startsWith("echo ") -> handleEchoCommand(command, workingDirectory)
                command.startsWith("gradle ") -> buildSystem.executeGradleCommand(command, workingDirectory)
                command.startsWith("git ") -> executeGitCommand(command, workingDirectory)
                command == "clear" -> CommandResult(true, "", "", 0)
                command == "pwd" -> CommandResult(true, workingDirectory, "", 0)
                command == "help" -> getHelpText()
                else -> executeSystemCommand(command, workingDirectory)
            }
            
            // Add to command history
            val historyEntry = CommandHistoryEntry(
                command = command,
                workingDirectory = workingDirectory,
                result = result,
                timestamp = System.currentTimeMillis(),
                duration = System.currentTimeMillis() - startTime
            )
            
            _commandHistory.value = _commandHistory.value + historyEntry
            
            result
        } catch (e: Exception) {
            CommandResult(false, "", e.message ?: "Unknown error", -1)
        }
    }
    
    private fun handleCdCommand(command: String, currentDir: String): CommandResult {
        val target = command.substring(3).trim()
        val newDir = if (target.startsWith("/")) {
            target
        } else {
            File(currentDir, target).absolutePath
        }
        
        return if (File(newDir).exists() && File(newDir).isDirectory) {
            CommandResult(true, newDir, "", 0)
        } else {
            CommandResult(false, "", "Directory not found: $target", 1)
        }
    }
    
    private fun handleMkdirCommand(command: String, workingDirectory: String): CommandResult {
        val dirName = command.substring(6).trim()
        val targetDir = File(workingDirectory, dirName)
        
        return if (targetDir.mkdirs() || targetDir.exists()) {
            _fileSystemChanges.tryEmit(FileSystemEvent.DirectoryCreated(targetDir.absolutePath))
            CommandResult(true, "", "", 0)
        } else {
            CommandResult(false, "", "Failed to create directory: $dirName", 1)
        }
    }
    
    private fun handleLsCommand(command: String, workingDirectory: String): CommandResult {
        val dir = File(workingDirectory)
        val files = dir.listFiles() ?: emptyArray()
        
        val output = files.joinToString("\n") { file ->
            val permissions = if (file.isDirectory()) "drwxr-xr-x" else "-rw-r--r--"
            val size = if (file.isDirectory()) "4096" else file.length().toString()
            val name = if (file.isDirectory()) "${file.name}/" else file.name
            "$permissions  $size  $name"
        }
        
        return CommandResult(true, output, "", 0)
    }
    
    private fun handleCatCommand(command: String, workingDirectory: String): CommandResult {
        val fileName = command.substring(4).trim()
        val file = File(workingDirectory, fileName)
        
        return if (file.exists() && file.isFile) {
            try {
                val content = file.readText()
                CommandResult(true, content, "", 0)
            } catch (e: Exception) {
                CommandResult(false, "", "Error reading file: ${e.message}", 1)
            }
        } else {
            CommandResult(false, "", "File not found: $fileName", 1)
        }
    }
    
    private fun handleEchoCommand(command: String, workingDirectory: String): CommandResult {
        val text = command.substring(5).trim()
        return CommandResult(true, text, "", 0)
    }
    
    private suspend fun executeGitCommand(command: String, workingDirectory: String): CommandResult {
        return executeSystemCommand(command, workingDirectory)
    }
    
    private suspend fun executeSystemCommand(command: String, workingDirectory: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder()
                .command(command.split(" "))
                .directory(File(workingDirectory))
            
            val process = processBuilder.start()
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            CommandResult(exitCode == 0, output, error, exitCode)
        } catch (e: Exception) {
            CommandResult(false, "", "Command execution failed: ${e.message}", -1)
        }
    }
    
    private fun getHelpText(): CommandResult {
        val helpText = """
        Iris Development Environment - Commands:
        
        File System:
        - ls                List directory contents
        - cd <dir>          Change directory
        - mkdir <dir>       Create directory
        - cat <file>        Display file contents
        - echo <text>       Echo text
        - pwd               Show current directory
        
        Development:
        - gradle <task>     Run Gradle task
        - git <command>     Git version control
        - build             Build current project
        - run               Run application
        - test              Run tests
        
        Tools:
        - edit <file>       Open file in editor
        - search <term>     Search in files
        - template <name>   Create from template
        
        System:
        - clear             Clear terminal
        - help              Show this help
        - exit              Exit session
        """.trimIndent()
        
        return CommandResult(true, helpText, "", 0)
    }
    
    /**
     * Create a new project from template
     */
    suspend fun createProject(
        name: String,
        template: ProjectTemplate,
        targetDirectory: String = devWorkspace.absolutePath
    ): ProjectCreationResult {
        return try {
            val projectDir = File(targetDirectory, name)
            projectDir.mkdirs()
            
            // Copy template files
            template.files.forEach { (path, content) ->
                val file = File(projectDir, path)
                file.parentFile?.mkdirs()
                file.writeText(content)
            }
            
            // Initialize build system
            when (template.type) {
                ProjectType.ANDROID -> buildSystem.initializeAndroidProject(projectDir)
                ProjectType.KOTLIN -> buildSystem.initializeKotlinProject(projectDir)
                ProjectType.JAVA -> buildSystem.initializeJavaProject(projectDir)
            }
            
            // Initialize git repo
            executeCommand("git init", projectDir.absolutePath)
            executeCommand("git add .", projectDir.absolutePath)
            executeCommand("git commit -m 'Initial commit'", projectDir.absolutePath)
            
            _fileSystemChanges.emit(FileSystemEvent.ProjectCreated(projectDir.absolutePath))
            
            ProjectCreationResult(true, projectDir.absolutePath, null)
        } catch (e: Exception) {
            ProjectCreationResult(false, "", e.message)
        }
    }
    
    /**
     * Get available project templates
     */
    fun getProjectTemplates(): List<ProjectTemplate> {
        return listOf(
            ProjectTemplate(
                name = "Android App",
                description = "Basic Android application with Compose UI",
                type = ProjectType.ANDROID,
                files = getAndroidTemplateFiles()
            ),
            ProjectTemplate(
                name = "Kotlin Library",
                description = "Kotlin/JVM library project",
                type = ProjectType.KOTLIN,
                files = getKotlinTemplateFiles()
            ),
            ProjectTemplate(
                name = "Java Console",
                description = "Java console application",
                type = ProjectType.JAVA,
                files = getJavaTemplateFiles()
            )
        )
    }
    
    private fun getAndroidTemplateFiles(): Map<String, String> {
        return mapOf(
            "build.gradle.kts" to """
                plugins {
                    id("com.android.application")
                    id("org.jetbrains.kotlin.android")
                }
                
                android {
                    compileSdk 34
                    
                    defaultConfig {
                        applicationId "com.example.myapp"
                        minSdk 24
                        targetSdk 34
                        versionCode 1
                        versionName "1.0"
                    }
                    
                    buildFeatures {
                        compose = true
                    }
                    
                    composeOptions {
                        kotlinCompilerExtensionVersion = "1.5.8"
                    }
                }
                
                dependencies {
                    implementation("androidx.compose.ui:ui:1.5.8")
                    implementation("androidx.activity:activity-compose:1.8.2")
                }
            """.trimIndent(),
            
            "src/main/java/com/example/myapp/MainActivity.kt" to """
                package com.example.myapp
                
                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.Text
                import androidx.compose.runtime.Composable
                
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            MyApp()
                        }
                    }
                }
                
                @Composable
                fun MyApp() {
                    Text("Hello, Development Environment!")
                }
            """.trimIndent(),
            
            "src/main/AndroidManifest.xml" to """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application
                        android:allowBackup="true"
                        android:theme="@style/Theme.Material3.DayNight">
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()
        )
    }
    
    private fun getKotlinTemplateFiles(): Map<String, String> {
        return mapOf(
            "build.gradle.kts" to """
                plugins {
                    kotlin("jvm") version "1.9.22"
                    application
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                    testImplementation("org.jetbrains.kotlin:kotlin-test")
                }
                
                application {
                    mainClass.set("MainKt")
                }
            """.trimIndent(),
            
            "src/main/kotlin/Main.kt" to """
                fun main() {
                    println("Hello from Kotlin!")
                }
            """.trimIndent()
        )
    }
    
    private fun getJavaTemplateFiles(): Map<String, String> {
        return mapOf(
            "build.gradle" to """
                apply plugin: 'java'
                apply plugin: 'application'
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    testImplementation 'junit:junit:4.13.2'
                }
                
                application {
                    mainClass = 'Main'
                }
            """.trimIndent(),
            
            "src/main/java/Main.java" to """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello from Java!");
                    }
                }
            """.trimIndent()
        )
    }
    
    /**
     * Get file system tree
     */
    fun getFileSystemTree(rootPath: String = devWorkspace.absolutePath): FileNode {
        return buildFileTree(File(rootPath))
    }
    
    private fun buildFileTree(file: File): FileNode {
        val children = if (file.isDirectory) {
            file.listFiles()
                ?.filter { !it.name.startsWith(".") }
                ?.map { buildFileTree(it) }
                ?: emptyList()
        } else {
            emptyList()
        }
        
        return FileNode(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = if (file.isFile) file.length() else 0,
            lastModified = file.lastModified(),
            children = children
        )
    }
    
    /**
     * Read file content
     */
    suspend fun readFile(filePath: String): String? = withContext(Dispatchers.IO) {
        try {
            File(filePath).readText()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Write file content
     */
    suspend fun writeFile(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            _fileSystemChanges.emit(FileSystemEvent.FileModified(filePath))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Search files in workspace
     */
    suspend fun searchFiles(query: String, directory: String = devWorkspace.absolutePath): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        
        File(directory).walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                try {
                    val content = file.readText()
                    val lines = content.lines()
                    
                    lines.forEachIndexed { lineIndex, line ->
                        if (line.contains(query, ignoreCase = true)) {
                            results.add(
                                SearchResult(
                                    filePath = file.absolutePath,
                                    fileName = file.name,
                                    lineNumber = lineIndex + 1,
                                    lineContent = line.trim(),
                                    matchStart = line.indexOf(query, ignoreCase = true),
                                    matchLength = query.length
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Skip files that can't be read
                }
            }
        
        results
    }
    
    /**
     * Get code completion suggestions
     */
    suspend fun getCodeCompletion(
        filePath: String,
        content: String,
        cursorPosition: Int
    ): List<CompletionItem> {
        return codeCompletion.getCompletions(filePath, content, cursorPosition)
    }
    
    fun cleanup() {
        scope.cancel()
        terminalSessions.clear()
    }
}

// Data classes for development environment
data class CommandResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val exitCode: Int
)

data class CommandHistoryEntry(
    val command: String,
    val workingDirectory: String,
    val result: CommandResult,
    val timestamp: Long,
    val duration: Long
)

data class ProjectTemplate(
    val name: String,
    val description: String,
    val type: ProjectType,
    val files: Map<String, String>
)

data class ProjectCreationResult(
    val success: Boolean,
    val projectPath: String,
    val error: String?
)

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val children: List<FileNode>
)

data class SearchResult(
    val filePath: String,
    val fileName: String,
    val lineNumber: Int,
    val lineContent: String,
    val matchStart: Int,
    val matchLength: Int
)

data class CompletionItem(
    val label: String,
    val detail: String,
    val insertText: String,
    val kind: CompletionKind
)

enum class ProjectType {
    ANDROID, KOTLIN, JAVA
}

enum class CompletionKind {
    METHOD, VARIABLE, CLASS, KEYWORD, SNIPPET
}

sealed class FileSystemEvent {
    data class FileCreated(val path: String) : FileSystemEvent()
    data class FileModified(val path: String) : FileSystemEvent()
    data class FileDeleted(val path: String) : FileSystemEvent()
    data class DirectoryCreated(val path: String) : FileSystemEvent()
    data class ProjectCreated(val path: String) : FileSystemEvent()
}