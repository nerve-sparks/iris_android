package com.nervesparks.iris.agent

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import android.telecom.TelecomManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Action Executor for System-level Operations
 * Handles phone calls, settings, app launching, etc.
 */

@Singleton
class ActionExecutor @Inject constructor(
    private val context: Context
) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    
    /**
     * Make a phone call
     */
    fun makeCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to dial intent if call permission not available
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        }
    }
    
    /**
     * Send SMS text message
     */
    fun sendText(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Open an app by name
     */
    fun openApp(appName: String) {
        try {
            // Try to find app by package name or display name
            val packageManager = context.packageManager
            val intent = when (appName.lowercase()) {
                "settings" -> Intent(Settings.ACTION_SETTINGS)
                "camera" -> Intent("android.media.action.IMAGE_CAPTURE")
                "phone", "dialer" -> Intent(Intent.ACTION_DIAL)
                "messages", "sms" -> Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:")
                }
                "contacts" -> Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("content://contacts/people")
                }
                "calculator" -> packageManager.getLaunchIntentForPackage("com.google.android.calculator")
                "chrome" -> packageManager.getLaunchIntentForPackage("com.android.chrome")
                "gmail" -> packageManager.getLaunchIntentForPackage("com.google.android.gm")
                else -> {
                    // Try to find app by searching installed packages
                    val installedApps = packageManager.getInstalledApplications(0)
                    val matchingApp = installedApps.find { appInfo ->
                        val label = packageManager.getApplicationLabel(appInfo).toString()
                        label.contains(appName, ignoreCase = true)
                    }
                    matchingApp?.let { packageManager.getLaunchIntentForPackage(it.packageName) }
                }
            }
            
            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(it)
            }
            
        } catch (e: Exception) {
            // App not found or couldn't launch
            throw Exception("Could not open app: $appName")
        }
    }
    
    /**
     * Toggle WiFi on/off
     */
    @Suppress("DEPRECATION")
    fun toggleWifi(enable: Boolean) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // For Android 10+, open WiFi settings
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } else {
                // For older versions
                wifiManager.isWifiEnabled = enable
            }
        } catch (e: Exception) {
            throw Exception("Could not toggle WiFi: ${e.message}")
        }
    }
    
    /**
     * Toggle Bluetooth on/off
     */
    fun toggleBluetooth(enable: Boolean) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                if (enable && !bluetoothAdapter.isEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } else if (!enable && bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.disable()
                }
            }
        } catch (e: Exception) {
            throw Exception("Could not toggle Bluetooth: ${e.message}")
        }
    }
    
    /**
     * Set screen brightness (0-100)
     */
    fun setBrightness(level: Int) {
        try {
            val brightnessValue = (level * 255) / 100
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        } catch (e: Exception) {
            // Open brightness settings if direct control fails
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Set media volume (0-100)
     */
    fun setVolume(level: Int) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val volume = (level * maxVolume) / 100
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            throw Exception("Could not set volume: ${e.message}")
        }
    }
    
    /**
     * Take a screenshot
     */
    fun takeScreenshot() {
        // This requires system-level permissions or accessibility service
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        // Use accessibility service to take screenshot if available
        // This is a placeholder - actual implementation would need accessibility permissions
    }
    
    /**
     * Open URL in browser
     */
    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Share text
     */
    fun shareText(text: String, title: String = "Share") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val chooser = Intent.createChooser(intent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }
    
    /**
     * Search on Google
     */
    fun searchGoogle(query: String) {
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        openUrl(searchUrl)
    }
    
    /**
     * Set alarm
     */
    fun setAlarm(hour: Int, minute: Int, message: String = "") {
        val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            if (message.isNotEmpty()) {
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Set timer
     */
    fun setTimer(seconds: Int, message: String = "") {
        val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
            if (message.isNotEmpty()) {
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, message)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        context.startActivity(intent)
    }
}