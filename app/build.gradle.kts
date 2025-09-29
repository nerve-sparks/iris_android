plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nervesparks.axia"
    compileSdk = 35

    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.nervesparks.axia"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            // Optimize for Samsung Galaxy S24 Ultra (Snapdragon 8 Gen 3)
            abiFilters += listOf("arm64-v8a")
        }
        
        // Samsung Galaxy S24 Ultra specific optimizations
        multiDexEnabled = true
        
        // Performance optimizations
        renderscriptTargetApi = 35
        renderscriptSupportModeEnabled = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            
            // Samsung Galaxy S24 Ultra optimizations
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
            renderscriptOptimLevel = 3
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/attach_hotspot_windows.dll"
            excludes += "META-INF/licenses/**"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
    
    // Samsung Galaxy S24 Ultra specific configurations
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
//    externalNativeBuild {
//        cmake {
//            path = file("src/main/cpp/CMakeLists.txt")
//            version = "3.22.1"
//        }
//    }
}

dependencies {
    // Core AndroidX libraries with latest versions
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    
    // Latest Compose BOM for Material 3
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.animation:animation-graphics")
    
    // Material 3 with full feature set
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation optimized for large screens
    implementation("androidx.navigation:navigation-runtime-ktx:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    
    // Enhanced UI components
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.6")
    implementation("androidx.compose.foundation:foundation-layout-android:1.7.6")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")
    implementation("androidx.paging:paging-compose:3.3.5")
    implementation("androidx.paging:paging-runtime-ktx:3.3.5")
    
    // Samsung Galaxy S24 Ultra optimizations
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.window:window-core:1.3.0")
    
    // Performance and memory optimizations
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.datastore:datastore-core:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    
    // Room database for long-term memory
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // Enhanced coroutines and flow support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    
    // Compose state management
    implementation("androidx.compose.runtime:runtime-saveable:1.7.6")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.6")
    
    // Network dependencies for Hugging Face & Contractor APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // JSON processing
    implementation("com.google.code.gson:gson:2.11.0")
    
    // Security and encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // System permissions for autonomous phone control
    implementation("androidx.core:core-role:1.1.0")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    
    // Project modules
    implementation(project(":llama"))
    
    // Legacy support (minimal)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    
    // Core library desugaring for Java 8+ APIs
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.5")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
}