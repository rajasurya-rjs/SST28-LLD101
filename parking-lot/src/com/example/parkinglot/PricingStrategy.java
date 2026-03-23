package com.example.parkinglot;

public interface PricingStrategy {
    double calculatePrice(SlotType slotType, long durationHours);
}
