package com.example.moviebooking;

import java.util.Objects;

public class Seat {
    private final String id;
    private final int row;
    private final int col;
    private final SeatCategory category;

    public Seat(String id, int row, int col, SeatCategory category) {
        this.id = Objects.requireNonNull(id);
        this.row = row;
        this.col = col;
        this.category = Objects.requireNonNull(category);
    }

    public String getId() { return id; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public SeatCategory getCategory() { return category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat seat)) return false;
        return id.equals(seat.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return id + "(" + category + ")";
    }
}
