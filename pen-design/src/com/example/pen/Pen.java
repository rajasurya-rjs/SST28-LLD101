package com.example.pen;

import java.util.Objects;

public class Pen implements Refillable {

    private final WriteStrategy writeStrategy;
    private final MechanismStrategy mechanismStrategy;
    private String color;
    private int inkLevel;
    private boolean started;

    public Pen(WriteStrategy writeStrategy, MechanismStrategy mechanismStrategy, String color) {
        this.writeStrategy = Objects.requireNonNull(writeStrategy, "writeStrategy");
        this.mechanismStrategy = Objects.requireNonNull(mechanismStrategy, "mechanismStrategy");
        this.color = Objects.requireNonNull(color, "color");
        this.inkLevel = 100;
        this.started = false;
    }

    @Override
    public void start() {
        mechanismStrategy.open();
        this.started = true;
    }

    @Override
    public void close() {
        mechanismStrategy.shut();
        this.started = false;
    }

    @Override
    public void write(String text) {
        if (!started) {
            throw new IllegalStateException("Pen not started. Call start() first.");
        }
        if (inkLevel <= 0) {
            throw new IllegalStateException("Out of ink. Call refill() first.");
        }
        writeStrategy.apply(text, color, inkLevel);
        inkLevel = Math.max(0, inkLevel - text.length());
    }

    @Override
    public void refill(String color) {
        this.color = Objects.requireNonNull(color, "color");
        this.inkLevel = 100;
        System.out.println("Refilled with " + color + " ink. Ink level: 100%");
    }

    public String getColor() {
        return color;
    }

    public int getInkLevel() {
        return inkLevel;
    }
}
