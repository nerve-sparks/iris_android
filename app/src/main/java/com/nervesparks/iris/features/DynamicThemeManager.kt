package com.nervesparks.iris.features

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Samsung Galaxy S24 Ultra Dynamic Theme System
 * Automatically adapts theme based on conversation context
 */
class DynamicThemeManager {

    var currentTheme by mutableStateOf(ThemeType.DEFAULT)
        private set

    var isThemeAnimating by mutableStateOf(false)
        private set

    enum class ThemeType {
        DEFAULT,        // Standard Iris theme
        CREATIVE,       // For creative writing, art discussions
        TECHNICAL,      // For coding, technical discussions  
        PROFESSIONAL,   // For business, formal discussions
        LEARNING,       // For educational content
        ENTERTAINMENT,  // For fun, casual conversations
        WELLNESS        // For health, mindfulness topics
    }

    /**
     * Analyze conversation context and suggest theme
     */
    fun analyzeAndUpdateTheme(lastMessage: String, conversationHistory: List<String>) {
        val suggestedTheme = detectThemeFromContent(lastMessage, conversationHistory)
        if (suggestedTheme != currentTheme) {
            updateTheme(suggestedTheme)
        }
    }

    /**
     * Manually set theme
     */
    fun updateTheme(newTheme: ThemeType) {
        if (newTheme != currentTheme) {
            isThemeAnimating = true
            currentTheme = newTheme
            // Animation will be handled by UI
        }
    }

    /**
     * Complete theme animation
     */
    fun completeThemeAnimation() {
        isThemeAnimating = false
    }

    /**
     * Detect theme based on conversation content
     */
    private fun detectThemeFromContent(lastMessage: String, history: List<String>): ThemeType {
        val allContent = (history.takeLast(5) + lastMessage).joinToString(" ").lowercase()
        
        val keywords = mapOf(
            ThemeType.CREATIVE to listOf(
                "write", "story", "creative", "art", "design", "music", "poem", "lyrics", 
                "painting", "drawing", "imagination", "inspire", "artistic", "compose"
            ),
            ThemeType.TECHNICAL to listOf(
                "code", "programming", "function", "class", "variable", "algorithm", "debug",
                "software", "development", "api", "database", "framework", "javascript", "python",
                "java", "kotlin", "android", "app", "technical", "engineering"
            ),
            ThemeType.PROFESSIONAL to listOf(
                "business", "meeting", "email", "presentation", "proposal", "strategy", "analysis",
                "report", "professional", "corporate", "management", "leadership", "finance",
                "marketing", "sales", "client", "customer"
            ),
            ThemeType.LEARNING to listOf(
                "learn", "study", "explain", "tutorial", "lesson", "education", "teach", "understand",
                "knowledge", "concept", "theory", "practice", "example", "quiz", "test", "homework"
            ),
            ThemeType.ENTERTAINMENT to listOf(
                "fun", "game", "joke", "funny", "entertainment", "movie", "tv", "show", "celebrity",
                "sports", "hobby", "leisure", "weekend", "party", "friends", "casual"
            ),
            ThemeType.WELLNESS to listOf(
                "health", "wellness", "meditation", "mindfulness", "exercise", "fitness", "yoga",
                "mental", "stress", "relaxation", "therapy", "healing", "nutrition", "sleep"
            )
        )

        // Count keyword matches for each theme
        val scores = keywords.mapValues { (_, words) ->
            words.count { keyword -> allContent.contains(keyword) }
        }

        // Return theme with highest score, default if no clear winner
        return scores.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ThemeType.DEFAULT
    }

    /**
     * Get color scheme for current theme optimized for Samsung AMOLED
     */
    fun getCurrentColorScheme(isDark: Boolean): ColorScheme {
        return if (isDark) {
            getDarkColorScheme(currentTheme)
        } else {
            getLightColorScheme(currentTheme)
        }
    }

    private fun getDarkColorScheme(theme: ThemeType): ColorScheme {
        return when (theme) {
            ThemeType.CREATIVE -> darkColorScheme(
                primary = Color(0xFFFF6B9D),      // Creative pink
                secondary = Color(0xFFB794F6),     // Creative purple
                tertiary = Color(0xFFFFD93D),      // Creative yellow
                background = Color(0xFF000000),    // AMOLED black
                surface = Color(0xFF1A0D1A)        // Dark pink tint
            )
            
            ThemeType.TECHNICAL -> darkColorScheme(
                primary = Color(0xFF00D4AA),       // Tech cyan
                secondary = Color(0xFF0EA5E9),     // Tech blue
                tertiary = Color(0xFF84CC16),      // Tech green
                background = Color(0xFF000000),    // AMOLED black
                surface = Color(0xFF0A1A1A)        // Dark tech tint
            )
            
            ThemeType.PROFESSIONAL -> darkColorScheme(
                primary = Color(0xFF3B82F6),       // Professional blue
                secondary = Color(0xFF6366F1),     // Professional indigo
                tertiary = Color(0xFF8B5CF6),      // Professional purple
                background = Color(0xFF000000),    // AMOLED black
                surface = Color(0xFF0F0F1A)        // Dark professional tint
            )
            
            ThemeType.LEARNING -> darkColorScheme(
                primary = Color(0xFFF59E0B),       // Learning amber
                secondary = Color(0xFFEF4444),     // Learning red
                tertiary = Color(0xFF10B981),      // Learning green
                background = Color(0xFF000000),    // AMOLED black
                surface = Color(0xFF1A1A0A)        // Dark learning tint
            )
            
            ThemeType.ENTERTAINMENT -> darkColorScheme(
                primary = Color(0xFF8B5CF6),       // Entertainment purple
                secondary = Color(0xFFEC4899),     // Entertainment pink
                tertiary = Color(0xFFE11D48),      // Entertainment red
                background = Color(0xFF000000),    // AMOLED black
                surface = Color(0xFF1A0A1A)        // Dark entertainment tint
            )
            
            ThemeType.WELLNESS -> darkColorScheme(
                primary = Color(0xFF10B981),       // Wellness green
                secondary = Color(0xFF06B6D4),     // Wellness cyan
                tertiary = Color(0xFF84CC16),      // Wellness lime
                background = Color(0xFF000000),    // AMOLED black
                surface = Color(0xFF0A1A0F)        // Dark wellness tint
            )
            
            else -> darkColorScheme(
                primary = Color(0xFF6366F1),       // Default indigo
                secondary = Color(0xFF8B5CF6),     // Default purple
                tertiary = Color(0xFF06B6D4),      // Default cyan
                background = Color(0xFF000000),    // AMOLED black
                surface = Color(0xFF1A1A1A)        // Default dark surface
            )
        }
    }

    private fun getLightColorScheme(theme: ThemeType): ColorScheme {
        return when (theme) {
            ThemeType.CREATIVE -> lightColorScheme(
                primary = Color(0xFFD63384),
                secondary = Color(0xFF9333EA),
                tertiary = Color(0xFFF59E0B),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFF0F5)
            )
            
            ThemeType.TECHNICAL -> lightColorScheme(
                primary = Color(0xFF0891B2),
                secondary = Color(0xFF0284C7),
                tertiary = Color(0xFF16A34A),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFF0FDFF)
            )
            
            ThemeType.PROFESSIONAL -> lightColorScheme(
                primary = Color(0xFF2563EB),
                secondary = Color(0xFF4F46E5),
                tertiary = Color(0xFF7C3AED),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFF8FAFC)
            )
            
            ThemeType.LEARNING -> lightColorScheme(
                primary = Color(0xFFD97706),
                secondary = Color(0xFFDC2626),
                tertiary = Color(0xFF059669),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFFBF0)
            )
            
            ThemeType.ENTERTAINMENT -> lightColorScheme(
                primary = Color(0xFF7C3AED),
                secondary = Color(0xFFDB2777),
                tertiary = Color(0xFFBE185D),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFDF2F8)
            )
            
            ThemeType.WELLNESS -> lightColorScheme(
                primary = Color(0xFF059669),
                secondary = Color(0xFF0891B2),
                tertiary = Color(0xFF65A30D),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFF0FDF4)
            )
            
            else -> lightColorScheme(
                primary = Color(0xFF4F46E5),
                secondary = Color(0xFF7C3AED),
                tertiary = Color(0xFF0891B2),
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFF7F2FA)
            )
        }
    }

    /**
     * Get theme description for UI display
     */
    fun getThemeDescription(theme: ThemeType): String {
        return when (theme) {
            ThemeType.DEFAULT -> "Default Theme"
            ThemeType.CREATIVE -> "Creative Mode - For artistic and creative conversations"
            ThemeType.TECHNICAL -> "Technical Mode - For coding and technical discussions"
            ThemeType.PROFESSIONAL -> "Professional Mode - For business and formal topics"
            ThemeType.LEARNING -> "Learning Mode - For educational content and studying"
            ThemeType.ENTERTAINMENT -> "Entertainment Mode - For fun and casual chats"
            ThemeType.WELLNESS -> "Wellness Mode - For health and mindfulness topics"
        }
    }
}