package com.nervesparks.iris

import android.app.DownloadManager
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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

            var status: State by remember  {
                mutableStateOf(
                    when (val downloadId = getActiveDownloadId(dm, item)) {
                        null -> {

                            if (item.destination.exists() && item.destination.length() > 0 && !isPartialDownload(item.destination)) {
                                Downloaded(item)
                            } else {
                                Ready
                            }
                        }
                        else -> Downloading(downloadId, -1L)
                    }
                )
            }
            var progress by rememberSaveable  { mutableDoubleStateOf(0.0) }
            var totalSize by rememberSaveable  { mutableStateOf<Long?>(null) }

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

//                         Ensure model is added dynamically
                        withContext(Dispatchers.Main) {
                            if (!viewModel.allModels.any { it["name"] == item.name }) {
                                println("testing")
                                println(item.source.toString())
                                val newModel = mapOf(
                                    "name" to item.name,
                                    "source" to item.source.toString(),
                                    "destination" to item.destination.path
                                )
                                viewModel.allModels = viewModel.allModels + newModel
                                Log.d(tag, "Model dynamically added to viewModel: $newModel")
                            }
                        }
//                        val newModel = mapOf(
//                            "name" to item.name,
//                            "source" to item.source.toString(),
//                            "destination" to item.destination.path
//                        )
//                        viewModel.allModels = viewModel.allModels + newModel
//                        Log.d(tag, "Model dynamically added to viewModel: $newModel")

                        viewModel.currentDownloadable = item
                        if(viewModel.loadedModelName.value == "") {
                            viewModel.load(
                                item.destination.path,
                                userThreads = viewModel.user_thread.toInt()
                            )
                        }

                        println(viewModel.allModels.any {it["name"] == item.name})
                        if (!viewModel.allModels.any { it["name"] == item.name }) {
                            val newModel = mapOf(
                                "name" to item.name,
                                "source" to item.source.toString(),
                                "destination" to item.destination.path
                            )
                            viewModel.allModels += newModel
                            Log.d(tag, "Outer : Model dynamically added to viewModel: $newModel")
                        }
                        return Downloaded(item)
                    }

                    progress = (sofar * 1.0) / total
                    delay(1000L)
                }
            }

            LaunchedEffect(status) {
                if (status is Downloading) {
                    status = waitForDownload(status as Downloading, item)
                }
            }
            fun onClick() {
                when (val s = status) {
                    is Downloaded -> {
                        viewModel.showModal = true
                        Log.d("item.destination.path", item.destination.path.toString())
                        viewModel.currentDownloadable = item
                        viewModel.load(item.destination.path, userThreads = viewModel.user_thread.toInt())
                    }

                    is Downloading -> {
                        Log.d("Downloading", "Already downloading in background")
                    }

                    else -> {
                        val request = DownloadManager.Request(item.source).apply {
                            setTitle("Downloading model")
                            setDescription("Downloading model: ${item.name}")
                            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                            setDestinationUri(item.destination.toUri())
                        }

                        val id = dm.enqueue(request)
                        status = Downloading(id, -1L) // Dynamically update status
                        coroutineScope.launch {
                            status = waitForDownload(Downloading(id, -1L), item)
                        }
                    }
                }
            }


            fun onStop() {
                if (status is Downloading) {
                    dm.remove((status as Downloading).id)
                    status = Ready
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { onClick() },
                    enabled = status !is Downloading && !viewModel.getIsSending(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB) // Navy Blue color
                    ),

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
                            "Load",
                            color = Color.White
                        )

                        is Ready -> Text(
                            "Download",
                            color = Color.White
                        )

                        is Error -> Text(
                            "Download}",
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


fun isAlreadyDownloading(dm: DownloadManager, item: Downloadable): Boolean {
    val query = DownloadManager.Query()
        .setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING)

    val cursor = dm.query(query)

    cursor?.use {
        while (it.moveToNext()) {
            val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_URI)
            val currentUri = it.getString(uriIndex)
            if (currentUri == item.source.toString()) {
                Log.d("CheckDownload", "Item is already downloading or pending.")
                return true
            }
        }
    }
    return false
}

private fun isPartialDownload(file: File): Boolean {

    return file.name.endsWith(".partial") ||
            file.name.endsWith(".download") ||
            file.name.endsWith(".tmp") ||

            file.name.contains(".part")
}

fun getActiveDownloadId(dm: DownloadManager, item: Downloadable): Long? {
    val query = DownloadManager.Query()
        .setFilterByStatus(
            DownloadManager.STATUS_RUNNING or
                    DownloadManager.STATUS_PENDING or
                    DownloadManager.STATUS_PAUSED
        )

    dm.query(query)?.use { cursor ->
        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
        val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)

        while (cursor.moveToNext()) {
            val currentUri = cursor.getString(uriIndex)
            if (currentUri == item.source.toString()) {
                return cursor.getLong(idIndex)
            }
        }
    }
    return null
}