package com.example.cache;

import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        // Setup: Mock database with some pre-loaded data
        Map<String, String> dbData = new HashMap<>();
        dbData.put("user:1", "Alice");
        dbData.put("user:2", "Bob");
        dbData.put("user:3", "Charlie");
        dbData.put("user:4", "Diana");
        dbData.put("user:5", "Eve");
        dbData.put("user:6", "Frank");

        MockDatabase<String, String> database = new MockDatabase<>(dbData);

        // Create distributed cache: 3 nodes, capacity 2 per node, modulo distribution, LRU eviction
        DistributionStrategy strategy = DistributionStrategyFactory.create("modulo");
        DistributedCacheManager<String, String> cache = new DistributedCacheManager<>(
                3, 2, strategy, "lru", database);

        System.out.println("=== 1. Cache Miss (fetches from DB) ===");
        String val1 = cache.get("user:1");
        System.out.println("Result: " + val1);

        System.out.println("\n=== 2. Cache Hit (served from cache) ===");
        String val2 = cache.get("user:1");
        System.out.println("Result: " + val2);

        System.out.println("\n=== 3. Distribution across nodes ===");
        cache.get("user:2");
        cache.get("user:3");
        cache.get("user:4");
        cache.printStats();

        System.out.println("=== 4. LRU Eviction (node capacity = 2) ===");
        cache.get("user:5");
        cache.get("user:6");
        cache.printStats();

        System.out.println("=== 5. Explicit put() ===");
        cache.put("user:7", "Grace");
        String val7 = cache.get("user:7");
        System.out.println("Result: " + val7);

        System.out.println("\n=== 6. Final Stats ===");
        cache.printStats();
    }
}
