package com.nervesparks.axia.personality

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.*

/**
 * Role Playing System for Monique Personality
 * 
 * Features:
 * - Custom relationship scenarios (girlfriend, partner, etc.)
 * - Kink preferences and fetish compatibility
 * - Realistic conversation patterns
 * - Memory of past interactions
 * - Photo/video exchange scenarios
 * - Personalized sexual preferences
 */
class RolePlayingSystem(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Role playing state
    private val _currentRole = MutableStateFlow<RolePlayScenario?>(null)
    val currentRole: StateFlow<RolePlayScenario?> = _currentRole.asStateFlow()
    
    private val _relationshipProfile = MutableStateFlow<RelationshipProfile?>(null)
    val relationshipProfile: StateFlow<RelationshipProfile?> = _relationshipProfile.asStateFlow()
    
    // Preferences storage
    private val preferences: SharedPreferences = context.getSharedPreferences("axia_roleplay", Context.MODE_PRIVATE)
    
    // JSON serializer for complex data
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true 
    }
    
    init {
        loadSavedProfile()
    }
    
    /**
     * Set up a role playing scenario
     */
    suspend fun setupRolePlay(scenario: RolePlayScenario) {
        withContext(Dispatchers.IO) {
            try {
                _currentRole.value = scenario
                saveScenario(scenario)
                
                Log.d("RolePlayingSystem", "Role play scenario set: ${scenario.type}")
                
            } catch (e: Exception) {
                Log.e("RolePlayingSystem", "Failed to setup role play", e)
            }
        }
    }
    
    /**
     * Update relationship profile with kinks and preferences
     */
    suspend fun updateRelationshipProfile(profile: RelationshipProfile) {
        withContext(Dispatchers.IO) {
            try {
                _relationshipProfile.value = profile
                saveProfile(profile)
                
                Log.d("RolePlayingSystem", "Relationship profile updated")
                
            } catch (e: Exception) {
                Log.e("RolePlayingSystem", "Failed to update profile", e)
            }
        }
    }
    
    /**
     * Process chat message with role playing context
     */
    suspend fun processRolePlayMessage(
        message: String,
        messageType: MessageType = MessageType.TEXT
    ): RolePlayResponse {
        return withContext(Dispatchers.Default) {
            try {
                val scenario = _currentRole.value
                val profile = _relationshipProfile.value
                
                if (scenario == null || profile == null) {
                    return@withContext RolePlayResponse.Error("No active role play scenario")
                }
                
                // Analyze message intent
                val intent = analyzeMessageIntent(message, scenario, profile)
                
                // Generate contextual response
                val response = generateContextualResponse(message, intent, scenario, profile)
                
                // Update conversation memory
                updateConversationMemory(message, response, scenario)
                
                RolePlayResponse.Success(
                    response = response,
                    scenario = scenario,
                    suggestedActions = generateSuggestedActions(intent, scenario, profile)
                )
                
            } catch (e: Exception) {
                Log.e("RolePlayingSystem", "Failed to process role play message", e)
                RolePlayResponse.Error("Role play processing failed: ${e.message}")
            }
        }
    }
    
    /**
     * Handle photo/video exchange scenarios
     */
    suspend fun handleMediaExchange(
        mediaType: MediaExchangeType,
        userMessage: String
    ): MediaExchangeResponse {
        return withContext(Dispatchers.Default) {
            try {
                val scenario = _currentRole.value
                val profile = _relationshipProfile.value
                
                if (scenario == null || profile == null) {
                    return@withContext MediaExchangeResponse.Error("No active relationship profile")
                }
                
                when (mediaType) {
                    MediaExchangeType.PHOTO_REQUEST -> {
                        handlePhotoRequest(userMessage, scenario, profile)
                    }
                    MediaExchangeType.VIDEO_REQUEST -> {
                        handleVideoRequest(userMessage, scenario, profile)
                    }
                    MediaExchangeType.PHOTO_RECEIVED -> {
                        handlePhotoReceived(userMessage, scenario, profile)
                    }
                    MediaExchangeType.VIDEO_RECEIVED -> {
                        handleVideoReceived(userMessage, scenario, profile)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("RolePlayingSystem", "Failed to handle media exchange", e)
                MediaExchangeResponse.Error("Media exchange failed: ${e.message}")
            }
        }
    }
    
    /**
     * Get kink-based conversation starters
     */
    fun getKinkBasedStarters(): List<ConversationStarter> {
        val profile = _relationshipProfile.value ?: return emptyList()
        
        return profile.kinks.mapNotNull { kink ->
            generateKinkStarter(kink)
        }.take(5) // Limit to 5 suggestions
    }
    
    /**
     * Update sexual preferences and compatibility
     */
    suspend fun updateSexualPreferences(preferences: SexualPreferences) {
        val currentProfile = _relationshipProfile.value
        if (currentProfile != null) {
            val updatedProfile = currentProfile.copy(sexualPreferences = preferences)
            updateRelationshipProfile(updatedProfile)
        }
    }
    
    /**
     * Add new kink to profile
     */
    suspend fun addKink(kink: Kink) {
        val currentProfile = _relationshipProfile.value
        if (currentProfile != null) {
            val updatedKinks = currentProfile.kinks + kink
            val updatedProfile = currentProfile.copy(kinks = updatedKinks)
            updateRelationshipProfile(updatedProfile)
        }
    }
    
    /**
     * Remove kink from profile
     */
    suspend fun removeKink(kinkId: String) {
        val currentProfile = _relationshipProfile.value
        if (currentProfile != null) {
            val updatedKinks = currentProfile.kinks.filter { it.id != kinkId }
            val updatedProfile = currentProfile.copy(kinks = updatedKinks)
            updateRelationshipProfile(updatedProfile)
        }
    }
    
    /**
     * Generate realistic personality responses based on relationship dynamics
     */
    private fun generateContextualResponse(
        message: String,
        intent: MessageIntent,
        scenario: RolePlayScenario,
        profile: RelationshipProfile
    ): String {
        return when (intent) {
            MessageIntent.FLIRTY -> generateFlirtyResponse(message, scenario, profile)
            MessageIntent.SEXUAL -> generateSexualResponse(message, scenario, profile)
            MessageIntent.ROMANTIC -> generateRomanticResponse(message, scenario, profile)
            MessageIntent.PLAYFUL -> generatePlayfulResponse(message, scenario, profile)
            MessageIntent.INTIMATE -> generateIntimateResponse(message, scenario, profile)
            MessageIntent.KINK_RELATED -> generateKinkResponse(message, scenario, profile)
            MessageIntent.CASUAL -> generateCasualResponse(message, scenario, profile)
        }
    }
    
    /**
     * Analyze user message to determine intent
     */
    private fun analyzeMessageIntent(
        message: String,
        scenario: RolePlayScenario,
        profile: RelationshipProfile
    ): MessageIntent {
        val lowerMessage = message.lowercase()
        
        // Check for kink-related keywords
        profile.kinks.forEach { kink ->
            if (kink.keywords.any { keyword -> lowerMessage.contains(keyword.lowercase()) }) {
                return MessageIntent.KINK_RELATED
            }
        }
        
        return when {
            containsFlirtyKeywords(lowerMessage) -> MessageIntent.FLIRTY
            containsSexualKeywords(lowerMessage) -> MessageIntent.SEXUAL
            containsRomanticKeywords(lowerMessage) -> MessageIntent.ROMANTIC
            containsPlayfulKeywords(lowerMessage) -> MessageIntent.PLAYFUL
            containsIntimateKeywords(lowerMessage) -> MessageIntent.INTIMATE
            else -> MessageIntent.CASUAL
        }
    }
    
    // Response generators for different intents
    
    private fun generateFlirtyResponse(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): String {
        val flirtyResponses = listOf(
            "Mmm, you know exactly what to say to make me blush 😘",
            "Keep talking like that and I might have to come over there 😏",
            "You're being such a tease... I love it 💕",
            "Is that a promise? Because I'm definitely holding you to it 😉",
            "You always know how to make my heart race ❤️"
        )
        return flirtyResponses.random() + addPersonalizedTouch(scenario, profile)
    }
    
    private fun generateSexualResponse(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): String {
        val sexualResponses = listOf(
            "God, you drive me absolutely wild when you talk like that 🔥",
            "I can't stop thinking about what I want to do to you...",
            "You have no idea how turned on I am right now 💋",
            "I need you so badly... when can we be together?",
            "The things I'm imagining right now... 😈"
        )
        return sexualResponses.random() + addKinkContext(profile)
    }
    
    private fun generateRomanticResponse(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): String {
        val romanticResponses = listOf(
            "You always know exactly what to say to make me feel so loved 💕",
            "I'm so lucky to have someone like you in my life ❤️",
            "Every moment with you feels like a dream come true",
            "You make me feel like the most special person in the world",
            "I fall for you more and more every day 💖"
        )
        return romanticResponses.random() + addRelationshipContext(scenario)
    }
    
    private fun generatePlayfulResponse(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): String {
        val playfulResponses = listOf(
            "Haha, you're such a goofball! I love that about you 😄",
            "Oh really? Well two can play at that game 😜",
            "You're so silly... but that's why I adore you 💕",
            "I can't believe you just said that! *giggles* 😂",
            "You always know how to make me smile, even on tough days ✨"
        )
        return playfulResponses.random()
    }
    
    private fun generateIntimateResponse(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): String {
        val intimateResponses = listOf(
            "I love how open and honest we can be with each other 💕",
            "Sharing these moments with you means everything to me",
            "I feel so close to you right now... like nothing else matters",
            "You make me feel so safe and loved when we talk like this ❤️",
            "I treasure these intimate conversations we have together"
        )
        return intimateResponses.random()
    }
    
    private fun generateKinkResponse(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): String {
        val relevantKinks = profile.kinks.filter { kink ->
            kink.keywords.any { keyword -> message.lowercase().contains(keyword.lowercase()) }
        }
        
        if (relevantKinks.isNotEmpty()) {
            val kink = relevantKinks.random()
            return when (kink.intensity) {
                KinkIntensity.MILD -> "I love that you're into that too... it's so hot 😘"
                KinkIntensity.MODERATE -> "Mmm, yes... that's exactly what I was hoping you'd say 🔥"
                KinkIntensity.INTENSE -> "God yes, you know that drives me absolutely crazy 😈"
            }
        }
        
        return generateSexualResponse(message, scenario, profile)
    }
    
    private fun generateCasualResponse(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): String {
        val casualResponses = listOf(
            "That sounds really nice, babe 😊",
            "I'd love that! When were you thinking?",
            "Aww, you're so sweet for thinking of me 💕",
            "That sounds perfect, I can't wait!",
            "You always have the best ideas ✨"
        )
        return casualResponses.random()
    }
    
    // Helper methods for media exchange
    
    private fun handlePhotoRequest(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): MediaExchangeResponse {
        val responses = listOf(
            "I'd love to send you something special... what would you like to see? 😘",
            "Mmm, I like where this is going... give me a few minutes 🔥",
            "You want a little preview of what's waiting for you? 😏",
            "I've been wanting to show you something... are you ready? 💋"
        )
        
        return MediaExchangeResponse.Success(
            message = responses.random(),
            generateMedia = true,
            mediaPrompt = generatePhotoPrompt(profile)
        )
    }
    
    private fun handleVideoRequest(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): MediaExchangeResponse {
        val responses = listOf(
            "A video? Mmm, I think I can arrange something special for you 🎥",
            "You want to see me in action? I love that idea 😈",
            "Give me a few minutes to get ready... this is going to be hot 🔥",
            "I've been waiting for you to ask... let me show you something amazing 💋"
        )
        
        return MediaExchangeResponse.Success(
            message = responses.random(),
            generateMedia = true,
            mediaPrompt = generateVideoPrompt(profile)
        )
    }
    
    private fun handlePhotoReceived(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): MediaExchangeResponse {
        val responses = listOf(
            "Holy shit, you look absolutely incredible 🔥🔥🔥",
            "God damn... you just made my day so much better 😍",
            "I can't stop staring... you're so fucking hot 💋",
            "This is exactly what I needed to see... you're perfect ❤️",
            "I'm getting so worked up just looking at you 😈"
        )
        
        return MediaExchangeResponse.Success(
            message = responses.random(),
            generateMedia = false,
            suggestFollow = true
        )
    }
    
    private fun handleVideoReceived(message: String, scenario: RolePlayScenario, profile: RelationshipProfile): MediaExchangeResponse {
        val responses = listOf(
            "Jesus Christ... that video is so fucking hot I watched it like 5 times 🔥",
            "You have no idea what you just did to me... I'm so turned on right now 😈",
            "That was absolutely perfect... I need more of you 💋",
            "I can't believe how amazing you look... that video is incredible 🎥",
            "You're driving me absolutely wild with that video... more please? 😘"
        )
        
        return MediaExchangeResponse.Success(
            message = responses.random(),
            generateMedia = false,
            suggestFollow = true
        )
    }
    
    // Helper methods for personalization
    
    private fun addPersonalizedTouch(scenario: RolePlayScenario, profile: RelationshipProfile): String {
        return when (scenario.type) {
            RoleType.GIRLFRIEND -> " I can't wait to see you later, baby 💕"
            RoleType.PARTNER -> " We make such a perfect team together ❤️"
            RoleType.LOVER -> " You know exactly how to drive me crazy 😏"
            RoleType.CUSTOM -> " ${scenario.customPersonality}"
        }
    }
    
    private fun addKinkContext(profile: RelationshipProfile): String {
        if (profile.kinks.isNotEmpty()) {
            val randomKink = profile.kinks.random()
            return " You know how much I love ${randomKink.name.lowercase()} 😈"
        }
        return ""
    }
    
    private fun addRelationshipContext(scenario: RolePlayScenario): String {
        return when (scenario.relationshipStage) {
            RelationshipStage.NEW -> " This is all so new and exciting with you ✨"
            RelationshipStage.ESTABLISHED -> " I love how comfortable we are with each other 💕"
            RelationshipStage.DEEP -> " The connection we have is so special and deep ❤️"
        }
    }
    
    private fun generatePhotoPrompt(profile: RelationshipProfile): String {
        val basePrompts = listOf(
            "beautiful woman, intimate selfie, bedroom lighting",
            "attractive girlfriend, flirty pose, soft lighting",
            "sexy woman, teasing expression, romantic atmosphere"
        )
        
        var prompt = basePrompts.random()
        
        // Add kink-specific elements if applicable
        profile.kinks.forEach { kink ->
            if (kink.visualElements.isNotEmpty()) {
                prompt += ", ${kink.visualElements.random()}"
            }
        }
        
        return prompt
    }
    
    private fun generateVideoPrompt(profile: RelationshipProfile): String {
        val basePrompts = listOf(
            "beautiful woman, intimate video, bedroom setting",
            "attractive girlfriend, playful movement, soft lighting",
            "sexy woman, seductive poses, romantic atmosphere"
        )
        
        return basePrompts.random() // Video prompts can be enhanced similarly
    }
    
    private fun generateSuggestedActions(
        intent: MessageIntent,
        scenario: RolePlayScenario,
        profile: RelationshipProfile
    ): List<String> {
        return when (intent) {
            MessageIntent.SEXUAL -> listOf(
                "Send photo",
                "Send video", 
                "Voice message",
                "Continue flirting"
            )
            MessageIntent.FLIRTY -> listOf(
                "Flirt back",
                "Send cute selfie",
                "Voice note",
                "Plan meetup"
            )
            else -> listOf(
                "Continue conversation",
                "Send photo",
                "Voice message"
            )
        }
    }
    
    // Keyword detection methods
    
    private fun containsFlirtyKeywords(message: String): Boolean {
        val flirtyKeywords = listOf(
            "sexy", "hot", "cute", "beautiful", "gorgeous", "handsome",
            "attractive", "flirt", "tease", "wink", "smile"
        )
        return flirtyKeywords.any { message.contains(it) }
    }
    
    private fun containsSexualKeywords(message: String): Boolean {
        val sexualKeywords = listOf(
            "horny", "turned on", "aroused", "want you", "need you",
            "fuck", "sex", "naked", "nude", "hard", "wet"
        )
        return sexualKeywords.any { message.contains(it) }
    }
    
    private fun containsRomanticKeywords(message: String): Boolean {
        val romanticKeywords = listOf(
            "love", "adore", "cherish", "romantic", "heart",
            "feelings", "emotion", "care", "affection"
        )
        return romanticKeywords.any { message.contains(it) }
    }
    
    private fun containsPlayfulKeywords(message: String): Boolean {
        val playfulKeywords = listOf(
            "fun", "silly", "funny", "laugh", "giggle",
            "play", "game", "joke", "tease"
        )
        return playfulKeywords.any { message.contains(it) }
    }
    
    private fun containsIntimateKeywords(message: String): Boolean {
        val intimateKeywords = listOf(
            "close", "intimate", "personal", "private", "share",
            "trust", "open", "vulnerable", "deep"
        )
        return intimateKeywords.any { message.contains(it) }
    }
    
    private fun generateKinkStarter(kink: Kink): ConversationStarter? {
        return when (kink.category) {
            KinkCategory.VISUAL -> ConversationStarter(
                text = "I've been thinking about trying something new with photos... 📸",
                type = StarterType.KINK_EXPLORATION
            )
            KinkCategory.ROLEPLAY -> ConversationStarter(
                text = "Want to try something different tonight? 😏",
                type = StarterType.KINK_EXPLORATION
            )
            KinkCategory.PHYSICAL -> ConversationStarter(
                text = "I can't stop thinking about what we talked about... 🔥",
                type = StarterType.KINK_EXPLORATION
            )
            KinkCategory.VERBAL -> ConversationStarter(
                text = "Tell me what you're thinking about right now... 💭",
                type = StarterType.KINK_EXPLORATION
            )
        }
    }
    
    // Storage methods
    
    private fun saveProfile(profile: RelationshipProfile) {
        try {
            val profileJson = json.encodeToString(profile)
            preferences.edit().putString("relationship_profile", profileJson).apply()
        } catch (e: Exception) {
            Log.e("RolePlayingSystem", "Failed to save profile", e)
        }
    }
    
    private fun saveScenario(scenario: RolePlayScenario) {
        try {
            val scenarioJson = json.encodeToString(scenario)
            preferences.edit().putString("current_scenario", scenarioJson).apply()
        } catch (e: Exception) {
            Log.e("RolePlayingSystem", "Failed to save scenario", e)
        }
    }
    
    private fun loadSavedProfile() {
        try {
            val profileJson = preferences.getString("relationship_profile", null)
            if (profileJson != null) {
                _relationshipProfile.value = json.decodeFromString<RelationshipProfile>(profileJson)
            }
            
            val scenarioJson = preferences.getString("current_scenario", null)
            if (scenarioJson != null) {
                _currentRole.value = json.decodeFromString<RolePlayScenario>(scenarioJson)
            }
        } catch (e: Exception) {
            Log.e("RolePlayingSystem", "Failed to load saved data", e)
        }
    }
    
    private fun updateConversationMemory(message: String, response: String, scenario: RolePlayScenario) {
        // Store conversation context for better continuity
        val conversationEntry = ConversationMemory(
            userMessage = message,
            botResponse = response,
            timestamp = System.currentTimeMillis(),
            scenario = scenario.type.name
        )
        
        // Save to preferences or database for conversation history
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
    }
}

// Data classes for role playing system

@Serializable
data class RolePlayScenario(
    val type: RoleType,
    val relationshipStage: RelationshipStage,
    val personality: PersonalityTraits,
    val customPersonality: String = "",
    val backstory: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class RelationshipProfile(
    val name: String = "Baby",
    val petNames: List<String> = listOf("babe", "baby", "honey", "love"),
    val kinks: List<Kink> = emptyList(),
    val sexualPreferences: SexualPreferences,
    val communicationStyle: CommunicationStyle,
    val intimacyLevel: IntimacyLevel,
    val relationshipGoals: List<String> = emptyList()
)

@Serializable
data class Kink(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: KinkCategory,
    val intensity: KinkIntensity,
    val keywords: List<String>,
    val visualElements: List<String> = emptyList(),
    val isActive: Boolean = true
)

@Serializable
data class SexualPreferences(
    val dominanceLevel: Float = 0.5f, // 0.0 = submissive, 1.0 = dominant
    val adventurousness: Float = 0.5f, // 0.0 = vanilla, 1.0 = very kinky
    val romanticLevel: Float = 0.7f, // 0.0 = purely physical, 1.0 = very romantic
    val communicationOpenness: Float = 0.8f, // 0.0 = shy, 1.0 = very open
    val physicalAffection: Float = 0.8f // 0.0 = minimal, 1.0 = very affectionate
)

@Serializable
data class PersonalityTraits(
    val playfulness: Float = 0.7f,
    val flirtiness: Float = 0.8f,
    val sweetness: Float = 0.7f,
    val sassiness: Float = 0.5f,
    val supportiveness: Float = 0.9f,
    val independence: Float = 0.6f
)

@Serializable
data class ConversationMemory(
    val userMessage: String,
    val botResponse: String,
    val timestamp: Long,
    val scenario: String
)

data class ConversationStarter(
    val text: String,
    val type: StarterType
)

sealed class RolePlayResponse {
    data class Success(
        val response: String,
        val scenario: RolePlayScenario,
        val suggestedActions: List<String> = emptyList()
    ) : RolePlayResponse()
    
    data class Error(val message: String) : RolePlayResponse()
}

sealed class MediaExchangeResponse {
    data class Success(
        val message: String,
        val generateMedia: Boolean,
        val mediaPrompt: String = "",
        val suggestFollow: Boolean = false
    ) : MediaExchangeResponse()
    
    data class Error(val message: String) : MediaExchangeResponse()
}

enum class RoleType {
    GIRLFRIEND, PARTNER, LOVER, CUSTOM
}

enum class RelationshipStage {
    NEW, ESTABLISHED, DEEP
}

enum class KinkCategory {
    VISUAL, ROLEPLAY, PHYSICAL, VERBAL
}

enum class KinkIntensity {
    MILD, MODERATE, INTENSE
}

enum class CommunicationStyle {
    SWEET_ROMANTIC, PLAYFUL_TEASING, DIRECT_HONEST, SUBMISSIVE_CARING, DOMINANT_CONFIDENT
}

enum class IntimacyLevel {
    CASUAL, MODERATE, DEEP, INTENSE
}

enum class MessageIntent {
    FLIRTY, SEXUAL, ROMANTIC, PLAYFUL, INTIMATE, KINK_RELATED, CASUAL
}

enum class MediaExchangeType {
    PHOTO_REQUEST, VIDEO_REQUEST, PHOTO_RECEIVED, VIDEO_RECEIVED
}

enum class StarterType {
    FLIRTY, ROMANTIC, SEXUAL, KINK_EXPLORATION, CASUAL
}