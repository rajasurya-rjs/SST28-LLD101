package com.example.moviebooking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class Booking {
    private final String id;
    private final User user;
    private final Show show;
    private final List<ShowSeat> seats;
    private double totalAmount;
    private BookingStatus status;
    private Payment payment;
    private final LocalDateTime createdAt;
    private ScheduledFuture<?> holdTimer;

    public Booking(String id, User user, Show show, List<ShowSeat> seats, double totalAmount) {
        this.id = id;
        this.user = user;
        this.show = show;
        this.seats = new ArrayList<>(seats);
        this.totalAmount = totalAmount;
        this.status = BookingStatus.SEATS_HELD;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public User getUser() { return user; }
    public Show getShow() { return show; }
    public List<ShowSeat> getSeats() { return Collections.unmodifiableList(seats); }
    public double getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public Payment getPayment() { return payment; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(BookingStatus status) { this.status = status; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public void setHoldTimer(ScheduledFuture<?> holdTimer) {
        this.holdTimer = holdTimer;
    }

    public void cancelHoldTimer() {
        if (holdTimer != null) {
            holdTimer.cancel(false);
        }
    }

    @Override
    public String toString() {
        return "Booking{id=" + id + ", user=" + user.getName() +
               ", show=" + show.getId() + ", seats=" + seats.size() +
               ", amount=Rs." + String.format("%.2f", totalAmount) +
               ", status=" + status + "}";
    }
}
