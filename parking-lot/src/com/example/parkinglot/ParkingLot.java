package com.example.parkinglot;

import java.util.*;

public class ParkingLot {

    private static boolean instanceCreated = false;

    private List<Level> levels;
    private List<EntryGate> entryGates;
    private PricingStrategy pricingStrategy;

    // ── Singleton (Bill Pugh Holder) ──────────────────────────────

    private ParkingLot() {
        if (instanceCreated) {
            throw new RuntimeException("Use getInstance(). Only one ParkingLot allowed.");
        }
        instanceCreated = true;
    }

    private static class Holder {
        private static final ParkingLot INSTANCE = new ParkingLot();
    }

    public static ParkingLot getInstance() {
        return Holder.INSTANCE;
    }

    // ── Setters (configure before use) ───────────────────────────

    public void setLevels(List<Level> levels) {
        this.levels = new ArrayList<>(Objects.requireNonNull(levels, "levels"));
    }

    public void setEntryGates(List<EntryGate> entryGates) {
        this.entryGates = new ArrayList<>(Objects.requireNonNull(entryGates, "entryGates"));
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = Objects.requireNonNull(pricingStrategy, "pricingStrategy");
    }

    // ── Core APIs ────────────────────────────────────────────────

    /**
     * Parks a vehicle and returns a ticket.
     *
     * 1. Validates that requestedSlotType is compatible with the vehicle.
     * 2. Builds a search order: requested type first, then larger compatible types.
     * 3. For each type, iterates levels (0 first) and allocates nearest slot.
     * 4. If no slot found anywhere, throws IllegalStateException.
     */
    public ParkingTicket park(Vehicle vehicle, long entryTime,
                              SlotType requestedSlotType, String entryGateId) {
        Objects.requireNonNull(vehicle, "vehicle");
        Objects.requireNonNull(requestedSlotType, "requestedSlotType");
        Objects.requireNonNull(entryGateId, "entryGateId");

        if (!vehicle.getType().canFitIn(requestedSlotType)) {
            throw new IllegalArgumentException(
                    vehicle.getType() + " cannot fit in " + requestedSlotType + " slot");
        }

        // Build search order: requested type first, then larger compatible types
        List<SlotType> searchOrder = new ArrayList<>();
        searchOrder.add(requestedSlotType);
        for (SlotType compatible : vehicle.getType().getCompatibleSlotTypes()) {
            if (compatible != requestedSlotType
                    && compatible.ordinal() > requestedSlotType.ordinal()) {
                searchOrder.add(compatible);
            }
        }

        // Try each slot type in order, each level in order
        for (SlotType slotType : searchOrder) {
            for (Level level : levels) {
                Slot slot = level.allocateNearest(slotType, entryGateId);
                if (slot != null) {
                    ParkingTicket ticket = new ParkingTicket(vehicle, slot, entryTime);
                    System.out.println("  [PARK] " + vehicle + " -> " + slot.getId()
                            + " (" + slot.getSlotType() + ", L" + level.getLevelIndex()
                            + ") via " + entryGateId);
                    return ticket;
                }
            }
        }

        throw new IllegalStateException(
                "No available slot for " + vehicle + " (requested: " + requestedSlotType + ")");
    }

    /**
     * Processes vehicle exit and returns the bill amount.
     * Billing is based on the ALLOCATED slot type, not the vehicle type.
     */
    public double exit(ParkingTicket ticket, long exitTime) {
        Objects.requireNonNull(ticket, "ticket");

        long durationMillis = exitTime - ticket.getEntryTime();
        long durationHours = Math.max(1, (long) Math.ceil(durationMillis / 3_600_000.0));

        double amount = pricingStrategy.calculatePrice(
                ticket.getAllocatedSlotType(), durationHours);

        // Free the slot
        Slot slot = ticket.getSlotAssigned();
        for (Level level : levels) {
            if (level.getLevelIndex() == slot.getLevelIndex()) {
                level.free(slot);
                break;
            }
        }

        System.out.println("  [EXIT] " + ticket.getVehicle() + " | slot=" + slot.getId()
                + " | " + durationHours + "hr(s) | billed as "
                + ticket.getAllocatedSlotType() + " | amount=" + amount);

        return amount;
    }

    /**
     * Returns current availability: SlotType -> [available, total].
     */
    public Map<SlotType, int[]> status() {
        Map<SlotType, int[]> result = new LinkedHashMap<>();
        for (SlotType type : SlotType.values()) {
            result.put(type, new int[]{0, 0});
        }
        for (Level level : levels) {
            for (SlotType type : SlotType.values()) {
                int[] levelCounts = level.getAvailability(type);
                int[] total = result.get(type);
                total[0] += levelCounts[0];
                total[1] += levelCounts[1];
            }
        }
        return result;
    }

    // ── Getters ──────────────────────────────────────────────────

    public List<Level> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    public List<EntryGate> getEntryGates() {
        return Collections.unmodifiableList(entryGates);
    }

    public PricingStrategy getPricingStrategy() {
        return pricingStrategy;
    }
}
