package com.example.elevator;

public class DefaultOutsideButtonPanel implements OutsideButtonPanel {
    private final int floor;
    private final Building building;

    public DefaultOutsideButtonPanel(int floor, Building building) {
        this.floor = floor;
        this.building = building;
    }

    @Override
    public void pressUp() {
        System.out.println("  Floor " + floor + ": UP button pressed");
        building.requestFromOutside(floor, Direction.UP);
    }

    @Override
    public void pressDown() {
        System.out.println("  Floor " + floor + ": DOWN button pressed");
        building.requestFromOutside(floor, Direction.DOWN);
    }
}
