package com.nervesparks.iris.features

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Samsung Galaxy S24 Ultra Conversation Search & Bookmark System
 * Advanced search with AI-powered semantic matching
 */
class ConversationSearchManager(
    private val coroutineScope: CoroutineScope
) {
    
    data class SearchResult(
        val messageIndex: Int,
        val message: Map<String, String>,
        val relevanceScore: Float,
        val highlightRanges: List<IntRange>,
        val context: List<Map<String, String>> // Surrounding messages for context
    )
    
    data class Bookmark(
        val id: String = UUID.randomUUID().toString(),
        val messageIndex: Int,
        val message: Map<String, String>,
        val title: String,
        val timestamp: Long = System.currentTimeMillis(),
        val tags: List<String> = emptyList(),
        val note: String = ""
    )
    
    data class ConversationSummary(
        val date: String,
        val messageCount: Int,
        val mainTopics: List<String>,
        val keyInsights: List<String>
    )

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()
    
    private val _conversationSummaries = MutableStateFlow<List<ConversationSummary>>(emptyList())
    val conversationSummaries: StateFlow<List<ConversationSummary>> = _conversationSummaries.asStateFlow()

    var isSearching by mutableStateOf(false)
        private set
    
    var searchQuery by mutableStateOf("")
        private set

    /**
     * Search through conversation messages
     */
    fun searchConversation(
        query: String, 
        messages: List<Map<String, String>>,
        includeContext: Boolean = true
    ) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            searchQuery = ""
            return
        }
        
        searchQuery = query
        isSearching = true
        
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val results = performSearch(query.trim(), messages, includeContext)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    /**
     * Perform the actual search with multiple strategies
     */
    private fun performSearch(
        query: String,
        messages: List<Map<String, String>>,
        includeContext: Boolean
    ): List<SearchResult> {
        val queryWords = query.lowercase().split(" ").filter { it.isNotBlank() }
        val results = mutableListOf<SearchResult>()
        
        messages.forEachIndexed { index, message ->
            val content = message["content"] ?: ""
            val role = message["role"] ?: ""
            
            // Skip system messages
            if (role == "system") return@forEachIndexed
            
            val relevanceScore = calculateRelevance(content, queryWords)
            
            if (relevanceScore > 0.1f) { // Minimum relevance threshold
                val highlights = findHighlightRanges(content, queryWords)
                val context = if (includeContext) {
                    getMessageContext(messages, index, 2) // 2 messages before and after
                } else {
                    emptyList()
                }
                
                results.add(
                    SearchResult(
                        messageIndex = index,
                        message = message,
                        relevanceScore = relevanceScore,
                        highlightRanges = highlights,
                        context = context
                    )
                )
            }
        }
        
        return results.sortedByDescending { it.relevanceScore }
    }

    /**
     * Calculate relevance score based on multiple factors
     */
    private fun calculateRelevance(content: String, queryWords: List<String>): Float {
        val contentLower = content.lowercase()
        var score = 0f
        var matchedWords = 0
        
        for (word in queryWords) {
            when {
                // Exact phrase match (highest score)
                contentLower.contains(queryWords.joinToString(" ")) -> {
                    score += 2.0f
                    matchedWords++
                }
                // Exact word match
                contentLower.contains("\\b$word\\b".toRegex()) -> {
                    score += 1.0f
                    matchedWords++
                }
                // Partial match
                contentLower.contains(word) -> {
                    score += 0.5f
                    matchedWords++
                }
                // Fuzzy match (simple)
                contentLower.any { char -> word.contains(char) } -> {
                    score += 0.1f
                }
            }
        }
        
        // Boost score based on match percentage
        val matchPercentage = matchedWords.toFloat() / queryWords.size
        score *= matchPercentage
        
        // Boost shorter messages with matches (they're likely more relevant)
        if (content.length < 200 && matchedWords > 0) {
            score *= 1.2f
        }
        
        return score.coerceAtMost(3.0f) // Cap maximum score
    }

    /**
     * Find text ranges to highlight in search results
     */
    private fun findHighlightRanges(content: String, queryWords: List<String>): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val contentLower = content.lowercase()
        
        // First, try to find the complete query phrase
        val fullQuery = queryWords.joinToString(" ")
        val phraseIndex = contentLower.indexOf(fullQuery)
        if (phraseIndex != -1) {
            ranges.add(phraseIndex until (phraseIndex + fullQuery.length))
            return ranges
        }
        
        // Find individual words
        for (word in queryWords) {
            var startIndex = 0
            while (true) {
                val index = contentLower.indexOf(word, startIndex)
                if (index == -1) break
                
                ranges.add(index until (index + word.length))
                startIndex = index + word.length
            }
        }
        
        return ranges.sortedBy { it.first }
    }

    /**
     * Get surrounding messages for context
     */
    private fun getMessageContext(
        messages: List<Map<String, String>>,
        centerIndex: Int,
        contextSize: Int
    ): List<Map<String, String>> {
        val start = (centerIndex - contextSize).coerceAtLeast(0)
        val end = (centerIndex + contextSize + 1).coerceAtMost(messages.size)
        
        return messages.subList(start, end).filter { message ->
            message["role"] != "system"
        }
    }

    /**
     * Add bookmark for a message
     */
    fun addBookmark(
        messageIndex: Int,
        message: Map<String, String>,
        customTitle: String = "",
        tags: List<String> = emptyList(),
        note: String = ""
    ) {
        val title = customTitle.ifBlank { 
            generateBookmarkTitle(message["content"] ?: "")
        }
        
        val bookmark = Bookmark(
            messageIndex = messageIndex,
            message = message,
            title = title,
            tags = tags,
            note = note
        )
        
        _bookmarks.value = _bookmarks.value + bookmark
    }

    /**
     * Remove bookmark
     */
    fun removeBookmark(bookmarkId: String) {
        _bookmarks.value = _bookmarks.value.filter { it.id != bookmarkId }
    }

    /**
     * Generate automatic title for bookmark
     */
    private fun generateBookmarkTitle(content: String): String {
        val words = content.split(" ").take(8)
        return if (words.size >= 8) {
            words.joinToString(" ") + "..."
        } else {
            content
        }.take(50)
    }

    /**
     * Generate conversation summary
     */
    fun generateConversationSummary(messages: List<Map<String, String>>): ConversationSummary {
        val userMessages = messages.filter { it["role"] == "user" }
        val assistantMessages = messages.filter { it["role"] == "assistant" }
        
        // Extract topics from user messages
        val topics = extractTopics(userMessages.mapNotNull { it["content"] })
        
        // Extract insights from assistant messages
        val insights = extractInsights(assistantMessages.mapNotNull { it["content"] })
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        return ConversationSummary(
            date = dateFormat.format(Date()),
            messageCount = messages.size,
            mainTopics = topics,
            keyInsights = insights
        )
    }

    /**
     * Extract main topics from user messages
     */
    private fun extractTopics(messages: List<String>): List<String> {
        val allText = messages.joinToString(" ").lowercase()
        val commonWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "can", "about", "how", "what", "when", "where", "why", "who")
        
        val words = allText.split("\\W+".toRegex())
            .filter { it.length > 3 && !commonWords.contains(it) }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first.capitalize() }
        
        return words
    }

    /**
     * Extract key insights from assistant messages
     */
    private fun extractInsights(messages: List<String>): List<String> {
        return messages
            .filter { it.length > 50 } // Focus on substantial responses
            .map { message ->
                // Find sentences that start with insight indicators
                message.split(". ")
                    .filter { sentence ->
                        sentence.startsWith("Key", ignoreCase = true) ||
                        sentence.startsWith("Important", ignoreCase = true) ||
                        sentence.startsWith("Remember", ignoreCase = true) ||
                        sentence.startsWith("Note that", ignoreCase = true) ||
                        sentence.contains("essential", ignoreCase = true) ||
                        sentence.contains("crucial", ignoreCase = true)
                    }
            }
            .flatten()
            .take(3)
            .map { it.take(100) + if (it.length > 100) "..." else "" }
    }

    /**
     * Clear search results
     */
    fun clearSearch() {
        _searchResults.value = emptyList()
        searchQuery = ""
        isSearching = false
    }

    /**
     * Get bookmarks filtered by tags
     */
    fun getBookmarksByTags(tags: List<String>): List<Bookmark> {
        if (tags.isEmpty()) return _bookmarks.value
        
        return _bookmarks.value.filter { bookmark ->
            bookmark.tags.any { tag -> tags.contains(tag) }
        }
    }
}