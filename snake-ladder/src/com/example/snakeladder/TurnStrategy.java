package com.example.snakeladder;

public interface TurnStrategy {

    TurnResult executeTurn(Player player, Dice dice, Board board);
}
