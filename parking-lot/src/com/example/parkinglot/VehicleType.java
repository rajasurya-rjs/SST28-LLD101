package com.example.parkinglot;

import java.util.List;

public enum VehicleType {
    TWO_WHEELER(List.of(SlotType.SMALL, SlotType.MEDIUM, SlotType.LARGE)),
    CAR(List.of(SlotType.MEDIUM, SlotType.LARGE)),
    BUS(List.of(SlotType.LARGE));

    private final List<SlotType> compatibleSlotTypes;

    VehicleType(List<SlotType> compatibleSlotTypes) {
        this.compatibleSlotTypes = compatibleSlotTypes;
    }

    public List<SlotType> getCompatibleSlotTypes() {
        return compatibleSlotTypes;
    }

    public boolean canFitIn(SlotType slotType) {
        return compatibleSlotTypes.contains(slotType);
    }
}
