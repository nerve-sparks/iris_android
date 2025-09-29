package com.nervesparks.iris.ui.development

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.development.ProjectTemplate
import com.nervesparks.iris.development.ProjectType

/**
 * Create Project Dialog for Development Environment
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    templates: List<ProjectTemplate>,
    onDismiss: () -> Unit,
    onCreateProject: (String, ProjectTemplate) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<ProjectTemplate?>(null) }
    var currentStep by remember { mutableStateOf(CreateProjectStep.NAME) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp, max = 600.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C2128)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Create New Project",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Step Indicator
                ProjectCreationSteps(
                    currentStep = currentStep,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Content based on current step
                when (currentStep) {
                    CreateProjectStep.NAME -> {
                        ProjectNameStep(
                            projectName = projectName,
                            onProjectNameChanged = { projectName = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    CreateProjectStep.TEMPLATE -> {
                        ProjectTemplateStep(
                            templates = templates,
                            selectedTemplate = selectedTemplate,
                            onTemplateSelected = { selectedTemplate = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    CreateProjectStep.CONFIRM -> {
                        ProjectConfirmationStep(
                            projectName = projectName,
                            template = selectedTemplate!!,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel/Back Button
                    if (currentStep == CreateProjectStep.NAME) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF8B949E)
                            )
                        ) {
                            Text("Cancel")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                currentStep = when (currentStep) {
                                    CreateProjectStep.TEMPLATE -> CreateProjectStep.NAME
                                    CreateProjectStep.CONFIRM -> CreateProjectStep.TEMPLATE
                                    else -> CreateProjectStep.NAME
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF8B949E)
                            )
                        ) {
                            Text("Back")
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Next/Create Button
                    val (buttonText, buttonEnabled) = when (currentStep) {
                        CreateProjectStep.NAME -> "Next" to projectName.isNotBlank()
                        CreateProjectStep.TEMPLATE -> "Next" to (selectedTemplate != null)
                        CreateProjectStep.CONFIRM -> "Create Project" to true
                    }
                    
                    Button(
                        onClick = {
                            when (currentStep) {
                                CreateProjectStep.NAME -> {
                                    currentStep = CreateProjectStep.TEMPLATE
                                }
                                CreateProjectStep.TEMPLATE -> {
                                    currentStep = CreateProjectStep.CONFIRM
                                }
                                CreateProjectStep.CONFIRM -> {
                                    onCreateProject(projectName, selectedTemplate!!)
                                }
                            }
                        },
                        enabled = buttonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF58A6FF)
                        )
                    ) {
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCreationSteps(
    currentStep: CreateProjectStep,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CreateProjectStep.values().forEachIndexed { index, step ->
            val isActive = step.ordinal <= currentStep.ordinal
            val isCurrent = step == currentStep
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            when {
                                isCurrent -> Color(0xFF58A6FF)
                                isActive -> Color(0xFF238636)
                                else -> Color(0xFF30363D)
                            },
                            RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && !isCurrent) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Step Label
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isActive) Color(0xFFE6EDF3) else Color(0xFF8B949E),
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                    )
                )
                
                // Connector Line (except for last step)
                if (index < CreateProjectStep.values().size - 1) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(
                                if (isActive) Color(0xFF238636) else Color(0xFF30363D)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectNameStep(
    projectName: String,
    onProjectNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Project Name",
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Choose a name for your new project",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF8B949E)
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChanged,
            label = { Text("Project Name") },
            placeholder = { Text("my-awesome-app") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE6EDF3),
                unfocusedTextColor = Color(0xFFE6EDF3),
                focusedLabelColor = Color(0xFF58A6FF),
                unfocusedLabelColor = Color(0xFF8B949E),
                focusedBorderColor = Color(0xFF58A6FF),
                unfocusedBorderColor = Color(0xFF30363D),
                focusedPlaceholderColor = Color(0xFF656D76),
                unfocusedPlaceholderColor = Color(0xFF656D76)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Project Name Rules
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161B22)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Project Name Rules:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val rules = listOf(
                    "Use lowercase letters and hyphens",
                    "Start with a letter",
                    "No spaces or special characters",
                    "Keep it descriptive but concise"
                )
                
                rules.forEach { rule ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "•",
                            color = Color(0xFF58A6FF),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8B949E)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectTemplateStep(
    templates: List<ProjectTemplate>,
    selectedTemplate: ProjectTemplate?,
    onTemplateSelected: (ProjectTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = "Choose Template",
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Select a project template to get started quickly",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF8B949E)
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(templates) { template ->
                TemplateSelectionCard(
                    template = template,
                    isSelected = selectedTemplate == template,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
    }
}

@Composable
private fun TemplateSelectionCard(
    template: ProjectTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF58A6FF).copy(alpha = 0.1f) else Color(0xFF161B22)
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF58A6FF))
        } else {
            BorderStroke(1.dp, Color(0xFF30363D))
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Template Icon
            Text(
                text = getProjectTypeIcon(template.type),
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Template Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF8B949E)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${template.files.size} files • ${template.type.name.lowercase().replaceFirstChar { it.uppercase() }} project",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF656D76)
                    )
                )
            }
            
            // Selection Indicator
            if (isSelected) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.checkbox_on_background),
                    contentDescription = "Selected",
                    tint = Color(0xFF58A6FF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ProjectConfirmationStep(
    projectName: String,
    template: ProjectTemplate,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Confirm Project Creation",
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Review your project settings before creation",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF8B949E)
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161B22)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                ProjectDetailRow("Project Name", projectName)
                Spacer(modifier = Modifier.height(12.dp))
                ProjectDetailRow("Template", template.name)
                Spacer(modifier = Modifier.height(12.dp))
                ProjectDetailRow("Type", template.type.name.lowercase().replaceFirstChar { it.uppercase() })
                Spacer(modifier = Modifier.height(12.dp))
                ProjectDetailRow("Files", "${template.files.size} initial files")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = Color(0xFF30363D))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF8B949E),
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE6EDF3)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Creation Info
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1F2937).copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ℹ️",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "The project will be created in your development workspace with a complete project structure and build configuration.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF8B949E)
                    )
                )
            }
        }
    }
}

@Composable
private fun ProjectDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF8B949E)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun getProjectTypeIcon(type: ProjectType): String {
    return when (type) {
        ProjectType.ANDROID -> "📱"
        ProjectType.KOTLIN -> "🔷"
        ProjectType.JAVA -> "☕"
    }
}

enum class CreateProjectStep(val title: String) {
    NAME("Name"),
    TEMPLATE("Template"),
    CONFIRM("Confirm")
}