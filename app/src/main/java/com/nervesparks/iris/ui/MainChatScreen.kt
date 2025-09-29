package com.nervesparks.iris.ui

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Data class for chat messages with enhanced functionality
 */
data class ChatMessage(
    val id: String,
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.ExperimentalComposeUiApi
import com.nervesparks.iris.streaming.StreamingTokenManager
import com.nervesparks.iris.profiles.PerformanceProfile
import com.nervesparks.iris.profiles.PerformanceProfileManager
import com.nervesparks.iris.actions.MessageActionManager
import com.nervesparks.iris.performance.ThermalPerformanceManager
import com.nervesparks.iris.memory.LongTermMemoryManager
import com.nervesparks.iris.health.ModelHealthChecker
import com.nervesparks.iris.benchmark.BenchmarkOverlay
import com.nervesparks.iris.benchmark.rememberBenchmarkOverlayState
import com.nervesparks.iris.ui.sheets.ParametersBottomSheet
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.LinearGradient
import com.nervesparks.iris.MainViewModel

import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.ChatMessageList
import com.nervesparks.iris.ui.components.DownloadModal
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.features.SmartClipboardManager
import com.nervesparks.iris.features.DynamicThemeManager
import com.nervesparks.iris.features.QuickActionsManager
import com.nervesparks.iris.features.ConversationSearchManager
import com.nervesparks.iris.features.SPenManager
import com.nervesparks.iris.features.SPenDrawingCanvas

import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainChatScreen (
    onNextButtonClicked: () -> Unit,
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>,
    extFileDir: File?,
){
    val kc = LocalSoftwareKeyboardController.current
    val windowInsets = WindowInsets.ime
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize our quick win features
    val smartClipboardManager = remember { 
        SmartClipboardManager(context, clipboard, scope)
    }
    val dynamicThemeManager = remember { DynamicThemeManager() }
    val quickActionsManager = remember { QuickActionsManager() }
    val searchManager = remember { ConversationSearchManager() }
    val sPenManager = remember { SPenManager(context) }
    
    // === NEW ADVANCED SYSTEMS ===
    
    // Streaming token system for ChatGPT-like live typing
    val streamingTokenManager = remember { StreamingTokenManager() }
    val streamingTokens by streamingTokenManager.tokenFlow.collectAsState(initial = null)
    
    // Performance profiles (Speed/Balanced/Quality)
    val performanceProfileManager = remember { PerformanceProfileManager(context) }
    val currentProfile by performanceProfileManager.currentProfile.collectAsState(initial = PerformanceProfile.BALANCED)
    
    // Message actions (Copy/Regenerate/Share/Pin)
    val messageActionManager = remember { MessageActionManager(context) }
    
    // Thermal performance management
    val thermalManager = remember { 
        ThermalPerformanceManager(context, context as Activity)
    }
    val thermalState by thermalManager.thermalState.collectAsState()
    
    // Long-term memory system
    val memoryManager = remember { LongTermMemoryManager(context) }
    
    // Model health checker
    val modelHealthChecker = remember { ModelHealthChecker(context) }
    
    // UI State
    val showBenchmark = rememberBenchmarkOverlayState()
    var showParametersSheet by rememberSaveable { mutableStateOf(false) }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    
    // 120Hz frame rate optimization
    LaunchedEffect(Unit) {
        try {
            (context as? Activity)?.window?.attributes?.preferredRefreshRate = 120f
        } catch (e: Exception) {
            // Ignore if not available
        }
    }
    
    // Start advanced systems
    LaunchedEffect(Unit) {
        thermalManager.startThermalMonitoring()
        thermalManager.enableSustainedPerformance()
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            thermalManager.stopThermalMonitoring()
            thermalManager.disableSustainedPerformance()
            streamingTokenManager.stopStreaming()
        }
    }
    
    // Handle streaming tokens
    LaunchedEffect(streamingTokens) {
        streamingTokens?.let { token ->
            if (token.isComplete) {
                // Store in long-term memory
                memoryManager.storeConversation(
                    messageId = token.messageId,
                    content = viewModel.messages.lastOrNull()?.message ?: "",
                    isFromUser = false,
                    sessionId = "session_${System.currentTimeMillis() / (1000 * 60 * 60)}"
                )
            } else if (token.token.isNotEmpty()) {
                // Update streaming display (placeholder - implement in viewModel)
                // viewModel.updateStreamingMessage(token.token)
                thermalManager.updateTokenRate(token.tokensPerSecond)
            }
        }
    }
    
    // Auto-adjust performance based on thermal state
    LaunchedEffect(thermalState) {
        if (thermalState.isThrottling && thermalState.temperature > 45f) {
            // Switch to power-save profile when overheating
            performanceProfileManager.setProfile(PerformanceProfile.SPEED)
        }
    }
    val conversationSearchManager = remember { ConversationSearchManager(scope) }
    val sPenManager = remember { SPenManager(context, scope) }
    
    // Feature states
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSPenCanvas by remember { mutableStateOf(false) }
    var showClipboardHelper by remember { mutableStateOf(false) }
    
    // Check clipboard periodically
    LaunchedEffect(Unit) {
        while (true) {
            smartClipboardManager.checkClipboard()
            if (smartClipboardManager.showClipboardHelper && !showClipboardHelper) {
                showClipboardHelper = true
            }
            kotlinx.coroutines.delay(2000) // Check every 2 seconds
        }
    }
    
    // Update theme based on conversation
    LaunchedEffect(viewModel.messages) {
        if (viewModel.messages.isNotEmpty()) {
            val lastMessage = viewModel.messages.lastOrNull()?.get("content") ?: ""
            val history = viewModel.messages.takeLast(5).mapNotNull { it["content"] }
            dynamicThemeManager.analyzeAndUpdateTheme(lastMessage, history)
        }
    }

    println("Thread started: ${Thread.currentThread().name}")
    val Prompts = listOf(
        "Explain how to develop a consistent reading habit.",
        "Write an email to your manager requesting leave for a day.",
        "Suggest time management strategies for handling multiple deadlines effectively.",
        "Draft a professional LinkedIn message to connect with a recruiter.",
        "Suggest ways to practice mindfulness in a busy daily schedule.",
        "Recommend a 15-minute daily workout routine to stay fit with a busy schedule.",
        "Provide a simple and polite Spanish translation of 'Excuse me, can you help me?' with explanation.",
        "List the top 5 science fiction novels that have most influenced modern technological thinking",
        "List security tips to protect personal information online.",
        "Recommend three books that can improve communication skills."
    )

    val allModelsExist = models.all { model -> model.destination.exists() }
    val Prompts_Home = listOf(
        "Explains complex topics simply.",
        "Remembers previous inputs.",
        "May sometimes be inaccurate.",
        "Unable to provide current affairs due to no internet connectivity."
    )
    var recognizedText by remember { mutableStateOf("") }
    val speechRecognizerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        recognizedText = results?.get(0)?:""
        viewModel.updateMessage(recognizedText)

    }





    val focusRequester = FocusRequester()
    var isFocused by remember { mutableStateOf(false) }
    var textFieldBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    if (allModelsExist) {
        viewModel.showModal = false
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LinearGradient()
        
        // === BENCHMARK OVERLAY ===
        BenchmarkOverlay(
            thermalManager = thermalManager,
            streamingManager = streamingTokenManager,
            isVisible = showBenchmark.value
        )

        // === PARAMETERS BOTTOM SHEET ===
        ParametersBottomSheet(
            isVisible = showParametersSheet,
            onDismiss = { showParametersSheet = false },
            profileManager = performanceProfileManager,
            onParametersChanged = { profile, customParams ->
                // Apply performance settings
                scope.launch {
                    performanceProfileManager.setProfile(profile)
                    customParams?.let {
                        performanceProfileManager.setCustomParameters(
                            it.temperature, it.topP, it.threads, it.contextLength
                        )
                    }
                    // Update viewModel with new settings (placeholder)
                    // viewModel.updatePerformanceSettings(profile)
                }
            }
        )



            // Screen content
            Column() {


                // Show modal if required
                if (viewModel.showModal) {
                    // Modal dialog to show download options
                    DownloadModal(viewModel = viewModel, dm = dm, models = models)

                }

                if (viewModel.showAlert) {
                    // Modal dialog to show download options
                    LoadingModal(viewModel)
                }

                // Smart Clipboard Helper Dialog
                if (clipboardState.showDialog) {
                    SmartClipboardDialog(
                        clipboardText = clipboardState.clipboardText,
                        suggestions = clipboardState.suggestions,
                        onSuggestionSelected = { suggestion ->
                            viewModel.updateMessage(suggestion)
                            smartClipboardManager.clearClipboard()
                        },
                        onDismiss = {
                            smartClipboardManager.clearClipboard()
                        }
                    )
                }

                // Conversation Search Dialog
                if (searchState.isSearching) {
                    ConversationSearchDialog(
                        messages = viewModel.messages,
                        searchResults = searchState.searchResults,
                        bookmarks = searchState.bookmarks,
                        onSearch = { query ->
                            searchManager.searchMessages(query, viewModel.messages)
                        },
                        onBookmark = { messageId ->
                            searchManager.bookmarkMessage(messageId)
                        },
                        onJumpToMessage = { messageId ->
                            // Scroll to message in chat
                            // Implementation would need scroll state management
                        },
                        onDismiss = {
                            searchManager.stopSearch()
                        }
                    )
                }

                Column {

                    // Quick Actions Overlay
                    quickActionsState?.let { actions ->
                        QuickActionsOverlay(
                            actions = actions,
                            onActionSelected = { action ->
                                when (action) {
                                    "Copy Last Response" -> {
                                        viewModel.messages.lastOrNull { !it.isFromUser }?.let { lastBot ->
                                            smartClipboardManager.copyToClipboard(lastBot.message)
                                        }
                                    }
                                    "Clear Chat" -> {
                                        viewModel.clearMessages()
                                    }
                                    "Voice Input" -> {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                        }
                                        speechRecognizerLauncher.launch(intent)
                                    }
                                }
                            },
                            onDismiss = {
                                quickActionsManager.hideActions()
                            }
                        )
                    }

                    // S Pen Drawing Canvas
                    if (sPenState.showDrawingMode) {
                        SPenDrawingCanvas(
                            onHandwritingRecognized = { text ->
                                viewModel.updateMessage(text)
                                sPenManager.exitDrawingMode()
                            },
                            onCancel = {
                                sPenManager.exitDrawingMode()
                            }
                        )
                    }

                    val scrollState = rememberLazyListState()


                    Box(modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    kc?.hide()
                                },
                                onDoubleTap = { kc?.hide() },
                                onLongPress = { kc?.hide() },
                                onPress = { kc?.hide() },
                            )
                        }
                        .pointerInput(Unit) {
                            // Quick Actions Gesture Detection
                            detectDragGestures(
                                onDragStart = { offset ->
                                    quickActionsManager.handleGestureStart(offset)
                                },
                                onDrag = { change, _ ->
                                    quickActionsManager.handleGestureMove(change.position)
                                },
                                onDragEnd = {
                                    quickActionsManager.handleGestureEnd()
                                }
                            )
                        }) {

                        if (viewModel.messages.isEmpty() && !viewModel.showModal && !viewModel.showAlert) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize() // Take up the whole screen
                                    .wrapContentHeight(Alignment.CenterVertically),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
//                                item { Spacer(Modifier.height(55.dp).fillMaxWidth()) }
                                // Header Text
                                item {
                                    Text(
                                        text = "Hello, Ask me " + "Anything",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.W300,
                                            letterSpacing = 1.sp,
                                            fontSize = 50.sp,
                                            lineHeight = 60.sp
                                        ),
                                        fontFamily = FontFamily.SansSerif,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .wrapContentHeight()
                                    )
                                }

                                // Items for Prompts_Home
                                items(Prompts_Home.size) { index ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp)
                                            .padding(8.dp)
                                            .background(
                                                Color(0xFF010825),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            // Circle Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp) // Icon size
                                                    .background(Color.White, shape = CircleShape)
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.info_svgrepo_com),
                                                    contentDescription = null,
                                                    tint = Color.Black
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Text
                                            Text(
                                                text = Prompts_Home.getOrNull(index) ?: "",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                                textAlign = TextAlign.Start, // Left align the text
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }

                                item{

                                }
                            }
                        }
                        else {

                            LazyColumn(state = scrollState) {
                                // Track the first user and assistant messages

                                var length = viewModel.messages.size

                                itemsIndexed(viewModel.messages.slice(3..< length) as? List<Map<String, String>> ?: emptyList()) { index, messageMap ->
                                    val role = messageMap["role"] ?: ""
                                    val content = messageMap["content"] ?: ""
                                    val trimmedMessage = if (content.endsWith("\n")) {
                                        content.substring(startIndex = 0, endIndex = content.length - 1)
                                    } else {
                                        content
                                    }

                                    // Skip rendering first user and first assistant messages

                                    if (role != "system") {
                                        if (role != "codeBlock") {
                                            Box {
                                                val context = LocalContext.current
                                                val interactionSource = remember { MutableInteractionSource() }
                                                val sheetState = rememberModalBottomSheetState()
                                                var isSheetOpen by rememberSaveable {
                                                    mutableStateOf(false)
                                                }
                                                if(isSheetOpen){
                                                    MessageBottomSheet(
                                                        message = trimmedMessage,
                                                        clipboard = clipboard,
                                                        context = context,
                                                        viewModel = viewModel,
                                                        onDismiss = {
                                                            isSheetOpen = false
                                                            viewModel.toggler = false
                                                        },
                                                        sheetState = sheetState
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            start = 8.dp,
                                                            top = 8.dp,
                                                            end = 8.dp,
                                                            bottom = 0.dp
                                                        ),
                                                ) {
                                                    if(role == "assistant") {
                                                        Image(
                                                            painter = painterResource(
                                                                id = R.drawable.logo
                                                            ),
                                                            contentDescription =  "Bot Icon",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Box(modifier = Modifier
                                                        .padding(horizontal = 2.dp)
                                                        .background(
                                                            color = if (role == "user") Color(
                                                                0xFF171E2C
                                                            ) else Color.Transparent,
                                                            shape = RoundedCornerShape(12.dp),
                                                        )
                                                        .combinedClickable(
                                                            interactionSource = interactionSource,
                                                            indication = ripple(color = Color.Gray),
                                                            onLongClick = {
                                                                if (viewModel.getIsSending()) {
                                                                    Toast
                                                                        .makeText(
                                                                            context,
                                                                            " Wait till generation is done! ",
                                                                            Toast.LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                                } else {
                                                                    isSheetOpen = true
                                                                }
                                                            },
                                                            onClick = {
                                                                kc?.hide()
                                                            }
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .padding(5.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .widthIn(max = 300.dp)
                                                                    .padding(3.dp)
                                                            ){
                                                                Text(
                                                                    text = if (trimmedMessage.startsWith("```")) {
                                                                        trimmedMessage.substring(3)
                                                                    } else {
                                                                        trimmedMessage
                                                                    },
                                                                    style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFA0A0A5)),
                                                                    modifier = Modifier
                                                                        .padding(start = 1.dp, end = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if(role == "user") {
                                                        Image(
                                                            painter = painterResource(
                                                                id = R.drawable.user_icon
                                                            ),
                                                            contentDescription = "Human Icon",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            val context = LocalContext.current
                                            val interactionSource = remember { MutableInteractionSource() }
                                            val sheetState = rememberModalBottomSheetState()
                                            var isSheetOpen by rememberSaveable {
                                                mutableStateOf(false)
                                            }
                                            // Code block rendering remains the same
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    .background(
                                                        Color.Black,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .fillMaxWidth()
                                            ) {
                                                if(isSheetOpen){
                                                    MessageBottomSheet(
                                                        message = trimmedMessage,
                                                        clipboard = clipboard,
                                                        context = context,
                                                        viewModel = viewModel,
                                                        onDismiss = {
                                                            isSheetOpen = false
                                                            viewModel.toggler = false
                                                        },
                                                        sheetState = sheetState
                                                    )
                                                }
                                                Column(modifier = Modifier.combinedClickable(
                                                        interactionSource = interactionSource,
                                                indication = ripple(color = Color.LightGray),
                                                onLongClick = {
                                                    if (viewModel.getIsSending()) {
                                                        Toast
                                                            .makeText(
                                                                context,
                                                                " Wait till generation is done! ",
                                                                Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                    } else {
                                                        isSheetOpen = true
                                                    }
                                                },
                                                onClick = {
                                                    kc?.hide()
                                                }
                                                )) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.End,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                top = 8.dp,
                                                                bottom = 8.dp,
                                                                start = 6.dp,
                                                                end = 6.dp
                                                            )
                                                    ) {
                                                        // Previous content here
                                                    }
                                                    Text(
                                                        text = if (trimmedMessage.startsWith("```")) {
                                                            trimmedMessage.substring(3)
                                                        } else {
                                                            trimmedMessage
                                                        },
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            color = Color(0xFFA0A0A5)
                                                        ),
                                                        modifier = Modifier.padding(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier
                                        .height(1.dp)
                                        .fillMaxWidth())
                                }
                            }

                            ScrollToBottomButton(
                                scrollState = scrollState,
                                messages = viewModel.messages,
                                viewModel = viewModel
                            )

                        }

                        //chat section ends here
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced space between cards
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(Prompts.size) { index ->
                            if(viewModel.messages.size <= 1){
                                Card(
                                    modifier = Modifier
                                        .height(100.dp)
                                        .clickable {
                                            viewModel.updateMessage(Prompts[index])
                                            focusRequester.requestFocus()
                                        }
                                        .padding(horizontal = 8.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF030815))
                                ) {

                                    Text(
                                        text = Prompts[index],
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFA0A0A5),
                                            fontSize = 12.sp,
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .width(200.dp)
                                            .height(100.dp)
                                            .padding(horizontal = 15.dp, vertical = 12.dp)
//                                                .align(Alignment.Center)
                                    )

                                }
                            }
                        }
                    }
                    //Prompt input field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF050B16))


                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 5.dp, top = 8.dp, bottom = 12.dp, end = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,

                            ) {


                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                    )
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
                                }
                                speechRecognizerLauncher.launch(intent)
                                focusManager.clearFocus()

                            }) {
                                Icon(
//                                imageVector = Icons.Default.Send,
                                    modifier = Modifier
                                        .size(25.dp)
                                        .weight(1f),
                                    painter = painterResource(id = R.drawable.microphone_new_svgrepo_com),
                                    contentDescription = "Mic",
                                    tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                                )
                            }

                            // Search Button
                            IconButton(onClick = {
                                searchManager.startSearch()
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    modifier = Modifier
                                        .size(25.dp)
                                        .weight(1f),
                                    painter = painterResource(id = R.drawable.search_svgrepo_com__3_),
                                    contentDescription = "Search",
                                    tint = Color(0xFFDDDDE4)
                                )
                            }

                            // S Pen Button (only show on Samsung devices)
                            if (sPenState.isSupported) {
                                IconButton(onClick = {
                                    sPenManager.enterDrawingMode()
                                    focusManager.clearFocus()
                                }) {
                                    Icon(
                                        modifier = Modifier
                                            .size(25.dp)
                                            .weight(1f),
                                        painter = painterResource(id = R.drawable.edit_3_svgrepo_com),
                                        contentDescription = "S Pen",
                                        tint = if (sPenState.showDrawingMode) Color(0xFF4CAF50) else Color(0xFFDDDDE4)
                                    )
                                }
                            }

                            // Performance Parameters Button
                            IconButton(onClick = {
                                showParametersSheet = true
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    modifier = Modifier
                                        .size(25.dp)
                                        .weight(1f),
                                    painter = painterResource(id = R.drawable.setting_4_svgrepo_com),
                                    contentDescription = "Performance Settings",
                                    tint = when (currentProfile) {
                                        PerformanceProfile.SPEED -> Color(0xFFFF6B6B)     // Red for Speed
                                        PerformanceProfile.BALANCED -> Color(0xFF4ECDC4)  // Teal for Balanced  
                                        PerformanceProfile.QUALITY -> Color(0xFF45B7D1)   // Blue for Quality
                                    }
                                )
                            }

                            // Benchmark Toggle Button
                            IconButton(onClick = {
                                showBenchmark.value = !showBenchmark.value
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    modifier = Modifier
                                        .size(25.dp)
                                        .weight(1f),
                                    painter = painterResource(id = R.drawable.bench_mark_icon),
                                    contentDescription = if (showBenchmark.value) "Hide Benchmark" else "Show Benchmark",
                                    tint = if (showBenchmark.value) Color(0xFF4ECDC4) else Color(0xFFDDDDE4)
                                )
                            }



                            val dragSelection = remember { mutableStateOf<TextRange?>(null) }
                            val lastKnownText = remember { mutableStateOf(viewModel.message) }

                            val textFieldValue = remember {
                                mutableStateOf(
                                    TextFieldValue(
                                        text = viewModel.message,
                                        selection = TextRange(viewModel.message.length) // Ensure cursor starts at the end
                                    )
                                )
                            }

                            TextField(

                                value = textFieldValue.value.copy(
                                    text = viewModel.message,
                                    selection = when {
                                        viewModel.message != lastKnownText.value -> {
                                            // If the message has changed programmatically,
                                            // preserve the current cursor/selection position
                                            textFieldValue.value.selection
                                        }
                                        else -> {
                                            // Otherwise, use the drag selection or current selection
                                            dragSelection.value ?: textFieldValue.value.selection
                                        }
                                    }
                                ),
                                onValueChange = { newValue ->
                                    // Update drag selection when the user drags or selects
                                    dragSelection.value = if (newValue.text == textFieldValue.value.text) {
                                        newValue.selection
                                    } else {
                                        null // Reset drag selection if the text changes programmatically
                                    }


                                    // Update the local state
                                    textFieldValue.value = newValue

                                    // Save the last known text and update ViewModel
                                    lastKnownText.value = newValue.text
                                    viewModel.updateMessage(newValue.text)
                                },
                                placeholder = { Text("Message") },
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coordinates ->
                                        textFieldBounds = coordinates.boundsInRoot()
                                    }
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused
                                    },
                                shape = RoundedCornerShape(size = 18.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFFBECBD1),
                                    unfocusedTextColor = Color(0xFFBECBD1),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent, // Optional, makes the indicator disappear
                                    focusedLabelColor = Color(0xFF626568),
                                    cursorColor = Color(0xFF626568),
                                    unfocusedContainerColor = Color(0xFF171E2C),
                                    focusedContainerColor = Color(0xFF22314A)
                                )
                            )




                            if (!viewModel.getIsSending()) {
                                val context = LocalContext.current

                                IconButton(onClick = {
                                    if(viewModel.loadedModelName.value == ""){
                                        focusManager.clearFocus()
                                        Toast.makeText(context, "Load A Model First", Toast.LENGTH_SHORT).show()
                                    }
                                    else {
                                        viewModel.send()
                                        focusManager.clearFocus()
                                    }
                                }
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .weight(1f),
                                        painter = painterResource(id = R.drawable.send_2_svgrepo_com),
                                        contentDescription = "Send",
                                        tint = Color(0xFFDDDDE4)
                                    )
                                }
                            } else if (viewModel.getIsSending()) {
                                IconButton(onClick = {
                                    viewModel.stop() }) {
                                    Icon(
                                        modifier = Modifier
                                            .weight(1f)
                                            .size(28.dp),
                                        painter = painterResource(id = R.drawable.square_svgrepo_com),
                                        contentDescription = "Stop",
                                        tint = Color(0xFFDDDDE4)
                                    )
                                }
                            }

                        }
                    }


                }
            }

    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScrollState = rememberLazyListState()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF01081a),
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ){
            Text(
                text = "Settings",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            LazyColumn(state = sheetScrollState) {
                item{
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF14161f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .border(
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color.LightGray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Select thread for process, 0 for default",
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "${viewModel.user_thread.toInt()}",
                                color = Color.White
                            )
                            Slider(
                                value = viewModel.user_thread,
                                onValueChange = {

                                    viewModel.user_thread = it
                                },
                                valueRange = 0f..8f,
                                steps = 7,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6200EE),
                                    activeTrackColor = Color(0xFF6200EE),
                                    inactiveTrackColor = Color.Gray
                                ),
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                text = "After changing thread please Save the changes!!",
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth(),

                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.DarkGray.copy(alpha = 1.0f), // Set the containerColor to transparent
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(8.dp), // Slightly more rounded corners
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 3.dp
                                ),
                                onClick = {
                                    viewModel.currentDownloadable?.destination?.path?.let {
                                        viewModel.load(
                                            it, viewModel.user_thread.toInt())
                                    }
                                }
                            ) {

                                Text("Save")
                            }
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun ScrollToBottomButton(
    viewModel: MainViewModel,
    scrollState: LazyListState,
    messages: List<Any>
) {
    val coroutineScope = rememberCoroutineScope()

    // State to track if auto-scrolling is enabled
    var isAutoScrolling by remember { mutableStateOf(false) }

    // State to control the button's visibility
    var isButtonVisible by remember { mutableStateOf(true) }

    // Determine if the user can scroll down
    val canScrollDown by remember {
        derivedStateOf { scrollState.canScrollForward }
    }

    // Continuously scroll to the bottom while auto-scrolling is enabled
    LaunchedEffect(viewModel.messages.size, isAutoScrolling) {
        if (isAutoScrolling) {
            coroutineScope.launch {
                scrollState.scrollToItem(viewModel.messages.size + 1)
            }
        }
    }

    // Stop auto-scrolling when the user scrolls manually
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isAutoScrolling = false
            isButtonVisible = true // Show the button again if the user scrolls manually
        }
    }

    // Continuously monitor changes in the last item's content
    LaunchedEffect(messages.lastOrNull()) {
        if (isAutoScrolling && messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.scrollToItem(viewModel.messages.size + 1)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = (canScrollDown || isAutoScrolling) && isButtonVisible, // Show button if needed
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FloatingActionButton(
                onClick = {
                    isAutoScrolling = true // Enable auto-scrolling
                    isButtonVisible = false // Hide the button on click
                    coroutineScope.launch {
                        scrollState.scrollToItem(viewModel.messages.size + 1)
                    }
                },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(56.dp),
                // Ensures a circular shape
                shape = RoundedCornerShape(percent = 50),
                containerColor = Color.White.copy(alpha = 0.5f),
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    tint = Color.White // White icon for better visibility
                )
            }
        }
    }
}


@Composable
fun ModelSelectorWithDownloadModal(
    viewModel: MainViewModel,
    downloadManager: DownloadManager,
    extFileDir: File?
) {


    val context = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()

    var mExpanded by remember { mutableStateOf(false) }
    var mSelectedText by remember { mutableStateOf("") }
    var mTextFieldSize by remember { mutableStateOf(Size.Zero) }
    var selectedModel by remember { mutableStateOf<Map<String, Any>?>(null) }

    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown

    // Search for local .gguf models
    val localModels = remember(extFileDir) {
        extFileDir?.listFiles { _, name -> name.endsWith(".gguf") }
            ?.map { file ->
                mapOf(
                    "name" to file.nameWithoutExtension,
                    "source" to file.toURI().toString(),
                    "destination" to file.name
                )
            } ?: emptyList()
    }
    // Combine local and remote models, ensuring uniqueness
    val combinedModels = remember(viewModel.allModels, localModels) {
        (viewModel.allModels + localModels).distinctBy { it["name"] }
    }
    viewModel.allModels = combinedModels


    Column(Modifier.padding(20.dp)) {

        OutlinedTextField(
            value= viewModel.loadedModelName.value,
            onValueChange = { mSelectedText = it },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    mTextFieldSize = coordinates.size.toSize()
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            mExpanded = !mExpanded
                        },
                        onPress = {
                            mExpanded = !mExpanded
                        }
                    )
                }
                .clickable {
                    mExpanded = !mExpanded
                },
            label = { Text("Select Model") },
            trailingIcon = {
                Icon(
                    icon,
                    contentDescription = "Toggle dropdown",
                    Modifier.clickable { mExpanded = !mExpanded },
                    tint =Color(0xFFcfcfd1)
                )
            },
            textStyle = TextStyle(color = Color(0xFFf5f5f5)),
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF666666),
                focusedBorderColor = Color(0xFFcfcfd1),
                unfocusedLabelColor = Color(0xFF666666),
                focusedLabelColor = Color(0xFFcfcfd1),
                unfocusedTextColor = Color(0xFFf5f5f5),
                focusedTextColor = Color(0xFFf7f5f5),
            )
        )



        DropdownMenu(
            modifier = Modifier
                .background(Color(0xFF01081a))
                .width(with(LocalDensity.current) { mTextFieldSize.width.toDp() })
                .padding(top = 2.dp)
                .border(1.dp, color = Color.LightGray.copy(alpha = 0.5f)),
            expanded = mExpanded,
            onDismissRequest = {
                mExpanded = false
            }
        ) {
            viewModel.allModels.forEach { model ->
                DropdownMenuItem(
                    modifier = Modifier
                        .background(color = Color(0xFF090b1a))
                        .padding(horizontal = 1.dp, vertical = 0.dp),
                    onClick = {
                        mSelectedText = model["name"].toString()
                        selectedModel = model
                        mExpanded = false

                        // Convert model to Downloadable and show modal
                        val downloadable = Downloadable(
                            name = model["name"].toString(),
                            source = Uri.parse(model["source"].toString()),
                            destination = File(extFileDir, model["destination"].toString())
                        )

                        viewModel.showModal = true
                        viewModel.currentDownloadable = downloadable
                    }
                ) {
                    model["name"]?.let { Text(text = it, color = Color.White) }
                }
            }
        }

        // Use showModal instead of switchModal

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBottomSheet(
    message: String,
    clipboard: ClipboardManager,
    context: Context,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    // Enhanced message actions
    val messageActionManager = remember { MessageActionManager(context) }
    
    // Create a temporary ChatMessage for action handling
    val chatMessage = remember(message) {
        ChatMessage(
            id = "temp_${System.currentTimeMillis()}",
            message = message,
            isFromUser = false, // Assume it's an AI message for action purposes
            timestamp = System.currentTimeMillis()
        )
    }
    
    val isPinned = messageActionManager.isMessagePinned(chatMessage.id)

    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = Color(0xFF0D1117),
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .background(color = Color(0xFF0D1117))
        ) {
            var sheetScrollState = rememberLazyListState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                // Header
                Text(
                    text = "Message Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // === ENHANCED ACTIONS ===
                
                // Copy Text Button (Enhanced)
                MessageActionButton(
                    icon = "📋",
                    text = "Copy Message",
                    description = "Copy to clipboard",
                    onClick = {
                        messageActionManager.executeAction(
                            MessageAction.COPY, 
                            chatMessage
                        )
                        onDismiss()
                    }
                )

                // Regenerate Button (for AI messages)
                if (!chatMessage.isFromUser) {
                    MessageActionButton(
                        icon = "🔄",
                        text = "Regenerate Response",
                        description = "Generate a new response",
                        onClick = {
                            messageActionManager.executeAction(
                                MessageAction.REGENERATE,
                                chatMessage,
                                onRegenerate = { messageId ->
                                    // Trigger regeneration in viewModel (placeholder)
                                    // viewModel.regenerateLastResponse()
                                }
                            )
                            onDismiss()
                        }
                    )
                }

                // Share Button
                MessageActionButton(
                    icon = "🔗",
                    text = "Share Message",
                    description = "Share via other apps",
                    onClick = {
                        messageActionManager.executeAction(
                            MessageAction.SHARE,
                            chatMessage
                        )
                        onDismiss()
                    }
                )

                // Pin/Unpin Button
                MessageActionButton(
                    icon = if (isPinned) "📌" else "📌",
                    text = if (isPinned) "Unpin Message" else "Pin Message",
                    description = if (isPinned) "Remove from pinned" else "Save as important",
                    iconTint = if (isPinned) Color(0xFFFFD700) else Color(0xFFE6EDF3),
                    onClick = {
                        val action = if (isPinned) MessageAction.UNPIN else MessageAction.PIN
                        messageActionManager.executeAction(action, chatMessage)
                        onDismiss()
                    }
                )

                Divider(
                    color = Color(0xFF30363D),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // === ORIGINAL ACTIONS ===

                // Select Text Button
                MessageActionButton(
                    icon = "✂️",
                    text = "Select Text To Copy",
                    description = "Select portions to copy",
                    enabled = !viewModel.getIsSending(),
                    onClick = {
                        viewModel.toggler = !viewModel.toggler
                    }
                )

                // Text to Speech Button
                MessageActionButton(
                    icon = if (viewModel.stateForTextToSpeech) "🔊" else "🔇",
                    text = if (viewModel.stateForTextToSpeech) "Text To Speech" else "Stop TTS",
                    description = "Read message aloud",
                    enabled = !viewModel.getIsSending(),
                    onClick = {
                        if (viewModel.stateForTextToSpeech) {
                            viewModel.textForTextToSpeech = message
                            viewModel.textToSpeech(context)
                        } else {
                            viewModel.stopTextToSpeech()
                        }
                        onDismiss()
                    }
                )

                // Selection Container (for text selection)
                if (viewModel.toggler) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2128)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = message,
                                color = Color(0xFFE6EDF3),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionButton(
    icon: String,
    text: String,
    description: String,
    enabled: Boolean = true,
    iconTint: Color = Color(0xFFE6EDF3),
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF21262D) else Color(0xFF161B22)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium,
                color = iconTint,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (enabled) Color(0xFFE6EDF3) else Color(0xFF656D76),
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (enabled) Color(0xFF8B949E) else Color(0xFF656D76)
                    )
                )
            }
        }
    }
}

@Composable
fun SmartClipboardDialog(
    clipboardText: String,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E2329),
        title = {
            Text(
                text = "Smart Clipboard",
                color = Color(0xFFDDDDE4),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    text = "Clipboard content:",
                    color = Color(0xFF9CA3AF),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = clipboardText.take(100) + if (clipboardText.length > 100) "..." else "",
                    color = Color(0xFFBECBD1),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "AI Suggestions:",
                        color = Color(0xFF9CA3AF),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSuggestionSelected(suggestion) },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A3441)
                                )
                            ) {
                                Text(
                                    text = suggestion,
                                    color = Color(0xFFBECBD1),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close", color = Color(0xFF4A90E2))
            }
        }
    )
}

@Composable
fun ConversationSearchDialog(
    messages: List<ChatMessage>,
    searchResults: List<ChatMessage>,
    bookmarks: List<String>,
    onSearch: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onJumpToMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E2329),
        title = {
            Text(
                text = "Search Conversations",
                color = Color(0xFFDDDDE4),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        onSearch(it)
                    },
                    label = { Text("Search messages...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFBECBD1),
                        unfocusedTextColor = Color(0xFFBECBD1),
                        focusedLabelColor = Color(0xFF9CA3AF),
                        unfocusedLabelColor = Color(0xFF9CA3AF),
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color(0xFF626568)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (searchResults.isNotEmpty()) {
                    Text(
                        text = "Search Results:",
                        color = Color(0xFF9CA3AF),
                        style = MaterialTheme.typography.bodySmall
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(searchResults) { message ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onJumpToMessage(message.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A3441)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = message.message.take(80) + if (message.message.length > 80) "..." else "",
                                        color = Color(0xFFBECBD1),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onBookmark(message.id) }
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (bookmarks.contains(message.id)) 
                                                    android.R.drawable.btn_star_big_on 
                                                else 
                                                    android.R.drawable.btn_star_big_off
                                            ),
                                            contentDescription = "Bookmark",
                                            tint = if (bookmarks.contains(message.id)) Color(0xFFFFD700) else Color(0xFF626568)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close", color = Color(0xFF4A90E2))
            }
        }
    )
}

@Composable
fun QuickActionsOverlay(
    actions: List<String>,
    onActionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2329)
            ),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    color = Color(0xFFDDDDE4),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                actions.forEach { action ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onActionSelected(action) },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2A3441)
                        )
                    ) {
                        Text(
                            text = action,
                            color = Color(0xFFBECBD1),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SPenDrawingCanvas(
    onHandwritingRecognized: (String) -> Unit,
    onCancel: () -> Unit
) {
    var drawingPaths by remember { mutableStateOf(listOf<androidx.compose.ui.graphics.Path>()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "S Pen Writing",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    TextButton(onClick = {
                        // Simulate handwriting recognition
                        onHandwritingRecognized("Write your message here")
                    }) {
                        Text("Recognize", color = Color(0xFF4A90E2))
                    }
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = Color(0xFFE74C3C))
                    }
                }
            }
            
            // Drawing Area
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val newPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(change.position.x, change.position.y)
                                lineTo(change.position.x, change.position.y)
                            }
                            drawingPaths = drawingPaths + newPath
                        }
                    }
            ) {
                drawingPaths.forEach { path ->
                    drawPath(
                        path = path,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            }
            
            // Instructions
            Text(
                text = "Write with your S Pen on the canvas above",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}