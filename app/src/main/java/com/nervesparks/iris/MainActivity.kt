package com.nervesparks.iris

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Intent
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.ui.SettingsBottomSheet
import com.nervesparks.iris.ui.theme.*


class MainViewModelFactory(
    private val llamaAndroid: LLamaAndroid,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(llamaAndroid, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class MainActivity(
    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
) : ComponentActivity() {

    private val tag: String? = this::class.simpleName

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private lateinit var viewModel: MainViewModel

    private fun availableMemory(): ActivityManager.MemoryInfo {
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(ChatGPTOnBackground, ChatGPTSurface)
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.statusBarColor = ChatGPTOnBackground.toArgb()

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )
        val userPrefsRepo = UserPreferencesRepository.getInstance(applicationContext)

        val lLamaAndroid = LLamaAndroid.instance()
        val viewModelFactory = MainViewModelFactory(lLamaAndroid, userPrefsRepo)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        val free = Formatter.formatFileSize(this, availableMemory().availMem)
        val total = Formatter.formatFileSize(this, availableMemory().totalMem)
        val transparentColor = Color.Transparent.toArgb()
        window.decorView.rootView.setBackgroundColor(transparentColor)
        viewModel.log("Current memory: $free / $total")
        viewModel.log("Downloads directory: ${getExternalFilesDir(null)}")

        val extFilesDir = getExternalFilesDir(null)
        val models = listOf(
            Downloadable(
                "EdgeLLM-1_8B-fp16.gguf",
                Uri.parse("https://huggingface.co/elm-team/EdgeLLM-GGUF/resolve/main/edgellm-1.8B-fp16.gguf?download=true"),
                File(extFilesDir, "edgellm-1.8B-fp16.gguf")
            )
        )
        if (extFilesDir != null) {
            viewModel.loadExistingModels(extFilesDir)
        }

        setContent {
            var showSettingSheet by remember { mutableStateOf(false) }
            var isBottomSheetVisible by rememberSaveable { mutableStateOf(false) }
            var modelData by rememberSaveable { mutableStateOf<List<Map<String, String>>?>(null) }
            var selectedModel by remember { mutableStateOf<String?>(null) }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val sheetState = rememberModalBottomSheetState()

            var UserGivenModel by remember {
                mutableStateOf(
                    TextFieldValue(
                        text = viewModel.userGivenModel,
                        selection = TextRange(viewModel.userGivenModel.length)
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
                        drawerContainerColor = ChatGPTOnBackground,
                    ) {
                        /* Drawer content wrapper */
                        Column(
                            modifier = Modifier
                                .padding(5.dp)
                                .fillMaxHeight()
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
                                        text = "EdgeLLM",
                                        fontWeight = FontWeight(500),
                                        color = ChatGPTOnBackground,
                                        fontSize = 30.sp
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (showSettingSheet) {
                                        SettingsBottomSheet(
                                            viewModel = viewModel,
                                            onDismiss = { showSettingSheet = false }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.padding(start = 45.dp)
                                ) {
                                    Text(
                                        text = "NerveSparks",
                                        color = ChatGPTOnBackground,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "Active Model",
                                    fontSize = 16.sp,
                                    color = ChatGPTOnBackground,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                )
                                Text(
                                    text = viewModel.loadedModelName.value,
                                    fontSize = 16.sp,
                                    color = ChatGPTOnBackground,
                                    modifier = Modifier
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))

                            // Drawer底部的几个按钮
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            color = ChatGPTSurface,
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
                                                    data = Uri.parse(
                                                        "https://github.com/nerve-sparks/iris_android"
                                                    )
                                                }
                                                context.startActivity(intent)
                                            }
                                    ) {
                                        Text(
                                            text = "Star us",
                                            color = ChatGPTOnBackground,
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Image(
                                            modifier = Modifier.size(24.dp),
                                            painter = painterResource(id = R.drawable.github_svgrepo_com),
                                            contentDescription = "Github icon"
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))

                                // "NerveSparks.com" button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            color = ChatGPTSurface,
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
                                            color =ChatGPTOnBackground,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))

                                // "powered by llama.cpp"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "powered by",
                                        color = ChatGPTOnBackground,
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
                                        color = ChatGPTOnBackground,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
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
    val gradient = Brush.linearGradient(
        colors = listOf(ChatGPTSurface,ChatGPTBackground),
        start = Offset(0f, 300f),
        end = Offset(0f, 1000f)
    )
    Box(
        modifier = Modifier
            .background(gradient)
            .fillMaxSize()
    )
}







// [END android_compose_layout_material_modal_drawer]









