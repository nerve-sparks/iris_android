package com.nervesparks.axia.agent

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nervesparks.axia.development.DevelopmentEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * System Controller - Direct Android system control
 */
class SystemController(private val context: Context) {
    
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    
    /**
     * WiFi Management
     */
    fun toggleWifi(enable: Boolean): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, we need to use system settings
                val intent = Intent(android.provider.Settings.Panel.ACTION_WIFI)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = enable
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Bluetooth Management
     */
    fun toggleBluetooth(enable: Boolean): Boolean {
        return try {
            val bluetoothAdapter = bluetoothManager.adapter
            if (enable && !bluetoothAdapter.isEnabled) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else if (!enable && bluetoothAdapter.isEnabled) {
                bluetoothAdapter.disable()
                true
            } else {
                true // Already in desired state
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Volume Control
     */
    fun setVolume(volumePercent: Int): Boolean {
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val volume = (maxVolume * volumePercent / 100).coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Launch Application
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Take Screenshot (requires system permissions)
     */
    fun takeScreenshot(): String? {
        return try {
            val screenshotDir = File(context.getExternalFilesDir(null), "screenshots")
            screenshotDir.mkdirs()
            
            val filename = "screenshot_${System.currentTimeMillis()}.png"
            val screenshotFile = File(screenshotDir, filename)
            
            // Use shell command to take screenshot (requires root)
            val process = ProcessBuilder("su", "-c", "screencap -p ${screenshotFile.absolutePath}").start()
            process.waitFor()
            
            if (screenshotFile.exists()) {
                screenshotFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get device information
     */
    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "version" to Build.VERSION.RELEASE,
            "sdk" to Build.VERSION.SDK_INT.toString(),
            "board" to Build.BOARD,
            "hardware" to Build.HARDWARE
        )
    }
    
    /**
     * Execute root command
     */
    suspend fun executeRootCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("su", "-c", command).start()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            
            if (error.isNotEmpty()) error else output
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

/**
 * Phone Controller - Direct phone functionality control
 */
class PhoneController(private val context: Context) {
    
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    
    /**
     * Make Phone Call
     */
    fun makeCall(phoneNumber: String): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                == PackageManager.PERMISSION_GRANTED) {
                
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Send SMS
     */
    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED) {
                
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get Contacts List
     */
    fun getContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            == PackageManager.PERMISSION_GRANTED) {
            
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: ""
                    val number = it.getString(numberIndex) ?: ""
                    contacts.add(Contact(name, number))
                }
            }
        }
        
        return contacts
    }
    
    /**
     * Answer incoming call
     */
    fun answerCall(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager.acceptRingingCall()
                true
            } else {
                // Use accessibility service or other method for older versions
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * End current call
     */
    fun endCall(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecomManager.endCall()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Terminal Controller - Enhanced terminal with system access
 */
class TerminalController(private val developmentEnvironment: DevelopmentEnvironment) {
    
    /**
     * Execute command with elevated privileges
     */
    suspend fun executeCommand(command: String, workingDirectory: String): com.nervesparks.axia.development.CommandResult {
        return try {
            // Try with root access first
            val rootResult = executeRootCommand(command, workingDirectory)
            if (rootResult.success) {
                rootResult
            } else {
                // Fallback to regular execution
                developmentEnvironment.executeCommand(command, workingDirectory)
            }
        } catch (e: Exception) {
            com.nervesparks.axia.development.CommandResult(
                success = false,
                output = "",
                error = e.message ?: "Unknown error",
                exitCode = -1
            )
        }
    }
    
    private suspend fun executeRootCommand(command: String, workingDirectory: String): com.nervesparks.axia.development.CommandResult {
        return try {
            val process = ProcessBuilder("su", "-c", "cd '$workingDirectory' && $command").start()
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            com.nervesparks.axia.development.CommandResult(
                success = exitCode == 0,
                output = output,
                error = error,
                exitCode = exitCode
            )
        } catch (e: Exception) {
            com.nervesparks.axia.development.CommandResult(
                success = false,
                output = "",
                error = e.message ?: "Root execution failed",
                exitCode = -1
            )
        }
    }
    
    /**
     * Create file with system permissions
     */
    suspend fun createFile(path: String, content: String): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            
            // Set appropriate permissions
            executeCommand("chmod 644 '$path'", file.parent ?: "/")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Install APK file
     */
    suspend fun installApk(apkPath: String): Boolean {
        val result = executeCommand("pm install '$apkPath'", "/")
        return result.success
    }
    
    /**
     * Grant application permissions
     */
    suspend fun grantPermission(packageName: String, permission: String): Boolean {
        val result = executeCommand("pm grant $packageName $permission", "/")
        return result.success
    }
    
    /**
     * Execute system service commands
     */
    suspend fun controlSystemService(serviceName: String, action: String): Boolean {
        val result = executeCommand("service $action $serviceName", "/")
        return result.success
    }
}

/**
 * Image Generator using local AI models
 */
class ImageGenerator(
    private val context: Context,
    private val memoryManager: com.nervesparks.axia.memory.AxiaMemoryManager
) {
    
    suspend fun generateImage(
        prompt: String,
        style: String = "realistic",
        width: Int = 1024,
        height: Int = 1024
    ): String? {
        return try {
            // Allocate memory for image generation (use swap for processing)
            val imageMemory = memoryManager.allocateMemory(
                size = (width * height * 4).toLong(), // RGBA
                priority = com.nervesparks.axia.memory.MemoryPriority.BACKGROUND,
                description = "Image generation buffer"
            )
            
            // Use Stable Diffusion or similar local model
            val imageDir = File(context.getExternalFilesDir(null), "generated_images")
            imageDir.mkdirs()
            
            val filename = "generated_${System.currentTimeMillis()}.png"
            val imageFile = File(imageDir, filename)
            
            // Run image generation (this would use a local AI model like Stable Diffusion)
            val success = runStableDiffusion(prompt, style, width, height, imageFile.absolutePath)
            
            if (success && imageFile.exists()) {
                imageFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun runStableDiffusion(
        prompt: String,
        style: String,
        width: Int,
        height: Int,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // This would integrate with a local Stable Diffusion model
            // For now, creating a placeholder implementation
            
            val command = """
                python3 /data/data/com.nervesparks.axia/models/stable_diffusion/generate.py \
                --prompt "$prompt" \
                --style "$style" \
                --width $width \
                --height $height \
                --output "$outputPath"
            """.trimIndent()
            
            val process = ProcessBuilder("sh", "-c", command).start()
            val exitCode = process.waitFor()
            
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}

data class Contact(
    val name: String,
    val phoneNumber: String
)