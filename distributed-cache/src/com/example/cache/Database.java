package com.example.cache;

public interface Database<K, V> {
    V fetch(K key);
    void store(K key, V value);
}
