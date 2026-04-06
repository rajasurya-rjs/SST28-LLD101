package com.example.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CacheNode<K, V> {
    private final String nodeId;
    private final Map<K, V> store;
    private final EvictionPolicy<K, V> evictionPolicy;

    public CacheNode(String nodeId, EvictionPolicy<K, V> evictionPolicy) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.evictionPolicy = Objects.requireNonNull(evictionPolicy, "evictionPolicy");
        this.store = new HashMap<>();
    }

    public V get(K key) {
        if (store.containsKey(key)) {
            evictionPolicy.onAccess(key);
            return store.get(key);
        }
        return null;
    }

    public void put(K key, V value) {
        if (evictionPolicy.isFull() && !store.containsKey(key)) {
            K evictedKey = evictionPolicy.evict();
            store.remove(evictedKey);
            System.out.println("[" + nodeId + "] Evicted key: " + evictedKey);
        }
        store.put(key, value);
        evictionPolicy.onInsert(key, value);
    }

    public boolean containsKey(K key) {
        return store.containsKey(key);
    }

    public int size() {
        return store.size();
    }

    public String getNodeId() {
        return nodeId;
    }
}
