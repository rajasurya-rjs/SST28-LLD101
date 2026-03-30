package com.example.elevator;

import java.util.ArrayList;
import java.util.List;

public class Building {
    private final List<Floor> floors;
    private final List<Elevator> elevators;
    private final ElevatorScheduler scheduler;
    private final MaintenanceService maintenanceService;
    private final ElevatorController controller;

    private static class Holder {
        private static final Building INSTANCE = new Building();
    }

    public static Building getInstance() {
        return Holder.INSTANCE;
    }

    private Building() {
        this.floors = new ArrayList<>();
        this.elevators = new ArrayList<>();
        this.scheduler = new ElevatorScheduler(new NearestElevatorStrategy());
        this.maintenanceService = new MaintenanceService();
        this.controller = new DefaultElevatorController();
    }

    public void configure(int numFloors, List<Elevator> elevators, SchedulingAlgorithm algorithm) {
        this.floors.clear();
        this.elevators.clear();

        for (int i = 0; i < numFloors; i++) {
            Floor floor = new Floor(i);
            floor.setOutsidePanel(new DefaultOutsideButtonPanel(i, this));
            this.floors.add(floor);
        }

        this.elevators.addAll(elevators);
        this.scheduler.setAlgorithm(algorithm);
        System.out.println("  Building configured: " + numFloors + " floors, "
                + elevators.size() + " elevators, strategy=" + algorithm);
    }

    public void addFloor() {
        int newFloorNum = floors.size();
        Floor floor = new Floor(newFloorNum);
        floor.setOutsidePanel(new DefaultOutsideButtonPanel(newFloorNum, this));
        floors.add(floor);
        System.out.println("  New floor added: Floor " + newFloorNum + " (total: " + floors.size() + " floors)");
    }

    public void requestFromOutside(int floor, Direction direction) {
        int targetFloor = direction == Direction.UP ? floors.size() - 1 : 0;
        Request request = new Request(floor, targetFloor, direction);
        scheduler.dispatch(request, elevators);
    }

    public void requestFromInside(String elevatorId, int floor) {
        for (Elevator elevator : elevators) {
            if (elevator.getId().equals(elevatorId)) {
                Direction dir = floor > elevator.getCurrentFloor() ? Direction.UP : Direction.DOWN;
                Request request = new Request(elevator.getCurrentFloor(), floor, dir);
                controller.handleRequest(request, elevator);
                return;
            }
        }
        System.out.println("  WARNING: Elevator " + elevatorId + " not found");
    }

    public void step() {
        for (Elevator elevator : elevators) {
            elevator.moveOneFloor();
        }
    }

    public void printStatus() {
        System.out.println("  --- Elevator Status ---");
        for (Elevator elevator : elevators) {
            System.out.println("  " + elevator);
        }
        System.out.println("  -----------------------");
    }

    public List<Elevator> getElevators() { return elevators; }
    public List<Floor> getFloors() { return floors; }
    public ElevatorScheduler getScheduler() { return scheduler; }
    public MaintenanceService getMaintenanceService() { return maintenanceService; }
    public ElevatorController getController() { return controller; }
}
