package com.nervesparks.iris.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen(OnModelsScreenButtonClicked: () -> Unit,
                   OnBackButtonClicked: (Int) -> Unit){
    Text(text = "This is Settings Screen")
    Button(onClick = OnModelsScreenButtonClicked) {
        Text(text = "Go to Models Screen")
    }

}