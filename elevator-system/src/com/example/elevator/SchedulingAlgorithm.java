package com.example.elevator;

import java.util.List;

public interface SchedulingAlgorithm {
    Elevator selectElevator(Request request, List<Elevator> elevators);
}
