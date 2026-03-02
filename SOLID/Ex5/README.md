# Ex5 — LSP: File Exporter Hierarchy

## 1. Context
A reporting tool exports student performance data to multiple formats.

## 2. Current behavior
- `Exporter` has `export(ExportRequest)` that returns `ExportResult`
- `PdfExporter` throws for large content (tightens preconditions)
- `CsvExporter` silently changes meaning by dropping newlines and commas poorly
- `JsonExporter` returns empty on null (inconsistent contract)
- `Main` demonstrates current behavior

## 3. What’s wrong (at least 5 issues)
1. Subclasses violate expectations of the base `Exporter` contract.
2. `PdfExporter` throws for valid requests (from base perspective).
3. `CsvExporter` changes semantics of fields (data corruption risk).
4. `JsonExporter` handles null differently than others.
5. Callers cannot rely on substitutability; they need format-specific workarounds.
6. Contract is not documented; behavior surprises are runtime.

## 4. Your task
Checkpoint A: Run and capture output.
Checkpoint B: Define a clear base contract (preconditions/postconditions).
Checkpoint C: Refactor hierarchy so all exporters honor the same contract.
Checkpoint D: Keep observable outputs identical for current inputs.

## 5. Constraints
- Keep `Main` outputs unchanged for the given samples.
- No external libraries.
- Default package.

## 6. Acceptance criteria
- Base contract is explicit and enforced consistently.
- No exporter tightens preconditions compared to base contract.
- Caller should not need `instanceof` to be safe.

## 7. How to run
```bash
cd SOLID/Ex5/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Export Demo ===
PDF: ERROR: PDF cannot handle content > 20 chars
CSV: OK bytes=42
JSON: OK bytes=61
```

## 9. Hints (OOP-only)
- If a subtype cannot support the base contract, reconsider inheritance.
- Prefer composition: separate “format encoding” from “delivery constraints”.

## 10. Stretch goals
- Add a new exporter without changing existing exporters.


---

# Preparation Notes (Diagram Style)

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ExportRequest req = new ExportRequest(                      │
│      "Weekly Report",                                        │
│      "Name,Score\nAyaan,82\nRiya,91\n"  ← 30 chars          │
│  );                                                          │
│                                                              │
│  Exporter pdf  = new PdfExporter();                          │
│  Exporter csv  = new CsvExporter();                          │
│  Exporter json = new JsonExporter();                         │
│                                                              │
│  safe(pdf, req)   → try e.export(req) catch RuntimeException │
│  safe(csv, req)   → try e.export(req) catch RuntimeException │
│  safe(json, req)  → try e.export(req) catch RuntimeException │
│                                                              │
│  ⚠ Main NEEDS try-catch because it CAN'T TRUST              │
│    that all exporters behave the same way.                   │
│    If substitutability worked, try-catch wouldn't be needed. │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│           abstract class Exporter                            │
│──────────────────────────────────────────────────────────────│
│  abstract ExportResult export(ExportRequest req)             │
│                                                              │
│  ⚠ NO CONTRACT DEFINED:                                     │
│    - What if req is null?         → not specified            │
│    - What if body is very long?   → not specified            │
│    - Can it throw?                → not specified            │
│    - Must it always return bytes? → not specified            │
│                                                              │
│  Each subclass makes its OWN rules.                          │
└──────┬───────────────┬───────────────┬───────────────────────┘
       │               │               │
       ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ PdfExporter  │ │ CsvExporter  │ │ JsonExporter │
│──────────────│ │──────────────│ │──────────────│
│ export(req): │ │ export(req): │ │ export(req): │
│              │ │              │ │              │
│ ⚠ if body   │ │ ⚠ replaces   │ │ ⚠ if req ==  │
│   > 20 chars │ │   \n → space │ │   null →     │
│   THROWS     │ │   ,  → space │ │   return     │
│   Illegal-   │ │              │ │   empty      │
│   Argument-  │ │   DATA IS    │ │   result     │
│   Exception  │ │   CORRUPTED  │ │              │
│              │ │   silently   │ │ others would │
│ TIGHTENS     │ │              │ │ crash on null│
│ precondition │ │ WEAKENS      │ │              │
│ (base allows │ │ postcondition│ │ INCONSISTENT │
│  any length) │ │ (data not    │ │ contract     │
│              │ │  preserved)  │ │              │
└──────────────┘ └──────────────┘ └──────────────┘

EXECUTION for req = ("Weekly Report", "Name,Score\nAyaan,82\nRiya,91\n"):

  pdf.export(req):
    body.length() = 30, > 20
    → THROWS IllegalArgumentException("PDF cannot handle content > 20 chars")
    → Main catches it → "ERROR: PDF cannot handle content > 20 chars"

  csv.export(req):
    body = "Name,Score\nAyaan,82\nRiya,91\n"
    → replace \n with space: "Name,Score Ayaan,82 Riya,91 "
    → replace , with space:  "Name Score Ayaan 82 Riya 91 "
    → csv = "title,body\nWeekly Report,Name Score Ayaan 82 Riya 91 \n"
    → 42 bytes → "OK bytes=42"
    ⚠ DATA CORRUPTED: commas and newlines that were PART OF THE DATA are gone

  json.export(req):
    req != null, so normal path
    → json = {"title":"Weekly Report","body":"Name,Score\nAyaan,82\nRiya,91\n"}
    → 61 bytes → "OK bytes=61"
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: PdfExporter TIGHTENS precondition  [LSP VIOLATION] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: PdfExporter.export()                                 │
│                                                              │
│  if (req.body.length() > 20)                                 │
│      throw new IllegalArgumentException(...)                 │
│                                                              │
│  BASE CONTRACT (Exporter):                                   │
│    export(ExportRequest) → ExportResult                      │
│    No mention of length limits. Any ExportRequest is valid.  │
│                                                              │
│  PDF SAYS: "I only accept body ≤ 20 chars"                   │
│    This is a TIGHTER precondition than the base.             │
│                                                              │
│  LSP RULE: Subtype must accept EVERYTHING the base accepts.  │
│    Subtype can accept MORE (weaken precondition) = OK        │
│    Subtype can accept LESS (tighten precondition) = VIOLATES │
│                                                              │
│  RESULT: Caller writes code for Exporter, passes valid req.  │
│    Swap in PdfExporter → BOOM, exception at runtime.         │
│    Not substitutable.                                        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: CsvExporter WEAKENS postcondition  [LSP VIOLATION] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: CsvExporter.export()                                 │
│                                                              │
│  body.replace("\n", " ").replace(",", " ")                   │
│                                                              │
│  EXPECTED POSTCONDITION:                                     │
│    Exported data should faithfully represent the input.      │
│    What goes in should come back out (in the format).        │
│                                                              │
│  CSV SAYS: "I'll silently destroy commas and newlines"       │
│    Input:  "Name,Score\nAyaan,82"                            │
│    Output: "Name Score Ayaan 82"  ← DATA CORRUPTED           │
│                                                              │
│  This is a WEAKER postcondition — promises LESS than base.   │
│                                                              │
│  LSP RULE: Subtype must guarantee EVERYTHING the base does.  │
│    Subtype can guarantee MORE (strengthen postcon) = OK      │
│    Subtype can guarantee LESS (weaken postcon) = VIOLATES    │
│                                                              │
│  RESULT: Caller expects data integrity.                      │
│    Swap in CsvExporter → data silently corrupted.            │
│    No error. No warning. Just wrong output.                  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: JsonExporter handles null differently [LSP VIOLN]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: JsonExporter.export()                                │
│                                                              │
│  if (req == null) return new ExportResult("application/json",│
│                                            new byte[0]);     │
│                                                              │
│  WHAT OTHERS DO WITH null req:                               │
│    PdfExporter  → NullPointerException (crash)               │
│    CsvExporter  → NullPointerException (crash)               │
│    JsonExporter → returns empty result (silent)              │
│                                                              │
│  THREE subclasses, THREE different behaviors for null.       │
│  No consistent contract. Caller can't predict what happens.  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: Main needs try-catch (can't trust hierarchy)       │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: Main.safe() method                                   │
│                                                              │
│  try { e.export(r); } catch (RuntimeException ex) { ... }    │
│                                                              │
│  WHY: Because PdfExporter THROWS for valid input.            │
│    If LSP held, Main could just call e.export(r) directly.   │
│    The try-catch IS the symptom of broken substitutability.  │
│                                                              │
│  IF you need instanceof or try-catch to handle subtypes      │
│  differently → the hierarchy violates LSP.                   │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: No defined contract on Exporter    [DESIGN SMELL]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  abstract ExportResult export(ExportRequest req);            │
│                                                              │
│  WHAT'S MISSING:                                             │
│    Preconditions:   Can req be null? Can body be null?        │
│                     Is there a size limit?                    │
│    Postconditions:  Must bytes be non-empty?                  │
│                     Must data be faithful to input?           │
│    Error handling:  Throw? Return error object? Return null?  │
│                                                              │
│  Without a defined contract, each subclass invents its own.  │
│  That's exactly WHY we have 3 different behaviors.           │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│  FIX 1: Add error handling TO THE CONTRACT (ExportResult)    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE ExportResult:                                        │
│    contentType: String                                       │
│    bytes: byte[]                                             │
│    ← no way to say "I failed" without throwing               │
│                                                              │
│  AFTER ExportResult:                                         │
│  ┌──────────────────────────────────────────┐                │
│  │  ExportResult                            │                │
│  │──────────────────────────────────────────│                │
│  │  contentType: String                     │                │
│  │  bytes: byte[]                           │                │
│  │  success: boolean        ← NEW           │                │
│  │  errorMessage: String    ← NEW           │                │
│  │──────────────────────────────────────────│                │
│  │  ExportResult(contentType, bytes)        │                │
│  │    → success=true, errorMessage=null     │                │
│  │                                          │                │
│  │  static ExportResult.error(message)      │                │
│  │    → success=false, bytes=empty          │                │
│  └──────────────────────────────────────────┘                │
│                                                              │
│  NOW: Exporters can report failure WITHOUT throwing.         │
│  Error handling is part of the contract, not a surprise.     │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 2: PdfExporter — return error instead of throwing       │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    if (body.length() > 20)                                   │
│        throw new IllegalArgumentException(...)   ← THROWS!   │
│                                                              │
│  AFTER:                                                      │
│    if (body.length() > 20)                                   │
│        return ExportResult.error("PDF cannot handle...")      │
│                            ⚠ returns, NOT throws             │
│                                                              │
│  Same information. Same message. But now it's                │
│  PART OF THE CONTRACT, not a runtime surprise.               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 3: CsvExporter — proper escaping instead of corruption  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    body.replace("\n", " ").replace(",", " ")                 │
│    → DATA LOST. "Name,Score" becomes "Name Score"            │
│                                                              │
│  AFTER:                                                      │
│    csvEscape(s):                                             │
│      if s contains , or \n or "                              │
│        → wrap in quotes, escape inner quotes                 │
│        → "Name,Score\nAyaan,82" stays intact                 │
│                                                              │
│  Data is PRESERVED, not destroyed.                           │
│  Postcondition now matches base expectation.                 │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 4: JsonExporter — consistent null handling              │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    if (req == null) return empty result  ← special case      │
│                                                              │
│  AFTER:                                                      │
│    No special null-req handling.                              │
│    Null fields (title/body) → treated as empty string "".    │
│    Same approach as other exporters.                         │
│    Consistent contract across all subtypes.                  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 5: Main — no try-catch needed anymore                   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    try { e.export(r); } catch (RuntimeException ex) { ... }  │
│    ← defensive because PdfExporter might throw               │
│                                                              │
│  AFTER:                                                      │
│    ExportResult result = e.export(r);                        │
│    if (!result.success)                                      │
│        return "ERROR: " + result.errorMessage;               │
│    return "OK bytes=" + result.bytes.length;                 │
│                                                              │
│  No try-catch. No instanceof. Just check the contract.       │
│  ALL exporters behave the same way. SUBSTITUTABLE.           │
└──────────────────────────────────────────────────────────────┘


LSP SUMMARY:

  Exporter base contract NOW defines:
    Precondition:  req must be non-null (ALL subtypes accept this)
    Postcondition: returns ExportResult (success or error, NEVER throws)
    Data rule:     export must faithfully represent input data

  BEFORE                               AFTER
  ──────                               ─────
  PdfExporter THROWS on long body →   Returns ExportResult.error()
  CsvExporter CORRUPTS data       →   Proper CSV escaping
  JsonExporter special null case  →   Consistent null handling
  Main needs try-catch            →   Main checks result.success
  No contract defined             →   Clear pre/postconditions

  ANY Exporter subtype can be swapped in.
  Caller code NEVER changes. That's LSP.
```
