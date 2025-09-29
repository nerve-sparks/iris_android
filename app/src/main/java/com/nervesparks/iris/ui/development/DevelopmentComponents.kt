package com.nervesparks.iris.ui.development

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.nervesparks.iris.development.*
import kotlinx.coroutines.launch

/**
 * File Explorer Sidebar for Development Environment
 */

@Composable
fun FileExplorerSidebar(
    developmentEnvironment: DevelopmentEnvironment,
    currentPath: String,
    selectedFile: String?,
    onPathChanged: (String) -> Unit,
    onFileSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fileTree by remember { mutableStateOf<FileNode?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    
    // Load file tree
    LaunchedEffect(currentPath) {
        fileTree = developmentEnvironment.getFileSystemTree(currentPath)
    }
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF161B22))
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold
                )
            )
            
            IconButton(
                onClick = {
                    scope.launch {
                        fileTree = developmentEnvironment.getFileSystemTree(currentPath)
                    }
                }
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_refresh),
                    contentDescription = "Refresh",
                    tint = Color(0xFF58A6FF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Current Path
        Text(
            text = currentPath,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF8B949E)
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        HorizontalDivider(color = Color(0xFF30363D))
        
        // File Tree
        fileTree?.let { tree ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    FileTreeNode(
                        node = tree,
                        level = 0,
                        expandedFolders = expandedFolders,
                        selectedFile = selectedFile,
                        onToggleExpanded = { path ->
                            expandedFolders = if (path in expandedFolders) {
                                expandedFolders - path
                            } else {
                                expandedFolders + path
                            }
                        },
                        onFileSelected = onFileSelected,
                        onPathChanged = onPathChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTreeNode(
    node: FileNode,
    level: Int,
    expandedFolders: Set<String>,
    selectedFile: String?,
    onToggleExpanded: (String) -> Unit,
    onFileSelected: (String) -> Unit,
    onPathChanged: (String) -> Unit
) {
    val isExpanded = node.path in expandedFolders
    val isSelected = selectedFile == node.path
    
    Column {
        // Node item
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        onToggleExpanded(node.path)
                        if (!isExpanded) {
                            onPathChanged(node.path)
                        }
                    } else {
                        onFileSelected(node.path)
                    }
                }
                .background(
                    if (isSelected) Color(0xFF58A6FF).copy(alpha = 0.1f) else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .padding(
                    start = (level * 16 + 8).dp,
                    top = 4.dp,
                    bottom = 4.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/Collapse Icon (for directories)
            if (node.isDirectory) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        if (isExpanded) android.R.drawable.arrow_down_float else android.R.drawable.arrow_up_float
                    ),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF8B949E),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // File/Folder Icon
            Text(
                text = getFileIcon(node),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // File name
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) Color(0xFF58A6FF) else Color(0xFFE6EDF3),
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // File size (for files only)
            if (!node.isDirectory && node.size > 0) {
                Text(
                    text = formatFileSize(node.size),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF656D76)
                    )
                )
            }
        }
        
        // Children (if directory is expanded)
        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                FileTreeNode(
                    node = child,
                    level = level + 1,
                    expandedFolders = expandedFolders,
                    selectedFile = selectedFile,
                    onToggleExpanded = onToggleExpanded,
                    onFileSelected = onFileSelected,
                    onPathChanged = onPathChanged
                )
            }
        }
    }
}

@Composable
fun BuilderView(
    developmentEnvironment: DevelopmentEnvironment,
    currentPath: String,
    modifier: Modifier = Modifier
) {
    var buildStatus by remember { mutableStateOf<BuildStatus?>(null) }
    var isBuilding by remember { mutableStateOf(false) }
    var buildOutput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Load build status
    LaunchedEffect(currentPath) {
        // buildStatus = developmentEnvironment.buildSystem.getBuildStatus(currentPath)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Build System",
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Actions
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(getBuildActions()) { action ->
                BuildActionButton(
                    action = action,
                    isExecuting = isBuilding,
                    onClick = {
                        scope.launch {
                            isBuilding = true
                            buildOutput = "Executing ${action.command}...\n"
                            
                            val result = developmentEnvironment.executeCommand(action.command, currentPath)
                            buildOutput += result.output
                            if (result.error.isNotEmpty()) {
                                buildOutput += "\nErrors:\n${result.error}"
                            }
                            
                            isBuilding = false
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Build Status
        buildStatus?.let { status ->
            BuildStatusCard(status = status)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Build Output
        Text(
            text = "Output",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0D1117)
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        text = buildOutput.ifEmpty { "No build output yet. Run a build command to see results." },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFE6EDF3),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectsView(
    developmentEnvironment: DevelopmentEnvironment,
    onCreateProject: (String, ProjectTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val projectTemplates = remember { developmentEnvironment.getProjectTemplates() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold
                )
            )
            
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58A6FF)
                )
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_add),
                    contentDescription = "Create",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Project")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Project Templates
        Text(
            text = "Project Templates",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projectTemplates) { template ->
                ProjectTemplateCard(
                    template = template,
                    onClick = {
                        // Will trigger create dialog with pre-selected template
                        showCreateDialog = true
                    }
                )
            }
        }
    }
    
    // Create Project Dialog
    if (showCreateDialog) {
        CreateProjectDialog(
            templates = projectTemplates,
            onDismiss = { showCreateDialog = false },
            onCreateProject = { name, template ->
                onCreateProject(name, template)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun BuildActionButton(
    action: BuildAction,
    isExecuting: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isExecuting,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF21262D)
        ),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Text(
            text = "${action.icon} ${action.name}",
            color = Color(0xFFE6EDF3)
        )
    }
}

@Composable
private fun BuildStatusCard(status: BuildStatus) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Build Status",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status:",
                    color = Color(0xFF8B949E),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = status.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = when (status.status) {
                        BuildStatusType.UP_TO_DATE -> Color(0xFF4CAF50)
                        BuildStatusType.OUT_OF_DATE -> Color(0xFFFF9800)
                        BuildStatusType.NEVER_BUILT -> Color(0xFF8B949E)
                        BuildStatusType.ERROR -> Color(0xFFFF5722)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Source Files:",
                    color = Color(0xFF8B949E),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = status.sourceFileCount.toString(),
                    color = Color(0xFFE6EDF3),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (status.lastBuildTime > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Last Build:",
                        color = Color(0xFF8B949E),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(status.lastBuildTime)),
                        color = Color(0xFFE6EDF3),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectTemplateCard(
    template: ProjectTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getProjectTypeIcon(template.type),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFE6EDF3),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF8B949E)
                        )
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getFileIcon(node: FileNode): String {
    return if (node.isDirectory) {
        "📁"
    } else {
        when (node.name.substringAfterLast(".", "")) {
            "kt" -> "🔷"
            "java" -> "☕"
            "xml" -> "📄"
            "json" -> "📋"
            "md" -> "📝"
            "gradle" -> "🐘"
            "properties" -> "⚙️"
            "png", "jpg", "jpeg" -> "🖼️"
            else -> "📄"
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${bytes / (1024 * 1024)}MB"
    }
}

private fun getBuildActions(): List<BuildAction> {
    return listOf(
        BuildAction("Build", "🔨", "gradle build"),
        BuildAction("Clean", "🧹", "gradle clean"),
        BuildAction("Test", "🧪", "gradle test"),
        BuildAction("Run", "▶️", "gradle run"),
        BuildAction("Assemble", "📦", "gradle assemble")
    )
}

private fun getProjectTypeIcon(type: ProjectType): String {
    return when (type) {
        ProjectType.ANDROID -> "📱"
        ProjectType.KOTLIN -> "🔷"
        ProjectType.JAVA -> "☕"
    }
}

data class BuildAction(
    val name: String,
    val icon: String,
    val command: String
)