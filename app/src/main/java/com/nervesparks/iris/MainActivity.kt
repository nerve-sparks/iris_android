package com.nervesparks.iris

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.WindowManager

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.ui.SettingsBottomSheet
import com.nervesparks.iris.ui.theme.IrisTheme
import kotlinx.coroutines.launch


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
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
): ComponentActivity() {

    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private lateinit var viewModel: MainViewModel

    // Samsung Galaxy S24 Ultra optimized gradient colors
    private val samsungDarkBlue = Color(0xFF000000) // Pure black for AMOLED
    private val samsungAccentBlue = Color(0xFF1976D2) // Samsung blue
    private val samsungGradientBrush = Brush.verticalGradient(
        colors = listOf(samsungDarkBlue, samsungAccentBlue.copy(alpha = 0.1f))
    )

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen for smooth loading experience
        val splashScreen = installSplashScreen()
        
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Samsung Galaxy S24 Ultra specific optimizations
        setupSamsungOptimizations()
        
        // Enhanced StrictMode for performance monitoring
        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectFileUriExposure()
                .detectCleartextNetwork()
                .penaltyLog()
                .build()
        )
        val userPrefsRepo = UserPreferencesRepository.getInstance(applicationContext)
        val lLamaAndroid = LLamaAndroid.instance()
        val viewModelFactory = MainViewModelFactory(lLamaAndroid, userPrefsRepo)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        val extFilesDir = getExternalFilesDir(null)

        val models = listOf(
            Downloadable(
                "Llama-3.2-3B-Instruct-Q4_K_L.gguf",
                Uri.parse("https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true"),
                File(extFilesDir, "Llama-3.2-3B-Instruct-Q4_K_L.gguf")
            ),
            Downloadable(
                "Llama-3.2-1B-Instruct-Q6_K_L.gguf",
                Uri.parse("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true"),
                File(extFilesDir, "Llama-3.2-1B-Instruct-Q6_K_L.gguf")
            ),
            Downloadable(
                "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf",
                Uri.parse("https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true"),
                File(extFilesDir, "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf")
            )
        )

        if (extFilesDir != null) {
            viewModel.loadExistingModels(extFilesDir)
        }



        setContent {
            // Calculate window size class for Samsung Galaxy S24 Ultra's large screen
            val windowSizeClass = calculateWindowSizeClass(this)
            val configuration = LocalConfiguration.current
            
            // Samsung Galaxy S24 Ultra has a large screen, optimize accordingly
            val isLargeScreen = configuration.screenWidthDp >= 600
            
            IrisTheme(
                dynamicColor = true // Enable Samsung's dynamic theming
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IrisMainContent(
                        windowSizeClass = windowSizeClass,
                        isLargeScreen = isLargeScreen
                    )
                }
            }
        }
    }

    @Composable
    private fun IrisMainContent(
        windowSizeClass: WindowSizeClass,
        isLargeScreen: Boolean
    ) {
        var showSettingSheet by remember { mutableStateOf(false) }
        var isBottomSheetVisible by rememberSaveable { mutableStateOf(false) }
        var modelData by rememberSaveable { mutableStateOf<List<Map<String, String>>?>(null) }
        var selectedModel by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var currentScreen by remember { mutableStateOf("chat") } // "chat", "workbench", "knowledge"
        val sheetState = rememberModalBottomSheetState()

        var userGivenModel by remember {
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
                        .width(if (isLargeScreen) 360.dp else 300.dp) // Optimize for Samsung's large screen
                        .fillMaxHeight(),
                    drawerContainerColor = MaterialTheme.colorScheme.surface, // Use theme colors
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0)
                ) {
                    // Samsung-optimized drawer content
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxHeight(),
                    ) {
                        // Top section with logo and name - enhanced for Samsung display
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
                                        text = "Axia",
                                        fontWeight = FontWeight(500),
                                        color = Color.White,
                                        fontSize = 30.sp
                                    )
                                    Spacer(Modifier.weight(1f))
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
                            Spacer(Modifier.height(20.dp))
                            Column (modifier = Modifier.padding(6.dp)){
                                Text(text = "Active Model", fontSize = 16.sp, color = Color(0xFF636466), modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp))
                                Text(text = viewModel.loadedModelName.value, fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp) )
                            }
                            
                            Spacer(Modifier.height(20.dp))
                            
                            // Development Workbench Section
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "Development", 
                                    fontSize = 16.sp, 
                                    color = Color(0xFF636466), 
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                                )
                                
                                // Workbench Navigation Item
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(horizontal = 8.dp)
                                        .background(
                                            color = Color(0xFF14161f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.LightGray.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            // Navigate to Workbench
                                            currentScreen = "workbench"
                                            scope.launch { drawerState.close() }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = "⚡ Workbench",
                                            color = Color(0xFF58A6FF),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "Termux+",
                                            color = Color(0xFF7D8590),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                // Knowledge Base Navigation Item
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(horizontal = 8.dp)
                                        .background(
                                            color = Color(0xFF14161f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = Color.LightGray.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            // Navigate to Knowledge Base
                                            currentScreen = "knowledge"
                                            scope.launch { drawerState.close() }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = "📚 Knowledge Base",
                                            color = Color(0xFF7EE787),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "Train",
                                            color = Color(0xFF7D8590),
                                            fontSize = 12.sp
                                        )
                                    }
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
                when (currentScreen) {
                    "workbench" -> {
                        com.nervesparks.iris.ui.development.DevelopmentEnvironmentScreen(
                            developmentEnvironment = remember { 
                                com.nervesparks.iris.development.DevelopmentEnvironment(LocalContext.current) 
                            },
                            onNavigateBack = { 
                                currentScreen = "chat" 
                            }
                        )
                    }
                    "knowledge" -> {
                        com.nervesparks.iris.ui.knowledge.KnowledgeBaseScreen(
                            knowledgeManager = remember { 
                                com.nervesparks.iris.knowledge.KnowledgeBaseManager(LocalContext.current) 
                            },
                            dao = remember { 
                                com.nervesparks.iris.knowledge.IrisDatabase.getDatabase(LocalContext.current).knowledgeDao() 
                            },
                            onBack = { 
                                currentScreen = "chat" 
                            }
                        )
                    }
                    else -> {
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
    }

    /**
     * Samsung Galaxy S24 Ultra specific optimizations
     */
    private fun setupSamsungOptimizations() {
        // Optimize for Samsung's Dynamic AMOLED 2X display
        window.apply {
            // Enable hardware acceleration for smooth performance
            setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            
            // Pure black status bar for AMOLED power efficiency
            statusBarColor = android.graphics.Color.BLACK
            navigationBarColor = android.graphics.Color.BLACK
            
            // Enable immersive mode for full-screen experience
            WindowCompat.setDecorFitsSystemWindows(this, false)
            
            val insetsController = WindowCompat.getInsetsController(this, decorView)
            insetsController.apply {
                // Optimize system bars for Samsung's edge-to-edge experience
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
        
        // Samsung Galaxy S24 Ultra performance optimizations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Optimize for 120Hz display refresh rate
            window.attributes.preferredRefreshRate = 120f
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









