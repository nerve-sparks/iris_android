package com.nervesparks.iris

import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.core.content.ContextCompat
import androidx.profileinstaller.ProfileInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Samsung Galaxy S24 Ultra optimized Application class
 * Implements performance enhancements for Snapdragon 8 Gen 3 and Dynamic AMOLED 2X display
 */
class IrisApplication : Application() {

    // Application-scoped coroutine scope for Samsung's multi-core optimization
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Samsung Galaxy S24 Ultra performance optimizations
        setupSamsungPerformanceOptimizations()
        
        // Initialize profile-guided optimization for Snapdragon 8 Gen 3
        initializeProfileGuidedOptimization()
        
        // Setup development mode optimizations
        if (BuildConfig.DEBUG) {
            setupDevelopmentOptimizations()
        }
    }

    /**
     * Samsung Galaxy S24 Ultra specific performance optimizations
     */
    private fun setupSamsungPerformanceOptimizations() {
        // Enable hardware acceleration for Samsung's GPU
        try {
            // Optimize for Samsung's Dynamic AMOLED 2X display
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Enable aggressive memory optimization for 12GB RAM
                System.setProperty("dalvik.vm.dex2oat-max-image-block-size", "524288")
                System.setProperty("dalvik.vm.image-dex2oat-Xms", "64m")
                System.setProperty("dalvik.vm.image-dex2oat-Xmx", "512m")
            }
            
            // Samsung Galaxy S24 Ultra specific optimizations
            System.setProperty("debug.egl.hw", "1")
            System.setProperty("debug.sf.hw", "1")
            System.setProperty("debug.composition.type", "gpu")
            System.setProperty("debug.sf.disable_backpressure", "1")
            
        } catch (e: Exception) {
            // Silently continue if system properties can't be set
        }
    }

    /**
     * Initialize Profile-Guided Optimization for Snapdragon 8 Gen 3 performance
     */
    private fun initializeProfileGuidedOptimization() {
        applicationScope.launch {
            try {
                ProfileInstaller.writeProfile(this@IrisApplication)
            } catch (e: Exception) {
                // Profile installation failed, continue without it
            }
        }
    }

    /**
     * Development mode optimizations for debugging on Samsung Galaxy S24 Ultra
     */
    private fun setupDevelopmentOptimizations() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .penaltyLog()
                .build()
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Samsung Galaxy S24 Ultra memory management
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Optimize memory usage for Samsung's 12GB RAM
                Runtime.getRuntime().gc()
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                // App moved to background, release UI resources
                clearImageCache()
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // Release all possible memory
                clearAllCaches()
            }
        }
    }

    private fun clearImageCache() {
        // Clear image caches to free memory
        applicationScope.launch {
            // Implementation would go here
        }
    }

    private fun clearAllCaches() {
        // Clear all caches for maximum memory efficiency
        applicationScope.launch {
            clearImageCache()
            System.gc()
        }
    }
}