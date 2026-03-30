# Elevator System — Low-Level Design

## Problem
Design a multi-elevator system for a building with inside/outside button panels, pluggable scheduling algorithms (Strategy Pattern), weight limits, maintenance control, and operator management.

---

## Class Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                      <<singleton>>                           │
│                        Building                              │
├──────────────────────────────────────────────────────────────┤
│ - floors: List<Floor>                                        │
│ - elevators: List<Elevator>                                  │
│ - scheduler: ElevatorScheduler                               │
│ - maintenanceService: MaintenanceService                     │
│ - controller: ElevatorController                             │
├──────────────────────────────────────────────────────────────┤
│ + getInstance(): Building                                    │
│ + configure(numFloors, elevators, algorithm)                 │
│ + addFloor()                                                 │
│ + requestFromOutside(floor, Direction)                       │
│ + requestFromInside(elevatorId, floor)                       │
│ + step()                                                     │
│ + printStatus()                                              │
└──────────────────────────────────────────────────────────────┘
       │ has-many       │ has-many       │ uses          │ uses
       ▼                ▼                ▼               ▼
┌────────────┐  ┌──────────────┐  ┌─────────────┐  ┌──────────────────┐
│   Floor    │  │   Elevator   │  │  Elevator   │  │  Maintenance     │
├────────────┤  ├──────────────┤  │  Scheduler  │  │  Service         │
│ - floorNum │  │ - id         │  ├─────────────┤  ├──────────────────┤
│ - outside  │  │ - currentFlr │  │ - algorithm │  │ + setMaintenance │
│   Panel    │  │ - state      │  ├─────────────┤  │   (Elevator)     │
├────────────┤  │ - doorState  │  │ + dispatch  │  │ + resumeOperation│
│ + getPanel │  │ - curWeight  │  │   (Request, │  │   (Elevator)     │
└────────────┘  │ - maxWeight  │  │   elevators)│  └──────────────────┘
                │ - pending    │  │ + setAlgo   │
                │   Floors     │  │   (algo)    │
                ├──────────────┤  └──────┬──────┘
                │ + addDest()  │         │ uses
                │ + moveOneFlr │         ▼
                │ + canOperate │  ┌────────────────────────────┐
                │ + isOverwt() │  │     <<interface>>          │
                │ + openDoor() │  │   SchedulingAlgorithm      │
                │ + closeDoor()│  ├────────────────────────────┤
                │ + addWeight()│  │ + selectElevator(          │
                │ + removeWt() │  │     Request,               │
                │ + clearPend()│  │     List<Elevator>)        │
                └──────────────┘  │   : Elevator               │
                                  └─────────┬──────────────────┘
                                            │ implements
                          ┌─────────────────┼─────────────────┐
                          ▼                 ▼                 ▼
                ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
                │  Nearest     │  │  LeastBusy   │  │ DirectionBased   │
                │  Elevator    │  │  Strategy    │  │ Strategy         │
                │  Strategy    │  ├──────────────┤  ├──────────────────┤
                ├──────────────┤  │ min pending  │  │ 1. same-dir elev │
                │ min distance │  │ count wins   │  │ 2. nearest idle  │
                │ to source    │  └──────────────┘  │ 3. nearest any   │
                └──────────────┘                    └──────────────────┘
```

---

## Button Panels

```
┌──────────────────────────────────┐
│        <<interface>>             │
│      OutsideButtonPanel          │
├──────────────────────────────────┤
│ + pressUp()                      │
│ + pressDown()                    │
└────────────┬─────────────────────┘
             │ implements
             ▼
┌──────────────────────────────────┐
│    DefaultOutsideButtonPanel     │
├──────────────────────────────────┤
│ - floor: int                     │
│ - building: Building             │
├──────────────────────────────────┤
│ + pressUp()                      │    ┌──────────────────────────────┐
│ + pressDown()                    │    │      InsideButtonPanel       │
└──────────────────────────────────┘    ├──────────────────────────────┤
                                        │ - elevatorId: String         │
┌──────────────────────────────────┐    │ - building: Building         │
│        <<interface>>             │    │ - controller: ElevController │
│      ElevatorController          │    │ - elevator: Elevator         │
├──────────────────────────────────┤    ├──────────────────────────────┤
│ + handleRequest(Request, Elev)   │    │ + pressFloor(int)            │
│ + openDoor(Elevator)             │    │ + pressDoorOpen()            │
│ + closeDoor(Elevator)            │    │ + pressDoorClose()           │
│ + triggerAlarm(Elevator)         │    │ + pressAlarm()               │
└────────────┬─────────────────────┘    └──────────────────────────────┘
             │ implements
             ▼
┌──────────────────────────────────┐
│   DefaultElevatorController      │
├──────────────────────────────────┤
│ + handleRequest(Request, Elev)   │
│ + openDoor(Elevator)             │
│ + closeDoor(Elevator)            │
│ + triggerAlarm(Elevator)         │
└──────────────────────────────────┘
```

---

## Enums & Value Objects

```
┌────────────────────────┐  ┌────────────────────────┐  ┌────────────────────┐
│  <<enum>> ElevatorState│  │   <<enum>> Direction   │  │ <<enum>> DoorState │
├────────────────────────┤  ├────────────────────────┤  ├────────────────────┤
│ MOVING_UP              │  │ UP                     │  │ OPEN               │
│ MOVING_DOWN            │  │ DOWN                   │  │ CLOSED             │
│ IDLE                   │  └────────────────────────┘  └────────────────────┘
│ UNDER_MAINTENANCE      │
└────────────────────────┘

┌──────────────────────────────────┐
│        Request (immutable)       │
├──────────────────────────────────┤
│ - sourceFloor: int               │
│ - targetFloor: int               │
│ - direction: Direction           │
├──────────────────────────────────┤
│ + getSourceFloor(): int          │
│ + getTargetFloor(): int          │
│ + getDirection(): Direction      │
└──────────────────────────────────┘
```

---

## Relationships

```
Building (Singleton)
 ├── has-many → Floor
 │                └── has-a → OutsideButtonPanel (interface)
 │                                └── DefaultOutsideButtonPanel
 ├── has-many → Elevator
 │                └── state: ElevatorState, DoorState
 ├── has-a   → ElevatorScheduler
 │                └── uses → SchedulingAlgorithm (interface)
 │                              ├── NearestElevatorStrategy
 │                              ├── LeastBusyStrategy
 │                              └── DirectionBasedStrategy
 ├── has-a   → ElevatorController (interface)
 │                └── DefaultElevatorController
 └── has-a   → MaintenanceService

InsideButtonPanel
 ├── references → Elevator
 ├── references → Building
 └── references → ElevatorController

Request ← created by button presses, consumed by scheduler/controller
```

---

## Design Patterns

### 1. Singleton (Bill Pugh Holder) — Building
- Private constructor, static inner `Holder` class
- Lazy, thread-safe initialization
- Configured via `configure()` method

### 2. Strategy — SchedulingAlgorithm
- Interface with `selectElevator(Request, List<Elevator>)`
- Three implementations: Nearest, LeastBusy, DirectionBased
- Swappable at runtime via `ElevatorScheduler.setAlgorithm()`

---

## How Request Dispatch Works

```
Button Pressed (inside or outside)
  │
  ├── Outside Button (Up/Down on a floor)
  │     └── building.requestFromOutside(floor, direction)
  │           └── scheduler.dispatch(request, elevators)
  │                 └── algorithm.selectElevator(request, elevators)
  │                       ├── filters: canOperate() == true
  │                       ├── selects best elevator per strategy
  │                       └── elevator.addDestination(targetFloor)
  │
  └── Inside Button (floor number inside elevator)
        └── building.requestFromInside(elevatorId, floor)
              └── controller.handleRequest(request, elevator)
                    ├── checks canOperate()
                    └── elevator.addDestination(targetFloor)
```

## How Elevator Movement Works (Step Simulation)

```
building.step()
  │
  └── for each elevator:
        elevator.moveOneFloor()
          │
          ├── canOperate() == false?
          │     └── print warning (maintenance or overweight), skip
          │
          ├── pendingFloors empty?
          │     └── set state = IDLE, skip
          │
          ├── next destination > currentFloor?
          │     └── state = MOVING_UP, currentFloor++
          │
          ├── next destination < currentFloor?
          │     └── state = MOVING_DOWN, currentFloor--
          │
          └── arrived at destination?
                ├── remove from pendingFloors
                ├── openDoor() → closeDoor()
                └── if no more pending → state = IDLE
```

---

## Scheduling Strategies

| Strategy | Logic | Best For |
|----------|-------|----------|
| **NearestElevator** | Picks elevator closest to source floor | Minimizing wait time |
| **LeastBusy** | Picks elevator with fewest pending requests | Load balancing |
| **DirectionBased** | Prefers elevator moving in same direction, then idle, then nearest | Reducing unnecessary direction changes |

---

## Weight & Maintenance Guards

```
Elevator.canOperate()
  └── state != UNDER_MAINTENANCE && currentWeight <= maxWeight

If canOperate() == false:
  - moveOneFloor() → prints warning, does nothing
  - handleRequest() → rejects request
  - selectElevator() → skips this elevator entirely
```

| Constraint | Default | Effect |
|-----------|---------|--------|
| Weight limit | 700 kg | Elevator won't move if exceeded |
| Maintenance | operator-controlled | Elevator excluded from scheduling |

---

## Build & Run

```bash
cd elevator-system/src
javac com/example/elevator/*.java
java com.example.elevator.App
```
