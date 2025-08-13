package com.nervesparks.iris.data

import android.content.Context
import com.nervesparks.iris.Template
import com.nervesparks.iris.data.repository.ModelConfiguration

class FakeUserPreferencesRepository(context: Context) : UserPreferencesRepository(context) {

    private val prefs = mutableMapOf<String, Any>()

    override fun getDefaultModelName(): String {
        return prefs[KEY_DEFAULT_MODEL_NAME] as? String ?: ""
    }

    override fun setDefaultModelName(modelName: String) {
        prefs[KEY_DEFAULT_MODEL_NAME] = modelName
    }

    override fun getHuggingFaceToken(): String {
        return prefs[KEY_HUGGINGFACE_TOKEN] as? String ?: ""
    }

    override fun setHuggingFaceToken(token: String) {
        prefs[KEY_HUGGINGFACE_TOKEN] = token
    }

    override fun getHuggingFaceUsername(): String {
        return prefs[KEY_HUGGINGFACE_USERNAME] as? String ?: ""
    }

    override fun setHuggingFaceUsername(username: String) {
        prefs[KEY_HUGGINGFACE_USERNAME] = username
    }

    override fun hasHuggingFaceCredentials(): Boolean {
        return getHuggingFaceToken().isNotEmpty() || getHuggingFaceUsername().isNotEmpty()
    }

    override fun setModelTemperature(temperature: Float) {
        prefs[KEY_MODEL_TEMPERATURE] = temperature
    }

    override fun getModelTemperature(): Float {
        return prefs[KEY_MODEL_TEMPERATURE] as? Float ?: 0.7f
    }

    override fun setModelTopP(topP: Float) {
        prefs[KEY_MODEL_TOP_P] = topP
    }

    override fun getModelTopP(): Float {
        return prefs[KEY_MODEL_TOP_P] as? Float ?: 0.9f
    }

    override fun setModelTopK(topK: Int) {
        prefs[KEY_MODEL_TOP_K] = topK
    }

    override fun getModelTopK(): Int {
        return prefs[KEY_MODEL_TOP_K] as? Int ?: 40
    }

    override fun setModelMaxTokens(maxTokens: Int) {
        prefs[KEY_MODEL_MAX_TOKENS] = maxTokens
    }

    override fun getModelMaxTokens(): Int {
        return prefs[KEY_MODEL_MAX_TOKENS] as? Int ?: 2048
    }

    override fun setModelContextLength(contextLength: Int) {
        prefs[KEY_MODEL_CONTEXT_LENGTH] = contextLength
    }

    override fun getModelContextLength(): Int {
        return prefs[KEY_MODEL_CONTEXT_LENGTH] as? Int ?: 4096
    }

    override fun setModelSystemPrompt(systemPrompt: String) {
        prefs[KEY_MODEL_SYSTEM_PROMPT] = systemPrompt
    }

    override fun getModelSystemPrompt(): String {
        return prefs[KEY_MODEL_SYSTEM_PROMPT] as? String ?: "You are a helpful AI assistant."
    }

    override fun setModelChatFormat(chatFormat: String) {
        prefs[KEY_MODEL_CHAT_FORMAT] = chatFormat
    }

    override fun getModelChatFormat(): String {
        return prefs[KEY_MODEL_CHAT_FORMAT] as? String ?: "CHATML"
    }

    override fun setModelThreadCount(threadCount: Int) {
        prefs[KEY_MODEL_THREAD_COUNT] = threadCount
    }

    override fun getModelThreadCount(): Int {
        return prefs[KEY_MODEL_THREAD_COUNT] as? Int ?: 4
    }

    override fun getModelConfiguration(modelName: String): ModelConfiguration {
        val prefix = "model_config_${modelName}_"
        return ModelConfiguration(
            temperature = prefs[prefix + "temperature"] as? Float ?: 0.7f,
            topP = prefs[prefix + "top_p"] as? Float ?: 0.9f,
            topK = prefs[prefix + "top_k"] as? Int ?: 40,
            threadCount = prefs[prefix + "thread_count"] as? Int ?: 2,
            contextLength = prefs[prefix + "context_length"] as? Int ?: 4096,
            systemPrompt = prefs[prefix + "system_prompt"] as? String ?: ""
        )
    }

    override fun saveModelConfiguration(modelName: String, config: ModelConfiguration) {
        val prefix = "model_config_${modelName}_"
        prefs[prefix + "temperature"] = config.temperature
        prefs[prefix + "top_p"] = config.topP
        prefs[prefix + "top_k"] = config.topK
        prefs[prefix + "thread_count"] = config.threadCount
        prefs[prefix + "context_length"] = config.contextLength
        prefs[prefix + "system_prompt"] = config.systemPrompt
    }

    override fun getCachedModels(): String {
        return prefs[KEY_CACHED_MODELS] as? String ?: ""
    }

    override fun setCachedModels(json: String) {
        prefs[KEY_CACHED_MODELS] = json
    }

    override fun setShowThinkingTokens(show: Boolean) {
        prefs[KEY_SHOW_THINKING_TOKENS] = show
    }

    override fun getShowThinkingTokens(): Boolean {
        return prefs[KEY_SHOW_THINKING_TOKENS] as? Boolean ?: true
    }

    override fun setThinkingTokenStyle(style: String) {
        prefs[KEY_THINKING_TOKEN_STYLE] = style
    }

    override fun getThinkingTokenStyle(): String {
        return prefs[KEY_THINKING_TOKEN_STYLE] as? String ?: "COLLAPSIBLE"
    }

    override fun setUITheme(theme: String) {
        prefs[KEY_UI_THEME] = theme
    }

    override fun getUITheme(): String {
        return prefs[KEY_UI_THEME] as? String ?: "DARK"
    }

    override fun setUIFontSize(fontSize: Float) {
        prefs[KEY_UI_FONT_SIZE] = fontSize
    }

    override fun getUIFontSize(): Float {
        return prefs[KEY_UI_FONT_SIZE] as? Float ?: 1.0f
    }

    override fun setUIEnableAnimations(enable: Boolean) {
        prefs[KEY_UI_ENABLE_ANIMATIONS] = enable
    }

    override fun getUIEnableAnimations(): Boolean {
        return prefs[KEY_UI_ENABLE_ANIMATIONS] as? Boolean ?: true
    }

    override fun setUIEnableHapticFeedback(enable: Boolean) {
        prefs[KEY_UI_ENABLE_HAPTIC_FEEDBACK] = enable
    }

    override fun getUIEnableHapticFeedback(): Boolean {
        return prefs[KEY_UI_ENABLE_HAPTIC_FEEDBACK] as? Boolean ?: true
    }

    override fun setPerfEnableMemoryOptimization(enable: Boolean) {
        prefs[KEY_PERF_ENABLE_MEMORY_OPTIMIZATION] = enable
    }

    override fun getPerfEnableMemoryOptimization(): Boolean {
        return prefs[KEY_PERF_ENABLE_MEMORY_OPTIMIZATION] as? Boolean ?: true
    }

    override fun setPerfEnableBackgroundProcessing(enable: Boolean) {
        prefs[KEY_PERF_ENABLE_BACKGROUND_PROCESSING] = enable
    }

    override fun getPerfEnableBackgroundProcessing(): Boolean {
        return prefs[KEY_PERF_ENABLE_BACKGROUND_PROCESSING] as? Boolean ?: true
    }

    override fun setSecurityBiometricEnabled(enabled: Boolean) {
        prefs[KEY_SECURITY_BIOMETRIC_ENABLED] = enabled
    }

    override fun getSecurityBiometricEnabled(): Boolean {
        return prefs[KEY_SECURITY_BIOMETRIC_ENABLED] as? Boolean ?: false
    }

    override fun getTemplates(): List<Template> {
        @Suppress("UNCHECKED_CAST")
        return prefs[KEY_TEMPLATES] as? List<Template> ?: emptyList()
    }

    override fun saveTemplates(templates: List<Template>) {
        prefs[KEY_TEMPLATES] = templates
    }

    override fun clearAll() {
        prefs.clear()
    }

    override fun exportConfiguration(): String {
        val json = org.json.JSONObject()
        json.put(KEY_DEFAULT_MODEL_NAME, getDefaultModelName())
        json.put(KEY_SHOW_THINKING_TOKENS, getShowThinkingTokens())
        json.put(KEY_THINKING_TOKEN_STYLE, getThinkingTokenStyle())
        json.put(KEY_MODEL_THREAD_COUNT, getModelThreadCount())
        json.put(KEY_MODEL_CONTEXT_LENGTH, getModelContextLength())
        json.put(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION, getPerfEnableMemoryOptimization())
        json.put(KEY_PERF_ENABLE_BACKGROUND_PROCESSING, getPerfEnableBackgroundProcessing())
        json.put(KEY_UI_THEME, getUITheme())
        json.put(KEY_UI_FONT_SIZE, getUIFontSize())
        json.put(KEY_UI_ENABLE_ANIMATIONS, getUIEnableAnimations())
        json.put(KEY_UI_ENABLE_HAPTIC_FEEDBACK, getUIEnableHapticFeedback())
        return json.toString()
    }

    override fun importConfiguration(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)
            if (json.has(KEY_DEFAULT_MODEL_NAME)) setDefaultModelName(json.getString(KEY_DEFAULT_MODEL_NAME))
            if (json.has(KEY_SHOW_THINKING_TOKENS)) setShowThinkingTokens(json.getBoolean(KEY_SHOW_THINKING_TOKENS))
            if (json.has(KEY_THINKING_TOKEN_STYLE)) setThinkingTokenStyle(json.getString(KEY_THINKING_TOKEN_STYLE))
            if (json.has(KEY_MODEL_THREAD_COUNT)) setModelThreadCount(json.getInt(KEY_MODEL_THREAD_COUNT))
            if (json.has(KEY_MODEL_CONTEXT_LENGTH)) setModelContextLength(json.getInt(KEY_MODEL_CONTEXT_LENGTH))
            if (json.has(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION)) setPerfEnableMemoryOptimization(json.getBoolean(KEY_PERF_ENABLE_MEMORY_OPTIMIZATION))
            if (json.has(KEY_PERF_ENABLE_BACKGROUND_PROCESSING)) setPerfEnableBackgroundProcessing(json.getBoolean(KEY_PERF_ENABLE_BACKGROUND_PROCESSING))
            if (json.has(KEY_UI_THEME)) setUITheme(json.getString(KEY_UI_THEME))
            if (json.has(KEY_UI_FONT_SIZE)) setUIFontSize(json.getDouble(KEY_UI_FONT_SIZE).toFloat())
            if (json.has(KEY_UI_ENABLE_ANIMATIONS)) setUIEnableAnimations(json.getBoolean(KEY_UI_ENABLE_ANIMATIONS))
            if (json.has(KEY_UI_ENABLE_HAPTIC_FEEDBACK)) setUIEnableHapticFeedback(json.getBoolean(KEY_UI_ENABLE_HAPTIC_FEEDBACK))
            true
        } catch (_: Exception) {
            false
        }
    }
}

private const val KEY_DEFAULT_MODEL_NAME = "default_model_name"
private const val KEY_HUGGINGFACE_TOKEN = "huggingface_token"
private const val KEY_HUGGINGFACE_USERNAME = "huggingface_username"
private const val KEY_MODEL_TEMPERATURE = "model_temperature"
private const val KEY_MODEL_TOP_P = "model_top_p"
private const val KEY_MODEL_TOP_K = "model_top_k"
private const val KEY_MODEL_MAX_TOKENS = "model_max_tokens"
private const val KEY_MODEL_CONTEXT_LENGTH = "model_context_length"
private const val KEY_MODEL_SYSTEM_PROMPT = "model_system_prompt"
private const val KEY_MODEL_CHAT_FORMAT = "model_chat_format"
private const val KEY_MODEL_THREAD_COUNT = "model_thread_count"
private const val KEY_CACHED_MODELS = "cached_models"
private const val KEY_MODEL_CONFIG_PREFIX = "model_config_"
private const val KEY_SHOW_THINKING_TOKENS = "show_thinking_tokens"
private const val KEY_THINKING_TOKEN_STYLE = "thinking_token_style"
private const val KEY_UI_THEME = "ui_theme"
private const val KEY_UI_FONT_SIZE = "ui_font_size"
private const val KEY_UI_ENABLE_ANIMATIONS = "ui_enable_animations"
private const val KEY_UI_ENABLE_HAPTIC_FEEDBACK = "ui_enable_haptic_feedback"
private const val KEY_PERF_ENABLE_MEMORY_OPTIMIZATION = "perf_enable_memory_optimization"
private const val KEY_PERF_ENABLE_BACKGROUND_PROCESSING = "perf_enable_background_processing"
private const val KEY_SECURITY_BIOMETRIC_ENABLED = "security_biometric_enabled"
private const val KEY_TEMPLATES = "user_templates"
