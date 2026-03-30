package com.example.moviebooking;

import java.util.UUID;

public class UpiPaymentStrategy implements PaymentStrategy {

    @Override
    public Payment pay(String bookingId, double amount) {
        Payment payment = new Payment(UUID.randomUUID().toString(), bookingId, amount, PaymentMethod.UPI);
        System.out.println("  [UPI] Processing payment of Rs." + String.format("%.2f", amount));
        payment.markSuccess();
        System.out.println("  [UPI] Payment successful!");
        return payment;
    }

    @Override
    public Payment refund(Payment originalPayment) {
        System.out.println("  [UPI] Processing refund of Rs." + String.format("%.2f", originalPayment.getAmount()));
        originalPayment.markRefunded();
        System.out.println("  [UPI] Refund processed successfully!");
        return originalPayment;
    }
}
