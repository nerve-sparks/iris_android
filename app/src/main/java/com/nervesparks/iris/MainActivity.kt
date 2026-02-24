package com.nervesparks.iris

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Intent
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.ui.MatrixBg
import com.nervesparks.iris.ui.MatrixGreen
import com.nervesparks.iris.ui.MatrixGreenBright
import com.nervesparks.iris.ui.MatrixGreenDark
import com.nervesparks.iris.ui.MatrixGreenMid
import com.nervesparks.iris.ui.MatrixRainBackground
import com.nervesparks.iris.ui.SettingsBottomSheet


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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#FF000000")

        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )
        val userPrefsRepo = UserPreferencesRepository.getInstance(applicationContext)
        val lLamaAndroid = LLamaAndroid.instance()
        val viewModelFactory = MainViewModelFactory(lLamaAndroid, userPrefsRepo)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        val transparentColor = Color.Transparent.toArgb()
        window.decorView.rootView.setBackgroundColor(transparentColor)

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
                        drawerContainerColor = MatrixBg,
                    ) {
                        // Matrix rain fills the drawer background
                        Box(modifier = Modifier.fillMaxSize()) {
                            MatrixRainBackground(modifier = Modifier.fillMaxSize())

                            // Semi-transparent overlay so text is readable
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MatrixBg.copy(alpha = 0.80f))
                            )

                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxHeight()
                            ) {
                                // ── Logo / Title ────────────────────────────────
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Matrix "M" icon in a glowing box
                                        MatrixLogoIcon(size = 42.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Matrix AI",
                                                fontWeight = FontWeight.Bold,
                                                color = MatrixGreenBright,
                                                fontSize = 26.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "v1.0 // offline",
                                                color = MatrixGreenMid,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (showSettingSheet) {
                                            SettingsBottomSheet(
                                                viewModel = viewModel,
                                                onDismiss = { showSettingSheet = false }
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(24.dp))

                                // Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MatrixGreenDark)
                                )

                                Spacer(Modifier.height(12.dp))

                                Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Text(
                                        text = "ACTIVE_MODEL:",
                                        fontSize = 11.sp,
                                        color = MatrixGreenMid,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (viewModel.loadedModelName.value.isEmpty())
                                            "[ NONE ]"
                                        else
                                            viewModel.loadedModelName.value,
                                        fontSize = 13.sp,
                                        color = MatrixGreen,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Column(
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // GitHub button
                                    MatrixDrawerButton(
                                        text = "[ SOURCE CODE ]",
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://github.com/nerve-sparks/iris_android")
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Powered by
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "powered by ",
                                            color = MatrixGreenDark,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "llama.cpp",
                                            color = MatrixGreenMid,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
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
fun MatrixLogoIcon(size: androidx.compose.ui.unit.Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoGlow"
    )
    Box(
        modifier = Modifier
            .size(size)
            .background(MatrixBg, RoundedCornerShape(8.dp))
            .border(
                BorderStroke(1.5.dp, MatrixGreen.copy(alpha = glow)),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "M",
            color = MatrixGreenBright.copy(alpha = glow),
            fontSize = (size.value * 0.55f).sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MatrixDrawerButton(text: String, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MatrixBg, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, MatrixGreenDark), RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MatrixGreenMid,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}


@Composable
fun LinearGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatrixBg)
    )
}
