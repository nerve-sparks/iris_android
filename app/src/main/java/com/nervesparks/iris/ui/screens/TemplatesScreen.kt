package com.nervesparks.iris.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.Template
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.PrimaryButton
import com.nervesparks.iris.ui.theme.SecondaryButton
import com.nervesparks.iris.ui.theme.SurfaceCard
import com.nervesparks.iris.ui.theme.ModernTextField
import kotlinx.coroutines.launch

@Composable
fun TemplatesScreen(
    viewModel: MainViewModel
) {
    val templates = viewModel.templates
    var showDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<Template?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(ComponentStyles.defaultPadding)
        ) {
            // Header
            Text(
                text = "Templates",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = ComponentStyles.defaultPadding)
            )

            // Add Template Button
            PrimaryButton(
                onClick = {
                    editingTemplate = null
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Template",
                    modifier = Modifier.padding(end = ComponentStyles.smallPadding)
                )
                Text("Add Template")
            }

            Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))

            // Templates List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ComponentStyles.smallPadding)
            ) {
                items(items = templates, key = { it.id }) { template ->
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ComponentStyles.defaultPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (template.content.isNotBlank()) {
                                    Text(
                                        text = template.content.take(100) + if (template.content.length > 100) "..." else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = ComponentStyles.smallSpacing)
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editingTemplate = template
                                        showDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val success = viewModel.deleteTemplate(template)
                                            snackbarHostState.showSnackbar(
                                                if (success) "Template deleted" else "Error deleting template"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            var name by remember(editingTemplate) { mutableStateOf(editingTemplate?.name ?: "") }
            var content by remember(editingTemplate) { mutableStateOf(editingTemplate?.content ?: "") }

            androidx.compose.ui.window.Dialog(onDismissRequest = { showDialog = false }) {
                com.nervesparks.iris.ui.theme.ThemedModalCard {
                    Column(Modifier.padding(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding)) {
                        Text(text = if (editingTemplate == null) "New Template" else "Edit Template", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.smallPadding))
                        ModernTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Content") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ComponentStyles.textFieldShape,
                            minLines = 3,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.height(com.nervesparks.iris.ui.theme.ComponentStyles.defaultPadding))
                        Row(horizontalArrangement = Arrangement.spacedBy(com.nervesparks.iris.ui.theme.ComponentStyles.defaultSpacing)) {
                            SecondaryButton(onClick = { showDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            PrimaryButton(onClick = {
                                if (name.isNotBlank()) {
                                    scope.launch {
                                        val success = if (editingTemplate == null) {
                                            viewModel.addTemplate(Template(name = name, content = content))
                                        } else {
                                            viewModel.editTemplate(editingTemplate!!.copy(name = name, content = content))
                                        }
                                        if (success) {
                                            showDialog = false
                                            snackbarHostState.showSnackbar(if (editingTemplate == null) "Template added" else "Template updated")
                                        } else {
                                            snackbarHostState.showSnackbar("Error saving template")
                                        }
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Name cannot be blank") }
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Save") }
                        }
                    }
                }
            }
        }
    }
}
