package com.example.moviebooking;

public class ShowSeat {
    private final Seat seat;
    private SeatStatus status;
    private double finalPrice;

    public ShowSeat(Seat seat, double finalPrice) {
        this.seat = seat;
        this.status = SeatStatus.AVAILABLE;
        this.finalPrice = finalPrice;
    }

    public Seat getSeat() { return seat; }
    public SeatStatus getStatus() { return status; }
    public double getFinalPrice() { return finalPrice; }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public void hold() {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat " + seat.getId() + " is not available, current status: " + status);
        }
        status = SeatStatus.HELD;
    }

    public void book() {
        if (status != SeatStatus.HELD) {
            throw new IllegalStateException("Seat " + seat.getId() + " is not held, current status: " + status);
        }
        status = SeatStatus.BOOKED;
    }

    public void release() {
        if (status == SeatStatus.AVAILABLE) {
            return;
        }
        status = SeatStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return seat.getId() + "[" + status + ", Rs." + String.format("%.2f", finalPrice) + "]";
    }
}
