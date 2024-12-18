package com.nervesparks.iris.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.LinearGradient

@Composable
fun SettingsScreen(OnModelsScreenButtonClicked: () -> Unit,
                   OnBackButtonClicked: (Int) -> Unit){
    LinearGradient()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp) // Add padding to the entire column for spacing
    ) {
        OutlinedButton(
            onClick = OnModelsScreenButtonClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // Transparent background
                contentColor = Color.White,
                disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp), // Slightly rounded corners
            modifier = Modifier
                .fillMaxWidth() // Make the button occupy the full width
                .height(56.dp) // Increase the height of the button
                .padding(vertical = 8.dp) // Add vertical padding for spacing
        ) {
            Text(text = "Models", color = Color.White, fontSize = 18.sp) // Larger text size
        }

        Divider(
            color = Color.Gray, // Line color
            thickness = 1.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .f
        )

        OutlinedButton(
            onClick = OnModelsScreenButtonClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Change Parameters", color = Color.White, fontSize = 18.sp)
        }
    }

}