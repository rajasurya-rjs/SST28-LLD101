package com.example.ratelimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the Pluggable Rate Limiting System.
 *
 * Scenarios:
 *   1. Basic usage — tenant T1 allowed 5 external calls/minute (Fixed Window)
 *   2. Algorithm swap — same scenario with Sliding Window, no business logic changes
 *   3. Key resolver — pluggable key construction from request context
 *   4. Multi-threaded burst — concurrent threads competing for quota
 */
public class App {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Pluggable Rate Limiting System Demo ===\n");

        demoBasicFixedWindow();
        demoAlgorithmSwap();
        demoKeyResolver();
        demoMultiThreadedBurst();

        System.out.println("\n=== Demo Complete ===");
    }

    // -----------------------------------------------------------------------
    // Scenario 1: Basic Fixed Window — T1 gets 5 calls/min
    // -----------------------------------------------------------------------
    private static void demoBasicFixedWindow() {
        System.out.println("--- Scenario 1: Fixed Window (5 requests/minute) ---");

        RateLimitRule rule = new RateLimitRule(5, Duration.ofMinutes(1));
        RateLimiterRegistry registry = new RateLimiterRegistry();
        registry.register("payment-api", RateLimiterFactory.fixedWindow(rule));

        String tenantId = "T1";

        for (int i = 1; i <= 8; i++) {
            boolean needsExternalCall = simulateBusinessLogic(i);

            if (!needsExternalCall) {
                System.out.printf("  Request %d: No external call needed (served from cache)%n", i);
                continue;
            }

            RateLimitResult result = registry.tryAcquire("payment-api", tenantId);
            if (result.isAllowed()) {
                System.out.printf("  Request %d: %s -> External call made%n", i, result);
            } else {
                System.out.printf("  Request %d: %s -> Request rejected%n", i, result);
            }
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Scenario 2: Swap to Sliding Window — zero changes in calling code
    // -----------------------------------------------------------------------
    private static void demoAlgorithmSwap() {
        System.out.println("--- Scenario 2: Sliding Window (same 5 requests/minute) ---");

        RateLimitRule rule = new RateLimitRule(5, Duration.ofMinutes(1));
        RateLimiterRegistry registry = new RateLimiterRegistry();

        // Only this line changes — the rest of the code is identical
        registry.register("payment-api", RateLimiterFactory.slidingWindow(rule));

        String tenantId = "T1";

        for (int i = 1; i <= 8; i++) {
            boolean needsExternalCall = simulateBusinessLogic(i);

            if (!needsExternalCall) {
                System.out.printf("  Request %d: No external call needed (served from cache)%n", i);
                continue;
            }

            RateLimitResult result = registry.tryAcquire("payment-api", tenantId);
            if (result.isAllowed()) {
                System.out.printf("  Request %d: %s -> External call made%n", i, result);
            } else {
                System.out.printf("  Request %d: %s -> Request rejected%n", i, result);
            }
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Scenario 3: Pluggable Key Resolver
    // -----------------------------------------------------------------------
    private static void demoKeyResolver() {
        System.out.println("--- Scenario 3: Pluggable Key Resolver ---");

        RateLimitRule rule = new RateLimitRule(3, Duration.ofMinutes(1));
        RateLimiterRegistry registry = new RateLimiterRegistry();
        registry.register("sms-api", RateLimiterFactory.fixedWindow(rule));

        // Per-tenant key resolver
        RateLimitKeyResolver perTenant = ctx -> ctx.get("tenantId");

        // Composite key resolver: tenant + provider
        RateLimitKeyResolver composite = ctx -> ctx.get("tenantId") + ":" + ctx.get("provider");

        Map<String, String> context1 = Map.of("tenantId", "T1", "provider", "twilio");
        Map<String, String> context2 = Map.of("tenantId", "T1", "provider", "vonage");

        System.out.println("  Using per-tenant key resolver:");
        for (int i = 1; i <= 4; i++) {
            String key = perTenant.resolve(context1);
            RateLimitResult result = registry.tryAcquire("sms-api", key);
            System.out.printf("    Call %d (key=%s): %s%n", i, key, result);
        }

        // Re-register with fresh limiter for composite demo
        registry.register("sms-api", RateLimiterFactory.fixedWindow(rule));

        System.out.println("  Using composite key resolver (tenant:provider):");
        for (int i = 1; i <= 4; i++) {
            // Alternate between two providers — each gets its own quota
            Map<String, String> ctx = (i % 2 == 1) ? context1 : context2;
            String key = composite.resolve(ctx);
            RateLimitResult result = registry.tryAcquire("sms-api", key);
            System.out.printf("    Call %d (key=%s): %s%n", i, key, result);
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Scenario 4: Multi-threaded burst
    // -----------------------------------------------------------------------
    private static void demoMultiThreadedBurst() throws InterruptedException {
        System.out.println("--- Scenario 4: Multi-threaded Burst (10 threads, limit 5) ---");

        RateLimitRule rule = new RateLimitRule(5, Duration.ofMinutes(1));
        RateLimiterRegistry registry = new RateLimiterRegistry();
        registry.register("ai-api", RateLimiterFactory.fixedWindow(rule));

        int threadCount = 10;
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger denied = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i + 1;
            executor.submit(() -> {
                try {
                    RateLimitResult result = registry.tryAcquire("ai-api", "T1");
                    if (result.isAllowed()) {
                        allowed.incrementAndGet();
                        System.out.printf("  Thread %2d: ALLOWED%n", threadId);
                    } else {
                        denied.incrementAndGet();
                        System.out.printf("  Thread %2d: DENIED%n", threadId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.printf("  Result: %d allowed, %d denied (limit was 5)%n",
                allowed.get(), denied.get());
        System.out.println();
    }

    /**
     * Simulates business logic that determines whether an external call is needed.
     * In this demo, every 3rd request is served from cache (no external call).
     */
    private static boolean simulateBusinessLogic(int requestNumber) {
        return requestNumber % 3 != 0; // Every 3rd request is cached
    }
}
