Proxy — Secure & Lazy-Load Reports (Refactoring)
------------------------------------------------
Narrative (Current Code)
A small CLI tool called CampusVault opens internal reports for different users.
Right now, ReportViewer talks directly to ReportFile and eagerly loads the report content every time.

Problems in the current design:
- No access control: any user can open any report.
- No lazy loading: expensive file loading happens immediately on each open.
- No caching: the same report may be loaded multiple times unnecessarily.
- Clients depend directly on the concrete implementation.

Your Task
1) Introduce a Report abstraction.
2) Keep the expensive file-reading logic inside a real subject (for example, RealReport).
3) Add a ReportProxy that:
   - checks whether the user is allowed to access the report
   - lazy-loads the real report only when needed
   - reuses the loaded real report for repeated views through the same proxy
4) Update ReportViewer / App so clients use the proxy instead of directly using the concrete file loader.

Acceptance Criteria
- Unauthorized users cannot view restricted reports.
- Real report loading happens only when access is granted.
- Real report content is loaded lazily (not during proxy construction).
- Repeated views of the same report through the same proxy should not reload the file every time.
- Output remains easy to verify from console logs.

Hints
- Define an interface: Report { void display(User user); }
- Let RealReport do the expensive load.
- Let ReportProxy hold metadata + a nullable RealReport reference.
- Add logs so it is obvious whether a report was really loaded.

Build & Run
  cd proxy-reports/src
  javac com/example/reports/*.java
  java com.example.reports.App

Repo intent
This is a refactoring assignment: the starter code works, but it does not use Proxy properly.
Students should refactor the design so access control + lazy loading happen via a proxy.

---

# Preparation Notes (Diagram Style)

## 11. Current Design (Broken Starter)

```
┌──────────────────────────────────────────────────────────────┐
│             ReportFile (BROKEN — CONCRETE, NO PROXY)          │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    String reportId                                           │
│    String title                                              │
│    String classification    (PUBLIC / FACULTY / ADMIN)        │
│──────────────────────────────────────────────────────────────│
│  display(User user):                                         │
│    String content = loadFromDisk();  ← EAGER, every call     │
│    print report header + content                             │
│                                                              │
│  ⚠ NO access control — any user can view any report          │
│  ⚠ NO lazy loading — expensive disk read happens every time  │
│  ⚠ NO caching — same report reloaded on every display()      │
│──────────────────────────────────────────────────────────────│
│  loadFromDisk():                                             │
│    System.out.println("[disk] loading...");                   │
│    Thread.sleep(120);    ← simulates expensive I/O           │
│    return content string                                     │
│                                                              │
│  Called EVERY time display() is invoked.                      │
│  Viewing the same report 5 times → 5 disk loads.             │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│        ReportViewer / App (BROKEN — DIRECT DEPENDENCY)        │
│──────────────────────────────────────────────────────────────│
│  App creates ReportFile objects directly:                     │
│    ReportFile r = new ReportFile("R-101", "Plan", "ADMIN");  │
│    r.display(student);   ← student sees ADMIN report! ⚠     │
│    r.display(admin);     ← loads from disk again! ⚠          │
│    r.display(admin);     ← loads from disk AGAIN! ⚠          │
│                                                              │
│  No interface abstraction — clients coupled to concrete.     │
│  Cannot insert access control without changing ReportFile.   │
│  Cannot add lazy loading without changing ReportFile.        │
└──────────────────────────────────────────────────────────────┘

THREE PROBLEMS in the starter:

  1. No access control
       Student calls reportFile.display(student) on ADMIN report
       → report is shown. No check. No denial.

  2. Eager loading every time
       display() calls loadFromDisk() internally, always.
       Viewing a report → 120ms disk wait, every single time.

  3. No caching
       display() the same report 3 times → 3 × loadFromDisk()
       → 360ms wasted. Content is identical each time.

  4. Concrete dependency
       App/ReportViewer use ReportFile directly.
       Cannot swap in a proxy without rewriting callers.
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: No access control               [SECURITY]         │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: ReportFile.display() — no user role check            │
│                                                              │
│  ReportFile("R-303", "Budget Audit", "ADMIN")                │
│  r.display(student);   ← student is STUDENT role             │
│  → report is shown! No permission check anywhere.            │
│                                                              │
│  IMPACT:                                                     │
│    Any user can view any report regardless of classification.│
│    ADMIN reports visible to STUDENT users.                   │
│    FACULTY reports visible to everyone.                      │
│    Classification field exists but is never enforced.        │
│                                                              │
│  FIX: Proxy checks user role against classification          │
│       BEFORE loading or displaying the report.               │
│       Access denied → return early, never touch real report. │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: Eager loading on every call      [PERFORMANCE]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: ReportFile.display() → loadFromDisk()                │
│                                                              │
│  display(user) {                                             │
│      String content = loadFromDisk();  ← 120ms EVERY call    │
│      print content                                           │
│  }                                                           │
│                                                              │
│  IMPACT:                                                     │
│    Report content loaded even if access will be denied.      │
│    Report content reloaded even if nothing changed.          │
│    3 views of same report = 3 disk loads = 360ms.            │
│                                                              │
│  FIX: RealReport loads content ONCE in constructor.          │
│       Proxy creates RealReport lazily (only when needed).    │
│       Subsequent displays reuse the already-loaded content.  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: No caching of loaded reports     [REDUNDANCY]      │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: ReportFile — content discarded after each display()  │
│                                                              │
│  loadFromDisk() returns a string → used once → discarded     │
│  Next display() → loadFromDisk() again → same string         │
│                                                              │
│  No field stores the loaded content between calls.           │
│  The expensive I/O is repeated needlessly.                   │
│                                                              │
│  FIX: Proxy holds a nullable RealReport reference.           │
│       First authorized display() → create RealReport (load). │
│       Second authorized display() → reuse existing reference.│
│       "[disk] loading..." message appears only ONCE.         │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: Clients coupled to concrete class  [COUPLING]      │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: App and ReportViewer use ReportFile directly         │
│                                                              │
│  ReportFile r = new ReportFile(...);                         │
│  r.display(user);                                            │
│                                                              │
│  No interface — cannot substitute a proxy transparently.     │
│  Adding access control requires modifying ReportFile itself  │
│  or adding checks in every caller.                           │
│                                                              │
│  FIX: Define Report interface { void display(User user); }   │
│       ReportFile → RealReport implements Report              │
│       ReportProxy implements Report                          │
│       Clients depend on Report — proxy is invisible to them. │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│       Report (INTERFACE — ABSTRACTION)                        │
│──────────────────────────────────────────────────────────────│
│  void display(User user)                                     │
│  ← both RealReport and ReportProxy implement this            │
│  ← clients depend only on this interface                     │
└──────────────────────────────────────────────────────────────┘
          ▲                               ▲
          │ implements                     │ implements
          │                               │
┌─────────────────────────┐   ┌─────────────────────────────┐
│  RealReport             │   │  ReportProxy                 │
│  (REAL SUBJECT)         │   │  (PROXY)                     │
│─────────────────────────│   │─────────────────────────────│
│  reportId, title,       │   │  reportId, title,            │
│  classification,        │   │  classification              │
│  content                │   │  AccessControl accessControl │
│                         │   │  RealReport realReport = null│
│─────────────────────────│   │  ← nullable, lazy            │
│  Constructor:           │   │─────────────────────────────│
│    [disk] loading...    │   │  display(User user):          │
│    Thread.sleep(120)    │   │    1. accessControl.canAccess │
│    content = "..."      │   │       (user, classification)? │
│    ← expensive, once    │   │       NO → print ACCESS DENIED│
│                         │   │              return           │
│  display(User):         │   │    2. if (realReport == null) │
│    print header+content │   │         realReport =          │
│    ← no access check    │   │           new RealReport(...) │
│    ← just renders       │   │       ← LAZY: loaded only now│
│                         │   │    3. realReport.display(user)│
│                         │   │       ← delegates to real     │
└─────────────────────────┘   └─────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│       AccessControl (PERMISSION CHECKER)                      │
│──────────────────────────────────────────────────────────────│
│  canAccess(User user, String classification) → boolean       │
│                                                              │
│  Rules:                                                      │
│    PUBLIC   → any role                                       │
│    FACULTY  → FACULTY or ADMIN                               │
│    ADMIN    → ADMIN only                                     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│      App (FIXED — USES PROXY, CLIENTS UNAWARE)               │
│──────────────────────────────────────────────────────────────│
│  Report publicReport  = new ReportProxy("R-101", "...",      │
│                             "PUBLIC");                        │
│  Report facultyReport = new ReportProxy("R-202", "...",      │
│                             "FACULTY");                       │
│  Report adminReport   = new ReportProxy("R-303", "...",      │
│                             "ADMIN");                         │
│                                                              │
│  ReportViewer viewer = new ReportViewer();                    │
│                                                              │
│  viewer.open(publicReport, student);                         │
│    → access OK → [disk] loading → displays content           │
│                                                              │
│  viewer.open(facultyReport, student);                        │
│    → ACCESS DENIED (STUDENT cannot view FACULTY)             │
│    → no disk load, no content shown                          │
│                                                              │
│  viewer.open(facultyReport, faculty);                        │
│    → access OK → [disk] loading → displays content           │
│                                                              │
│  viewer.open(adminReport, admin);                            │
│    → access OK → [disk] loading → displays content           │
│                                                              │
│  viewer.open(adminReport, admin);                            │
│    → access OK → already loaded (no [disk] message)          │
│    → displays cached content                                 │
│                                                              │
│  ReportViewer.open() calls report.display(user).             │
│  It has NO idea whether it's a proxy or real report.         │
└──────────────────────────────────────────────────────────────┘

HOW THE PROXY CONTROLS ACCESS + LAZY LOADING:

  viewer.open(adminReport, student)
       │
       ▼
  ReportProxy.display(student)
       │
       ├─ accessControl.canAccess(STUDENT, "ADMIN") → false
       │
       └─ "ACCESS DENIED" — return immediately
          RealReport never created. No disk load. No 120ms wait.

  viewer.open(adminReport, admin)
       │
       ▼
  ReportProxy.display(admin)
       │
       ├─ accessControl.canAccess(ADMIN, "ADMIN") → true
       │
       ├─ realReport == null → create new RealReport(...)
       │     [disk] loading report R-303 ...  ← 120ms, once
       │
       └─ realReport.display(admin) → prints content

  viewer.open(adminReport, admin)  ← SECOND TIME
       │
       ▼
  ReportProxy.display(admin)
       │
       ├─ accessControl.canAccess(ADMIN, "ADMIN") → true
       │
       ├─ realReport != null → SKIP creation (already cached)
       │     no [disk] loading message — no I/O
       │
       └─ realReport.display(admin) → prints content instantly


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                                  AFTER
  ──────                                  ─────
  no Report interface             →       Report interface
  ReportFile used directly        →       ReportProxy wraps RealReport
  no access control               →       AccessControl checks role
  eager load every display()      →       lazy load on first authorized view
  no caching                      →       RealReport cached in proxy
  client coupled to concrete      →       client depends on Report interface
  STUDENT sees ADMIN reports      →       STUDENT gets ACCESS DENIED
  3 views = 3 disk loads          →       3 views = 1 disk load
```
