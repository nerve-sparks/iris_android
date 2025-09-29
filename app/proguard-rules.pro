# Samsung Galaxy S24 Ultra Optimized ProGuard Rules
# Enhanced configuration for optimal performance on Snapdragon 8 Gen 3

# Standard optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Samsung Galaxy S24 Ultra specific optimizations
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin optimizations for Snapdragon 8 Gen 3
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
}

# AndroidX and Jetpack Compose optimizations
-keep class androidx.compose.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.core.** { *; }
-dontwarn androidx.**

# Material 3 optimizations
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# LLaMA Android native library optimizations
-keep class android.llama.cpp.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Application specific classes - keep main components
-keep class com.nervesparks.iris.** { *; }
-keep class com.nervesparks.iris.MainActivity { *; }
-keep class com.nervesparks.iris.MainViewModel { *; }

# DataStore optimizations for Samsung performance
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# OkHttp optimizations for network performance
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Samsung Galaxy S24 Ultra graphics and performance optimizations
-keep class android.graphics.** { *; }
-keep class android.view.** { *; }
-keep class android.widget.** { *; }

# Remove debug logging in release builds for better performance
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Samsung specific system optimizations
-keep class com.samsung.android.** { *; }
-dontwarn com.samsung.android.**

# Keep serializable classes for state preservation
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Parcelable optimizations for Samsung's enhanced IPC
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Coroutines optimizations for multi-core Snapdragon performance
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Remove unnecessary R8 warnings
-dontwarn java.lang.invoke.StringConcatFactory
