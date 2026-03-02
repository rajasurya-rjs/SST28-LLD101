# Ex4 — OCP: Hostel Fee Calculator

## 1. Context
Hostel fees depend on room type and add-ons. New room types and add-ons will be introduced.

## 2. Current behavior
- Uses `BookingRequest` with roomType and add-ons
- Calculates monthly fee + one-time deposit
- Prints a receipt and saves booking

## 3. What’s wrong (at least 5 issues)
1. `HostelFeeCalculator.calculate` is a switch-case on room types.
2. Add-ons are handled with repeated branching logic.
3. Adding a room type requires editing the big switch (OCP violation).
4. Money arithmetic is scattered and formatted inconsistently.
5. Calculator also prints and persists booking data.

## 4. Your task
Checkpoint A: Run and capture output.
Checkpoint B: Encapsulate room pricing and add-on pricing behind abstractions.
Checkpoint C: Remove switch-case from main calculation logic.
Checkpoint D: Preserve output.

## 5. Constraints
- Keep receipt formatting identical.
- Keep `BookingRequest` fields unchanged.
- No external libs.

## 6. Acceptance criteria
- New room type can be added without editing a switch in calculator.
- Add-ons can be added without editing the core fee algorithm.

## 7. How to run
```bash
cd SOLID/Ex4/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Hostel Fee Calculator ===
Room: DOUBLE | AddOns: [LAUNDRY, MESS]
Monthly: 16000.00
Deposit: 5000.00
TOTAL DUE NOW: 21000.00
Saved booking: H-7781
```

## 9. Hints (OOP-only)
- Prefer a list of pricing components (room + add-ons) that contribute to totals.
- Keep printing separate from calculation.

## 10. Stretch goals
- Add “late fee” rule without editing the main calculation loop.



# Preparation Notes (Diagram Style)
## OCP: Hostel Fee Calculator
## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BookingRequest req = new BookingRequest(                     │
│      LegacyRoomTypes.DOUBLE,       // roomType = int 2       │
│      List.of(AddOn.LAUNDRY, AddOn.MESS)  // add-ons list     │
│  );                                                          │
│                                                              │
│  FakeBookingRepo repo = new FakeBookingRepo();  ⚠ CONCRETE   │
│                                                              │
│  HostelFeeCalculator calc = new HostelFeeCalculator(repo);   │
│  calc.process(req);                                          │
│                                                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│     HostelFeeCalculator                                      │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  Fields:                                                     │
│    private final FakeBookingRepo repo;  ⚠ CONCRETE CLASS     │
│                                                              │
│  Constructor:                                                │
│    HostelFeeCalculator(FakeBookingRepo repo)                 │
│                          ▲ no interface                      │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  process(BookingRequest req):                                │
│  │                                                           │
│  │ STEP 1: monthly = calculateMonthly(req)  ← see below     │
│  │                                                           │
│  │ STEP 2: deposit = new Money(5000.00)     ← hardcoded     │
│  │                                                           │
│  │ STEP 3: ReceiptPrinter.print(req, monthly, deposit)       │
│  │         → “Room: DOUBLE | AddOns: [LAUNDRY, MESS]”       │
│  │         → “Monthly: 16500.00”                             │
│  │         → “Deposit: 5000.00”                              │
│  │         → “TOTAL DUE NOW: 21500.00”                       │
│  │                                                           │
│  │ STEP 4: bookingId = “H-7781”                              │
│  │                                                           │
│  │ STEP 5: repo.save(“H-7781”, req, monthly, deposit)        │
│  │         → “Saved booking: H-7781”                         │
│  │                                                           │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  calculateMonthly(BookingRequest req):                        │
│  │                                                           │
│  │ ⚠ ROOM PRICING — switch-case block:                      │
│  │ ┌─────────────────────────────────────────────────┐       │
│  │ │  switch (req.roomType) {                        │       │
│  │ │      case SINGLE (1) → base = 14000.0           │       │
│  │ │      case DOUBLE (2) → base = 15000.0  ◄── HIT │       │
│  │ │      case TRIPLE (3) → base = 12000.0           │       │
│  │ │      default         → base = 16000.0           │       │
│  │ │  }                                              │       │
│  │ └─────────────────────────────────────────────────┘       │
│  │                                                           │
│  │ ⚠ ADD-ON PRICING — if-else chain:                        │
│  │ ┌─────────────────────────────────────────────────┐       │
│  │ │  for (AddOn a : req.addOns) {                   │       │
│  │ │      if (a == MESS)         add += 1000.0       │       │
│  │ │      else if (a == LAUNDRY) add += 500.0 ◄── HIT│      │
│  │ │      else if (a == GYM)     add += 300.0        │       │
│  │ │  }                                              │       │
│  │ └─────────────────────────────────────────────────┘       │
│  │                                                           │
│  │ return Money(15000 + 500 + 1000) = Money(16500.00)        │
│  │                                                           │
└──────────────────────────────────────────────────────────────┘
         │                              │
         ▼                              ▼
┌────────────────────┐      ┌────────────────────────┐
│  ReceiptPrinter    │      │  FakeBookingRepo       │
│────────────────────│      │────────────────────────│
│ static print()     │      │ NO interface           │
│ takes req, monthly,│      │ save(id, req, m, d)    │
│ deposit            │      │ → prints “Saved...”    │
│ prints 4 lines     │      │                        │
└────────────────────┘      └────────────────────────┘

┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│ BookingRequest  │  │ LegacyRoomTypes│  │ AddOn (enum)   │
│────────────────│  │────────────────│  │────────────────│
│ roomType: int  │  │ SINGLE = 1     │  │ MESS           │
│ addOns: List   │  │ DOUBLE = 2     │  │ LAUNDRY        │
│ <AddOn>        │  │ TRIPLE = 3     │  │ GYM            │
│                │  │ DELUXE = 4     │  │                │
│                │  │ nameOf(int)→str│  │                │
└────────────────┘  └────────────────┘  └────────────────┘

┌────────────────┐
│ Money          │
│────────────────│
│ amount: double │
│ plus(Money)    │
│ toString()     │
│ → “16500.00”   │
└────────────────┘
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: switch on roomType               [OCP VIOLATION]   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: calculateMonthly() → the switch block                │
│                                                              │
│  switch (req.roomType) {                                     │
│      SINGLE → 14000                                          │
│      DOUBLE → 15000                                          │
│      TRIPLE → 12000                                          │
│      default → 16000                                         │
│  }                                                           │
│                                                              │
│  WHY IT'S BAD:                                               │
│    Want to add SUITE room (₹20000)?                          │
│      → Must open calculateMonthly()                          │
│      → Must add: case SUITE → base = 20000.0                │
│      → Risk breaking existing cases                          │
│                                                              │
│    Want to add AC room (₹18000)?                             │
│      → Must open SAME method AGAIN                           │
│      → Add another case                                      │
│                                                              │
│    Every. New. Room. Type. = edit this switch.                │
│    Class is NOT closed for modification.                     │
│                                                              │
│  OCP says: open for extension, closed for modification.      │
│  Here:     must MODIFY the class to EXTEND functionality.    │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: if-else chain for add-ons        [OCP VIOLATION]   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: calculateMonthly() → the for-loop with if-else       │
│                                                              │
│  for (AddOn a : req.addOns) {                                │
│      if (a == MESS)         add += 1000.0                    │
│      else if (a == LAUNDRY) add += 500.0                     │
│      else if (a == GYM)     add += 300.0                     │
│  }                                                           │
│                                                              │
│  WHY IT'S BAD:                                               │
│    Want to add WIFI add-on (₹200)?                           │
│      → Must open SAME calculateMonthly() method              │
│      → Must add: else if (a == WIFI) add += 200.0           │
│                                                              │
│    TWO different OCP violations in ONE method:               │
│      switch for rooms + if-else for add-ons                  │
│      Both force modification for extension.                  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: Concrete repo dependency         [DIP VIOLATION]   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: HostelFeeCalculator field + constructor               │
│                                                              │
│  private final FakeBookingRepo repo;                         │
│                 ▲                                            │
│                 concrete class — NO interface                 │
│                                                              │
│  HostelFeeCalculator ─── depends on ───► FakeBookingRepo     │
│  (HIGH-level module)                      (LOW-level module) │
│                                                              │
│  WHY IT'S BAD:                                               │
│    Want real database storage?                               │
│      → Must change field type in HostelFeeCalculator         │
│      → Can't swap FakeBookingRepo for DbBookingRepo          │
│      → Can't mock for unit tests                             │
│                                                              │
│  DIP says: depend on ABSTRACTIONS, not concrete classes.     │
│  Here:     high-level directly depends on low-level.         │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: Magic numbers                    [CODE SMELL]      │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: calculateMonthly() + process()                        │
│                                                              │
│  14000.0  15000.0  12000.0  16000.0  ← room prices          │
│  1000.0   500.0    300.0             ← add-on prices         │
│  5000.00                             ← deposit               │
│                                                              │
│  No names. No constants. No config. Just raw numbers.        │
│  If MESS price changes from 1000→1200, you hunt for 1000.0  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: process() does too much          [SRP VIOLATION]   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: process() method — 5 responsibilities:               │
│                                                              │
│    STEP 1: calculateMonthly()  ← pricing logic               │
│    STEP 2: Money(5000.00)      ← deposit rule                │
│    STEP 3: ReceiptPrinter      ← presentation                │
│    STEP 4: generate ID         ← ID logic                    │
│    STEP 5: repo.save()         ← persistence                 │
│                                                              │
│  3 reasons to change in ONE class:                           │
│    pricing changes  → edit this class                        │
│    receipt changes   → edit this class                        │
│    storage changes   → edit this class                        │
│                                                              │
│  SRP says: ONE reason to change per class.                   │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│  FIX for ISSUE 1 → RoomPricing interface (kills the switch)  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE (in calculateMonthly):                               │
│    switch (req.roomType) {                                   │
│        SINGLE → 14000 / DOUBLE → 15000 / ...                │
│    }                                                         │
│                                                              │
│  AFTER:                                                      │
│  ┌──────────────────────────────┐                            │
│  │  «interface» RoomPricing     │                            │
│  │──────────────────────────────│                            │
│  │  boolean supports(int type)  │  ← “am I the right one?”  │
│  │  double basePrice()          │  ← “here's my price”      │
│  └──────────────┬───────────────┘                            │
│                 │ implements                                  │
│      ┌──────────┼──────────┬──────────┐                      │
│      ▼          ▼          ▼          ▼                      │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                │
│  │Single  │ │Double  │ │Triple  │ │Deluxe  │                │
│  │Room    │ │Room    │ │Room    │ │Room    │                │
│  │Pricing │ │Pricing │ │Pricing │ │Pricing │                │
│  │────────│ │────────│ │────────│ │────────│                │
│  │supports│ │supports│ │supports│ │supports│                │
│  │(SINGLE)│ │(DOUBLE)│ │(TRIPLE)│ │(DELUXE)│                │
│  │→14000  │ │→15000  │ │→12000  │ │→16000  │                │
│  └────────┘ └────────┘ └────────┘ └────────┘                │
│                                                              │
│  In calculator — switch REPLACED with loop:                  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  for (RoomPricing rp : roomPricings) {                 │  │
│  │      if (rp.supports(req.roomType)) {                  │  │
│  │          base = rp.basePrice();                        │  │
│  │          break;                                        │  │
│  │      }                                                 │  │
│  │  }                                                     │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  SUITE room? → new SuiteRoomPricing class, add to list.      │
│  Calculator code? → UNTOUCHED. Switch is GONE.               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX for ISSUE 2 → AddOnPricing interface (kills if-else)    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE (in calculateMonthly):                               │
│    if (a == MESS) add += 1000                                │
│    else if (a == LAUNDRY) add += 500                         │
│    else if (a == GYM) add += 300                             │
│                                                              │
│  AFTER:                                                      │
│  ┌──────────────────────────────┐                            │
│  │  «interface» AddOnPricing    │                            │
│  │──────────────────────────────│                            │
│  │  boolean supports(AddOn a)   │  ← “am I the right one?”  │
│  │  double price()              │  ← “here's my price”      │
│  └──────────────┬───────────────┘                            │
│                 │ implements                                  │
│      ┌──────────┼──────────┐                                 │
│      ▼          ▼          ▼                                 │
│  ┌────────┐ ┌────────┐ ┌────────┐                            │
│  │Mess    │ │Laundry │ │Gym     │                            │
│  │AddOn   │ │AddOn   │ │AddOn   │                            │
│  │Pricing │ │Pricing │ │Pricing │                            │
│  │→1000   │ │→500    │ │→300    │                            │
│  └────────┘ └────────┘ └────────┘                            │
│                                                              │
│  In calculator — if-else REPLACED with loop:                 │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  for (AddOn a : req.addOns) {                          │  │
│  │      for (AddOnPricing ap : addOnPricings) {           │  │
│  │          if (ap.supports(a)) {                         │  │
│  │              addOnTotal += ap.price();                  │  │
│  │              break;                                    │  │
│  │          }                                             │  │
│  │      }                                                 │  │
│  │  }                                                     │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  WIFI add-on? → new WifiAddOnPricing class, add to list.     │
│  Calculator code? → UNTOUCHED. If-else is GONE.              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX for ISSUE 3 → BookingRepository interface (kills DIP)   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    private final FakeBookingRepo repo; ← concrete            │
│                                                              │
│  AFTER:                                                      │
│    private final BookingRepository repo; ← interface!        │
│                                                              │
│  ┌────────────────────────────────────────┐                  │
│  │  «interface» BookingRepository          │                  │
│  │────────────────────────────────────────│                  │
│  │  void save(id, req, monthly, deposit)  │                  │
│  └────────────────────┬───────────────────┘                  │
│                       ▲ implements                           │
│  ┌────────────────────┴───────────────────┐                  │
│  │  FakeBookingRepo                       │                  │
│  └────────────────────────────────────────┘                  │
│                                                              │
│  Real DB? → DbRepo implements BookingRepository.             │
│  Calculator code? → UNTOUCHED.                               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  Wiring in Main — all injected via constructor               │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  List<RoomPricing> rooms = List.of(                          │
│      new SingleRoomPricing(),    // SINGLE → 14000           │
│      new DoubleRoomPricing(),    // DOUBLE → 15000           │
│      new TripleRoomPricing(),    // TRIPLE → 12000           │
│      new DeluxeRoomPricing()     // DELUXE → 16000           │
│  );                                                          │
│                                                              │
│  List<AddOnPricing> addOns = List.of(                        │
│      new MessAddOnPricing(),     // MESS    → 1000           │
│      new LaundryAddOnPricing(),  // LAUNDRY → 500            │
│      new GymAddOnPricing()       // GYM     → 300            │
│  );                                                          │
│                                                              │
│  BookingRepository repo = new FakeBookingRepo();             │
│                                                              │
│  calc = new HostelFeeCalculator(rooms, addOns, repo);        │
│             ▲ constructor now takes interfaces/lists          │
│                                                              │
│  Everything created OUTSIDE, injected IN.                    │
└──────────────────────────────────────────────────────────────┘


OCP PROOF:

  SUITE room?   → new SuiteRoomPricing → add to list
                  Calculator? NEVER TOUCHED  ✓

  WIFI add-on?  → new WifiAddOnPricing → add to list
                  Calculator? NEVER TOUCHED  ✓

  Real DB?      → DbRepo implements BookingRepository
                  Calculator? NEVER TOUCHED  ✓

  OPEN for extension. CLOSED for modification.
```

