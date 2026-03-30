package com.example.moviebooking;

public class NotificationService implements BookingObserver {

    @Override
    public void onBookingConfirmed(Booking booking) {
        System.out.println("  [NOTIFICATION] Booking confirmed for " + booking.getUser().getName() +
                " | Movie: " + booking.getShow().getMovie().getTitle() +
                " | Seats: " + booking.getSeats().size() +
                " | Amount: Rs." + String.format("%.2f", booking.getTotalAmount()) +
                " | Email sent to: " + booking.getUser().getEmail());
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        System.out.println("  [NOTIFICATION] Booking cancelled for " + booking.getUser().getName() +
                " | Booking ID: " + booking.getId() +
                " | Refund of Rs." + String.format("%.2f", booking.getTotalAmount()) +
                " initiated to " + booking.getPayment().getMethod() +
                " | Email sent to: " + booking.getUser().getEmail());
    }
}
