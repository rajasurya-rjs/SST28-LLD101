package com.example.elevator;

import java.util.List;

public class LeastBusyStrategy implements SchedulingAlgorithm {

    @Override
    public Elevator selectElevator(Request request, List<Elevator> elevators) {
        Elevator leastBusy = null;
        int minPending = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (!elevator.canOperate()) continue;

            if (elevator.getPendingCount() < minPending) {
                minPending = elevator.getPendingCount();
                leastBusy = elevator;
            }
        }
        return leastBusy;
    }

    @Override
    public String toString() {
        return "LeastBusyStrategy";
    }
}
