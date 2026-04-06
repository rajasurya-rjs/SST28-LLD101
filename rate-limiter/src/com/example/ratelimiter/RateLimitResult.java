package com.example.ratelimiter;

/**
 * Immutable result of a rate limit check, carrying the decision and diagnostics.
 */
public final class RateLimitResult {

    private final boolean allowed;
    private final int remainingRequests;
    private final long retryAfterMillis;

    private RateLimitResult(boolean allowed, int remainingRequests, long retryAfterMillis) {
        this.allowed = allowed;
        this.remainingRequests = remainingRequests;
        this.retryAfterMillis = retryAfterMillis;
    }

    public static RateLimitResult allowed(int remaining) {
        return new RateLimitResult(true, remaining, 0);
    }

    public static RateLimitResult denied(long retryAfterMillis) {
        return new RateLimitResult(false, 0, retryAfterMillis);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public int getRemainingRequests() {
        return remainingRequests;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }

    @Override
    public String toString() {
        if (allowed) {
            return "ALLOWED (remaining: " + remainingRequests + ")";
        }
        return "DENIED (retry after: " + retryAfterMillis + "ms)";
    }
}
