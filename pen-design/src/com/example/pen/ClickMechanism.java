package com.example.pen;

public class ClickMechanism implements MechanismStrategy {

    @Override
    public void open() {
        System.out.println("Clicking pen open");
    }

    @Override
    public void shut() {
        System.out.println("Clicking pen closed");
    }
}
