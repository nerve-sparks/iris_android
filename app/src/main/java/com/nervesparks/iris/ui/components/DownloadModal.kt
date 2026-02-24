package com.nervesparks.iris.ui.components

import android.app.DownloadManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.Downloadable
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.MatrixBg
import com.nervesparks.iris.ui.MatrixGreen
import com.nervesparks.iris.ui.MatrixGreenBright
import com.nervesparks.iris.ui.MatrixGreenDark
import com.nervesparks.iris.ui.MatrixGreenMid

@Composable
fun DownloadModal(viewModel: MainViewModel, dm: DownloadManager, models: List<Downloadable>) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MatrixBg,
            modifier = Modifier
                .padding(10.dp)
                .height(320.dp)
                .fillMaxWidth()
                .border(1.dp, MatrixGreenDark, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "> DOWNLOAD_REQUIRED",
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreenBright,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "! Keep app open during download",
                    color = MatrixGreenMid,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Download at least 1 model to start",
                    fontWeight = FontWeight.Bold,
                    color = MatrixGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )

                // Separator
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MatrixGreenDark))

                LazyColumn(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(models.filter { !it.destination.exists() }) { model ->
                        MatrixDownloadCard(viewModel, dm, model)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixDownloadCard(viewModel: MainViewModel, dm: DownloadManager, model: Downloadable) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, MatrixGreenDark, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MatrixGreenDark.copy(alpha = 0.2f),
            contentColor = MatrixGreen
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = model.name,
                color = MatrixGreen.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Downloadable.Button(viewModel, dm, model)
        }
    }
}
