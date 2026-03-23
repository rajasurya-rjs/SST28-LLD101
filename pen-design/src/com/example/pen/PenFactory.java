package com.example.pen;

import java.util.Objects;

public final class PenFactory {

    private PenFactory() {
    }

    public static Pen getPen(String type, String color, String mechanism) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(mechanism, "mechanism");

        WriteStrategy writeStrategy = switch (type) {
            case "ink-pen" -> new InkWriteStrategy();
            case "ball-pen" -> new BallWriteStrategy();
            case "gel-pen" -> new GelWriteStrategy();
            default -> throw new IllegalArgumentException("Unknown pen type: " + type);
        };

        MechanismStrategy mechanismStrategy = switch (mechanism) {
            case "with-cap" -> new WithCapMechanism();
            case "click-mechanism" -> new ClickMechanism();
            default -> throw new IllegalArgumentException("Unknown mechanism: " + mechanism);
        };

        return new Pen(writeStrategy, mechanismStrategy, color);
    }
}
