package com.nervesparks.iris

//import com.example.llama.ui.theme.LlamaAndroidTheme
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.unit.toSize
import com.nervesparks.iris.ui.ModelSelectorWithDownloadModal
import com.nervesparks.iris.ui.SettingsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException


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
    private var model_name = "Llama 3.2 1B Instruct (Q6_K_L, 1.09 GiB)"

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


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
                "Llama-3.2-3B-Instruct-Q4_K_L",
                Uri.parse("https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true"),
                File(extFilesDir, "Llama-3.2-3B-Instruct-Q4_K_L.gguf")

            ),
            Downloadable(
                "Llama-3.2-1B-Instruct-Q6_K_L",
                Uri.parse("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true"),
                File(extFilesDir, "Llama-3.2-1B-Instruct-Q6_K_L.gguf")
            ),
            Downloadable(
                "stablelm-2-1_6b-chat.Q4_K_M.imx",
                Uri.parse("https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true"),
                File(extFilesDir, "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf")
            )
        )

        if (extFilesDir != null) {
            viewModel.loadExistingModels(extFilesDir)
        }



        setContent {
            var showSettingSheet by remember { mutableStateOf(false) }
            var isBottomSheetVisible by rememberSaveable  { mutableStateOf(false) }
            var modelData by rememberSaveable  { mutableStateOf<List<Map<String, String>>?>(null) }
            var selectedModel by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val sheetState = rememberModalBottomSheetState()

            var UserGivenModel by remember {
                mutableStateOf(
                    TextFieldValue(
                        text = viewModel.userGivenModel,
                        selection = TextRange(viewModel.userGivenModel.length) // Ensure cursor starts at the end
                    )
                )
            }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            ModalNavigationDrawer(

                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                        drawerContainerColor= Color(0xFF070915),

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
                                    modifier =  Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
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
                                    Spacer(Modifier.weight(1f))
//                                    IconButton(
//                                        onClick = onNextButtonClicked
//
//                                    ){
//                                        Icon( painter = painterResource(id = R.drawable.settings_5_svgrepo_com),
//                                            contentDescription = "Setting logo",
//                                            modifier = Modifier.size(25.dp),
//                                            tint = Color.White
//                                        )
//
//                                    }
                                    if (showSettingSheet) {
                                        SettingsBottomSheet(
                                            viewModel= viewModel,
                                            onDismiss = { showSettingSheet = false } // Control visibility from here
                                        )
                                    }
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
                            val coroutineScope = rememberCoroutineScope()

                            // Bottom sheet content


                            if (isBottomSheetVisible) {
                                ModalBottomSheet(
                                    onDismissRequest = {
                                        isBottomSheetVisible = false
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
//                                            LazyColumn(
//                                                modifier = Modifier.fillMaxWidth()
//                                            ) {
//                                                modelData?.forEach { model ->
//                                                    item {
//                                                        model["rfilename"]?.takeIf { it.endsWith(".gguf") }?.let { filename ->
//                                                            val fullUrl = "https://huggingface.co/${viewModel.userGivenModel}/resolve/main/${filename}?download=true"
//                                                            Log.i("This is the url", fullUrl)
//
//                                                            Downloadable.Button(
//                                                                viewModel,
//                                                                dm,
//                                                                Downloadable(
//                                                                    name = filename,
//                                                                    source = Uri.parse(fullUrl),
//                                                                    destination =  File(extFileDir, filename)
//                                                                )
//                                                            )
//
////                                                        val fileSize = getRemoteFileSize(fullUrl)
////                                                        fileSize?.let { size ->
////                                                            Text(
////                                                                text = size,
////                                                                style = MaterialTheme.typography.bodyLarge,
////                                                                fontWeight = FontWeight.Bold
////                                                            )
////                                                        }
//                                                        }
//                                                    }
//                                                }
//                                            }

                                            // Customize this based on your actual ModelData structure

                                            // Add more details as needed

                                        }
                                    }
                                }
                            }

//                            ModelSelectorWithDownloadModal(viewModel = viewModel, downloadManager = dm, extFileDir = extFileDir)

                            Column (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ){
                                Text(
                                    text = "Example: bartowski/Llama-3.2-1B-Instruct-GGUF",
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .padding(4.dp),
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                OutlinedTextField(
                                    value = UserGivenModel,
                                    onValueChange = { newValue ->
                                        UserGivenModel = newValue
                                        // Update ViewModel or perform other actions with the new value
                                        viewModel.userGivenModel = newValue.text
                                    },
                                    label = { Text("Search Models Online") },
//                            placeholder = (Text("Example: bartowski/Llama-3.2-1B-Instruct-GGUF")),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = Color.Transparent),
                                    singleLine = true,
                                    maxLines = 1,
                                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFF666666),
                                        focusedBorderColor = Color(0xFFcfcfd1),
                                        unfocusedLabelColor = Color(0xFF666666),
                                        focusedLabelColor = Color(0xFFcfcfd1),
                                        unfocusedTextColor = Color(0xFFf5f5f5),
                                        focusedTextColor = Color(0xFFf7f5f5),
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if(viewModel.SearchedName != viewModel.userGivenModel) {
                                            viewModel.SearchedName = viewModel.userGivenModel
                                            // Perform action when button is clicked

                                            coroutineScope.launch {
                                                isLoading = true // Show loading state

                                                try {
                                                    val response = withContext(Dispatchers.IO) {
                                                        // Perform network request
                                                        val url =
                                                            URL("https://huggingface.co/api/models/${viewModel.userGivenModel}")
                                                        val connection =
                                                            url.openConnection() as HttpURLConnection
                                                        connection.requestMethod = "GET"
                                                        connection.setRequestProperty(
                                                            "Accept",
                                                            "application/json"
                                                        )
                                                        connection.connectTimeout = 10000
                                                        connection.readTimeout = 10000

                                                        val responseCode = connection.responseCode
                                                        if (responseCode == HttpURLConnection.HTTP_OK) {
                                                            connection.inputStream.bufferedReader()
                                                                .use { it.readText() }
                                                        } else {
                                                            val errorStream =
                                                                connection.errorStream?.bufferedReader()
                                                                    ?.use { it.readText() }
                                                            throw Exception("HTTP error code: $responseCode - ${errorStream ?: "No additional error details"}")
                                                        }
                                                    }

                                                    // Handle the response
                                                    Log.i("response", response)
                                                    val jsonResponse = JSONObject(response)
                                                    val siblingsArray = jsonResponse.getJSONArray("siblings")
                                                    modelData =
                                                        (0 until siblingsArray.length()).mapNotNull { index ->
                                                            val jsonObject = siblingsArray.getJSONObject(index)
                                                            val filename = jsonObject.optString("rfilename", "")

                                                            if (filename.isNotEmpty()) {
                                                                mapOf("rfilename" to filename)
                                                            } else {
                                                                null
                                                            }
                                                        }
                                                    Log.i("response hello", modelData.toString())
                                                    isBottomSheetVisible = true
                                                } catch (e: Exception) {
                                                    // Handle exceptions
                                                    Log.e("ModelFetch", "Failed to fetch model", e)
                                                    isBottomSheetVisible = true
                                                    errorMessage = when (e) {
                                                        is UnknownHostException -> "No internet connection"
                                                        is SocketTimeoutException -> "Connection timed out"
                                                        else -> "Failed to fetch model: ${e.localizedMessage ?: "Unknown error"}"
                                                    }
                                                } finally {
                                                    isLoading = false // Hide loading state
                                                }
                                            }
                                        }
                                        else {
                                            isBottomSheetVisible = true
                                        }

                                    },



                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    enabled = UserGivenModel.text.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent, // Set the containerColor to transparent
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp), // Slightly more rounded corners
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 6.dp,
                                        pressedElevation = 3.dp
                                    )
                                ){
                                    Text(
                                        text = when {
                                            isLoading -> "Searching..."
                                            viewModel.SearchedName != viewModel.userGivenModel -> "Search Model"
                                            else -> "Open"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }



                            }

                            Spacer(modifier = Modifier.weight(1f))
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
                                            color = Color(0xFF14161f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.LightGray.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    val context = LocalContext.current
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data =
                                                        Uri.parse("https://github.com/nerve-sparks/iris_android")
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
                                            color = Color(0xFF14161f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.LightGray.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    val context = LocalContext.current
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
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

                ChatScreen(
                    viewModel,
                    clipboardManager,
                    downloadManager,
                    models,
                    extFilesDir,
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
        start = Offset(0f, 300f),
        end = Offset(0f, 1000f)

    )
    Box(modifier = Modifier.background(gradient).fillMaxSize())
}







// [END android_compose_layout_material_modal_drawer]









