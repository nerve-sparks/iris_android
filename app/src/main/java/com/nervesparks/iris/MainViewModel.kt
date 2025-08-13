package com.nervesparks.iris

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import com.nervesparks.iris.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.Locale
import java.util.UUID

import android.app.Application
import android.content.Context
import com.nervesparks.iris.data.repository.ChatRepository
import com.nervesparks.iris.data.DocumentRepository
import com.nervesparks.iris.data.db.Chat
import com.nervesparks.iris.data.db.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.nervesparks.iris.data.WebSearchService
import com.nervesparks.iris.data.AndroidSearchService
import com.nervesparks.iris.data.WebSearchService.SearchResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@HiltViewModel
class MainViewModel @Inject constructor(
    private val llamaAndroid: LLamaAndroid,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val chatRepository: ChatRepository,
    private val modelRepository: com.nervesparks.iris.data.repository.ModelRepository,
    private val huggingFaceApiService: com.nervesparks.iris.data.HuggingFaceApiService,
    private val documentRepository: DocumentRepository,
    private val webSearchService: WebSearchService,
    private val androidSearchService: AndroidSearchService,
    application: Application
) : AndroidViewModel(application) {

    val chats = chatRepository.observeChats()

    fun renameChat(chat: Chat, title: String) {
        viewModelScope.launch {
            chatRepository.renameChat(chat, title)
        }
    }

    fun deleteChat(chat: Chat) {
        viewModelScope.launch {
            chatRepository.deleteChat(chat)
        }
    }

    fun indexDocument(text: String) {
        viewModelScope.launch {
            val embedding = embedText(text)
            documentRepository.addDocument(text, embedding.toList())
        }
    }

    suspend fun embedText(text: String): FloatArray =
        withContext(Dispatchers.Default) {
            try {
                llamaAndroid.getEmbeddings(text) ?: floatArrayOf()
            } catch (e: Exception) {
                Log.e(tag, "getEmbeddings failed", e)
                floatArrayOf()
            }
        }

    private var currentChat: com.nervesparks.iris.data.db.Chat? = null
    companion object {
//        @JvmStatic
//        private val NanosPerSecond = 1_000_000_000.0
    }

    // Load existing chat
    fun loadChat(chatId: Long) {
        if (chatId <= 0) return
        viewModelScope.launch {
            val chat = chatRepository.observeChat(chatId).first()
            val msgs = chatRepository.loadMessages(chatId)
            messages = msgs.sortedBy { it.index }.map { m ->
                mapOf("role" to m.role, "content" to m.content)
            }
            currentChat = chat
            first = false
        }
    }

    // Thinking token settings - moved to top to ensure initialization
    var showThinkingTokens by mutableStateOf(true)
    var thinkingTokenStyle by mutableStateOf("COLLAPSIBLE") // COLLAPSIBLE, ALWAYS_VISIBLE, HIDDEN
    // Ensure native stripping respects UI + model capability
    private fun applyThinkStripGate() {
        try {
            LLamaAndroid.instance().let {
                val enableStrip = !(supportsReasoning && showThinkingTokens)
                it.set_strip_think(enableStrip)
            }
        } catch (_: Exception) {}
    }

    // Flag indicating if the currently loaded model supports reasoning tokens
    var supportsReasoning by mutableStateOf(false)

    var downloadableModels by mutableStateOf<List<Downloadable>>(emptyList())
        private set

    private val _defaultModelName = mutableStateOf("")
    val defaultModelName: State<String> = _defaultModelName

    // Model configuration variables - must be initialized BEFORE init {}
    private var _modelTemperature = mutableStateOf(0.7f)
    var modelTemperature: Float
        get() = _modelTemperature.value
        set(value) { _modelTemperature.value = value }

    private var _modelTopP = mutableStateOf(0.9f)
    var modelTopP: Float
        get() = _modelTopP.value
        set(value) { _modelTopP.value = value }

    private var _modelTopK = mutableStateOf(40)
    var modelTopK: Int
        get() = _modelTopK.value
        set(value) { _modelTopK.value = value }

    private var _modelMaxTokens = mutableStateOf(2048)
    var modelMaxTokens: Int
        get() = _modelMaxTokens.value
        set(value) { _modelMaxTokens.value = value }

    private var _modelContextLength = mutableStateOf(32768) // Increased for Qwen3 support
    var modelContextLength: Int
        get() = _modelContextLength.value
        set(value) { _modelContextLength.value = value }

    private var _modelSystemPrompt = mutableStateOf("You are a helpful AI assistant.")
    var modelSystemPrompt: String
        get() = _modelSystemPrompt.value
        set(value) { _modelSystemPrompt.value = value }

    private var _modelChatFormat = mutableStateOf("CHATML")
    var modelChatFormat: String
        get() = _modelChatFormat.value
        set(value) { _modelChatFormat.value = value }

    private var _modelThreadCount = mutableStateOf(4)
    var modelThreadCount: Int
        get() = _modelThreadCount.value
        set(value) { _modelThreadCount.value = value }

    // GPU layers to offload (-1 = Auto)
    private var _modelGpuLayers = mutableStateOf(-1)
    var modelGpuLayers: Int
        get() = _modelGpuLayers.value
        set(value) { _modelGpuLayers.value = value }

    init {
        loadDefaultModelName()
        loadModelSettings() // Re-enabled model settings loading
        loadThinkingTokenSettings()
        
        // Hugging Face token must be provided by user via Settings

        // Detect hardware capabilities at startup - moved to after library loads
        // detectHardwareCapabilities()



        viewModelScope.launch {
            try {
                val models = modelRepository.refreshAvailableModels()
                allModels = models
                Log.d(tag, "Loaded ${models.size} models from repository (defaults + API)")
            } catch (e: Exception) {
                Log.e(tag, "Error refreshing available models from API; repository will provide curated defaults", e)
                allModels = emptyList()
            }
        }
    }
    private fun loadDefaultModelName(){
        try {
            _defaultModelName.value = userPreferencesRepository.getDefaultModelName()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading default model name, using empty string", e)
            _defaultModelName.value = ""
        }
    }

    fun setDefaultModelName(modelName: String){
        userPreferencesRepository.setDefaultModelName(modelName)
        _defaultModelName.value = modelName
    }

    lateinit var selectedModel: String
    private val tag: String? = this::class.simpleName

    var messages by mutableStateOf(

            listOf<Map<String, String>>(),
        )
        private set
    var showModal by mutableStateOf(true)
    var showDownloadInfoModal by mutableStateOf(false)
    var showModelSelection by mutableStateOf(false)
    var selectedModelForSwitch by mutableStateOf("")
    var user_thread by mutableStateOf(0f)
    var topP by mutableStateOf(0.9f)
    var topK by mutableStateOf(40)
    var temp by mutableStateOf(0.7f)

    var allModels by mutableStateOf<List<Map<String, String>>>(emptyList())

    private var first by mutableStateOf(
        true
    )
    var userSpecifiedThreads by mutableIntStateOf(2)
    var message by mutableStateOf("")
        private set

    var userGivenModel by mutableStateOf("")
    var SearchedName by mutableStateOf("")

    private var textToSpeech:TextToSpeech? = null

    var textForTextToSpeech = ""
    var stateForTextToSpeech by mutableStateOf(true)
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening by mutableStateOf(false)
        private set
    var voiceError by mutableStateOf<String?>(null)

    var eot_str = ""

    // Hardware acceleration state variables - Initialize with CPU defaults
    var availableBackends by mutableStateOf("CPU")
    var currentBackend by mutableStateOf("CPU")
    var optimalBackend by mutableStateOf("CPU")
    var gpuInfo by mutableStateOf("CPU Only - Use Settings to detect hardware")
    var isAdrenoGpu by mutableStateOf(false)
    var showBackendSelection by mutableStateOf(false)
    var backendError by mutableStateOf<String?>(null)

    // Web search services are injected via DI

    fun performWebSearch(query: String, summarize: Boolean = true) {
        // Add the search query as a user message
        viewModelScope.launch {
            pruneForNewTokens(llamaAndroid.countTokens("Search the web for: $query"))
        }
        addMessage("user", "Search the web for: $query")
        
        viewModelScope.launch {
            try {
                // Update search state
                isSearching = true
                currentSearchQuery = query
                searchProgress = "🔍 Initializing web search..."

                // Show search in progress
                addMessage("assistant", "🔍 Searching the web for \"$query\"...")

                searchProgress = "🌐 Querying search engine..."

                // Try API-based search first
                val searchResponse = webSearchService.searchWeb(query)

                searchProgress = "📊 Processing search results..."

                if (searchResponse.success && searchResponse.results != null && searchResponse.results.isNotEmpty()) {
                    searchProgress = "📝 Formatting results for display..."

                    // Format and display search results
                    val formattedResults = webSearchService.formatSearchResults(searchResponse.results, query)
                    addMessage("assistant", formattedResults)

                    // If summarize is true, ask the model to summarize the results
                    if (summarize && searchResponse.results.isNotEmpty()) {
                        searchProgress = "🤖 Generating AI summary..."

                        val summaryPrompt = """
                            Based on the search results above, provide a concise summary of the key information about "$query".
                            Focus on the most important facts and recent developments.
                        """.trimIndent()

                        try {
                            viewModelScope.launch {
                                pruneForNewTokens(llamaAndroid.countTokens(summaryPrompt))
                            }
                            addMessage("user", summaryPrompt)
                            processWebSearch(summaryPrompt)
                            searchProgress = "Search complete"
                        } catch (e: Exception) {
                            Log.e(tag, "Error generating summary", e)
                            addMessage(
                                "assistant",
                                "❌ Summary failed: ${e.message}\n\nPlease try again later."
                            )
                            searchProgress = "Summary failed"
                        }
                    } else {
                        searchProgress = "Search complete"
                    }
                } else {
                    // If API search fails, try Android browser search
                    searchProgress = "📱 Launching browser search..."

                    val androidSearchResponse = androidSearchService.launchBrowserSearch(query)

                    if (androidSearchResponse.success) {
                        val formattedResults = androidSearchService.formatSearchResults(androidSearchResponse.results ?: emptyList(), query)
                        addMessage("assistant", formattedResults)
                    } else {
                        // Handle search error
                        val errorMessage = searchResponse.error ?: "Unknown search error"
                        addMessage("assistant", "❌ Search failed: $errorMessage\n\nI've tried API search and browser search. Please try rephrasing your search query.")
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Error performing web search", e)
                addMessage("assistant", "❌ Search error: ${e.message}\n\nPlease try again later.")
                searchProgress = "Search error"
            } finally {
                // Reset search state
                isSearching = false
                currentSearchQuery = ""
            }
        }
    }
    
    private fun processWebSearch(prompt: String) {
        viewModelScope.launch {
            try {
                startGeneration()
                
                var workingMessages = messages.toMutableList()
                val reserve = 256

                // Trim history until it fits within the context window
                var finalPrompt = ""
                while (true) {
                    finalPrompt = if (template.isNotBlank()) {
                        val jinjava = com.hubspot.jinjava.Jinjava()
                        val context = mapOf("messages" to workingMessages)
                        jinjava.render(template, context)
                    } else {
                        com.nervesparks.iris.llm.TemplateRegistry.render(
                            modelChatFormat,
                            workingMessages,
                            modelSystemPrompt,
                            includeThinkingTags = supportsReasoning
                        )
                    }
                    val promptTokens = llamaAndroid.countTokens(finalPrompt)
                    if (promptTokens <= modelContextLength - reserve || workingMessages.size <= 1) {
                        break
                    }
                    val removeIdx = workingMessages.indexOfFirst { it["role"] != "system" }
                    if (removeIdx >= 0) {
                        workingMessages = workingMessages.drop(removeIdx + 1).toMutableList()
                    } else {
                        break
                    }
                }

                if (workingMessages.size != messages.size) {
                    messages = workingMessages
                    addMessage(
                        "system",
                        "⚠️ Earlier messages were removed to stay within the model's context limit."
                    )
                }

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Log.e(tag, "processWebSearch() failed", it)
                        addMessage("error", it.message ?: "")
                    }
                    .collect {
                        generatedTokens++
                        updateTokenCount(generatedTokens)
                        contextLimit = llamaAndroid.countTokens(finalPrompt) + generatedTokens

                        if (getIsMarked()) {
                            addMessage("codeBlock", it)
                        } else {
                            try {
                                val json = org.json.JSONObject(it)
                                if (json.has("tool")) {
                                    val tool = json.getString("tool")
                                    val args = json.getJSONObject("args")
                                    val argsMap = mutableMapOf<String, Any>()
                                    args.keys().forEach { key ->
                                        argsMap[key] = args.get(key)
                                    }
                                    handleToolCall(com.nervesparks.iris.data.ToolCall(tool, argsMap))
                                } else {
                                    val cleanedResponse = cleanThinkingResponse(it)
                                    addMessage("assistant", cleanedResponse)
                                }
                            } catch (e: org.json.JSONException) {
                                val cleanedResponse = cleanThinkingResponse(it)
                                addMessage("assistant", cleanedResponse)
                            }
                        }
                    }
                    .also {
                        endGeneration()
                        persistChat()
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error in processWebSearch", e)
                addMessage("error", "Failed to process web search: ${e.message}")
                endGeneration()
            }
        }
    }

    fun startVoiceRecognition() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voiceError = "Microphone permission not granted"
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceError = "Speech recognition not available"
            return
        }
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        voiceError = null
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = data?.firstOrNull() ?: ""
                        updateMessage(text)
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        voiceError = "Error: $error"
                    }

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    // Quick action and attachment handlers
    var lastQuickAction by mutableStateOf<String?>(null)
        private set
    var lastAttachmentAction by mutableStateOf<String?>(null)
        private set

    fun onLatestNews() {
        lastQuickAction = "latest_news"
        performWebSearch("latest news")
    }

    fun onCreateImages() {
        lastQuickAction = "create_images"
        performWebSearch("create images")
    }

    fun onCartoonStyle() {
        lastQuickAction = "cartoon_style"
        performWebSearch("cartoon style art")
    }

    fun onCameraAttachment() {
        lastAttachmentAction = "camera"
    }

    fun onPhotosAttachment() {
        lastAttachmentAction = "photos"
    }

    fun onFilesAttachment() {
        lastAttachmentAction = "files"
    }

    fun summarizeDocument(text: String) {
        viewModelScope.launch {
            val prompt = "Summarize the following text:\n\n$text"
            pruneForNewTokens(llamaAndroid.countTokens(prompt))
            addMessage("user", prompt)
            send()
        }
    }

    fun handleToolCall(toolCall: com.nervesparks.iris.data.ToolCall) {
        Log.d(tag, "Handling tool call: $toolCall")
        
        viewModelScope.launch {
            try {
                when (toolCall.name) {
                    "web_search", "brave_search" -> {
                        val query = toolCall.args["query"] as? String
                        if (query != null) {
                            Log.d(tag, "Executing web search for: $query")
                            
                            // Show tool execution in progress
                            addMessage("assistant", "🔍 Executing web search for \"$query\"...")
                            
                            // Perform the search
                            val searchResponse = webSearchService.searchWeb(query)
                            
                            if (searchResponse.success && searchResponse.results != null) {
                                // Format and display results
                                val formattedResults = webSearchService.formatSearchResults(searchResponse.results, query)
                                addMessage("assistant", formattedResults)
                            } else {
                                val errorMessage = searchResponse.error ?: "Unknown search error"
                                addMessage("assistant", "❌ Web search failed: $errorMessage")
                            }
                        } else {
                            addMessage("assistant", "❌ Invalid search query provided")
                        }
                    }
                    "wolfram_alpha" -> {
                        val query = toolCall.args["query"] as? String
                        if (query != null) {
                            addMessage("assistant", "🧮 Wolfram Alpha calculation for \"$query\"\n\nThis feature is not yet implemented. Please try a different approach.")
                        } else {
                            addMessage("assistant", "❌ Invalid Wolfram Alpha query")
                        }
                    }
                    "python", "code_interpreter" -> {
                        val code = toolCall.args["code"] as? String
                        if (code != null) {
                            addMessage("assistant", "🐍 Python code execution:\n\n```python\n$code\n```\n\nThis feature is not yet implemented. Please try a different approach.")
                        } else {
                            addMessage("assistant", "❌ Invalid Python code")
                        }
                    }
                    else -> {
                        addMessage("assistant", "❌ Unknown tool: ${toolCall.name}\n\nAvailable tools: web_search, brave_search, wolfram_alpha, python")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error handling tool call", e)
                addMessage("assistant", "❌ Tool execution error: ${e.message}")
            }
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer
                    .process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        if (extractedText.isNotBlank()) {
                            // Index the document for retrieval and summarize for the chat
                            indexDocument(extractedText)
                            summarizeDocument(extractedText)
                        } else {
                            addMessage("assistant", "❌ No text found in image")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "OCR failed", e)
                        addMessage("assistant", "❌ OCR failed: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error processing image", e)
                addMessage("assistant", "❌ Unable to process image: ${e.message}")
            }
        }
    }

    var isCodeMode by mutableStateOf(false)
        private set

    fun toggleCodeMode() {
        isCodeMode = !isCodeMode
    }

    fun sendCode(code: String) {
        val prompt = "Analyze the following code:\n\n```\n$code\n```"
        viewModelScope.launch {
            pruneForNewTokens(llamaAndroid.countTokens(prompt))
        }
        addMessage("user", prompt)
        // Don't call send() to avoid infinite recursion
        // Instead, directly process the code analysis
        processCodeAnalysis(prompt)
    }

    private fun processCodeAnalysis(prompt: String) {
        viewModelScope.launch {
            try {
                startGeneration()
                
                var workingMessages = messages.toMutableList()
                val reserve = 256

                // Trim history until it fits within the context window
                var finalPrompt = ""
                while (true) {
                    finalPrompt = if (template.isNotBlank()) {
                        val jinjava = com.hubspot.jinjava.Jinjava()
                        val context = mapOf("messages" to workingMessages)
                        jinjava.render(template, context)
                    } else {
                        com.nervesparks.iris.llm.TemplateRegistry.render(
                            modelChatFormat,
                            workingMessages,
                            modelSystemPrompt,
                            includeThinkingTags = supportsReasoning
                        )
                    }
                    val promptTokens = llamaAndroid.countTokens(finalPrompt)
                    if (promptTokens <= modelContextLength - reserve || workingMessages.size <= 1) {
                        break
                    }
                    val removeIdx = workingMessages.indexOfFirst { it["role"] != "system" }
                    if (removeIdx >= 0) {
                        workingMessages = workingMessages.drop(removeIdx + 1).toMutableList()
                    } else {
                        break
                    }
                }

                if (workingMessages.size != messages.size) {
                    messages = workingMessages
                    addMessage(
                        "system",
                        "⚠️ Earlier messages were removed to stay within the model's context limit."
                    )
                }

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Log.e(tag, "processCodeAnalysis() failed", it)
                        addMessage("error", it.message ?: "")
                    }
                    .collect {
                        generatedTokens++
                        updateTokenCount(generatedTokens)
                        contextLimit = llamaAndroid.countTokens(finalPrompt) + generatedTokens

                        if (getIsMarked()) {
                            addMessage("codeBlock", it)
                        } else {
                            addMessage("assistant", it)
                        }
                    }
                    .also {
                        endGeneration()
                        persistChat()
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error in processCodeAnalysis", e)
                addMessage("error", "Failed to analyze code: ${e.message}")
                endGeneration()
            }
        }
    }

    fun translate(text: String, targetLanguage: String) {
        val prompt = "Translate the following text to $targetLanguage:\n\n$text"
        viewModelScope.launch {
            pruneForNewTokens(llamaAndroid.countTokens(prompt))
        }
        addMessage("user", prompt)
        processTranslation(prompt)
    }

    /**
     * Cleans the model response by removing raw thinking tags and formatting
     */
    private fun cleanThinkingResponse(response: String): String {
        // Remove raw <think> and </think> tags
        var cleaned = response.replace("<think>", "").replace("</think>", "")
        
        // If the response starts with thinking content, try to extract just the answer
        if (cleaned.contains("Let me think") || cleaned.contains("Okay, let's see")) {
            // Try to find the actual answer after thinking
            val answerPatterns = listOf(
                "So the answer is:",
                "Therefore,",
                "Answer:",
                "The answer is:",
                "Result:"
            )
            
            for (pattern in answerPatterns) {
                if (cleaned.contains(pattern)) {
                    val parts = cleaned.split(pattern)
                    if (parts.size > 1) {
                        cleaned = parts[1].trim()
                        break
                    }
                }
            }
        }
        
        return cleaned.trim()
    }

    private fun processTranslation(prompt: String) {
        viewModelScope.launch {
            try {
                startGeneration()
                
                var workingMessages = messages.toMutableList()
                val reserve = 256

                // Trim history until it fits within the context window
                var finalPrompt = ""
                while (true) {
                    finalPrompt = if (template.isNotBlank()) {
                        val jinjava = com.hubspot.jinjava.Jinjava()
                        val context = mapOf("messages" to workingMessages)
                        jinjava.render(template, context)
                    } else {
                        com.nervesparks.iris.llm.TemplateRegistry.render(
                            modelChatFormat,
                            workingMessages,
                            modelSystemPrompt,
                            includeThinkingTags = supportsReasoning
                        )
                    }
                    val promptTokens = llamaAndroid.countTokens(finalPrompt)
                    if (promptTokens <= modelContextLength - reserve || workingMessages.size <= 1) {
                        break
                    }
                    val removeIdx = workingMessages.indexOfFirst { it["role"] != "system" }
                    if (removeIdx >= 0) {
                        workingMessages = workingMessages.drop(removeIdx + 1).toMutableList()
                    } else {
                        break
                    }
                }

                if (workingMessages.size != messages.size) {
                    messages = workingMessages
                    addMessage(
                        "system",
                        "⚠️ Earlier messages were removed to stay within the model's context limit."
                    )
                }

                contextLimit = llamaAndroid.countTokens(finalPrompt)
                maxContextLimit = modelContextLength

                var generatedTokens = 0
                llamaAndroid.send(finalPrompt)
                    .catch {
                        Log.e(tag, "processTranslation() failed", it)
                        addMessage("error", it.message ?: "")
                    }
                    .collect {
                        generatedTokens++
                        updateTokenCount(generatedTokens)
                        contextLimit = llamaAndroid.countTokens(finalPrompt) + generatedTokens

                        if (getIsMarked()) {
                            addMessage("codeBlock", it)
                        } else {
                            addMessage("assistant", it)
                        }
                    }
                    .also {
                        endGeneration()
                        persistChat()
                    }
            } catch (e: Exception) {
                Log.e(tag, "Error in processTranslation", e)
                addMessage("error", "Failed to translate: ${e.message}")
                endGeneration()
            }
        }
    }

    private fun stripThinking(input: String): String {
        // Remove <think>...</think> segments and any stray tags for display-only purposes
        var out = input
        out = out.replace(Regex("(?is)<think>.*?</think>"), "")
        out = out.replace("<think>", "").replace("</think>", "")
        return out
    }

    suspend fun quantizeModel(model: String, quantizeType: String): Int {
        val inputFile = File(getApplication<Application>().getExternalFilesDir(null), model)
        val outputFile = File(
            getApplication<Application>().getExternalFilesDir(null),
            "${model.substringBeforeLast(".")}-$quantizeType.gguf"
        )
        return llamaAndroid.quantize(inputFile.absolutePath, outputFile.absolutePath, quantizeType)
    }

    private var template by mutableStateOf("")

    fun updateTemplate(template: String) {
        this.template = template
    }

    // User-defined prompt templates
    var templates = mutableStateListOf<Template>()
        private set

    fun addTemplate(template: Template): Boolean {
        return try {
            templates.add(template)
            userPreferencesRepository.saveTemplates(templates)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun editTemplate(updated: Template): Boolean {
        val index = templates.indexOfFirst { it.id == updated.id }
        return if (index != -1) {
            try {
                templates[index] = updated
                userPreferencesRepository.saveTemplates(templates)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun deleteTemplate(template: Template): Boolean {
        val removed = templates.removeAll { it.id == template.id }
        return if (removed) {
            try {
                userPreferencesRepository.saveTemplates(templates)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun clearLastQuickAction() {
        lastQuickAction = null
    }

    fun clearLastAttachmentAction() {
        lastAttachmentAction = null
    }

    // Performance monitoring variables
    var tps by mutableStateOf(0.0) // Tokens per second
    var ttft by mutableStateOf(0L) // Time to first token (milliseconds)
    var latency by mutableStateOf(0L) // Average latency per token (milliseconds)
    var memoryUsage by mutableStateOf(0L) // Memory usage in MB
    var contextLimit by mutableStateOf(0) // Current context length
    var maxContextLimit by mutableStateOf(0) // Maximum context limit
    var isGenerating by mutableStateOf(false) // Whether currently generating
    var generationStartTime by mutableStateOf(0L) // Start time of generation
    var tokensGenerated by mutableStateOf(0) // Number of tokens generated in current session
    var totalGenerationTime by mutableStateOf(0L) // Total generation time in milliseconds

    // GPU offload reporting (N/N)
    var offloadedLayers by mutableStateOf(-1)
    var totalLayers by mutableStateOf(-1)

    // Performance monitoring functions
    fun startGeneration() {
        isGenerating = true
        generationStartTime = System.currentTimeMillis()
        tokensGenerated = 0
        totalGenerationTime = 0L
        ttft = 0L
        tps = 0.0
        latency = 0L
    }

    fun endGeneration() {
        isGenerating = false
        val endTime = System.currentTimeMillis()
        totalGenerationTime = endTime - generationStartTime
        
        // Calculate TPS if we have tokens and time
        if (tokensGenerated > 0 && totalGenerationTime > 0) {
            tps = (tokensGenerated * 1000.0) / totalGenerationTime
        }
        
        // Calculate average latency
        if (tokensGenerated > 0) {
            latency = totalGenerationTime / tokensGenerated
        persistChat()
        }
    }

    fun updateTokenCount(count: Int) {
        tokensGenerated = count
        if (ttft == 0L && count > 0) {
            // Calculate time to first token
            ttft = System.currentTimeMillis() - generationStartTime
        }
        
        // Update total generation time
        totalGenerationTime = System.currentTimeMillis() - generationStartTime
        
        // Calculate TPS in real-time
        val elapsedTime = totalGenerationTime.toDouble()
        if (elapsedTime > 0) {
            tps = (count * 1000.0) / elapsedTime
        }
        
        // Calculate average latency
        if (count > 0) {
            latency = totalGenerationTime / count
        }
        
        // Update memory usage periodically
        viewModelScope.launch {
            try {
                val memoryUsage = getMemoryUsage()
                updateMemoryUsage(memoryUsage / (1024 * 1024)) // Convert to MB
            } catch (e: Exception) {
                Log.e(tag, "Error updating memory usage", e)
            }
        }
    }

    fun updateMemoryUsage(usageMB: Long) {
        memoryUsage = usageMB
    }
    
    fun getCurrentMemoryUsage(): Long {
        return memoryUsage
    }
    
    suspend fun refreshMemoryUsage() {
        try {
            val usage = llamaAndroid.getMemoryUsage()
            updateMemoryUsage(usage)
        } catch (e: Exception) {
            Log.e(tag, "Error getting memory usage: ${e.message}")
            updateMemoryUsage(0)
        }
    }

    fun updateContextLimit(current: Int, max: Int) {
        contextLimit = current
        maxContextLimit = max
    }

    fun resetPerformanceMetrics() {
        tps = 0.0
        ttft = 0L
        latency = 0L
        tokensGenerated = 0
        totalGenerationTime = 0L
        isGenerating = false
    }

    var showModelSettings by mutableStateOf(false)

    // Model configuration functions
    fun updateModelSettings(
        temperature: Float = modelTemperature,
        topP: Float = modelTopP,
        topK: Int = modelTopK,
        maxTokens: Int = modelMaxTokens,
        contextLength: Int = modelContextLength,
        systemPrompt: String = modelSystemPrompt,
        chatFormat: String = modelChatFormat,
        threadCount: Int = modelThreadCount,
        gpuLayers: Int = -1
    ) {
        modelTemperature = temperature
        modelTopP = topP
        modelTopK = topK
        modelMaxTokens = maxTokens
        modelContextLength = contextLength
        modelSystemPrompt = systemPrompt
        modelChatFormat = chatFormat
        modelThreadCount = threadCount
        // only set if provided
        if (gpuLayers != -2) {
            try {
                val v = gpuLayers
                // store in a backing pref via repository if available later in file
            } catch (_: Exception) {}
        }
        
        // Save to preferences
        userPreferencesRepository.setModelTemperature(temperature)
        userPreferencesRepository.setModelTopP(topP)
        userPreferencesRepository.setModelTopK(topK)
        userPreferencesRepository.setModelMaxTokens(maxTokens)
        userPreferencesRepository.setModelContextLength(contextLength)
        userPreferencesRepository.setModelSystemPrompt(systemPrompt)
        userPreferencesRepository.setModelChatFormat(chatFormat)
        userPreferencesRepository.setModelThreadCount(threadCount)
        userPreferencesRepository.setModelGpuLayers(gpuLayers)
    }

    fun loadModelSettings() {
        try {
            Log.d("MainViewModel", "Loading model settings...")
            
            // Try to load each setting with individual try-catch blocks
            try {
                modelTemperature = userPreferencesRepository.getModelTemperature()
                Log.d("MainViewModel", "Loaded temperature: $modelTemperature")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading temperature, using default", e)
                modelTemperature = 0.7f
            }
            
            try {
                modelTopP = userPreferencesRepository.getModelTopP()
                Log.d("MainViewModel", "Loaded topP: $modelTopP")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading topP, using default", e)
                modelTopP = 0.9f
            }
            
            try {
                modelTopK = userPreferencesRepository.getModelTopK()
                Log.d("MainViewModel", "Loaded topK: $modelTopK")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading topK, using default", e)
                modelTopK = 40
            }
            
            try {
                modelMaxTokens = userPreferencesRepository.getModelMaxTokens()
                Log.d("MainViewModel", "Loaded maxTokens: $modelMaxTokens")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading maxTokens, using default", e)
                modelMaxTokens = 2048
            }
            
            try {
                modelContextLength = userPreferencesRepository.getModelContextLength()
                Log.d("MainViewModel", "Loaded contextLength: $modelContextLength")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading contextLength, using default", e)
                modelContextLength = 32768  // Increased for Qwen3 support
            }
            // Guard against early init races
            try { maxContextLimit = modelContextLength } catch (_: Throwable) {}
            
            try {
                modelSystemPrompt = userPreferencesRepository.getModelSystemPrompt()
                Log.d("MainViewModel", "Loaded systemPrompt: $modelSystemPrompt")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading systemPrompt, using default", e)
                modelSystemPrompt = "You are a helpful AI assistant."
            }
            
            try {
                modelChatFormat = userPreferencesRepository.getModelChatFormat()
                Log.d("MainViewModel", "Loaded chatFormat: $modelChatFormat")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading chatFormat, using default", e)
                modelChatFormat = "CHATML"
            }
            
            try {
                modelThreadCount = userPreferencesRepository.getModelThreadCount()
                Log.d("MainViewModel", "Loaded threadCount: $modelThreadCount")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading threadCount, using default", e)
                modelThreadCount = 4
            }

            try {
                modelGpuLayers = userPreferencesRepository.getModelGpuLayers()
                Log.d("MainViewModel", "Loaded gpuLayers: $modelGpuLayers")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading gpuLayers, using default", e)
                modelGpuLayers = -1
            }
            
            Log.d("MainViewModel", "Model settings loaded successfully")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in loadModelSettings", e)
        }
    }

    fun showModelSettings() {
        showModelSettings = true
    }

    fun hideModelSettings() {
        showModelSettings = false
    }

    // Thinking token settings
    fun updateShowThinkingTokens(show: Boolean) {
        showThinkingTokens = show
        userPreferencesRepository.setShowThinkingTokens(show)
        applyThinkStripGate()
    }

    fun updateThinkingTokenStyle(style: String) {
        thinkingTokenStyle = style
        userPreferencesRepository.setThinkingTokenStyle(style)
    }

    fun loadThinkingTokenSettings() {
        try {
            Log.d("MainViewModel", "Loading thinking token settings...")
            
            try {
                showThinkingTokens = userPreferencesRepository.getShowThinkingTokens()
                Log.d("MainViewModel", "Loaded showThinkingTokens: $showThinkingTokens")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading showThinkingTokens, using default", e)
                showThinkingTokens = true
            }
            
            try {
                thinkingTokenStyle = userPreferencesRepository.getThinkingTokenStyle()
                Log.d("MainViewModel", "Loaded thinkingTokenStyle: $thinkingTokenStyle")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading thinkingTokenStyle, using default", e)
                thinkingTokenStyle = "COLLAPSIBLE"
            }
            
            Log.d("MainViewModel", "Thinking token settings loaded successfully")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error in loadThinkingTokenSettings", e)
        }
    }


    var refresh by mutableStateOf(false)

    fun loadExistingModels(directory: File) {
        Log.d(tag, "loadExistingModels called with directory: ${directory.absolutePath}")
        Log.d(tag, "Directory exists: ${directory.exists()}")
        Log.d(tag, "Directory is readable: ${directory.canRead()}")
        
        // List all files in directory first
        val allFiles = directory.listFiles()
        Log.d(tag, "All files in directory: ${allFiles?.size ?: 0}")
        allFiles?.forEach { file ->
            Log.d(tag, "File: ${file.name}, extension: ${file.extension}, isFile: ${file.isFile}")
        }
        
        // List models in the directory that end with .gguf
        val ggufFiles = directory.listFiles { file -> file.extension == "gguf" }
        Log.d(tag, "Found ${ggufFiles?.size ?: 0} .gguf files")
        
        ggufFiles?.forEach { file ->
            val modelName = file.name
            Log.i("This is the modelname", modelName)
            if (!allModels.any { it["name"] == modelName }) {
                allModels += mapOf(
                    "name" to modelName,
                    "source" to "local",
                    "destination" to file.name,
                    "supportsReasoning" to "false"
                )
                Log.d(tag, "Added model to allModels: $modelName")
            } else {
                Log.d(tag, "Model already in allModels: $modelName")
            }
        }
        
        Log.d(tag, "Found ${allModels.size} total models")
        
        // Check if we have any models available
        // De-duplicate by name while preserving first occurrence (curated > API > local)
        val deduped = allModels
            .asReversed()
            .distinctBy { it["name"] }
            .asReversed()

        val availableModels = deduped.filter { model ->
            val destinationPath = File(directory, model["destination"].toString())
            destinationPath.exists()
        }
        
        Log.d(tag, "Available models: ${availableModels.size}")
        availableModels.forEach { model ->
            Log.d(tag, "Available model: ${model["name"]}")
        }

        if (availableModels.isNotEmpty()) {
            Log.d(tag, "Models available, setting up UI for model selection")
            
            // Set the first available model as currentDownloadable but don't auto-load
            val firstModel = availableModels.first()
            val destinationPath = File(directory, firstModel["destination"].toString())
            currentDownloadable = Downloadable(
                firstModel["name"].toString(),
                Uri.parse(firstModel["source"].toString()),
                destinationPath
            )
            
            // If we have a default model and it exists, use it
            if (defaultModelName.value.isNotEmpty()) {
                val defaultModel = availableModels.find { model -> model["name"] == defaultModelName.value }
                if (defaultModel != null) {
                    val defaultPath = File(directory, defaultModel["destination"].toString())
                    currentDownloadable = Downloadable(
                        defaultModel["name"].toString(),
                        Uri.parse(defaultModel["source"].toString()),
                        defaultPath
                    )
                }
            }
            
            // DON'T show download modal when models exist - show model selection instead
            showModal = false
            showModelSelection = true
            Log.d(tag, "Set showModal=false, showModelSelection=true")
        } else {
            Log.d(tag, "No models available, showing download modal")
            // No models available, show download modal
            showModal = true
            showModelSelection = false
        }
    }

    // New function to switch between models
    fun switchModel(modelName: String, directory: File) {
        val model = allModels.find { it["name"] == modelName }
        if (model != null) {
            val destinationPath = File(directory, model["destination"].toString())
            if (destinationPath.exists()) {
                // Set reasoning support based on model configuration
                Log.d(tag, "=== SWITCH MODEL REASONING DEBUG ===")
                Log.d(tag, "Switching to model: $modelName")
                Log.d(tag, "Model configuration: $model")
                
                val reasoningSupport = model["supportsReasoning"]
                Log.d(tag, "Raw reasoning support value: $reasoningSupport")
                
                supportsReasoning = reasoningSupport == "true"
                Log.d(tag, "Final supportsReasoning: $supportsReasoning")
                Log.d(tag, "=== END SWITCH MODEL DEBUG ===")
                
                // Unload current model if any
                viewModelScope.launch {
                    try {
                        llamaAndroid.unload()
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error unloading model", e)
                    }
                }
                
                // Set new model as current
                currentDownloadable = Downloadable(
                    model["name"].toString(),
                    Uri.parse(model["source"].toString()),
                    destinationPath
                )
                
                // Load the new model with CPU backend to avoid OpenCL issues
                Log.d(tag, "Loading model with CPU backend to avoid OpenCL issues")
                load(destinationPath.path, modelThreadCount, backend = "cpu")
                
                // Update default model name
                setDefaultModelName(modelName)
                
                Log.i("MainViewModel", "Switched to model: $modelName")
            }
        }
    }

    // New function to get available models (delegate to repository cache)
    fun getAvailableModels(directory: File): List<Map<String, String>> =
        try {
            // fast path from cached state if available
            val cached = allModels.filter { model ->
                val destinationPath = File(directory, model["destination"].toString())
                destinationPath.exists()
            }
            if (cached.isNotEmpty()) cached else emptyList()
        } catch (e: Exception) {
            Log.e(tag, "Error getting available models", e)
            emptyList()
        }

    // Model selection UI functions
    fun showModelSelectionDialog() {
        showModelSelection = true
    }

    fun hideModelSelectionDialog() {
        showModelSelection = false
        selectedModelForSwitch = ""
    }

    fun selectModelForSwitch(modelName: String) {
        selectedModelForSwitch = modelName
    }

    fun confirmModelSwitch(directory: File) {
        if (selectedModelForSwitch.isNotEmpty()) {
            switchModel(selectedModelForSwitch, directory)
            hideModelSelectionDialog()
        }
    }



    fun textToSpeech(context: Context) {
        if (!getIsSending()) {
            // If TTS is already initialized, stop it first
            textToSpeech?.stop()

            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.let { txtToSpeech ->
                        txtToSpeech.language = Locale.US
                        txtToSpeech.setSpeechRate(1.0f)

                        // Add a unique utterance ID for tracking
                        val utteranceId = UUID.randomUUID().toString()

                        txtToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onDone(utteranceId: String?) {
                                // Reset state when speech is complete
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onError(utteranceId: String?) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = true
                                }
                            }

                            override fun onStart(utteranceId: String?) {
                                // Update state to indicate speech is playing
                                CoroutineScope(Dispatchers.Main).launch {
                                    stateForTextToSpeech = false
                                }
                            }
                        })

                        txtToSpeech.speak(
                            textForTextToSpeech,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            utteranceId
                        )
                    }
                }
            }
        }
    }



    fun stopTextToSpeech() {
        textToSpeech?.apply {
            stop()  // Stops current speech
            shutdown()  // Releases the resources
        }
        textToSpeech = null

        // Reset state to allow restarting
        stateForTextToSpeech = true
    }



    var toggler by mutableStateOf(false)
    var showAlert by mutableStateOf(false)
    var switchModal by mutableStateOf(false)
    var currentDownloadable: Downloadable? by mutableStateOf(null)

    override fun onCleared() {
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
        super.onCleared()

        viewModelScope.launch {
            try {

                llamaAndroid.unload()

            } catch (exc: IllegalStateException) {
                addMessage("error", exc.message ?: "")
            }
        }
    }

    private suspend fun pruneForNewTokens(additionalTokens: Int) {
        if (contextLimit + additionalTokens <= modelContextLength) return

        var workingMessages = messages.toMutableList()
        var pruned = false
        while (contextLimit + additionalTokens > modelContextLength && workingMessages.size > 1) {
            val removeIdx = workingMessages.indexOfFirst { it["role"] != "system" }
            if (removeIdx >= 0) {
                workingMessages = workingMessages.drop(removeIdx + 1).toMutableList()
                val prompt = if (template.isNotBlank()) {
                    val jinjava = com.hubspot.jinjava.Jinjava()
                    val context = mapOf("messages" to workingMessages)
                    jinjava.render(template, context)
                } else {
                    com.nervesparks.iris.llm.TemplateRegistry.render(
                        modelChatFormat,
                        workingMessages,
                        modelSystemPrompt,
                        includeThinkingTags = supportsReasoning
                    )
                }
                contextLimit = llamaAndroid.countTokens(prompt)
                pruned = true
            } else {
                break
            }
        }

        if (pruned) {
            messages = workingMessages
            addMessage(
                "system",
                "⚠️ Earlier messages were removed to stay within the model's context limit."
            )
            val prompt = if (template.isNotBlank()) {
                val jinjava = com.hubspot.jinjava.Jinjava()
                val context = mapOf("messages" to messages)
                jinjava.render(template, context)
            } else {
                com.nervesparks.iris.llm.TemplateRegistry.render(
                    modelChatFormat,
                    messages,
                    modelSystemPrompt,
                    includeThinkingTags = supportsReasoning
                )
            }
            contextLimit = llamaAndroid.countTokens(prompt)
        }
    }

    fun send() {
        Log.d(tag, "Send button clicked")
        val reserveTokens = 256
        val userMessage = removeExtraWhiteSpaces(message)
        message = ""

        if (isCodeMode) {
            sendCode(userMessage)
            return
        }

        // Preflight: ensure native handles are valid before attempting to send
        runCatching {
            val mh = llamaAndroid.getModel()
            val ch = llamaAndroid.getContext()
            val bh = llamaAndroid.getBatch()
            val sh = llamaAndroid.getSampler()
            if (mh == 0L || ch == 0L || bh == 0L || sh == 0L) {
                addMessage("error", "Model not loaded or not ready. Load a model first.")
                return
            }
        }

        // Add to messages console.
        if (userMessage.isNotBlank()) {
            Log.d(tag, "User message is not blank: $userMessage")
            if(first){
                addMessage("system", "This is a conversation between User and Iris, a friendly chatbot. Iris is helpful, kind, honest, good at writing, and never fails to answer any requests immediately and with precision. When responding, Iris should use <think> tags to show its reasoning process before providing the final answer.")
                addMessage("user", "Hi")
                addMessage("assistant", "How may I help You?")
                first = false
            }

            viewModelScope.launch {
                pruneForNewTokens(llamaAndroid.countTokens(userMessage))
            }
            addMessage("user", userMessage)
            persistChat()

            // Start performance monitoring
            startGeneration()

            viewModelScope.launch {
                // If GPU offload is effectively zero, force CPU backend for stability
                runCatching {
                    if (llamaAndroid.isAdrenoGpu() && llamaAndroid.getOffloadCounts()[0] == 0) {
                        Log.i(tag, "Detected zero GPU offload; switching backend to CPU for this session")
                        llamaAndroid.setBackend("cpu")
                    }
                }
                try {
                    val userEmbedding = embedText(userMessage)
                    val similarDocs = if (userEmbedding.isNotEmpty()) {
                        documentRepository.topKSimilar(userEmbedding.toList(), 3)
                    } else emptyList()
                    val contextDocs = similarDocs.joinToString("\n") { it.text }
                    val fullMessage = if (contextDocs.isNotEmpty()) {
                        "Context: $contextDocs\n\nQuestion: $userMessage"
                    } else {
                        userMessage
                    }

            var workingMessages = messages.toMutableList()
                    val reserve = reserveTokens

                    // Trim history until it fits within the context window
                    var prompt = ""
                    while (true) {
                        prompt = if (template.isNotBlank()) {
                            val jinjava = com.hubspot.jinjava.Jinjava()
                            val context = mapOf("messages" to workingMessages)
                            jinjava.render(template, context)
                        } else {
                            com.nervesparks.iris.llm.TemplateRegistry.render(
                                modelChatFormat,
                                workingMessages,
                                modelSystemPrompt,
                                includeThinkingTags = supportsReasoning
                            )
                        }
                        // Count on stripped template if thinking is disabled
                        val countPrompt = if (supportsReasoning && showThinkingTokens) prompt else stripThinking(prompt)
                        val promptTokens = llamaAndroid.countTokens(countPrompt)
                        if (promptTokens <= modelContextLength - reserve || workingMessages.size <= 1) {
                            break
                        }
                        val removeIdx = workingMessages.indexOfFirst { it["role"] != "system" }
                        if (removeIdx >= 0) {
                            workingMessages = workingMessages.drop(removeIdx + 1).toMutableList()
                        } else {
                            break
                        }
                    }

                    if (workingMessages.size != messages.size) {
                        messages = workingMessages
                        addMessage(
                            "system",
                            "⚠️ Earlier messages were removed to stay within the model's context limit."
                        )
                    }

                    val countPrompt2 = if (supportsReasoning && showThinkingTokens) prompt else stripThinking(prompt)
                    contextLimit = llamaAndroid.countTokens(countPrompt2)
                    maxContextLimit = modelContextLength

                    var generatedTokens = 0
                    llamaAndroid.send(prompt)
                        .catch {
                            Log.e(tag, "send() failed", it)
                            addMessage("error", it.message ?: "")
                        }
                        .collect {
                            generatedTokens++
                            updateTokenCount(generatedTokens)
                            contextLimit = llamaAndroid.countTokens(prompt) + generatedTokens

                            if (getIsMarked()) {
                                addMessage("codeBlock", it)
                            } else {
                                try {
                                    val json = org.json.JSONObject(it)
                                    if (json.has("tool")) {
                                        val tool = json.getString("tool")
                                        val args = json.getJSONObject("args")
                                        val argsMap = mutableMapOf<String, Any>()
                                        args.keys().forEach { key ->
                                            argsMap[key] = args.get(key)
                                        }
                                        handleToolCall(com.nervesparks.iris.data.ToolCall(tool, argsMap))
                                    } else {
                                        val display = if (showThinkingTokens && supportsReasoning) it else stripThinking(it)
                                        addMessage("assistant", display)
                                    }
                                } catch (e: org.json.JSONException) {
                                    val display = if (showThinkingTokens && supportsReasoning) it else stripThinking(it)
                                    addMessage("assistant", display)
                                }
                            }
                        }
                } finally {
                    endGeneration()
                    if (!getIsCompleteEOT()) {
                        trimEOT()
                    }
                }
            

            }
        } else {
            Log.d(tag, "User message is blank")
        }
    }

//    fun bench(pp: Int, tg: Int, pl: Int, nr: Int = 1) {
//        viewModelScope.launch {
//            try {
//                val start = System.nanoTime()
//                val warmupResult = llamaAndroid.bench(pp, tg, pl, nr)
//                val end = System.nanoTime()
//
//                messages += warmupResult
//
//                val warmup = (end - start).toDouble() / NanosPerSecond
//                messages += "Warm up time: $warmup seconds, please wait..."
//
//                if (warmup > 5.0) {
//                    messages += "Warm up took too long, aborting benchmark"
//                    return@launch
//                }
//
//                messages += llamaAndroid.bench(512, 128, 1, 3)
//            } catch (exc: IllegalStateException) {
//                Log.e(tag, "bench() failed", exc)
//                messages += exc.message!!
//            }
//        }
//    }

    suspend fun unload(){
        llamaAndroid.unload()
    }

    var tokensList = mutableListOf<String>() // Store emitted tokens
    var benchmarkStartTime: Long = 0L // Track the benchmark start time
    var tokensPerSecondsFinal: Double by mutableStateOf(0.0) // Track tokens per second and trigger UI updates
    var isBenchmarkingComplete by mutableStateOf(false) // Flag to track if benchmarking is complete
    
    // Comparative benchmark results
    var comparativeBenchmarkResults by mutableStateOf<Map<String, Any>?>(null)
    var isComparativeBenchmarkRunning by mutableStateOf(false)
    var selectedBenchmarkModel by mutableStateOf("")
    var showBenchmarkModelSelection by mutableStateOf(false)

    fun myCustomBenchmark() {
        viewModelScope.launch {
            try {
                tokensList.clear() // Reset the token list before benchmarking
                benchmarkStartTime = System.currentTimeMillis() // Record the start time
                isBenchmarkingComplete = false // Reset benchmarking flag

                // Launch a coroutine to update the tokens per second every second
                launch {
                    while (!isBenchmarkingComplete) {
                        delay(1000L) // Delay 1 second
                        val elapsedTime = System.currentTimeMillis() - benchmarkStartTime
                        if (elapsedTime > 0) {
                            tokensPerSecondsFinal = tokensList.size.toDouble() / (elapsedTime / 1000.0)
                        }
                    }
                }

                llamaAndroid.myCustomBenchmark()
                    .collect { emittedString ->
                        if (emittedString != null) {
                            tokensList.add(emittedString) // Add each token to the list
                            Log.d(tag, "Token collected: $emittedString")
                        }
                    }
            } catch (exc: IllegalStateException) {
                Log.e(tag, "myCustomBenchmark() failed", exc)
            } catch (exc: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(tag, "myCustomBenchmark() timed out", exc)
            } catch (exc: Exception) {
                Log.e(tag, "Unexpected error during myCustomBenchmark()", exc)
            } finally {
                // Benchmark complete, log the final tokens per second value
                val elapsedTime = System.currentTimeMillis() - benchmarkStartTime
                val finalTokensPerSecond = if (elapsedTime > 0) {
                    tokensList.size.toDouble() / (elapsedTime / 1000.0)
                } else {
                    0.0
                }
                Log.d(tag, "Benchmark complete. Tokens/sec: $finalTokensPerSecond")

                // Update the final tokens per second and stop updating the value
                tokensPerSecondsFinal = finalTokensPerSecond
                isBenchmarkingComplete = true // Mark benchmarking as complete
            }
        }
    }
    
    fun runComparativeBenchmark() {
        viewModelScope.launch {
            try {
                Log.d(tag, "Starting comparative benchmark...")
                isComparativeBenchmarkRunning = true
                comparativeBenchmarkResults = null
                
                                    // Use actual model and context handles for real benchmarking
                    Log.d(tag, "Calling native benchmark function with real model...")
                    val modelHandle = llamaAndroid.getModel()
                    val contextHandle = llamaAndroid.getContext()
                    val batchHandle = llamaAndroid.getBatch()
                    val samplerHandle = llamaAndroid.getSampler()

                    Log.d(tag, "Model handle: $modelHandle, Context handle: $contextHandle")

                    // Verify that all handles are valid before proceeding
                    if (modelHandle == 0L || contextHandle == 0L || batchHandle == 0L || samplerHandle == 0L) {
                        Log.e(tag, "Model not loaded. Skipping comparative benchmark.")
                        comparativeBenchmarkResults = mapOf("error" to "Model not loaded.")
                        return@launch
                    }

                // Ensure we're on the correct thread where the library is loaded and call native
                val resultsJson = withContext(llamaAndroid.runLoop) {
                    llamaAndroid.runComparativeBenchmark(modelHandle, contextHandle, batchHandle, samplerHandle)
                }
                Log.d(tag, "Native benchmark returned: $resultsJson")
                
                // Parse the JSON results
                try {
                    val jsonObject = org.json.JSONObject(resultsJson)
                    val results = mutableMapOf<String, Any>()
                    
                    // Parse CPU results
                    val cpuObj = jsonObject.getJSONObject("cpu")
                    results["cpu_tokens_per_sec"] = cpuObj.getDouble("tokens_per_sec")
                    results["cpu_duration_ms"] = cpuObj.getInt("duration_ms")
                    results["cpu_tokens_generated"] = cpuObj.getInt("tokens_generated")
                    
                    // Parse GPU results
                    val gpuObj = jsonObject.getJSONObject("gpu")
                    results["gpu_available"] = gpuObj.getBoolean("available")
                    
                    if (results["gpu_available"] as Boolean) {
                        results["gpu_tokens_per_sec"] = gpuObj.getDouble("tokens_per_sec")
                        results["gpu_duration_ms"] = gpuObj.getInt("duration_ms")
                        results["gpu_tokens_generated"] = gpuObj.getInt("tokens_generated")
                        
                        // Calculate speedup
                        val speedup = jsonObject.getDouble("speedup")
                        results["speedup"] = speedup
                        results["speedup_percentage"] = ((speedup - 1.0) * 100.0)
                    } else {
                        results["gpu_error"] = gpuObj.getString("error")
                    }
                    
                    comparativeBenchmarkResults = results
                    Log.d(tag, "Comparative benchmark completed: $results")
                    Log.d(tag, "Setting comparativeBenchmarkResults to: $comparativeBenchmarkResults")
                    
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing benchmark results", e)
                    comparativeBenchmarkResults = mapOf("error" to "Failed to parse results: ${e.message}")
                }
                
            } catch (e: Exception) {
                Log.e(tag, "Comparative benchmark failed", e)
                comparativeBenchmarkResults = mapOf("error" to "Benchmark failed: ${e.message}")
            } finally {
                isComparativeBenchmarkRunning = false
            }
        }
    }
    
    fun showBenchmarkModelSelection() {
        showBenchmarkModelSelection = true
    }
    
    fun hideBenchmarkModelSelection() {
        showBenchmarkModelSelection = false
        selectedBenchmarkModel = ""
    }
    
    fun runBenchmarkWithModel(modelName: String, directory: File) {
        viewModelScope.launch {
            isComparativeBenchmarkRunning = true

            // Save the currently loaded model so we can restore it later
            val previousModelName = loadedModelName.value
            val previousModelPath = allModels
                .find { it["name"] == previousModelName }
                ?.let { File(directory, it["destination"].toString()).path }

            try {
                Log.d(tag, "Starting benchmark with model: $modelName")

                // Attempt to load the user selected model
                val loaded = loadModelByName(modelName, directory)
                if (!loaded) {
                    comparativeBenchmarkResults = mapOf("error" to "Failed to load model: $modelName")
                    return@launch
                }
                
                // Wait for the model to fully load and get valid handles
                var attempts = 0
                var modelHandle = 0L
                var contextHandle = 0L
                var batchHandle = 0L
                var samplerHandle = 0L
                
                while (attempts < 10 && (modelHandle == 0L || contextHandle == 0L || batchHandle == 0L || samplerHandle == 0L)) {
                    delay(500) // Wait 500ms between attempts
                    modelHandle = llamaAndroid.getModel()
                    contextHandle = llamaAndroid.getContext()
                    batchHandle = llamaAndroid.getBatch()
                    samplerHandle = llamaAndroid.getSampler()
                    attempts++
                    Log.d(tag, "Attempt $attempts: Model=$modelHandle, Context=$contextHandle, Batch=$batchHandle, Sampler=$samplerHandle")
                }
                
                if (modelHandle == 0L || contextHandle == 0L || batchHandle == 0L || samplerHandle == 0L) {
                    Log.e(tag, "Model handles still invalid after $attempts attempts")
                    comparativeBenchmarkResults = mapOf("error" to "Model not properly loaded after $attempts attempts")
                    return@launch
                }

                // Run the benchmark and capture any errors
                try {
                    runComparativeBenchmark()
                } catch (benchExc: Exception) {
                    Log.e(tag, "Benchmark with model failed", benchExc)
                    comparativeBenchmarkResults = mapOf("error" to "Benchmark failed: ${benchExc.message}")
                }

            } finally {
                // Restore the previously loaded model
                if (previousModelPath != null) {
                    try {
                        val backend = if (currentBackend.equals("OpenCL", true)) "opencl" else "cpu"
                        load(previousModelPath, userThreads = modelThreadCount, backend = backend)
                    } catch (restoreExc: Exception) {
                        Log.e(tag, "Failed to reload previous model", restoreExc)
                    }
                }
                isComparativeBenchmarkRunning = false
                hideBenchmarkModelSelection()
            }
        }
    }

    var loadedModelName = mutableStateOf("");

    fun load(pathToModel: String, userThreads: Int, backend: String = "cpu")  {
        viewModelScope.launch {
            try{
                llamaAndroid.unload()
            } catch (exc: IllegalStateException){
                Log.e(tag, "load() failed", exc)
            }
                            try {
                // Set desired backend prior to loading model/context
                try {
                    val requested = backend.lowercase()
                    val success = llamaAndroid.setBackend(requested)
                    if (success) {
                        currentBackend = if (requested == "vulkan") "Vulkan" else if (requested == "opencl") "OpenCL" else "CPU"
                        Log.d(tag, "Backend set OK: $currentBackend")
                    } else {
                        currentBackend = "CPU"
                        Log.e(tag, "Backend set failed for '$requested', falling back to CPU")
                    }
                } catch (be: Exception) {
                    currentBackend = "CPU"
                    Log.e(tag, "Exception when setting backend to $backend: ${be.message}")
                }
                // If GPU was requested but registry is missing, force CPU and retry
                if (currentBackend.equals("Vulkan", true) && !availableBackends.contains("Vulkan")) {
                    Log.w(tag, "Vulkan requested but not present; forcing CPU backend")
                    try { llamaAndroid.setBackend("cpu"); currentBackend = "CPU" } catch (_: Exception) {}
                }
                
                var modelName = pathToModel.split("/")
                loadedModelName.value = modelName.last()
                
                // Add reasoning support detection here
                Log.d(tag, "=== REASONING SUPPORT DEBUG ===")
                Log.d(tag, "Loading model: ${loadedModelName.value}")
                Log.d(tag, "All models count: ${allModels.size}")
                Log.d(tag, "All model names: ${allModels.map { it["name"] }}")
                
                val foundModel = allModels.find { it["name"] == loadedModelName.value }
                Log.d(tag, "Found model in allModels: $foundModel")
                
                val reasoningSupport = foundModel?.get("supportsReasoning")
                Log.d(tag, "Raw reasoning support value: $reasoningSupport")
                
                // Fallback logic: if model not found, check if it's a known reasoning model
                supportsReasoning = if (reasoningSupport == "true") {
                    true
                } else if (foundModel == null) {
                    // Fallback: check if model name contains reasoning-related keywords
                    val reasoningKeywords = listOf("reasoning", "think", "qwen", "nemotron", "openreasoning")
                    val hasReasoningKeyword = reasoningKeywords.any { keyword ->
                        loadedModelName.value.lowercase().contains(keyword.lowercase())
                    }
                    Log.d(tag, "Model not found in allModels, checking keywords. Has reasoning keyword: $hasReasoningKeyword")
                    hasReasoningKeyword
                } else {
                    false
                }
                
                // Remove forced reasoning; trust metadata / keyword fallback only
                
                Log.d(tag, "Final supportsReasoning: $supportsReasoning")
                Log.d(tag, "=== END REASONING DEBUG ===")
                
                showModal = false
                showAlert = true
                
                Log.d(tag, "Loading model with settings: backend=$currentBackend, threads=$modelThreadCount, ngl=${modelGpuLayers}, topK=$modelTopK, topP=$modelTopP, temp=$modelTemperature")
                // Ensure native library is loaded before first JNI call
                if (!llamaAndroid.isNativeLibraryLoaded()) {
                    Log.w(tag, "Native lib not yet loaded; attempting sync load")
                    llamaAndroid.ensureLibraryLoaded()
                }
                
                // Use model settings instead of default parameters
                llamaAndroid.load(
                    pathToModel,
                    userThreads = modelThreadCount,
                    topK = modelTopK,
                    topP = modelTopP,
                    temp = modelTemperature,
                    gpuLayers = modelGpuLayers
                )
                
                Log.d(tag, "Model loaded successfully: ${loadedModelName.value}")
                showAlert = false
                applyThinkStripGate()
                // Keep native token logs quiet in normal operation
                try { llamaAndroid.setVerboseTokens(false) } catch (_: Exception) {}
                // Fetch offload counts for display
                try {
                    val counts = llamaAndroid.getOffloadCounts()
                    if (counts.size == 2) {
                        offloadedLayers = counts[0]
                        totalLayers = counts[1]
                        Log.d(tag, "Offload counts: ${counts[0]}/${counts[1]}")
                    }
                } catch (_: Exception) {}
                // Export runtime diagnostics
                try {
                    val diag = llamaAndroid.exportDiag()
                    Log.d(tag, "Runtime diag: $diag")
                } catch (_: Exception) {}

            } catch (exc: IllegalStateException) {
                Log.e(tag, "load() failed", exc)
                // Since OpenCL is disabled, just report the error
                addMessage("error", "Failed to load model: ${exc.message}")
            } catch (exc: Exception) {
                Log.e(tag, "load() failed with exception", exc)
                addMessage("error", "Failed to load model: ${exc.message}")
            }
            showModal = false
            showAlert = false
            eot_str = llamaAndroid.send_eot_str()
        }
    }
    
    /**
     * Load a model by file path - wrapper for the load method
     */
    fun loadModel(modelPath: String) {
        val modelName = modelPath.substringAfterLast("/")
        Log.d(tag, "=== REASONING SUPPORT DEBUG ===")
        Log.d(tag, "Loading model: $modelName")
        Log.d(tag, "All models count: ${allModels.size}")
        Log.d(tag, "All model names: ${allModels.map { it["name"] }}")
        
        val foundModel = allModels.find { it["name"] == modelName }
        Log.d(tag, "Found model in allModels: $foundModel")
        
        val reasoningSupport = foundModel?.get("supportsReasoning")
        Log.d(tag, "Raw reasoning support value: $reasoningSupport")
        
        // Fallback logic: if model not found, check if it's a known reasoning model
        supportsReasoning = if (reasoningSupport == "true") {
            true
        } else if (foundModel == null) {
            // Fallback: check if model name contains reasoning-related keywords
            val reasoningKeywords = listOf("reasoning", "think", "qwen", "nemotron", "openreasoning")
            val hasReasoningKeyword = reasoningKeywords.any { keyword ->
                modelName.lowercase().contains(keyword.lowercase())
            }
            Log.d(tag, "Model not found in allModels, checking keywords. Has reasoning keyword: $hasReasoningKeyword")
            hasReasoningKeyword
        } else {
            false
        }
        Log.d(tag, "Final supportsReasoning: $supportsReasoning")
        
        // Set chat template from model definition
        val chatTemplate = foundModel?.get("chatTemplate")
        if (chatTemplate != null) {
            Log.d(tag, "Setting chat template from model definition: $chatTemplate")
            modelChatFormat = chatTemplate
        } else {
            Log.d(tag, "No chat template found in model definition, using default: $modelChatFormat")
        }
        
        Log.d(tag, "=== END REASONING DEBUG ===")
        
        // Prefer Vulkan, then OpenCL, else CPU
        val backend = when {
            availableBackends.contains("Vulkan") -> "vulkan"
            availableBackends.contains("OpenCL") -> "opencl"
            else -> "cpu"
        }
        load(modelPath, modelThreadCount, backend = backend)
    }
    
    /**
     * Load a model by name from the external files directory
     */
    fun loadModelByName(modelName: String, directory: File): Boolean {
        Log.d(tag, "Loading model by name: $modelName from directory: ${directory.absolutePath}")
        // Hint native where to scan for ggml backends (directory that contains libggml-*.so)
        try {
            val nativeLibDir = getApplication<Application>().applicationInfo.nativeLibraryDir
            LLamaAndroid.instance().setBackendSearchDir(nativeLibDir)
            Log.d(tag, "Set backend search dir to $nativeLibDir")
        } catch (e: Exception) {
            Log.w(tag, "Failed to set backend search dir: ${e.message}")
        }
        Log.d(tag, "=== REASONING SUPPORT DEBUG ===")
        Log.d(tag, "All models count: ${allModels.size}")
        Log.d(tag, "All model names: ${allModels.map { it["name"] }}")

        val foundModel = allModels.find { it["name"] == modelName }
        Log.d(tag, "Found model in allModels: $foundModel")

        val reasoningSupport = foundModel?.get("supportsReasoning")
        Log.d(tag, "Raw reasoning support value: $reasoningSupport")

        // Fallback logic: if model not found, check if it's a known reasoning model
        supportsReasoning = if (reasoningSupport == "true") {
            true
        } else if (foundModel == null) {
            val reasoningKeywords = listOf("reasoning", "think", "qwen", "nemotron", "openreasoning")
            val hasReasoningKeyword = reasoningKeywords.any { keyword ->
                modelName.lowercase().contains(keyword.lowercase())
            }
            Log.d(tag, "Model not found in allModels, checking keywords. Has reasoning keyword: $hasReasoningKeyword")
            hasReasoningKeyword
        } else {
            false
        }
        Log.d(tag, "Final supportsReasoning: $supportsReasoning")

        // Set chat template from model definition
        val chatTemplate = foundModel?.get("chatTemplate")
        if (chatTemplate != null) {
            Log.d(tag, "Setting chat template from model definition: $chatTemplate")
            modelChatFormat = chatTemplate
        } else {
            Log.d(tag, "No chat template found in model definition, using default: $modelChatFormat")
        }

        Log.d(tag, "=== END REASONING DEBUG ===")

        // Remove forced reasoning; trust metadata / keyword fallback only

        return try {
            val model = allModels.find { it["name"] == modelName }
            if (model != null) {
                val destinationPath = File(directory, model["destination"].toString())
                if (destinationPath.exists()) {
                    // Use the currently selected backend (support Vulkan/OpenCL/CPU)
                    val backend = when {
                        currentBackend.equals("Vulkan", true) -> "vulkan"
                        currentBackend.equals("OpenCL", true) -> "opencl"
                        else -> "cpu"
                    }
                    load(destinationPath.path, userThreads = modelThreadCount, backend = backend)
                    true
                } else {
                    val msg = "Model file not found: ${destinationPath.path}"
                    Log.e(tag, msg)
                    addMessage("error", msg)
                    false
                }
            } else {
                val msg = "Model not found: $modelName"
                Log.e(tag, msg)
                addMessage("error", msg)
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading model by name: ${e.message}", e)
            addMessage("error", "Failed to load model: ${e.message}")
            false
        }
    }

    // Set the current backend (CPU or GPU)
    fun selectBackend(backend: String) {
        viewModelScope.launch {
            try {
                val success = llamaAndroid.setBackend(backend.lowercase())
                if (success) {
                    currentBackend = backend
                    backendError = null
                    Log.d(tag, "Backend changed to: $backend")

                    // If a model is already loaded, recreate with the new backend
                    if (llamaAndroid.getModel() != 0L) {
                        Log.d(tag, "Recreating model/context for backend=$backend")
                        try { llamaAndroid.unload() } catch (_: Exception) {}
                        // Ensure backend search dir remains set
                        try {
                            val nativeLibDir = getApplication<Application>().applicationInfo.nativeLibraryDir
                            LLamaAndroid.instance().setBackendSearchDir(nativeLibDir)
                        } catch (_: Exception) {}
                        // Reload previously selected model name if any
                        val name = loadedModelName.value
                        if (name.isNotBlank()) {
                            val extDir = getApplication<Application>().getExternalFilesDir(null) ?: return@launch
                            loadModelByName(name, extDir)
                        }
                    }
                } else {
                    backendError = "Failed to switch backend to $backend"
                    currentBackend = "CPU"
                    Log.e(tag, "Backend switch failed, reverted to CPU")
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to set backend to $backend: ${e.message}")
                backendError = "Failed to switch backend to $backend: ${e.message}"
                currentBackend = "CPU"
                try {
                    llamaAndroid.setBackend("cpu")
                } catch (_: Exception) {}
            }
        }
    }

    // Hardware acceleration detection - Called manually from settings
    fun detectHardwareCapabilities() {
        viewModelScope.launch {
            try {
                Log.d(tag, "Starting hardware detection...")

                // Force initialization of native library by using the dispatcher
                try {
                    Log.d(tag, "Forcing native library initialization...")
                    withContext(llamaAndroid.runLoop) {
                        Log.d(tag, "Native library initialization triggered")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error during native library initialization: ${e.message}")
                }

                // Check if native library is loaded before calling native functions
                if (!llamaAndroid.isNativeLibraryLoaded()) {
                    Log.w(tag, "Native library not loaded, attempting synchronous load...")
                    if (!llamaAndroid.ensureLibraryLoaded()) {
                        Log.w(tag, "Synchronous library load failed, using CPU-only mode")
                        availableBackends = "CPU"
                        currentBackend = "CPU"
                        optimalBackend = "CPU"
                        gpuInfo = "CPU Only - Native library not loaded"
                        isAdrenoGpu = false
                        backendError = "Native library not available"
                        return@launch
                    }
                }

                // Try to detect real hardware capabilities
                val backends = llamaAndroid.getAvailableBackends().split(",").map { it.trim() }
                availableBackends = backends.joinToString(",")
                optimalBackend = llamaAndroid.getOptimalBackend()
                gpuInfo = llamaAndroid.getGpuInfo()
                isAdrenoGpu = llamaAndroid.isAdrenoGpu()
                currentBackend = optimalBackend
                backendError = null

                Log.d(tag, "Hardware detection: Available backends: $availableBackends")
                Log.d(tag, "Hardware detection: Optimal backend: $optimalBackend")
                Log.d(tag, "Hardware detection: GPU info: $gpuInfo")
                Log.d(tag, "Hardware detection: Is Adreno GPU: $isAdrenoGpu")
            } catch (e: Exception) {
                Log.e(tag, "Error detecting hardware capabilities: ${e.message}")
                // Fallback to CPU-only
                availableBackends = "CPU"
                currentBackend = "CPU"
                optimalBackend = "CPU"
                gpuInfo = "CPU Only - Detection failed"
                isAdrenoGpu = false
                backendError = "Hardware detection failed: ${e.message}"
            }
        }
    }
    private fun addMessage(role: String, content: String) {
        val newMessage = mapOf("role" to role, "content" to content)

        messages = if (messages.isNotEmpty() && messages.last()["role"] == role) {
            val lastMessageContent = messages.last()["content"] ?: ""
            val updatedContent = "$lastMessageContent$content"
            val updatedLastMessage = messages.last() + ("content" to updatedContent)
            messages.toMutableList().apply {
                set(messages.lastIndex, updatedLastMessage)
            }
        } else {
            messages + listOf(newMessage)
        }
    }

    private fun trimEOT() {
        if (messages.isEmpty()) return
        val lastMessageContent = messages.last()["content"] ?: ""
        // Only slice if the content is longer than the EOT string
        if (lastMessageContent.length < eot_str.length) return

        val updatedContent = lastMessageContent.slice(0..(lastMessageContent.length-eot_str.length))
        val updatedLastMessage = messages.last() + ("content" to updatedContent)
        messages = messages.toMutableList().apply {
            set(messages.lastIndex, updatedLastMessage)
        }
        messages.last()["content"]?.let { Log.e(tag, it) }
    }

    private fun removeExtraWhiteSpaces(input: String): String {
        // Replace multiple white spaces with a single space
        return input.replace("\\s+".toRegex(), " ")
    }

    fun persistChat() {
        val title = messages.firstOrNull { it["role"] == "user" }?.get("content")?.take(64) ?: "Chat"
        val baseChat = currentChat?.copy(title = title, updated = System.currentTimeMillis())
            ?: Chat(title = title)

        viewModelScope.launch(Dispatchers.IO) {
            val id = chatRepository.saveChatWithMessages(
                baseChat,
                messages.mapIndexed { idx, m ->
                    Message(
                        chatId = baseChat.id,
                        role = m["role"] ?: "assistant",
                        content = m["content"] ?: "",
                        index = idx
                    )
                }
            )
            if (currentChat == null || currentChat?.id != id) {
                currentChat = baseChat.copy(id = id)
            }
        }
    }

    private fun parseTemplateJson(chatData: List<Map<String, String>> ):String{
        var chatStr = ""
        for (data in chatData){
            val role = data["role"]
            val content = data["content"]
            if (role != "log"){
                chatStr += "$role \n$content \n"
            }

        }
        return chatStr
    }
    fun updateMessage(newMessage: String) {
        message = newMessage
    }

    fun clear() {
        messages = listOf(

        )
        first = true
    }

    fun log(message: String) {
//        addMessage("log", message)
    }

    fun getIsSending(): Boolean {
        return llamaAndroid.getIsSending()
    }

    private fun getIsMarked(): Boolean {
        return llamaAndroid.getIsMarked()
    }

    fun getIsCompleteEOT(): Boolean{
        return llamaAndroid.getIsCompleteEOT()
    }

    fun stop() {
        llamaAndroid.stopTextGeneration()
    }

    // Add missing methods for compilation fixes
    fun searchModels(query: String): SearchResponse {
        // This is now a synchronous wrapper for the async search
        // The actual search should be called from a coroutine scope
        return SearchResponse(
            success = false,
            data = null,
            error = "Use searchModelsAsync() for proper async search"
        )
    }

    suspend fun searchModelsAsync(query: String): SearchResponse {
        return try {
            val token = userPreferencesRepository.getHuggingFaceToken()
            val authHeader = if (token.isNotEmpty()) "Bearer $token" else null
            
            val models = huggingFaceApiService.searchModels(query, authHeader)
            
            val searchResults = models.map { model ->
                ModelSearchResult(
                    id = model.id,
                    name = model.name,
                    description = model.tags.joinToString(", "),
                    downloads = model.downloads,
                    likes = model.likes,
                    tags = model.tags
                )
            }
            
            SearchResponse(
                success = true,
                data = searchResults,
                error = null
            )
        } catch (e: Exception) {
            Log.e(tag, "Error searching models", e)
            SearchResponse(
                success = false,
                data = null,
                error = "Search failed: ${e.message}"
            )
        }
    }

    fun getModelDetails(modelId: String): ModelDetailsResponse {
        // This is now a synchronous wrapper for the async search
        return ModelDetailsResponse(
            success = false,
            data = null,
            error = "Use getModelDetailsAsync() for proper async search"
        )
    }

    suspend fun getModelDetailsAsync(modelId: String): ModelDetailsResponse {
        return try {
            val token = userPreferencesRepository.getHuggingFaceToken()
            val authHeader = if (token.isNotEmpty()) "Bearer $token" else null
            
            val model = huggingFaceApiService.getModelDetails(modelId, authHeader)
            
            val detailResult = ModelDetailResult(
                id = model.id,
                name = model.name,
                description = model.tags.joinToString(", "),
                downloads = model.downloads,
                likes = model.likes,
                tags = model.tags,
                siblings = model.siblings.map { file ->
                    ModelFile(
                        filename = file.filename,
                        size = file.size,
                        quantType = null // Not available in current API response
                    )
                }
            )
            
            ModelDetailsResponse(
                success = true,
                data = listOf(detailResult),
                error = null
            )
        } catch (e: Exception) {
            Log.e(tag, "Error getting model details", e)
            ModelDetailsResponse(
                success = false,
                data = null,
                error = "Failed to get model details: ${e.message}"
            )
        }
    }

    

    // Memory management functions
    fun unloadCurrentModel() {
        viewModelScope.launch {
            try {
                llamaAndroid.unload()
                loadedModelName.value = ""
                selectedModel = ""
                Log.d(tag, "Current model unloaded successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error unloading model", e)
            }
        }
    }
    
    suspend fun getMemoryUsage(): Long {
        return try {
            llamaAndroid.getMemoryUsage()
        } catch (e: Exception) {
            Log.e(tag, "Error getting memory usage", e)
            0L
        }
    }
    
    fun isModelLoaded(): Boolean {
        return loadedModelName.value.isNotEmpty()
    }
    
    fun getLoadedModelName(): String {
        return loadedModelName.value
    }
    
    // Memory optimization
    fun optimizeMemory() {
        viewModelScope.launch {
            try {
                // Clear message history if it's too large
                if (messages.size > 100) {
                    val keepCount = 50
                    messages = messages.takeLast(keepCount)
                    Log.d(tag, "Cleared message history, kept last $keepCount messages")
                }
                
                // Force garbage collection if memory usage is high
                val memoryUsage = getMemoryUsage()
                if (memoryUsage > 500 * 1024 * 1024) { // 500MB threshold
                    System.gc()
                    Log.d(tag, "Forced garbage collection due to high memory usage: ${memoryUsage / (1024 * 1024)}MB")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error optimizing memory", e)
            }
        }
    }

    // Search state management
    var isSearching by mutableStateOf(false)
    var searchResults by mutableStateOf<List<SearchResult>>(emptyList())
    var currentSearchQuery by mutableStateOf("")
    var searchError by mutableStateOf<String?>(null)
    var searchProgress by mutableStateOf("")

}

// Add data class for search response with proper structure
data class SearchResponse(
    val success: Boolean,
    val data: List<ModelSearchResult>?,
    val error: String?
)

data class ModelSearchResult(
    val id: String,
    val name: String,
    val description: String?,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>
)

data class ModelDetailsResponse(
    val success: Boolean,
    val data: List<ModelDetailResult>?,
    val error: String?
)

data class ModelDetailResult(
    val id: String,
    val name: String,
    val description: String?,
    val downloads: Int,
    val likes: Int,
    val tags: List<String>,
    val siblings: List<ModelFile>
)

data class ModelFile(
    val filename: String,
    val size: Long?,
    val quantType: String?
)

fun sentThreadsValue(){

}
