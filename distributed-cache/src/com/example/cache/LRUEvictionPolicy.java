package com.example.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUEvictionPolicy<K, V> implements EvictionPolicy<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> accessOrderMap;

    public LRUEvictionPolicy(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.accessOrderMap = new LinkedHashMap<>(capacity, 0.75f, true);
    }

    @Override
    public void onAccess(K key) {
        accessOrderMap.get(key);
    }

    @Override
    public void onInsert(K key, V value) {
        accessOrderMap.put(key, value);
    }

    @Override
    public K evict() {
        Iterator<Map.Entry<K, V>> iterator = accessOrderMap.entrySet().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("Cannot evict from empty cache");
        }
        Map.Entry<K, V> eldest = iterator.next();
        iterator.remove();
        return eldest.getKey();
    }

    @Override
    public void onRemove(K key) {
        accessOrderMap.remove(key);
    }

    @Override
    public boolean isFull() {
        return accessOrderMap.size() >= capacity;
    }
}
