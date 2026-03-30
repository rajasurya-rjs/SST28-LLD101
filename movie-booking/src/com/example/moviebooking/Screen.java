package com.example.moviebooking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Screen {
    private final String id;
    private final String name;
    private final String theaterId;
    private final List<Seat> seats;

    public Screen(String id, String name, String theaterId, List<Seat> seats) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.theaterId = Objects.requireNonNull(theaterId);
        this.seats = Collections.unmodifiableList(new ArrayList<>(seats));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTheaterId() { return theaterId; }
    public List<Seat> getSeats() { return seats; }

    @Override
    public String toString() {
        return "Screen{" + name + ", seats=" + seats.size() + "}";
    }
}
