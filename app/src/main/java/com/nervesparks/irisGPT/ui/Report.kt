package com.nervesparks.irisGPT.ui

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.nervesparks.irisGPT.MainViewModel
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val processor: String = Build.HARDWARE,
    val totalRam: Long = 0,
    val availableRam: Long = 0
)

data class ReportContentNew(
    val reportText: String = "",
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val modelTemp: Float = 0f,
    val modelTopP: Float = 0f,
    val modelTopK: Int = 0,
    val modelHistory: List<Map<String, String>> = emptyList()
)

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        return networkInfo.isConnected
    }
}

@Composable
fun ReportScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reportText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = reportText,
            onValueChange = { reportText = it },
            label = { Text("Report Content") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            enabled = !isSending,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF666666),
                focusedBorderColor = Color(0xFFcfcfd1),
                unfocusedLabelColor = Color(0xFF666666),
                focusedLabelColor = Color(0xFFcfcfd1),
                unfocusedTextColor = Color(0xFFf5f5f5),
                focusedTextColor = Color(0xFFf7f5f5),
            )
        )

        if (showError) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF2563EB).copy(alpha = 0.6f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 1.dp
            ),
            onClick = {
                scope.launch {
                    if (!isInternetAvailable(context)) {
                        showError = true
                        errorMessage = "Internet connection required"
                        Toast.makeText(context, "No internet connection", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    isSending = true
                    showError = false
                    try {
                        val reportContent = createReportContent(
                            context,
                            reportText,
                            viewModel
                        )
                        val success = sendReportToFirebaseNew(reportContent)
                        if (success) {
                            reportText = ""
                            Toast.makeText(context, "Report sent successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            showError = true
                            errorMessage = "Failed to send report"
                            Toast.makeText(context, "Failed to send report", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ReportScreen", "Error sending report", e)
                        showError = true
                        errorMessage = e.message ?: "Unknown error occurred"
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isSending = false
                    }
                }
            },
            enabled = reportText.isNotBlank() && !isSending,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Send Report",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

suspend fun sendReportToFirebaseNew(reportContent: ReportContentNew): Boolean = suspendCoroutine { continuation ->
    try {
        val db = Firebase.firestore
        db.collection("reports")
            .add(reportContent)
            .addOnSuccessListener { documentReference ->
                Log.d("Firebase", "DocumentSnapshot added with ID: ${documentReference.id}")
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error adding document", e)
                continuation.resume(false)
            }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("Firebase", "Failed to send report to Firebase Firestore: ${e.message}")
        continuation.resume(false)
    }
}

private fun createReportContent(
    context: Context,
    reportText: String,
    viewModel: MainViewModel
): ReportContentNew {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val deviceInfo = DeviceInfo(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        processor = Build.HARDWARE,
        totalRam = memoryInfo.totalMem,
        availableRam = memoryInfo.availMem
    )

    return ReportContentNew(
        reportText = reportText,
        deviceInfo = deviceInfo,
        modelTemp = viewModel.temp,
        modelTopP = viewModel.topP,
        modelTopK = viewModel.topK,
        modelHistory = viewModel.messages
    )
}