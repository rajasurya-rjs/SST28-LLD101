package com.example.snakeladder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Game {

    private final Board board;
    private final List<Player> players;
    private final Dice dice;
    private final TurnStrategy turnStrategy;
    private int currentPlayerIndex;
    private final List<Player> winners;

    public Game(Board board, List<Player> players, Dice dice, TurnStrategy turnStrategy) {
        this.board = Objects.requireNonNull(board, "board");
        this.players = new ArrayList<>(Objects.requireNonNull(players, "players"));
        this.dice = Objects.requireNonNull(dice, "dice");
        this.turnStrategy = Objects.requireNonNull(turnStrategy, "turnStrategy");
        this.currentPlayerIndex = 0;
        this.winners = new ArrayList<>();

        if (this.players.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players");
        }
    }

    public TurnResult makeTurn() {
        if (isGameOver()) {
            throw new IllegalStateException("Game is already over");
        }

        Player current = players.get(currentPlayerIndex);
        TurnResult result = turnStrategy.executeTurn(current, dice, board);

        if (current.getPosition() == board.getMaxPosition()) {
            winners.add(current);
            players.remove(currentPlayerIndex);
            if (!players.isEmpty()) {
                currentPlayerIndex = currentPlayerIndex % players.size();
            }
        } else {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }

        return result;
    }

    public boolean isGameOver() {
        return players.size() < 2;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public List<Player> getWinners() {
        return Collections.unmodifiableList(winners);
    }

    public List<Player> getRemainingPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Board getBoard() {
        return board;
    }
}
