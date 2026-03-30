package com.example.elevator;

import java.util.List;

public class ElevatorScheduler {
    private SchedulingAlgorithm algorithm;

    public ElevatorScheduler(SchedulingAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public void setAlgorithm(SchedulingAlgorithm algorithm) {
        this.algorithm = algorithm;
        System.out.println("  Scheduler algorithm changed to: " + algorithm);
    }

    public SchedulingAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void dispatch(Request request, List<Elevator> elevators) {
        Elevator selected = algorithm.selectElevator(request, elevators);
        if (selected == null) {
            System.out.println("  WARNING: No eligible elevator for " + request);
            return;
        }
        selected.addDestination(request.getTargetFloor());
        System.out.println("  Assigned " + request + " -> " + selected.getId());
    }
}
