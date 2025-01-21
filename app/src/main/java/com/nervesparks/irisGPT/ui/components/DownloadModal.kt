package com.nervesparks.irisGPT.ui.components

import android.app.DownloadManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nervesparks.irisGPT.Downloadable
import com.nervesparks.irisGPT.MainViewModel

@Composable
fun DownloadModal(viewModel: MainViewModel, dm: DownloadManager, models: List<Downloadable>) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF233340),
            modifier = Modifier
                .padding(10.dp)
                .height(300.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeaderText("Download Required")
                HeaderText("Don't close or minimize the app!")

                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(models.filter { !it.destination.exists() }) { model ->
                        DownloadCard(viewModel, dm, model)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
private fun DownloadCard(viewModel: MainViewModel, dm: DownloadManager, model: Downloadable) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xff0f172a),
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = model.name,
                color = Color(0xFFbbbdbf)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Downloadable.Button(viewModel, dm, model)
        }
    }
}
