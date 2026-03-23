package com.example.pen;

public class Pencil implements Writable {

    private boolean started;

    public Pencil() {
        this.started = false;
    }

    @Override
    public void start() {
        System.out.println("Sharpening pencil");
        this.started = true;
    }

    @Override
    public void close() {
        System.out.println("Putting pencil down");
        this.started = false;
    }

    @Override
    public void write(String text) {
        if (!started) {
            throw new IllegalStateException("Pencil not started. Call start() first.");
        }
        System.out.println("[Pencil] " + text);
    }
}
