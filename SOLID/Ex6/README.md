# Ex6 — LSP: Notification Sender Inheritance

## 1. Context
A campus system sends notifications via email, SMS, and WhatsApp.

## 2. Current behavior
- `NotificationSender.send(Notification)` is the base method
- `EmailSender` silently truncates messages (changes meaning)
- `WhatsAppSender` rejects non-E.164 numbers (tightens precondition)
- `SmsSender` ignores subject but base type implies subject may be used

## 3. What’s wrong (at least 5 issues)
1. Subtypes impose extra constraints not present in base contract.
2. Subtypes change semantics (truncation, ignoring fields).
3. Callers cannot rely on base behavior without knowing subtype.
4. Runtime surprises (exceptions) force subtype-specific handling.
5. Contract is vague and untested; inheritance is misused.

## 4. Your task
- Make substitutability true: if code works with `NotificationSender`, it should work with any sender.
- Preserve current outputs for the sample inputs in `Main`.

## 5. Constraints
- Preserve console output for current demo.
- No external libs.

## 6. Acceptance criteria
- Base contract is clear and upheld.
- No subtype tightens preconditions compared to base.

## 7. How to run
```bash
cd SOLID/Ex6/src
javac *.java
java Main
```

## 8. Sample output
```text
=== Notification Demo ===
EMAIL -> to=riya@sst.edu subject=Welcome body=Hello and welcome to SST!
SMS -> to=9876543210 body=Hello and welcome to SST!
WA ERROR: phone must start with + and country code
AUDIT entries=3
```

## 9. Hints (OOP-only)
- If channels have different requirements, avoid forcing them into a single inherited contract.
- Consider separating validation/normalization as a responsibility.

## 10. Stretch goals
- Add a new sender without editing existing ones.


## 10. Stretch goals
- Add a new sender without editing existing ones.

---

# Preparation Notes (Diagram Style)

## 11. Current Design

```
┌──────────────────────────────────────────────────────────────┐
│                         Main.java                            │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  Notification n = new Notification(                          │
│      "Welcome",                                              │
│      "Hello and welcome to SST!",                            │
│      "riya@sst.edu",                                         │
│      "9876543210"            ← NO "+" prefix                 │
│  );                                                          │
│                                                              │
│  AuditLog audit = new AuditLog();                            │
│                                                              │
│  NotificationSender email = new EmailSender(audit);          │
│  NotificationSender sms   = new SmsSender(audit);            │
│  NotificationSender wa    = new WhatsAppSender(audit);       │
│                                                              │
│  email.send(n);    → works                                   │
│  sms.send(n);      → works                                  │
│  try {                                                       │
│      wa.send(n);   → ⚠ THROWS! needs try-catch              │
│  } catch (RuntimeException ex) {                             │
│      "WA ERROR: " + ex.getMessage()                          │
│      audit.add("WA failed")                                  │
│  }                                                           │
│                                                              │
│  ⚠ Main can't treat all senders the same.                   │
│    WhatsApp needs special handling = NOT substitutable.       │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│         abstract class NotificationSender                    │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    protected final AuditLog audit;                           │
│                                                              │
│  abstract void send(Notification n);                         │
│               ^^^^                                           │
│  Returns VOID — no way to report failure except THROWING.    │
│                                                              │
│  ⚠ NO CONTRACT DEFINED:                                     │
│    - Can it throw? Not specified                             │
│    - Must it use all fields (subject, body, email, phone)?   │
│    - Can it modify the body? Not specified                   │
│    - What if phone is invalid? Not specified                 │
└──────┬───────────────┬───────────────┬───────────────────────┘
       │               │               │
       ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌───────────────┐
│ EmailSender  │ │ SmsSender    │ │ WhatsAppSender│
│──────────────│ │──────────────│ │───────────────│
│ send(n):     │ │ send(n):     │ │ send(n):      │
│              │ │              │ │               │
│ ⚠ if body   │ │ ⚠ IGNORES    │ │ ⚠ if phone    │
│   > 40 chars │ │   n.subject  │ │   doesn't     │
│   TRUNCATES  │ │   completely │ │   start "+"   │
│   silently   │ │              │ │   THROWS      │
│              │ │ prints only  │ │   Illegal-    │
│ body gets    │ │ phone + body │ │   Argument-   │
│ CHOPPED to   │ │              │ │   Exception   │
│ 40 chars     │ │ subject is   │ │               │
│              │ │ just DROPPED │ │ TIGHTENS      │
│ WEAKENS      │ │              │ │ precondition  │
│ postcondition│ │ WEAKENS      │ │               │
│ (data lost)  │ │ postcondition│ │               │
│              │ │ (field lost) │ │               │
└──────────────┘ └──────────────┘ └───────────────┘
         │               │               │
         ▼               ▼               ▼
┌──────────────────────────────────────────────────────────────┐
│  AuditLog                                                    │
│──────────────────────────────────────────────────────────────│
│  entries: List<String>                                       │
│  add("email sent") / add("sms sent") / add("WA failed")     │
│  size() → 3                                                  │
└──────────────────────────────────────────────────────────────┘

┌────────────────────────┐  ┌────────────────────────┐
│   SenderConfig         │  │   ConsolePreview       │
│────────────────────────│  │────────────────────────│
│ maxLen = 160           │  │ preview(String s)      │
│                        │  │                        │
│ DEAD CODE              │  │ DEAD CODE              │
│ never used anywhere    │  │ never used anywhere    │
└────────────────────────┘  └────────────────────────┘

EXECUTION:

  email.send(n):
    body = "Hello and welcome to SST!" (25 chars, ≤ 40)
    → no truncation this time
    → "EMAIL -> to=riya@sst.edu subject=Welcome body=Hello and welcome to SST!"
    → audit.add("email sent")

  sms.send(n):
    → IGNORES subject entirely
    → "SMS -> to=9876543210 body=Hello and welcome to SST!"
    → audit.add("sms sent")

  wa.send(n):
    phone = "9876543210", does NOT start with "+"
    → THROWS IllegalArgumentException("phone must start with + and country code")
    → Main catches it → "WA ERROR: phone must start with + and country code"
    → audit.add("WA failed")

  audit.size() → 3 (email sent, sms sent, WA failed)
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: WhatsAppSender TIGHTENS precondition [LSP VIOLN]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: WhatsAppSender.send()                                │
│                                                              │
│  if (phone == null || !phone.startsWith("+"))                │
│      throw new IllegalArgumentException(...)                 │
│                                                              │
│  BASE CONTRACT (NotificationSender):                         │
│    void send(Notification n) — no mention of phone format.   │
│    Any Notification with a phone field is valid.             │
│                                                              │
│  WHATSAPP SAYS: "phone MUST start with +"                    │
│    This is TIGHTER than what the base requires.              │
│                                                              │
│  LSP RULE:                                                   │
│    Subtype can accept MORE (weaken precondition) = OK        │
│    Subtype can accept LESS (tighten precondition) = VIOLATES │
│                                                              │
│  RESULT: Caller writes code for NotificationSender.          │
│    Swap in WhatsAppSender → BOOM, exception at runtime.      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: EmailSender TRUNCATES body silently [LSP VIOLN]    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: EmailSender.send()                                   │
│                                                              │
│  if (body.length() > 40) body = body.substring(0, 40);       │
│                                                              │
│  EXPECTED: send the full notification as provided.           │
│  ACTUAL: silently chops body to 40 chars. Data LOST.         │
│                                                              │
│  This is a WEAKER postcondition — promises LESS than base.   │
│  Caller sends "Hello and welcome to SST! Please check..."    │
│  Email sends  "Hello and welcome to SST! Please check" ←CUT │
│                                                              │
│  No error. No warning. Just data corruption.                 │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: SmsSender IGNORES subject field    [LSP VIOLATION] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: SmsSender.send()                                     │
│                                                              │
│  prints: "SMS -> to=" + n.phone + " body=" + n.body          │
│  ⚠ n.subject is NEVER used                                  │
│                                                              │
│  Base contract accepts Notification with subject+body.       │
│  SmsSender silently drops subject = weaker postcondition.    │
│                                                              │
│  (Debatable — SMS genuinely has no subject.                  │
│   But it reveals the base contract is too broad.)            │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: Main needs try-catch for WhatsApp                  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: Main.main()                                          │
│                                                              │
│  email.send(n);   ← just call                                │
│  sms.send(n);     ← just call                                │
│  try {                                                       │
│      wa.send(n);  ← ⚠ SPECIAL HANDLING                      │
│  } catch ...                                                 │
│                                                              │
│  Main KNOWS that WhatsApp is different.                      │
│  If LSP held, all three would be: sender.send(n); — done.   │
│  try-catch IS the symptom of broken substitutability.        │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: void return — no way to report failure             │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: NotificationSender.send() returns void               │
│                                                              │
│  abstract void send(Notification n);                         │
│           ^^^^                                               │
│  Only option for failure: THROW an exception.                │
│  No structured way to say "I failed, here's why."           │
│  This FORCES WhatsApp to throw = FORCES the LSP violation.   │
│                                                              │
│  The void contract is the ROOT CAUSE of the problem.         │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 6: Dead code                       [CODE SMELL]       │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  SenderConfig   → maxLen=160, NEVER used anywhere            │
│  ConsolePreview → preview(), NEVER used anywhere             │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│  FIX 1: SendResult class — structured error reporting        │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  ┌──────────────────────────────────────────┐                │
│  │  SendResult                              │                │
│  │──────────────────────────────────────────│                │
│  │  success: boolean                        │                │
│  │  errorMessage: String                    │                │
│  │──────────────────────────────────────────│                │
│  │  static SendResult.ok()                  │                │
│  │    → success=true, errorMessage=null     │                │
│  │                                          │                │
│  │  static SendResult.error(message)        │                │
│  │    → success=false, errorMessage=msg     │                │
│  └──────────────────────────────────────────┘                │
│                                                              │
│  NOW: Senders can report failure WITHOUT throwing.           │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 2: Change return type from void → SendResult            │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    abstract void send(Notification n);                       │
│             ^^^^                                             │
│  AFTER:                                                      │
│    abstract SendResult send(Notification n);                 │
│             ^^^^^^^^^^                                       │
│                                                              │
│  This is the KEY change. Error handling is now               │
│  PART OF THE CONTRACT, not an exception surprise.            │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 3: WhatsAppSender — return error instead of throwing    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    if (!phone.startsWith("+"))                               │
│        throw new IllegalArgumentException(...)  ← THROWS!    │
│                                                              │
│  AFTER:                                                      │
│    if (!phone.startsWith("+"))                               │
│        audit.add("WA failed");                               │
│        return SendResult.error("phone must start with +...")  │
│                          ⚠ returns, NOT throws               │
│                                                              │
│  Same information. Same message. No explosion.               │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 4: EmailSender — no silent truncation                   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    if (body.length() > 40) body = body.substring(0, 40);     │
│    → silently chops data                                     │
│                                                              │
│  AFTER:                                                      │
│    Sends full body as-is. No truncation.                     │
│    return SendResult.ok();                                   │
│                                                              │
│  Data integrity preserved. Postcondition matches base.       │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  FIX 5: Main — no try-catch needed anymore                   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  BEFORE:                                                     │
│    email.send(n);                                            │
│    sms.send(n);                                              │
│    try { wa.send(n); } catch (...) { ... }  ← SPECIAL       │
│                                                              │
│  AFTER:                                                      │
│    email.send(n);                                            │
│    sms.send(n);                                              │
│    SendResult waResult = wa.send(n);                         │
│    if (!waResult.success)                                    │
│        System.out.println("WA ERROR: " + waResult.error);   │
│                                                              │
│  No try-catch. Uniform handling. All senders substitutable.  │
└──────────────────────────────────────────────────────────────┘


LSP SUMMARY:

  BEFORE                                    AFTER
  ──────                                    ─────
  WhatsAppSender THROWS on bad phone   →   Returns SendResult.error()
  EmailSender TRUNCATES body silently  →   Sends full body
  send() returns void (no error path)  →   Returns SendResult
  Main needs try-catch for WhatsApp    →   Main checks result.success
  No contract defined                  →   Clear contract: never throws,
                                            always returns SendResult

  ANY NotificationSender subtype can be swapped in.
  Caller code NEVER changes. That's LSP.
```

