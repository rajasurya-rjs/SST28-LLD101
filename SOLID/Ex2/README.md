# Ex2 — SRP: Campus Cafeteria Billing

## 1. Context
A cafeteria billing console generates invoices for student orders. It currently handles menu definition, tax logic, discount logic, invoice formatting, and persistence.

## 2. Current behavior
- Uses an in-memory menu
- Builds an order (hard-coded in `Main`)
- Computes subtotal, tax, discount, and total
- Prints an invoice and writes it to a file-like store (in-memory)

## 3. What’s wrong with the design (at least 5 issues)
1. `CafeteriaSystem.checkout` mixes menu lookup, pricing, tax rules, discount rules, printing, and persistence.
2. Tax rules are hard-coded and not extensible.
3. Discounts are hard-coded with ad-hoc conditions.
4. Invoice formatting is mixed with money calculations.
5. Persistence is a concrete class; hard to test without writing.
6. `Main` depends on too many internal details.

## 4. Your task (refactor plan)
Checkpoint A: Run and capture output.
Checkpoint B: Separate pricing/tax/discount computations into dedicated components.
Checkpoint C: Move invoice formatting out of `CafeteriaSystem`.
Checkpoint D: Introduce small abstractions to decouple persistence and rules.
Checkpoint E: Keep output identical.

## 5. Constraints
- Preserve exact invoice text and line order.
- Keep `MenuItem` and `OrderLine` public fields unchanged.
- No external libraries.

## 6. Acceptance criteria
- `CafeteriaSystem` orchestrates only; it should not format strings or encode tax/discount specifics.
- Adding a new discount should not require editing a long method.

## 7. How to run
```bash
cd SOLID/Ex2/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Cafeteria Billing ===
Invoice# INV-1001
- Veg Thali x2 = 160.00
- Coffee x1 = 30.00
Subtotal: 190.00
Tax(5%): 9.50
Discount: -10.00
TOTAL: 189.50
Saved invoice: INV-1001 (lines=7)
```

## 9. Hints (OOP-only)
- Keep “rules” behind interfaces so new rules can be added without editing a big method.
- Keep formatting as a separate responsibility.

## 10. Stretch goals
- Add a second invoice for a staff member with different discount policy.



---

# Preparation Notes (Diagram Style)

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│  1. Creates CafeteriaSystem                                  │
│  2. Adds menu:                                               │
│       M1 → Veg Thali  ₹80                                   │
│       C1 → Coffee     ₹30                                   │
│       S1 → Sandwich   ₹60                                   │
│  3. Order: 2x Veg Thali + 1x Coffee                         │
│  4. Calls sys.checkout("student", order)                     │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              CafeteriaSystem.checkout()                       │
│          THE GOD METHOD — does everything                    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  STEP 1: Generate ID                                         │
│  │  invId = "INV-1001"                                       │
│  │                                                           │
│  STEP 2: Calculate line totals                               │
│  │  Veg Thali: 80 × 2 = 160                                 │
│  │  Coffee:    30 × 1 =  30                                 │
│  │  subtotal = ₹190                                          │
│  │                                                           │
│  STEP 3: Tax (static call)                                   │
│  │  TaxRules.taxPercent("student") → 5%                      │
│  │  tax = 190 × 0.05 = ₹9.50                                │
│  │                                                           │
│  STEP 4: Discount (static call)                              │
│  │  DiscountRules.discountAmount("student", 190, 2)          │
│  │  subtotal ≥ 180 → discount = ₹10.00                      │
│  │                                                           │
│  STEP 5: Total                                               │
│  │  total = 190 + 9.50 - 10 = ₹189.50                       │
│  │                                                           │
│  STEP 6: Format invoice (StringBuilder — INLINE)             │
│  │  "Invoice# INV-1001"                                      │
│  │  "- Veg Thali x2 = 160.00"                               │
│  │  "- Coffee x1 = 30.00"                                   │
│  │  "Subtotal: 190.00"                                       │
│  │  "Tax(5%): 9.50"                                          │
│  │  "Discount: -10.00"                                       │
│  │  "TOTAL: 189.50"                                          │
│  │                                                           │
│  STEP 7: InvoiceFormatter.identityFormat(str) → returns str  │
│  │        ^^^ DOES NOTHING — dead code                       │
│  │                                                           │
│  STEP 8: System.out.print(printable)                         │
│  │                                                           │
│  STEP 9: store.save(invId, printable)                        │
│          store = new FileStore() ← hardcoded inside class    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌────────────────┐  ┌──────────────────┐  ┌────────────────────┐
│   TaxRules     │  │  DiscountRules   │  │    FileStore       │
│────────────────│  │──────────────────│  │────────────────────│
│ static method  │  │ static method    │  │ NOT an interface   │
│ taxPercent()   │  │ discountAmount() │  │ created internally │
│                │  │                  │  │ with new FileStore()│
│ "student" → 5% │  │ "student" +     │  │                    │
│ "staff"   → 2% │  │  sub≥180 → ₹10  │  │ save(name,content) │
│ default   → 8% │  │ "staff" +       │  │ countLines(name)   │
│                │  │  lines≥3 → ₹15  │  │                    │
│                │  │  else    → ₹5   │  │ stores in HashMap  │
└────────────────┘  └──────────────────┘  └────────────────────┘

┌────────────────────────┐
│   InvoiceFormatter     │
│────────────────────────│
│ identityFormat(s):     │
│   return s;            │
│                        │
│ DEAD CODE — does       │
│ nothing. Real format   │
│ is inside checkout()   │
└────────────────────────┘
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│         CafeteriaSystem.checkout() — GOD METHOD              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌───────────┐│
│  │  PRICING   │ │    TAX     │ │  DISCOUNT  │ │ FORMATTING││
│  │  subtotal  │ │  5% logic  │ │  ₹10 logic │ │ StringBuilder
│  │  calc      │ │            │ │            │ │ inline    ││
│  └────────────┘ └────────────┘ └────────────┘ └───────────┘│
│                                                              │
│  ┌────────────┐ ┌────────────────────────────────────────┐  │
│  │  PRINTING  │ │          PERSISTENCE                   │  │
│  │  sysout    │ │  FileStore.save() + countLines()       │  │
│  └────────────┘ └────────────────────────────────────────┘  │
│                                                              │
│  SRP VIOLATION — 4 reasons to change in ONE class:          │
│     1. Tax rules change       → edit checkout()             │
│     2. Discount policy change → edit checkout()             │
│     3. Invoice format change  → edit checkout()             │
│     4. Storage change         → edit checkout()             │
└──────────────────────────────────────────────────────────────┘


DIP VIOLATIONS — 3 concrete couplings, zero abstractions:

  CafeteriaSystem ──── static call ────► TaxRules
                       (can't swap)       taxPercent() is static
                       (can't mock)       permanently married

  CafeteriaSystem ──── static call ────► DiscountRules
                       (can't swap)       discountAmount() is static
                       (can't mock)       permanently married

  CafeteriaSystem ──── new FileStore() ── ► FileStore
                       (created inside)    not injected
                       (no interface)      can't swap to DB


DEAD CODE:

  InvoiceFormatter.identityFormat()
  │
  │  input: "Invoice# INV-1001..."
  │  output: "Invoice# INV-1001..."  ← SAME STRING
  │
  └── Pretends to format but does nothing.
      Real formatting is inside checkout().
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│                      Main.java (wiring)                      │
│──────────────────────────────────────────────────────────────│
│  InvoiceStore store       = new FileStore();                 │
│  TaxCalculator taxCalc    = new TaxRules();                  │
│  DiscountCalculator disc  = new DiscountRules();             │
│  InvoiceFormatter fmt     = new InvoiceFormatter();          │
│                                                              │
│  sys = new CafeteriaSystem(store, taxCalc, disc, fmt);       │
│                                                              │
│  All created EXTERNALLY, injected via CONSTRUCTOR            │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              CafeteriaSystem (REFACTORED)                     │
│              Now a PURE ORCHESTRATOR                          │
│──────────────────────────────────────────────────────────────│
│  Fields (all interfaces/abstractions):                       │
│    - InvoiceStore store                                      │
│    - TaxCalculator taxCalculator                             │
│    - DiscountCalculator discountCalculator                   │
│    - InvoiceFormatter formatter                              │
│──────────────────────────────────────────────────────────────│
│  checkout(customerType, lines):                              │
│    1. Calculate subtotal (core billing — stays here)         │
│    2. taxCalc.taxPercent(customerType)    → get tax          │
│    3. discountCalc.discountAmount(...)    → get discount     │
│    4. formatter.format(invId, lines, ...) → get string       │
│    5. System.out.print(printable)                            │
│    6. store.save(invId, printable)                           │
│                                                              │
│  NO formatting code. NO static calls. NO concrete deps.     │
└──────┬──────────────┬────────────────┬──────────────┬────────┘
       │              │                │              │
       ▼              ▼                ▼              ▼
┌─────────────┐ ┌───────────────┐ ┌────────────┐ ┌──────────────┐
│ «interface» │ │ «interface»   │ │ «interface»│ │ InvoiceFormat│
│ TaxCalc     │ │ DiscountCalc  │ │ InvoiceStr │ │ -ter         │
│─────────────│ │───────────────│ │────────────│ │──────────────│
│ taxPercent  │ │ discountAmt   │ │ save()     │ │ format()     │
│ (custType)  │ │ (custType,    │ │ countLines │ │ builds full  │
│             │ │  sub, lines)  │ │ ()         │ │ invoice str  │
└──────┬──────┘ └───────┬───────┘ └─────┬──────┘ │ with all     │
       │                │               │        │ line items,  │
       ▼                ▼               ▼        │ tax, disc,   │
┌─────────────┐ ┌───────────────┐ ┌────────────┐│ total        │
│ TaxRules    │ │ DiscountRules │ │ FileStore  ││ (REAL work)  │
│ implements  │ │ implements    │ │ implements │└──────────────┘
│ TaxCalc     │ │ DiscountCalc  │ │ InvoiceStr │
│─────────────│ │───────────────│ │────────────│
│ student→ 5% │ │ student+      │ │ HashMap    │
│ staff  → 2% │ │  sub≥180→₹10  │ │ save()     │
│ default→ 8% │ │ staff+        │ │ countLines │
│             │ │  lines≥3→₹15  │ │ ()         │
└─────────────┘ └───────────────┘ └────────────┘


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                           AFTER
  ──────                           ─────
  TaxRules: static method    →    TaxRules implements TaxCalculator
  DiscountRules: static      →    DiscountRules implements DiscountCalculator
  FileStore: new FileStore() →    FileStore implements InvoiceStore
  InvoiceFormatter: FAKE     →    InvoiceFormatter: REAL format()
  checkout(): does everything→    checkout(): only orchestrates


WHY SWAP IS EASY NOW:

  Want GST tax?     → GSTCalc implements TaxCalculator     → plug in Main
  Want festival     → FestivalDisc implements DiscountCalc  → plug in Main
    discount?
  Want real DB?     → DbStore implements InvoiceStore       → plug in Main
  Want HTML invoice?→ HtmlFormatter (new class)             → plug in Main

  CafeteriaSystem NEVER changes. SRP + DIP satisfied.
```

