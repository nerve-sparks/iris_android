package com.nervesparks.iris.streaming

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages real-time token streaming for ChatGPT-like live typing experience
 * Optimized for Samsung Galaxy S24 Ultra with 120Hz display
 */
class StreamingTokenManager {
    private val _tokenFlow = MutableSharedFlow<StreamingToken>(
        replay = 0,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val tokenFlow: SharedFlow<StreamingToken> = _tokenFlow.asSharedFlow()
    
    private val tokenQueue = ConcurrentLinkedQueue<String>()
    private val isStreaming = AtomicBoolean(false)
    private val tokenCount = AtomicLong(0)
    private val startTime = AtomicLong(0)
    
    private var streamingJob: Job? = null
    private var tokensPerSecond = 0.0
    
    companion object {
        private const val TAG = "StreamingTokenManager"
        private const val TARGET_FPS_120HZ = 16L // 16ms for 60fps, 8ms for 120fps
        private const val SMOOTH_ANIMATION_DELAY = 8L // Optimized for 120Hz
    }
    
    data class StreamingToken(
        val token: String,
        val isComplete: Boolean = false,
        val tokensPerSecond: Double = 0.0,
        val totalTokens: Long = 0,
        val messageId: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Start streaming tokens with ChatGPT-like smooth animation
     */
    fun startStreaming(messageId: String) {
        if (isStreaming.compareAndSet(false, true)) {
            startTime.set(System.currentTimeMillis())
            tokenCount.set(0)
            
            streamingJob = CoroutineScope(Dispatchers.Main).launch {
                while (isStreaming.get()) {
                    val token = tokenQueue.poll()
                    if (token != null) {
                        val currentCount = tokenCount.incrementAndGet()
                        val elapsed = (System.currentTimeMillis() - startTime.get()) / 1000.0
                        tokensPerSecond = if (elapsed > 0) currentCount / elapsed else 0.0
                        
                        _tokenFlow.emit(
                            StreamingToken(
                                token = token,
                                tokensPerSecond = tokensPerSecond,
                                totalTokens = currentCount,
                                messageId = messageId
                            )
                        )
                        
                        // Smooth 120Hz animation delay
                        delay(SMOOTH_ANIMATION_DELAY)
                    } else {
                        delay(10) // Short delay when no tokens available
                    }
                }
            }
        }
    }
    
    /**
     * Add token to streaming queue
     */
    fun addToken(token: String) {
        if (isStreaming.get()) {
            tokenQueue.offer(token)
        }
    }
    
    /**
     * Add multiple tokens for batch processing
     */
    fun addTokens(tokens: List<String>) {
        if (isStreaming.get()) {
            tokens.forEach { tokenQueue.offer(it) }
        }
    }
    
    /**
     * Complete current streaming session
     */
    fun completeStreaming(messageId: String) {
        if (isStreaming.compareAndSet(true, false)) {
            CoroutineScope(Dispatchers.Main).launch {
                // Drain remaining tokens
                while (tokenQueue.isNotEmpty()) {
                    val token = tokenQueue.poll()
                    if (token != null) {
                        val currentCount = tokenCount.incrementAndGet()
                        _tokenFlow.emit(
                            StreamingToken(
                                token = token,
                                tokensPerSecond = tokensPerSecond,
                                totalTokens = currentCount,
                                messageId = messageId
                            )
                        )
                        delay(SMOOTH_ANIMATION_DELAY)
                    }
                }
                
                // Send completion signal
                _tokenFlow.emit(
                    StreamingToken(
                        token = "",
                        isComplete = true,
                        tokensPerSecond = tokensPerSecond,
                        totalTokens = tokenCount.get(),
                        messageId = messageId
                    )
                )
                
                streamingJob?.cancel()
                Log.d(TAG, "Streaming completed. Total tokens: ${tokenCount.get()}, Rate: ${tokensPerSecond} t/s")
            }
        }
    }
    
    /**
     * Stop streaming immediately
     */
    fun stopStreaming() {
        isStreaming.set(false)
        streamingJob?.cancel()
        tokenQueue.clear()
    }
    
    /**
     * Get current streaming statistics
     */
    fun getStreamingStats(): StreamingStats {
        val elapsed = (System.currentTimeMillis() - startTime.get()) / 1000.0
        return StreamingStats(
            tokensPerSecond = if (elapsed > 0) tokenCount.get() / elapsed else 0.0,
            totalTokens = tokenCount.get(),
            elapsedTime = elapsed,
            queueSize = tokenQueue.size
        )
    }
    
    data class StreamingStats(
        val tokensPerSecond: Double,
        val totalTokens: Long,
        val elapsedTime: Double,
        val queueSize: Int
    )
}