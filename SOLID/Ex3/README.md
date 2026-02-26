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

1. What does this program do?
A placement eligibility checker. Given a student's profile, it evaluates whether the student is eligible for campus placements based on multiple rules.

Flow
Main creates a StudentProfile for Ayaan: CGR=8.10, attendance=72, credits=18, flag=NONE
Main creates EligibilityEngine with a FakeEligibilityStore
Calls engine.runAndPrint(s) which:
Calls evaluate(s) → the core logic
Inside evaluate() (EligibilityEngine.java:15-35):
Checks disciplinary flag → NONE → passes
Checks CGR < 8.0 → 8.10 → passes
Checks attendance < 75 → 72 → FAILS → adds "attendance below 75", status = NOT_ELIGIBLE
Stops here (else-if chain, so credits check never runs)
Returns EligibilityEngineResult("NOT_ELIGIBLE", ["attendance below 75"])
ReportPrinter prints the student info + result + reasons
FakeEligibilityStore.save() prints "Saved evaluation for roll=23BCS1001"
Supporting classes
StudentProfile — data holder (rollNo, name, cgr, attendancePct, earnedCredits, disciplinaryFlag)
LegacyFlags — constants: NONE=0, WARNING=1, SUSPENDED=2
ReportPrinter — prints the formatted report
FakeEligibilityStore — fake persistence, just prints "Saved evaluation..."
RuleInput — has config values (minCgr=8.0, minAttendance=75, minCredits=20) but is completely unused (dead code smell)
Numbers — utility class with clamp() — also unused (dead code smell)
2. Design issues
Issue 1 — evaluate() is a long if/else-if chain → OCP violation
Lines 20-32: Four rules hardcoded as if/else-if. Want to add a new rule (say "minimum CGPA in final semester")? You must edit evaluate(). OCP says the class should be open for extension, closed for modification. This is the opposite — every new rule forces modification.

Issue 2 — else-if means only ONE rule can fail → Logic bug / design flaw
Because it's else-if, the moment one rule fails, all remaining rules are skipped. If a student has low attendance AND low credits, only the attendance reason is reported. This is a design decision baked into the structure — you can't easily change it to report ALL failures without rewriting the chain.

Issue 3 — Rule thresholds are hardcoded → OCP violation
8.0, 75, 20 are magic numbers inside the method. RuleInput exists with these exact values but is never used. If thresholds change, you edit the engine method itself.

Issue 4 — FakeEligibilityStore is a concrete dependency → DIP violation
Line 4: private final FakeEligibilityStore store — concrete class, not an interface. Can't swap to a real database without editing EligibilityEngine.

Issue 5 — ReportPrinter is created internally → DIP violation (minor)
Line 9: ReportPrinter p = new ReportPrinter() — created inside runAndPrint(), not injected. Tightly coupled.

Issue 6 — Dead code: RuleInput and Numbers → Code smell
Both classes exist but are never referenced anywhere. RuleInput has the exact config values that are hardcoded in evaluate() — it was meant to be used but wasn't.

3. Which SOLID principle is violated?
Primary: OCP (Open/Closed Principle)
"Software entities should be open for extension, but closed for modification."

Every time you need a new eligibility rule, you must open EligibilityEngine.evaluate() and add another else-if branch. The engine is closed for extension, open for modification — the exact opposite of OCP.

Secondary: DIP (Dependency Inversion Principle)
FakeEligibilityStore is a concrete class, not behind an interface. ReportPrinter is instantiated internally.

4. My fix (Deep Walkthrough)
4a. Created EligibilityRule interface

public interface EligibilityRule {
    String check(StudentProfile s);
}
Returns null if the student passes the rule. Returns a reason string if they fail. This is the extension point — the key to OCP.

4b. One class per rule
Each rule becomes its own class implementing EligibilityRule:

DisciplinaryRule


public class DisciplinaryRule implements EligibilityRule {
    public String check(StudentProfile s) {
        if (s.disciplinaryFlag != LegacyFlags.NONE) return "disciplinary flag present";
        return null;
    }
}
CgrRule


public class CgrRule implements EligibilityRule {
    public String check(StudentProfile s) {
        if (s.cgr < 8.0) return "CGR below 8.0";
        return null;
    }
}
AttendanceRule — checks attendancePct < 75

CreditsRule — checks earnedCredits < 20

Each rule is tiny, focused, independently testable.

4c. Refactored EligibilityEngine
The if/else-if chain is replaced by a loop over a list of rules:


public EligibilityEngine(List<EligibilityRule> rules, EligibilityStore store) {
    this.rules = rules;
    this.store = store;
}

public EligibilityEngineResult evaluate(StudentProfile s) {
    List<String> reasons = new ArrayList<>();
    String status = "ELIGIBLE";
    for (EligibilityRule rule : rules) {
        String reason = rule.check(s);
        if (reason != null) {
            status = "NOT_ELIGIBLE";
            reasons.add(reason);
            break;
        }
    }
    return new EligibilityEngineResult(status, reasons);
}
The engine doesn't know what the rules are. It just iterates and asks each one. This is OCP — to add a new rule, you create a new class and add it to the list. EligibilityEngine is never modified.

4d. Created EligibilityStore interface

public interface EligibilityStore {
    void save(String roll, String status);
}
FakeEligibilityStore now implements EligibilityStore. Fixes the DIP violation.

4e. Wiring in Main

List<EligibilityRule> rules = List.of(
    new DisciplinaryRule(),
    new CgrRule(),
    new AttendanceRule(),
    new CreditsRule()
);
EligibilityEngine engine = new EligibilityEngine(rules, new FakeEligibilityStore());
Rules are assembled externally and injected. Order in the list = order of evaluation. Want to add a new rule? Create the class, add one line here. Engine code untouched.
