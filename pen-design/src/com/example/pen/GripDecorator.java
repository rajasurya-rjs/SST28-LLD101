package com.example.pen;

import java.util.Objects;

public class GripDecorator implements Writable {

    private final Writable delegate;

    public GripDecorator(Writable delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void write(String text) {
        System.out.println("Grip engaged: stabilizing hand");
        delegate.write(text);
        System.out.println("Grip released");
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
