package com.nervesparks.iris

//import com.example.llama.ui.theme.LlamaAndroidTheme
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.speech.RecognizerIntent
import android.text.Editable
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.text.format.Formatter
import android.transition.Transition
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.io.File
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.lifecycle.viewModelScope
import java.security.AccessController.getContext
import java.util.Locale
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
                "Llama 3.2 1B Instruct (Q6_K_L, 1.09 GiB)",
                Uri.parse("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true"),
                File(extFilesDir, "Llama-3.2-1B-Instruct-Q6_K_L.gguf")
            ),
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




@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    val kc = LocalSoftwareKeyboardController.current
//    val systemUiController = rememberSystemUiController()
//
//    systemUiController.setSystemBarsColor(
//        color = Color(0xFF232627)
//    )

    //variable to toggle auto-scrolling

//    var showModal by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    val Prompts = listOf("Today's match score ", "Tell me more about ..", "Can you tell me about your services?" , "I need help with an issue I’m facing. Can you assist me?" , "What’s the capital of France?", "I’d like to schedule an appointment for ",
        "What are the top 5 things to do in Paris?" , "What are some good exercises to improve my posture?" , "Can you recommend some good books/movies based on ?" , "Can you translate this sentence into Spanish?" , "Tell me about the latest news.")

    val allModelsExist = models.all { model -> model.destination.exists() }
    val Prompts_Home = listOf("Explain quantum computing in simple terms", "Remember what user said earlier!!", "May occasionally generate incorrect")
    var recognizedText by remember {mutableStateOf("")}
    val speechRecognizerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        recognizedText = results?.get(0)?:""
        viewModel.updateMessage(recognizedText)

    }
    val fontname = GoogleFont("Jost")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val focusRequester = FocusRequester()
    var isFocused by remember { mutableStateOf(false) }
    var isTappedInsideTextField by remember { mutableStateOf(false) }
    var textFieldBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    // Hide modal if all model destinations exist
    if (allModelsExist) {
        viewModel.showModal = false
    }
    var isSpeaking by remember { mutableStateOf(false) }

    fun scrollToBottom(scrollState: LazyListState) {
        viewModel.viewModelScope.launch {
            scrollState.scrollToItem(viewModel.messages.size - 1)
        }
    }


    Box() {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(

            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    drawerContainerColor=Color(0xFF070915),

                ) {
                    /*Drawer content wrapper */
                    Column(
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxHeight(),
                    ) {
                        // Top section with logo and name
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Centered Background Logo",
                                    modifier = Modifier.size(35.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.padding(5.dp))
                                Text(
                                    text = "Iris",
                                    fontWeight = FontWeight(500),
                                    color = Color.White,
                                    fontSize = 30.sp
                                )
                            }
                            Row(
                                modifier = Modifier.padding(start = 45.dp)
                            ) {
                                Text(
                                    text = "NerveSparks",
                                    color = Color(0xFF636466),
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // This will push the buttons to the bottom
                        Spacer(modifier = Modifier.weight(1f))

                        // Bottom section with link buttons
                        Column(
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Star us button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = Color(0xFF22314A),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                            ) {
                                val context = LocalContext.current
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().clickable {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://github.com/nerve-sparks/iris_android")
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text(
                                        text = "Star us",
                                        color = Color(0xFF78797a),
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.width(8.dp))

                                    Image(
                                        modifier = Modifier
                                            .size(24.dp),
                                        painter = painterResource(id = R.drawable.github_svgrepo_com),
                                        contentDescription = "Github icon"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            // NerveSparks button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = Color(0xFF22314A),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                val context = LocalContext.current
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().clickable {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://nervesparks.com")
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text(
                                        text = "NerveSparks.com",
                                        color = Color(0xFF78797a),
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.width(8.dp))


                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            // Powered by section - Right-aligned
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                 Text(
                                    text = "powered by",
                                    color = Color(0xFF636466),
                                    fontSize = 14.sp
                                )
                                    val context = LocalContext.current
                                    Text(
                                        modifier = Modifier
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = Uri.parse("https://github.com/ggerganov/llama.cpp")
                                                }
                                                context.startActivity(intent)
                                            },
                                        text = " llama.cpp",
                                        color = Color(0xFF78797a),
                                        fontSize = 16.sp
                                    )

                            }
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



                    //Top app bar starts here.
                    Row(

                        modifier = Modifier
                            .background(color = Color.Transparent)
                            .padding(start = 20.dp, end = 10.dp)
                            .height(60.dp)
                            .fillMaxWidth().
                        clickable { kc?.hide() },
                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically,// This will make the Row take the full width of the Box
                    ) {

//                  icon of drawer
                        Button(
                            onClick = {
                                kc?.hide()
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

                        //New Chat Button

                        Button(
                            onClick = {
                                kc?.hide()
                                viewModel.clear()
                                viewModel.stop()

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
//

                //Top app bar stops here



                Column {


                    val scrollState = rememberLazyListState()


                    Box(modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {  kc?.hide()  },
                                onDoubleTap = {  kc?.hide() },
                                onLongPress = { kc?.hide() },
                                onPress = { kc?.hide() },



                                )
                        }) {
//

                        if (viewModel.messages.isEmpty() && !viewModel.showModal && !viewModel.showAlert) {
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
                                            fontWeight = FontWeight.W300,
                                            letterSpacing = 1.sp,
                                            fontSize = 50.sp,
                                            lineHeight = 60.sp
                                        ),
                                        fontFamily = FontFamily.SansSerif,
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
                                            .height(60.dp)
                                            .padding(8.dp)
                                            .background(
                                                Color(0xFF010825),
                                                shape = RoundedCornerShape(20.dp)
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
                                                    .size(20.dp) // Icon size
                                                    .background(Color.White, shape = CircleShape)
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.info_svgrepo_com),
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
                                                fontSize = 12.sp,
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
                                itemsIndexed(viewModel.messages as? List<Map<String, String>> ?: emptyList()) { _, messageMap ->
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


//

                                            )
                                            {

                                                val context = LocalContext.current
                                                val interactionSource = remember { MutableInteractionSource() }
                                                val sheetState = rememberModalBottomSheetState()
                                                var isSheetOpen by rememberSaveable {
                                                    mutableStateOf(false)
                                                }
                                                if(isSheetOpen){
                                                    ModalBottomSheet(
                                                        sheetState = sheetState,
                                                        containerColor = Color(0xFF01081a),
                                                        onDismissRequest = {
                                                            isSheetOpen = false
                                                            viewModel.toggler = false
                                                        })
                                                    {
                                                        //Bottom Sheet
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(20.dp)
                                                                .background(color = Color(0xFF01081a))

                                                        ){
                                                            var sheetScrollState = rememberLazyListState()
                                                            Column (
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 5.dp)

                                                            ){
                                                                //copy text
                                                                TextButton(
                                                                    colors = ButtonDefaults.buttonColors(Color(0xFF171E2C)),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 8.dp),
                                                                    onClick = {
                                                                    clipboard.setText(
                                                                        AnnotatedString(trimmedMessage)
                                                                    )
                                                                    Toast.makeText(context, "text copied!!", Toast.LENGTH_SHORT).show()
                                                                }
                                                                ) {
                                                                    Text( text = "Copy Text", color = Color(0xFFA0A0A5))
                                                                }

                                                                //copy select text

                                                                TextButton(
                                                                    colors = ButtonDefaults.buttonColors(Color(0xFF171E2C)),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 8.dp),

                                                                    onClick = {
                                                                       viewModel.toggler = !viewModel.toggler
                                                                    }
                                                                ) {
                                                                    Text(text = "Select Text To Copy", color = Color(0xFFA0A0A5))
                                                                }

                                                                TextButton(
                                                                    colors = ButtonDefaults.buttonColors(Color(0xFF171E2C)),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 8.dp),
                                                                    enabled = !viewModel.getIsSending(),
                                                                    onClick = {
                                                                        if (viewModel.stateForTextToSpeech) {
                                                                            viewModel.textForTextToSpeech = trimmedMessage
                                                                            viewModel.textToSpeech(context)
                                                                        } else {
                                                                            viewModel.stopTextToSpeech()
                                                                        }
                                                                    }
                                                                ) {
                                                                    Text(
                                                                        text = if (viewModel.stateForTextToSpeech) "Text To Speech" else "Stop",
                                                                        color = Color(0xFFA0A0A5)
                                                                    )
                                                                }
                                                                LazyColumn(state = sheetScrollState) {
                                                                    item {
                                                                        SelectionContainer {
                                                                            if(viewModel.toggler) {
                                                                                Box(
                                                                                    contentAlignment = Alignment.Center,
                                                                                    modifier = Modifier
                                                                                        .fillMaxWidth()
                                                                                        .background( color = Color.Black)
                                                                                        .padding(25.dp)

                                                                                ){
                                                                                    Text(
                                                                                        text = AnnotatedString(
                                                                                            trimmedMessage
                                                                                        ), color = Color.White
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                        }
//
                                                    }
                                                }
                                                Row(
                                                    horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 0.dp),

                                                    ) {
                                                    if(role == "assistant")
                                                    {
                                                        Image(
                                                            painter = painterResource(
                                                                id = R.drawable.logo
                                                            ),
                                                            contentDescription =  "Bot Icon",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Box( modifier = Modifier
                                                        .padding(horizontal = 2.dp)
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
                                                                isSheetOpen = true
//
                                                            },
                                                            onClick = {
                                                                kc?.hide()
                                                            }
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
                                                                    //selection text
                                                                    Text(
                                                                        text = if (trimmedMessage.startsWith("```")) {
                                                                            trimmedMessage.substring(3)
                                                                        } else {
                                                                            trimmedMessage
                                                                        },
                                                                        style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFA0A0A5)),
                                                                        modifier = Modifier
                                                                            .padding(start = 1.dp, end = 1.dp)
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


//

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
                                item {
                                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth())
                                }
                            }

                            ScrollToBottomButton(
                                scrollState = scrollState,
                                messages = viewModel.messages,
                                viewModel = viewModel
                            )

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
                                        .clickable {
                                            viewModel.updateMessage(Prompts[index])
                                            focusRequester.requestFocus()
                                        }
                                        .padding(horizontal = 8.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF030815))
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
                    //Prompt input field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF050B16))

                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 5.dp, top = 8.dp, bottom = 8.dp, end = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,

                            ) {


                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                    )
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
                                }
                                speechRecognizerLauncher.launch(intent)
                                focusManager.clearFocus()

                            }) {
                                Icon(
//                                imageVector = Icons.Default.Send,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .weight(1f),
                                    painter = painterResource(id = R.drawable.mic_on_svgrepo_com),
                                    contentDescription = "Mic",
                                    tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                                )
                            }



                            TextField(
                                value = TextFieldValue(text = viewModel.message, selection = TextRange(viewModel.message.length)),
                                onValueChange = { viewModel.updateMessage(it.text) },


                                placeholder = { Text("Message") },
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coordinates ->
                                        textFieldBounds = coordinates.boundsInRoot()
                                    }
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



                            if (!viewModel.getIsSending()) {

                                IconButton(onClick = {
                                    viewModel.send()
                                    focusManager.clearFocus()
                                }
                                ) {
                                    Icon(
//                                imageVector = Icons.Default.Send,
                                        modifier = Modifier
                                            .size(28.dp)
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
                                            .weight(1f)
                                            .size(32.dp),
                                        painter = painterResource(id = R.drawable.square_svgrepo_com),
                                        contentDescription = "Stop",
                                        tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
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
@Composable
fun ScrollToBottomButton(
    viewModel: MainViewModel,
    scrollState: LazyListState,
    messages: List<Any>
) {
    val coroutineScope = rememberCoroutineScope()

    // State to track if auto-scrolling is enabled
    var isAutoScrolling by remember { mutableStateOf(false) }

    // State to control the button's visibility
    var isButtonVisible by remember { mutableStateOf(true) }

    // Determine if the user can scroll down
    val canScrollDown by remember {
        derivedStateOf { scrollState.canScrollForward }
    }

    // Continuously scroll to the bottom while auto-scrolling is enabled
    LaunchedEffect(viewModel.messages.size, isAutoScrolling) {
        if (isAutoScrolling) {
            coroutineScope.launch {
                scrollState.scrollToItem(viewModel.messages.size + 1)
            }
        }
    }

    // Stop auto-scrolling when the user scrolls manually
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isAutoScrolling = false
            isButtonVisible = true // Show the button again if the user scrolls manually
        }
    }

    // Continuously monitor changes in the last item's content
    LaunchedEffect(messages.lastOrNull()) {
        if (isAutoScrolling && messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.scrollToItem(viewModel.messages.size +1)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = (canScrollDown || isAutoScrolling) && isButtonVisible, // Show button if needed
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FloatingActionButton(
                onClick = {
                    isAutoScrolling = true // Enable auto-scrolling
                    isButtonVisible = false // Hide the button on click
                    coroutineScope.launch {
                        scrollState.scrollToItem(messages.size +1 )
                    }
                },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom"
                )
            }
        }
    }
}






// [END android_compose_layout_material_modal_drawer]









