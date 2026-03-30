package com.example.moviebooking;

public interface PaymentStrategy {
    Payment pay(String bookingId, double amount);
    Payment refund(Payment originalPayment);
}
