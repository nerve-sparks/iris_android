package com.nervesparks.iris

//import com.example.llama.ui.theme.LlamaAndroidTheme
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.text.format.Formatter
import android.transition.Transition
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import java.io.File
import java.security.AccessController.getContext
import kotlin.math.log


class MainActivity(
//    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
): ComponentActivity() {
//    private val tag: String? = this::class.simpleName
//
//    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    // Get a MemoryInfo object for the device's current memory status.
//    private fun availableMemory(): ActivityManager.MemoryInfo {
//        return ActivityManager.MemoryInfo().also { memoryInfo ->
//            activityManager.getMemoryInfo(memoryInfo)
//        }
//    }

    val darkNavyBlue = Color(0xFF001F3D) // Dark navy blue color
    val lightNavyBlue = Color(0xFF3A4C7C)



    val gradientBrush = Brush.verticalGradient(
        colors = listOf(darkNavyBlue, lightNavyBlue)
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#FF070915")//for status bar color

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

//        val free = Formatter.formatFileSize(this, availableMemory().availMem)
//        val total = Formatter.formatFileSize(this, availableMemory().totalMem)
        val transparentColor = Color.Transparent.toArgb()
        window.decorView.rootView.setBackgroundColor(transparentColor)
//        viewModel.log("Current memory: $free / $total")
//        viewModel.log("Downloads directory: ${getExternalFilesDir(null)}")


        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(
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

//    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
//        if (event.action == MotionEvent.ACTION_DOWN) {
//
//                val focusedView = currentFocus
//
//            if (focusedView != null && focusedView !is TextField) {
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
//                focusedView.clearFocus()
//            }
//        }
//        return super.dispatchTouchEvent(event)
//    }


}

@Composable
fun LinearGradient() {
    val darkNavyBlue = Color(0xFF050a14)
    val lightNavyBlue = Color(0xFF051633)
    val gradient = Brush.linearGradient(
        colors = listOf(darkNavyBlue, lightNavyBlue),
        start = Offset(0f, 300f),
        end = Offset(0f, 1000f)

    )
    Box(modifier = Modifier.background(gradient))
}




@OptIn(ExperimentalFoundationApi::class)
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val focusRequester = FocusRequester()
    var isFocused by remember { mutableStateOf(false) }
    // Hide modal if all model destinations exist
    if (allModelsExist) {
        viewModel.showModal = false
    }


    Box(
        modifier = if(!viewModel.showModal || viewModel.showAlert) {
            Modifier.fillMaxSize()} else{
                Modifier
                .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    println("TAP in parent Box ontap")

                    if (isFocused) {
                        focusManager.clearFocus()
                        isFocused = false
                    }
                }, onDoubleTap = {
                    println("TAP in parent Box dd tap")

                    if (isFocused) {
                        focusManager.clearFocus()
                        isFocused = false
                    }
                },onPress={
                    println("TAP in parent Box onpress")

                    if (isFocused) {
                        focusManager.clearFocus()
                        isFocused = false
                    }
                }, onLongPress = {
                    println("TAP in parent Box onlongpress")

                    if (isFocused) {
                        focusManager.clearFocus()
                        isFocused = false
                    }
                })
            }
        }


    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(

            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(300.dp)    // or your desired width
                        .fillMaxHeight(),
                    drawerContainerColor=Color(0xFF070915),

                ) {
                    /*Drawer content */
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxHeight()
                        ,

                        verticalArrangement = Arrangement.SpaceBetween

                    ) {
                        // top logo ,name of app
                        Column(
                        ){
                            Row(
                                verticalAlignment = Alignment.CenterVertically,

                                ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Centered Background Logo",
                                    modifier = Modifier
                                        .size(30.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.padding(5.dp))
                                Text(
                                    text = "Iris",
                                    fontWeight = FontWeight(500),
                                    color = Color.White,
//                            modifier = Modifier.weight(),
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
//                                modifier = Modifier.padding(end = 20.dp),
                                text= "powered by",
                                color = Color(0xFF636466),
                                fontSize = 10.sp
                            )
                            Text(

                                text= " llama.cpp",
                                color = Color(0xFF78797a),
                                fontSize = 12.sp
                            )
                        }
                    }

                }
            },
        ) {


           // Screen content
            Column() {

                // Show modal if required
                if (viewModel.showModal) {
                    // Modal dialog to show download options
                    Dialog(onDismissRequest = {}) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black,
                            modifier = Modifier
                                .padding(10.dp)
                                .height(230.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .height(140.dp)
                                    ,
                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {
                                Text(
                                    text = "Download Required",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Don't close or minimize the app!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(35.dp))

                                models.forEach { model ->
                                    if (!model.destination.exists()) {
//                                        Text(text = model.name, modifier = Modifier.padding(9.dp))
                                        Downloadable.Button(viewModel, dm, model)
                                    }
                                }


                                Spacer(modifier = Modifier.height(20.dp))

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
                                    Text(
                                        text = "Loading Model \n" +
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

                Column {

                    //Top app bar starts here.
                    Row(

                        modifier = Modifier
                            .background(color = Color.Transparent)
                            .padding(start = 20.dp, end = 10.dp)
                            .height(60.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically,// This will make the Row take the full width of the Box
                    ) {
//                Image(
//                    painter = painterResource(id = R.drawable.logo),
//                    contentDescription = "Logo",
//                    modifier = Modifier
//                        .padding(2.dp)
//                        .size(40.dp)
//                )
//
//
//                  icon of drawer
                        Button(
                            onClick = {
                                scope.launch {
                                    drawerState.apply {
                                        if (isClosed) open() else close()
                                    }
                                }
                            },
                            modifier = Modifier
                                .height(26.dp)
                                .padding(0.dp),

                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 1.dp, vertical = 0.dp)
                        ) {

                            Icon(
                                painter = painterResource(id= R.drawable.burger_menu_left_svgrepo_com),
                                contentDescription = "open side drawer",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                            Text(
                                text = "Iris",
                                fontWeight = FontWeight(500),
                                color = Color.White,
//                            modifier = Modifier.weight(),
                                fontSize = 24.sp
                            )
                        }



                        //New Text Button
//                Button(
//                    onClick = {
//                        viewModel.stop()
//                        viewModel.clear()
//                    },
//                    modifier = Modifier
//                        .background(Color.Transparent),
//                    colors = ButtonDefaults.buttonColors(Color.Transparent)
//                ) {
//                    Text(
//                        "New ",
//                        color = Color.White,
//                        style = TextStyle(fontWeight = FontWeight.W400),
//                        fontSize = 18.sp
//                    )
//
//                    Icon(
//                        imageVector = Icons.Default.Add,
//                        contentDescription = "newChat",
//                        tint = Color.White // Optional: set the color of the icon
//                    )
//
//                }
                        //New Chat Button

                        Button(
                            onClick = {
                                viewModel.stop()
                                viewModel.clear()
                            },
                            modifier = Modifier
                                .height(26.dp)
                                .padding(0.dp),

                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
//                                    shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(horizontal = 1.dp, vertical = 0.dp)
                        ) {

                            Icon(

                                painter = painterResource(id = R.drawable.edit_3_svgrepo_com),
                                contentDescription = "newChat",
                                tint = Color.White
                            )
                        }
                    }
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(0.2.dp)
//                            .background(color = Color.White)
//                    ) {}//extra spacing
                }
                //Top app bar stops here



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
//                        .blur(5.dp, BlurredEdgeTreatment.Rectangle)

                        if (viewModel.messages.size == 0 && viewModel.showModal==false && viewModel.showAlert ==false) {
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

                                item{

                                }
                            }
                        }
                        else {
                            LazyColumn(state = scrollState) {  //chat section starts here

                                coroutineScope.launch {

                                    if (autoScrollEnabled) {
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
                                    if (role != "system") {
                                        if (role != "codeBlock") {

                                            Box(
                                                modifier = Modifier

//                                                .padding(
//                                                    end = if (role == "user") 8.dp else 64.dp, // Margin for user
//                                                    start = if (role == "assistant") 8.dp else 64.dp // Margin for assistant
//                                                )
//                                                .widthIn(min = 50.dp, max = 300.dp) // Dynamic bubble width
//                                                .background(
//                                                    color = if (role == "user") Color.LightGray else Color(0xFF232627), // Light gray for user, dark for assistant
//                                                    shape = RoundedCornerShape(12.dp) // Rounded corners for bubble
//                                                )

                                            )
                                            {
                                                val context = LocalContext.current
                                                Row(
                                                    horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 8.dp),

                                                    ) {
                                                    val interactionSource = remember { MutableInteractionSource() }

                                                    if(role == "assistant") {
                                                        Image(
                                                            painter = painterResource(
                                                                id = R.drawable.logo
                                                            ),
                                                            contentDescription =  "Bot Icon",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Box( modifier = Modifier
                                                        .padding(horizontal = 8.dp)
                                                        .background(
                                                            color = if (role == "user") Color(
                                                                0xFF171E2C
                                                            ) else Color.Transparent,
                                                            shape = RoundedCornerShape(12.dp),
                                                        )
                                                        .combinedClickable(
                                                            interactionSource = interactionSource,
                                                            indication = ripple(color = Color.Gray),
                                                            onLongClick = {
                                                                clipboard.setText(
                                                                    AnnotatedString(trimmedMessage)
                                                                )
                                                                Toast.makeText(context, "text copied!!", Toast.LENGTH_LONG).show()
                                                            },
                                                            onClick = {}
                                                        )
                                                    )
                                                    {

                                                        Row(
                                                            modifier = Modifier
                                                                .padding(5.dp)
                                                        ) {


                                                            Box(
                                                                modifier = Modifier
                                                                    .widthIn(max = 300.dp)
                                                                    .padding(3.dp)

                                                            ){
                                                                Text(
                                                                    text = if (trimmedMessage.startsWith("```")) {
                                                                        trimmedMessage.substring(3)
                                                                    } else {
                                                                        trimmedMessage
                                                                    },
                                                                    style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFA0A0A5)),
                                                                    modifier = Modifier
                                                                        .padding(start = 18.dp)

                                                                )
                                                            }
                                                        }


                                                    }
                                                    if(role == "user") {
                                                        Image(
                                                            painter = painterResource(
                                                                id = R.drawable.user_icon
                                                            ),
                                                            contentDescription = "Human Icon",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }



                                                }


//                                            }
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
                                                        horizontalArrangement = Arrangement.End,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(
                                                                top = 8.dp,
                                                                bottom = 8.dp,
                                                                start = 6.dp,
                                                                end = 6.dp
                                                            )
                                                    ) {


//                                                        Image(
//                                                            painter = painterResource(id = R.drawable.copy1),
//                                                            contentDescription = "Copy Icon user",
//                                                            modifier = Modifier
//                                                                .size(22.dp)
//                                                                .clickable {
//                                                                    // Copy text to clipboard
//                                                                    clipboard.setPrimaryClip(
//                                                                        android.content.ClipData.newPlainText(
//                                                                            "Text",
//                                                                            content
//                                                                        )
//                                                                    )
//                                                                }
//                                                        )

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

                         //chat section ends here
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
                                    Button( onClick = {
                                        viewModel.updateMessage(Prompts[index])
                                        focusRequester.requestFocus()
                                    },
//                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01081a)),
                                        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 0.dp)

                                    ) {
                                        Text(
                                            text = Prompts[index],
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFA0A0A5),
                                                fontSize = 15.sp,
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .width(200.dp)
                                                .height(100.dp)
                                                .padding(horizontal = 15.dp, vertical = 12.dp)
//                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    //Prompt input field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF050B16))

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
                                value = TextFieldValue(text = viewModel.message, selection = TextRange(viewModel.message.length)),
                                onValueChange = { viewModel.updateMessage(it.text) },

//                        label = { Text("Message") } ,
                                placeholder = { Text("Message") },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        isFocused = focusState.isFocused
                                    },

                                shape = RoundedCornerShape(size = 18.dp),
                                colors = TextFieldDefaults.colors(

                                    focusedTextColor = Color(0xFFBECBD1),
                                    unfocusedTextColor = Color(0xFFBECBD1),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent, // Optional, makes the indicator disappear
                                    focusedLabelColor = Color(0xFF626568),
                                    cursorColor = Color(0xFF626568),
                                    unfocusedContainerColor = Color(0xFF171E2C),
                                    focusedContainerColor = Color(0xFF22314A)
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
}


// [END android_compose_layout_material_modal_drawer]









