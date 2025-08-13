package com.nervesparks.iris.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.nervesparks.iris.Template
import com.nervesparks.iris.data.repository.ModelConfiguration
import org.json.JSONArray
import org.json.JSONObject

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val KEY_DEFAULT_MODEL_NAME = "default_model_name"
private const val KEY_HUGGINGFACE_TOKEN = "huggingface_token"
private const val KEY_HUGGINGFACE_USERNAME = "huggingface_username"
// Web search (Google Programmable Search) keys
private const val KEY_GOOGLE_API_KEY = "google_api_key"
private const val KEY_GOOGLE_CSE_ID = "google_cse_id"

// Model configuration keys
private const val KEY_MODEL_TEMPERATURE = "model_temperature"
private const val KEY_MODEL_TOP_P = "model_top_p"
private const val KEY_MODEL_TOP_K = "model_top_k"
private const val KEY_MODEL_MAX_TOKENS = "model_max_tokens"
private const val KEY_MODEL_CONTEXT_LENGTH = "model_context_length"
private const val KEY_MODEL_SYSTEM_PROMPT = "model_system_prompt"
private const val KEY_MODEL_CHAT_FORMAT = "model_chat_format"
private const val KEY_MODEL_THREAD_COUNT = "model_thread_count"
private const val KEY_MODEL_GPU_LAYERS = "model_gpu_layers"
private const val KEY_CACHED_MODELS = "cached_models"
private const val KEY_MODEL_CONFIG_PREFIX = "model_config_"

// Thinking token settings keys
private const val KEY_SHOW_THINKING_TOKENS = "show_thinking_tokens"
private const val KEY_THINKING_TOKEN_STYLE = "thinking_token_style"

// Template storage key
private const val KEY_TEMPLATES = "user_templates"

// UI settings keys
private const val KEY_UI_THEME = "ui_theme"
private const val KEY_UI_FONT_SIZE = "ui_font_size"
private const val KEY_UI_ENABLE_ANIMATIONS = "ui_enable_animations"
private const val KEY_UI_ENABLE_HAPTIC_FEEDBACK = "ui_enable_haptic_feedback"

// Performance settings keys
private const val KEY_PERF_ENABLE_MEMORY_OPTIMIZATION = "perf_enable_memory_optimization"
private const val KEY_PERF_ENABLE_BACKGROUND_PROCESSING = "perf_enable_background_processing"

// Security settings keys
private const val KEY_SECURITY_BIOMETRIC_ENABLED = "security_biometric_enabled"

open class UserPreferencesRepository protected constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context).also { INSTANCE = it }
            }
        }
    }

    private val appContext: Context = context.applicationContext

    protected open fun createSharedPreferences(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            USER_PREFERENCES_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    protected open val sharedPreferences: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createSharedPreferences(appContext)
    }

    // Get the default model name, returns empty string if not set
    open fun getDefaultModelName(): String {
        return sharedPreferences.getString(KEY_DEFAULT_MODEL_NAME, "") ?: ""
    }

    // Set the default model name
    open fun setDefaultModelName(modelName: String) {
        sharedPreferences.edit().putString(KEY_DEFAULT_MODEL_NAME, modelName).apply()
    }

    // Get HuggingFace token
    open fun getHuggingFaceToken(): String {
        return sharedPreferences.getString(KEY_HUGGINGFACE_TOKEN, "") ?: ""
    }

    // Set HuggingFace token
    open fun setHuggingFaceToken(token: String) {
        sharedPreferences.edit().putString(KEY_HUGGINGFACE_TOKEN, token).apply()
    }

    // Get HuggingFace username
    open fun getHuggingFaceUsername(): String {
        return sharedPreferences.getString(KEY_HUGGINGFACE_USERNAME, "") ?: ""
    }

    // Set HuggingFace username
    open fun setHuggingFaceUsername(username: String) {
        sharedPreferences.edit().putString(KEY_HUGGINGFACE_USERNAME, username).apply()
    }

    // Check if HuggingFace credentials are set
    open fun hasHuggingFaceCredentials(): Boolean {
        return getHuggingFaceToken().isNotEmpty() || getHuggingFaceUsername().isNotEmpty()
    }

    // Temporary method for testing - now disabled to prevent committing secrets
    open fun setTestHuggingFaceToken() {
        // NO-OP. Obtain token from UI or secure storage.
    }

    // Model configuration methods
    open fun setModelTemperature(temperature: Float) {
        sharedPreferences.edit().putFloat(KEY_MODEL_TEMPERATURE, temperature).apply()
    }

    // Google Programmable Search settings (stored encrypted)
    open fun setGoogleApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_GOOGLE_API_KEY, key).apply()
    }

    open fun getGoogleApiKey(): String {
        return sharedPreferences.getString(KEY_GOOGLE_API_KEY, "") ?: ""
    }

    open fun setGoogleCseId(id: String) {
        sharedPreferences.edit().putString(KEY_GOOGLE_CSE_ID, id).apply()
    }

    open fun getGoogleCseId(): String {
        return sharedPreferences.getString(KEY_GOOGLE_CSE_ID, "") ?: ""
    }

    open fun getModelTemperature(): Float {
        return sharedPreferences.getFloat(KEY_MODEL_TEMPERATURE, 0.7f)
    }

    open fun setModelTopP(topP: Float) {
        sharedPreferences.edit().putFloat(KEY_MODEL_TOP_P, topP).apply()
    }

    open fun getModelTopP(): Float {
        return sharedPreferences.getFloat(KEY_MODEL_TOP_P, 0.9f)
    }

    open fun setModelTopK(topK: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_TOP_K, topK).apply()
    }

    open fun getModelTopK(): Int {
        return sharedPreferences.getInt(KEY_MODEL_TOP_K, 40)
    }

    open fun setModelMaxTokens(maxTokens: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_MAX_TOKENS, maxTokens).apply()
    }

    open fun getModelMaxTokens(): Int {
        return sharedPreferences.getInt(KEY_MODEL_MAX_TOKENS, 2048)
    }

    open fun setModelContextLength(contextLength: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_CONTEXT_LENGTH, contextLength).apply()
    }

    open fun getModelContextLength(): Int {
        return sharedPreferences.getInt(KEY_MODEL_CONTEXT_LENGTH, 4096)
    }

    open fun setModelSystemPrompt(systemPrompt: String) {
        sharedPreferences.edit().putString(KEY_MODEL_SYSTEM_PROMPT, systemPrompt).apply()
    }

    open fun getModelSystemPrompt(): String {
        return sharedPreferences.getString(KEY_MODEL_SYSTEM_PROMPT, "You are a helpful AI assistant.") ?: "You are a helpful AI assistant."
    }

    open fun setModelChatFormat(chatFormat: String) {
        sharedPreferences.edit().putString(KEY_MODEL_CHAT_FORMAT, chatFormat).apply()
    }

    open fun getModelChatFormat(): String {
        return sharedPreferences.getString(KEY_MODEL_CHAT_FORMAT, "CHATML") ?: "CHATML"
    }

    open fun setModelThreadCount(threadCount: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_THREAD_COUNT, threadCount).apply()
    }

    open fun getModelThreadCount(): Int {
        return sharedPreferences.getInt(KEY_MODEL_THREAD_COUNT, 4)
    }

    open fun setModelGpuLayers(layers: Int) {
        sharedPreferences.edit().putInt(KEY_MODEL_GPU_LAYERS, layers).apply()
    }

    open fun getModelGpuLayers(): Int {
        return sharedPreferences.getInt(KEY_MODEL_GPU_LAYERS, -1)
    }

    // Per-model configuration
    open fun getModelConfiguration(modelName: String): ModelConfiguration {
        val prefix = "${KEY_MODEL_CONFIG_PREFIX}${modelName}_"
        return ModelConfiguration(
            temperature = sharedPreferences.getFloat(prefix + "temperature", 0.7f),
            topP = sharedPreferences.getFloat(prefix + "top_p", 0.9f),
            topK = sharedPreferences.getInt(prefix + "top_k", 40),
            threadCount = sharedPreferences.getInt(prefix + "thread_count", 2),
            contextLength = sharedPreferences.getInt(prefix + "context_length", 4096),
            systemPrompt = sharedPreferences.getString(prefix + "system_prompt", "") ?: ""
        )
    }

    open fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        val prefix = "${KEY_MODEL_CONFIG_PREFIX}${modelName}_"
        sharedPreferences.edit().apply {
            putFloat(prefix + "temperature", config.temperature)
            putFloat(prefix + "top_p", config.topP)
            putInt(prefix + "top_k", config.topK)
            putInt(prefix + "thread_count", config.threadCount)
            putInt(prefix + "context_length", config.contextLength)
            putString(prefix + "system_prompt", config.systemPrompt)
            apply()
        }
    }

    open fun getCachedModels(): String {
        return sharedPreferences.getString(KEY_CACHED_MODELS, "") ?: ""
    }

    open fun setCachedModels(json: String) {
        sharedPreferences.edit().putString(KEY_CACHED_MODELS, json).apply()
    }

    // Thinking token settings
    open fun setShowThinkingTokens(show: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_THINKING_TOKENS, show).apply()
    }

    open fun getShowThinkingTokens(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_THINKING_TOKENS, true)
    }

    open fun setThinkingTokenStyle(style: String) {
        sharedPreferences.edit().putString(KEY_THINKING_TOKEN_STYLE, style).apply()
    }

    open fun getThinkingTokenStyle(): String {
        return sharedPreferences.getString(KEY_THINKING_TOKEN_STYLE, "COLLAPSIBLE") ?: "COLLAPSIBLE"
    }

    // UI settings
    open fun setUITheme(theme: String) {
        sharedPreferences.edit().putString(KEY_UI_THEME, theme).apply()
    }

    open fun getUITheme(): String {
        return sharedPreferences.getString(KEY_UI_THEME, "DARK") ?: "DARK"
    }

    open fun setUIFontSize(fontSize: Float) {
        sharedPreferences.edit().putFloat(KEY_UI_FONT_SIZE, fontSize).apply()
    }

    open fun getUIFontSize(): Float {
        return sharedPreferences.getFloat(KEY_UI_FONT_SIZE, 1.0f)
    }

    open fun setUIEnableAnimations(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_UI_ENABLE_ANIMATIONS, enable).apply()
    }

    open fun getUIEnableAnimations(): Boolean {
        return sharedPreferences.getBoolean(KEY_UI_ENABLE_ANIMATIONS, true)
    }

    open fun setUIEnableHapticFeedback(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_UI_ENABLE_HAPTIC_FEEDBACK, enable).apply()
    }

    open fun getUIEnableHapticFeedback(): Boolean {
        return sharedPreferences.getBoolean(KEY_UI_ENABLE_HAPTIC_FEEDBACK, true)
    }

    // Performance settings
    open fun setPerfEnableMemoryOptimization(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, enable).apply()
    }

    open fun getPerfEnableMemoryOptimization(): Boolean {
        return sharedPreferences.getBoolean(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, true)
    }

    open fun setPerfEnableBackgroundProcessing(enable: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, enable).apply()
    }

    open fun getPerfEnableBackgroundProcessing(): Boolean {
        return sharedPreferences.getBoolean(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, true)
    }

    // Security settings
    open fun setSecurityBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SECURITY_BIOMETRIC_ENABLED, enabled).apply()
    }

    open fun getSecurityBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SECURITY_BIOMETRIC_ENABLED, false)
    }

    // Template management
    open fun getTemplates(): List<Template> {
        val json = sharedPreferences.getString(KEY_TEMPLATES, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                Template(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    content = obj.getString("content")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    open fun saveTemplates(templates: List<Template>) {
        val array = JSONArray()
        templates.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("content", t.content)
            array.put(obj)
        }
        sharedPreferences.edit().putString(KEY_TEMPLATES, array.toString()).apply()
    }

    // Clear all stored preferences
    open fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    // Export all configuration to JSON
    open fun exportConfiguration(): String {
        val jsonObject = org.json.JSONObject()
        
        // Global settings
        jsonObject.put("defaultModelName", getDefaultModelName())
        jsonObject.put("huggingFaceToken", getHuggingFaceToken())
        jsonObject.put("huggingFaceUsername", getHuggingFaceUsername())
        jsonObject.put("modelTemperature", getModelTemperature())
        jsonObject.put("modelTopP", getModelTopP())
        jsonObject.put("modelTopK", getModelTopK())
        jsonObject.put("modelMaxTokens", getModelMaxTokens())
        jsonObject.put("modelContextLength", getModelContextLength())
        jsonObject.put("modelSystemPrompt", getModelSystemPrompt())
        jsonObject.put("modelChatFormat", getModelChatFormat())
        jsonObject.put("modelThreadCount", getModelThreadCount())
        jsonObject.put("showThinkingTokens", getShowThinkingTokens())
        jsonObject.put("thinkingTokenStyle", getThinkingTokenStyle())
        jsonObject.put("uiTheme", getUITheme())
        jsonObject.put("uiFontSize", getUIFontSize())
        jsonObject.put("uiEnableAnimations", getUIEnableAnimations())
        jsonObject.put("uiEnableHapticFeedback", getUIEnableHapticFeedback())
        jsonObject.put("perfEnableMemoryOptimization", getPerfEnableMemoryOptimization())
        jsonObject.put("perfEnableBackgroundProcessing", getPerfEnableBackgroundProcessing())
        
        return jsonObject.toString()
    }
    
    // Import configuration from JSON
    open fun importConfiguration(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)
            val editor = sharedPreferences.edit()
            
            // Import global settings
            if (json.has("defaultModelName")) editor.putString(KEY_DEFAULT_MODEL_NAME, json.getString("defaultModelName"))
            if (json.has("huggingFaceToken")) editor.putString(KEY_HUGGINGFACE_TOKEN, json.getString("huggingFaceToken"))
            if (json.has("huggingFaceUsername")) editor.putString(KEY_HUGGINGFACE_USERNAME, json.getString("huggingFaceUsername"))
            if (json.has("modelTemperature")) editor.putFloat(KEY_MODEL_TEMPERATURE, json.getDouble("modelTemperature").toFloat())
            if (json.has("modelTopP")) editor.putFloat(KEY_MODEL_TOP_P, json.getDouble("modelTopP").toFloat())
            if (json.has("modelTopK")) editor.putInt(KEY_MODEL_TOP_K, json.getInt("modelTopK"))
            if (json.has("modelMaxTokens")) editor.putInt(KEY_MODEL_MAX_TOKENS, json.getInt("modelMaxTokens"))
            if (json.has("modelContextLength")) editor.putInt(KEY_MODEL_CONTEXT_LENGTH, json.getInt("modelContextLength"))
            if (json.has("modelSystemPrompt")) editor.putString(KEY_MODEL_SYSTEM_PROMPT, json.getString("modelSystemPrompt"))
            if (json.has("modelChatFormat")) editor.putString(KEY_MODEL_CHAT_FORMAT, json.getString("modelChatFormat"))
            if (json.has("modelThreadCount")) editor.putInt(KEY_MODEL_THREAD_COUNT, json.getInt("modelThreadCount"))
            if (json.has("showThinkingTokens")) editor.putBoolean(KEY_SHOW_THINKING_TOKENS, json.getBoolean("showThinkingTokens"))
            if (json.has("thinkingTokenStyle")) editor.putString(KEY_THINKING_TOKEN_STYLE, json.getString("thinkingTokenStyle"))
            if (json.has("uiTheme")) editor.putString(KEY_UI_THEME, json.getString("uiTheme"))
            if (json.has("uiFontSize")) editor.putFloat(KEY_UI_FONT_SIZE, json.getDouble("uiFontSize").toFloat())
            if (json.has("uiEnableAnimations")) editor.putBoolean(KEY_UI_ENABLE_ANIMATIONS, json.getBoolean("uiEnableAnimations"))
            if (json.has("uiEnableHapticFeedback")) editor.putBoolean(KEY_UI_ENABLE_HAPTIC_FEEDBACK, json.getBoolean("uiEnableHapticFeedback"))
            if (json.has("perfEnableMemoryOptimization")) editor.putBoolean(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, json.getBoolean("perfEnableMemoryOptimization"))
            if (json.has("perfEnableBackgroundProcessing")) editor.putBoolean(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, json.getBoolean("perfEnableBackgroundProcessing"))
            
            editor.apply()
            true
        } catch (e: Exception) {
            android.util.Log.e("UserPreferencesRepository", "Error importing configuration", e)
            false
        }
    }

} 
