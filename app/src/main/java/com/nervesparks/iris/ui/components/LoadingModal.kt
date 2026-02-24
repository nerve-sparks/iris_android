package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.ui.LoadingMatrixAnimation
import com.nervesparks.iris.ui.MatrixBg
import com.nervesparks.iris.ui.MatrixGreen

/** Kept for backwards compatibility — renders the Matrix loading dialog */
@Composable
fun LoadingModal(viewModel: MainViewModel) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape  = RoundedCornerShape(16.dp),
            color  = Color(0xFF000D00),
            shadowElevation = 24.dp,
            modifier = Modifier.size(280.dp, 220.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MatrixBg)
            ) {
                LoadingMatrixAnimation(
                    label = "LOADING MODEL",
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = viewModel.loadedModelName.value,
                    color = MatrixGreen.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                )
            }
        }
    }
}
