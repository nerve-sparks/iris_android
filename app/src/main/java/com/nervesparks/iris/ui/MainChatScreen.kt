package com.nervesparks.iris.ui

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.LinearGradient
import com.nervesparks.iris.MainViewModel

import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.ChatMessageList
import com.nervesparks.iris.ui.components.DownloadModal
import com.nervesparks.iris.ui.components.LoadingModal

import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainChatScreen (
    onNextButtonClicked: () -> Unit,
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>,
    extFileDir: File?,

){
    val kc = LocalSoftwareKeyboardController.current
    val windowInsets = WindowInsets.ime
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
    var recognizedText by remember { mutableStateOf("") }
    val speechRecognizerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        recognizedText = results?.get(0)?:""
        viewModel.updateMessage(recognizedText)

    }





    val focusRequester = FocusRequester()
    var isFocused by remember { mutableStateOf(false) }
    var textFieldBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    if (allModelsExist) {
        viewModel.showModal = false
    }
    Box(
        modifier = Modifier.fillMaxSize()


    ) {
        LinearGradient()



            // Screen content
            Column() {


                // Show modal if required
                if (viewModel.showModal) {
                    // Modal dialog to show download options
                    DownloadModal(viewModel = viewModel, dm = dm, models = models)

                }

                if (viewModel.showAlert) {
                    // Modal dialog to show download options
                    LoadingModal(viewModel)
                }

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

                        if (viewModel.messages.isEmpty() && !viewModel.showModal && !viewModel.showAlert) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize() // Take up the whole screen
                                    .wrapContentHeight(Alignment.CenterVertically),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
//                                item { Spacer(Modifier.height(55.dp).fillMaxWidth()) }
                                // Header Text
                                item {
                                    Text(
                                        text = "Hello, Ask me " + "Anything",
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

                            ChatMessageList(viewModel = viewModel, scrollState= scrollState)

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
                                .padding(start = 5.dp, top = 8.dp, bottom = 12.dp, end = 5.dp),
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
                                val context = LocalContext.current

                                IconButton(onClick = {
                                    if(viewModel.loadedModelName.value == ""){
                                        focusManager.clearFocus()
                                        Toast.makeText(context, "Load A Model First", Toast.LENGTH_SHORT).show()
                                    }
                                    else {
                                        viewModel.send()
                                        focusManager.clearFocus()
                                    }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScrollState = rememberLazyListState()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF01081a),
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ){
            Text(
                text = "Settings",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            LazyColumn(state = sheetScrollState) {
                item{
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF14161f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .border(
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color.LightGray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Select thread for process, 0 for default",
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "${viewModel.user_thread.toInt()}",
                                color = Color.White
                            )
                            Slider(
                                value = viewModel.user_thread,
                                onValueChange = {

                                    viewModel.user_thread = it
                                },
                                valueRange = 0f..8f,
                                steps = 7,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6200EE),
                                    activeTrackColor = Color(0xFF6200EE),
                                    inactiveTrackColor = Color.Gray
                                ),
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                text = "After changing thread please Save the changes!!",
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth(),

                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.DarkGray.copy(alpha = 1.0f), // Set the containerColor to transparent
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(8.dp), // Slightly more rounded corners
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 3.dp
                                ),
                                onClick = {
                                    viewModel.currentDownloadable?.destination?.path?.let {
                                        viewModel.load(
                                            it, viewModel.user_thread.toInt())
                                    }
                                }
                            ) {

                                Text("Save")
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
                scrollState.scrollToItem(viewModel.messages.size + 1)
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
                        scrollState.scrollToItem(viewModel.messages.size + 1)
                    }
                },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(56.dp),
                // Ensures a circular shape
                shape = RoundedCornerShape(percent = 50),
                containerColor = Color.White.copy(alpha = 0.5f),
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    tint = Color.White // White icon for better visibility
                )
            }
        }
    }
}


@Composable
fun ModelSelectorWithDownloadModal(
    viewModel: MainViewModel,
    downloadManager: DownloadManager,
    extFileDir: File?
) {


    val context = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()

    var mExpanded by remember { mutableStateOf(false) }
    var mSelectedText by remember { mutableStateOf("") }
    var mTextFieldSize by remember { mutableStateOf(Size.Zero) }
    var selectedModel by remember { mutableStateOf<Map<String, Any>?>(null) }

    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown

    // Search for local .gguf models
    val localModels = remember(extFileDir) {
        extFileDir?.listFiles { _, name -> name.endsWith(".gguf") }
            ?.map { file ->
                mapOf(
                    "name" to file.nameWithoutExtension,
                    "source" to file.toURI().toString(),
                    "destination" to file.name
                )
            } ?: emptyList()
    }
    // Combine local and remote models, ensuring uniqueness
    val combinedModels = remember(viewModel.allModels, localModels) {
        (viewModel.allModels + localModels).distinctBy { it["name"] }
    }
    viewModel.allModels = combinedModels


    Column(Modifier.padding(20.dp)) {

        OutlinedTextField(
            value= viewModel.loadedModelName.value,
            onValueChange = { mSelectedText = it },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    mTextFieldSize = coordinates.size.toSize()
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            mExpanded = !mExpanded
                        },
                        onPress = {
                            mExpanded = !mExpanded
                        }
                    )
                }
                .clickable {
                    mExpanded = !mExpanded
                },
            label = { Text("Select Model") },
            trailingIcon = {
                Icon(
                    icon,
                    contentDescription = "Toggle dropdown",
                    Modifier.clickable { mExpanded = !mExpanded },
                    tint =Color(0xFFcfcfd1)
                )
            },
            textStyle = TextStyle(color = Color(0xFFf5f5f5)),
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF666666),
                focusedBorderColor = Color(0xFFcfcfd1),
                unfocusedLabelColor = Color(0xFF666666),
                focusedLabelColor = Color(0xFFcfcfd1),
                unfocusedTextColor = Color(0xFFf5f5f5),
                focusedTextColor = Color(0xFFf7f5f5),
            )
        )



        DropdownMenu(
            modifier = Modifier
                .background(Color(0xFF01081a))
                .width(with(LocalDensity.current) { mTextFieldSize.width.toDp() })
                .padding(top = 2.dp)
                .border(1.dp, color = Color.LightGray.copy(alpha = 0.5f)),
            expanded = mExpanded,
            onDismissRequest = {
                mExpanded = false
            }
        ) {
            viewModel.allModels.forEach { model ->
                DropdownMenuItem(
                    modifier = Modifier
                        .background(color = Color(0xFF090b1a))
                        .padding(horizontal = 1.dp, vertical = 0.dp),
                    onClick = {
                        mSelectedText = model["name"].toString()
                        selectedModel = model
                        mExpanded = false

                        // Convert model to Downloadable and show modal
                        val downloadable = Downloadable(
                            name = model["name"].toString(),
                            source = Uri.parse(model["source"].toString()),
                            destination = File(extFileDir, model["destination"].toString())
                        )

                        viewModel.showModal = true
                        viewModel.currentDownloadable = downloadable
                    }
                ) {
                    model["name"]?.let { Text(text = it, color = Color.White) }
                }
            }
        }

        // Use showModal instead of switchModal

    }
}

