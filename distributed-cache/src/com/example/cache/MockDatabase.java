package com.example.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MockDatabase<K, V> implements Database<K, V> {
    private final Map<K, V> data;

    public MockDatabase() {
        this.data = new HashMap<>();
    }

    public MockDatabase(Map<K, V> initialData) {
        Objects.requireNonNull(initialData, "initialData");
        this.data = new HashMap<>(initialData);
    }

    @Override
    public V fetch(K key) {
        V value = data.get(key);
        System.out.println("[DB] Fetching key: " + key + " → " + value);
        return value;
    }

    @Override
    public void store(K key, V value) {
        data.put(key, value);
        System.out.println("[DB] Stored key: " + key + " → " + value);
    }
}
