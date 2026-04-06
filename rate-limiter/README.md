# Pluggable Rate Limiting System for External Resource Usage

## Problem Statement

Design a rate limiting module that guards **external paid API calls** (not incoming client requests).
The rate limiter is consulted only when business logic determines an external call is needed — not
every API request consumes quota.

## Class Diagram

```
+-------------------------+          +-------------------------+
|    <<interface>>        |          |     RateLimitRule       |
|      RateLimiter        |          |  (immutable config)     |
|-------------------------|          |-------------------------|
| + tryAcquire(key):      |          | - maxRequests: int      |
|     RateLimitResult     |          | - window: Duration      |
+----------+--------------+          +-------------------------+
           |
     +-----+------+
     |             |
+----+--------+ +--+------------------+
| FixedWindow | | SlidingWindow       |
| RateLimiter | | RateLimiter         |
|-------------| |---------------------|
| - rule      | | - rule              |
| - clock     | | - clock             |
| - windows:  | | - windows:          |
|  CHM<Key,   | |  CHM<Key,           |
|  WindowState| |  SlidingWindowState> |
|>            | |                     |
+-------------+ +---------------------+

+-------------------------+          +-------------------------+
|   RateLimiterFactory    |          |  RateLimiterRegistry    |
|   (static factory)      |          |  (scope -> limiter map) |
|-------------------------|          |-------------------------|
| + fixedWindow(rule)     |          | - limiters: CHM         |
| + slidingWindow(rule)   |          | + register(scope, rl)   |
+-------------------------+          | + tryAcquire(scope, key) |
                                     | + unregister(scope)     |
+-------------------------+          +-------------------------+
| <<functional interface>>|
| RateLimitKeyResolver    |          +-------------------------+
|-------------------------|          |   RateLimitResult       |
| + resolve(context):     |          |   (value object)        |
|     String              |          |-------------------------|
+-------------------------+          | - allowed: boolean      |
                                     | - remainingRequests     |
+-------------------------+          | - retryAfterMillis      |
| RateLimitExceeded-      |          +-------------------------+
|   Exception             |
|-------------------------|
| - retryAfterMillis      |
+-------------------------+
```

## Design Patterns Used

| Pattern   | Where                        | Why                                              |
|-----------|------------------------------|--------------------------------------------------|
| Strategy  | `RateLimiter` interface      | Swap algorithms without changing callers          |
| Factory   | `RateLimiterFactory`         | Decouple creation from usage                      |
| Registry  | `RateLimiterRegistry`        | Central lookup for scope-based rate limiters       |

## Key Design Decisions

### 1. Strategy Pattern for Algorithm Pluggability
The `RateLimiter` interface is the Strategy. Each algorithm (Fixed Window, Sliding Window) is a
ConcreteStrategy. Adding a new algorithm (Token Bucket, Leaky Bucket, Sliding Log) requires:
1. Create a new class implementing `RateLimiter`
2. Add a factory method in `RateLimiterFactory`
3. **Zero changes** to Registry, callers, or business logic → Open/Closed Principle

### 2. Thread-Safety via ConcurrentHashMap.compute()
Both algorithms use `ConcurrentHashMap.compute(key, fn)` for atomic per-key updates:
- Two threads with **different keys** run fully in parallel (no contention)
- Two threads with the **same key** are serialized by the map (no race conditions)
- No explicit locks (`ReentrantLock`, `synchronized`) needed

### 3. Clock Injection for Testability
Algorithm classes accept `java.time.Clock` via constructor. Tests can inject a fixed or
controllable clock to verify window boundaries without `Thread.sleep()`.

### 4. Scope-based Registry
The `RateLimiterRegistry` maps "scopes" (external resources) to their rate limiters.
Different resources can use different algorithms and limits:
- `"stripe-api"` → Sliding Window, 100/min
- `"twilio-sms"` → Fixed Window, 50/min

### 5. Rate Limiter Guards External Calls, Not Incoming Requests
The rate limiter is **not middleware**. It is consulted only at the point where business
logic has already decided an external call is needed:
```java
if (needsExternalCall) {
    RateLimitResult result = registry.tryAcquire("stripe-api", tenantId);
    if (!result.isAllowed()) throw new RateLimitExceededException(...);
    stripeClient.charge(...);
}
```

## Algorithm Trade-offs

### Fixed Window Counter
```
Window:  |-------- 60s --------|-------- 60s --------|
Counts:  |    count = 5        |    count = 0        |
```
**Pros:** Simple, O(1) time/space, easy to debug
**Cons:** Boundary burst problem — if 5 requests arrive at second 59 and 5 more at second 61,
10 requests pass in 2 seconds despite a 5/minute limit

### Sliding Window Counter (Weighted Approximation)
```
         prev window           curr window
         |--- 60s ---|--- 60s ---|
         prevCount=4  currCount=2
                           ^-- 20s elapsed
         overlapRatio = (60-20)/60 = 0.67
         effectiveCount = 4 * 0.67 + 2 = 4.67
```
**Pros:** Smooths boundary bursts, still O(1) time/space
**Cons:** Approximation (not exact), slightly more complex

### When to Use Which
- **Fixed Window:** When simplicity matters more than precision (internal services, non-critical limits)
- **Sliding Window:** When smoother rate limiting is important (billing, paid APIs, SLAs)

## Build and Run

```bash
cd rate-limiter/src
javac com/example/ratelimiter/*.java
java com.example.ratelimiter.App
```

## Example Output

```
=== Pluggable Rate Limiting System Demo ===

--- Scenario 1: Fixed Window (5 requests/minute) ---
  Request 1: ALLOWED (remaining: 4) -> External call made
  Request 2: ALLOWED (remaining: 3) -> External call made
  Request 3: No external call needed (served from cache)
  Request 4: ALLOWED (remaining: 2) -> External call made
  Request 5: ALLOWED (remaining: 1) -> External call made
  Request 6: No external call needed (served from cache)
  Request 7: ALLOWED (remaining: 0) -> External call made
  Request 8: DENIED (retry after: ...ms) -> Request rejected

--- Scenario 2: Sliding Window (same 5 requests/minute) ---
  [Same pattern — algorithm swapped with one line change]

--- Scenario 3: Pluggable Key Resolver ---
  [Per-tenant and composite key strategies demonstrated]

--- Scenario 4: Multi-threaded Burst (10 threads, limit 5) ---
  [Exactly 5 allowed, 5 denied — thread-safety verified]
```
