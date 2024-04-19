package com.example.llama

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.example.llama.ui.theme.LlamaAndroidTheme
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.io.File

class MainActivity(
    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
): ComponentActivity() {
    private val tag: String? = this::class.simpleName

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    // Get a MemoryInfo object for the device's current memory status.
    private fun availableMemory(): ActivityManager.MemoryInfo {
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        val free = Formatter.formatFileSize(this, availableMemory().availMem)
        val total = Formatter.formatFileSize(this, availableMemory().totalMem)


        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(

            Downloadable(
                "Stable LM 2 1.6B chat (Q4_K_M, 1 GiB)",
                Uri.parse("https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true"),
                File(extFilesDir, "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf")
            ),
        )

        setContent {
            LlamaAndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF141718)
                ) {
                    MainCompose(
                        viewModel,
                        clipboardManager,
                        downloadManager,
                        models,
                    )
                }

            }
        }
    }
}

@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {


                Row(

                    modifier = Modifier
                        .background(Color(0xFF232627))
                        .padding(start = 5.dp)
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,// This will make the Row take the full width of the Box
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .padding(2.dp)
                            .size(40.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "NS GPT",
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        fontSize = 24.sp
                    )
                }


        val scrollState = rememberLazyListState()

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = scrollState) {
                itemsIndexed(viewModel.messages) { index, messageMap ->
                    val role = messageMap["role"] ?: ""
                    val content = messageMap["content"] ?: ""
                    val trimmedMessage = if (content.endsWith("\n")) {
                        content.substring(startIndex = 0, endIndex = content.length - 1)
                    } else {
                        content
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                when (role) {
                                    "user" -> Color(0xFF232627)
                                    "assistant" -> Color.Transparent
                                    else -> Color.Transparent
                                }
                            )
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Column {



                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top= 8.dp, bottom = 8.dp, start = 6.dp, end = 6.dp)){
                                    Image(
                                    painter = painterResource(id = if (role == "assistant") R.drawable.logo else R.drawable.bot_icon),
                                    contentDescription = if (role == "assistant") "Bot Icon" else "Human Icon",
                                    modifier = Modifier.size(20.dp)
                                )

                                    Image(
                                        painter = painterResource(id = if (role == "assistant") R.drawable.copy1 else R.drawable.copy1),
                                        contentDescription = if (role == "assistant") "Copy Icon" else "Copy Icon",
                                        modifier = Modifier.size(22.dp).clickable {
                                            // Copy text to clipboard
                                            clipboard.setPrimaryClip(
                                                android.content.ClipData.newPlainText("Text", content)
                                            )
                                        }
                                    )
                                }
                                Text(
                                    trimmedMessage,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color(
                                            0xFFA0A0A5
                                        )
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                )

                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.message,
                    onValueChange = { viewModel.updateMessage(it) },
                    label = { Text("Message") },
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { viewModel.send() }) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)  // Adding top margin
        ) {
            Button(
                onClick = { viewModel.clear() },
                modifier = Modifier
                    .background(Color(0xFF232627))
            ) {
                Text(
                    "Clear",
                    color = Color.White
                )
            }
            Button(
                onClick = { viewModel.stop() },
                modifier = Modifier
                    .background(Color(0xFF232627))
            ) {
                Text(
                    "Stop",
                    color = Color.White
                )
            }
            Button(
                onClick = {
                    viewModel.messages.joinToString("\n") { it["content"] ?: "" }.let {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", it))
                    }
                },
                modifier = Modifier
                    .background(Color(0xFF232627))
            ) {
                Text(
                    "Copy",
                    color = Color.White
                )
            }
        }

        Column {
            for (model in models) {
                Downloadable.Button(viewModel, dm, model)
            }
        }
    }
}

