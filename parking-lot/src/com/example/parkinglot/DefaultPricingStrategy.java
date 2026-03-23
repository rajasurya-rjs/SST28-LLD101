package com.example.parkinglot;

public class DefaultPricingStrategy implements PricingStrategy {

    private static final double SMALL_RATE  = 10.0;
    private static final double MEDIUM_RATE = 20.0;
    private static final double LARGE_RATE  = 30.0;

    @Override
    public double calculatePrice(SlotType slotType, long durationHours) {
        if (durationHours < 1) durationHours = 1;

        return switch (slotType) {
            case SMALL  -> SMALL_RATE  * durationHours;
            case MEDIUM -> MEDIUM_RATE * durationHours;
            case LARGE  -> LARGE_RATE  * durationHours;
        };
    }

    @Override
    public String toString() {
        return "DefaultPricing{SMALL=" + SMALL_RATE
                + "/hr, MEDIUM=" + MEDIUM_RATE
                + "/hr, LARGE=" + LARGE_RATE + "/hr}";
    }
}
