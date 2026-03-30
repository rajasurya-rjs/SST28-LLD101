package com.example.elevator;

import java.util.ArrayList;
import java.util.List;

public class Elevator {
    private final String id;
    private int currentFloor;
    private ElevatorState state;
    private DoorState doorState;
    private double currentWeight;
    private final double maxWeight;
    private final List<Integer> pendingFloors;

    public Elevator(String id, int startFloor, double maxWeight) {
        this.id = id;
        this.currentFloor = startFloor;
        this.state = ElevatorState.IDLE;
        this.doorState = DoorState.CLOSED;
        this.currentWeight = 0;
        this.maxWeight = maxWeight;
        this.pendingFloors = new ArrayList<>();
    }

    public Elevator(String id, int startFloor) {
        this(id, startFloor, 700.0);
    }

    public void addDestination(int floor) {
        if (!pendingFloors.contains(floor) && floor != currentFloor) {
            pendingFloors.add(floor);
        }
    }

    public void moveOneFloor() {
        if (!canOperate()) {
            if (state == ElevatorState.UNDER_MAINTENANCE) {
                System.out.println("  [" + id + "] Cannot move - under maintenance");
            } else {
                System.out.println("  [" + id + "] Cannot move - overweight (" + currentWeight + "kg / " + maxWeight + "kg)");
            }
            return;
        }

        if (pendingFloors.isEmpty()) {
            state = ElevatorState.IDLE;
            return;
        }

        int nextDest = pendingFloors.get(0);

        if (nextDest > currentFloor) {
            state = ElevatorState.MOVING_UP;
            currentFloor++;
            System.out.println("  [" + id + "] Moving UP to floor " + currentFloor);
        } else if (nextDest < currentFloor) {
            state = ElevatorState.MOVING_DOWN;
            currentFloor--;
            System.out.println("  [" + id + "] Moving DOWN to floor " + currentFloor);
        }

        if (currentFloor == nextDest) {
            pendingFloors.remove(0);
            System.out.println("  [" + id + "] Arrived at floor " + currentFloor);
            openDoor();
            closeDoor();
            if (pendingFloors.isEmpty()) {
                state = ElevatorState.IDLE;
            }
        }
    }

    public boolean canOperate() {
        return state != ElevatorState.UNDER_MAINTENANCE && !isOverweight();
    }

    public boolean isOverweight() {
        return currentWeight > maxWeight;
    }

    public void openDoor() {
        doorState = DoorState.OPEN;
        System.out.println("  [" + id + "] Door OPENED at floor " + currentFloor);
    }

    public void closeDoor() {
        doorState = DoorState.CLOSED;
        System.out.println("  [" + id + "] Door CLOSED at floor " + currentFloor);
    }

    public void addWeight(double kg) {
        currentWeight += kg;
        System.out.println("  [" + id + "] Weight added: " + kg + "kg (total: " + currentWeight + "kg)");
    }

    public void removeWeight(double kg) {
        currentWeight = Math.max(0, currentWeight - kg);
        System.out.println("  [" + id + "] Weight removed: " + kg + "kg (total: " + currentWeight + "kg)");
    }

    public String getId() { return id; }
    public int getCurrentFloor() { return currentFloor; }
    public ElevatorState getState() { return state; }
    public void setState(ElevatorState state) { this.state = state; }
    public DoorState getDoorState() { return doorState; }
    public double getCurrentWeight() { return currentWeight; }
    public double getMaxWeight() { return maxWeight; }
    public int getPendingCount() { return pendingFloors.size(); }
    public List<Integer> getPendingFloors() { return new ArrayList<>(pendingFloors); }

    public void clearPendingFloors() {
        pendingFloors.clear();
    }

    @Override
    public String toString() {
        return "[" + id + "] Floor=" + currentFloor + ", State=" + state
                + ", Door=" + doorState + ", Weight=" + currentWeight + "kg"
                + ", Pending=" + pendingFloors;
    }
}
