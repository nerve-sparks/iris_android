package com.nervesparks.iris

import android.app.Application
import com.nervesparks.iris.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
@HiltAndroidApp
class IrisStarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
