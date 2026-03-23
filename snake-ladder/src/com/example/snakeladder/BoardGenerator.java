package com.example.snakeladder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class BoardGenerator {

    private static final int MAX_RETRIES = 1000;

    private BoardGenerator() {
    }

    public static Board generate(int n) {
        int maxPos = n * n;
        int availablePositions = maxPos - 2;
        int requiredEndpoints = 4 * n;

        if (requiredEndpoints > availablePositions) {
            throw new IllegalArgumentException(
                "Board too small: need " + requiredEndpoints + " endpoints but only " + availablePositions + " positions available"
            );
        }

        Random random = new Random();
        Set<Integer> occupied = new HashSet<>();
        occupied.add(1);
        occupied.add(maxPos);

        Map<Integer, Snake> snakes = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Snake snake = generateSnake(n, maxPos, occupied, random);
            snakes.put(snake.getHead(), snake);
            occupied.add(snake.getHead());
            occupied.add(snake.getTail());
        }

        Map<Integer, Ladder> ladders = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Ladder ladder = generateLadder(n, maxPos, occupied, random);
            ladders.put(ladder.getStart(), ladder);
            occupied.add(ladder.getStart());
            occupied.add(ladder.getEnd());
        }

        return new Board(n, snakes, ladders);
    }

    private static Snake generateSnake(int n, int maxPos, Set<Integer> occupied, Random random) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            int head = 2 + random.nextInt(maxPos - 2);
            int tail = 2 + random.nextInt(maxPos - 2);

            if (head <= tail) continue;
            if (occupied.contains(head) || occupied.contains(tail)) continue;
            if (head == tail) continue;

            int headRow = (head - 1) / n;
            int tailRow = (tail - 1) / n;
            if (headRow == tailRow) continue;

            return new Snake(head, tail, n);
        }
        throw new RuntimeException("Failed to place snake after " + MAX_RETRIES + " attempts");
    }

    private static Ladder generateLadder(int n, int maxPos, Set<Integer> occupied, Random random) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            int start = 2 + random.nextInt(maxPos - 2);
            int end = 2 + random.nextInt(maxPos - 2);

            if (end <= start) continue;
            if (occupied.contains(start) || occupied.contains(end)) continue;
            if (start == end) continue;

            int startRow = (start - 1) / n;
            int endRow = (end - 1) / n;
            if (startRow == endRow) continue;

            return new Ladder(start, end, n);
        }
        throw new RuntimeException("Failed to place ladder after " + MAX_RETRIES + " attempts");
    }
}
