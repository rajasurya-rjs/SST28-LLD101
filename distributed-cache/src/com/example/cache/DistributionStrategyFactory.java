package com.example.cache;

import java.util.Objects;

public final class DistributionStrategyFactory {

    private DistributionStrategyFactory() {}

    public static DistributionStrategy create(String type) {
        Objects.requireNonNull(type, "type");
        switch (type.toLowerCase()) {
            case "modulo":
                return new ModuloDistribution();
            default:
                throw new IllegalArgumentException("Unknown distribution strategy: " + type);
        }
    }
}
