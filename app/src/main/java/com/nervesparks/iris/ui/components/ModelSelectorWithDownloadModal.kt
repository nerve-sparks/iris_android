package com.nervesparks.iris.ui.components

import android.app.Activity
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

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
                    tint = Color(0xFFcfcfd1)
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
        if (viewModel.showModal && viewModel.currentDownloadable != null) {
            Dialog(onDismissRequest = {
                viewModel.showModal = false  // Consistent with the condition
                viewModel.currentDownloadable = null  // Optional: clear the current downloadable
            }) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black,
                    modifier = Modifier
                        .padding(10.dp)
                        .height(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .height(140.dp),
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
                        // Use the current downloadable from the view model
                        viewModel.currentDownloadable?.let { downloadable ->
                            Downloadable.Button(viewModel, downloadManager, downloadable)
                            if (downloadable.destination.exists()){
                                Spacer(modifier = Modifier.height(25.dp))
                                Button(
                                    onClick = {

                                        coroutineScope.launch {  viewModel.unload()}
                                        // Delete the model file
                                        downloadable.destination.delete()
                                        // Reset dialog visibility and update UI
                                        viewModel.showModal = false
                                        viewModel.currentDownloadable = null

                                        Toast.makeText(context, "Restarting App!!.", Toast.LENGTH_SHORT).show()
                                        val packageManager: PackageManager = context.packageManager
                                        val intent: Intent = packageManager.getLaunchIntentForPackage(context.packageName)!!
                                        val componentName: ComponentName = intent.component!!
                                        val restartIntent: Intent = Intent.makeRestartActivityTask(componentName)
                                        context.startActivity(restartIntent)
                                        Runtime.getRuntime().exit(0)



                                    },
                                ) {
                                    Text(text = "Delete Model")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}