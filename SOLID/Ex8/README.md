# Ex8 — ISP: Student Club Management Admin Tools

## 1. Context
Clubs have different roles: treasurer, secretary, event lead. The admin tool interface currently combines everything.

## 2. Current behavior
- One interface `ClubAdminTools` includes finance, minutes, and event operations
- Each role tool implements methods it does not use (dummy / exceptions)
- `ClubConsole` calls only the relevant subset per role

## 3. What’s wrong (at least 5 issues)
1. Fat interface forces irrelevant methods.
2. Dummy implementations cause hidden failures later.
3. Clients depend on methods they don’t need.
4. New role tools become harder to implement safely.
5. Capabilities are unclear.

## 4. Your task
- Split interface into smaller role/capability interfaces.
- Ensure each tool depends only on the methods it uses.
- Preserve output.

## 5. Constraints
- Preserve output and command order.
- Keep class names unchanged.

## 6. Acceptance criteria
- No dummy/no-op implementations for irrelevant methods.
- `ClubConsole` depends on minimal interfaces.

## 7. How to run
```bash
cd SOLID/Ex8/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Club Admin ===
Ledger: +5000 (sponsor)
Minutes added: "Meeting at 5pm"
Event created: HackNight (budget=2000)
Summary: ledgerBalance=5000, minutes=1, events=1
```

## 9. Hints (OOP-only)
- Identify client groups: finance client, minutes client, events client.
- Split by what callers actually need.

## 10. Stretch goals
- Add “publicity lead” without implementing finance methods.




---

# Preparation Notes (Diagram Style)

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│  ClubConsole console = new ClubConsole();                    │
│  console.run();                                              │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    ClubConsole.run()                         │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ClubAdminTools treasurer = new TreasurerTool(); ← FAT TYPE  │
│  ClubAdminTools secretary = new SecretaryTool(); ← FAT TYPE  │
│  ClubAdminTools eventLead = new EventLeadTool(); ← FAT TYPE  │
│                                                              │
│  treasurer.addIncome(5000, "sponsor");                       │
│    → "Ledger: +5000 (sponsor)"                               │
│                                                              │
│  secretary.addMinutes("Meeting at 5pm");                     │
│    → "Minutes added: \"Meeting at 5pm\""                     │
│                                                              │
│  eventLead.createEvent("HackNight", 2000);                   │
│    → "Event created: HackNight (budget=2000)"                │
│                                                              │
│  Summary line reads balance, minutes, events count           │
│    → "Summary: ledgerBalance=5000, minutes=1, events=1"      │
│                                                              │
│  ⚠ All variables typed as ClubAdminTools — the fat          │
│    interface. ClubConsole sees all 5 methods on every tool.  │
│    treasurer.addMinutes("x") would COMPILE with no warning.  │
└──────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│              «interface» ClubAdminTools                      │
│──────────────────────────────────────────────────────────────│
│  void addIncome(double amount, String note)                  │
│  void addExpense(double amount, String note)                 │
│  void addMinutes(String text)                                │
│  void createEvent(String name, double budget)                │
│  int  getEventsCount()                                       │
│                                                              │
│  ⚠ 5 METHODS — every role needs only a strict subset.       │
└──────────┬─────────────────┬──────────────────┬─────────────┘
           │                 │                  │
           ▼                 ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌──────────────────┐
│ TreasurerTool   │ │ SecretaryTool   │ │ EventLeadTool    │
│─────────────────│ │─────────────────│ │──────────────────│
│ addIncome    ✓  │ │ addMinutes   ✓  │ │ createEvent   ✓  │
│ addExpense   ✓  │ │                 │ │ getEventsCount✓  │
│  (BudgetLedger) │ │  (MinutesBook)  │ │  (EventPlanner)  │
│                 │ │                 │ │                  │
│ addMinutes      │ │ addIncome       │ │ addIncome        │
│ /* irrelevant */│ │ /* irrelevant */│ │ /* irrelevant */ │
│ createEvent     │ │ addExpense      │ │ addExpense       │
│ /* irrelevant */│ │ /* irrelevant */│ │ /* irrelevant */ │
│ getEventsCount  │ │ createEvent     │ │ addMinutes       │
│   return 0      │ │ /* irrelevant */│ │ /* irrelevant */ │
│                 │ │ getEventsCount  │ │                  │
│                 │ │   return 0      │ │                  │
│ ⚠ 3 dummies    │ │ ⚠ 4 dummies    │ │ ⚠ 3 dummies     │
└─────────────────┘ └─────────────────┘ └──────────────────┘

┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│  BudgetLedger  │  │  MinutesBook   │  │  EventPlanner  │
│────────────────│  │────────────────│  │────────────────│
│ credit(double) │  │ record(String) │  │ plan(name,     │
│ debit(double)  │  │ count()        │  │   budget)      │
│ balance()      │  │                │  │ count()        │
│                │  │ used by        │  │                │
│ used by        │  │ SecretaryTool  │  │ used by        │
│ TreasurerTool  │  │ only           │  │ EventLeadTool  │
│ only           │  │                │  │ only           │
└────────────────┘  └────────────────┘  └────────────────┘
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: TreasurerTool forced to implement 3 dummy methods  │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: TreasurerTool.java                                   │
│                                                              │
│  TreasurerTool's only real job: addIncome and addExpense.    │
│  It has NO interest in minutes, events, or event counts.     │
│                                                              │
│  FORCED DUMMIES:                                             │
│    addMinutes(String text)           { /* irrelevant */ }    │
│    createEvent(String name, double b){ /* irrelevant */ }    │
│    getEventsCount()                  { return 0; }           │
│                                                              │
│  ISP RULE: No class should be forced to implement methods    │
│  it does not use.                                            │
│                                                              │
│  RISK: ClubConsole could call treasurer.createEvent(...)     │
│  and it compiles fine. The dummy runs silently. No event     │
│  is actually created. The bug is invisible.                  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: SecretaryTool forced to implement 4 dummy methods  │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: SecretaryTool.java                                   │
│                                                              │
│  SecretaryTool's only real job: addMinutes.                  │
│  It has NO interest in finances or events at all.            │
│                                                              │
│  FORCED DUMMIES:                                             │
│    addIncome(double, String)         { /* irrelevant */ }    │
│    addExpense(double, String)        { /* irrelevant */ }    │
│    createEvent(String, double)       { /* irrelevant */ }    │
│    getEventsCount()                  { return 0; }           │
│                                                              │
│  The WORST offender: 4 out of 5 methods are meaningless.    │
│  The interface has overwhelmed this role's real concern.     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: EventLeadTool forced to implement 3 dummy methods  │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: EventLeadTool.java                                   │
│                                                              │
│  EventLeadTool's real job: createEvent and getEventsCount.   │
│  It has NO interest in money or meeting minutes.             │
│                                                              │
│  FORCED DUMMIES:                                             │
│    addIncome(double, String)         { /* irrelevant */ }    │
│    addExpense(double, String)        { /* irrelevant */ }    │
│    addMinutes(String text)           { /* irrelevant */ }    │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: ClubConsole depends on fat interface               │
│           — can call wrong methods on any role               │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: ClubConsole.java                                     │
│                                                              │
│  ClubAdminTools treasurer = new TreasurerTool();             │
│  ClubAdminTools secretary = new SecretaryTool();             │
│  ClubAdminTools eventLead = new EventLeadTool();             │
│                                                              │
│  Because every variable is typed as ClubAdminTools:          │
│    treasurer.addMinutes("notes")   ← compiles, no error      │
│    secretary.addIncome(1000,"...")  ← compiles, no error      │
│    eventLead.addExpense(200,"...")  ← compiles, no error      │
│                                                              │
│  All compile. All run. All do exactly nothing.               │
│  The fat interface provides ZERO protection against          │
│  calling the wrong operation on the wrong role tool.         │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: New roles require implementing all 5 methods       │
│                                          [ISP VIOLATION]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: Any new tool class implementing ClubAdminTools       │
│                                                              │
│  Adding a "PublicityLead" role (posts announcements)?        │
│    → MUST implement addIncome    { /* irrelevant */ }        │
│    → MUST implement addExpense   { /* irrelevant */ }        │
│    → MUST implement addMinutes   { /* irrelevant */ }        │
│    → MUST implement createEvent  { /* irrelevant */ }        │
│    → MUST implement getEventsCount{ return 0; }              │
│                                                              │
│  0 of 5 methods are meaningful for publicity.                │
│  The fat interface makes every future role pay the full      │
│  implementation cost of ALL existing roles' methods.         │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│  Split ClubAdminTools → 3 narrow role interfaces             │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE (one fat interface):                                 │
│    interface ClubAdminTools {                                │
│        addIncome(double, String); addExpense(double, String);│
│        addMinutes(String);                                   │
│        createEvent(String, double); getEventsCount();        │
│    }                                                         │
│                                                              │
│  AFTER (3 focused role interfaces):                          │
│                                                              │
│  ┌────────────────────────────────┐                          │
│  │ «interface» FinanceTool        │                          │
│  │────────────────────────────────│                          │
│  │ void addIncome(double, String) │                          │
│  │ void addExpense(double, String)│                          │
│  └────────────────────────────────┘                          │
│                                                              │
│  ┌────────────────────────────────┐                          │
│  │ «interface» MinutesTool        │                          │
│  │────────────────────────────────│                          │
│  │ void addMinutes(String text)   │                          │
│  └────────────────────────────────┘                          │
│                                                              │
│  ┌────────────────────────────────┐                          │
│  │ «interface» EventTool          │                          │
│  │────────────────────────────────│                          │
│  │ void createEvent(String,double)│                          │
│  │ int  getEventsCount()          │                          │
│  └────────────────────────────────┘                          │
│                                                              │
│  Each interface maps to exactly one role's responsibility.   │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  Each tool implements ONLY what it needs                     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  TreasurerTool implements FinanceTool                        │
│    addIncome, addExpense  ← REAL implementations             │
│    via BudgetLedger                                          │
│    NO addMinutes. NO createEvent. NO getEventsCount.         │
│    NO dummy methods.                                         │
│                                                              │
│  SecretaryTool implements MinutesTool                        │
│    addMinutes  ← the ONE method it needs                     │
│    via MinutesBook                                           │
│    NO addIncome. NO addExpense. NO event methods.            │
│    NO dummy methods.                                         │
│                                                              │
│  EventLeadTool implements EventTool                          │
│    createEvent, getEventsCount  ← REAL implementations       │
│    via EventPlanner                                          │
│    NO addIncome. NO addExpense. NO addMinutes.               │
│    NO dummy methods.                                         │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ClubConsole — uses specific interface types                 │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    ClubAdminTools treasurer = new TreasurerTool();           │
│    ClubAdminTools secretary = new SecretaryTool();           │
│    ClubAdminTools eventLead = new EventLeadTool();           │
│    treasurer.addMinutes("x")  ← compiles, does nothing       │
│    secretary.addIncome(...)   ← compiles, does nothing       │
│                                                              │
│  AFTER:                                                      │
│    FinanceTool  treasurer = new TreasurerTool();             │
│    MinutesTool  secretary = new SecretaryTool();             │
│    EventTool    eventLead = new EventLeadTool();             │
│                                                              │
│    treasurer.addIncome(5000, "sponsor");  ← only finance ops │
│    secretary.addMinutes("Meeting at 5pm"); ← only minutes    │
│    eventLead.createEvent("HackNight", 2000); ← only events   │
│                                                              │
│    treasurer.addMinutes(...)  → COMPILE ERROR                │
│    secretary.addIncome(...)   → COMPILE ERROR                │
│                                                              │
│  Compiler enforces role boundaries.                          │
│  Calling the wrong operation on the wrong tool = caught      │
│  at compile time, not discovered through silent dummies.     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  Summary of dependency graph (before vs after)               │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    ClubConsole ─── ClubAdminTools (5 methods)                │
│                         ▲        ▲        ▲                  │
│                 TreasurerTool SecretaryTool EventLeadTool     │
│                  (3 dummies)  (4 dummies)  (3 dummies)       │
│                                                              │
│  AFTER:                                                      │
│    ClubConsole ─── FinanceTool  (2 methods)                  │
│                         ▲                                    │
│                    TreasurerTool  ← 0 dummies                │
│                                                              │
│    ClubConsole ─── MinutesTool   (1 method)                  │
│                         ▲                                    │
│                    SecretaryTool  ← 0 dummies                │
│                                                              │
│    ClubConsole ─── EventTool     (2 methods)                 │
│                         ▲                                    │
│                    EventLeadTool  ← 0 dummies                │
│                                                              │
└──────────────────────────────────────────────────────────────┘


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                               AFTER
  ──────                               ─────
  1 fat interface (5 methods)    →    3 narrow role interfaces
  TreasurerTool: 3 dummy methods →    0 dummy methods
  SecretaryTool: 4 dummy methods →    0 dummy methods
  EventLeadTool: 3 dummy methods →    0 dummy methods
  ClubConsole: ClubAdminTools    →    FinanceTool / MinutesTool / EventTool
  Wrong-role call: silent dummy  →    COMPILE ERROR


ISP PROOF:

  New "PublicityLead" role (posts announcements)?
    → Define interface PublicityTool { void postAnnouncement(String); }
    → PublicityLeadTool implements PublicityTool
    → NO addIncome. NO addMinutes. NO createEvent.
    → NO dummy methods. ISP satisfied.

  Every class depends ONLY on the methods it actually uses.
```

