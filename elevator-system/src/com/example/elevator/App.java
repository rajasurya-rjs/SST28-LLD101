package com.example.elevator;

import java.util.List;

public class App {

    public static void main(String[] args) {
        Building building = Building.getInstance();
        ElevatorController controller = building.getController();
        MaintenanceService maintenance = building.getMaintenanceService();

        // ============================================================
        // 1. SETUP: 10 floors, 3 elevators
        // ============================================================
        System.out.println("=== ELEVATOR SYSTEM DEMO ===\n");
        System.out.println("--- 1. Setup ---");

        Elevator e1 = new Elevator("E1", 0);
        Elevator e2 = new Elevator("E2", 5);
        Elevator e3 = new Elevator("E3", 9);

        building.configure(10, List.of(e1, e2, e3), new NearestElevatorStrategy());
        building.printStatus();

        // Wire inside button panels
        InsideButtonPanel e1Panel = new InsideButtonPanel(e1, building, controller);
        InsideButtonPanel e2Panel = new InsideButtonPanel(e2, building, controller);
        InsideButtonPanel e3Panel = new InsideButtonPanel(e3, building, controller);

        // ============================================================
        // 2. OUTSIDE BUTTON REQUESTS
        // ============================================================
        System.out.println("\n--- 2. Outside Button Requests ---");
        System.out.println("Person on floor 3 presses UP, person on floor 7 presses DOWN:");

        building.getFloors().get(3).getOutsidePanel().pressUp();
        building.getFloors().get(7).getOutsidePanel().pressDown();

        System.out.println("\nSimulating 3 steps...");
        for (int i = 0; i < 3; i++) {
            building.step();
        }
        building.printStatus();

        // ============================================================
        // 3. INSIDE BUTTON REQUESTS
        // ============================================================
        System.out.println("\n--- 3. Inside Button Requests ---");
        System.out.println("Person inside E1 presses floor 6, person inside E2 presses floor 0:");

        e1Panel.pressFloor(6);
        e2Panel.pressFloor(0);

        System.out.println("\nSimulating 6 steps...");
        for (int i = 0; i < 6; i++) {
            building.step();
        }
        building.printStatus();

        // ============================================================
        // 4. WEIGHT LIMIT DEMO
        // ============================================================
        System.out.println("\n--- 4. Weight Limit ---");

        e1.addWeight(650);
        e1Panel.pressFloor(8);

        System.out.println("\nAdding 100kg more (total 750kg > 700kg limit):");
        e1.addWeight(100);

        System.out.println("\nTrying to move E1 while overweight:");
        building.step();

        System.out.println("\nRemoving 200kg:");
        e1.removeWeight(200);

        System.out.println("\nE1 can now move:");
        building.step();
        building.printStatus();

        // ============================================================
        // 5. MAINTENANCE DEMO
        // ============================================================
        System.out.println("\n--- 5. Maintenance ---");

        System.out.println("Setting E3 to maintenance:");
        maintenance.setMaintenance(e3);

        System.out.println("\nRequest from floor 9 (E3 is in maintenance, should be skipped):");
        building.getFloors().get(9).getOutsidePanel().pressUp();

        System.out.println("\nResuming E3:");
        maintenance.resumeOperation(e3);
        building.printStatus();

        // ============================================================
        // 6. STRATEGY SWAP DEMO
        // ============================================================
        System.out.println("\n--- 6. Strategy Swap ---");

        System.out.println("Switching to LeastBusyStrategy:");
        building.getScheduler().setAlgorithm(new LeastBusyStrategy());

        e1Panel.pressFloor(2);
        e1Panel.pressFloor(4);
        System.out.println("E1 now has 2+ pending requests.");

        System.out.println("\nNew request from floor 3 UP (should pick least busy elevator):");
        building.getFloors().get(3).getOutsidePanel().pressUp();

        System.out.println("\nSwitching to DirectionBasedStrategy:");
        building.getScheduler().setAlgorithm(new DirectionBasedStrategy());

        // Set E2 moving up to test direction-based
        e2Panel.pressFloor(8);
        System.out.println("\nRequest from floor 5 UP (should prefer E2 if it's moving up):");
        building.requestFromOutside(5, Direction.UP);

        System.out.println("\nSimulating 3 steps...");
        for (int i = 0; i < 3; i++) {
            building.step();
        }
        building.printStatus();

        // ============================================================
        // 7. ADD FLOOR DEMO
        // ============================================================
        System.out.println("\n--- 7. Add Floor ---");

        building.addFloor();
        System.out.println("Request from new floor 10:");
        building.getFloors().get(10).getOutsidePanel().pressUp();

        System.out.println("\nSimulating 2 steps...");
        for (int i = 0; i < 2; i++) {
            building.step();
        }
        building.printStatus();

        // ============================================================
        // 8. ALARM DEMO
        // ============================================================
        System.out.println("\n--- 8. Alarm ---");
        e2Panel.pressAlarm();
        building.printStatus();

        System.out.println("\n=== DEMO COMPLETE ===");
    }
}
