package com.example.cache;

public interface DistributionStrategy {
    int getNode(String key, int totalNodes);
}
