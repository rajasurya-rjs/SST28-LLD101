package com.example.snakeladder;

import java.util.Random;

public class Dice {

    private final Random random;

    public Dice() {
        this.random = new Random();
    }

    public Dice(long seed) {
        this.random = new Random(seed);
    }

    public int roll() {
        return 1 + random.nextInt(6);
    }
}
