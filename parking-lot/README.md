# Parking Lot — Low-Level Design

## Problem
Design a multi-level parking lot with multiple entry gates, three slot types (small/medium/large), vehicle-slot compatibility, nearest-slot allocation, concurrency-safe booking, and billing by allocated slot type.

---

## Class Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     <<singleton>>                           │
│                      ParkingLot                             │
├─────────────────────────────────────────────────────────────┤
│ - levels: List<Level>                                       │
│ - entryGates: List<EntryGate>                               │
│ - pricingStrategy: PricingStrategy                          │
├─────────────────────────────────────────────────────────────┤
│ + getInstance(): ParkingLot                                 │
│ + setLevels(List<Level>)                                    │
│ + setEntryGates(List<EntryGate>)                            │
│ + setPricingStrategy(PricingStrategy)                       │
│ + park(Vehicle, entryTime, SlotType, gateId): ParkingTicket │
│ + exit(ParkingTicket, exitTime): double                     │
│ + status(): Map<SlotType, int[]>                            │
└─────────────────────────────────────────────────────────────┘
         │ has-many           │ uses
         ▼                    ▼
┌──────────────────┐  ┌────────────────────────┐
│      Level       │  │  <<interface>>          │
├──────────────────┤  │  PricingStrategy        │
│ - levelIndex     │  ├────────────────────────┤
│ - slots: List    │  │ + calculatePrice(       │
│ - lock: Lock     │  │     SlotType, hours)    │
├──────────────────┤  └────────┬───────────────┘
│ + allocateNearest│           │ implements
│   (SlotType,     │           ▼
│    gateId): Slot │  ┌────────────────────────┐
│ + free(Slot)     │  │ DefaultPricingStrategy  │
│ + getAvailability│  ├────────────────────────┤
│   (SlotType)     │  │ SMALL=10/hr            │
└───────┬──────────┘  │ MEDIUM=20/hr           │
        │ has-many    │ LARGE=30/hr            │
        ▼             └────────────────────────┘
┌──────────────────────┐
│        Slot          │
├──────────────────────┤     ┌──────────────┐
│ - id: String         │     │  EntryGate   │
│ - slotType: SlotType │     ├──────────────┤
│ - levelIndex: int    │     │ - id: String │
│ - distanceFromGates  │     └──────────────┘
│ - occupied: boolean  │
├──────────────────────┤     ┌──────────────────────────┐
│ + markOccupied()     │     │     ParkingTicket        │
│ + markFree()         │     ├──────────────────────────┤
│ + getDistanceFrom()  │     │ - ticketId: String       │
└──────────────────────┘     │ - vehicle: Vehicle       │
                             │ - slotAssigned: Slot     │
┌──────────────────┐         │ - allocatedSlotType      │
│     Vehicle      │         │ - entryTime: long        │
├──────────────────┤         └──────────────────────────┘
│ - licensePlate   │
│ - type           │
└──────────────────┘

┌──────────────────────────────────────────────┐
│              <<enum>> SlotType               │
├──────────────────────────────────────────────┤
│ SMALL  │  MEDIUM  │  LARGE                   │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│            <<enum>> VehicleType              │
├──────────────────────────────────────────────┤
│ TWO_WHEELER → [SMALL, MEDIUM, LARGE]         │
│ CAR         → [MEDIUM, LARGE]                │
│ BUS         → [LARGE]                        │
└──────────────────────────────────────────────┘
```

---

## Relationships

```
ParkingLot (Singleton)
 ├── has-many → Level
 │                └── has-many → Slot
 ├── has-many → EntryGate
 └── uses    → PricingStrategy (interface)
                   └── DefaultPricingStrategy

ParkingTicket ← created by park(), consumed by exit()
Vehicle       ← passed into park()
VehicleType   → knows compatible SlotTypes
```

---

## Design Patterns

### 1. Singleton (Bill Pugh Holder) — ParkingLot
- Private constructor with guard flag to prevent reflection attacks
- Static inner `Holder` class ensures lazy, thread-safe initialization
- Attributes set via setters (not constructor) per requirement

### 2. Strategy — PricingStrategy
- Interface with `calculatePrice(SlotType, hours)`
- `DefaultPricingStrategy` has fixed rates per slot type
- Swap pricing strategy without touching ParkingLot code

---

## How `park()` Works

```
park(vehicle, entryTime, requestedSlotType, entryGateId)
  │
  ├── 1. Validate: is requestedSlotType compatible with vehicle?
  │      TWO_WHEELER can fit in SMALL/MEDIUM/LARGE
  │      CAR can fit in MEDIUM/LARGE
  │      BUS can fit only in LARGE
  │
  ├── 2. Build search order:
  │      requested type first → then larger compatible types
  │      e.g., bike requests SMALL → [SMALL, MEDIUM, LARGE]
  │
  ├── 3. For each slot type in search order:
  │      For each level (0 first, then 1, 2...):
  │        level.allocateNearest(slotType, gateId)
  │          ┌─ lock.lock() ─────────────────────┐
  │          │ scan slots                         │
  │          │ filter: matching type + unoccupied │
  │          │ pick: minimum distance from gate   │
  │          │ mark occupied                      │
  │          └─ lock.unlock() ───────────────────┘
  │        if found → create ParkingTicket, return it
  │
  └── 4. No slot found → throw IllegalStateException
```

## How `exit()` Works

```
exit(ticket, exitTime)
  │
  ├── 1. duration = exitTime - entryTime (round up, min 1 hour)
  ├── 2. amount = pricingStrategy.calculatePrice(ALLOCATED slot type, hours)
  │      (bike in MEDIUM slot → billed at MEDIUM rate, not SMALL)
  ├── 3. level.free(slot) — under lock, marks slot free
  └── 4. return amount
```

---

## Concurrency

- **One `ReentrantLock` per Level** — find + mark is one atomic operation
- Different levels don't contend → parallel parking on different floors
- Same level serializes → no double-booking possible

```
Thread A (Gate G1)          Thread B (Gate G2)
    │                           │
    ├─ level0.allocate()        ├─ level0.allocate()
    │   lock acquired ──────►   │   blocks...
    │   finds M-0-3 nearest     │   ...waiting
    │   marks M-0-3 occupied    │   ...waiting
    │   lock released ─────►    │   lock acquired
    │                           │   M-0-3 is taken
    │                           │   finds M-0-5 nearest
    │                           │   marks M-0-5 occupied
    │                           │   lock released
    ▼                           ▼
  Different slots, no conflict
```

---

## Vehicle-Slot Compatibility

A smaller vehicle **can** park in a larger slot. Billing is by **allocated slot type**.

| Vehicle | Can Park In | Example |
|---------|-------------|---------|
| TWO_WHEELER | SMALL, MEDIUM, LARGE | Bike in MEDIUM → billed at Rs 20/hr |
| CAR | MEDIUM, LARGE | Car in LARGE → billed at Rs 30/hr |
| BUS | LARGE only | Bus in LARGE → billed at Rs 30/hr |

---

## Build & Run

```bash
cd parking-lot/src
javac com/example/parkinglot/*.java
java com.example.parkinglot.App
```
