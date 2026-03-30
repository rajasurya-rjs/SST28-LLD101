package com.example.elevator;

public interface ElevatorController {
    void handleRequest(Request request, Elevator elevator);
    void openDoor(Elevator elevator);
    void closeDoor(Elevator elevator);
    void triggerAlarm(Elevator elevator);
}
