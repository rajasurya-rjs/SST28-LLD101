package com.example.ratelimiter;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration for a rate limit: max requests within a time window.
 */
public final class RateLimitRule {

    private final int maxRequests;
    private final Duration window;

    public RateLimitRule(int maxRequests, Duration window) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive, got: " + maxRequests);
        }
        this.window = Objects.requireNonNull(window, "window must not be null");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive, got: " + window);
        }
        this.maxRequests = maxRequests;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public Duration getWindow() {
        return window;
    }

    public long getWindowMillis() {
        return window.toMillis();
    }

    @Override
    public String toString() {
        return maxRequests + " requests per " + window;
    }
}
