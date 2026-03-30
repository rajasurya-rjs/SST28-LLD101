package com.example.elevator;

public class Floor {
    private final int floorNumber;
    private OutsideButtonPanel outsidePanel;

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public OutsideButtonPanel getOutsidePanel() {
        return outsidePanel;
    }

    public void setOutsidePanel(OutsideButtonPanel outsidePanel) {
        this.outsidePanel = outsidePanel;
    }

    @Override
    public String toString() {
        return "Floor " + floorNumber;
    }
}
