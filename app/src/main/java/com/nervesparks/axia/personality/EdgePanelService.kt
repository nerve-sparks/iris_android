package com.nervesparks.axia.personality

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.provider.Settings
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.os.Bundle
import java.util.*

/**
 * Edge Panel System for Screen Overlay
 * 
 * Features:
 * - Floating edge panel like Samsung Edge Panel
 * - Voice chat with screen sharing capability
 * - Always-on-top overlay functionality  
 * - Seamless app switching without exit
 * - Voice guidance while using other apps
 * - Gesture controls for quick access
 */
class EdgePanelService : Service(), TextToSpeech.OnInitListener {
    
    private lateinit var windowManager: WindowManager
    private var edgePanel: View? = null
    private var chatOverlay: View? = null
    private var isExpanded = false
    private var isDragging = false
    
    // Voice components
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isSpeaking = false
    
    // Coroutine scope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Chat state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    
    // Role playing integration
    private lateinit var rolePlayingSystem: RolePlayingSystem
    private lateinit var personalitySystem: DualPersonalitySystem
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            textToSpeech = TextToSpeech(this, this)
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            
            // Initialize systems
            rolePlayingSystem = RolePlayingSystem(this)
            personalitySystem = DualPersonalitySystem(this, null) // Hardware optimizer can be null for overlay
            
            setupSpeechRecognizer()
            createEdgePanel()
            
            Log.d("EdgePanelService", "Edge panel service created")
            
        } catch (e: Exception) {
            Log.e("EdgePanelService", "Failed to create edge panel service", e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            Log.d("EdgePanelService", "Text-to-speech initialized")
        }
    }
    
    /**
     * Create the floating edge panel
     */
    private fun createEdgePanel() {
        try {
            // Check for overlay permission
            if (!Settings.canDrawOverlays(this)) {
                Log.w("EdgePanelService", "Overlay permission not granted")
                requestOverlayPermission()
                return
            }
            
            edgePanel = createEdgePanelView()
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.END
            params.x = 0
            params.y = 200
            
            windowManager.addView(edgePanel, params)
            
            Log.d("EdgePanelService", "Edge panel created and added to window")
            
        } catch (e: Exception) {
            Log.e("EdgePanelService", "Failed to create edge panel", e)
        }
    }
    
    /**
     * Create the edge panel view
     */
    private fun createEdgePanelView(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@EdgePanelService, android.R.color.black)
            setPadding(16, 16, 16, 16)
            alpha = 0.8f
        }
        
        // Edge handle (always visible)
        val edgeHandle = createEdgeHandle()
        panel.addView(edgeHandle)
        
        // Expandable chat area (hidden by default)
        val chatArea = createChatArea()
        chatArea.visibility = View.GONE
        panel.addView(chatArea)
        
        // Set up touch listeners
        setupPanelTouchListener(panel, edgeHandle, chatArea)
        
        return panel
    }
    
    /**
     * Create the edge handle (thin bar on screen edge)
     */
    private fun createEdgeHandle(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(8, 100)
            background = ContextCompat.getDrawable(this@EdgePanelService, android.R.color.white)
            alpha = 0.6f
        }
    }
    
    /**
     * Create the expandable chat area
     */
    private fun createChatArea(): LinearLayout {
        val chatArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(400, 600)
            background = ContextCompat.getDrawable(this@EdgePanelService, android.R.color.black)
            setPadding(16, 16, 16, 16)
        }
        
        // Chat messages area
        val messagesArea = createMessagesArea()
        chatArea.addView(messagesArea)
        
        // Input area
        val inputArea = createInputArea()
        chatArea.addView(inputArea)
        
        // Voice controls
        val voiceControls = createVoiceControls()
        chatArea.addView(voiceControls)
        
        return chatArea
    }
    
    /**
     * Create messages display area
     */
    private fun createMessagesArea(): ScrollView {
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // Weight to take remaining space
            )
        }
        
        val messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        scrollView.addView(messagesContainer)
        
        // Observe chat messages
        serviceScope.launch {
            chatMessages.collect { messages ->
                updateMessagesDisplay(messagesContainer, messages)
            }
        }
        
        return scrollView
    }
    
    /**
     * Create input area for typing
     */
    private fun createInputArea(): LinearLayout {
        val inputArea = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 0)
        }
        
        val editText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "Type message..."
            setTextColor(ContextCompat.getColor(this@EdgePanelService, android.R.color.white))
            setHintTextColor(ContextCompat.getColor(this@EdgePanelService, android.R.color.darker_gray))
        }
        
        val sendButton = Button(this).apply {
            text = "Send"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val message = editText.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                    editText.text.clear()
                }
            }
        }
        
        inputArea.addView(editText)
        inputArea.addView(sendButton)
        
        return inputArea
    }
    
    /**
     * Create voice control buttons
     */
    private fun createVoiceControls(): LinearLayout {
        val voiceControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 0)
        }
        
        val listenButton = Button(this).apply {
            text = "🎤"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                toggleVoiceListening()
            }
        }
        
        val speakButton = Button(this).apply {
            text = "🔊"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                toggleTextToSpeech()
            }
        }
        
        val minimizeButton = Button(this).apply {
            text = "−"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                collapsePanel()
            }
        }
        
        voiceControls.addView(listenButton)
        voiceControls.addView(speakButton)
        voiceControls.addView(minimizeButton)
        
        return voiceControls
    }
    
    /**
     * Set up touch listeners for panel interaction
     */
    private fun setupPanelTouchListener(panel: LinearLayout, handle: View, chatArea: LinearLayout) {
        var initialX = 0f
        var initialY = 0f
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        panel.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = (panel.layoutParams as WindowManager.LayoutParams).x.toFloat()
                    initialY = (panel.layoutParams as WindowManager.LayoutParams).y.toFloat()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    if (kotlin.math.abs(deltaX) > 10 || kotlin.math.abs(deltaY) > 10) {
                        isDragging = true
                        val params = panel.layoutParams as WindowManager.LayoutParams
                        params.x = (initialX - deltaX).toInt()
                        params.y = (initialY + deltaY).toInt()
                        windowManager.updateViewLayout(panel, params)
                    }
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Single tap - toggle panel expansion
                        togglePanelExpansion(chatArea)
                    }
                    isDragging = false
                    true
                }
                
                else -> false
            }
        }
    }
    
    /**
     * Toggle panel expansion/collapse
     */
    private fun togglePanelExpansion(chatArea: LinearLayout) {
        isExpanded = !isExpanded
        chatArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
        
        Log.d("EdgePanelService", "Panel ${if (isExpanded) "expanded" else "collapsed"}")
    }
    
    /**
     * Collapse the panel
     */
    private fun collapsePanel() {
        isExpanded = false
        edgePanel?.findViewById<LinearLayout>(0)?.visibility = View.GONE
    }
    
    /**
     * Send message and get response
     */
    private fun sendMessage(message: String) {
        serviceScope.launch {
            try {
                // Add user message
                val userMessage = ChatMessage(
                    text = message,
                    isFromUser = true,
                    timestamp = System.currentTimeMillis()
                )
                
                val currentMessages = _chatMessages.value.toMutableList()
                currentMessages.add(userMessage)
                _chatMessages.value = currentMessages
                
                // Get response from role playing system
                val response = rolePlayingSystem.processRolePlayMessage(message)
                
                when (response) {
                    is RolePlayResponse.Success -> {
                        val botMessage = ChatMessage(
                            text = response.response,
                            isFromUser = false,
                            timestamp = System.currentTimeMillis()
                        )
                        
                        val updatedMessages = _chatMessages.value.toMutableList()
                        updatedMessages.add(botMessage)
                        _chatMessages.value = updatedMessages
                        
                        // Speak the response if TTS is enabled
                        if (isSpeaking && textToSpeech != null) {
                            textToSpeech?.speak(
                                response.response,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "message_${System.currentTimeMillis()}"
                            )
                        }
                    }
                    
                    is RolePlayResponse.Error -> {
                        Log.e("EdgePanelService", "Role play response error: ${response.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("EdgePanelService", "Failed to send message", e)
            }
        }
    }
    
    /**
     * Update messages display
     */
    private fun updateMessagesDisplay(container: LinearLayout, messages: List<ChatMessage>) {
        try {
            container.removeAllViews()
            
            messages.forEach { message ->
                val messageView = createMessageView(message)
                container.addView(messageView)
            }
            
            // Auto-scroll to bottom
            (container.parent as? ScrollView)?.post {
                (container.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
            }
            
        } catch (e: Exception) {
            Log.e("EdgePanelService", "Failed to update messages display", e)
        }
    }
    
    /**
     * Create individual message view
     */
    private fun createMessageView(message: ChatMessage): View {
        val messageLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
        
        val textView = TextView(this).apply {
            text = message.text
            setTextColor(
                ContextCompat.getColor(
                    this@EdgePanelService,
                    if (message.isFromUser) android.R.color.holo_blue_light
                    else android.R.color.white
                )
            )
            textSize = 14f
            setPadding(16, 12, 16, 12)
            background = ContextCompat.getDrawable(
                this@EdgePanelService,
                if (message.isFromUser) android.R.color.darker_gray
                else android.R.color.black
            )
        }
        
        messageLayout.addView(textView)
        
        return messageLayout
    }
    
    /**
     * Setup speech recognizer
     */
    private fun setupSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("EdgePanelService", "Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d("EdgePanelService", "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Voice level indicator could be updated here
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Voice data buffer
            }
            
            override fun onEndOfSpeech() {
                Log.d("EdgePanelService", "End of speech")
                isListening = false
            }
            
            override fun onError(error: Int) {
                Log.e("EdgePanelService", "Speech recognition error: $error")
                isListening = false
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.d("EdgePanelService", "Speech recognized: $spokenText")
                    sendMessage(spokenText)
                }
                isListening = false
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Partial results can be shown for real-time feedback
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Speech events
            }
        })
    }
    
    /**
     * Toggle voice listening
     */
    private fun toggleVoiceListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d("EdgePanelService", "Stopped voice listening")
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d("EdgePanelService", "Started voice listening")
        }
    }
    
    /**
     * Toggle text-to-speech
     */
    private fun toggleTextToSpeech() {
        isSpeaking = !isSpeaking
        
        if (!isSpeaking) {
            textToSpeech?.stop()
        }
        
        Log.d("EdgePanelService", "Text-to-speech ${if (isSpeaking) "enabled" else "disabled"}")
    }
    
    /**
     * Request overlay permission
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        try {
            edgePanel?.let { windowManager.removeView(it) }
            chatOverlay?.let { windowManager.removeView(it) }
            
            textToSpeech?.shutdown()
            speechRecognizer?.destroy()
            
            rolePlayingSystem.cleanup()
            personalitySystem.cleanup()
            serviceScope.cancel()
            
            Log.d("EdgePanelService", "Edge panel service destroyed")
            
        } catch (e: Exception) {
            Log.e("EdgePanelService", "Error during service destruction", e)
        }
    }
    
    /**
     * Start the edge panel service
     */
    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, EdgePanelService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, EdgePanelService::class.java)
            context.stopService(intent)
        }
    }
}

// Data class for chat messages
data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val hasMedia: Boolean = false,
    val mediaType: String? = null
)