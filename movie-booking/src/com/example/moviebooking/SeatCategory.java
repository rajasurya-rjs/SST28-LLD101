package com.example.moviebooking;

public enum SeatCategory {
    SILVER(200.0),
    GOLD(500.0),
    DIAMOND(800.0);

    private final double basePrice;

    SeatCategory(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getBasePrice() {
        return basePrice;
    }
}
