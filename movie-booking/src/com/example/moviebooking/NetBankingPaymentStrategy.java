package com.example.moviebooking;

import java.util.UUID;

public class NetBankingPaymentStrategy implements PaymentStrategy {

    @Override
    public Payment pay(String bookingId, double amount) {
        Payment payment = new Payment(UUID.randomUUID().toString(), bookingId, amount, PaymentMethod.NET_BANKING);
        System.out.println("  [NET_BANKING] Processing payment of Rs." + String.format("%.2f", amount));
        payment.markSuccess();
        System.out.println("  [NET_BANKING] Payment successful!");
        return payment;
    }

    @Override
    public Payment refund(Payment originalPayment) {
        System.out.println("  [NET_BANKING] Processing refund of Rs." + String.format("%.2f", originalPayment.getAmount()));
        originalPayment.markRefunded();
        System.out.println("  [NET_BANKING] Refund processed successfully!");
        return originalPayment;
    }
}
