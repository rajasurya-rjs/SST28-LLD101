package com.example.moviebooking;

import java.util.List;

public class PricingStrategyFactory {

    private PricingStrategyFactory() {}

    public static List<PricingRule> defaultRules() {
        return List.of(
            new ShowTimingPricingRule(),
            new DayOfWeekPricingRule(),
            new WeekOfMonthPricingRule()
        );
    }

    public static List<PricingRule> defaultRulesWithDemand() {
        return List.of(
            new ShowTimingPricingRule(),
            new DayOfWeekPricingRule(),
            new WeekOfMonthPricingRule(),
            new DemandPricingRule(0.7, 1.5)
        );
    }

    public static double computePrice(Seat seat, Show show, List<PricingRule> rules) {
        double price = seat.getCategory().getBasePrice();
        for (PricingRule rule : rules) {
            price = rule.apply(price, show, seat);
        }
        return Math.round(price * 100.0) / 100.0;
    }
}
