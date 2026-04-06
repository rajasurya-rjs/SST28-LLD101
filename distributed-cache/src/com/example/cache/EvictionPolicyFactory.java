package com.example.cache;

import java.util.Objects;

public final class EvictionPolicyFactory {

    private EvictionPolicyFactory() {}

    public static <K, V> EvictionPolicy<K, V> create(String type, int capacity) {
        Objects.requireNonNull(type, "type");
        switch (type.toLowerCase()) {
            case "lru":
                return new LRUEvictionPolicy<>(capacity);
            default:
                throw new IllegalArgumentException("Unknown eviction policy: " + type);
        }
    }
}
