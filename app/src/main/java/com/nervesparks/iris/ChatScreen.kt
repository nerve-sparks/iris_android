package com.nervesparks.iris

import android.app.DownloadManager
import android.content.ClipboardManager
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nervesparks.iris.ui.AboutScreen
import com.nervesparks.iris.ui.BenchMarkScreen
import com.nervesparks.iris.ui.MainChatScreen
import com.nervesparks.iris.ui.ModelsScreen
import com.nervesparks.iris.ui.ParametersScreen
import com.nervesparks.iris.ui.SearchResultScreen
import com.nervesparks.iris.ui.SettingsScreen
import java.io.File


enum class ChatScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Settings(title = R.string.settings_screen_title),
    SearchResults(title = R.string.search_results_screen_title),
    ModelsScreen(title = R.string.models_screen_title),
    ParamsScreen(title = R.string.parameters_screen_title),
    AboutScreen(title = R.string.about_screen_title),
    BenchMarkScreen(title = R.string.benchmark_screen_title),
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenAppBar(
    currentScreen: ChatScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    onSettingsClick: () -> Unit, // New parameter for settings navigation
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
) {
    val kc = LocalSoftwareKeyboardController.current
    val darkNavyBlue = Color(0xFF050a14)
    TopAppBar(
        title = {
            Text(
                stringResource(currentScreen.title),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 28.sp)
            )
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = modifier.background(darkNavyBlue),
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button),
                        tint = Color.White
                    )
                }
            }

        },

        actions = { // New actions section
            if(!canNavigateBack) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_gear_rounded), // Make sure to import this
                        contentDescription = stringResource(R.string.setting),
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            if(!canNavigateBack) {
                IconButton(
                    onClick = {
                        kc?.hide()
                        viewModel.stop()
                        viewModel.clear()
                    }
                ){
                    Icon(
                        modifier = Modifier.size(25.dp),
                        painter = painterResource(id = R.drawable.edit_3_svgrepo_com),
                        contentDescription = "newChat",
                        tint = Color.White
                    )
                }
            }
        }

    )
}



@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    clipboardManager: ClipboardManager,
    downloadManager: DownloadManager,
    models: List<Downloadable>,
    extFileDir: File?,
    navController: NavHostController = rememberNavController()
) {
    // Define gradient colors
    val darkNavyBlue = Color(0xFF050a14)
    val lightNavyBlue = Color(0xFF051633)

    // Create gradient brush
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(darkNavyBlue, lightNavyBlue)
    )

    // Wrap the entire Scaffold with a Box that has the gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        Scaffold(
            backgroundColor = Color.Transparent, // Make Scaffold background transparent
            topBar = {
                ChatScreenAppBar(
                    currentScreen = ChatScreen.valueOf(
                        navController.currentBackStackEntryAsState().value?.destination?.route
                            ?: ChatScreen.Start.name
                    ),
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navigateUp = { navController.navigateUp() },
                    onSettingsClick = {navController.navigate(ChatScreen.Settings.name)},
                    viewModel = viewModel,
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ChatScreen.Start.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(route = ChatScreen.Start.name) {
                    MainChatScreen(
                        onNextButtonClicked = {
                            navController.navigate(ChatScreen.Settings.name)
                        },
                        viewModel = viewModel,
                        dm = downloadManager,
                        clipboard = clipboardManager,
                        models = models,
                        extFileDir = extFileDir,
                    )
                }
                composable(route = ChatScreen.Settings.name) {
                    SettingsScreen(
                        OnModelsScreenButtonClicked = {
                            navController.navigate(ChatScreen.ModelsScreen.name)
                        },
                        OnParamsScreenButtonClicked = {
                          navController.navigate((ChatScreen.ParamsScreen.name))
                        },
                        OnAboutScreenButtonClicked = {
                            navController.navigate((ChatScreen.AboutScreen.name))
                        },
                        OnBenchMarkScreenButtonClicked = {
                            navController.navigate((ChatScreen.BenchMarkScreen.name))
                        }

                    )
                }
                composable(route = ChatScreen.SearchResults.name) {
                    if (extFileDir != null) {
                        SearchResultScreen(
                            viewModel,
                            downloadManager,
                            extFileDir)
                    }
                }
                composable(route = ChatScreen.ModelsScreen.name) {
                    ModelsScreen(dm = downloadManager, extFileDir = extFileDir, viewModel = viewModel,onSearchResultButtonClick = {navController.navigate(
                        ChatScreen.SearchResults.name
                    )})
                }
                composable(route = ChatScreen.ParamsScreen.name){
                    ParametersScreen(viewModel)
                }
                composable(route = ChatScreen.AboutScreen.name){
                    AboutScreen()
                }
                composable(route = ChatScreen.BenchMarkScreen.name){
                    BenchMarkScreen(viewModel)
                }
            }
        }
    }
}

