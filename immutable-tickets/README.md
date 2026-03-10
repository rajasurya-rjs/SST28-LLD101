Exercise B — Immutable Classes (Incident Tickets)
------------------------------------------------
Narrative
A small CLI tool called **HelpLite** creates and manages support/incident tickets.
Today, `IncidentTicket` is **mutable**:
- multiple constructors
- public setters
- validation scattered across the codebase
- objects can be modified after being "created", causing audit/log inconsistencies

Refactor the design so `IncidentTicket` becomes **immutable** and is created using a **Builder**.

What you have (Starter)
- `IncidentTicket` has public setters + several constructors.
- `TicketService` creates a ticket, then mutates it later (bad).
- Validation is duplicated and scattered, making it easy to miss checks.
- `TryIt` demonstrates how the same object can change unexpectedly.

Tasks
1) Refactor `IncidentTicket` to an **immutable class**
   - private final fields
   - no setters
   - defensive copying for collections
   - safe getters (no internal state leakage)

2) Introduce `IncidentTicket.Builder`
   - Required: `id`, `reporterEmail`, `title`
   - Optional: `description`, `priority`, `tags`, `assigneeEmail`, `customerVisible`, `slaMinutes`, `source`
   - Builder should be fluent (`builder().id(...).title(...).build()`)

3) Centralize validation
   - Move ALL validation to `Builder.build()`
   - Use helpers in `Validation.java` (add more if needed)
   - Examples:
     - id: non-empty, length <= 20, only [A-Z0-9-] (you can reuse helper)
     - reporterEmail/assigneeEmail: must look like an email
     - title: non-empty, length <= 80
     - priority: one of LOW/MEDIUM/HIGH/CRITICAL
     - slaMinutes: if provided, must be between 5 and 7,200

4) Update `TicketService`
   - Stop mutating a ticket after creation
   - Any “updates” should create a **new** ticket instance (e.g., by Builder copy/from method)
   - Keep the API simple; you can add `toBuilder()` or `Builder.from(existing)`

Acceptance
- `IncidentTicket` has no public setters and fields are final.
- Tickets cannot be modified after creation (including tags list).
- Validation happens only in one place (`build()`).
- `TryIt` still works, but now demonstrates immutability (attempted mutations should not compile or have no effect).
- Code compiles and runs with the starter commands below.

Build/Run (Starter demo)
  cd immutable-tickets/src
  javac com/example/tickets/*.java TryIt.java
  java TryIt

Tip
After refactor, you can update `TryIt` to show:
- building a ticket
- “updating” by creating a new instance
- tags list is not mutable from outside



---

# Preparation Notes (Diagram Style)

## 11. Current Design (Broken Starter)

```
┌──────────────────────────────────────────────────────────────┐
│               IncidentTicket (BROKEN — MUTABLE)              │
│──────────────────────────────────────────────────────────────│
│  Fields (no final — all mutable):                            │
│    String id                                                 │
│    String title                                              │
│    String priority                                           │
│    String reporterEmail                                      │
│    public List<String> tags   ← public field, no copy        │
│    String assigneeEmail                                      │
│    boolean customerVisible                                   │
│    Integer slaMinutes                                        │
│    String source                                             │
│──────────────────────────────────────────────────────────────│
│  Multiple constructors — no single creation path:            │
│                                                              │
│    IncidentTicket(id, email, title)            ← 3-arg       │
│    IncidentTicket(id, email, title, priority)  ← 4-arg       │
│    ← caller must know which to use; easy to skip validation  │
│──────────────────────────────────────────────────────────────│
│  Public setters — anyone can mutate at any time:             │
│    setId(String)                                             │
│    setTitle(String)                                          │
│    setPriority(String)                                       │
│    setReporterEmail(String)                                  │
│    setAssigneeEmail(String)                                  │
│    setTags(List<String>)                                     │
│──────────────────────────────────────────────────────────────│
│  Validation scattered across the codebase:                   │
│    3-arg constructor → checks email format                   │
│    4-arg constructor → checks id length                      │
│    TicketService     → validates priority is non-null        │
│    ← NO single entry point — easy to miss rules              │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│       TicketService (BROKEN — MUTATES AFTER CREATION)        │
│──────────────────────────────────────────────────────────────│
│  createTicket(id, email, title):                             │
│    IncidentTicket t = new IncidentTicket(                    │
│        "TCK-1001", "reporter@example.com", "Bug"             │
│    );              ← 3-arg, no priority set yet              │
│    t.setPriority("HIGH");          ← MUTATES AFTER CREATION  │
│    t.setAssigneeEmail("a@b.com");  ← MUTATES AFTER CREATION  │
│    t.getTags().add("ESCALATED");   ← EXTERNAL LIST MUTATION  │
│    return t;                                                 │
│                                                              │
│  escalateToCritical(IncidentTicket t):                       │
│    t.setPriority("CRITICAL");      ← ORIGINAL OBJECT CHANGED │
│    t.getTags().add("ESCALATED");   ← SAME OBJECT MUTATED     │
│    ← t is now different everywhere it was referenced         │
└──────────────────────────────────────────────────────────────┘

MUTATION CHAIN — how one ticket silently changes state:

  IncidentTicket t = new IncidentTicket("TCK-1001", email, "Bug")
                       │  priority = null
                       │  tags     = []
                       ▼
  t.setPriority("HIGH");
                       │  priority = "HIGH"
                       ▼
  t.getTags().add("ESCALATED");
                       │  tags = ["ESCALATED"]
                       ▼
  service.escalateToCritical(t);   ← SAME object, mutated again
                       │  priority = "CRITICAL"
                       │  tags     = ["ESCALATED", "ESCALATED"]
                       ▼
  Audit log recorded "Bug created with priority HIGH"
  Object now shows CRITICAL — log is stale and wrong.
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: Public setters after construction  [MUTABILITY]    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: IncidentTicket — every set*() method                 │
│                                                              │
│  t.setPriority("CRITICAL");                                  │
│  t.setAssigneeEmail("newdev@example.com");                   │
│  t.setTitle("Updated title");                                │
│                                                              │
│  WHY IT'S BAD:                                               │
│    A ticket represents a FACT about an incident.             │
│    Facts should not change — updates should be new facts.    │
│    Two threads reading the same t at different times may     │
│    see completely different tickets with the same id.        │
│    Audit logs that captured t are stale as soon as t changes.│
│                                                              │
│  FIX: private final fields, no setters.                      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: Exposed tags list           [ENCAPSULATION BREAK]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: IncidentTicket.tags field / getTags() return value   │
│                                                              │
│  public List<String> tags;              ← public field       │
│  // OR getter returns the internal list reference            │
│                                                              │
│  ticket.getTags().add("HACKED");   ← modifies internal state │
│  ticket.getTags().clear();         ← wipes all tags          │
│                                                              │
│  The object has zero control over its own state.             │
│  Any caller can silently rewrite the tag list at any time.   │
│                                                              │
│  FIX: Defensive copy in constructor (new ArrayList<>(b.tags))│
│       getTags() returns Collections.unmodifiableList(tags).  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: Scattered validation        [VALIDATION SMELL]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: 3-arg constructor, 4-arg constructor, TicketService  │
│                                                              │
│  3-arg constructor  → checks reporterEmail format            │
│  4-arg constructor  → checks id length                       │
│  TicketService      → checks priority is non-null            │
│                                                              │
│  PROBLEMS:                                                   │
│    Use 3-arg constructor → id length never checked.          │
│    Use new IncidentTicket() directly → all checks skipped.   │
│    Add a new field → must remember to add checks in 3 places.│
│    A ticket can exist in an invalid state between creation   │
│    and the deferred validation calls.                        │
│                                                              │
│  FIX: ALL validation in Builder.build() — one place,         │
│       always runs, impossible to bypass.                     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: Multiple constructors       [API CONFUSION]        │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: IncidentTicket class                                 │
│                                                              │
│  new IncidentTicket(id, email, title)                        │
│  new IncidentTicket(id, email, title, priority)              │
│                                                              │
│  WHY IT'S BAD:                                               │
│    Caller must guess which constructor is "right".           │
│    Adding a 5th required field → add another constructor.    │
│    Telescoping constructors grow without bound.              │
│    Can't set optional fields without calling setters,        │
│    turning creation into a multi-step mutation sequence.     │
│                                                              │
│  FIX: One private constructor taking Builder.                │
│       Builder is the only path to construct a ticket.        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: In-place updates break audit trail  [AUDIT FAIL]   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: TicketService.escalateToCritical()                   │
│                                                              │
│  t.setPriority("CRITICAL");  ← SAME object modified          │
│                                                              │
│  TIMELINE:                                                   │
│    09:00 — ticket created, priority=HIGH, event logged       │
│    09:15 — t.setPriority("CRITICAL") called                  │
│    09:15 — log: "ticket is now CRITICAL"                     │
│    Where is the ticket as it was at 09:00? Gone.             │
│    t was mutated in-place. There is no "before" state.       │
│    Any reference to t now sees CRITICAL, not HIGH.           │
│                                                              │
│  FIX: escalateToCritical returns a NEW ticket.               │
│       Original t unchanged — the 09:00 state is preserved.   │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│               IncidentTicket (FIXED — IMMUTABLE)             │
│──────────────────────────────────────────────────────────────│
│  Fields (ALL private final — NO setters):                    │
│    private final String id;                                  │
│    private final String reporterEmail;                       │
│    private final String title;                               │
│    private final String description;                         │
│    private final String priority;                            │
│    private final List<String> tags;   ← defensive copy       │
│    private final String assigneeEmail;                       │
│    private final boolean customerVisible;                    │
│    private final Integer slaMinutes;                         │
│    private final String source;                              │
│──────────────────────────────────────────────────────────────│
│  ONE private constructor (only Builder can call it):         │
│    private IncidentTicket(Builder b) {                       │
│        this.id           = b.id;                             │
│        this.tags         = new ArrayList<>(b.tags); ← COPY  │
│        ...               all other fields assigned           │
│    }                                                         │
│──────────────────────────────────────────────────────────────│
│  Safe getters — no internal state leakage:                   │
│    getTags() → Collections.unmodifiableList(tags)            │
│    all others return scalar values directly                  │
│──────────────────────────────────────────────────────────────│
│  toBuilder() — the safe "update" mechanism:                  │
│    Returns a Builder pre-populated with all current values.  │
│    Caller changes only what they need, then calls build().   │
│    Result is a BRAND NEW IncidentTicket.                     │
│    Original ticket is UNCHANGED.                             │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│         IncidentTicket.Builder (inner static class)          │
│──────────────────────────────────────────────────────────────│
│  Fields (same names, mutable — Builder is the scratchpad):   │
│    String id / reporterEmail / title / description           │
│    String priority / assigneeEmail / source                  │
│    List<String> tags = new ArrayList<>()                     │
│    boolean customerVisible / Integer slaMinutes              │
│──────────────────────────────────────────────────────────────│
│  Fluent setters (each returns this for chaining):            │
│    .id(String)            ← required                         │
│    .reporterEmail(String) ← required                         │
│    .title(String)         ← required                         │
│    .priority(String)      ← required (LOW/MEDIUM/HIGH/CRIT)  │
│    .description(String)   ← optional                         │
│    .tags(List<String>)    ← optional                         │
│    .assigneeEmail(String) ← optional                         │
│    .slaMinutes(Integer)   ← optional (5–7200)                │
│    .customerVisible(bool) ← optional                         │
│    .source(String)        ← optional                         │
│──────────────────────────────────────────────────────────────│
│  build() — ALL validation in ONE place:                      │
│    Validation.requireTicketId(id)                            │
│    Validation.requireEmail(reporterEmail, "reporterEmail")   │
│    Validation.requireNonBlank(title, "title")                │
│    Validation.requireMaxLen(title, 80, "title")              │
│    Validation.requireOneOf(priority, "priority",             │
│        "LOW","MEDIUM","HIGH","CRITICAL")                     │
│    if (assigneeEmail != null)                                │
│        Validation.requireEmail(assigneeEmail, "assigneeEmail"│
│    Validation.requireRange(slaMinutes, 5, 7200, "slaMinutes")│
│    return new IncidentTicket(this);   ← only creation path   │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│     TicketService (FIXED — no mutation of existing objects)  │
│──────────────────────────────────────────────────────────────│
│  createTicket(id, email, title):                             │
│    return new IncidentTicket.Builder()                       │
│        .id(id)                                               │
│        .reporterEmail(email)                                 │
│        .title(title)                                         │
│        .priority("MEDIUM")                                   │
│        .source("CLI")                                        │
│        .tags(List.of("NEW"))                                 │
│        .build();          ← one shot, fully validated        │
│                                                              │
│  escalateToCritical(IncidentTicket t):  ← t NEVER TOUCHED   │
│    List<String> newTags = new ArrayList<>(t.getTags());      │
│    newTags.add("ESCALATED");                                 │
│    return t.toBuilder()                                      │
│        .priority("CRITICAL")                                 │
│        .tags(newTags)                                        │
│        .build();   ← BRAND NEW ticket, t still has old state │
│                                                              │
│  assign(IncidentTicket t, String assigneeEmail):             │
│    return t.toBuilder()                                      │
│        .assigneeEmail(assigneeEmail)                         │
│        .build();   ← NEW ticket, t unchanged                 │
└──────────────────────────────────────────────────────────────┘

HOW toBuilder() ENABLES SAFE "UPDATES":

  IncidentTicket original = new IncidentTicket.Builder()
      .id("TCK-1001").reporterEmail("r@x.com").title("Bug")
      .priority("HIGH").build();
                       │
                       ▼  original is HIGH — permanently
                       │
  IncidentTicket escalated = original.toBuilder()
      .priority("CRITICAL")
      .build();
                       │
                       ▼  escalated is CRITICAL
                       │
                       ▼  original is still HIGH  ← UNCHANGED

  Both objects exist. Audit log can reference original.
  No state was lost. No mutation happened.


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                                  AFTER
  ──────                                  ─────
  public setters                 →        no setters (private final fields)
  multiple constructors          →        one private constructor via Builder
  public / raw tag list          →        defensive copy + unmodifiableList
  validation in 3 separate places→        all validation in Builder.build()
  t.set*() mutates original      →        t.toBuilder()...build() → new ticket
  audit trail broken by mutation →        original always preserved
```

