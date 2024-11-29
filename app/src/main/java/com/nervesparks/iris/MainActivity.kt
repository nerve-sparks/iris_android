package com.nervesparks.iris

//import com.google.accompanist.systemuicontroller.rememberSystemUiController
//import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipboardManager
import android.net.Uri
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.style.BackgroundColorSpan
//import android.text.format.Formatter
//import android.view.GestureDetector
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.io.File
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.loader.content.Loader


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



//        val free = Formatter.formatFileSize(this, availableMemory().availMem)
//        val total = Formatter.formatFileSize(this, availableMemory().totalMem)
        val transparentColor = Color.Transparent.toArgb()
        val darkNavyBlue = Color(0xFF001F3D) // Dark navy blue color
        val lightNavyBlue = Color(0xFF3A4C7C)

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(darkNavyBlue, lightNavyBlue)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#FF232627")//for status bar color


        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )
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
                color = MaterialTheme.colorScheme.background
            ) {
                LinearGradient()
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
fun LinearGradient() {
    val darkNavyBlue = Color(0xFF050a14)
    val lightNavyBlue = Color(0xFF051633)
    val gradient = Brush.linearGradient(
        colors = listOf(darkNavyBlue, lightNavyBlue),
                start = Offset(4f, 0f),
                end = Offset(0f, 1000f)

    )
    Box(modifier = Modifier.background(gradient))
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
    val Prompts = listOf("Today's match score ", "Tell me more about ..", "Can you tell me about your services?" , "I need help with an issue I’m facing. Can you assist me?" , "What’s the capital of France?", "I’d like to schedule an appointment for ",
            "What are the top 5 things to do in Paris?" , "What are some good exercises to improve my posture?" , "Can you recommend some good books/movies based on ?" , "Can you translate this sentence into Spanish?" , "Tell me about the latest news.")
    val allModelsExist = models.all { model -> model.destination.exists() }

    val Prompts_Home = listOf("Explain quantum computing in simple terms", "Remember what user said earlier!!", "May occasionally generate incorrect")

    // Hide modal if all model destinations exist
    if (allModelsExist) {
        viewModel.showModal = false
    }

Box(
    modifier = Modifier
        .fillMaxSize(),
){
//    Image(
//        painter = painterResource(id = R.drawable.logo),
//        contentDescription = "Centered Background Logo",
//        modifier = Modifier
//            .align(Alignment.Center)
//            .size(50.dp),
//        contentScale = ContentScale.Fit
//    )
    Column(modifier = Modifier.padding()) {

        // Show modal if required
        if (viewModel.showModal) {
            // Modal dialog to show download options
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.LightGray,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally

                    ) {
                        Text(text = "Download Required", fontWeight = FontWeight.Bold,color = Color(0xFFDC3911))
                        Text(text = "Don't close or minimize the app!", fontWeight = FontWeight.Bold, color = Color(0xFFDC3911))
                        Spacer(modifier = Modifier.height(16.dp))

                        models.forEach { model ->
                            if (!model.destination.exists()) {
                                Text(text = model.name, modifier = Modifier.padding(5.dp))
                                Downloadable.Button(viewModel, dm, model)
                            }
                        }


                        Spacer(modifier = Modifier.height(16.dp))

//                        TextButton(onClick = { viewModel.showModal = false }) {
//                            Text(text = "Close")
//                        }

                    }
                }
            }
        }
        if (viewModel.showAlert) {
            // Modal dialog to show download options
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF01081a),
                    modifier = Modifier
                        .padding(10.dp)
                        .alpha(0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .wrapContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ){
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            )
                            {
                                Text(text = "Loading Model \n" +
                                        "Please wait...",
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                        }
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            color = Color(0xFF17246a)
                        )
                    }
                }
            }
        }



//        Column{
//
//          //Top app bar starts here.
//            Row(
//
//                modifier = Modifier
//                    .background(Color(0xFF232627))
//                    .padding(start = 5.dp)
//                    .height(60.dp)
//                    .fillMaxWidth(),
//                horizontalArrangement = Arrangement.Center,
//
//                verticalAlignment = Alignment.CenterVertically,// This will make the Row take the full width of the Box
//            ) {
//                Image(
//                    painter = painterResource(id = R.drawable.logo),
//                    contentDescription = "Logo",
//                    modifier = Modifier
//                        .padding(2.dp)
//                        .size(40.dp)
//                ) //Logo
//                Spacer(modifier = Modifier.padding(4.dp))
//                Text(
//                    text = "Iris",
//                    fontWeight = FontWeight(500),
//                    color = Color.White,
//                    modifier = Modifier.weight(1f),
//                    fontSize = 24.sp
//                ) //Name
//
//                //New Text Button
////                Button(
////                    onClick = {
////                        viewModel.stop()
////                        viewModel.clear()
////                    },
////                    modifier = Modifier
////                        .background(Color.Transparent),
////                    colors = ButtonDefaults.buttonColors(Color.Transparent)
////                ) {
////                    Text(
////                        "New ",
////                        color = Color.White,
////                        style = TextStyle(fontWeight = FontWeight.W400),
////                        fontSize = 18.sp
////                    )
////
////                    Icon(
////                        imageVector = Icons.Default.Add,
////                        contentDescription = "newChat",
////                        tint = Color.White // Optional: set the color of the icon
////                    )
////
////                }
//
//
//            }
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(0.2.dp)
//                    .background(color = Color.White)
//            ) {}//extra spacing
//        }
        //Top app bar stops here
//        Divider(color = Color(0xFFA0A0A5))


        //New Chat Button
//        Column {
//            Box(
//                modifier = Modifier
////                    .background(Color(0xFF232627))
//                    .padding(start = 20.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
//                    .fillMaxWidth()
//                    .height(60.dp),
//                contentAlignment = Alignment.CenterStart
//            ) {
//                Button(
//                    onClick = {
//                        viewModel.stop()
//                        viewModel.clear()
//                    },
//                    modifier = Modifier
//                        .height(45.dp)
//                        .padding(0.dp),
//
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C61DD)),
//                    shape = RoundedCornerShape(22.dp),
//                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .background(Color(0xFF000000), shape = RoundedCornerShape(20.dp))
//                            .height(24.dp)
//                            .padding(2.dp)
//
//                    ) {
//                        Icon(
//
//                            imageVector = Icons.Default.Add,
//                            contentDescription = "newChat",
//                            tint = Color.White
//                        )
//                    }
//                    Spacer(modifier = Modifier.width(6.dp))
//                    Text(
//                        "New Chat",
//                        color = Color.White,
//                        style = TextStyle(fontWeight = FontWeight.W400),
//                        fontSize = 15.sp
//                    )
//                }
//            }
//        }
        Column {
            val scrollState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            Box(modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { autoScrollEnabled = false },
                        onDoubleTap = { autoScrollEnabled = false },
                        onLongPress = { autoScrollEnabled = false },
                        onPress = { autoScrollEnabled = false },


                        )
                }) {
                if (viewModel.messages.size == 0) {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize() // Take up the whole screen
                            .wrapContentHeight(Alignment.CenterVertically),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header Text
                        item {
                            Text(
                                text = "Hello, Ask me " + "Anything..",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.W500,
                                    letterSpacing = 1.sp,
                                    fontSize = 50.sp,
                                    lineHeight = 60.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .wrapContentHeight()
                            )
                        }

                        // Items for Prompts_Home
                        items(Prompts_Home.size) { index ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .padding(8.dp)
                                    .background(
                                        Color(0xFF01081a),
                                        shape = RoundedCornerShape(30.dp)
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    // Circle Icon
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp) // Icon size
                                            .background(Color.White, shape = CircleShape)
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Text
                                    Text(
                                        text = Prompts_Home.getOrNull(index) ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                        textAlign = TextAlign.Start, // Left align the text
                                        fontSize = 15.sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                }
                else {
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
                                                 "assistant" -> Color(0xFF1c1c1e)
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
                                                 contentDescription = "Copy Icon user",
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
                 }
             }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp), // Reduced space between cards
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(Prompts.size) { index ->
                    if(viewModel.messages.size <= 1){
                        Card(
                            modifier = Modifier
                                .height(100.dp)
                                .padding(horizontal = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF01081a))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = Prompts[index],
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFA0A0A5),
                                        fontSize = 15.sp,),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(100.dp)
                                        .padding(horizontal = 15.dp, vertical = 12.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            //chat section ends here


            //Prompt input field
            Box(modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFF090d17))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 8.dp, bottom = 8.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,

                    ) {
//                    OutlinedTextField(
//                        value = viewModel.message,
//                        onValueChange = { viewModel.updateMessage(it) },
//                        label = { Text("Message") },
//                        modifier = Modifier
////                            .weight(1f)
//                            .background(Color(0xFF141A26), shape = RoundedCornerShape(22.dp))
//
//                           ,
//                        shape = RoundedCornerShape(size = 22.dp),
//                        colors = OutlinedTextFieldDefaults.colors(
//                            focusedTextColor = Color.White,
//                            focusedBorderColor = Color.White,
//                            focusedLabelColor = Color.White,
//                            cursorColor = Color.White,
//
//                        ),

                    TextField(
                        value = viewModel.message,
                        onValueChange = { viewModel.updateMessage(it) },
//                        label = { Text("Message") } ,
                        placeholder = { Text("Message") },
                        modifier = Modifier
                            .weight(2f),
                        shape = RoundedCornerShape(size = 20.dp),
                        colors = TextFieldDefaults.colors(

                            focusedTextColor = Color(0xFFADB2B8),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent, // Optional, makes the indicator disappear
                            focusedLabelColor = Color(0xFF626568),
                            cursorColor = Color(0xFF626568),
                            unfocusedContainerColor = Color(0xFF13203b),
                            focusedContainerColor=Color(0xFF13203b)
                        )
                    )

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


                    if (!viewModel.getIsSending()) {

                        IconButton(onClick = {
                            autoScrollEnabled = true
                            viewModel.send()
                            focusManager.clearFocus()
                        }) {
                            Icon(
//                                imageVector = Icons.Default.Send,
                                modifier = Modifier
                                    .size(32.dp)
                                    .weight(1f),
                                painter = painterResource(id = R.drawable.send_2_svgrepo_com),
                                contentDescription = "Send",
                                tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                            )
                        }
                    } else if (viewModel.getIsSending()) {
                        IconButton(onClick = { viewModel.stop() }) {
                            Icon(
                                modifier = Modifier
                                    .weight(1f),
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                            )
                        }
                    }

                }
            }

//            Row(
//                horizontalArrangement = Arrangement.SpaceEvenly,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 1.dp)  // Adding top margin
//            ) {
////                        Button(
////                            onClick = { viewModel.clear() },
////                            modifier = Modifier
////                                .background(Color(0xFF232627))
////                        ) {
////                            Text(
////                                "Clear",
////                                color = Color.White
////                            )
////                        }
////                        Button(
////                            onClick = { viewModel.stop() },
////                            modifier = Modifier
////                                .background(Color(0xFF232627))
////                        ) {
////                            Text(
////                                "Stop",
////                                color = Color.White
////                            )
////                        }
//
//            }

//            Column {
//                for (model in models) {
//                    Downloadable.Button(viewModel, dm, model)
//                }
//            }
        }
    }

}
}



