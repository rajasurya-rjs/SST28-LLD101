package com.example.pen;

public class BallWriteStrategy implements WriteStrategy {

    @Override
    public void apply(String text, String color, int inkLevel) {
        System.out.println("[Ball Pen | " + color + " | ink:" + inkLevel + "%] " + text);
    }
}
