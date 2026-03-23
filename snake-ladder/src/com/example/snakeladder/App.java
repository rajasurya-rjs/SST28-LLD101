package com.example.snakeladder;

import java.util.List;

public class App {

    public static void main(String[] args) {
        int boardSize = 10;
        List<String> playerNames = List.of("Alice", "Bob", "Charlie");
        GameVersion version = GameVersion.EASY;

        System.out.println("=== Snake & Ladder Game ===");
        System.out.println("Board: " + boardSize + "x" + boardSize);
        System.out.println("Players: " + playerNames);
        System.out.println("Version: " + version);
        System.out.println();

        Game game = GameFactory.create(boardSize, playerNames, version);

        System.out.println("Board setup:");
        System.out.println("  Snakes:  " + game.getBoard().getSnakes());
        System.out.println("  Ladders: " + game.getBoard().getLadders());
        System.out.println();

        int turnCount = 0;
        while (!game.isGameOver()) {
            TurnResult result = game.makeTurn();
            turnCount++;
            System.out.println("Turn " + turnCount + ": " + result);
        }

        System.out.println();
        System.out.println("=== Game Over ===");
        System.out.println("Winners (in order): " + game.getWinners());
        System.out.println("Last player standing: " + game.getRemainingPlayers());

        System.out.println();
        System.out.println("--- Running Difficult Version ---");
        System.out.println();

        Game hardGame = GameFactory.create(boardSize, playerNames, GameVersion.DIFFICULT);

        System.out.println("Board setup:");
        System.out.println("  Snakes:  " + hardGame.getBoard().getSnakes());
        System.out.println("  Ladders: " + hardGame.getBoard().getLadders());
        System.out.println();

        turnCount = 0;
        while (!hardGame.isGameOver()) {
            TurnResult result = hardGame.makeTurn();
            turnCount++;
            System.out.println("Turn " + turnCount + ": " + result);
        }

        System.out.println();
        System.out.println("=== Game Over (Difficult) ===");
        System.out.println("Winners (in order): " + hardGame.getWinners());
        System.out.println("Last player standing: " + hardGame.getRemainingPlayers());
    }
}
