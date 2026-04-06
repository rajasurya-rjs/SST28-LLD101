package com.example.ratelimiter;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Counter algorithm (weighted approximation).
 *
 * Maintains the current window's count and the previous window's count. The
 * effective request count is calculated as:
 *
 *   effectiveCount = previousCount * overlapRatio + currentCount
 *
 * where overlapRatio = (windowSize - elapsedInCurrentWindow) / windowSize
 *
 * This approximates a true sliding window without storing individual timestamps.
 *
 * Trade-offs:
 *   + O(1) time and space per key (same as fixed window)
 *   + Smooths out the boundary burst problem of fixed windows
 *   - Approximation, not exact — can slightly over- or under-count at boundaries
 *   - Slightly more complex than fixed window
 *
 * Thread-safety: ConcurrentHashMap.compute() provides atomic per-key updates.
 */
public class SlidingWindowRateLimiter implements RateLimiter {

    private final RateLimitRule rule;
    private final Clock clock;
    private final ConcurrentHashMap<String, SlidingWindowState> windows = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(RateLimitRule rule, Clock clock) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SlidingWindowRateLimiter(RateLimitRule rule) {
        this(rule, Clock.systemUTC());
    }

    @Override
    public RateLimitResult tryAcquire(String key) {
        Objects.requireNonNull(key, "key");

        long windowMillis = rule.getWindowMillis();
        int maxRequests = rule.getMaxRequests();

        // Holder for the computed effective count (used outside compute lambda)
        double[] effectiveCount = new double[1];

        SlidingWindowState state = windows.compute(key, (k, existing) -> {
            long now = clock.millis();
            long currentWindowStart = now - (now % windowMillis);

            if (existing == null) {
                // First request ever for this key
                effectiveCount[0] = 1;
                return new SlidingWindowState(currentWindowStart, 1, 0, 0);
            }

            // Advance windows if needed
            if (currentWindowStart != existing.currWindowStart) {
                if (currentWindowStart - existing.currWindowStart >= 2 * windowMillis) {
                    // More than two windows have passed — everything is stale
                    effectiveCount[0] = 1;
                    return new SlidingWindowState(currentWindowStart, 1, 0, 0);
                }
                // Previous window becomes the old current
                existing.prevCount = existing.currCount;
                existing.prevWindowStart = existing.currWindowStart;
                existing.currWindowStart = currentWindowStart;
                existing.currCount = 0;
            }

            // Calculate weighted count BEFORE adding the new request
            long elapsed = now - existing.currWindowStart;
            double overlapRatio = (windowMillis - elapsed) / (double) windowMillis;
            double weighted = existing.prevCount * overlapRatio + existing.currCount;

            if (weighted + 1 > maxRequests) {
                // Would exceed — do NOT increment the counter
                effectiveCount[0] = weighted + 1;
                return existing;
            }

            // Allowed — increment
            existing.currCount++;
            effectiveCount[0] = weighted + 1;
            return existing;
        });

        if (effectiveCount[0] <= maxRequests) {
            int remaining = (int) (maxRequests - effectiveCount[0]);
            return RateLimitResult.allowed(remaining);
        }

        // Denied — estimate retry time as remaining time in current window
        long now = clock.millis();
        long currentWindowStart = now - (now % windowMillis);
        long retryAfter = Math.max(0, currentWindowStart + windowMillis - now);
        return RateLimitResult.denied(retryAfter);
    }

    /**
     * Mutable state tracking current and previous window counts.
     * Only mutated inside compute(), which is atomic per-key.
     */
    private static class SlidingWindowState {
        long currWindowStart;
        int currCount;
        long prevWindowStart;
        int prevCount;

        SlidingWindowState(long currWindowStart, int currCount,
                           long prevWindowStart, int prevCount) {
            this.currWindowStart = currWindowStart;
            this.currCount = currCount;
            this.prevWindowStart = prevWindowStart;
            this.prevCount = prevCount;
        }
    }
}
