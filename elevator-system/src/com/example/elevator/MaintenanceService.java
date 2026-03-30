package com.example.elevator;

public class MaintenanceService {

    public void setMaintenance(Elevator elevator) {
        elevator.setState(ElevatorState.UNDER_MAINTENANCE);
        elevator.clearPendingFloors();
        System.out.println("  [" + elevator.getId() + "] Set to UNDER_MAINTENANCE. Pending requests cleared.");
    }

    public void resumeOperation(Elevator elevator) {
        if (elevator.getState() != ElevatorState.UNDER_MAINTENANCE) {
            System.out.println("  [" + elevator.getId() + "] Not in maintenance mode.");
            return;
        }
        elevator.setState(ElevatorState.IDLE);
        System.out.println("  [" + elevator.getId() + "] Resumed operation. State set to IDLE.");
    }
}
