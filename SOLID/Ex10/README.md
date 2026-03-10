# Ex10 — DIP: Campus Transport Booking

## 1. Context
A campus transport service books rides for students. It calculates distance, allocates a driver, and charges payment.

## 2. Current behavior
- `TransportBookingService.book` directly creates concrete `PaymentGateway`, `DriverAllocator`, `DistanceCalculator`
- Prints receipt

## 3. What’s wrong (at least 5 issues)
1. High-level booking logic depends on concrete services (hard-coded `new`).
2. Hard to test booking without real dependencies.
3. Hard to add a new payment method without editing booking logic.
4. Business rules (pricing) mixed with infrastructure calls.
5. No clear abstraction boundaries.

## 4. Your task
- Introduce abstractions and inject them into booking service.
- Preserve output.

## 5. Constraints
- Preserve receipt output format.
- Keep `TripRequest` fields unchanged.
- No external libs.

## 6. Acceptance criteria
- Booking service depends only on abstractions.
- Concrete implementations can be swapped without editing booking logic.

## 7. How to run
```bash
cd SOLID/Ex10/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Transport Booking ===
DistanceKm=6.0
Driver=DRV-17
Payment=PAID txn=TXN-9001
RECEIPT: R-501 | fare=90.00
```

## 9. Hints (OOP-only)
- Make the booking service accept interfaces in constructor.
- Keep pricing rules separate from infrastructure calls.

## 10. Stretch goals
- Add a “mock” allocator and gateway for tests without touching booking logic.




---

# Preparation Notes (Diagram Style)

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  GeoPoint from = new GeoPoint(12.97, 77.59);                 │
│  GeoPoint to   = new GeoPoint(12.93, 77.62);                 │
│                                                              │
│  TripRequest req = new TripRequest(                          │
│      "23BCS1007",                                            │
│      from,   // lat=12.97, lon=77.59                         │
│      to      // lat=12.93, lon=77.62                         │
│  );                                                          │
│                                                              │
│  new TransportBookingService().book(req);                    │
│        ▲                                                     │
│        no-arg constructor — takes NOTHING                    │
│        everything created INSIDE book()                      │
│                                                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│          TransportBookingService.book()                      │
│         THE GOD METHOD — creates and calls all deps          │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  STEP 1: Instantiate dependencies directly                   │
│  │  DistanceCalculator dist = new DistanceCalculator();      │
│  │  DriverAllocator     alloc= new DriverAllocator();        │
│  │  PaymentGateway      pay  = new PaymentGateway();         │
│  │                                                           │
│  STEP 2: Calculate distance                                  │
│  │  double km = dist.km(req.from, req.to);                   │
│  │  → Manhattan: |12.97-12.93| + |77.59-77.62|              │
│  │             = 0.04 + 0.03 = 0.07 × 200 = 14.0            │
│  │  → prints "DistanceKm=14.0"                               │
│  │                                                           │
│  STEP 3: Allocate driver                                     │
│  │  String driver = alloc.allocate(req.studentId);           │
│  │  → hardcoded "DRV-17"                                     │
│  │  → prints "Driver=DRV-17"                                 │
│  │                                                           │
│  STEP 4: Compute fare (inline business logic)                │
│  │  fare = 50.0 + 14.0 × 6.6666666667 = 143.33              │
│  │  fare = Math.round(fare × 100.0) / 100.0 = 143.33         │
│  │                                                           │
│  STEP 5: Charge payment                                      │
│  │  String txn = pay.charge(req.studentId, fare);            │
│  │  → hardcoded "TXN-9001"                                   │
│  │  → prints "Payment=PAID txn=TXN-9001"                     │
│  │                                                           │
│  STEP 6: Create and print receipt                            │
│  │  BookingReceipt r = new BookingReceipt("R-501", fare);    │
│  │  → prints "RECEIPT: R-501 | fare=143.33"                  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
         │              │              │
         ▼              ▼              ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ DistanceCalc-    │ │ DriverAllocator  │ │ PaymentGateway   │
│ ulator           │ │──────────────────│ │──────────────────│
│──────────────────│ │ allocate(id):    │ │ charge(id, amt): │
│ km(a, b):        │ │  returns         │ │  returns         │
│  Manhattan ×200  │ │  "DRV-17"        │ │  "TXN-9001"      │
│  rounded 1 dp    │ │  (hardcoded)     │ │  (hardcoded)     │
│  14.0 for sample │ │                  │ │  prints PAID...  │
└──────────────────┘ └──────────────────┘ └──────────────────┘

┌──────────────────┐
│ BookingReceipt   │
│──────────────────│
│ receiptId: String│
│ fare: double     │
│ toString()       │
│ → "RECEIPT: R-501│
│   | fare=143.33" │
└──────────────────┘

┌────────────────────────┐
│   ConsoleUi            │
│────────────────────────│
│ (formerly unused file) │
│                        │
│ DEAD CODE              │
│ no interfaces here     │
│ in the broken version  │
└────────────────────────┘
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: new DistanceCalculator() inside book() [DIP VIOLN] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: TransportBookingService.book() — first line          │
│                                                              │
│  DistanceCalculator dist = new DistanceCalculator();         │
│                           ▲                                  │
│                           concrete, hardcoded inside         │
│                                                              │
│  TransportBookingService ─── new ───► DistanceCalculator     │
│  (HIGH-level module)                  (LOW-level detail)     │
│                                                              │
│  WHY IT'S BAD:                                               │
│    Want GPS-based distance instead of Manhattan formula?     │
│      → Must open TransportBookingService and change new      │
│      → Can't inject a mock for unit tests                    │
│                                                              │
│  DIP says: high-level should not depend on low-level.        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: new DriverAllocator() inside book() [DIP VIOLATION]│
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: TransportBookingService.book()                       │
│                                                              │
│  DriverAllocator alloc = new DriverAllocator();              │
│                                                              │
│  TransportBookingService ─── new ───► DriverAllocator        │
│                                                              │
│  No interface exists. Can't swap in a test double.           │
│  Want load-balanced allocator for peak hours?                │
│    → Must edit the booking service itself.                   │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: new PaymentGateway() inside book()  [DIP VIOLATION]│
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: TransportBookingService.book()                       │
│                                                              │
│  PaymentGateway pay = new PaymentGateway();                  │
│                                                              │
│  TransportBookingService ─── new ───► PaymentGateway         │
│                                                              │
│  Want Razorpay instead of the default gateway?               │
│  Want to skip payment entirely in tests?                     │
│    → Can't. Service hardwires PaymentGateway.                │
│    → Test always hits payment code. No isolation.            │
└──────────────────────────────────────────────────────────────┘

DIP VIOLATIONS SUMMARY — 3 concrete couplings, zero abstractions:

  TransportBookingService ── new ──► DistanceCalculator
                            (hardcoded) no DistanceService interface
                                        can't swap distance algorithm

  TransportBookingService ── new ──► DriverAllocator
                            (hardcoded) no AllocationService interface
                                        can't mock for tests

  TransportBookingService ── new ──► PaymentGateway
                            (hardcoded) no PaymentService interface
                                        can't swap to Razorpay or mock


┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: Fare pricing logic buried inside book()            │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: TransportBookingService.book()                       │
│                                                              │
│  fare = 50.0 + km * 6.6666666667;                            │
│  fare = Math.round(fare * 100.0) / 100.0;                    │
│               ▲ magic numbers, inline                        │
│                                                              │
│  Business rules (base fare, per-km rate) are MIXED with      │
│  infrastructure calls (distance, allocation, payment).       │
│  Change pricing → edit book(). Add surge pricing → edit.     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: book() does too much              [SRP VIOLATION]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: TransportBookingService.book() — 6 steps in 1 method │
│                                                              │
│    STEP 1-3: instantiate 3 concrete deps ← construction      │
│    STEP 4:   calculate distance          ← infrastructure     │
│    STEP 5:   allocate driver             ← infrastructure     │
│    STEP 6:   compute fare                ← business rules     │
│    STEP 7:   charge payment              ← infrastructure     │
│    STEP 8:   create receipt and print    ← presentation       │
│                                                              │
│  4 reasons to change in ONE method:                          │
│    distance algo changes → edit book()                       │
│    pricing rules change  → edit book()                       │
│    payment method changes→ edit book()                       │
│    receipt format changes→ edit book()                       │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│  FIX: Introduce 3 interfaces in ConsoleUi.java               │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ┌─────────────────────────────────┐                         │
│  │  «interface» DistanceService    │  ← replaces calc dep    │
│  │─────────────────────────────────│                         │
│  │  double km(GeoPoint, GeoPoint)  │                         │
│  └────────────┬────────────────────┘                         │
│               │ implements                                    │
│               ▼                                              │
│  ┌─────────────────────────────────┐                         │
│  │  DistanceCalculator             │                         │
│  │  implements DistanceService     │                         │
│  └─────────────────────────────────┘                         │
│                                                              │
│  ┌─────────────────────────────────┐                         │
│  │  «interface» AllocationService  │  ← replaces alloc dep   │
│  │─────────────────────────────────│                         │
│  │  String allocate(String studentId)                        │
│  └────────────┬────────────────────┘                         │
│               │ implements                                    │
│               ▼                                              │
│  ┌─────────────────────────────────┐                         │
│  │  DriverAllocator                │                         │
│  │  implements AllocationService   │                         │
│  └─────────────────────────────────┘                         │
│                                                              │
│  ┌─────────────────────────────────────────────┐             │
│  │  «interface» PaymentService                 │  ← replaces │
│  │─────────────────────────────────────────────│             │
│  │  String charge(String studentId, double amt)│             │
│  └────────────┬────────────────────────────────┘             │
│               │ implements                                    │
│               ▼                                              │
│  ┌─────────────────────────────────┐                         │
│  │  PaymentGateway                 │                         │
│  │  implements PaymentService      │                         │
│  └─────────────────────────────────┘                         │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│          TransportBookingService (REFACTORED)                │
│          Now a PURE ORCHESTRATOR                             │
│──────────────────────────────────────────────────────────────│
│  Fields (all interfaces):                                    │
│    - DistanceService   dist                                  │
│    - AllocationService alloc                                 │
│    - PaymentService    pay                                   │
│──────────────────────────────────────────────────────────────│
│  Constructor:                                                │
│    TransportBookingService(                                  │
│        DistanceService dist,                                 │
│        AllocationService alloc,                              │
│        PaymentService pay)                                   │
│          ▲ no concrete types anywhere                        │
│──────────────────────────────────────────────────────────────│
│  book(TripRequest req):                                      │
│    double km      = dist.km(req.from, req.to);  ← interface  │
│    String driver  = alloc.allocate(req.studentId);           │
│    double fare    = 50.0 + km * 6.6666666667;    ← business  │
│    fare = Math.round(fare*100.0)/100.0;                      │
│    String txn     = pay.charge(req.studentId, fare);         │
│    new BookingReceipt("R-501", fare) → print                 │
│                                                              │
│  ZERO new keywords for deps. ZERO concrete types in fields.  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                  Main.java (wiring)                          │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  DistanceService   dist  = new DistanceCalculator();         │
│  AllocationService alloc = new DriverAllocator();            │
│  PaymentService    pay   = new PaymentGateway();             │
│                                                              │
│  new TransportBookingService(dist, alloc, pay).book(req);    │
│                                                              │
│  All concretes created OUTSIDE. None inside service.         │
└──────────────────────────────────────────────────────────────┘


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                                  AFTER
  ──────                                  ─────
  new DistanceCalculator() inside book → DistanceService injected
  new DriverAllocator() inside book    → AllocationService injected
  new PaymentGateway() inside book     → PaymentService injected
  No interfaces existed                → 3 interfaces in ConsoleUi.java
  ConsoleUi.java: dead code            → ConsoleUi.java: interface home
  book() constructs + orchestrates     → book() only orchestrates


WHY SWAP IS EASY NOW:

  GPS distance algo?   → GpsDistance implements DistanceService     → plug in Main
  Load balancer?       → SmartAllocator implements AllocationService → plug in Main
  Razorpay?            → RazorpayService implements PaymentService   → plug in Main
  Mock for tests?      → MockPayment implements PaymentService       → plug in Main

  TransportBookingService NEVER changes. DIP satisfied.
```

