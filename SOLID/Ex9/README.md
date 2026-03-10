# Ex9 — DIP: Assignment Evaluation Pipeline

## 1. Context
An evaluation pipeline checks submissions using a rubric, runs plagiarism checks, grades code, and writes a report.

## 2. Current behavior
- `EvaluationPipeline.evaluate` directly instantiates concrete graders/checkers/writers with `new`
- Prints a final summary line and “writes” a report

## 3. What’s wrong (at least 5 issues)
1. High-level pipeline depends on low-level concrete classes (hard-coded `new` everywhere).
2. Hard to test pipeline without running real checks.
3. Changing a component requires editing pipeline code.
4. No clear abstraction boundaries; responsibilities are mixed.
5. Configuration is embedded (paths, thresholds).

## 4. Your task
Checkpoint A: Run and capture output.
Checkpoint B: Introduce small abstractions for grader/checker/writer.
Checkpoint C: Inject dependencies into pipeline.
Checkpoint D: Keep output identical.

## 5. Constraints
- Preserve output and line order.
- Keep `Submission` fields unchanged.
- No external libraries.

## 6. Acceptance criteria
- Pipeline depends on abstractions, not concretes.
- Easy to substitute test doubles.

## 7. How to run
```bash
cd SOLID/Ex9/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Evaluation Pipeline ===
PlagiarismScore=12
CodeScore=78
Report written: report-23BCS1007.txt
FINAL: PASS (total=90)
```

## 9. Hints (OOP-only)
- Define minimal interfaces with only what the pipeline needs.
- Pass dependencies via constructor.

## 10. Stretch goals
- Add a second grading strategy without editing pipeline logic.




---

# Preparation Notes (Diagram Style)

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  Submission sub = new Submission(                            │
│      "23BCS1007",                                            │
│      "class Solution { ... }"   ← contains "class"          │
│  );                                                          │
│                                                              │
│  new EvaluationPipeline().evaluate(sub);                     │
│        ▲                                                     │
│        no-arg constructor — takes NOTHING                    │
│        everything created INSIDE evaluate()                  │
│                                                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│           EvaluationPipeline.evaluate()                      │
│          THE GOD METHOD — creates and calls all deps         │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  STEP 1: Instantiate dependencies directly                   │
│  │  Rubric rubric       = new Rubric();   ← bonus=28        │
│  │  PlagiarismChecker pc= new PlagiarismChecker();           │
│  │  CodeGrader grader   = new CodeGrader();                  │
│  │  ReportWriter writer = new ReportWriter();                │
│  │                                                           │
│  STEP 2: Run plagiarism check                                │
│  │  int plag = pc.check(sub);                                │
│  │  → sub.code contains "class" → plag = 12                  │
│  │  → prints "PlagiarismScore=12"                            │
│  │                                                           │
│  STEP 3: Run code grading                                    │
│  │  int code = grader.grade(sub, rubric);                    │
│  │  → base = min(80, 50 + code.length()%40)                  │
│  │  → code = base + rubric.bonus (28) = 94                   │
│  │  → prints "CodeScore=94"                                  │
│  │                                                           │
│  STEP 4: Write report                                        │
│  │  String name = writer.write(sub, plag, code);             │
│  │  → "report-" + sub.roll + ".txt"                          │
│  │  → prints "Report written: report-23BCS1007.txt"          │
│  │                                                           │
│  STEP 5: Compute total and print result                      │
│  │  total = plag + code = 12 + 94 = 106                      │
│  │  → prints "FINAL: PASS (total=106)"                       │
│                                                              │
└──────────────────────────────────────────────────────────────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
┌──────────────┐ ┌────────────┐ ┌──────────────┐ ┌──────────────┐
│  Rubric      │ │ Plagiarism │ │  CodeGrader  │ │ ReportWriter │
│──────────────│ │ Checker    │ │──────────────│ │──────────────│
│ bonus = 28   │ │────────────│ │ grade(s, r): │ │ write(s,p,c):│
│              │ │ check(s):  │ │  base = min( │ │  returns     │
│ plain data   │ │  "class"   │ │  80, 50 +    │ │  "report-"   │
│ holder       │ │  in code   │ │  len%40)     │ │  + roll      │
│              │ │  → 12      │ │  + r.bonus   │ │  + ".txt"    │
│              │ │  else → 40 │ │  → 94        │ │              │
└──────────────┘ └────────────┘ └──────────────┘ └──────────────┘

┌────────────────────────┐
│   SimpleConsole        │
│────────────────────────│
│ (formerly unused file) │
│                        │
│ DEAD CODE              │
│ no Checker/Grader/     │
│ Writer interfaces here │
│ in the broken version  │
└────────────────────────┘
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: new Rubric() inside evaluate()      [DIP VIOLATION] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: EvaluationPipeline.evaluate() — first line           │
│                                                              │
│  Rubric rubric = new Rubric();                               │
│                 ▲                                            │
│                 concrete instantiation, hardcoded inside     │
│                                                              │
│  EvaluationPipeline ─── new ───► Rubric                      │
│  (HIGH-level module)              (LOW-level detail)         │
│                                                              │
│  WHY IT'S BAD:                                               │
│    Want a different rubric (e.g. bonus=50 for finals)?       │
│      → Must open EvaluationPipeline and change new Rubric()  │
│      → Can't inject a mock Rubric for tests                  │
│                                                              │
│  DIP says: high-level should not depend on low-level.        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: new PlagiarismChecker() inside     [DIP VIOLATION] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: EvaluationPipeline.evaluate()                        │
│                                                              │
│  PlagiarismChecker pc = new PlagiarismChecker();             │
│                                                              │
│  EvaluationPipeline ─── new ───► PlagiarismChecker           │
│                                                              │
│  No interface exists. Can't swap in a test double.           │
│  Want mock that always returns 0 for clean tests?            │
│    → Must edit the pipeline itself.                          │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: new CodeGrader() inside evaluate() [DIP VIOLATION] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: EvaluationPipeline.evaluate()                        │
│                                                              │
│  CodeGrader grader = new CodeGrader();                       │
│                                                              │
│  Want a stricter grader that subtracts style penalties?      │
│    → Can't plug it in. Pipeline hardwires CodeGrader.        │
│                                                              │
│  Grade changes → edit pipeline. Tests → edit pipeline.       │
│  Class is NOT closed for modification.                       │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: new ReportWriter() inside evaluate()[DIP VIOLATION] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: EvaluationPipeline.evaluate()                        │
│                                                              │
│  ReportWriter writer = new ReportWriter();                   │
│                                                              │
│  Want to write JSON reports for an API? Or suppress output   │
│  in tests?                                                   │
│    → Can't. Hard dependency. Must modify pipeline.           │
└──────────────────────────────────────────────────────────────┘

DIP VIOLATIONS SUMMARY — 4 concrete couplings, zero abstractions:

  EvaluationPipeline ──── new ────► Rubric
                         (hardcoded) no interface
                                     can't swap bonus value

  EvaluationPipeline ──── new ────► PlagiarismChecker
                         (hardcoded) no Checker interface
                                     can't mock for tests

  EvaluationPipeline ──── new ────► CodeGrader
                         (hardcoded) no Grader interface
                                     can't swap grading strategy

  EvaluationPipeline ──── new ────► ReportWriter
                         (hardcoded) no Writer interface
                                     can't swap to JSON or null writer


┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: CodeGrader leaks Rubric into interface concern     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  grade(Submission s, Rubric r)                               │
│                       ▲ Rubric is a low-level detail         │
│                                                              │
│  A grading abstraction should say: grade(Submission s)       │
│  The rubric is the grader's own configuration.               │
│  Exposing it in the signature couples callers to Rubric.     │
│  Pipeline must know about Rubric — but it shouldn't.         │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│  FIX: Introduce 3 interfaces in SimpleConsole.java           │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ┌───────────────────────┐                                   │
│  │  «interface» Checker  │  ← replaces PlagiarismChecker dep │
│  │───────────────────────│                                   │
│  │  int check(Submission)│                                   │
│  └───────────┬───────────┘                                   │
│              │ implements                                     │
│              ▼                                               │
│  ┌───────────────────────┐                                   │
│  │  PlagiarismChecker    │                                   │
│  │  implements Checker   │                                   │
│  └───────────────────────┘                                   │
│                                                              │
│  ┌───────────────────────┐                                   │
│  │  «interface» Grader   │  ← replaces CodeGrader dep        │
│  │───────────────────────│                                   │
│  │  int grade(Submission)│  ← Rubric moved INSIDE grader     │
│  └───────────┬───────────┘                                   │
│              │ implements                                     │
│              ▼                                               │
│  ┌───────────────────────┐                                   │
│  │  CodeGrader           │  Rubric rubric = new Rubric()     │
│  │  implements Grader    │  handled internally               │
│  └───────────────────────┘                                   │
│                                                              │
│  ┌────────────────────────────────────────┐                  │
│  │  «interface» Writer                    │  ← replaces dep  │
│  │────────────────────────────────────────│                  │
│  │  String write(Submission, int, int)    │                  │
│  └───────────┬────────────────────────────┘                  │
│              │ implements                                     │
│              ▼                                               │
│  ┌───────────────────────┐                                   │
│  │  ReportWriter         │                                   │
│  │  implements Writer    │                                   │
│  └───────────────────────┘                                   │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│              EvaluationPipeline (REFACTORED)                 │
│              Now a PURE ORCHESTRATOR                         │
│──────────────────────────────────────────────────────────────│
│  Fields (all interfaces):                                    │
│    - Checker  checker                                        │
│    - Grader   grader                                         │
│    - Writer   writer                                         │
│──────────────────────────────────────────────────────────────│
│  Constructor:                                                │
│    EvaluationPipeline(Checker c, Grader g, Writer w)        │
│                        ▲ no Rubric, no concretes             │
│──────────────────────────────────────────────────────────────│
│  evaluate(Submission sub):                                   │
│    int plag       = checker.check(sub);    ← uses interface  │
│    int code       = grader.grade(sub);     ← uses interface  │
│    String report  = writer.write(sub, plag, code);           │
│    int total = plag + code;                                  │
│    print FINAL result                                        │
│                                                              │
│  ZERO new keywords. ZERO concrete types.                     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                  Main.java (wiring)                          │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  Checker c = new PlagiarismChecker();                        │
│  Grader  g = new CodeGrader();                               │
│  Writer  w = new ReportWriter();                             │
│                                                              │
│  new EvaluationPipeline(c, g, w).evaluate(sub);              │
│                                                              │
│  All concretes created OUTSIDE. None inside pipeline.        │
└──────────────────────────────────────────────────────────────┘


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                                AFTER
  ──────                                ─────
  new Rubric() inside evaluate()    →   Rubric inside CodeGrader (private)
  new PlagiarismChecker() inside    →   Checker interface, injected
  new CodeGrader() inside           →   Grader interface, injected
  new ReportWriter() inside         →   Writer interface, injected
  grade(Submission, Rubric)         →   grade(Submission) — rubric hidden
  No interfaces existed             →   3 interfaces in SimpleConsole.java
  SimpleConsole.java: dead code     →   SimpleConsole.java: interface home


WHY SWAP IS EASY NOW:

  Mock checker (always 0)?  → MockChecker implements Checker   → plug in Main
  Strict grader?            → StrictGrader implements Grader   → plug in Main
  JSON report writer?       → JsonWriter implements Writer     → plug in Main
  Suppress output in tests? → NullWriter implements Writer     → plug in Main

  EvaluationPipeline NEVER changes. DIP satisfied.
```

