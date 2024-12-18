package com.nervesparks.iris.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.components.LoadingModal
import kotlinx.coroutines.withContext

@Composable

fun ParametersScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Top column with flexible weight
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .shadow(
                    elevation = 8.dp, // Shadow elevation
                    shape = RoundedCornerShape(8.dp) // Shape of the shadow
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xff0f172a),
                contentColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(15.dp)
            ) {
                if (viewModel.showAlert) {
                    // Modal dialog to show download options
                    LoadingModal(viewModel)

                }
                Text(
                    text = "Select thread for process, 0 for default",
                    color = Color.White
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
                        thumbColor = Color(0xFF2563EB),
                        activeTrackColor = Color(0xFF2563EB),
                        inactiveTrackColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "After changing please Save the changes!!",
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(15.dp))
            }
        }

        // Bottom buttons side by side
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            //Default button
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
                    viewModel.user_thread = 0f
                    viewModel.currentDownloadable?.destination?.path?.let {
                        viewModel.load(it, viewModel.user_thread.toInt())
                        Toast.makeText(context, "Settings reset to default", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Reset Default")
            }

            Spacer(modifier = Modifier.width(8.dp))
            // save button
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
                    viewModel.currentDownloadable?.destination?.path?.let {
                        viewModel.load(it, viewModel.user_thread.toInt())
                        Toast.makeText(context, "Changes have been saved", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save")
            }
        }
    }
}

