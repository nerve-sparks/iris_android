package com.nervesparks.iris.ui

import android.app.DownloadManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.ModelCard
import java.io.File

@Composable
fun ModelsScreen(extFileDir: File?, viewModel: MainViewModel, onSearchResultButtonClick: () -> Unit, dm: DownloadManager) {
//    val models = listOf(
//        Downloadable(
//            "Llama-3.2-3B-Instruct-Q4_K_L",
//            Uri.parse("https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_L.gguf?download=true"),
//            File(extFileDir, "Llama-3.2-3B-Instruct-Q4_K_L.gguf")
//        ),
//        Downloadable(
//            "Llama-3.2-1B-Instruct-Q6_K_L",
//            Uri.parse("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q6_K_L.gguf?download=true"),
//            File(extFileDir, "Llama-3.2-1B-Instruct-Q6_K_L.gguf")
//        ),
//        Downloadable(
//            "stablelm-2-1_6b-chat.Q4_K_M.imx",
//            Uri.parse("https://huggingface.co/Crataco/stablelm-2-1_6b-chat-imatrix-GGUF/resolve/main/stablelm-2-1_6b-chat.Q4_K_M.imx.gguf?download=true"),
//            File(extFileDir, "stablelm-2-1_6b-chat.Q4_K_M.imx.gguf")
//        )
//    )
//
//    if (extFileDir != null) {
//        viewModel.loadExistingModels(extFileDir)
//    }
//
//    val allModelsExist = models.all { model -> model.destination.exists() }
//
//    if (allModelsExist) {
//        viewModel.showModal = false
//    }

    Box {
        LazyColumn (modifier = Modifier.padding(horizontal = 15.dp)){
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .clickable {
                                onSearchResultButtonClick()
                            }
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp), // Icon size,
                            painter = painterResource(id = R.drawable.search_svgrepo_com__3_),
                            contentDescription = "Parameters",
                            tint = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Search Hugging-Face Models",
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .padding(vertical = 12.dp, horizontal = 7.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = Color.DarkGray, // Set the color of the divider
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(25.dp))
                    // Suggested Models Section
                    Text(text = "Suggested Models", color = Color.White.copy(alpha = .5f), modifier = Modifier.padding(5.dp),fontSize = 18.sp)
                }
            }


            // Show first three suggested models
            items(viewModel.allModels.take(3)) { model ->
                extFileDir?.let {
                    model["source"]?.let { source ->
                        ModelCard(
                            model["name"].toString(),
                            viewModel = viewModel,
                            dm = dm,
                            extFilesDir = extFileDir,
                            downloadLink = source,
                            showDeleteButton = true
                        )
                    }
                }
            }
            item {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = Color.DarkGray, // Set the color of the divider
                    thickness = 1.dp
                )
            }

            item {
                // My Models Section
                Text(text = "My Models", color = Color.White.copy(alpha = .5f), modifier = Modifier.padding(5.dp),fontSize = 18.sp)
            }



            // Display all models not in Suggested Models
            items(viewModel.allModels.drop(3)) { model ->
                extFileDir?.let {
                    model["source"]?.let { source ->
                        ModelCard(
                            model["name"].toString(),
                            viewModel = viewModel,
                            dm = dm,
                            extFilesDir = extFileDir,
                            downloadLink = source,
                            showDeleteButton = true
                        )
                    }
                }
            }
        }
    }
}