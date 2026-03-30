package com.example.elevator;

import java.util.List;

public class DirectionBasedStrategy implements SchedulingAlgorithm {

    @Override
    public Elevator selectElevator(Request request, List<Elevator> elevators) {
        Elevator bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        // Priority 1: elevator moving in same direction that hasn't passed the floor
        for (Elevator elevator : elevators) {
            if (!elevator.canOperate()) continue;

            if (request.getDirection() == Direction.UP
                    && elevator.getState() == ElevatorState.MOVING_UP
                    && elevator.getCurrentFloor() <= request.getSourceFloor()) {
                int distance = request.getSourceFloor() - elevator.getCurrentFloor();
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = elevator;
                }
            } else if (request.getDirection() == Direction.DOWN
                    && elevator.getState() == ElevatorState.MOVING_DOWN
                    && elevator.getCurrentFloor() >= request.getSourceFloor()) {
                int distance = elevator.getCurrentFloor() - request.getSourceFloor();
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = elevator;
                }
            }
        }
        if (bestMatch != null) return bestMatch;

        // Priority 2: nearest idle elevator
        bestDistance = Integer.MAX_VALUE;
        for (Elevator elevator : elevators) {
            if (!elevator.canOperate()) continue;
            if (elevator.getState() == ElevatorState.IDLE) {
                int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMatch = elevator;
                }
            }
        }
        if (bestMatch != null) return bestMatch;

        // Priority 3: fallback to nearest operable elevator
        bestDistance = Integer.MAX_VALUE;
        for (Elevator elevator : elevators) {
            if (!elevator.canOperate()) continue;
            int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = elevator;
            }
        }
        return bestMatch;
    }

    @Override
    public String toString() {
        return "DirectionBasedStrategy";
    }
}
