package com.nervesparks.iris.workbench.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nervesparks.iris.workbench.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Integrated Development Workbench Screen
 * Termux-style interface with GUI development capabilities
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkbenchScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val workbench = rememberIntegratedWorkbench(context)
    val scope = rememberCoroutineScope()
    
    var selectedMode by rememberSaveable { mutableStateOf(WorkbenchMode.TERMINAL) }
    val pagerState = rememberPagerState(pageCount = { WorkbenchMode.entries.size })
    
    // Sync pager with selected mode
    LaunchedEffect(selectedMode) {
        pagerState.animateScrollToPage(WorkbenchMode.entries.indexOf(selectedMode))
    }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedMode = WorkbenchMode.entries[pagerState.currentPage]
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Top App Bar
        WorkbenchTopBar(
            onNavigateBack = onNavigateBack,
            selectedMode = selectedMode,
            onModeChange = { selectedMode = it }
        )
        
        // Mode Tabs
        WorkbenchTabs(
            selectedMode = selectedMode,
            onModeChange = { selectedMode = it }
        )
        
        // Content Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (WorkbenchMode.entries[page]) {
                WorkbenchMode.TERMINAL -> TerminalTab(workbench)
                WorkbenchMode.FILE_MANAGER -> FileManagerTab(workbench)
                WorkbenchMode.CODE_EDITOR -> CodeEditorTab(workbench)
                WorkbenchMode.BUILD_TOOLS -> BuildToolsTab(workbench)
                WorkbenchMode.PACKAGE_MANAGER -> PackageManagerTab(workbench)
                WorkbenchMode.GIT_CLIENT -> GitClientTab(workbench)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkbenchTopBar(
    onNavigateBack: () -> Unit,
    selectedMode: WorkbenchMode,
    onModeChange: (WorkbenchMode) -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Development Workbench",
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFE6EDF3)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Open workbench settings */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFE6EDF3)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF161B22)
        )
    )
}

@Composable
private fun WorkbenchTabs(
    selectedMode: WorkbenchMode,
    onModeChange: (WorkbenchMode) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = WorkbenchMode.entries.indexOf(selectedMode),
        containerColor = Color(0xFF161B22),
        contentColor = Color(0xFFE6EDF3),
        edgePadding = 0.dp
    ) {
        WorkbenchMode.entries.forEach { mode ->
            Tab(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                text = {
                    Text(
                        text = mode.displayName(),
                        fontSize = 14.sp,
                        fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(
                        imageVector = mode.icon(),
                        contentDescription = mode.displayName(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun TerminalTab(workbench: IntegratedWorkbench) {
    val terminalOutput by workbench.terminalOutput.collectAsStateWithLifecycle()
    val currentDirectory by workbench.currentDirectory.collectAsStateWithLifecycle()
    val isExecuting by workbench.isExecuting.collectAsStateWithLifecycle()
    
    var commandInput by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    
    // Auto-scroll to bottom when new output arrives
    LaunchedEffect(terminalOutput) {
        if (terminalOutput.isNotEmpty()) {
            scrollState.animateScrollToItem(Int.MAX_VALUE)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Current directory indicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22)
        ) {
            Text(
                text = "📁 $currentDirectory",
                color = Color(0xFF58A6FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp),
                fontFamily = FontFamily.Monospace
            )
        }
        
        // Terminal output
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (terminalOutput.isEmpty()) {
                item {
                    Text(
                        text = """
                            Welcome to Integrated Development Workbench
                            
                            Type 'help' for available commands
                            Features:
                            • Termux-style terminal with GUI enhancements
                            • Integrated code editor and file manager
                            • Android build system integration
                            • Git repository management
                            • Real-time app development and testing
                        """.trimIndent(),
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            
            items(terminalOutput.split("\n")) { line ->
                SelectionContainer {
                    Text(
                        text = line,
                        color = when {
                            line.startsWith("ERROR:") -> Color(0xFFF85149)
                            line.startsWith("workbench:") -> Color(0xFF58A6FF)
                            line.contains("SUCCESS") -> Color(0xFF56D364)
                            else -> Color(0xFFE6EDF3)
                        },
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            if (isExecuting) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF58A6FF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Executing...",
                            color = Color(0xFF7D8590),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        
        // Command input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    color = Color(0xFF58A6FF),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = {
                        Text(
                            text = "Enter command...",
                            color = Color(0xFF7D8590),
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commandInput.isNotBlank() && !isExecuting) {
                                scope.launch {
                                    workbench.executeCommand(commandInput.trim())
                                    commandInput = ""
                                }
                            }
                        }
                    ),
                    enabled = !isExecuting,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3),
                        focusedBorderColor = Color(0xFF58A6FF),
                        unfocusedBorderColor = Color(0xFF30363D),
                        cursorColor = Color(0xFF58A6FF)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (commandInput.isNotBlank() && !isExecuting) {
                            scope.launch {
                                workbench.executeCommand(commandInput.trim())
                                commandInput = ""
                            }
                        }
                    },
                    enabled = !isExecuting && commandInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Execute",
                        tint = if (isExecuting || commandInput.isBlank()) {
                            Color(0xFF7D8590)
                        } else {
                            Color(0xFF58A6FF)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileManagerTab(workbench: IntegratedWorkbench) {
    val projectFiles by workbench.projectFiles.collectAsStateWithLifecycle()
    val currentDirectory by workbench.currentDirectory.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Directory header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Directory",
                    tint = Color(0xFF58A6FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentDirectory,
                    color = Color(0xFFE6EDF3),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            workbench.executeCommand("ls -la")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF58A6FF)
                    )
                }
            }
        }
        
        // File list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Parent directory (..)
            item {
                FileItem(
                    name = "..",
                    isDirectory = true,
                    size = 0L,
                    lastModified = 0L,
                    permissions = "drwx",
                    onClick = {
                        scope.launch {
                            workbench.executeCommand("cd ..")
                        }
                    }
                )
            }
            
            items(projectFiles) { file ->
                FileItem(
                    name = file.name,
                    isDirectory = file.isDirectory,
                    size = file.size,
                    lastModified = file.lastModified,
                    permissions = file.permissions,
                    onClick = {
                        scope.launch {
                            if (file.isDirectory) {
                                workbench.executeCommand("cd ${file.name}")
                            } else {
                                workbench.executeCommand("edit ${file.name}")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FileItem(
    name: String,
    isDirectory: Boolean,
    size: Long,
    lastModified: Long,
    permissions: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color(0xFF0D1117)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = if (isDirectory) "Directory" else "File",
                tint = if (isDirectory) Color(0xFF58A6FF) else Color(0xFF7D8590),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color(0xFFE6EDF3),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                
                Row {
                    Text(
                        text = permissions,
                        color = Color(0xFF7D8590),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    if (!isDirectory && size > 0) {
                        Text(
                            text = " • ${formatFileSize(size)}",
                            color = Color(0xFF7D8590),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    if (lastModified > 0) {
                        Text(
                            text = " • ${formatDate(lastModified)}",
                            color = Color(0xFF7D8590),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeEditorTab(workbench: IntegratedWorkbench) {
    var selectedFile by rememberSaveable { mutableStateOf<String?>(null) }
    var fileContent by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Editor header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF161B22)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Code Editor",
                    tint = Color(0xFF58A6FF),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = selectedFile ?: "No file selected",
                    color = Color(0xFFE6EDF3),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (hasUnsavedChanges) {
                    Text(
                        text = "●",
                        color = Color(0xFFF85149),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        selectedFile?.let { file ->
                            scope.launch {
                                if (workbench.writeFileContent(file, fileContent)) {
                                    hasUnsavedChanges = false
                                }
                            }
                        }
                    },
                    enabled = selectedFile != null && hasUnsavedChanges
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        tint = if (selectedFile != null && hasUnsavedChanges) {
                            Color(0xFF56D364)
                        } else {
                            Color(0xFF7D8590)
                        }
                    )
                }
            }
        }
        
        // Editor content
        if (selectedFile != null) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF58A6FF))
                }
            } else {
                OutlinedTextField(
                    value = fileContent,
                    onValueChange = { 
                        fileContent = it
                        hasUnsavedChanges = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6EDF3),
                        unfocusedTextColor = Color(0xFFE6EDF3),
                        focusedBorderColor = Color(0xFF58A6FF),
                        unfocusedBorderColor = Color(0xFF30363D),
                        cursorColor = Color(0xFF58A6FF)
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "No file",
                        tint = Color(0xFF7D8590),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select a file to edit",
                        color = Color(0xFF7D8590),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use 'edit filename' command or file manager",
                        color = Color(0xFF7D8590),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
    
    // Load file content when selected file changes
    LaunchedEffect(selectedFile) {
        selectedFile?.let { file ->
            isLoading = true
            fileContent = workbench.readFileContent(file)
            hasUnsavedChanges = false
            isLoading = false
        }
    }
}

@Composable
private fun BuildToolsTab(workbench: IntegratedWorkbench) {
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Build Tools",
                color = Color(0xFFE6EDF3),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // Build actions
        item {
            BuildActionCard(
                title = "Build Debug APK",
                description = "Compile and build debug version",
                icon = Icons.Default.Build,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("build debug")
                    }
                }
            )
        }
        
        item {
            BuildActionCard(
                title = "Build Release APK",
                description = "Compile and build release version",
                icon = Icons.Default.Publish,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("build release")
                    }
                }
            )
        }
        
        item {
            BuildActionCard(
                title = "Clean Build",
                description = "Clean all build artifacts",
                icon = Icons.Default.CleaningServices,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("gradle clean")
                    }
                }
            )
        }
        
        item {
            BuildActionCard(
                title = "Run Tests",
                description = "Execute unit and integration tests",
                icon = Icons.Default.Quiz,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("gradle test")
                    }
                }
            )
        }
        
        item {
            BuildActionCard(
                title = "Generate APK Bundle",
                description = "Generate Android App Bundle (AAB)",
                icon = Icons.Default.Archive,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("gradle bundleRelease")
                    }
                }
            )
        }
    }
}

@Composable
private fun BuildActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF58A6FF),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFE6EDF3),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Execute",
                tint = Color(0xFF7D8590),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PackageManagerTab(workbench: IntegratedWorkbench) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.GetApp,
            contentDescription = "Package Manager",
            tint = Color(0xFF58A6FF),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Package Manager",
            color = Color(0xFFE6EDF3),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Coming Soon",
            color = Color(0xFF7D8590),
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Advanced package management features",
            color = Color(0xFF7D8590),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun GitClientTab(workbench: IntegratedWorkbench) {
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Git Client",
                color = Color(0xFFE6EDF3),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        item {
            GitActionCard(
                title = "Git Status",
                description = "Check repository status",
                icon = Icons.Default.Info,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("git status")
                    }
                }
            )
        }
        
        item {
            GitActionCard(
                title = "Git Log",
                description = "View commit history",
                icon = Icons.Default.History,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("git log --oneline -10")
                    }
                }
            )
        }
        
        item {
            GitActionCard(
                title = "Git Add All",
                description = "Stage all changes",
                icon = Icons.Default.Add,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("git add .")
                    }
                }
            )
        }
        
        item {
            GitActionCard(
                title = "Git Commit",
                description = "Commit staged changes",
                icon = Icons.Default.Save,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("git commit -m 'Workbench commit'")
                    }
                }
            )
        }
        
        item {
            GitActionCard(
                title = "Git Pull",
                description = "Pull latest changes",
                icon = Icons.Default.CloudDownload,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("git pull")
                    }
                }
            )
        }
        
        item {
            GitActionCard(
                title = "Git Push",
                description = "Push commits to remote",
                icon = Icons.Default.CloudUpload,
                onClick = {
                    scope.launch {
                        workbench.executeCommand("git push")
                    }
                }
            )
        }
    }
}

@Composable
private fun GitActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF58A6FF),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFE6EDF3),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = Color(0xFF7D8590),
                    fontSize = 14.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Execute",
                tint = Color(0xFF7D8590),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Extension functions for WorkbenchMode
private fun WorkbenchMode.displayName(): String = when (this) {
    WorkbenchMode.TERMINAL -> "Terminal"
    WorkbenchMode.FILE_MANAGER -> "Files"
    WorkbenchMode.CODE_EDITOR -> "Editor"
    WorkbenchMode.BUILD_TOOLS -> "Build"
    WorkbenchMode.PACKAGE_MANAGER -> "Packages"
    WorkbenchMode.GIT_CLIENT -> "Git"
}

private fun WorkbenchMode.icon(): ImageVector = when (this) {
    WorkbenchMode.TERMINAL -> Icons.Default.Terminal
    WorkbenchMode.FILE_MANAGER -> Icons.Default.Folder
    WorkbenchMode.CODE_EDITOR -> Icons.Default.Code
    WorkbenchMode.BUILD_TOOLS -> Icons.Default.Build
    WorkbenchMode.PACKAGE_MANAGER -> Icons.Default.GetApp
    WorkbenchMode.GIT_CLIENT -> Icons.Default.Source
}

// Utility functions
private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}