package com.example.elevator;

public class InsideButtonPanel {
    private final String elevatorId;
    private final Building building;
    private final ElevatorController controller;
    private final Elevator elevator;

    public InsideButtonPanel(Elevator elevator, Building building, ElevatorController controller) {
        this.elevatorId = elevator.getId();
        this.elevator = elevator;
        this.building = building;
        this.controller = controller;
    }

    public void pressFloor(int floor) {
        System.out.println("  [" + elevatorId + "] Inside button pressed: floor " + floor);
        building.requestFromInside(elevatorId, floor);
    }

    public void pressDoorOpen() {
        System.out.println("  [" + elevatorId + "] Door Open button pressed");
        controller.openDoor(elevator);
    }

    public void pressDoorClose() {
        System.out.println("  [" + elevatorId + "] Door Close button pressed");
        controller.closeDoor(elevator);
    }

    public void pressAlarm() {
        System.out.println("  [" + elevatorId + "] Alarm button pressed");
        controller.triggerAlarm(elevator);
    }
}
