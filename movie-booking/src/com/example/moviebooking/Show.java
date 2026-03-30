package com.example.moviebooking;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Show {
    private final String id;
    private final Movie movie;
    private final Screen screen;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Map<String, ShowSeat> seatMap;
    private final ReentrantLock lock;
    private final List<PricingRule> pricingRules;

    private Show(Builder builder) {
        this.id = builder.id;
        this.movie = builder.movie;
        this.screen = builder.screen;
        this.startTime = builder.startTime;
        this.endTime = builder.startTime.plusMinutes(builder.movie.getDurationMinutes());
        this.pricingRules = Collections.unmodifiableList(new ArrayList<>(builder.pricingRules));
        this.lock = new ReentrantLock();

        this.seatMap = new LinkedHashMap<>();
        for (Seat seat : screen.getSeats()) {
            double price = PricingStrategyFactory.computePrice(seat, this, pricingRules);
            seatMap.put(seat.getId(), new ShowSeat(seat, price));
        }
    }

    public String getId() { return id; }
    public Movie getMovie() { return movie; }
    public Screen getScreen() { return screen; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<PricingRule> getPricingRules() { return pricingRules; }

    public Map<String, ShowSeat> getSeatMap() {
        return Collections.unmodifiableMap(seatMap);
    }

    public List<ShowSeat> getAvailableSeats() {
        lock.lock();
        try {
            return seatMap.values().stream()
                    .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                    .collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    public int getTotalSeats() {
        return seatMap.size();
    }

    public int getBookedSeatCount() {
        lock.lock();
        try {
            return (int) seatMap.values().stream()
                    .filter(s -> s.getStatus() != SeatStatus.AVAILABLE)
                    .count();
        } finally {
            lock.unlock();
        }
    }

    public void holdSeats(List<String> seatIds) {
        lock.lock();
        try {
            for (String seatId : seatIds) {
                ShowSeat showSeat = seatMap.get(seatId);
                if (showSeat == null) {
                    throw new IllegalArgumentException("Seat " + seatId + " does not exist in this show");
                }
                if (showSeat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new IllegalStateException("Seat " + seatId + " is not available, current status: " + showSeat.getStatus());
                }
            }
            for (String seatId : seatIds) {
                seatMap.get(seatId).hold();
            }
        } finally {
            lock.unlock();
        }
    }

    public void confirmSeats(List<String> seatIds) {
        lock.lock();
        try {
            for (String seatId : seatIds) {
                seatMap.get(seatId).book();
            }
        } finally {
            lock.unlock();
        }
    }

    public void releaseSeats(List<String> seatIds) {
        lock.lock();
        try {
            for (String seatId : seatIds) {
                ShowSeat showSeat = seatMap.get(seatId);
                if (showSeat != null) {
                    showSeat.release();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "Show{id=" + id + ", movie=" + movie.getTitle() +
               ", screen=" + screen.getName() + ", time=" + startTime + "}";
    }

    public static class Builder {
        private String id;
        private Movie movie;
        private Screen screen;
        private LocalDateTime startTime;
        private List<PricingRule> pricingRules = new ArrayList<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder movie(Movie movie) { this.movie = movie; return this; }
        public Builder screen(Screen screen) { this.screen = screen; return this; }
        public Builder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public Builder pricingRules(List<PricingRule> rules) { this.pricingRules = new ArrayList<>(rules); return this; }

        public Show build() {
            Objects.requireNonNull(id, "Show ID is required");
            Objects.requireNonNull(movie, "Movie is required");
            Objects.requireNonNull(screen, "Screen is required");
            Objects.requireNonNull(startTime, "Start time is required");
            return new Show(this);
        }
    }
}
