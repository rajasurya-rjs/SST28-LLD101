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

# Preparation Notes

## 11. What does this program do?

A campus cafeteria billing system. A student orders food, the system generates an invoice with tax, discount, and saves it.

### Flow
- `Main` creates `CafeteriaSystem` and adds 3 menu items: Veg Thali (₹80), Coffee (₹30), Sandwich (₹60)
- Student places an order: 2x Veg Thali + 1x Coffee
- `checkout("student", order)` is called, which inside one single method:
  - Generates invoice ID → `INV-1001`
  - Loops through order lines, looks up menu, calculates line totals → (160 + 30 = ₹190)
  - Calls `TaxRules.taxPercent("student")` → 5% → tax = ₹9.50
  - Calls `DiscountRules.discountAmount("student", 190, 2)` → subtotal ≥ 180 → discount = ₹10
  - Calculates total → 190 + 9.50 - 10 = ₹189.50
  - Formats entire invoice string inline using `StringBuilder`
  - Prints invoice to console
  - Saves invoice to `FileStore` (in-memory map)
  - Prints "Saved invoice: INV-1001 (lines=7)"

### Supporting classes
- `MenuItem` — data holder (id, name, price)
- `OrderLine` — data holder (itemId, qty)
- `TaxRules` — static method, returns tax % based on customer type
- `DiscountRules` — static method, returns discount amount based on customer type, subtotal, and line count
- `InvoiceFormatter` — has one method `identityFormat()` that returns input unchanged (dead code)
- `FileStore` — in-memory persistence using a `HashMap`

## 12. Design issues

### Issue 1 — `checkout()` is a god method → SRP violation
One method handles pricing, tax, discount, formatting, printing, and persistence. That's 6 responsibilities. If tax rules change, you edit this method. If invoice format changes, you edit the same method. 4 different reasons to change in one class.

### Issue 2 — Tax rules via static call → DIP violation
Line 24: `TaxRules.taxPercent(customerType)` is a static method call. `CafeteriaSystem` (high-level) is directly coupled to `TaxRules` (low-level). No abstraction in between. Can't swap tax strategy. Can't mock in tests.

### Issue 3 — Discount rules via static call → DIP violation
Line 27: `DiscountRules.discountAmount(...)` — same problem. Hardwired to a concrete implementation. Want a different discount strategy? You're stuck editing the same class.

### Issue 4 — Formatting mixed with calculations → SRP violation
Lines 13-34: `StringBuilder` invoice construction is woven between pricing math. Two concerns tangled. Can't change invoice layout without touching billing logic.

### Issue 5 — `FileStore` is concrete and created internally → DIP violation
Line 5: `private final FileStore store = new FileStore()`. Not injected. Not behind an interface. High-level module creates its own low-level dependency. Can't swap storage. Can't test without writing.

### Issue 6 — `InvoiceFormatter` is fake dead code → SRP violation (indirect)
`identityFormat()` returns input unchanged. Actual formatting lives inside `checkout()`. The formatting responsibility is still inside `CafeteriaSystem`. SRP remains broken despite this class existing.

## 13. Which SOLID principle is violated?

### Primary: SRP (Single Responsibility Principle)
> "A class should have only one reason to change."

`CafeteriaSystem` has 4 reasons to change: tax rules, discount policy, invoice format, storage mechanism.

### Secondary: DIP (Dependency Inversion Principle)
> "High-level modules should not depend on low-level modules. Both should depend on abstractions."

`FileStore` is concrete and created internally. `TaxRules` and `DiscountRules` are accessed via static calls. No abstractions anywhere.

## 14. My fix

### 14a. Created `TaxCalculator` interface
```java
public interface TaxCalculator {
    double taxPercent(String customerType);
}
```
`TaxRules` now implements `TaxCalculator`. No longer static. Can create `GSTCalculator implements TaxCalculator` tomorrow with zero change to `CafeteriaSystem`.

### 14b. Created `DiscountCalculator` interface
```java
public interface DiscountCalculator {
    double discountAmount(String customerType, double subtotal, int distinctLines);
}
```
`DiscountRules` now implements `DiscountCalculator`. Want a festival discount? Create `FestivalDiscount implements DiscountCalculator`. Plug in. Done.

### 14c. Created `InvoiceStore` interface
```java
public interface InvoiceStore {
    void save(String name, String content);
    int countLines(String name);
}
```
`FileStore` now implements `InvoiceStore`. Want database storage? Create `DbStore implements InvoiceStore`. `CafeteriaSystem` doesn't know or care.

### 14d. Made `InvoiceFormatter` real
All `StringBuilder` formatting extracted from `checkout()` into `InvoiceFormatter.format()`. Takes 8 parameters (invId, lines, menu, subtotal, taxPct, tax, discount, total). Returns the formatted invoice string. The original was a fake passthrough. Now it owns formatting completely.

### 14e. Refactored `CafeteriaSystem`
Constructor takes all 4 dependencies via injection:
```java
public CafeteriaSystem(InvoiceStore store, TaxCalculator taxCalculator,
        DiscountCalculator discountCalculator, InvoiceFormatter formatter)
```
`checkout()` becomes a pure orchestrator:
1. Calculate subtotal (core billing logic — stays here)
2. Ask `taxCalculator` → get tax
3. Ask `discountCalculator` → get discount
4. Ask `formatter` → get formatted string
5. Print it
6. Ask `store` → save it

No formatting code. No static calls. No concrete dependencies.

### 14f. Wiring in `Main`
```java
InvoiceStore store = new FileStore();
TaxCalculator taxCalc = new TaxRules();
DiscountCalculator discountCalc = new DiscountRules();
InvoiceFormatter formatter = new InvoiceFormatter();
CafeteriaSystem sys = new CafeteriaSystem(store, taxCalc, discountCalc, formatter);
```
All components created externally and injected. `CafeteriaSystem` has no idea which concrete classes it uses.

