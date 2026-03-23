package com.example.pen;

public class WithCapMechanism implements MechanismStrategy {

    @Override
    public void open() {
        System.out.println("Removing cap");
    }

    @Override
    public void shut() {
        System.out.println("Putting cap back");
    }
}
