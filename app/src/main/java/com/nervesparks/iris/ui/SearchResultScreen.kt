package com.nervesparks.iris.ui

import android.app.DownloadManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.ModelCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val coroutineScope = rememberCoroutineScope()


    var UserGivenModel by remember {
        mutableStateOf(
            TextFieldValue(
                text = viewModel.userGivenModel,
                selection = TextRange(viewModel.userGivenModel.length)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Search Input and Button Row
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
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        val response = withContext(Dispatchers.IO) {
                            val url = URL("https://huggingface.co/api/models/${UserGivenModel.text}")
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.setRequestProperty("Accept", "application/json")
                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000

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

                        val jsonResponse = JSONObject(response)
                        val siblingsArray = jsonResponse.getJSONArray("siblings")
                        modelData = (0 until siblingsArray.length()).mapNotNull { index ->
                            val jsonObject = siblingsArray.getJSONObject(index)
                            val filename = jsonObject.optString("rfilename", "")

                            if (filename.isNotEmpty()) {
                                mapOf("rfilename" to filename)
                            } else {
                                null
                            }
                        }
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
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filteredModels = models.filter { model -> model["rfilename"]?.endsWith("gguf") == true }
                items(filteredModels) { model ->
                    ModelCard(
                        modelName = model["rfilename"] ?: "Unknown Model",
                        dm = dm,
                        viewModel = viewModel,
                        extFilesDir = extFilesDir,
                        downloadLink = ""

                    )
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

