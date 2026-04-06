package com.example.cache;

public interface EvictionPolicy<K, V> {
    void onAccess(K key);
    void onInsert(K key, V value);
    K evict();
    void onRemove(K key);
    boolean isFull();
}
