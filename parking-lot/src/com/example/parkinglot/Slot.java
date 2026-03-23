package com.example.parkinglot;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class Slot {
    private final String id;
    private final SlotType slotType;
    private final int levelIndex;
    private final Map<String, Integer> distanceFromGates;
    private boolean occupied;

    public Slot(String id, SlotType slotType, int levelIndex,
                Map<String, Integer> distanceFromGates) {
        this.id = Objects.requireNonNull(id, "id");
        this.slotType = Objects.requireNonNull(slotType, "slotType");
        if (levelIndex < 0) {
            throw new IllegalArgumentException("levelIndex cannot be negative: " + levelIndex);
        }
        this.levelIndex = levelIndex;
        this.distanceFromGates = Collections.unmodifiableMap(
                Map.copyOf(Objects.requireNonNull(distanceFromGates, "distanceFromGates")));
        this.occupied = false;
    }

    public String getId() { return id; }
    public SlotType getSlotType() { return slotType; }
    public int getLevelIndex() { return levelIndex; }
    public boolean isOccupied() { return occupied; }

    public void markOccupied() { this.occupied = true; }
    public void markFree() { this.occupied = false; }

    public int getDistanceFrom(String gateId) {
        Integer dist = distanceFromGates.get(gateId);
        if (dist == null) {
            throw new IllegalArgumentException("Unknown gate: " + gateId);
        }
        return dist;
    }

    @Override
    public String toString() {
        return "Slot{" + id + ", " + slotType + ", L" + levelIndex
                + ", occupied=" + occupied + "}";
    }
}
