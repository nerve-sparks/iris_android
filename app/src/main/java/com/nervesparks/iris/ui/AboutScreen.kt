package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            SectionHeader(text = "Welcome to Matrix AI")
        }
        item {
            Text(
                text = "Matrix AI is an offline Android chat application powered by the llama.cpp framework. " +
                        "Designed to operate entirely offline, it ensures privacy and independence from external servers. " +
                        "Whether you're a developer exploring AI applications or a privacy-conscious user, " +
                        "Matrix AI provides a seamless and secure way to experience conversational AI. " +
                        "Please note that the app may occasionally generate inaccurate results.",
                fontSize = 15.sp,
                color = MatrixGreen.copy(alpha = 0.85f),
                lineHeight = 22.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SectionHeader(text = "Features")
        }

        items(features) { feature ->
            FeatureItem(feature = feature)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(text = "FAQs")
        }

        items(faqs) { faq ->
            FaqItem(question = faq.first, answer = faq.second)
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "> $text",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MatrixGreenBright,
        fontFamily = FontFamily.Monospace,
        modifier = modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun FeatureItem(feature: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(MatrixGreenDark, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MatrixGreenBright,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = feature,
            fontSize = 14.sp,
            color = MatrixGreen.copy(alpha = 0.8f),
            lineHeight = 20.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(MatrixGreenDark, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MatrixGreenBright,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = question,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MatrixGreenBright,
                lineHeight = 20.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            fontSize = 13.sp,
            color = MatrixGreen.copy(alpha = 0.65f),
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 32.dp)
        )
    }
}

private val features = listOf(
    "Offline Functionality: Runs without the need for an internet connection.",
    "Privacy First: All data is processed locally on your device.",
    "Customizable Models: Download and use your preferred AI model with ease.",
    "Matrix Interface: Immersive digital rain aesthetic powered by real-time Canvas.",
    "Open Source: Built on the foundations of llama.cpp, enabling developers to contribute."
)

private val faqs = listOf(
    "What is Matrix AI?" to "Matrix AI is an offline AI chat assistant with a Matrix-themed interface, powered by llama.cpp running entirely on your device.",
    "Do I need an internet connection?" to "Only to download models. After that, Matrix AI operates entirely offline. All inference is done locally.",
    "Which AI models are supported?" to "Matrix AI supports GGUF models. You can download and integrate them as needed from Hugging Face.",
    "Is my data safe?" to "Yes. Since Matrix AI works offline, no data is transmitted to external servers, ensuring complete privacy.",
    "How do I change parameters?" to "Settings > Change Parameters > Modify the parameters > Save changes.",
    "How do I download models online?" to "Go to Settings > Models > Search Hugging Face Models > Enter the model name > Download.",
    "How do I delete a model?" to "Go to Settings > Models > Select the model > Click the Delete button."
)
