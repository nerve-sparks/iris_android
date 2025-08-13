package com.nervesparks.iris

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Intent
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.nervesparks.iris.BuildConfig

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
import androidx.compose.foundation.layout.WindowInsets
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
import android.app.Application
import androidx.compose.foundation.border
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.security.BiometricAuthenticator
import com.nervesparks.iris.ui.theme.IrisStarTheme
import com.nervesparks.iris.ui.navigation.AppNavigation
import com.nervesparks.iris.workers.ModelUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    private val downloadManager by lazy { getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildConfig.DEBUG) {
            // Allow network operations on main thread for development
            val policy = VmPolicy.Builder()
                .detectAll()
                .build()
            StrictMode.setVmPolicy(policy)
        }

        val extFilesDir = getExternalFilesDir(null)

        // Proactively detect hardware after native init to stabilize backend choice on cold start
        lifecycleScope.launchWhenStarted {
            try { viewModel.detectHardwareCapabilities() } catch (_: Exception) {}
        }

        val models = emptyList<Downloadable>()

        if (extFilesDir != null) {
            viewModel.loadExistingModels(extFilesDir)
        }

        ModelUpdateWorker.schedule(this)

        if (preferencesRepository.getSecurityBiometricEnabled()) {
            val biometricAuthenticator = BiometricAuthenticator(this)
            biometricAuthenticator.authenticate(this) {
                setContent {
                    IrisStarTheme {
                        AppNavigation(
                            viewModel = viewModel,
                            clipboardManager = clipboardManager,
                            downloadManager = downloadManager,
                            models = models,
                            extFilesDir = extFilesDir,
                            preferencesRepository = preferencesRepository
                        )
                    }
                }
            }
        } else {
            setContent {
                IrisStarTheme {
                    AppNavigation(
                        viewModel = viewModel,
                        clipboardManager = clipboardManager,
                        downloadManager = downloadManager,
                        models = models,
                        extFilesDir = extFilesDir,
                        preferencesRepository = preferencesRepository
                    )
                }
            }
        }
    }
}











