package com.example.moviebooking;

public interface PricingRule {
    double apply(double currentPrice, Show show, Seat seat);
}
