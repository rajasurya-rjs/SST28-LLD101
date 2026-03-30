package com.example.elevator;

public class DefaultElevatorController implements ElevatorController {

    @Override
    public void handleRequest(Request request, Elevator elevator) {
        if (!elevator.canOperate()) {
            System.out.println("  [" + elevator.getId() + "] Cannot handle request - "
                    + (elevator.getState() == ElevatorState.UNDER_MAINTENANCE ? "under maintenance" : "overweight"));
            return;
        }
        elevator.addDestination(request.getTargetFloor());
        System.out.println("  [" + elevator.getId() + "] Request accepted: go to floor " + request.getTargetFloor());
    }

    @Override
    public void openDoor(Elevator elevator) {
        elevator.openDoor();
    }

    @Override
    public void closeDoor(Elevator elevator) {
        elevator.closeDoor();
    }

    @Override
    public void triggerAlarm(Elevator elevator) {
        System.out.println("  [" + elevator.getId() + "] ALARM TRIGGERED! Emergency stop.");
        elevator.setState(ElevatorState.IDLE);
        elevator.clearPendingFloors();
    }
}
