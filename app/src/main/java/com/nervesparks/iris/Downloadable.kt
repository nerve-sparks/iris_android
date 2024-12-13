package com.nervesparks.iris

import android.app.DownloadManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.database.getLongOrNull
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class Downloadable(val name: String, val source: Uri, val destination: File) {
    companion object {
        @JvmStatic
        private val tag: String? = this::class.qualifiedName

        sealed interface State
        data object Ready : State
        data class Downloading(val id: Long, val totalSize: Long) : State
        data class Downloaded(val downloadable: Downloadable) : State
        data class Error(val message: String) : State
        data object Stopped : State

        @JvmStatic
        @Composable
        fun Button(viewModel: MainViewModel, dm: DownloadManager, item: Downloadable) {
            var status: State by remember {
                mutableStateOf(
                    if (item.destination.exists()) Downloaded(item)
                    else Ready
                )
            }
            var progress by remember { mutableDoubleStateOf(0.0) }
            var totalSize by remember { mutableStateOf<Long?>(null) }

            val coroutineScope = rememberCoroutineScope()

            suspend fun waitForDownload(result: Downloading, item: Downloadable): State {
                while (true) {
                    val cursor = dm.query(DownloadManager.Query().setFilterById(result.id))

                    if (cursor == null) {
                        Log.e(tag, "dm.query() returned null")
                        return Error("dm.query() returned null")
                    }

                    if (!cursor.moveToFirst() || cursor.count < 1) {
                        cursor.close()
                        Log.i(tag, "cursor.moveToFirst() returned false or cursor.count < 1, download canceled?")
                        return Ready
                    }

                    val pix = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val tix = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val sofar = cursor.getLongOrNull(pix) ?: 0
                    val total = cursor.getLongOrNull(tix) ?: 1
                    totalSize = total
                    cursor.close()

                    if (sofar == total) {
                        Log.d(tag, "Download complete: ${item.destination.path}")

                        // Ensure model is added dynamically
                        withContext(Dispatchers.Main) {
                            if (!viewModel.allModels.any { it["name"] == item.name }) {
                                val newModel = mapOf(
                                    "name" to item.name,
                                    "source" to item.source.toString(),
                                    "destination" to item.destination.path
                                )
                                viewModel.allModels = viewModel.allModels + newModel
                                Log.d(tag, "Model dynamically added to viewModel: $newModel")
                            }
                        }

                        viewModel.load(item.destination.path)
                        return Downloaded(item)
                    }

                    progress = (sofar * 1.0) / total
                    delay(1000L)
                }
            }


            fun onClick() {
                when (val s = status) {
                    is Downloaded -> {
                        viewModel.showModal = true
                        Log.d("item.destination.path", item.destination.path.toString())
                        viewModel.load(item.destination.path)
                    }

                    is Downloading -> {
                        coroutineScope.launch {
                            status = waitForDownload(s, item)
                        }
                    }

                    is Stopped -> {
                        // Handle the paused or stopped download state.
                        Log.i(tag, "Download has been stopped for ${item.name}")
                        status = Ready
                    }

                    else -> {
                        item.destination.delete()

                        val request = DownloadManager.Request(item.source).apply {
                            setTitle("Downloading model")
                            setDescription("Downloading model: ${item.name}")
                            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                            setDestinationUri(item.destination.toUri())
                        }

                        viewModel.log("Saving ${item.name} to ${item.destination.path}")
                        Log.i(
                            tag,
                            "Saving ${item.name} to ${item.destination.path} \n Download only on Wifi. \n"
                        )

                        val id = dm.enqueue(request)
                        status = Downloading(id, totalSize ?: -1L)
                        coroutineScope.launch {
                            status = waitForDownload(Downloading(id, totalSize ?: -1L), item)
                        }
                    }
                }
            }

            fun onStop() {
                if (status is Downloading) {
                    dm.remove((status as Downloading).id)
                    status = Stopped
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onClick() },
                    enabled = status !is Downloading && !viewModel.getIsSending(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF141414) // Navy Blue color
                    )
                ) {
                    when (status) {
                        is Downloading -> Text(
                            text = buildAnnotatedString {
                                append("Downloading ")
                                withStyle(style = SpanStyle(color = Color.Cyan)) {
                                    append("${(progress * 100).toInt()}%")
                                }
                            },
                            color = Color.White
                        )

                        is Downloaded -> Text(
                            "Load ${item.name}",
                            color = Color.White
                        )

                        is Ready -> Text(
                            "Download ${item.name}",
                            color = Color.White
                        )

                        is Error -> Text(
                            "Download ${item.name}",
                            color = Color.White
                        )

                        is Stopped -> Text(
                            "Stopped",
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (status is Downloading) {
                    Button(
                        onClick = { onStop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White // Red color for stop button
                        )
                    ) {
                        Text("Stop Download", color = Color.Black)
                    }
                }

                totalSize?.let {
                    Text(
                        text = "File size: ${it / (1024 * 1024)} MB",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


