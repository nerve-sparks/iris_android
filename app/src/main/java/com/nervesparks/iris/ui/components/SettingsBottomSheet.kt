package com.nervesparks.iris.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel

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