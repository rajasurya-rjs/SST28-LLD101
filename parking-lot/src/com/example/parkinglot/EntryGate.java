package com.example.parkinglot;

import java.util.Objects;

public class EntryGate {
    private final String id;

    public EntryGate(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String getId() { return id; }

    @Override
    public String toString() {
        return "Gate(" + id + ")";
    }
}
