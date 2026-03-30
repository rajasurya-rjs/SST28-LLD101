package com.example.moviebooking;

public class ShowTimingPricingRule implements PricingRule {

    @Override
    public double apply(double currentPrice, Show show, Seat seat) {
        ShowTiming timing = ShowTiming.from(show.getStartTime().toLocalTime());
        double multiplier = switch (timing) {
            case MORNING -> 0.8;
            case AFTERNOON -> 0.9;
            case EVENING -> 1.2;
            case NIGHT -> 1.0;
        };
        return currentPrice * multiplier;
    }
}
