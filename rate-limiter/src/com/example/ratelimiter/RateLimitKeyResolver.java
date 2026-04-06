package com.example.ratelimiter;

import java.util.Map;

/**
 * Functional interface for building rate-limit keys from request context.
 *
 * This allows flexible key strategies without modifying the core RateLimiter:
 *   - Per tenant:   ctx -> ctx.get("tenantId")
 *   - Per API key:  ctx -> ctx.get("apiKey")
 *   - Composite:    ctx -> ctx.get("tenantId") + ":" + ctx.get("provider")
 */
@FunctionalInterface
public interface RateLimitKeyResolver {

    /**
     * Resolves a rate-limiting key from the given context map.
     *
     * @param context a map of contextual attributes (e.g., tenantId, apiKey, provider)
     * @return the resolved key string used for rate limiting
     */
    String resolve(Map<String, String> context);
}
