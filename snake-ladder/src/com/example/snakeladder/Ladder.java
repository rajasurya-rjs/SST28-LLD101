package com.example.snakeladder;

public final class Ladder {

    private final int start;
    private final int end;

    public Ladder(int start, int end, int boardWidth) {
        if (end <= start) {
            throw new IllegalArgumentException("Ladder end (" + end + ") must be greater than start (" + start + ")");
        }
        int startRow = (start - 1) / boardWidth;
        int endRow = (end - 1) / boardWidth;
        if (startRow == endRow) {
            throw new IllegalArgumentException("Ladder must span different rows: start row " + startRow + " == end row " + endRow);
        }
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "Ladder{" + start + " -> " + end + "}";
    }
}
