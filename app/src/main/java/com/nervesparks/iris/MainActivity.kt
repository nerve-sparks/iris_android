package com.nervesparks.iris

//import com.example.llama.ui.theme.LlamaAndroidTheme
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.font.FontFamily
import com.nervesparks.iris.ui.components.MessageBottomSheet
import com.nervesparks.iris.ui.components.ModelSelectorWithDownloadModal
import com.nervesparks.iris.ui.components.ScrollToBottomButton
import com.nervesparks.iris.ui.components.SearchResultBottomSheet
import com.nervesparks.iris.ui.components.SettingsBottomSheet
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


    // Get a MemoryInfo object for the device's current memory status.
//    private fun availableMemory(): ActivityManager.MemoryInfo {
//        return ActivityManager.MemoryInfo().also { memoryInfo ->
//            activityManager.getMemoryInfo(memoryInfo)
//        }
//    }
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

//        models.forEach { model ->
//            if (model.destination.exists() and (model.name == model_name)) {
//                viewModel.load(model.destination.path)
//            }
//        }
//        models.find { model -> model.destination.exists() }?.let { model ->
//            viewModel.load(model.destination.path, userThreads = viewModel.user_thread)
//            viewModel.currentDownloadable = model
//        }
        if (extFilesDir != null) {
            viewModel.loadExistingModels(extFilesDir)
        }



        setContent {


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
    Box(modifier = Modifier.background(gradient))
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>,
    extFileDir: File?
) {
    val kc = LocalSoftwareKeyboardController.current

    val focusManager = LocalFocusManager.current
    println("Thread started: ${Thread.currentThread().name}")
    val Prompts = listOf(
        "Explain the strategic turning points of the Battle of Midway during World War II",
        "Describe the innovative technologies that are transforming renewable energy production",
        "Outline the core consulting services provided by management consulting firms like McKinsey",
        "Walk me through a systematic approach to debugging a complex software issue",
        "Trace the architectural and cultural evolution of Paris from medieval times to the modern era",
        "Highlight the architectural marvels of Paris, from the Eiffel Tower to the hidden gems of Montmartre",
        "Recommend a targeted 15-minute daily routine to improve posture and reduce back pain",
        "List the top 5 science fiction novels that have most influenced modern technological thinking",
        "Provide a precise Spanish translation of 'Innovation drives progress' with grammatical explanations",
        "Analyze the impact of artificial intelligence on global economic and social landscapes in 2024"
    )

    val allModelsExist = models.all { model -> model.destination.exists() }
    val Prompts_Home = listOf(
        "Explains complex topics simply.",
        "Remembers previous inputs.",
        "May sometimes be inaccurate."
    )
    var recognizedText by remember {mutableStateOf("")}
    val speechRecognizerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        recognizedText = results?.get(0)?:""
        viewModel.updateMessage(recognizedText)

    }
    var showSettingSheet by remember { mutableStateOf(false) }
    var isBottomSheetVisible by rememberSaveable  { mutableStateOf(false) }
    var modelData by rememberSaveable  { mutableStateOf<List<Map<String, String>>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userGivenModel by remember {
        mutableStateOf(
            TextFieldValue(
                text = viewModel.userGivenModel,
                selection = TextRange(viewModel.userGivenModel.length) // Ensure cursor starts at the end
            )
        )
    }
    val focusRequester = FocusRequester()
    var isFocused by remember { mutableStateOf(false) }
    var textFieldBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    if (allModelsExist) {
        viewModel.showModal = false
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
                                IconButton(
                                    onClick = {
                                        showSettingSheet = true;
                                    }
                                ){
                                    Icon( painter = painterResource(id = R.drawable.settings_5_svgrepo_com),
                                        contentDescription = "Centered Background Logo",
                                        modifier = Modifier.size(25.dp),
                                        tint = Color.White
                                    )

                                }
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
                            if (extFileDir != null) {
                                SearchResultBottomSheet(viewModel, dm, extFileDir, modelData, isInitiallyVisible = isBottomSheetVisible, onDismiss = {isBottomSheetVisible = false})
                            }
                        }

                       ModelSelectorWithDownloadModal(viewModel = viewModel, downloadManager = dm, extFileDir = extFileDir)

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
                            value = userGivenModel,
                            onValueChange = { newValue ->
                                userGivenModel = newValue
                                // Update ViewModel or perform other actions with the new value
                                viewModel.userGivenModel = newValue.text
                            },
                            label = { Text("Search Models Online") },
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
                                    enabled = userGivenModel.text.isNotBlank(),
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


           // Screen content
            Column() {


                // Show modal if required
                if (viewModel.showModal) {
                    // Modal dialog to show download options
                    Dialog(onDismissRequest = {}) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black,
                            modifier = Modifier
                                .padding(10.dp)
                                .height(230.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .height(140.dp)
                                    ,
                                horizontalAlignment = Alignment.CenterHorizontally

                            ) {
                                item {  Text(
                                    text = "Download Required",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )}
                                item {
                                Text(
                                    text = "Don't close or minimize the app!",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )}
                                item {Spacer(modifier = Modifier.height(35.dp))}

                               item{ models.forEach { model ->
                                    if (!model.destination.exists()) {
//                                        Text(text = model.name, modifier = Modifier.padding(9.dp))
                                        Downloadable.Button(viewModel, dm, model)
                                    }
                                }}


                                item {Spacer(modifier = Modifier.height(20.dp))}



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
                                Text(
                                    text = viewModel.loadedModelName.value,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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
                            .fillMaxWidth()
                            .clickable { kc?.hide() },
                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically,
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
                        Button(
                            onClick = {
                                kc?.hide()
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
//

                //Top app bar stops here

                Column {


                    val scrollState = rememberLazyListState()


                    Box(modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    kc?.hide()
                                },
                                onDoubleTap = { kc?.hide() },
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

                            LazyColumn(state = scrollState) {
                                // Track the first user and assistant messages

                                val length = viewModel.messages.size

                                itemsIndexed(viewModel.messages.slice(3..< length) as? List<Map<String, String>> ?: emptyList()) { index, messageMap ->
                                    val role = messageMap["role"] ?: ""
                                    val content = messageMap["content"] ?: ""
                                    val trimmedMessage = if (content.endsWith("\n")) {
                                        content.substring(startIndex = 0, endIndex = content.length - 1)
                                    } else {
                                        content
                                    }

                                    // Skip rendering first user and first assistant messages

                                    if (role != "system") {
                                        if (role != "codeBlock") {
                                            Box {
                                                val context = LocalContext.current
                                                val interactionSource = remember { MutableInteractionSource() }
                                                val sheetState = rememberModalBottomSheetState()
                                                var isSheetOpen by rememberSaveable {
                                                    mutableStateOf(false)
                                                }
                                                if(isSheetOpen){
                                                    MessageBottomSheet(
                                                        message = trimmedMessage,
                                                        clipboard = clipboard,
                                                        context = context,
                                                        viewModel = viewModel,
                                                        onDismiss = {
                                                            isSheetOpen = false
                                                            viewModel.toggler = false
                                                        },
                                                        sheetState = sheetState
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            start = 8.dp,
                                                            top = 8.dp,
                                                            end = 8.dp,
                                                            bottom = 0.dp
                                                        ),
                                                ) {
                                                    if(role == "assistant") {
                                                        Image(
                                                            painter = painterResource(
                                                                id = R.drawable.logo
                                                            ),
                                                            contentDescription =  "Bot Icon",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Box(modifier = Modifier
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
                                                                if (viewModel.getIsSending()) {
                                                                    Toast
                                                                        .makeText(
                                                                            context,
                                                                            " Wait till generation is done! ",
                                                                            Toast.LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                                } else {
                                                                    isSheetOpen = true
                                                                }
                                                            },
                                                            onClick = {
                                                                kc?.hide()
                                                            }
                                                        )
                                                    ) {
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
                                            // Code block rendering remains the same
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
                                                        // Previous content here
                                                    }
                                                    Text(
                                                        text = if (trimmedMessage.startsWith("```")) {
                                                            trimmedMessage.substring(3)
                                                        } else {
                                                            trimmedMessage
                                                        },
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            color = Color(0xFFA0A0A5)
                                                        ),
                                                        modifier = Modifier.padding(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier
                                        .height(1.dp)
                                        .fillMaxWidth())
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
                                                fontSize = 12.sp,
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
                                        .size(25.dp)
                                        .weight(1f),
                                    painter = painterResource(id = R.drawable.microphone_new_svgrepo_com),
                                    contentDescription = "Mic",
                                    tint = Color(0xFFDDDDE4) // Optional: set the color of the icon
                                )
                            }



                            val dragSelection = remember { mutableStateOf<TextRange?>(null) }
                            val lastKnownText = remember { mutableStateOf(viewModel.message) }

                            val textFieldValue = remember {
                                mutableStateOf(
                                    TextFieldValue(
                                        text = viewModel.message,
                                        selection = TextRange(viewModel.message.length) // Ensure cursor starts at the end
                                    )
                                )
                            }

                            TextField(

                                value = textFieldValue.value.copy(
                                    text = viewModel.message,
                                    selection = when {
                                        viewModel.message != lastKnownText.value -> {
                                            // If the message has changed programmatically,
                                            // preserve the current cursor/selection position
                                            textFieldValue.value.selection
                                        }
                                        else -> {
                                            // Otherwise, use the drag selection or current selection
                                            dragSelection.value ?: textFieldValue.value.selection
                                        }
                                    }
                                ),
                                onValueChange = { newValue ->
                                    // Update drag selection when the user drags or selects
                                    dragSelection.value = if (newValue.text == textFieldValue.value.text) {
                                        newValue.selection
                                    } else {
                                        null // Reset drag selection if the text changes programmatically
                                    }


                                    // Update the local state
                                    textFieldValue.value = newValue

                                    // Save the last known text and update ViewModel
                                    lastKnownText.value = newValue.text
                                    viewModel.updateMessage(newValue.text)
                                },
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
                                        modifier = Modifier
                                            .size(30.dp)
                                            .weight(1f),
                                        painter = painterResource(id = R.drawable.send_2_svgrepo_com),
                                        contentDescription = "Send",
                                        tint = Color(0xFFDDDDE4)
                                    )
                                }
                            } else if (viewModel.getIsSending()) {
                                IconButton(onClick = {
                                    viewModel.stop() }) {
                                    Icon(
                                        modifier = Modifier
                                            .weight(1f)
                                            .size(28.dp),
                                        painter = painterResource(id = R.drawable.square_svgrepo_com),
                                        contentDescription = "Stop",
                                        tint = Color(0xFFDDDDE4)
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






// [END android_compose_layout_material_modal_drawer]









