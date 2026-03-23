package com.example.parkinglot;

import java.util.Objects;

public class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = Objects.requireNonNull(licensePlate, "licensePlate");
        this.type = Objects.requireNonNull(type, "type");
    }

    public String getLicensePlate() { return licensePlate; }
    public VehicleType getType() { return type; }

    @Override
    public String toString() {
        return type + "(" + licensePlate + ")";
    }
}
