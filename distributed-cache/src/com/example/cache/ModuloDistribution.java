package com.example.cache;

public class ModuloDistribution implements DistributionStrategy {

    @Override
    public int getNode(String key, int totalNodes) {
        return Math.abs(key.hashCode()) % totalNodes;
    }
}
