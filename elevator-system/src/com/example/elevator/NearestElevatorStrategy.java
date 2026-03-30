package com.example.elevator;

import java.util.List;

public class NearestElevatorStrategy implements SchedulingAlgorithm {

    @Override
    public Elevator selectElevator(Request request, List<Elevator> elevators) {
        Elevator nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (!elevator.canOperate()) continue;

            int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = elevator;
            }
        }
        return nearest;
    }

    @Override
    public String toString() {
        return "NearestElevatorStrategy";
    }
}
