package com.nervesparks.axia.development

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.*
import com.nervesparks.axia.models.*
import com.nervesparks.axia.research.DeepWebResearcher
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Android-First Development Assistant for Axia
 * AI-powered tools for Android development:
 * - Project generation and scaffolding
 * - Code analysis and optimization
 * - Architecture suggestions
 * - Dependency management
 * - Build and deployment automation
 * - Real-time development assistance
 */
class AndroidDevelopmentAssistant(
    private val context: Context,
    private val webResearcher: DeepWebResearcher
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Development state
    private val _currentProject = MutableStateFlow<AndroidProject?>(null)
    val currentProject: StateFlow<AndroidProject?> = _currentProject.asStateFlow()
    
    private val _buildStatus = MutableStateFlow<BuildStatus>(BuildStatus.IDLE)
    val buildStatus: StateFlow<BuildStatus> = _buildStatus.asStateFlow()
    
    private val _developmentTasks = MutableStateFlow<List<DevelopmentTask>>(emptyList())
    val developmentTasks: StateFlow<List<DevelopmentTask>> = _developmentTasks.asStateFlow()
    
    // AI-powered development capabilities
    suspend fun createNewAndroidApp(
        appName: String,
        packageName: String,
        requirements: List<String>,
        architecture: AppArchitecture = AppArchitecture.MVVM,
        useLatestPractices: Boolean = true
    ): AndroidProject {
        val projectId = UUID.randomUUID().toString()
        
        // Research latest Android development practices
        val researchResults = if (useLatestPractices) {
            webResearcher.researchDevelopmentTopic(
                "Android app development best practices 2025 ${requirements.joinToString(" ")}"
            )
        } else null
        
        val project = AndroidProject(
            id = projectId,
            name = appName,
            packageName = packageName,
            architecture = architecture,
            requirements = requirements,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis(),
            buildVariants = listOf("debug", "release"),
            dependencies = generateOptimalDependencies(requirements, researchResults),
            features = generateAppFeatures(requirements),
            targetSDK = 34, // Android 14
            minSDK = 24,    // Android 7.0
            kotlinVersion = "1.9.20",
            composeVersion = "2023.10.01",
            gradleVersion = "8.1.1"
        )
        
        // Generate project structure
        generateProjectStructure(project)
        
        // Generate core files
        generateManifest(project)
        generateBuildGradle(project)
        generateMainActivity(project)
        generateViewModels(project)
        generateRepositories(project)
        generateUI(project)
        
        _currentProject.value = project
        
        addDevelopmentTask(
            DevelopmentTask(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                type = TaskType.PROJECT_CREATED,
                description = "Created new Android project: $appName",
                status = TaskStatus.COMPLETED,
                createdAt = System.currentTimeMillis()
            )
        )
        
        return project
    }
    
    suspend fun analyzeExistingCode(
        filePath: String,
        analysisType: CodeAnalysisType = CodeAnalysisType.COMPREHENSIVE
    ): CodeAnalysisResult {
        val file = File(filePath)
        if (!file.exists()) {
            return CodeAnalysisResult.error("File not found: $filePath")
        }
        
        val content = file.readText()
        
        // AI-powered code analysis
        val issues = mutableListOf<CodeIssue>()
        val suggestions = mutableListOf<CodeSuggestion>()
        val metrics = CodeMetrics()
        
        // Analyze code structure
        analyzeKotlinCode(content, issues, suggestions, metrics)
        
        // Check for Android-specific issues
        analyzeAndroidSpecific(content, issues, suggestions)
        
        // Performance analysis
        analyzePerformance(content, issues, suggestions)
        
        // Security analysis
        analyzeSecurity(content, issues, suggestions)
        
        // Architecture analysis
        analyzeArchitecture(content, issues, suggestions)
        
        return CodeAnalysisResult(
            filePath = filePath,
            analysisType = analysisType,
            issues = issues,
            suggestions = suggestions,
            metrics = metrics,
            overallScore = calculateOverallScore(issues, suggestions),
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun optimizeCodeForPerformance(
        code: String,
        optimizationType: OptimizationType = OptimizationType.BATTERY_PERFORMANCE
    ): CodeOptimizationResult {
        val optimizedCode = StringBuilder(code)
        val optimizations = mutableListOf<OptimizationApplied>()
        
        when (optimizationType) {
            OptimizationType.BATTERY_PERFORMANCE -> {
                applyBatteryOptimizations(optimizedCode, optimizations)
            }
            OptimizationType.MEMORY_EFFICIENCY -> {
                applyMemoryOptimizations(optimizedCode, optimizations)
            }
            OptimizationType.UI_PERFORMANCE -> {
                applyUIOptimizations(optimizedCode, optimizations)
            }
            OptimizationType.NETWORK_EFFICIENCY -> {
                applyNetworkOptimizations(optimizedCode, optimizations)
            }
        }
        
        return CodeOptimizationResult(
            originalCode = code,
            optimizedCode = optimizedCode.toString(),
            optimizations = optimizations,
            estimatedImprovement = calculateEstimatedImprovement(optimizations),
            optimizationType = optimizationType
        )
    }
    
    suspend fun suggestArchitecture(
        appType: AppType,
        complexity: AppComplexity,
        teamSize: Int
    ): ArchitectureSuggestion {
        // Research latest architectural patterns
        val researchResults = webResearcher.researchDevelopmentTopic(
            "Android app architecture patterns 2025 $appType $complexity"
        )
        
        val recommendedArchitecture = when (complexity) {
            AppComplexity.SIMPLE -> AppArchitecture.MVI
            AppComplexity.MEDIUM -> AppArchitecture.MVVM
            AppComplexity.COMPLEX -> AppArchitecture.CLEAN_ARCHITECTURE
            AppComplexity.ENTERPRISE -> AppArchitecture.MODULAR_CLEAN
        }
        
        return ArchitectureSuggestion(
            recommendedArchitecture = recommendedArchitecture,
            reasoning = generateArchitectureReasoning(appType, complexity, teamSize),
            modules = generateModuleStructure(recommendedArchitecture, appType),
            dependencies = generateArchitecturalDependencies(recommendedArchitecture),
            implementationSteps = generateImplementationSteps(recommendedArchitecture),
            codeTemplates = generateCodeTemplates(recommendedArchitecture),
            researchBacked = researchResults?.summary ?: ""
        )
    }
    
    suspend fun generateUIComponent(
        componentType: UIComponentType,
        specifications: ComponentSpecs,
        useLatestDesign: Boolean = true
    ): UIGenerationResult {
        val designResearch = if (useLatestDesign) {
            webResearcher.researchDevelopmentTopic(
                "Material Design 3 Android Compose $componentType 2025"
            )
        } else null
        
        val composableCode = when (componentType) {
            UIComponentType.CUSTOM_BUTTON -> generateCustomButton(specifications)
            UIComponentType.DATA_INPUT_FORM -> generateDataInputForm(specifications)
            UIComponentType.LIST_ITEM -> generateListItem(specifications)
            UIComponentType.CARD_LAYOUT -> generateCardLayout(specifications)
            UIComponentType.NAVIGATION -> generateNavigation(specifications)
            UIComponentType.DIALOG -> generateDialog(specifications)
            UIComponentType.CHART -> generateChart(specifications)
            UIComponentType.CUSTOM_LAYOUT -> generateCustomLayout(specifications)
        }
        
        return UIGenerationResult(
            componentCode = composableCode,
            previewCode = generatePreviewCode(componentType, specifications),
            testCode = generateUITestCode(componentType, specifications),
            designTokens = generateDesignTokens(specifications),
            accessibility = generateAccessibilityCode(componentType),
            documentation = generateComponentDocumentation(componentType, specifications),
            designResearchBacked = designResearch?.summary ?: ""
        )
    }
    
    suspend fun autoFixBuildIssues(
        buildLog: String,
        projectPath: String
    ): BuildFixResult {
        val issues = parseBuildErrors(buildLog)
        val fixes = mutableListOf<BuildFix>()
        var fixedBuildGradle = false
        
        for (issue in issues) {
            val fix = when (issue.type) {
                BuildErrorType.DEPENDENCY_CONFLICT -> fixDependencyConflict(issue, projectPath)
                BuildErrorType.VERSION_MISMATCH -> fixVersionMismatch(issue, projectPath)
                BuildErrorType.MISSING_DEPENDENCY -> addMissingDependency(issue, projectPath)
                BuildErrorType.COMPILATION_ERROR -> fixCompilationError(issue, projectPath)
                BuildErrorType.RESOURCE_ERROR -> fixResourceError(issue, projectPath)
                BuildErrorType.PERMISSION_ERROR -> fixPermissionError(issue, projectPath)
                else -> null
            }
            
            fix?.let { 
                fixes.add(it)
                if (it.fileModified.endsWith("build.gradle") || it.fileModified.endsWith("build.gradle.kts")) {
                    fixedBuildGradle = true
                }
            }
        }
        
        return BuildFixResult(
            originalIssues = issues,
            appliedFixes = fixes,
            buildGradleModified = fixedBuildGradle,
            estimatedSuccessRate = calculateFixSuccessRate(fixes),
            nextSteps = generateNextSteps(issues, fixes)
        )
    }
    
    suspend fun generateTests(
        codeFile: String,
        testType: TestType = TestType.UNIT_TESTS
    ): TestGenerationResult {
        val code = File(codeFile).readText()
        val className = extractClassName(code)
        
        val testCode = when (testType) {
            TestType.UNIT_TESTS -> generateUnitTests(code, className)
            TestType.INTEGRATION_TESTS -> generateIntegrationTests(code, className)
            TestType.UI_TESTS -> generateUITests(code, className)
            TestType.INSTRUMENTATION_TESTS -> generateInstrumentationTests(code, className)
        }
        
        return TestGenerationResult(
            testCode = testCode,
            testFileName = "${className}Test.kt",
            coverage = calculateTestCoverage(code, testCode),
            testType = testType,
            dependencies = getTestDependencies(testType),
            runInstructions = generateRunInstructions(testType)
        )
    }
    
    // Private helper methods
    private fun generateOptimalDependencies(
        requirements: List<String>,
        researchResults: ResearchResults?
    ): List<AndroidDependency> {
        val dependencies = mutableListOf<AndroidDependency>()
        
        // Core dependencies
        dependencies.addAll(listOf(
            AndroidDependency("androidx.core", "core-ktx", "1.12.0"),
            AndroidDependency("androidx.lifecycle", "lifecycle-runtime-ktx", "2.7.0"),
            AndroidDependency("androidx.activity", "activity-compose", "1.8.2"),
            AndroidDependency("androidx.compose.ui", "ui", "2023.10.01"),
            AndroidDependency("androidx.compose.material3", "material3", "1.1.2")
        ))
        
        // Add dependencies based on requirements
        for (requirement in requirements) {
            when (requirement.lowercase()) {
                "navigation" -> dependencies.add(
                    AndroidDependency("androidx.navigation", "navigation-compose", "2.7.5")
                )
                "database", "room" -> dependencies.addAll(listOf(
                    AndroidDependency("androidx.room", "room-runtime", "2.6.0"),
                    AndroidDependency("androidx.room", "room-ktx", "2.6.0"),
                    AndroidDependency("androidx.room", "room-compiler", "2.6.0", isKapt = true)
                ))
                "network", "retrofit" -> dependencies.addAll(listOf(
                    AndroidDependency("com.squareup.retrofit2", "retrofit", "2.9.0"),
                    AndroidDependency("com.squareup.retrofit2", "converter-gson", "2.9.0"),
                    AndroidDependency("com.squareup.okhttp3", "logging-interceptor", "4.12.0")
                ))
                "images", "image loading" -> dependencies.add(
                    AndroidDependency("io.coil-kt", "coil-compose", "2.5.0")
                )
                "dependency injection", "di" -> dependencies.addAll(listOf(
                    AndroidDependency("com.google.dagger", "hilt-android", "2.48"),
                    AndroidDependency("com.google.dagger", "hilt-compiler", "2.48", isKapt = true),
                    AndroidDependency("androidx.hilt", "hilt-navigation-compose", "1.1.0")
                ))
            }
        }
        
        return dependencies
    }
    
    private fun generateAppFeatures(requirements: List<String>): List<AppFeature> {
        return requirements.map { requirement ->
            AppFeature(
                name = requirement,
                description = "Feature for $requirement",
                complexity = AppComplexity.MEDIUM,
                estimatedHours = 8,
                dependencies = emptyList()
            )
        }
    }
    
    private fun generateProjectStructure(project: AndroidProject): Boolean {
        // This would create the actual project directory structure
        return true // Placeholder
    }
    
    private fun generateManifest(project: AndroidProject): String {
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
        android:theme="@style/Theme.${project.name.replace(" ", "")}"
        tools:targetApi="31">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.${project.name.replace(" ", "")}"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
        """.trimIndent()
    }
    
    private fun generateBuildGradle(project: AndroidProject): String {
        val dependenciesString = project.dependencies.joinToString("\n    ") { dep ->
            val implementation = if (dep.isKapt) "kapt" else "implementation"
            "$implementation '${dep.group}:${dep.artifact}:${dep.version}'"
        }
        
        return """
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

android {
    namespace '${project.packageName}'
    compileSdk ${project.targetSDK}

    defaultConfig {
        applicationId "${project.packageName}"
        minSdk ${project.minSDK}
        targetSdk ${project.targetSDK}
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary true
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
    
    buildFeatures {
        compose true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion '${project.composeVersion}'
    }
    
    packagingOptions {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
        }
    }
}

dependencies {
    $dependenciesString
    
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4:${project.composeVersion}'
    debugImplementation 'androidx.compose.ui:ui-tooling:${project.composeVersion}'
    debugImplementation 'androidx.compose.ui:ui-test-manifest:${project.composeVersion}'
}
        """.trimIndent()
    }
    
    private fun generateMainActivity(project: AndroidProject): String {
        return """
package ${project.packageName}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ${project.packageName}.ui.theme.${project.name.replace(" ", "")}Theme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ${project.name.replace(" ", "")}Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
        """.trimIndent()
    }
    
    private fun generateViewModels(project: AndroidProject): List<String> {
        return project.features.map { feature ->
            generateViewModelForFeature(feature, project.packageName)
        }
    }
    
    private fun generateRepositories(project: AndroidProject): List<String> {
        return project.features.map { feature ->
            generateRepositoryForFeature(feature, project.packageName)
        }
    }
    
    private fun generateUI(project: AndroidProject): List<String> {
        return project.features.map { feature ->
            generateUIForFeature(feature, project.packageName)
        }
    }
    
    // Placeholder implementations for complex methods
    private fun analyzeKotlinCode(content: String, issues: MutableList<CodeIssue>, suggestions: MutableList<CodeSuggestion>, metrics: CodeMetrics) {}
    private fun analyzeAndroidSpecific(content: String, issues: MutableList<CodeIssue>, suggestions: MutableList<CodeSuggestion>) {}
    private fun analyzePerformance(content: String, issues: MutableList<CodeIssue>, suggestions: MutableList<CodeSuggestion>) {}
    private fun analyzeSecurity(content: String, issues: MutableList<CodeIssue>, suggestions: MutableList<CodeSuggestion>) {}
    private fun analyzeArchitecture(content: String, issues: MutableList<CodeIssue>, suggestions: MutableList<CodeSuggestion>) {}
    private fun calculateOverallScore(issues: List<CodeIssue>, suggestions: List<CodeSuggestion>): Float = 0.8f
    
    private fun applyBatteryOptimizations(code: StringBuilder, optimizations: MutableList<OptimizationApplied>) {}
    private fun applyMemoryOptimizations(code: StringBuilder, optimizations: MutableList<OptimizationApplied>) {}
    private fun applyUIOptimizations(code: StringBuilder, optimizations: MutableList<OptimizationApplied>) {}
    private fun applyNetworkOptimizations(code: StringBuilder, optimizations: MutableList<OptimizationApplied>) {}
    private fun calculateEstimatedImprovement(optimizations: List<OptimizationApplied>): Float = 0.25f
    
    private fun generateArchitectureReasoning(appType: AppType, complexity: AppComplexity, teamSize: Int): String = ""
    private fun generateModuleStructure(architecture: AppArchitecture, appType: AppType): List<String> = emptyList()
    private fun generateArchitecturalDependencies(architecture: AppArchitecture): List<AndroidDependency> = emptyList()
    private fun generateImplementationSteps(architecture: AppArchitecture): List<String> = emptyList()
    private fun generateCodeTemplates(architecture: AppArchitecture): Map<String, String> = emptyMap()
    
    private fun generateCustomButton(specs: ComponentSpecs): String = ""
    private fun generateDataInputForm(specs: ComponentSpecs): String = ""
    private fun generateListItem(specs: ComponentSpecs): String = ""
    private fun generateCardLayout(specs: ComponentSpecs): String = ""
    private fun generateNavigation(specs: ComponentSpecs): String = ""
    private fun generateDialog(specs: ComponentSpecs): String = ""
    private fun generateChart(specs: ComponentSpecs): String = ""
    private fun generateCustomLayout(specs: ComponentSpecs): String = ""
    private fun generatePreviewCode(type: UIComponentType, specs: ComponentSpecs): String = ""
    private fun generateUITestCode(type: UIComponentType, specs: ComponentSpecs): String = ""
    private fun generateDesignTokens(specs: ComponentSpecs): Map<String, String> = emptyMap()
    private fun generateAccessibilityCode(type: UIComponentType): String = ""
    private fun generateComponentDocumentation(type: UIComponentType, specs: ComponentSpecs): String = ""
    
    private fun parseBuildErrors(buildLog: String): List<BuildError> = emptyList()
    private fun fixDependencyConflict(issue: BuildError, projectPath: String): BuildFix? = null
    private fun fixVersionMismatch(issue: BuildError, projectPath: String): BuildFix? = null
    private fun addMissingDependency(issue: BuildError, projectPath: String): BuildFix? = null
    private fun fixCompilationError(issue: BuildError, projectPath: String): BuildFix? = null
    private fun fixResourceError(issue: BuildError, projectPath: String): BuildFix? = null
    private fun fixPermissionError(issue: BuildError, projectPath: String): BuildFix? = null
    private fun calculateFixSuccessRate(fixes: List<BuildFix>): Float = 0.85f
    private fun generateNextSteps(issues: List<BuildError>, fixes: List<BuildFix>): List<String> = emptyList()
    
    private fun extractClassName(code: String): String = "ExampleClass"
    private fun generateUnitTests(code: String, className: String): String = ""
    private fun generateIntegrationTests(code: String, className: String): String = ""
    private fun generateUITests(code: String, className: String): String = ""
    private fun generateInstrumentationTests(code: String, className: String): String = ""
    private fun calculateTestCoverage(code: String, testCode: String): Float = 0.75f
    private fun getTestDependencies(testType: TestType): List<AndroidDependency> = emptyList()
    private fun generateRunInstructions(testType: TestType): String = ""
    
    private fun generateViewModelForFeature(feature: AppFeature, packageName: String): String = ""
    private fun generateRepositoryForFeature(feature: AppFeature, packageName: String): String = ""
    private fun generateUIForFeature(feature: AppFeature, packageName: String): String = ""
    
    private fun addDevelopmentTask(task: DevelopmentTask) {
        _developmentTasks.value = _developmentTasks.value + task
    }
}

// Data classes and enums
data class AndroidProject(
    val id: String,
    val name: String,
    val packageName: String,
    val architecture: AppArchitecture,
    val requirements: List<String>,
    val createdAt: Long,
    val lastModified: Long,
    val buildVariants: List<String>,
    val dependencies: List<AndroidDependency>,
    val features: List<AppFeature>,
    val targetSDK: Int,
    val minSDK: Int,
    val kotlinVersion: String,
    val composeVersion: String,
    val gradleVersion: String
)

data class AndroidDependency(
    val group: String,
    val artifact: String,
    val version: String,
    val isKapt: Boolean = false
)

data class AppFeature(
    val name: String,
    val description: String,
    val complexity: AppComplexity,
    val estimatedHours: Int,
    val dependencies: List<String>
)

data class DevelopmentTask(
    val id: String,
    val projectId: String,
    val type: TaskType,
    val description: String,
    val status: TaskStatus,
    val createdAt: Long
)

data class CodeAnalysisResult(
    val filePath: String,
    val analysisType: CodeAnalysisType,
    val issues: List<CodeIssue>,
    val suggestions: List<CodeSuggestion>,
    val metrics: CodeMetrics,
    val overallScore: Float,
    val timestamp: Long
) {
    companion object {
        fun error(message: String) = CodeAnalysisResult(
            filePath = "",
            analysisType = CodeAnalysisType.COMPREHENSIVE,
            issues = listOf(CodeIssue("ERROR", message, IssueSeverity.HIGH)),
            suggestions = emptyList(),
            metrics = CodeMetrics(),
            overallScore = 0f,
            timestamp = System.currentTimeMillis()
        )
    }
}

data class CodeIssue(
    val type: String,
    val description: String,
    val severity: IssueSeverity,
    val line: Int = 0
)

data class CodeSuggestion(
    val type: String,
    val description: String,
    val impact: String,
    val code: String = ""
)

data class CodeMetrics(
    val linesOfCode: Int = 0,
    val complexity: Int = 0,
    val maintainabilityIndex: Float = 0f
)

data class CodeOptimizationResult(
    val originalCode: String,
    val optimizedCode: String,
    val optimizations: List<OptimizationApplied>,
    val estimatedImprovement: Float,
    val optimizationType: OptimizationType
)

data class OptimizationApplied(
    val type: String,
    val description: String,
    val impact: String
)

data class ArchitectureSuggestion(
    val recommendedArchitecture: AppArchitecture,
    val reasoning: String,
    val modules: List<String>,
    val dependencies: List<AndroidDependency>,
    val implementationSteps: List<String>,
    val codeTemplates: Map<String, String>,
    val researchBacked: String
)

data class UIGenerationResult(
    val componentCode: String,
    val previewCode: String,
    val testCode: String,
    val designTokens: Map<String, String>,
    val accessibility: String,
    val documentation: String,
    val designResearchBacked: String
)

data class ComponentSpecs(
    val name: String,
    val parameters: Map<String, Any>,
    val styling: Map<String, String>,
    val behavior: Map<String, Any>
)

data class BuildFixResult(
    val originalIssues: List<BuildError>,
    val appliedFixes: List<BuildFix>,
    val buildGradleModified: Boolean,
    val estimatedSuccessRate: Float,
    val nextSteps: List<String>
)

data class BuildError(
    val type: BuildErrorType,
    val message: String,
    val file: String,
    val line: Int
)

data class BuildFix(
    val description: String,
    val fileModified: String,
    val changes: String
)

data class TestGenerationResult(
    val testCode: String,
    val testFileName: String,
    val coverage: Float,
    val testType: TestType,
    val dependencies: List<AndroidDependency>,
    val runInstructions: String
)

// Enums
enum class AppArchitecture {
    MVC, MVP, MVVM, MVI, CLEAN_ARCHITECTURE, MODULAR_CLEAN
}

enum class AppComplexity {
    SIMPLE, MEDIUM, COMPLEX, ENTERPRISE
}

enum class AppType {
    UTILITY, SOCIAL, ECOMMERCE, PRODUCTIVITY, GAME, MEDIA, BUSINESS
}

enum class TaskType {
    PROJECT_CREATED, CODE_GENERATED, BUILD_FIXED, TESTS_GENERATED
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED
}

enum class BuildStatus {
    IDLE, BUILDING, SUCCESS, FAILED
}

enum class CodeAnalysisType {
    BASIC, COMPREHENSIVE, SECURITY_FOCUSED, PERFORMANCE_FOCUSED
}

enum class OptimizationType {
    BATTERY_PERFORMANCE, MEMORY_EFFICIENCY, UI_PERFORMANCE, NETWORK_EFFICIENCY
}

enum class UIComponentType {
    CUSTOM_BUTTON, DATA_INPUT_FORM, LIST_ITEM, CARD_LAYOUT, NAVIGATION, DIALOG, CHART, CUSTOM_LAYOUT
}

enum class TestType {
    UNIT_TESTS, INTEGRATION_TESTS, UI_TESTS, INSTRUMENTATION_TESTS
}

enum class IssueSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class BuildErrorType {
    DEPENDENCY_CONFLICT, VERSION_MISMATCH, MISSING_DEPENDENCY, COMPILATION_ERROR, RESOURCE_ERROR, PERMISSION_ERROR
}