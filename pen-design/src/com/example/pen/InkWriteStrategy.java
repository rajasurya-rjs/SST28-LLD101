package com.example.pen;

public class InkWriteStrategy implements WriteStrategy {

    @Override
    public void apply(String text, String color, int inkLevel) {
        System.out.println("[Ink Pen | " + color + " | ink:" + inkLevel + "%] " + text);
    }
}
