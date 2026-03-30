package com.example.moviebooking;

public class PaymentStrategyFactory {

    private PaymentStrategyFactory() {}

    public static PaymentStrategy getStrategy(PaymentMethod method) {
        return switch (method) {
            case UPI -> new UpiPaymentStrategy();
            case CARD -> new CardPaymentStrategy();
            case NET_BANKING -> new NetBankingPaymentStrategy();
        };
    }
}
