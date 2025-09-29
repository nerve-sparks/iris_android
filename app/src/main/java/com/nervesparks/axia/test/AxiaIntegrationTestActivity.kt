package com.nervesparks.axia.test

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nervesparks.axia.R
import com.nervesparks.axia.personality.*
import com.nervesparks.axia.roleplay.RolePlayingSystem
import com.nervesparks.axia.ui.EdgePanelService
import kotlinx.coroutines.launch

/**
 * αξία (Axia.OS) Integration Test Activity
 * 
 * Comprehensive testing interface for:
 * - Dual personality system (Nomad/Monique)
 * - Role-playing with girlfriend scenarios and kinks
 * - AI body enhancement for NSFW content
 * - Edge panel overlay service with voice chat
 * - Biometric security and panic modes
 * 
 * Company: Axia Research
 * Project: αξία (Axia.OS) - AI Girlfriend Simulation System
 */
class AxiaIntegrationTestActivity : AppCompatActivity() {

    private lateinit var personalityManager: PersonalityManager
    private lateinit var rolePlayingSystem: RolePlayingSystem
    
    // UI Components
    private lateinit var statusText: TextView
    private lateinit var personalityIndicator: TextView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var switchPersonalityButton: Button
    private lateinit var edgePanelButton: Button
    private lateinit var testRolePlayButton: Button
    private lateinit var testEnhancementButton: Button
    private lateinit var panicButton: Button
    private lateinit var responseArea: TextView
    private lateinit var authStatusText: TextView
    
    // Test data
    private val testKinks = listOf(
        "romantic",
        "playful",
        "sensual",
        "adventurous"
    )
    
    private val testScenarios = listOf(
        "We're having a cozy movie night together",
        "Planning our dream vacation",
        "Sharing intimate thoughts before bed",
        "Exchanging playful messages during work"
    )

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 456
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_integration_test)
        
        initializeComponents()
        setupUI()
        requestPermissions()
        
        updateStatus("αξία (Axia.OS) Integration Test - Ready")
    }
    
    private fun initializeComponents() {
        personalityManager = PersonalityManager(this)
        rolePlayingSystem = RolePlayingSystem(this)
        
        // Initialize UI components
        statusText = findViewById(R.id.statusText)
        personalityIndicator = findViewById(R.id.personalityIndicator)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        switchPersonalityButton = findViewById(R.id.switchPersonalityButton)
        edgePanelButton = findViewById(R.id.edgePanelButton)
        testRolePlayButton = findViewById(R.id.testRolePlayButton)
        testEnhancementButton = findViewById(R.id.testEnhancementButton)
        panicButton = findViewById(R.id.panicButton)
        responseArea = findViewById(R.id.responseArea)
        authStatusText = findViewById(R.id.authStatusText)
    }
    
    private fun setupUI() {
        // Send message button
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                processMessage(message)
                messageInput.text.clear()
            }
        }
        
        // Switch personality button
        switchPersonalityButton.setOnClickListener {
            switchPersonality()
        }
        
        // Edge panel button
        edgePanelButton.setOnClickListener {
            toggleEdgePanel()
        }
        
        // Test role-play button
        testRolePlayButton.setOnClickListener {
            testRolePlayScenarios()
        }
        
        // Test enhancement button
        testEnhancementButton.setOnClickListener {
            testAIEnhancement()
        }
        
        // Panic button
        panicButton.setOnClickListener {
            activatePanicMode()
        }
        
        // Observe personality changes
        lifecycleScope.launch {
            personalityManager.activePersonality.collect { personality ->
                updatePersonalityIndicator(personality)
            }
        }
    }
    
    private fun processMessage(message: String) {
        lifecycleScope.launch {
            try {
                updateStatus("Processing message: $message")
                
                val result = personalityManager.processMessage(message, "test_user")
                
                when (result) {
                    is MessageProcessingResult.Success -> {
                        val rolePlayIndicator = if (result.usedRolePlay) " [Role-Play Enhanced]" else ""
                        responseArea.text = "${result.response}$rolePlayIndicator"
                        updateStatus("Message processed successfully")
                    }
                    is MessageProcessingResult.Blocked -> {
                        responseArea.text = "❌ Message blocked: ${result.reason}"
                        updateStatus("Message blocked by safety filter")
                    }
                    is MessageProcessingResult.RequiresAuthentication -> {
                        responseArea.text = "🔒 Authentication required for ${result.targetPersonality}"
                        updateStatus("Switching to secure mode...")
                        // Auto-switch if possible
                        switchToPersonality(result.targetPersonality)
                    }
                    is MessageProcessingResult.Error -> {
                        responseArea.text = "❌ Error: ${result.message}"
                        updateStatus("Error processing message")
                    }
                }
            } catch (e: Exception) {
                responseArea.text = "❌ Exception: ${e.message}"
                updateStatus("Exception occurred")
            }
        }
    }
    
    private fun switchPersonality() {
        lifecycleScope.launch {
            try {
                val currentPersonality = personalityManager.activePersonality.value
                val targetPersonality = when (currentPersonality) {
                    PersonalityMode.NOMAD -> PersonalityMode.MONIQUE
                    PersonalityMode.MONIQUE -> PersonalityMode.NOMAD
                }
                
                updateStatus("Switching to $targetPersonality...")
                switchToPersonality(targetPersonality)
            } catch (e: Exception) {
                updateStatus("Error switching personality: ${e.message}")
            }
        }
    }
    
    private fun switchToPersonality(targetPersonality: PersonalityMode) {
        lifecycleScope.launch {
            try {
                val result = personalityManager.requestPersonalitySwitch(targetPersonality, this@AxiaIntegrationTestActivity)
                
                when (result) {
                    is PersonalitySwitchResult.Success -> {
                        updateStatus("Successfully switched to $targetPersonality")
                        authStatusText.text = "✅ Authenticated"
                    }
                    is PersonalitySwitchResult.AuthenticationFailed -> {
                        updateStatus("Authentication failed: ${result.reason}")
                        authStatusText.text = "❌ Auth Failed"
                    }
                    is PersonalitySwitchResult.Blocked -> {
                        updateStatus("Switch blocked: ${result.reason}")
                        authStatusText.text = "🔒 Blocked"
                    }
                }
            } catch (e: Exception) {
                updateStatus("Exception during switch: ${e.message}")
            }
        }
    }
    
    private fun testRolePlayScenarios() {
        lifecycleScope.launch {
            try {
                updateStatus("Testing role-play scenarios...")
                
                // First ensure we're in Monique mode
                if (personalityManager.activePersonality.value != PersonalityMode.MONIQUE) {
                    switchToPersonality(PersonalityMode.MONIQUE)
                    kotlinx.coroutines.delay(1000) // Wait for switch
                }
                
                // Setup test profile with kinks
                rolePlayingSystem.setupRolePlayProfile(
                    userId = "test_user",
                    scenario = "girlfriend",
                    kinks = testKinks,
                    preferences = mapOf(
                        "communication_style" to "playful and affectionate",
                        "intimacy_level" to "high",
                        "relationship_stage" to "established"
                    )
                )
                
                val responses = mutableListOf<String>()
                
                // Test various scenarios
                testScenarios.forEach { scenario ->
                    val response = rolePlayingSystem.processRolePlayMessage(scenario, "test_user")
                    responses.add("Scenario: $scenario\\nResponse: $response\\n")
                    kotlinx.coroutines.delay(500)
                }
                
                responseArea.text = responses.joinToString("\\n---\\n")
                updateStatus("Role-play test completed - ${responses.size} scenarios tested")
                
            } catch (e: Exception) {
                responseArea.text = "❌ Role-play test error: ${e.message}"
                updateStatus("Role-play test failed")
            }
        }
    }
    
    private fun testAIEnhancement() {
        lifecycleScope.launch {
            try {
                updateStatus("Testing AI body enhancement...")
                
                // Simulate processing a test image
                val testImageUri = "android.resource://${packageName}/${R.drawable.user_icon}"
                
                val result = personalityManager.processMediaMessage(
                    mediaUri = testImageUri,
                    mediaType = "image/png",
                    userId = "test_user"
                )
                
                when (result) {
                    is MediaProcessingResult.Success -> {
                        responseArea.text = "✅ Enhancement Test Complete\\n" +
                                "Original: $testImageUri\\n" +
                                "Enhanced: ${result.enhancedMediaUri}\\n" +
                                "Response: ${result.response}"
                        updateStatus("AI enhancement test successful")
                    }
                    is MediaProcessingResult.RequiresAuthentication -> {
                        responseArea.text = "🔒 Authentication required for AI enhancement"
                        updateStatus("Switching to secure mode for enhancement...")
                        switchToPersonality(PersonalityMode.MONIQUE)
                    }
                    is MediaProcessingResult.Error -> {
                        responseArea.text = "❌ Enhancement error: ${result.message}"
                        updateStatus("AI enhancement test failed")
                    }
                }
                
            } catch (e: Exception) {
                responseArea.text = "❌ Enhancement test exception: ${e.message}"
                updateStatus("AI enhancement test error")
            }
        }
    }
    
    private fun toggleEdgePanel() {
        try {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return
            }
            
            val intent = Intent(this, EdgePanelService::class.java)
            
            if (EdgePanelService.isServiceRunning) {
                stopService(intent)
                edgePanelButton.text = "Start Edge Panel"
                updateStatus("Edge panel service stopped")
            } else {
                startForegroundService(intent)
                edgePanelButton.text = "Stop Edge Panel"
                updateStatus("Edge panel service started")
            }
        } catch (e: Exception) {
            updateStatus("Edge panel error: ${e.message}")
        }
    }
    
    private fun activatePanicMode() {
        try {
            personalityManager.activatePanicLock()
            responseArea.text = "🚨 PANIC MODE ACTIVATED\\nAll sensitive data cleared\\nSwitched to safe mode"
            updateStatus("Panic mode activated - system secured")
            authStatusText.text = "🚨 PANIC"
        } catch (e: Exception) {
            updateStatus("Panic mode error: ${e.message}")
        }
    }
    
    private fun updatePersonalityIndicator(personality: PersonalityMode) {
        runOnUiThread {
            personalityIndicator.text = when (personality) {
                PersonalityMode.NOMAD -> "🤖 NOMAD (Safe Mode)"
                PersonalityMode.MONIQUE -> "💋 MONIQUE (Adult Mode)"
            }
            
            personalityIndicator.setTextColor(
                ContextCompat.getColor(
                    this,
                    when (personality) {
                        PersonalityMode.NOMAD -> android.R.color.holo_blue_dark
                        PersonalityMode.MONIQUE -> android.R.color.holo_red_dark
                    }
                )
            )
        }
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = "Status: $message"
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.USE_BIOMETRIC,
            Manifest.permission.USE_FINGERPRINT
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            updateStatus("Overlay permission granted")
            toggleEdgePanel()
        } else {
            updateStatus("Overlay permission denied")
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] != PackageManager.PERMISSION_GRANTED
                }
                
                if (deniedPermissions.isEmpty()) {
                    updateStatus("All permissions granted")
                } else {
                    updateStatus("Some permissions denied: $deniedPermissions")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop edge panel service if running
        if (EdgePanelService.isServiceRunning) {
            stopService(Intent(this, EdgePanelService::class.java))
        }
        
        // Clear any sensitive session data
        lifecycleScope.launch {
            personalityManager.endSession()
        }
    }
}