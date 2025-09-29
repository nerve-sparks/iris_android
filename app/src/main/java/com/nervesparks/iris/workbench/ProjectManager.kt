package com.nervesparks.iris.workbench

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.*
import java.util.*

/**
 * Project Manager for the Workbench
 * Handles creation, management, and building of development projects
 */

data class WorkbenchProject(
    val id: String,
    val name: String,
    val path: String,
    val type: ProjectType,
    val description: String,
    val createdAt: Long,
    val lastModified: Long,
    val gitRepository: String? = null,
    val buildConfiguration: BuildConfiguration,
    val dependencies: List<ProjectDependency> = emptyList(),
    val isActive: Boolean = false
)

enum class ProjectType(val displayName: String, val templatePath: String) {
    ANDROID_APP("Android App", "templates/android_app"),
    ANDROID_LIBRARY("Android Library", "templates/android_library"),
    KOTLIN_MULTIPLATFORM("Kotlin Multiplatform", "templates/kmp"),
    COMPOSE_MULTIPLATFORM("Compose Multiplatform", "templates/compose_mp"),
    SPRING_BOOT("Spring Boot API", "templates/spring_boot"),
    REACT_NATIVE("React Native", "templates/react_native"),
    FLUTTER("Flutter", "templates/flutter"),
    NODEJS_API("Node.js API", "templates/nodejs"),
    PYTHON_SCRIPT("Python Script", "templates/python"),
    WEB_APP("Web Application", "templates/webapp"),
    CLI_TOOL("CLI Tool", "templates/cli"),
    CUSTOM("Custom Project", "templates/custom")
}

data class BuildConfiguration(
    val buildTool: BuildTool,
    val targetSdk: Int? = null,
    val minSdk: Int? = null,
    val javaVersion: String? = null,
    val kotlinVersion: String? = null,
    val gradleVersion: String? = null,
    val buildVariants: List<String> = listOf("debug", "release"),
    val customTasks: List<String> = emptyList()
)

enum class BuildTool(val displayName: String, val configFile: String) {
    GRADLE("Gradle", "build.gradle.kts"),
    MAVEN("Maven", "pom.xml"),
    NPM("NPM", "package.json"),
    YARN("Yarn", "package.json"),
    CARGO("Cargo", "Cargo.toml"),
    PIP("Pip", "requirements.txt"),
    CUSTOM("Custom", "Makefile")
}

data class ProjectDependency(
    val name: String,
    val version: String,
    val type: DependencyType,
    val scope: String = "implementation"
)

enum class DependencyType {
    LIBRARY, PLUGIN, DEV_DEPENDENCY, TEST_DEPENDENCY
}

data class ProjectTemplate(
    val type: ProjectType,
    val name: String,
    val description: String,
    val files: List<TemplateFile>
)

data class TemplateFile(
    val path: String,
    val content: String,
    val isExecutable: Boolean = false
)

data class BuildResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val duration: Long,
    val artifacts: List<String> = emptyList()
)

class ProjectManager(private val context: Context) {
    companion object {
        private const val TAG = "ProjectManager"
        private const val PROJECTS_DIR = "projects"
        private const val TEMPLATES_DIR = "templates"
        private const val CONFIG_FILE = "workbench_projects.json"
    }
    
    private val projectsDir = File(context.filesDir, PROJECTS_DIR)
    private val templatesDir = File(context.filesDir, TEMPLATES_DIR)
    private val configFile = File(context.filesDir, CONFIG_FILE)
    
    private val _projects = MutableStateFlow<List<WorkbenchProject>>(emptyList())
    val projects: StateFlow<List<WorkbenchProject>> = _projects.asStateFlow()
    
    private val _activeProject = MutableStateFlow<WorkbenchProject?>(null)
    val activeProject: StateFlow<WorkbenchProject?> = _activeProject.asStateFlow()
    
    private val _buildStatus = MutableStateFlow<BuildResult?>(null)
    val buildStatus: StateFlow<BuildResult?> = _buildStatus.asStateFlow()
    
    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()
    
    init {
        initializeProjectManager()
    }
    
    private fun initializeProjectManager() {
        try {
            projectsDir.mkdirs()
            templatesDir.mkdirs()
            
            setupProjectTemplates()
            loadProjects()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize project manager", e)
        }
    }
    
    private fun setupProjectTemplates() {
        createAndroidAppTemplate()
        createKotlinLibraryTemplate()
        createSpringBootTemplate()
        createPythonScriptTemplate()
        createWebAppTemplate()
    }
    
    /**
     * Create a new project from template
     */
    suspend fun createProject(
        name: String,
        type: ProjectType,
        description: String,
        packageName: String? = null,
        gitInit: Boolean = true
    ): WorkbenchProject? {
        return withContext(Dispatchers.IO) {
            try {
                val projectId = UUID.randomUUID().toString()
                val projectPath = File(projectsDir, name).absolutePath
                val projectDir = File(projectPath)
                
                if (projectDir.exists()) {
                    Log.w(TAG, "Project directory already exists: $projectPath")
                    return@withContext null
                }
                
                projectDir.mkdirs()
                
                // Create project from template
                val template = getProjectTemplate(type, name, packageName)
                createProjectFromTemplate(projectDir, template)
                
                // Initialize Git repository if requested
                if (gitInit) {
                    initializeGitRepository(projectDir)
                }
                
                val buildConfig = createDefaultBuildConfiguration(type)
                
                val project = WorkbenchProject(
                    id = projectId,
                    name = name,
                    path = projectPath,
                    type = type,
                    description = description,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    buildConfiguration = buildConfig
                )
                
                // Save project
                val currentProjects = _projects.value.toMutableList()
                currentProjects.add(project)
                _projects.value = currentProjects
                saveProjects()
                
                Log.i(TAG, "Created project: $name")
                project
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create project: $name", e)
                null
            }
        }
    }
    
    /**
     * Load an existing project
     */
    suspend fun loadProject(projectPath: String): WorkbenchProject? {
        return withContext(Dispatchers.IO) {
            try {
                val projectDir = File(projectPath)
                if (!projectDir.exists() || !projectDir.isDirectory) {
                    return@withContext null
                }
                
                val projectType = detectProjectType(projectDir)
                val buildConfig = loadBuildConfiguration(projectDir, projectType)
                
                val project = WorkbenchProject(
                    id = UUID.randomUUID().toString(),
                    name = projectDir.name,
                    path = projectPath,
                    type = projectType,
                    description = "Imported project",
                    createdAt = projectDir.lastModified(),
                    lastModified = projectDir.lastModified(),
                    buildConfiguration = buildConfig
                )
                
                val currentProjects = _projects.value.toMutableList()
                if (currentProjects.none { it.path == projectPath }) {
                    currentProjects.add(project)
                    _projects.value = currentProjects
                    saveProjects()
                }
                
                project
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load project from: $projectPath", e)
                null
            }
        }
    }
    
    /**
     * Build the active project
     */
    suspend fun buildProject(
        project: WorkbenchProject,
        buildVariant: String = "debug"
    ): BuildResult {
        return withContext(Dispatchers.IO) {
            _isBuilding.value = true
            val startTime = System.currentTimeMillis()
            
            try {
                Log.i(TAG, "Building project: ${project.name} ($buildVariant)")
                
                val result = when (project.buildConfiguration.buildTool) {
                    BuildTool.GRADLE -> buildGradleProject(project, buildVariant)
                    BuildTool.MAVEN -> buildMavenProject(project, buildVariant)
                    BuildTool.NPM -> buildNpmProject(project, buildVariant)
                    else -> BuildResult(
                        success = false,
                        output = "",
                        error = "Build tool not supported: ${project.buildConfiguration.buildTool}",
                        duration = 0L
                    )
                }
                
                val duration = System.currentTimeMillis() - startTime
                val finalResult = result.copy(duration = duration)
                
                _buildStatus.value = finalResult
                finalResult
                
            } catch (e: Exception) {
                Log.e(TAG, "Build failed for project: ${project.name}", e)
                val errorResult = BuildResult(
                    success = false,
                    output = "",
                    error = e.message ?: "Unknown build error",
                    duration = System.currentTimeMillis() - startTime
                )
                _buildStatus.value = errorResult
                errorResult
                
            } finally {
                _isBuilding.value = false
            }
        }
    }
    
    private fun buildGradleProject(project: WorkbenchProject, buildVariant: String): BuildResult {
        val projectDir = File(project.path)
        val gradlew = File(projectDir, if (isWindows()) "gradlew.bat" else "gradlew")
        
        val command = if (gradlew.exists()) {
            listOf(gradlew.absolutePath, "assemble${buildVariant.capitalize()}")
        } else {
            listOf("gradle", "assemble${buildVariant.capitalize()}")
        }
        
        return executeCommand(command, projectDir)
    }
    
    private fun buildMavenProject(project: WorkbenchProject, buildVariant: String): BuildResult {
        val projectDir = File(project.path)
        val command = listOf("mvn", "clean", "compile", "package")
        return executeCommand(command, projectDir)
    }
    
    private fun buildNpmProject(project: WorkbenchProject, buildVariant: String): BuildResult {
        val projectDir = File(project.path)
        val command = listOf("npm", "run", "build")
        return executeCommand(command, projectDir)
    }
    
    private fun executeCommand(command: List<String>, workingDir: File): BuildResult {
        return try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(workingDir)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            BuildResult(
                success = exitCode == 0,
                output = output,
                error = if (exitCode != 0) "Build failed with exit code: $exitCode" else "",
                duration = 0L // Will be set by caller
            )
            
        } catch (e: Exception) {
            BuildResult(
                success = false,
                output = "",
                error = e.message ?: "Command execution failed",
                duration = 0L
            )
        }
    }
    
    /**
     * Set active project
     */
    fun setActiveProject(project: WorkbenchProject) {
        _activeProject.value = project
        
        // Update projects list to mark active project
        val updatedProjects = _projects.value.map { p ->
            p.copy(isActive = p.id == project.id)
        }
        _projects.value = updatedProjects
    }
    
    /**
     * Delete a project
     */
    suspend fun deleteProject(project: WorkbenchProject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val projectDir = File(project.path)
                if (projectDir.exists()) {
                    projectDir.deleteRecursively()
                }
                
                val updatedProjects = _projects.value.filter { it.id != project.id }
                _projects.value = updatedProjects
                
                if (_activeProject.value?.id == project.id) {
                    _activeProject.value = null
                }
                
                saveProjects()
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete project: ${project.name}", e)
                false
            }
        }
    }
    
    private fun createProjectFromTemplate(projectDir: File, template: ProjectTemplate) {
        template.files.forEach { templateFile ->
            val file = File(projectDir, templateFile.path)
            file.parentFile?.mkdirs()
            file.writeText(templateFile.content)
            
            if (templateFile.isExecutable) {
                file.setExecutable(true)
            }
        }
    }
    
    private fun getProjectTemplate(
        type: ProjectType,
        projectName: String,
        packageName: String?
    ): ProjectTemplate {
        return when (type) {
            ProjectType.ANDROID_APP -> createAndroidAppTemplate(projectName, packageName)
            ProjectType.KOTLIN_MULTIPLATFORM -> createKmpTemplate(projectName, packageName)
            ProjectType.SPRING_BOOT -> createSpringBootTemplate(projectName, packageName)
            else -> createGenericTemplate(type, projectName)
        }
    }
    
    private fun createAndroidAppTemplate(
        projectName: String = "MyAndroidApp",
        packageName: String? = null
    ): ProjectTemplate {
        val pkg = packageName ?: "com.example.${projectName.lowercase()}"
        val packagePath = pkg.replace('.', '/')
        
        return ProjectTemplate(
            type = ProjectType.ANDROID_APP,
            name = projectName,
            description = "Android application project",
            files = listOf(
                TemplateFile("build.gradle.kts", createAndroidBuildGradle(pkg)),
                TemplateFile("app/build.gradle.kts", createAndroidAppBuildGradle(pkg)),
                TemplateFile("app/src/main/AndroidManifest.xml", createAndroidManifest(pkg)),
                TemplateFile("app/src/main/java/$packagePath/MainActivity.kt", createMainActivity(pkg)),
                TemplateFile("app/src/main/res/values/strings.xml", createStringsXml(projectName)),
                TemplateFile("app/src/main/res/values/colors.xml", createColorsXml()),
                TemplateFile("settings.gradle.kts", "rootProject.name = \"$projectName\"\ninclude(\":app\")"),
                TemplateFile("gradle.properties", createGradleProperties()),
                TemplateFile("gradlew", createGradlewScript(), true),
                TemplateFile("README.md", "# $projectName\n\nAndroid application built with the Workbench.")
            )
        )
    }
    
    private fun createAndroidBuildGradle(packageName: String): String {
        return """
plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "1.9.10" apply false
}
        """.trimIndent()
    }
    
    private fun createAndroidAppBuildGradle(packageName: String): String {
        return """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "$packageName"
    compileSdk = 34

    defaultConfig {
        applicationId = "$packageName"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:2.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview:2.6.0")
    implementation("androidx.compose.material3:material3:1.1.2")
}
        """.trimIndent()
    }
    
    private fun createAndroidManifest(packageName: String): String {
        return """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DynamicColors.DayNight"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.DynamicColors.DayNight">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
        """.trimIndent()
    }
    
    private fun createMainActivity(packageName: String): String {
        return """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Workbench")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello ${'$'}name! This app was created with the Workbench.",
        modifier = modifier.padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MaterialTheme {
        Greeting("Workbench")
    }
}
        """.trimIndent()
    }
    
    private fun createStringsXml(appName: String): String {
        return """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$appName</string>
</resources>
        """.trimIndent()
    }
    
    private fun createColorsXml(): String {
        return """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
        """.trimIndent()
    }
    
    private fun createGradleProperties(): String {
        return """
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
        """.trimIndent()
    }
    
    private fun createGradlewScript(): String {
        return """#!/bin/sh
# Gradle wrapper script - created by Workbench
./gradlew "$@"
        """.trimIndent()
    }
    
    private fun createKmpTemplate(projectName: String, packageName: String?): ProjectTemplate {
        // Implementation for KMP template
        return ProjectTemplate(ProjectType.KOTLIN_MULTIPLATFORM, projectName, "KMP project", emptyList())
    }
    
    private fun createSpringBootTemplate(projectName: String, packageName: String?): ProjectTemplate {
        // Implementation for Spring Boot template
        return ProjectTemplate(ProjectType.SPRING_BOOT, projectName, "Spring Boot API", emptyList())
    }
    
    private fun createGenericTemplate(type: ProjectType, projectName: String): ProjectTemplate {
        return ProjectTemplate(type, projectName, "Generic project", emptyList())
    }
    
    private fun initializeGitRepository(projectDir: File) {
        try {
            val process = ProcessBuilder("git", "init")
                .directory(projectDir)
                .start()
            process.waitFor()
            
            // Create .gitignore
            val gitignore = File(projectDir, ".gitignore")
            gitignore.writeText("""
build/
.gradle/
*.iml
.idea/
local.properties
*.log
        """.trimIndent())
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize git repository", e)
        }
    }
    
    private fun detectProjectType(projectDir: File): ProjectType {
        return when {
            File(projectDir, "build.gradle.kts").exists() || File(projectDir, "build.gradle").exists() -> {
                if (File(projectDir, "app/build.gradle.kts").exists()) ProjectType.ANDROID_APP
                else ProjectType.KOTLIN_MULTIPLATFORM
            }
            File(projectDir, "pom.xml").exists() -> ProjectType.SPRING_BOOT
            File(projectDir, "package.json").exists() -> ProjectType.NODEJS_API
            File(projectDir, "requirements.txt").exists() -> ProjectType.PYTHON_SCRIPT
            else -> ProjectType.CUSTOM
        }
    }
    
    private fun loadBuildConfiguration(projectDir: File, type: ProjectType): BuildConfiguration {
        return createDefaultBuildConfiguration(type)
    }
    
    private fun createDefaultBuildConfiguration(type: ProjectType): BuildConfiguration {
        return when (type) {
            ProjectType.ANDROID_APP, ProjectType.ANDROID_LIBRARY -> BuildConfiguration(
                buildTool = BuildTool.GRADLE,
                targetSdk = 34,
                minSdk = 24,
                javaVersion = "1.8",
                kotlinVersion = "1.9.10",
                gradleVersion = "8.1.4"
            )
            ProjectType.SPRING_BOOT -> BuildConfiguration(
                buildTool = BuildTool.GRADLE,
                javaVersion = "11"
            )
            ProjectType.NODEJS_API -> BuildConfiguration(
                buildTool = BuildTool.NPM
            )
            else -> BuildConfiguration(
                buildTool = BuildTool.CUSTOM
            )
        }
    }
    
    private fun loadProjects() {
        try {
            if (configFile.exists()) {
                val jsonContent = configFile.readText()
                val jsonArray = JSONArray(jsonContent)
                
                val projects = mutableListOf<WorkbenchProject>()
                for (i in 0 until jsonArray.length()) {
                    val jsonProject = jsonArray.getJSONObject(i)
                    val project = parseProject(jsonProject)
                    if (project != null) {
                        projects.add(project)
                    }
                }
                
                _projects.value = projects
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load projects", e)
        }
    }
    
    private fun saveProjects() {
        try {
            val jsonArray = JSONArray()
            _projects.value.forEach { project ->
                val jsonProject = serializeProject(project)
                jsonArray.put(jsonProject)
            }
            
            configFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save projects", e)
        }
    }
    
    private fun parseProject(json: JSONObject): WorkbenchProject? {
        return try {
            val buildConfig = BuildConfiguration(
                buildTool = BuildTool.valueOf(json.optString("buildTool", "GRADLE")),
                targetSdk = json.optInt("targetSdk", 0).takeIf { it > 0 },
                minSdk = json.optInt("minSdk", 0).takeIf { it > 0 },
                javaVersion = json.optString("javaVersion").takeIf { it.isNotEmpty() },
                kotlinVersion = json.optString("kotlinVersion").takeIf { it.isNotEmpty() },
                gradleVersion = json.optString("gradleVersion").takeIf { it.isNotEmpty() }
            )
            
            WorkbenchProject(
                id = json.getString("id"),
                name = json.getString("name"),
                path = json.getString("path"),
                type = ProjectType.valueOf(json.getString("type")),
                description = json.getString("description"),
                createdAt = json.getLong("createdAt"),
                lastModified = json.getLong("lastModified"),
                gitRepository = json.optString("gitRepository").takeIf { it.isNotEmpty() },
                buildConfiguration = buildConfig,
                isActive = json.optBoolean("isActive", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse project JSON", e)
            null
        }
    }
    
    private fun serializeProject(project: WorkbenchProject): JSONObject {
        return JSONObject().apply {
            put("id", project.id)
            put("name", project.name)
            put("path", project.path)
            put("type", project.type.name)
            put("description", project.description)
            put("createdAt", project.createdAt)
            put("lastModified", project.lastModified)
            put("gitRepository", project.gitRepository ?: "")
            put("buildTool", project.buildConfiguration.buildTool.name)
            put("targetSdk", project.buildConfiguration.targetSdk ?: 0)
            put("minSdk", project.buildConfiguration.minSdk ?: 0)
            put("javaVersion", project.buildConfiguration.javaVersion ?: "")
            put("kotlinVersion", project.buildConfiguration.kotlinVersion ?: "")
            put("gradleVersion", project.buildConfiguration.gradleVersion ?: "")
            put("isActive", project.isActive)
        }
    }
    
    private fun isWindows(): Boolean {
        return System.getProperty("os.name").lowercase().contains("windows")
    }
}

@Composable
fun rememberProjectManager(context: Context): ProjectManager {
    return remember {
        ProjectManager(context)
    }
}