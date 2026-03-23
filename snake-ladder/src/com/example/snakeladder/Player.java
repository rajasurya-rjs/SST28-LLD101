package com.example.snakeladder;

import java.util.Objects;

public class Player {

    private final String name;
    private int position;

    public Player(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.position = 0;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Position cannot be negative: " + position);
        }
        this.position = position;
    }

    @Override
    public String toString() {
        return name + "(pos=" + position + ")";
    }
}
