package com.example.parkinglot;

import java.util.*;

public class App {

    public static void main(String[] args) throws InterruptedException {

        // ── 1. Get singleton & configure ─────────────────────────
        ParkingLot lot = ParkingLot.getInstance();

        EntryGate gate1 = new EntryGate("G1");
        EntryGate gate2 = new EntryGate("G2");
        lot.setEntryGates(List.of(gate1, gate2));

        // Build 2 levels, each with small/medium/large slots
        lot.setLevels(List.of(
                buildLevel(0, gate1, gate2),
                buildLevel(1, gate1, gate2)
        ));

        lot.setPricingStrategy(new DefaultPricingStrategy());

        System.out.println("=== Parking Lot System ===");
        System.out.println("Pricing: " + lot.getPricingStrategy());
        System.out.println();

        // ── 2. Print initial status ──────────────────────────────
        printStatus(lot);

        // ── 3. Park vehicles (sequential demo) ──────────────────
        System.out.println("--- Parking Vehicles ---");
        long now = System.currentTimeMillis();

        ParkingTicket t1 = lot.park(
                new Vehicle("KA-01-1234", VehicleType.TWO_WHEELER),
                now, SlotType.SMALL, "G1");

        ParkingTicket t2 = lot.park(
                new Vehicle("KA-02-5678", VehicleType.CAR),
                now, SlotType.MEDIUM, "G2");

        ParkingTicket t3 = lot.park(
                new Vehicle("KA-03-9999", VehicleType.BUS),
                now, SlotType.LARGE, "G1");

        // Bike parks in MEDIUM (compatibility: smaller vehicle in larger slot)
        ParkingTicket t4 = lot.park(
                new Vehicle("KA-04-1111", VehicleType.TWO_WHEELER),
                now, SlotType.MEDIUM, "G2");

        System.out.println();
        printStatus(lot);

        // ── 4. Exit vehicles ─────────────────────────────────────
        System.out.println("--- Exiting Vehicles ---");
        long exitTime = now + 2 * 3_600_000; // 2 hours later

        double amt1 = lot.exit(t1, exitTime);
        double amt2 = lot.exit(t2, exitTime);
        double amt3 = lot.exit(t3, exitTime);
        double amt4 = lot.exit(t4, exitTime); // bike in MEDIUM → billed at MEDIUM rate

        System.out.println();
        System.out.println("Bike in SMALL slot: Rs " + amt1 + " (SMALL rate)");
        System.out.println("Car in MEDIUM slot: Rs " + amt2 + " (MEDIUM rate)");
        System.out.println("Bus in LARGE slot:  Rs " + amt3 + " (LARGE rate)");
        System.out.println("Bike in MEDIUM slot: Rs " + amt4 + " (billed as MEDIUM, not SMALL!)");

        System.out.println();
        printStatus(lot);

        // ── 5. Concurrent parking demo ───────────────────────────
        System.out.println("--- Concurrent Parking (2 threads, same slot type) ---");
        long concurrentTime = System.currentTimeMillis();

        Thread thread1 = new Thread(() -> {
            ParkingTicket ct = lot.park(
                    new Vehicle("MH-01-AAAA", VehicleType.CAR),
                    concurrentTime, SlotType.MEDIUM, "G1");
            System.out.println("  Thread-1 got: " + ct.getSlotAssigned().getId());
        });

        Thread thread2 = new Thread(() -> {
            ParkingTicket ct = lot.park(
                    new Vehicle("MH-02-BBBB", VehicleType.CAR),
                    concurrentTime, SlotType.MEDIUM, "G2");
            System.out.println("  Thread-2 got: " + ct.getSlotAssigned().getId());
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println();
        printStatus(lot);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static Level buildLevel(int levelIndex, EntryGate g1, EntryGate g2) {
        List<Slot> slots = new ArrayList<>();
        int slotNum = 0;

        // 3 SMALL slots per level
        for (int i = 0; i < 3; i++) {
            slots.add(makeSlot("S-" + levelIndex + "-" + (slotNum++),
                    SlotType.SMALL, levelIndex, g1, g2, i));
        }
        // 3 MEDIUM slots per level
        for (int i = 0; i < 3; i++) {
            slots.add(makeSlot("M-" + levelIndex + "-" + (slotNum++),
                    SlotType.MEDIUM, levelIndex, g1, g2, i + 3));
        }
        // 2 LARGE slots per level
        for (int i = 0; i < 2; i++) {
            slots.add(makeSlot("L-" + levelIndex + "-" + (slotNum++),
                    SlotType.LARGE, levelIndex, g1, g2, i + 6));
        }
        return new Level(levelIndex, slots);
    }

    private static Slot makeSlot(String id, SlotType type, int levelIndex,
                                  EntryGate g1, EntryGate g2, int position) {
        // Distance = level weight (10 per level) + position offset
        // G1 is near position 0, G2 is near position 7
        int distG1 = levelIndex * 10 + position;
        int distG2 = levelIndex * 10 + Math.abs(7 - position);
        return new Slot(id, type, levelIndex, Map.of(g1.getId(), distG1, g2.getId(), distG2));
    }

    private static void printStatus(ParkingLot lot) {
        System.out.println("Status:");
        Map<SlotType, int[]> status = lot.status();
        for (Map.Entry<SlotType, int[]> entry : status.entrySet()) {
            int[] counts = entry.getValue();
            System.out.println("  " + entry.getKey() + ": "
                    + counts[0] + " available / " + counts[1] + " total");
        }
        System.out.println();
    }
}
