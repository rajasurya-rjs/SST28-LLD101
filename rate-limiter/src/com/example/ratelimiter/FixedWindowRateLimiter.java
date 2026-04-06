package com.example.ratelimiter;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed Window Counter algorithm.
 *
 * Divides time into discrete, non-overlapping windows (e.g., every 60 seconds
 * aligned to epoch). Each window maintains a counter per key. When a new window
 * starts, the counter resets.
 *
 * Trade-offs:
 *   + Simple, O(1) time and space per key
 *   + Easy to understand and debug
 *   - Boundary burst problem: up to 2x the limit can pass if requests cluster
 *     at the end of one window and the start of the next
 *
 * Thread-safety: ConcurrentHashMap.compute() provides atomic per-key updates.
 */
public class FixedWindowRateLimiter implements RateLimiter {

    private final RateLimitRule rule;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(RateLimitRule rule, Clock clock) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public FixedWindowRateLimiter(RateLimitRule rule) {
        this(rule, Clock.systemUTC());
    }

    @Override
    public RateLimitResult tryAcquire(String key) {
        Objects.requireNonNull(key, "key");

        long windowMillis = rule.getWindowMillis();
        int maxRequests = rule.getMaxRequests();

        // compute() is atomic per-key in ConcurrentHashMap
        WindowState state = windows.compute(key, (k, existing) -> {
            long now = clock.millis();
            long currentWindowStart = now - (now % windowMillis);

            if (existing == null || existing.windowStart != currentWindowStart) {
                // New window — start fresh with count = 1 (optimistic: count this request)
                return new WindowState(currentWindowStart, 1);
            }

            // Same window — increment
            existing.count++;
            return existing;
        });

        if (state.count <= maxRequests) {
            return RateLimitResult.allowed(maxRequests - state.count);
        }

        // Denied — calculate time until window resets
        long now = clock.millis();
        long windowEnd = state.windowStart + windowMillis;
        long retryAfter = Math.max(0, windowEnd - now);
        return RateLimitResult.denied(retryAfter);
    }

    /**
     * Mutable state for a single window. Only mutated inside compute(), which
     * is atomic per-key, so no additional synchronization is needed.
     */
    private static class WindowState {
        final long windowStart;
        int count;

        WindowState(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
