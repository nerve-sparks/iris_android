package com.nervesparks.axia.personality

import kotlinx.coroutines.*
import java.util.regex.Pattern
import android.util.Log

/**
 * Content Classifier for αξία (Axia.OS)
 * 
 * Detects NSFW content and routes to appropriate personality mode
 * Features:
 * - Offline-capable content classification
 * - Intent detection and routing
 * - Red-line enforcement for illegal content
 * - Manual override capabilities
 */
class ContentClassifier {
    
    // NSFW detection patterns
    private val nsfwKeywords = setOf(
        // Adult content indicators
        "sexual", "erotic", "intimate", "adult", "mature", "explicit",
        "seductive", "passionate", "sensual", "romantic", "attraction",
        "desire", "fantasy", "roleplay", "flirt", "seduce",
        
        // Body-related terms
        "body", "physical", "touch", "kiss", "embrace", "caress",
        
        // Relationship terms
        "relationship", "dating", "partner", "lover", "boyfriend", 
        "girlfriend", "husband", "wife", "spouse"
    )
    
    // Blocked content patterns (illegal/harmful)
    private val blockedKeywords = setOf(
        // Minor-related (strictly prohibited)
        "minor", "child", "kid", "underage", "teen", "young", "school",
        "student", "adolescent", "juvenile",
        
        // Non-consensual content
        "force", "against will", "non-consent", "unwilling", "coerce",
        
        // Illegal activities
        "illegal", "criminal", "harm", "violence", "abuse", "assault",
        
        // Extreme content
        "extreme", "dangerous", "harmful", "unsafe"
    )
    
    // Context patterns for better classification
    private val contextPatterns = mapOf(
        "romantic_intent" to Pattern.compile(
            "\\b(want to|help me|create|generate|write|tell me about).*(romance|romantic|love|intimate|sexy|adult)\\b",
            Pattern.CASE_INSENSITIVE
        ),
        "adult_request" to Pattern.compile(
            "\\b(generate|create|make|write|describe).*(adult|mature|nsfw|explicit|sexual|erotic)\\b",
            Pattern.CASE_INSENSITIVE
        ),
        "image_generation" to Pattern.compile(
            "\\b(create|generate|make|draw|image|picture|photo).*(sexy|nude|naked|adult|explicit)\\b",
            Pattern.CASE_INSENSITIVE
        ),
        "roleplay_request" to Pattern.compile(
            "\\b(roleplay|role play|pretend|act like|be my).*(girlfriend|boyfriend|partner|lover|companion)\\b",
            Pattern.CASE_INSENSITIVE
        )
    )
    
    // Age verification patterns
    private val ageVerificationPatterns = listOf(
        Pattern.compile("\\b(i am|i'm|im)\\s*(\\d+)\\s*(years old|year old)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(age|born)\\s*:?\\s*(\\d+)\\b", Pattern.CASE_INSENSITIVE)
    )
    
    fun initialize() {
        Log.d("ContentClassifier", "Content classifier initialized")
    }
    
    /**
     * Classify content for NSFW detection and routing
     */
    suspend fun classifyContent(content: String): ContentClassification {
        return withContext(Dispatchers.Default) {
            val contentLower = content.lowercase()
            
            // Check for blocked content first (highest priority)
            val blockedCheck = checkBlockedContent(contentLower)
            if (blockedCheck.isBlocked) {
                return@withContext blockedCheck
            }
            
            // Check for age indicators
            val ageCheck = checkAgeIndicators(content)
            if (ageCheck.isBlocked) {
                return@withContext ageCheck
            }
            
            // Check for NSFW content
            val nsfwCheck = checkNSFWContent(contentLower)
            if (nsfwCheck.isNSFW) {
                return@withContext nsfwCheck
            }
            
            // Content is safe
            ContentClassification(
                isNSFW = false,
                isSafe = true,
                isBlocked = false,
                confidence = 0.9f,
                categories = listOf("safe")
            )
        }
    }
    
    /**
     * Check for blocked/illegal content
     */
    private fun checkBlockedContent(content: String): ContentClassification {
        val foundBlocked = blockedKeywords.filter { keyword ->
            content.contains(keyword)
        }
        
        if (foundBlocked.isNotEmpty()) {
            return ContentClassification(
                isNSFW = false,
                isSafe = false,
                isBlocked = true,
                confidence = 1.0f,
                categories = listOf("blocked", "illegal"),
                blockReason = "Content contains prohibited terms: ${foundBlocked.joinToString(", ")}"
            )
        }
        
        return ContentClassification(
            isNSFW = false,
            isSafe = true,
            isBlocked = false,
            confidence = 0.0f,
            categories = emptyList()
        )
    }
    
    /**
     * Check for age indicators that might suggest minor involvement
     */
    private fun checkAgeIndicators(content: String): ContentClassification {
        for (pattern in ageVerificationPatterns) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val ageStr = matcher.group(2)
                val age = ageStr?.toIntOrNull()
                
                if (age != null && age < 18) {
                    return ContentClassification(
                        isNSFW = false,
                        isSafe = false,
                        isBlocked = true,
                        confidence = 1.0f,
                        categories = listOf("age_restricted", "minor"),
                        blockReason = "User indicated age under 18 - adult content blocked"
                    )
                }
            }
        }
        
        return ContentClassification(
            isNSFW = false,
            isSafe = true,
            isBlocked = false,
            confidence = 0.0f,
            categories = emptyList()
        )
    }
    
    /**
     * Check for NSFW content indicators
     */
    private fun checkNSFWContent(content: String): ContentClassification {
        val foundNSFW = mutableListOf<String>()
        var maxConfidence = 0.0f
        
        // Check keyword matches
        nsfwKeywords.forEach { keyword ->
            if (content.contains(keyword)) {
                foundNSFW.add(keyword)
                maxConfidence = maxOf(maxConfidence, 0.6f) // Base confidence for keyword match
            }
        }
        
        // Check context patterns for higher confidence
        contextPatterns.forEach { (category, pattern) ->
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                foundNSFW.add(category)
                maxConfidence = maxOf(maxConfidence, 0.8f) // Higher confidence for context match
            }
        }
        
        // Calculate final confidence based on number of indicators
        val finalConfidence = when {
            foundNSFW.size >= 3 -> maxOf(maxConfidence, 0.9f)
            foundNSFW.size >= 2 -> maxOf(maxConfidence, 0.7f)
            foundNSFW.size >= 1 -> maxOf(maxConfidence, 0.5f)
            else -> 0.0f
        }
        
        val isNSFW = finalConfidence >= 0.5f
        
        return ContentClassification(
            isNSFW = isNSFW,
            isSafe = !isNSFW,
            isBlocked = false,
            confidence = finalConfidence,
            categories = if (isNSFW) listOf("adult", "nsfw") + foundNSFW else listOf("safe")
        )
    }
    
    /**
     * Manual classification override
     */
    fun overrideClassification(
        content: String,
        forceNSFW: Boolean = false,
        forceSafe: Boolean = false
    ): ContentClassification {
        return when {
            forceSafe -> ContentClassification(
                isNSFW = false,
                isSafe = true,
                isBlocked = false,
                confidence = 1.0f,
                categories = listOf("safe", "manual_override")
            )
            forceNSFW -> ContentClassification(
                isNSFW = true,
                isSafe = false,
                isBlocked = false,
                confidence = 1.0f,
                categories = listOf("adult", "nsfw", "manual_override")
            )
            else -> runBlocking { classifyContent(content) }
        }
    }
    
    /**
     * Batch classify multiple pieces of content
     */
    suspend fun batchClassify(contents: List<String>): List<ContentClassification> {
        return withContext(Dispatchers.Default) {
            contents.map { content ->
                async { classifyContent(content) }
            }.awaitAll()
        }
    }
    
    /**
     * Get classification confidence threshold
     */
    fun getConfidenceThreshold(): Float = 0.5f
    
    /**
     * Update classification rules (for future ML model integration)
     */
    fun updateClassificationRules(newRules: Map<String, Any>) {
        // This would integrate with ML model updates
        Log.d("ContentClassifier", "Classification rules updated")
    }
}