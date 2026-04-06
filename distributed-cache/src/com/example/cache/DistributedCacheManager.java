package com.example.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DistributedCacheManager<K, V> {
    private final List<CacheNode<K, V>> nodes;
    private final DistributionStrategy distributionStrategy;
    private final Database<K, V> database;

    public DistributedCacheManager(int numNodes, int capacityPerNode,
                                   DistributionStrategy distributionStrategy,
                                   String evictionPolicyType,
                                   Database<K, V> database) {
        if (numNodes <= 0) {
            throw new IllegalArgumentException("numNodes must be positive");
        }
        if (capacityPerNode <= 0) {
            throw new IllegalArgumentException("capacityPerNode must be positive");
        }
        this.distributionStrategy = Objects.requireNonNull(distributionStrategy, "distributionStrategy");
        this.database = Objects.requireNonNull(database, "database");

        List<CacheNode<K, V>> nodeList = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            EvictionPolicy<K, V> policy = EvictionPolicyFactory.create(evictionPolicyType, capacityPerNode);
            nodeList.add(new CacheNode<>("Node-" + i, policy));
        }
        this.nodes = Collections.unmodifiableList(nodeList);
    }

    public V get(K key) {
        int nodeIndex = distributionStrategy.getNode(key.toString(), nodes.size());
        CacheNode<K, V> node = nodes.get(nodeIndex);

        if (node.containsKey(key)) {
            V value = node.get(key);
            System.out.println("[CACHE HIT] Key: " + key + " from " + node.getNodeId());
            return value;
        }

        V value = database.fetch(key);
        if (value != null) {
            node.put(key, value);
            System.out.println("[CACHE MISS] Key: " + key + " fetched from DB → stored in " + node.getNodeId());
        } else {
            System.out.println("[CACHE MISS] Key: " + key + " not found in DB");
        }
        return value;
    }

    public void put(K key, V value) {
        int nodeIndex = distributionStrategy.getNode(key.toString(), nodes.size());
        CacheNode<K, V> node = nodes.get(nodeIndex);
        node.put(key, value);
        database.store(key, value);
        System.out.println("[PUT] Key: " + key + " → " + node.getNodeId());
    }

    public void printStats() {
        System.out.println("\n--- Cache Stats ---");
        for (CacheNode<K, V> node : nodes) {
            System.out.println(node.getNodeId() + ": " + node.size() + " entries");
        }
        System.out.println("-------------------\n");
    }
}
