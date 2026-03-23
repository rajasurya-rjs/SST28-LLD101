package com.example.snakeladder;

public final class Snake {

    private final int head;
    private final int tail;

    public Snake(int head, int tail, int boardWidth) {
        if (head <= tail) {
            throw new IllegalArgumentException("Snake head (" + head + ") must be greater than tail (" + tail + ")");
        }
        int headRow = (head - 1) / boardWidth;
        int tailRow = (tail - 1) / boardWidth;
        if (headRow == tailRow) {
            throw new IllegalArgumentException("Snake must span different rows: head row " + headRow + " == tail row " + tailRow);
        }
        this.head = head;
        this.tail = tail;
    }

    public int getHead() {
        return head;
    }

    public int getTail() {
        return tail;
    }

    @Override
    public String toString() {
        return "Snake{" + head + " -> " + tail + "}";
    }
}
