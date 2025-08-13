# Google Custom Search API Setup Guide

## 🔍 Setting Up Real Web Search

To enable real web search functionality in Iris, you need to set up Google Custom Search API.

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the "Custom Search API" for your project

### Step 2: Get API Key

1. In Google Cloud Console, go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "API Key"
3. Copy the API key (you'll need this for the app)

### Step 3: Create Custom Search Engine

1. Go to [Google Programmable Search Engine](https://programmablesearchengine.google.com/)
2. Click "Create a search engine"
3. Enter any website (e.g., `https://www.google.com`)
4. Give it a name (e.g., "Iris AI Search")
5. Click "Create"
6. Go to "Setup" > "Search engine ID"
7. Copy the Search Engine ID (starts with numbers)

### Step 4: Configure in the App (no code changes)

1. Open the app and go to Settings → HuggingFace & Web Search.
2. Enter your Google API Key and your Custom Search Engine ID.
3. These values are stored securely using encrypted preferences. Do not hardcode keys in source.

### Step 5: Build and Test

```bash
./gradlew assembleDebug installDebug
```

## 🎯 Benefits

- ✅ **Real Google search results**
- ✅ **100 free searches per day**
- ✅ **Reliable and fast**
- ✅ **Comprehensive results**

## 🔄 Fallback System

If Google API is not configured, the app will:
1. Try DuckDuckGo API
2. Provide helpful fallback information
3. Give search tips and alternatives

## 📱 Alternative: Android System Integration

If you prefer not to use external APIs, we can also implement:

### Option A: WebView Integration
- Open search results in embedded WebView
- Use Android's built-in browser capabilities
- No API keys required

### Option B: Intent-Based Search
- Launch system browser with search query
- Use Android's search intent system
- Integrate with user's default browser

Would you like me to implement any of these alternatives instead? 