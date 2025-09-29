package com.nervesparks.iris.development

import android.content.Context
import kotlinx.coroutines.*
import java.io.File

/**
 * Build System Manager
 * Handles Gradle, Maven, and other build tools
 */

class BuildSystemManager(private val context: Context) {
    
    /**
     * Execute Gradle command
     */
    suspend fun executeGradleCommand(command: String, workingDirectory: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val gradlew = findGradleWrapper(workingDirectory)
            val actualCommand = if (gradlew != null) {
                command.replace("gradle ", "$gradlew ")
            } else {
                command
            }
            
            val processBuilder = ProcessBuilder()
                .command(actualCommand.split(" "))
                .directory(File(workingDirectory))
            
            val process = processBuilder.start()
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            CommandResult(exitCode == 0, output, error, exitCode)
        } catch (e: Exception) {
            CommandResult(false, "", "Gradle execution failed: ${e.message}", -1)
        }
    }
    
    private fun findGradleWrapper(directory: String): String? {
        val gradlew = File(directory, "gradlew")
        return if (gradlew.exists()) {
            gradlew.absolutePath
        } else {
            // Look in parent directories
            var parent = File(directory).parentFile
            while (parent != null) {
                val parentGradlew = File(parent, "gradlew")
                if (parentGradlew.exists()) {
                    return parentGradlew.absolutePath
                }
                parent = parent.parentFile
            }
            null
        }
    }
    
    /**
     * Initialize Android project
     */
    suspend fun initializeAndroidProject(projectDir: File): Boolean {
        return try {
            // Create gradle wrapper
            createGradleWrapper(projectDir)
            
            // Create settings.gradle.kts
            File(projectDir, "settings.gradle.kts").writeText("""
                pluginManagement {
                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        google()
                        mavenCentral()
                    }
                }
                
                rootProject.name = "${projectDir.name}"
                include(":app")
            """.trimIndent())
            
            // Create app directory
            File(projectDir, "app").mkdirs()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Initialize Kotlin project
     */
    suspend fun initializeKotlinProject(projectDir: File): Boolean {
        return try {
            createGradleWrapper(projectDir)
            
            File(projectDir, "settings.gradle.kts").writeText("""
                rootProject.name = "${projectDir.name}"
            """.trimIndent())
            
            // Create source directories
            File(projectDir, "src/main/kotlin").mkdirs()
            File(projectDir, "src/test/kotlin").mkdirs()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Initialize Java project
     */
    suspend fun initializeJavaProject(projectDir: File): Boolean {
        return try {
            createGradleWrapper(projectDir)
            
            File(projectDir, "settings.gradle").writeText("""
                rootProject.name = '${projectDir.name}'
            """.trimIndent())
            
            // Create source directories
            File(projectDir, "src/main/java").mkdirs()
            File(projectDir, "src/test/java").mkdirs()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createGradleWrapper(projectDir: File) {
        val gradleDir = File(projectDir, "gradle/wrapper")
        gradleDir.mkdirs()
        
        // Create gradle-wrapper.properties
        File(gradleDir, "gradle-wrapper.properties").writeText("""
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
            networkTimeout=10000
            validateDistributionUrl=true
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
        """.trimIndent())
        
        // Create gradlew script
        File(projectDir, "gradlew").writeText("""
            #!/bin/sh
            
            # Gradle wrapper script
            DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
            APP_NAME="Gradle"
            APP_BASE_NAME=${'$'}{0##*/}
            
            # Use Android's Java
            if [ -z "${'$'}JAVA_HOME" ] ; then
                JAVA_HOME="/system"
            fi
            
            JAVA="${'$'}JAVA_HOME/bin/java"
            GRADLE_APP_DIR=${'$'}{0%/*}
            
            exec "${'$'}JAVA" ${'$'}DEFAULT_JVM_OPTS -jar "${'$'}GRADLE_APP_DIR/gradle/wrapper/gradle-wrapper.jar" "${'$'}@"
        """.trimIndent())
        
        // Make gradlew executable
        File(projectDir, "gradlew").setExecutable(true)
    }
    
    /**
     * Get build status
     */
    suspend fun getBuildStatus(projectDir: String): BuildStatus {
        return try {
            val buildDir = File(projectDir, "build")
            val lastBuild = if (buildDir.exists()) {
                buildDir.listFiles()?.maxByOrNull { it.lastModified() }?.lastModified() ?: 0
            } else {
                0
            }
            
            val sourceFiles = File(projectDir, "src").walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .toList()
            
            val lastModified = sourceFiles.maxOfOrNull { it.lastModified() } ?: 0
            
            val status = when {
                lastBuild == 0L -> BuildStatus.NEVER_BUILT
                lastModified > lastBuild -> BuildStatus.OUT_OF_DATE
                else -> BuildStatus.UP_TO_DATE
            }
            
            BuildStatus(status.status, lastBuild, sourceFiles.size)
        } catch (e: Exception) {
            BuildStatus(BuildStatusType.ERROR, 0, 0)
        }
    }
    
    /**
     * Run tests
     */
    suspend fun runTests(projectDir: String): TestResult {
        val result = executeGradleCommand("gradle test", projectDir)
        
        return TestResult(
            success = result.success,
            totalTests = parseTestCount(result.output, "tests"),
            passedTests = parseTestCount(result.output, "passed"),
            failedTests = parseTestCount(result.output, "failed"),
            skippedTests = parseTestCount(result.output, "skipped"),
            output = result.output,
            error = result.error
        )
    }
    
    private fun parseTestCount(output: String, type: String): Int {
        val regex = Regex("(\\d+) $type")
        return regex.find(output)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}

data class BuildStatus(
    val status: BuildStatusType,
    val lastBuildTime: Long,
    val sourceFileCount: Int
)

enum class BuildStatusType {
    UP_TO_DATE, OUT_OF_DATE, NEVER_BUILT, ERROR
}

data class TestResult(
    val success: Boolean,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val skippedTests: Int,
    val output: String,
    val error: String
)