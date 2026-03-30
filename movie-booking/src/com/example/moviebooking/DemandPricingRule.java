package com.example.moviebooking;

public class DemandPricingRule implements PricingRule {
    private final double highDemandThreshold;
    private final double multiplier;

    public DemandPricingRule(double highDemandThreshold, double multiplier) {
        this.highDemandThreshold = highDemandThreshold;
        this.multiplier = multiplier;
    }

    @Override
    public double apply(double currentPrice, Show show, Seat seat) {
        int totalSeats = show.getTotalSeats();
        if (totalSeats == 0) return currentPrice;

        double occupancy = (double) show.getBookedSeatCount() / totalSeats;
        if (occupancy >= highDemandThreshold) {
            return currentPrice * multiplier;
        }
        return currentPrice;
    }
}
