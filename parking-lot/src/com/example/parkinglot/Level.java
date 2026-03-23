package com.example.parkinglot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class Level {
    private final int levelIndex;
    private final List<Slot> slots;
    private final ReentrantLock lock;

    public Level(int levelIndex, List<Slot> slots) {
        if (levelIndex < 0) {
            throw new IllegalArgumentException("levelIndex cannot be negative: " + levelIndex);
        }
        this.levelIndex = levelIndex;
        this.slots = new ArrayList<>(Objects.requireNonNull(slots, "slots"));
        this.lock = new ReentrantLock();
    }

    public int getLevelIndex() { return levelIndex; }

    /**
     * Thread-safe: finds the nearest unoccupied slot of the given type
     * for the given gate, marks it occupied, and returns it.
     * Returns null if no matching slot is available on this level.
     */
    public Slot allocateNearest(SlotType slotType, String gateId) {
        lock.lock();
        try {
            Slot nearest = null;
            int minDist = Integer.MAX_VALUE;
            for (Slot slot : slots) {
                if (slot.getSlotType() == slotType
                        && !slot.isOccupied()
                        && slot.getDistanceFrom(gateId) < minDist) {
                    minDist = slot.getDistanceFrom(gateId);
                    nearest = slot;
                }
            }
            if (nearest != null) {
                nearest.markOccupied();
            }
            return nearest;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Thread-safe: marks the given slot as free.
     */
    public void free(Slot slot) {
        lock.lock();
        try {
            slot.markFree();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns counts: [available, total] for the given slot type on this level.
     */
    public int[] getAvailability(SlotType slotType) {
        lock.lock();
        try {
            int total = 0;
            int available = 0;
            for (Slot slot : slots) {
                if (slot.getSlotType() == slotType) {
                    total++;
                    if (!slot.isOccupied()) {
                        available++;
                    }
                }
            }
            return new int[]{available, total};
        } finally {
            lock.unlock();
        }
    }

    public List<Slot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    @Override
    public String toString() {
        return "Level{" + levelIndex + ", slots=" + slots.size() + "}";
    }
}
