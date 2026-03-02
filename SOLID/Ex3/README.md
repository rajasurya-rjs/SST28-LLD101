# Ex3 — OCP: Placement Eligibility Rules Engine

## 1. Context
Placement eligibility depends on multiple rules: CGR threshold, attendance percentage, earned credits, and disciplinary flags. More rules will be added later.

## 2. Current behavior
- Evaluates a `StudentProfile`
- Returns ELIGIBLE or NOT_ELIGIBLE with reasons
- Prints a report

## 3. What’s wrong with the design (at least 5 issues)
1. `EligibilityEngine.evaluate` is a long if/else chain with mixed responsibilities.
2. Adding a new rule requires editing the same method (risk of regressions).
3. Rule configuration is hard-coded.
4. Reasons formatting is mixed with evaluation.
5. Engine does persistence-ish logging via `FakeEligibilityStore`.
6. Type/flag logic is scattered.

## 4. Your task
Checkpoint A: Run and capture output.
Checkpoint B: Move each rule to its own unit (class) behind a shared abstraction.
Checkpoint C: Make it possible to add a new rule without editing the main evaluation logic.
Checkpoint D: Keep report text identical.

## 5. Constraints
- Keep `StudentProfile` fields unchanged.
- Preserve order of reasons in output.
- No external libraries.

## 6. Acceptance criteria
- New eligibility rule can be added by creating a new class and wiring it with minimal edits.
- No giant conditional chains.

## 7. How to run
```bash
cd SOLID/Ex3/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Placement Eligibility ===
Student: Ayaan (CGR=8.10, attendance=72, credits=18, flag=NONE)
RESULT: NOT_ELIGIBLE
- attendance below 75
Saved evaluation for roll=23BCS1001
```

## 9. Hints (OOP-only)
- Use a list of rule objects and iterate.
- Keep rules small and single-purpose.

## 10. Stretch goals
- Read rule thresholds from a config object without editing rule logic.

  

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│  1. Creates StudentProfile for Ayaan:                        │
│       rollNo     = "23BCS1001"                               │
│       name       = "Ayaan"                                   │
│       cgr        = 8.10                                      │
│       attendance = 72                                        │
│       credits    = 18                                        │
│       flag       = NONE (0)                                  │
│                                                              │
│  2. Creates EligibilityEngine(new FakeEligibilityStore())    │
│                               ^^^^^^^^^^^^^^^^^^^^^^^^       │
│                               concrete class, not interface  │
│  3. Calls engine.runAndPrint(s)                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              EligibilityEngine                                │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    store: FakeEligibilityStore  ← concrete class, no iface  │
│──────────────────────────────────────────────────────────────│
│  runAndPrint(s):                                             │
│    1. ReportPrinter p = new ReportPrinter()                  │
│       ^^^ created internally, NOT injected                   │
│    2. EligibilityEngineResult r = evaluate(s)                │
│    3. p.print(s, r)   → prints student info + result         │
│    4. store.save(rollNo, status) → prints "Saved..."         │
│──────────────────────────────────────────────────────────────│
│  evaluate(s):  ← THE PROBLEM METHOD (if/else-if chain)      │
│                                                              │
│    List<String> reasons = new ArrayList<>();                 │
│    String status = "ELIGIBLE";                               │
│                                                              │
│    if (s.disciplinaryFlag != NONE) {                         │
│    │   status = "NOT_ELIGIBLE";                              │
│    │   reasons.add("disciplinary flag present");             │
│    │                                                         │
│    } else if (s.cgr < 8.0) {          ← magic number        │
│    │   status = "NOT_ELIGIBLE";                              │
│    │   reasons.add("CGR below 8.0");                         │
│    │                                                         │
│    } else if (s.attendancePct < 75) {  ← magic number       │
│    │   status = "NOT_ELIGIBLE";                              │
│    │   reasons.add("attendance below 75");                   │
│    │                                                         │
│    } else if (s.earnedCredits < 20) {  ← magic number       │
│    │   status = "NOT_ELIGIBLE";                              │
│    │   reasons.add("credits below 20");                      │
│    }                                                         │
│                                                              │
│    return new EligibilityEngineResult(status, reasons);      │
│                                                              │
│  EXECUTION for Ayaan (cgr=8.10, attend=72, credits=18):     │
│    Rule 1: flag != NONE?   → NONE       → PASS              │
│    Rule 2: cgr < 8.0?     → 8.10       → PASS              │
│    Rule 3: attend < 75?   → 72         → FAIL!             │
│       status = "NOT_ELIGIBLE"                                │
│       reasons = ["attendance below 75"]                      │
│    Rule 4: credits < 20?  → NEVER REACHED (else-if)         │
│       Ayaan has credits=18 (would fail) but SKIPPED          │
│                                                              │
│  Result: NOT_ELIGIBLE, ["attendance below 75"]               │
└──────────────────────────────────────────────────────────────┘
         │                              │
         ▼                              ▼
┌──────────────────────┐  ┌───────────────────────────────────┐
│  FakeEligibilityStore│  │        ReportPrinter              │
│──────────────────────│  │───────────────────────────────────│
│  save(roll, status): │  │  Prints:                          │
│    prints "Saved     │  │  "Student: Ayaan (CGR=8.10,       │
│    evaluation for    │  │   attendance=72, credits=18,       │
│    roll=23BCS1001"   │  │   flag=NONE)"                     │
│                      │  │  "RESULT: NOT_ELIGIBLE"            │
│  concrete class      │  │  "- attendance below 75"           │
│  NOT an interface    │  │                                    │
└──────────────────────┘  │  created internally with NEW       │
                          │  NOT injected                      │
                          └───────────────────────────────────┘

┌────────────────────────┐  ┌────────────────────────┐
│   RuleInput            │  │   Numbers              │
│────────────────────────│  │────────────────────────│
│ minCgr = 8.0           │  │ clamp(x, a, b)         │
│ minAttendance = 75     │  │                        │
│ minCredits = 20        │  │ DEAD CODE              │
│                        │  │ never used anywhere    │
│ DEAD CODE              │  │                        │
│ never used anywhere    │  │                        │
│ these exact values are │  │                        │
│ hardcoded in evaluate()│  │                        │
│ as magic numbers       │  │                        │
└────────────────────────┘  └────────────────────────┘
```

## 12. Issues

### OCP Violation — `if/else-if` chain in `evaluate()`

```
if (flag != NONE)         ← Rule 1: disciplinary
else if (cgr < 8.0)      ← Rule 2: CGR
else if (attend < 75)    ← Rule 3: attendance
else if (credits < 20)   ← Rule 4: credits
```

- **CLOSED for extension** — can't add a rule without editing `evaluate()`
- **OPEN for modification** — must edit `evaluate()` every time
- This is the **opposite** of OCP ("Open for extension, closed for modification")
- Want to add Rule 5 ("no active backlogs")? You **must** open `evaluate()` and add another `else-if` — risk breaking existing rules
- `else-if` means only the **first failure** is caught:
  - Ayaan: `attendance=72` (FAIL) **and** `credits=18` (FAIL)
  - But **only** `"attendance below 75"` is reported
  - Credits check is **never reached** because of `else-if`

### OCP Violation — Magic numbers hardcoded

```
evaluate() has:
  8.0   ← CGR threshold       ← magic number
  75    ← attendance threshold ← magic number
  20    ← credits threshold    ← magic number
```

- `RuleInput` class **exists** with these exact values (`minCgr = 8.0`, `minAttendance = 75`, `minCredits = 20`) but is **never used** — dead code
- If thresholds change → must edit `evaluate()` directly

### DIP Violations

- `EligibilityEngine` → `FakeEligibilityStore` — **concrete class**, not an interface. Can't swap to real database, can't mock for testing.
- `runAndPrint()` → `new ReportPrinter()` — **created internally**, not injected. Can't swap for different output format.

### Dead Code

- `RuleInput` — has threshold values, never referenced
- `Numbers` — has `clamp()` utility, never referenced
- Both exist in the codebase but serve no purpose

## 13. The Fix

### Key Idea

> Replace `if/else-if` chain with a **list of rule objects**.
> Engine just **loops and asks** each rule.
> Engine doesn't know what rules exist.
> Adding a new rule = new class + one line in Main.
> **Engine code never changes.**

### Refactored `Main.java` (wiring)

```java
List<EligibilityRule> rules = List.of(
    new DisciplinaryRule(),    // Rule 1
    new CgrRule(),             // Rule 2
    new AttendanceRule(),      // Rule 3
    new CreditsRule()          // Rule 4
);

EligibilityStore store = new FakeEligibilityStore();
EligibilityEngine engine = new EligibilityEngine(rules, store);

engine.runAndPrint(s);

// Want Rule 5? Just add: new BacklogRule()
// ONE new class + ONE line here. Engine UNTOUCHED.
// Want to reorder? Move lines around. Engine UNTOUCHED.
```

### `EligibilityRule` interface — the extension point

```
┌──────────────────────────────────────────────────────────────┐
│        «interface» EligibilityRule                            │
│──────────────────────────────────────────────────────────────│
│   String check(StudentProfile s)                             │
│     → returns null    = student PASSES this rule             │
│     → returns String  = student FAILS (with reason message)  │
│                                                              │
│   This is the EXTENSION POINT — the key to OCP.             │
│   Any new rule just implements this interface.               │
└──────┬───────────┬───────────┬───────────┬───────────────────┘
       │           │           │           │
       ▼           ▼           ▼           ▼
┌────────────┐┌──────────┐┌─────────────┐┌────────────┐
│Disciplinary││ CgrRule  ││ Attendance  ││ Credits    │
│Rule        ││          ││ Rule        ││ Rule       │
│────────────││──────────││─────────────││────────────│
│ check(s):  ││ check(s):││ check(s):   ││ check(s):  │
│ if flag != ││ if cgr   ││ if attend   ││ if credits │
│   NONE     ││   < 8.0  ││   < 75      ││   < 20     │
│  return    ││  return  ││  return     ││  return    │
│  "discip-  ││  "CGR    ││  "attend-   ││  "credits  │
│   linary   ││   below  ││   ance      ││   below    │
│   flag     ││   8.0"   ││   below 75" ││   20"      │
│   present" ││          ││             ││            │
│ else       ││ else     ││ else        ││ else       │
│  return    ││  return  ││  return     ││  return    │
│  null      ││  null    ││  null       ││  null      │
│ (PASS)     ││ (PASS)   ││ (PASS)      ││ (PASS)     │
│            ││          ││             ││            │
│ ~8 lines   ││ ~8 lines ││ ~8 lines    ││ ~8 lines   │
│ testable   ││ testable ││ testable    ││ testable   │
│ alone      ││ alone    ││ alone       ││ alone      │
└────────────┘└──────────┘└─────────────┘└────────────┘
```

### Refactored `EligibilityEngine`

```
┌──────────────────────────────────────────────────────────────┐
│              EligibilityEngine (REFACTORED)                   │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    List<EligibilityRule> rules  ← INJECTED from Main         │
│    EligibilityStore store       ← INTERFACE, not concrete    │
│                                                              │
│  Constructor:                                                │
│    EligibilityEngine(List<EligibilityRule> rules,            │
│                      EligibilityStore store)                  │
│    Both INJECTED. Engine creates nothing itself.             │
│──────────────────────────────────────────────────────────────│
│  evaluate(StudentProfile s):                                 │
│                                                              │
│    List<String> reasons = new ArrayList<>();                 │
│    String status = "ELIGIBLE";                               │
│                                                              │
│    for (EligibilityRule rule : rules) {                      │
│    │                                                         │
│    │   String reason = rule.check(s);                        │
│    │   │                                                     │
│    │   │  What check() returns:                              │
│    │   │    null   → student PASSES → continue to next rule  │
│    │   │    String → student FAILS  → stop                   │
│    │   │                                                     │
│    │   if (reason != null) {                                 │
│    │       status = "NOT_ELIGIBLE";                          │
│    │       reasons.add(reason);                              │
│    │       break;   ← stop at first failure                  │
│    │   }             (remove break → get ALL failures)       │
│    │                                                         │
│    }                                                         │
│                                                              │
│    return new EligibilityEngineResult(status, reasons);      │
│                                                              │
│  KEY POINT: Engine does NOT know:                            │
│    - What rules exist                                        │
│    - What each rule checks (cgr? credits? anything)          │
│    - What thresholds are (8.0? 75? 20?)                      │
│  It just LOOPS and ASKS. That's it.                          │
│──────────────────────────────────────────────────────────────│
│  runAndPrint(s):                                             │
│    ReportPrinter p = new ReportPrinter();                    │
│    EligibilityEngineResult r = evaluate(s);                  │
│    p.print(s, r);                                            │
│    store.save(s.rollNo, r.status);                           │
│    ^^^^^                                                     │
│    store is EligibilityStore (INTERFACE)                      │
│    not FakeEligibilityStore (concrete)                        │
└──────────────────────────────────────────────────────────────┘
```

### Execution Trace for Ayaan (`cgr=8.10`, `attend=72`, `credits=18`)

```
rules = [DisciplinaryRule, CgrRule, AttendanceRule, CreditsRule]

Iteration 1: DisciplinaryRule.check(Ayaan)
  → flag == NONE → return null → PASS ✓ → continue

Iteration 2: CgrRule.check(Ayaan)
  → cgr = 8.10, not < 8.0 → return null → PASS ✓ → continue

Iteration 3: AttendanceRule.check(Ayaan)
  → attend = 72, < 75 → return "attendance below 75" → FAIL ✗
  → status = "NOT_ELIGIBLE"
  → reasons = ["attendance below 75"]
  → break (stop here)

Iteration 4: CreditsRule → NEVER REACHED (break)

Result: NOT_ELIGIBLE, ["attendance below 75"]
(same output as before — behavior preserved)
```

### `EligibilityStore` interface (DIP fix)

```
        «interface» EligibilityStore
        ┌──────────────────────────┐
        │  void save(roll, status) │
        └────────────┬─────────────┘
                     │
                     ▼
        ┌──────────────────────────┐
        │  FakeEligibilityStore    │
        │  implements              │
        │  EligibilityStore        │
        │──────────────────────────│
        │  save(roll, status):     │
        │    prints "Saved         │
        │    evaluation for        │
        │    roll=23BCS1001"       │
        └──────────────────────────┘

Tomorrow: DatabaseStore implements EligibilityStore
          → ZERO change to engine
```

### Before vs After

| **Before** | **After** |
|---|---|
| 4 rules as `if/else-if` | 4 classes implementing `EligibilityRule` |
| New rule = edit `evaluate()` | New rule = new class + 1 line in Main |
| Engine **knows** all rule logic | Engine **knows nothing** about rules |
| Thresholds hardcoded (`8.0`, `75`, `20`) as magic numbers | Each rule owns its own threshold |
| Only first failure reported | Remove `break` → get ALL failures |
| `FakeEligibilityStore` concrete | `EligibilityStore` interface |
| `ReportPrinter` created inside | (still internal — minor) |
| `RuleInput`, `Numbers` unused | Dead code can be removed |

**Primary fix: OCP** — Engine is now **closed for modification**, **open for extension**. New rule = new class implementing `EligibilityRule`. Engine code **never touched**.

**Secondary fix: DIP** — Store is now behind `EligibilityStore` interface. Can swap `FakeEligibilityStore` for `DatabaseStore`.

