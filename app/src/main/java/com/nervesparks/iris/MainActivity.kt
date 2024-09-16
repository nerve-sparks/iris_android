package com.nervesparks.iris

//import com.google.accompanist.systemuicontroller.rememberSystemUiController
//import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
//import android.text.format.Formatter
//import android.view.GestureDetector
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import java.io.File

class MainActivity(
    //activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
) : ComponentActivity() {

//    private val tag: String? = this::class.simpleName
//    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy {
        clipboardManager ?: getSystemService<ClipboardManager>()!!
    }

    private val viewModel: MainViewModel by viewModels()

    // Get a MemoryInfo object for the device's current memory status.
//    private fun availableMemory(): ActivityManager.MemoryInfo {
//        return ActivityManager.MemoryInfo().also { memoryInfo ->
//            activityManager.getMemoryInfo(memoryInfo)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#FF232627")//for status bar color


        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

//        val free = Formatter.formatFileSize(this, availableMemory().availMem)
//        val total = Formatter.formatFileSize(this, availableMemory().totalMem)
        val transparentColor = Color.Transparent.toArgb()
        window.decorView.rootView.setBackgroundColor(transparentColor)


        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(

            //            Downloadable(
//                "Phi-3-mini 4k Instruct(Q4 2.2 GiB)",
//                Uri.parse("https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf?download=true"),
//                File(extFilesDir, "Phi-3-mini-4k-instruct-q4.gguf")
//            ),

            Downloadable(
                "Stable LM 2 1.6B chat (Q4_K_M, 1 GiB)",
                Uri.parse("https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true"),
                File(extFilesDir, "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf")
            ),
        )
        models.forEach { model ->
            if (model.destination.exists()) {
                viewModel.load(model.destination.path)
            }
        }


        setContent {

            // A surface container using the 'background' color from the theme

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF141718),
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


@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    //val kc = LocalSoftwareKeyboardController.current
//    val systemUiController = rememberSystemUiController()
//
//    systemUiController.setSystemBarsColor(
//        color = Color(0xFF232627)
//    )

    //variable to toggle auto-scrolling
    var autoScrollEnabled by remember { mutableStateOf(true) }
//    var showModal by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    val allModelsExist = models.all { model -> model.destination.exists() }

    // Hide modal if all model destinations exist
    if (allModelsExist) {
        viewModel.showModal = false
    }


    Column(modifier = Modifier.padding(bottom = 10.dp)) {

        // Show modal if required
        if (viewModel.showModal) {
            // Modal dialog to show download options
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Download Required Models", fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(16.dp))

                        models.forEach { model ->
                            if (!model.destination.exists()) {
                                Text(text = model.name, modifier = Modifier.padding(8.dp))
                                Downloadable.Button(viewModel, dm, model)
                            }
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = { viewModel.showModal = false }) {
                            Text(text = "Close")
                        }

                    }
                }
            }
        }


        Column{

          //Top app bar starts here.
            Row(

                modifier = Modifier
                    .background(Color(0xFF232627))
                    .padding(start = 5.dp)
                    .height(60.dp)
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
                ) //Logo
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = "Iris",
                    fontWeight = FontWeight(500),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp
                ) //Name

                //New Text Button
                Button(
                    onClick = {
                        viewModel.stop()
                        viewModel.clear()
                    },
                    modifier = Modifier
                        .background(Color.Transparent),
                    colors = ButtonDefaults.buttonColors(Color.Transparent)
                ) {
                    Text(
                        "New ",
                        color = Color.White,
                        style = TextStyle(fontWeight = FontWeight.W400),
                        fontSize = 18.sp
                    )

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "newChat",
                        tint = Color.White // Optional: set the color of the icon
                    )

                }


            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.2.dp)
                    .background(color = Color.White)
            ) {}//extra spacing
        }
        //Top app bar stops here
        Divider(color = Color(0xFFA0A0A5))


        Column {


            val scrollState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            Box(modifier = Modifier.weight(1f).pointerInput(Unit) {
                detectTapGestures(
                    onTap = {autoScrollEnabled = false},
                    onDoubleTap = {autoScrollEnabled = false },
                    onLongPress = { autoScrollEnabled = false},
                    onPress = { autoScrollEnabled = false},


                )
            }) {
                LazyColumn(state = scrollState) {  //chat section starts here

                    coroutineScope.launch {

                        if(autoScrollEnabled) {
                            scrollState.scrollToItem(viewModel.messages.size)
                        }
                    }

                    itemsIndexed(viewModel.messages) { _, messageMap ->
                        val role = messageMap["role"] ?: ""
                        val content = messageMap["content"] ?: ""
                        val trimmedMessage = if (content.endsWith("\n")) {
                            content.substring(startIndex = 0, endIndex = content.length - 1)
                        } else {
                            content
                        }
                        if(role != "system") {
                            if (role != "codeBlock") {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (role) {
                                                "user" -> Color.Transparent
                                                "assistant" -> Color(0xFF232627)
                                                "log" -> Color(0xFF232627)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .fillMaxWidth()
                                        .padding(
                                            bottom = 4.dp
                                        )
                                ) {
                                    Column {


                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = 8.dp,
                                                    bottom = 8.dp,
                                                    start = 6.dp,
                                                    end = 6.dp
                                                )
                                        ) {
                                            Image(
                                                painter = painterResource(
                                                    id = if (role == "assistant" || role == "log") R.drawable.logo
                                                    else R.drawable.user_icon
                                                ),
                                                contentDescription = if (role == "assistant" || role == "log") "Bot Icon" else "Human Icon",
                                                modifier = Modifier.size(20.dp)
                                            )

                                            Image(
                                                painter = painterResource(id = R.drawable.copy1),
                                                contentDescription = "Copy Icon",
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clickable {
                                                        // Copy text to clipboard
                                                        clipboard.setPrimaryClip(
                                                            android.content.ClipData.newPlainText(
                                                                "Text",
                                                                content
                                                            )
                                                        )
                                                    }
                                            )

                                        }
                                        Text(
                                            text = if (trimmedMessage.startsWith("```")) {
                                                trimmedMessage.substring(3)
                                            } else {
                                                trimmedMessage
                                            },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color(
                                                    0xFFA0A0A5
                                                )
                                            ),
                                            modifier = Modifier.padding(start = 18.dp, end = 14.dp)

                                        )

                                    }
                                }

                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .background(
                                            Color.Black,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .fillMaxWidth()

                                ) {
                                    Column {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = 8.dp,
                                                    bottom = 8.dp,
                                                    start = 6.dp,
                                                    end = 6.dp
                                                )
                                        ) {


                                            Image(
                                                painter = painterResource(id = R.drawable.copy1),
                                                contentDescription = "Copy Icon",
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clickable {
                                                        // Copy text to clipboard
                                                        clipboard.setPrimaryClip(
                                                            android.content.ClipData.newPlainText(
                                                                "Text",
                                                                content
                                                            )
                                                        )
                                                    }
                                            )

                                        }
                                        Text(
                                            text = if (trimmedMessage.startsWith("```")) {
                                                trimmedMessage.substring(3)
                                            } else {
                                                trimmedMessage
                                            },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color(
                                                    0xFFA0A0A5
                                                )
                                            ),
                                            modifier = Modifier.padding(16.dp) // Add padding for content
                                        )
                                    }


                                }

                            }
                        }
                    }
                } //chat section ends here
            }
            //Prompt input field
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
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            focusedLabelColor = Color.White,
                            cursorColor = Color.White
                        ),
//                        keyboardOptions = KeyboardOptions(
//                            keyboardType = KeyboardType.Ascii,
//                            imeAction = ImeAction.Done
//                        ),
//                        keyboardActions = KeyboardActions(
//                            onDone = {
//                                //kc?.show()
//                                //kc?.hide()
//                            },
//
//                        ),

                    )
                    if (!viewModel.getIsSending()) {

                        IconButton(onClick = {
                            autoScrollEnabled = true
                            viewModel.send()
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                            )
                        }
                    } else if (viewModel.getIsSending()) {
                        IconButton(onClick = { viewModel.stop() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                            )
                        }
                    }

                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)  // Adding top margin
            ) {
//                        Button(
//                            onClick = { viewModel.clear() },
//                            modifier = Modifier
//                                .background(Color(0xFF232627))
//                        ) {
//                            Text(
//                                "Clear",
//                                color = Color.White
//                            )
//                        }
//                        Button(
//                            onClick = { viewModel.stop() },
//                            modifier = Modifier
//                                .background(Color(0xFF232627))
//                        ) {
//                            Text(
//                                "Stop",
//                                color = Color.White
//                            )
//                        }

            }

            Column {
                for (model in models) {
                    Downloadable.Button(viewModel, dm, model)
                }
            }
        }
    }

}

