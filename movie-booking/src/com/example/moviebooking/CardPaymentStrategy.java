package com.example.moviebooking;

import java.util.UUID;

public class CardPaymentStrategy implements PaymentStrategy {

    @Override
    public Payment pay(String bookingId, double amount) {
        Payment payment = new Payment(UUID.randomUUID().toString(), bookingId, amount, PaymentMethod.CARD);
        System.out.println("  [CARD] Processing payment of Rs." + String.format("%.2f", amount));
        payment.markSuccess();
        System.out.println("  [CARD] Payment successful!");
        return payment;
    }

    @Override
    public Payment refund(Payment originalPayment) {
        System.out.println("  [CARD] Processing refund of Rs." + String.format("%.2f", originalPayment.getAmount()));
        originalPayment.markRefunded();
        System.out.println("  [CARD] Refund processed successfully!");
        return originalPayment;
    }
}
