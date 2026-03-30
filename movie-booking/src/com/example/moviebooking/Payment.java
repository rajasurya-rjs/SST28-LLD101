package com.example.moviebooking;

import java.time.LocalDateTime;
import java.util.Objects;

public class Payment {
    private final String id;
    private final String bookingId;
    private final double amount;
    private final PaymentMethod method;
    private PaymentStatus status;
    private final LocalDateTime timestamp;

    public Payment(String id, String bookingId, double amount, PaymentMethod method) {
        this.id = Objects.requireNonNull(id);
        this.bookingId = Objects.requireNonNull(bookingId);
        this.amount = amount;
        this.method = Objects.requireNonNull(method);
        this.status = PaymentStatus.PENDING;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getBookingId() { return bookingId; }
    public double getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void markSuccess() { this.status = PaymentStatus.SUCCESS; }
    public void markFailed() { this.status = PaymentStatus.FAILED; }
    public void markRefunded() { this.status = PaymentStatus.REFUNDED; }

    @Override
    public String toString() {
        return "Payment{" + method + ", Rs." + String.format("%.2f", amount) + ", " + status + "}";
    }
}
