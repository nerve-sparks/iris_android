package com.nervesparks.iris.ui.components

import android.app.DownloadManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultBottomSheet(
    viewModel: MainViewModel,
    dm: DownloadManager,
    extFileDir: File,
    modelData: List<Map<String, String>>?,
    isInitiallyVisible: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    // Manage bottom sheet visibility
    var isBottomSheetVisible by remember { mutableStateOf(isInitiallyVisible) }

    // Manage loading and error states
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Create sheet state
    val sheetState = rememberModalBottomSheetState()

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                isBottomSheetVisible = false
                onDismiss()
            },
            sheetState = sheetState,
            containerColor = Color.Black,
        ) {
            // Bottom sheet content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    // Make the models scrollable
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        modelData?.forEach { model ->
                            item {
                                model["rfilename"]?.takeIf { it.endsWith(".gguf") }?.let { filename ->
                                    val fullUrl = "https://huggingface.co/${viewModel.userGivenModel}/resolve/main/${filename}?download=true"
                                    Log.i("ModelDownloadBottomSheet", "Download URL: $fullUrl")

                                    Downloadable.Button(
                                        viewModel,
                                        dm,
                                        Downloadable(
                                            name = filename,
                                            source = Uri.parse(fullUrl),
                                            destination = File(extFileDir, filename)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}