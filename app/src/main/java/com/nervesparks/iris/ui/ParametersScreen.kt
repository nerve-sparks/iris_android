package com.nervesparks.iris.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.LoadingModal


@Composable
fun ParametersScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    // Main container with fillMaxSize
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {


            Text(
                text = "After changing please Save the changes",
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

        // Card with sliders taking weight of 1f to fill available space
        androidx.compose.material3.Card(
            modifier = Modifier
                .weight(1f) // This makes the card fill available space while allowing buttons to stay at bottom
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xff0f172a),
                contentColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (viewModel.showAlert) {
                LoadingModal(viewModel)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize() // Fill the card's space
                    .padding(15.dp)
            ) {
                item {
                    SettingSection(
                        title = "Thread Selection",
                        description = "Select thread for process, 0 for default"
                    ) {
                        Text(
                            text = "${viewModel.user_thread.toInt()}",
                            color = Color.White
                        )
                        Slider(
                            value = viewModel.user_thread,
                            onValueChange = { viewModel.user_thread = it },
                            valueRange = 0f..8f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2563EB),
                                activeTrackColor = Color(0xFF2563EB),
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }
                }

                item { SectionDivider() }

                item {
                    SettingSection(
                        title = "Temperature",
                        description = "Adjust randomness (0.0 - 1.0)"
                    ) {
                        Text(
                            text = String.format("%.2f", viewModel.temp),
                            color = Color.White
                        )
                        Slider(
                            value = viewModel.temp,
                            onValueChange = { viewModel.temp = it },
                            valueRange = 0f..1f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2563EB),
                                activeTrackColor = Color(0xFF2563EB),
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }
                }

                item { SectionDivider() }

                item {
                    SettingSection(
                        title = "Top P",
                        description = "Nucleus sampling threshold (0.0 - 1.0)"
                    ) {
                        Text(
                            text = String.format("%.2f", viewModel.topP),
                            color = Color.White
                        )
                        Slider(
                            value = viewModel.topP,
                            onValueChange = { viewModel.topP = it },
                            valueRange = 0f..1f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2563EB),
                                activeTrackColor = Color(0xFF2563EB),
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }
                }

                item { SectionDivider() }

                item {
                    SettingSection(
                        title = "Top K",
                        description = "Number of tokens to consider (0 - 50)"
                    ) {
                        Text(
                            text = "${viewModel.topK.toInt()}",
                            color = Color.White
                        )
                        Slider(
                            value = viewModel.topK.toFloat(),
                            onValueChange = { viewModel.topK = it.toInt() },
                            valueRange = 0f..50f,
                            steps = 49,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2563EB),
                                activeTrackColor = Color(0xFF2563EB),
                                inactiveTrackColor = Color.Gray
                            )
                        )
                    }
                }


            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons row outside the card, will stay at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9CA3AF).copy(alpha = 1.0f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 3.dp
                ),
                onClick = {
                    if(viewModel.loadedModelName.value == "") {
                        Toast.makeText(context, "Load A Model First", Toast.LENGTH_SHORT).show()
                    }else {
                        viewModel.user_thread = 0f
                        viewModel.temp = 0f
                        viewModel.topK = 0
                        viewModel.topP = 0f
                        viewModel.currentDownloadable?.destination?.path?.let {
                            viewModel.load(it, viewModel.user_thread.toInt())
                            Toast.makeText(context, "Settings reset to default", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            ) {
                Text("Reset Default")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB).copy(alpha = 1.0f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 3.dp
                ),
                onClick = {
                    if(viewModel.loadedModelName.value == "") {
                        Toast.makeText(context, "Load A Model First", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.currentDownloadable?.destination?.path?.let {
                            viewModel.load(it, viewModel.user_thread.toInt())
                            Toast.makeText(context, "Changes have been saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text("Save")
            }
        }
    }
}
@Composable
private fun SettingSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = description,
        color = Color.Gray,
        style = MaterialTheme.typography.caption
    )
    Spacer(modifier = Modifier.height(8.dp))
    content()
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SectionDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.DarkGray,
        thickness = 1.dp
    )
}
