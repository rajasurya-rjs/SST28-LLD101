package com.example.snakeladder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Board {

    private final int size;
    private final Map<Integer, Snake> snakes;
    private final Map<Integer, Ladder> ladders;

    public Board(int size, Map<Integer, Snake> snakes, Map<Integer, Ladder> ladders) {
        if (size < 3) {
            throw new IllegalArgumentException("Board size must be at least 3, got: " + size);
        }
        this.size = size;
        this.snakes = Collections.unmodifiableMap(Map.copyOf(Objects.requireNonNull(snakes, "snakes")));
        this.ladders = Collections.unmodifiableMap(Map.copyOf(Objects.requireNonNull(ladders, "ladders")));
    }

    public int getSize() {
        return size;
    }

    public int getMaxPosition() {
        return size * size;
    }

    public Snake getSnakeAt(int position) {
        return snakes.get(position);
    }

    public Ladder getLadderAt(int position) {
        return ladders.get(position);
    }

    public int getFinalPosition(int position) {
        Snake snake = snakes.get(position);
        if (snake != null) {
            return snake.getTail();
        }
        Ladder ladder = ladders.get(position);
        if (ladder != null) {
            return ladder.getEnd();
        }
        return position;
    }

    public List<Snake> getSnakes() {
        return List.copyOf(snakes.values());
    }

    public List<Ladder> getLadders() {
        return List.copyOf(ladders.values());
    }

    @Override
    public String toString() {
        return "Board{size=" + size + "x" + size + ", snakes=" + snakes.size() + ", ladders=" + ladders.size() + "}";
    }
}
