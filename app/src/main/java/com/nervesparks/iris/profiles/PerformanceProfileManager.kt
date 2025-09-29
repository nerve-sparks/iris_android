package com.nervesparks.iris.profiles

import android.content.Context
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Performance profile system similar to ChatGPT/Copilot
 * Speed / Balanced / Quality presets
 */

private val Context.dataStore by preferencesDataStore(name = "performance_profiles")

enum class PerformanceProfile(
    val displayName: String,
    val temperature: Float,
    val topP: Float,
    val threads: Int,
    val contextLength: Int,
    val repeatPenalty: Float,
    val description: String
) {
    SPEED(
        displayName = "Speed",
        temperature = 0.3f,
        topP = 0.8f,
        threads = 8,  // High threads for S24 Ultra
        contextLength = 2048,
        repeatPenalty = 1.05f,
        description = "Fast responses, optimized for quick interactions"
    ),
    
    BALANCED(
        displayName = "Balanced", 
        temperature = 0.7f,
        topP = 0.9f,
        threads = 6,
        contextLength = 4096,
        repeatPenalty = 1.1f,
        description = "Best balance of speed and quality"
    ),
    
    QUALITY(
        displayName = "Quality",
        temperature = 0.9f,
        topP = 0.95f,
        threads = 4,  // Lower threads for better quality
        contextLength = 8192,
        repeatPenalty = 1.15f,
        description = "Highest quality responses, slower generation"
    );
    
    companion object {
        fun fromString(value: String): PerformanceProfile {
            return values().find { it.name == value } ?: BALANCED
        }
    }
}

@Composable
fun rememberPerformanceProfile(context: Context): MutableState<PerformanceProfile> {
    val profileState = rememberSaveable { mutableStateOf(PerformanceProfile.BALANCED) }
    
    // Load saved profile
    LaunchedEffect(Unit) {
        context.dataStore.data.map { preferences ->
            val savedProfile = preferences[stringPreferencesKey("performance_profile")]
            PerformanceProfile.fromString(savedProfile ?: "BALANCED")
        }.collect { profile ->
            profileState.value = profile
        }
    }
    
    return profileState
}

class PerformanceProfileManager(private val context: Context) {
    
    private val profileKey = stringPreferencesKey("performance_profile")
    private val customTempKey = floatPreferencesKey("custom_temperature")
    private val customTopPKey = floatPreferencesKey("custom_top_p")
    private val customThreadsKey = intPreferencesKey("custom_threads") 
    private val customContextKey = intPreferencesKey("custom_context")
    
    val currentProfile: Flow<PerformanceProfile> = context.dataStore.data
        .map { preferences ->
            val profileName = preferences[profileKey] ?: "BALANCED"
            PerformanceProfile.fromString(profileName)
        }
    
    suspend fun setProfile(profile: PerformanceProfile) {
        context.dataStore.edit { preferences ->
            preferences[profileKey] = profile.name
        }
    }
    
    suspend fun setCustomParameters(
        temperature: Float,
        topP: Float, 
        threads: Int,
        contextLength: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[customTempKey] = temperature
            preferences[customTopPKey] = topP
            preferences[customThreadsKey] = threads
            preferences[customContextKey] = contextLength
        }
    }
    
    val customParameters: Flow<CustomParameters> = context.dataStore.data
        .map { preferences ->
            CustomParameters(
                temperature = preferences[customTempKey] ?: 0.7f,
                topP = preferences[customTopPKey] ?: 0.9f,
                threads = preferences[customThreadsKey] ?: 6,
                contextLength = preferences[customContextKey] ?: 4096
            )
        }
    
    data class CustomParameters(
        val temperature: Float,
        val topP: Float,
        val threads: Int,
        val contextLength: Int
    )
}