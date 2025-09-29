#!/bin/bash

# 🚀 AXIA Enhanced Build & Test Script
# This script will build your APK and help you test it

echo "🔧 Building your enhanced Axia app with contractor system..."

# Step 1: Clean the project
echo "📱 Cleaning project..."
./gradlew clean

# Step 2: Build the debug APK
echo "🔨 Building APK (this may take a few minutes)..."
timeout 300 ./gradlew assembleDebug --no-daemon --offline

# Step 3: Check if APK was created
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ SUCCESS! APK built successfully!"
    echo "📱 Your APK is at: app/build/outputs/apk/debug/app-debug.apk"
    echo "📊 APK size: $(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)"
    
    echo ""
    echo "🚀 INSTALLATION OPTIONS:"
    echo "1. Connect Android device via USB and run: adb install app/build/outputs/apk/debug/app-debug.apk"
    echo "2. Copy APK to your device and install manually"
    echo "3. Use Android Studio to install and run"
    echo ""
    echo "📱 Your Axia app now includes:"
    echo "  ✅ 100% FREE local AI models (Hugging Face)"
    echo "  ✅ Optional cloud APIs (OpenAI, Claude, Gemini, Cohere)"
    echo "  ✅ Autonomous phone control & automation"
    echo "  ✅ Advanced development tools"
    echo "  ✅ Secure API key management"
    echo "  ✅ Professional content generation"
    echo ""
    echo "🎯 Ready to test! Launch the app and try:"
    echo "  • 'Hello, show me your new capabilities'"
    echo "  • 'Download a free local model'"
    echo "  • 'Add my API key' (optional)"
    echo "  • 'Send a text message'"
    echo "  • 'Help me write code'"
    
else
    echo "❌ Build failed or APK not found"
    echo "🔍 Checking for build errors..."
    
    if [ -f "app/build/outputs/logs/manifest_merge_blame_file/debug/manifest-merger-blame-debug-report.txt" ]; then
        echo "📄 Manifest merge issues found - check the report"
    fi
    
    echo "💡 Try these troubleshooting steps:"
    echo "1. ./gradlew clean"
    echo "2. Check Android SDK is properly installed"
    echo "3. Ensure ANDROID_HOME is set"
    echo "4. Try: ./gradlew assembleDebug --stacktrace --info"
fi

echo ""
echo "📚 For complete feature list and testing guide, see:"
echo "    📖 TESTING_AND_DEPLOYMENT_GUIDE.md"
echo "    📖 AXIA_CONTRACTOR_API_SYSTEM.md"