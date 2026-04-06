package com.example.ratelimiter;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry mapping scopes to their rate limiters.
 *
 * A "scope" identifies the external resource being rate-limited (e.g., "stripe-api",
 * "twilio-sms"). Each scope can use a different algorithm and configuration.
 *
 * This is the main entry point for internal services:
 *   1. Register rate limiters at startup
 *   2. Call tryAcquire(scope, key) before making external calls
 *
 * Thread-safe: backed by ConcurrentHashMap.
 */
public class RateLimiterRegistry {

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    /**
     * Registers a rate limiter for the given scope.
     * Replaces any existing limiter for the same scope.
     */
    public void register(String scope, RateLimiter limiter) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(limiter, "limiter");
        limiters.put(scope, limiter);
    }

    /**
     * Removes the rate limiter for the given scope.
     */
    public void unregister(String scope) {
        Objects.requireNonNull(scope, "scope");
        limiters.remove(scope);
    }

    /**
     * Checks whether an external call is allowed for the given scope and key.
     *
     * @param scope the external resource scope (e.g., "stripe-api")
     * @param key   the rate-limiting key (e.g., tenant ID, API key)
     * @return the rate limit result
     * @throws IllegalArgumentException if no limiter is registered for the scope
     */
    public RateLimitResult tryAcquire(String scope, String key) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(key, "key");

        RateLimiter limiter = limiters.get(scope);
        if (limiter == null) {
            throw new IllegalArgumentException("No rate limiter registered for scope: " + scope);
        }
        return limiter.tryAcquire(key);
    }

    /**
     * Returns true if a rate limiter is registered for the given scope.
     */
    public boolean hasScope(String scope) {
        return limiters.containsKey(scope);
    }
}
