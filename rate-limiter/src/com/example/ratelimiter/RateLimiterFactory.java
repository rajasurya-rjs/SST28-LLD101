package com.example.ratelimiter;

import java.time.Clock;
import java.util.Objects;

/**
 * Factory for creating configured RateLimiter instances.
 *
 * Callers use this factory instead of directly instantiating algorithm classes,
 * keeping business logic decoupled from specific implementations.
 *
 * To add a new algorithm:
 *   1. Create a class implementing RateLimiter
 *   2. Add a factory method here
 *   3. No other code changes required (Open/Closed Principle)
 */
public final class RateLimiterFactory {

    private RateLimiterFactory() {
        // Non-instantiable utility class
    }

    public static RateLimiter fixedWindow(RateLimitRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new FixedWindowRateLimiter(rule);
    }

    public static RateLimiter fixedWindow(RateLimitRule rule, Clock clock) {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(clock, "clock");
        return new FixedWindowRateLimiter(rule, clock);
    }

    public static RateLimiter slidingWindow(RateLimitRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new SlidingWindowRateLimiter(rule);
    }

    public static RateLimiter slidingWindow(RateLimitRule rule, Clock clock) {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(clock, "clock");
        return new SlidingWindowRateLimiter(rule, clock);
    }
}
