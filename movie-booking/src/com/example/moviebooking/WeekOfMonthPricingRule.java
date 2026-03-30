package com.example.moviebooking;

public class WeekOfMonthPricingRule implements PricingRule {

    @Override
    public double apply(double currentPrice, Show show, Seat seat) {
        int dayOfMonth = show.getStartTime().getDayOfMonth();
        double multiplier;
        if (dayOfMonth <= 7) {
            multiplier = 1.1; // first week — new releases
        } else if (dayOfMonth >= 25) {
            multiplier = 0.9; // last week
        } else {
            multiplier = 1.0; // middle weeks
        }
        return currentPrice * multiplier;
    }
}
