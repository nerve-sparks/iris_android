package com.nervesparks.iris.ui.development

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.nervesparks.iris.development.*
import kotlinx.coroutines.launch

/**
 * Development Environment UI
 * Termux-style CLI/GUI hybrid for in-app development
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevelopmentEnvironmentScreen(
    developmentEnvironment: DevelopmentEnvironment,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // UI State
    var activeTab by remember { mutableStateOf(DevTab.TERMINAL) }
    var showFileExplorer by remember { mutableStateOf(false) }
    var terminalSessionId by remember { mutableStateOf("main") }
    
    // Terminal State
    val terminalSession = remember(terminalSessionId) {
        developmentEnvironment.createTerminalSession(terminalSessionId)
    }
    val terminalOutput by terminalSession.output.collectAsState(initial = null)
    val isExecuting by terminalSession.isExecuting.collectAsState()
    
    // File System State
    var currentPath by remember { mutableStateOf(developmentEnvironment.getFileSystemTree().path) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    
    // Command Input State
    var commandInput by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Header with tabs
        DevelopmentHeader(
            activeTab = activeTab,
            onTabSelected = { activeTab = it },
            onNavigateBack = onNavigateBack,
            onToggleFileExplorer = { showFileExplorer = !showFileExplorer }
        )
        
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // File Explorer Sidebar (if enabled)
            AnimatedVisibility(
                visible = showFileExplorer,
                enter = slideInHorizontally() + expandHorizontally(),
                exit = slideOutHorizontally() + shrinkHorizontally()
            ) {
                FileExplorerSidebar(
                    developmentEnvironment = developmentEnvironment,
                    currentPath = currentPath,
                    selectedFile = selectedFile,
                    onPathChanged = { currentPath = it },
                    onFileSelected = { selectedFile = it },
                    modifier = Modifier.width(300.dp)
                )
            }
            
            // Main Content Area
            when (activeTab) {
                DevTab.TERMINAL -> {
                    TerminalView(
                        terminalSession = terminalSession,
                        terminalOutput = terminalOutput,
                        commandInput = commandInput,
                        onCommandInputChanged = { commandInput = it },
                        onCommandExecute = { command ->
                            scope.launch {
                                commandHistory = commandHistory + command
                                historyIndex = -1
                                terminalSession.executeCommand(command)
                                commandInput = ""
                            }
                        },
                        onHistoryNavigate = { direction ->
                            when (direction) {
                                HistoryDirection.UP -> {
                                    if (historyIndex < commandHistory.size - 1) {
                                        historyIndex++
                                        commandInput = commandHistory[commandHistory.size - 1 - historyIndex]
                                    }
                                }
                                HistoryDirection.DOWN -> {
                                    if (historyIndex > 0) {
                                        historyIndex--
                                        commandInput = commandHistory[commandHistory.size - 1 - historyIndex]
                                    } else {
                                        historyIndex = -1
                                        commandInput = ""
                                    }
                                }
                            }
                        },
                        isExecuting = isExecuting,
                        keyboardController = keyboardController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                DevTab.EDITOR -> {
                    CodeEditorView(
                        developmentEnvironment = developmentEnvironment,
                        selectedFile = selectedFile,
                        onFileContentChanged = { filePath, content ->
                            scope.launch {
                                developmentEnvironment.writeFile(filePath, content)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                DevTab.BUILDER -> {
                    BuilderView(
                        developmentEnvironment = developmentEnvironment,
                        currentPath = currentPath,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                DevTab.PROJECTS -> {
                    ProjectsView(
                        developmentEnvironment = developmentEnvironment,
                        onCreateProject = { name, template ->
                            scope.launch {
                                developmentEnvironment.createProject(name, template)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun DevelopmentHeader(
    activeTab: DevTab,
    onTabSelected: (DevTab) -> Unit,
    onNavigateBack: () -> Unit,
    onToggleFileExplorer: () -> Unit
) {
    Surface(
        color = Color(0xFF161B22),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_revert),
                        contentDescription = "Back",
                        tint = Color(0xFFE6EDF3)
                    )
                }
                
                Text(
                    text = "Development Environment",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Row {
                IconButton(onClick = onToggleFileExplorer) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_sort_by_size),
                        contentDescription = "File Explorer",
                        tint = Color(0xFF58A6FF)
                    )
                }
            }
        }
        
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFE6EDF3),
            edgePadding = 16.dp
        ) {
            DevTab.values().forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = "${tab.icon} ${tab.title}",
                            color = if (activeTab == tab) Color(0xFF58A6FF) else Color(0xFF8B949E)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TerminalView(
    terminalSession: TerminalSession,
    terminalOutput: TerminalOutput?,
    commandInput: String,
    onCommandInputChanged: (String) -> Unit,
    onCommandExecute: (String) -> Unit,
    onHistoryNavigate: (HistoryDirection) -> Unit,
    isExecuting: Boolean,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        // Terminal Output
        val outputState = rememberLazyListState()
        
        LazyColumn(
            state = outputState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            item {
                // Welcome message
                Text(
                    text = "Iris Development Terminal v1.0\nType 'help' for commands\n",
                    color = Color(0xFF58A6FF),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            terminalOutput?.let { output ->
                item {
                    TerminalOutputItem(output = output)
                }
            }
        }
        
        // Scroll to bottom when new output arrives
        LaunchedEffect(terminalOutput) {
            if (terminalOutput != null) {
                outputState.animateScrollToItem(outputState.layoutInfo.totalItemsCount - 1)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Command Input
        TerminalCommandInput(
            input = commandInput,
            onInputChanged = onCommandInputChanged,
            onExecute = onCommandExecute,
            onHistoryNavigate = onHistoryNavigate,
            isExecuting = isExecuting,
            prompt = terminalSession.getPrompt(),
            keyboardController = keyboardController
        )
    }
}

@Composable
private fun TerminalOutputItem(output: TerminalOutput) {
    val textColor = when (output.type) {
        OutputType.COMMAND -> Color(0xFF58A6FF)
        OutputType.STDOUT -> Color(0xFFE6EDF3)
        OutputType.STDERR -> Color(0xFFFF6B6B)
        OutputType.SYSTEM -> Color(0xFF4CAF50)
        OutputType.CLEAR -> Color.Transparent
    }
    
    if (output.type != OutputType.CLEAR) {
        Text(
            text = output.content,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

@Composable
private fun TerminalCommandInput(
    input: String,
    onInputChanged: (String) -> Unit,
    onExecute: (String) -> Unit,
    onHistoryNavigate: (HistoryDirection) -> Unit,
    isExecuting: Boolean,
    prompt: String,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Prompt
        Text(
            text = prompt,
            color = Color(0xFF4CAF50),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Command Input Field
        BasicTextField(
            value = input,
            onValueChange = onInputChanged,
            enabled = !isExecuting,
            textStyle = TextStyle(
                color = Color(0xFFE6EDF3),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(Color(0xFF58A6FF)),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (input.isNotBlank()) {
                        onExecute(input)
                        keyboardController?.hide()
                    }
                }
            ),
            modifier = Modifier
                .weight(1f)
                .background(
                    Color(0xFF21262D),
                    RoundedCornerShape(4.dp)
                )
                .padding(8.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Send Button
        Button(
            onClick = {
                if (input.isNotBlank()) {
                    onExecute(input)
                }
            },
            enabled = !isExecuting && input.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF58A6FF)
            ),
            modifier = Modifier.size(40.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isExecuting) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text("▶", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CodeEditorView(
    developmentEnvironment: DevelopmentEnvironment,
    selectedFile: String?,
    onFileContentChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fileContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Load file content when selected file changes
    LaunchedEffect(selectedFile) {
        if (selectedFile != null) {
            isLoading = true
            fileContent = developmentEnvironment.readFile(selectedFile) ?: ""
            isLoading = false
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedFile != null) {
            // File Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFile.substringAfterLast("/"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Text(
                    text = selectedFile,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF8B949E)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Code Editor
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF58A6FF))
                }
            } else {
                BasicTextField(
                    value = fileContent,
                    onValueChange = { newContent ->
                        fileContent = newContent
                        onFileContentChanged(selectedFile, newContent)
                    },
                    textStyle = TextStyle(
                        color = Color(0xFFE6EDF3),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF0D1117),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                )
            }
        } else {
            // No file selected
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No file selected",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFFE6EDF3)
                        )
                    )
                    Text(
                        text = "Select a file from the explorer to edit",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF8B949E)
                        )
                    )
                }
            }
        }
    }
}

// Development Environment Tabs
enum class DevTab(val title: String, val icon: String) {
    TERMINAL("Terminal", "🖥️"),
    EDITOR("Editor", "📝"),
    BUILDER("Builder", "🔨"),
    PROJECTS("Projects", "📂")
}

enum class HistoryDirection {
    UP, DOWN
}