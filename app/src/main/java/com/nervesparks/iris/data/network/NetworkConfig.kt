package com.nervesparks.iris.data.network

/**
 * Configuration values for network caching behaviour.
 * @param cacheSizeBytes size of the on-disk HTTP cache.
 * @param maxAgeSeconds max age for cached responses before they are considered stale.
 * @param maxStaleSeconds how long to keep stale responses before eviction.
 */
data class NetworkConfig(
    val cacheSizeBytes: Long = 10L * 1024 * 1024, // 10MB
    val maxAgeSeconds: Int = 60,
    val maxStaleSeconds: Int = 60 * 60 * 24, // 1 day
    val connectTimeoutMs: Long = 15_000,
    val readTimeoutMs: Long = 30_000,
    val writeTimeoutMs: Long = 30_000,
    val callTimeoutMs: Long = 60_000,
    val userAgent: String = "IrisStar/1.0 (Android)"
)
