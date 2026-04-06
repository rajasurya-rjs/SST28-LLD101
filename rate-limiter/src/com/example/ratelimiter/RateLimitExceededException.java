package com.example.ratelimiter;

/**
 * Thrown when an external call is denied due to rate limit exhaustion.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterMillis;

    public RateLimitExceededException(String message, long retryAfterMillis) {
        super(message);
        this.retryAfterMillis = retryAfterMillis;
    }

    public RateLimitExceededException(String message) {
        this(message, 0);
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
