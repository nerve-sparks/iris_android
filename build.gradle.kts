// Top-level build file optimized for Samsung Galaxy S24 Ultra development
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.22" apply false
}

// Samsung Galaxy S24 Ultra build optimizations
allprojects {
    configurations.all {
        resolutionStrategy {
            // Force latest versions for Samsung compatibility
            force("androidx.compose.compiler:compiler:1.5.8")
            force("androidx.compose.runtime:runtime:1.6.0")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Samsung Galaxy S24 Ultra specific build configurations
ext {
    set("compileSdkVersion", 35)
    set("targetSdkVersion", 35)
    set("minSdkVersion", 28)
    set("versionCode", 2)
    set("versionName", "1.1.0")
    
    // Samsung optimizations
    set("jvmTarget", "17")
    set("kotlinCompilerExtensionVersion", "1.5.8")
    set("composeBomVersion", "2024.12.01")
}
