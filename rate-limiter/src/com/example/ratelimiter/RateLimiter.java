package com.example.ratelimiter;

/**
 * Strategy interface for rate limiting algorithms.
 *
 * Each implementation encapsulates a specific algorithm (Fixed Window, Sliding Window, etc.).
 * Callers interact only with this interface, enabling algorithm swapping without code changes.
 */
public interface RateLimiter {

    /**
     * Attempts to acquire permission for one request under the given key.
     *
     * This method atomically checks the current count and records the request if allowed.
     *
     * @param key the rate-limiting key (e.g., "tenant:T1", "apikey:xyz")
     * @return a RateLimitResult indicating whether the request is allowed or denied
     */
    RateLimitResult tryAcquire(String key);
}
