package com.nervesparks.iris.ui

import android.app.DownloadManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.InfoModal
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.components.ModelCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

@Composable
fun SearchResultScreen(viewModel: MainViewModel, dm: DownloadManager, extFilesDir: File) {
    var modelData by rememberSaveable { mutableStateOf<List<Map<String, String>>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showingFiles by remember { mutableStateOf(false) }
    var selectedModelId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val kc = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var UserGivenModel by remember {
        mutableStateOf(
            TextFieldValue(
                text = viewModel.userGivenModel,
                selection = TextRange(viewModel.userGivenModel.length)
            )
        )
    }
    if (viewModel.showAlert) {
        // Modal dialog to show download options
        LoadingModal(viewModel)

    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        InfoModal(
            showDialog = viewModel.showDownloadInfoModal,
            onDismiss = { viewModel.showDownloadInfoModal = false }
        )
        // Search Input and Button Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.wrapContentSize()
        ) {
            Text(
                text = "Example: bartowski/Llama-3.2-1B-Instruct-GGUF",
                modifier = Modifier.padding(4.dp),
                color = Color.White,
                fontSize = 10.sp
            )

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString("bartowski/Llama-3.2-1B-Instruct-GGUF"))
                    Toast.makeText(context, "Text copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.copy1),
                    contentDescription = "Copy text",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = UserGivenModel,
            onValueChange = { newValue ->
                UserGivenModel = newValue
                viewModel.userGivenModel = newValue.text
            },
            label = { Text("Search Models Online") },
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Transparent),
            singleLine = true,
            maxLines = 1,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF666666),
                focusedBorderColor = Color(0xFFcfcfd1),
                unfocusedLabelColor = Color(0xFF666666),
                focusedLabelColor = Color(0xFFcfcfd1),
                unfocusedTextColor = Color(0xFFf5f5f5),
                focusedTextColor = Color(0xFFf7f5f5),
            )
        )

        Spacer(Modifier.height(16.dp))


        Button(
            onClick = {
                kc?.hide()
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        val response = withContext(Dispatchers.IO) {
                            // Use Hugging Face search API to find models matching the search term
                            // Filter for models with "gguf" in their tags
                            val searchTerm = java.net.URLEncoder.encode(UserGivenModel.text, "UTF-8")
                            val url = URL("https://huggingface.co/api/models?search=$searchTerm&filter=gguf&sort=downloads&direction=-1&limit=20")
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.setRequestProperty("Accept", "application/json")
                            connection.connectTimeout = 15000
                            connection.readTimeout = 15000

                            val responseCode = connection.responseCode
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                connection.inputStream.bufferedReader().use { it.readText() }
                            } else {
                                val errorStream = connection.errorStream?.bufferedReader()
                                    ?.use { it.readText() }
                                throw Exception(
                                    "HTTP error code: $responseCode - ${errorStream ?: "No additional error details"}"
                                )
                            }
                        }

                        // Parse the search results (returns an array of model objects)
                        val jsonArray = JSONArray(response)
                        val searchResults = mutableListOf<Map<String, String>>()
                        
                        for (i in 0 until jsonArray.length()) {
                            val modelObj = jsonArray.getJSONObject(i)
                            val modelId = modelObj.optString("id", "")
                            
                            if (modelId.isNotEmpty()) {
                                // For each model found, we'll add it to results
                                // The actual files will be fetched when user selects the model
                                searchResults.add(mapOf(
                                    "modelId" to modelId,
                                    "rfilename" to modelId // Using modelId as placeholder
                                ))
                            }
                        }
                        
                        modelData = searchResults
                    } catch (e: Exception) {
                        Log.e("ModelFetch", "Failed to fetch model", e)
                        errorMessage = when (e) {
                            is UnknownHostException -> "No internet connection"
                            is SocketTimeoutException -> "Connection timed out"
                            else -> "Failed to fetch model: ${e.localizedMessage ?: "Unknown error"}"
                        }
                        modelData = null
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = UserGivenModel.text.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF171E24),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF171E2C),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = when {
                    isLoading -> "Searching..."
                    else -> "Search Model"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        // Error Message
        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        
        // Model Results
        modelData?.let { models ->
            if (models.isEmpty()) {
                Text(
                    text = "No GGUF models found matching '${UserGivenModel.text}'",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else if (showingFiles) {
                // Showing GGUF files from a selected repository
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GGUF files in $selectedModelId:",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = {
                            showingFiles = false
                            selectedModelId = null
                            // Search again to show repositories
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF374151),
                            contentColor = Color.White
                        )
                    ) {
                        Text("← Back", fontSize = 12.sp)
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(models) { model ->
                        ModelCard(
                            modelName = model["rfilename"] ?: "Unknown Model",
                            dm = dm,
                            viewModel = viewModel,
                            extFilesDir = extFilesDir,
                            downloadLink = "https://huggingface.co/${model["modelId"]}/resolve/main/${model["rfilename"]}",
                            showDeleteButton = false
                        )
                    }
                }
            } else {
                // Showing model repositories
                Text(
                    text = "Found ${models.size} model(s). Click to view GGUF files:",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(models) { model ->
                        ModelRepositoryCard(
                            modelId = model["modelId"] ?: "Unknown",
                            dm = dm,
                            viewModel = viewModel,
                            extFilesDir = extFilesDir,
                            onModelClick = { modelId ->
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val response = withContext(Dispatchers.IO) {
                                            val url = URL("https://huggingface.co/api/models/$modelId")
                                            val connection = url.openConnection() as HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.setRequestProperty("Accept", "application/json")
                                            connection.connectTimeout = 15000
                                            connection.readTimeout = 15000

                                            val responseCode = connection.responseCode
                                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                                connection.inputStream.bufferedReader().use { it.readText() }
                                            } else {
                                                throw Exception("Failed to fetch model files: HTTP $responseCode")
                                            }
                                        }

                                        val jsonResponse = JSONObject(response)
                                        val siblingsArray = jsonResponse.getJSONArray("siblings")
                                        val files = (0 until siblingsArray.length()).mapNotNull { index ->
                                            val jsonObject = siblingsArray.getJSONObject(index)
                                            val filename = jsonObject.optString("rfilename", "")

                                            if (filename.endsWith(".gguf", ignoreCase = true)) {
                                                mapOf(
                                                    "rfilename" to filename,
                                                    "modelId" to modelId
                                                )
                                            } else {
                                                null
                                            }
                                        }
                                        
                                        if (files.isEmpty()) {
                                            errorMessage = "No GGUF files found in this repository"
                                        } else {
                                            modelData = files
                                            showingFiles = true
                                            selectedModelId = modelId
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ModelFileFetch", "Failed to fetch files", e)
                                        errorMessage = "Failed to fetch files: ${e.localizedMessage}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Loading Indicator (Optional)
        if (isLoading) {
            Text(
                text = "Searching for models...",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ModelRepositoryCard(
    modelId: String,
    dm: DownloadManager,
    viewModel: MainViewModel,
    extFilesDir: File,
    onModelClick: (String) -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFF1a1d24)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelId,
                    color = Color.White,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Click to view GGUF files",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Button(
                onClick = { onModelClick(modelId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("View Files", fontSize = 14.sp)
            }
        }
    }
}

