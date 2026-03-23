package com.example.pen;

public class GelWriteStrategy implements WriteStrategy {

    @Override
    public void apply(String text, String color, int inkLevel) {
        System.out.println("[Gel Pen | " + color + " | ink:" + inkLevel + "%] " + text);
    }
}
