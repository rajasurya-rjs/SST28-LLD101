package com.example.snakeladder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GameFactory {

    private GameFactory() {
    }

    public static Game create(int boardSize, List<String> playerNames, GameVersion version) {
        Objects.requireNonNull(playerNames, "playerNames");
        Objects.requireNonNull(version, "version");

        if (boardSize < 3) {
            throw new IllegalArgumentException("Board size must be at least 3, got: " + boardSize);
        }
        if (playerNames.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players, got: " + playerNames.size());
        }

        Board board = BoardGenerator.generate(boardSize);

        List<Player> players = new ArrayList<>();
        for (String name : playerNames) {
            players.add(new Player(name));
        }

        Dice dice = new Dice();

        TurnStrategy strategy = switch (version) {
            case EASY -> new EasyTurnStrategy();
            case DIFFICULT -> new DifficultTurnStrategy();
        };

        return new Game(board, players, dice, strategy);
    }
}
