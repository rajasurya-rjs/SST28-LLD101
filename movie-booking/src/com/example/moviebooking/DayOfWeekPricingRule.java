package com.example.moviebooking;

public class DayOfWeekPricingRule implements PricingRule {

    @Override
    public double apply(double currentPrice, Show show, Seat seat) {
        DayType dayType = DayType.from(show.getStartTime().toLocalDate());
        double multiplier = switch (dayType) {
            case WEEKEND -> 1.3;
            case WEEKDAY -> 1.0;
        };
        return currentPrice * multiplier;
    }
}
