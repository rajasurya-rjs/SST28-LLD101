package com.example.snakeladder;

import java.util.ArrayList;
import java.util.List;

public class EasyTurnStrategy implements TurnStrategy {

    @Override
    public TurnResult executeTurn(Player player, Dice dice, Board board) {
        int startPosition = player.getPosition();
        List<Integer> rolls = new ArrayList<>();
        StringBuilder msg = new StringBuilder();
        msg.append(player.getName());

        boolean keepRolling = true;
        while (keepRolling) {
            int diceValue = dice.roll();
            rolls.add(diceValue);

            int candidatePos = player.getPosition() + diceValue;

            if (candidatePos > board.getMaxPosition()) {
                msg.append(" rolled ").append(diceValue)
                   .append(": ").append(player.getPosition())
                   .append(" -> exceeds ").append(board.getMaxPosition())
                   .append(", stays at ").append(player.getPosition());
                keepRolling = false;
            } else {
                int finalPos = board.getFinalPosition(candidatePos);
                msg.append(" rolled ").append(diceValue)
                   .append(": ").append(player.getPosition())
                   .append(" -> ").append(candidatePos);

                if (finalPos != candidatePos) {
                    Snake snake = board.getSnakeAt(candidatePos);
                    if (snake != null) {
                        msg.append(" ~bitten by snake~ -> ").append(finalPos);
                    } else {
                        msg.append(" ~climbed ladder~ -> ").append(finalPos);
                    }
                }

                player.setPosition(finalPos);

                if (finalPos == board.getMaxPosition()) {
                    msg.append(" *** WINS! ***");
                    keepRolling = false;
                } else if (diceValue == 6) {
                    msg.append(" | Got 6, rolls again!");
                    msg.append("\n  ");
                } else {
                    keepRolling = false;
                }
            }
        }

        return new TurnResult(player.getName(), rolls, startPosition, player.getPosition(), msg.toString());
    }
}
