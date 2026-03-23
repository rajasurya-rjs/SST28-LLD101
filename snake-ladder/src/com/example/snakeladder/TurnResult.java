package com.example.snakeladder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TurnResult {

    private final String playerName;
    private final List<Integer> diceRolls;
    private final int startPosition;
    private final int endPosition;
    private final String message;

    public TurnResult(String playerName, List<Integer> diceRolls, int startPosition, int endPosition, String message) {
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.diceRolls = Collections.unmodifiableList(List.copyOf(diceRolls));
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.message = Objects.requireNonNull(message, "message");
    }

    public String getPlayerName() {
        return playerName;
    }

    public List<Integer> getDiceRolls() {
        return diceRolls;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
