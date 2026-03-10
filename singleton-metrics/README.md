Exercise A — Singleton Refactoring (Metrics Registry)
----------------------------------------------------
Narrative
A CLI tool called **PulseMeter** collects runtime metrics (counters) and exposes them globally
so any part of the app can increment counters like `REQUESTS_TOTAL`, `DB_ERRORS`, etc.

The current implementation is **not a real singleton**, **not thread-safe**, and is vulnerable to
**reflection** and **serialization** breaking the singleton guarantee.

Your job is to refactor it into a **proper, thread-safe, lazy-initialized Singleton**.

What you have (Starter)
- `MetricsRegistry` is *intended* to be global, but:
  - `getInstance()` can return different objects under concurrency.
  - The constructor is not private.
  - Reflection can create multiple instances.
  - Serialization/deserialization can produce a new instance.
- `MetricsLoader` incorrectly uses `new MetricsRegistry()`.

Tasks
1) Make `MetricsRegistry` a proper, **thread-safe singleton**
   - **Lazy initialization**
   - **Private constructor**
   - Thread safety: pick one approach (recommended: static holder or double-checked locking)

2) Block reflection-based multiple construction
   - If the constructor is called when an instance already exists, throw an exception
   - (Hint: use a static flag/instance check inside the constructor)

3) Preserve singleton on serialization
   - Implement `readResolve()` so deserialization returns the same singleton instance

4) Update `MetricsLoader` to use the singleton
   - No `new MetricsRegistry()` anywhere in code

Acceptance
- Single instance across threads within a JVM run.
- Reflection cannot construct a second instance.
- Deserialization returns the same instance.
- Loading metrics from `metrics.properties` works.
- Values are accessible via:
  - `increment(key)`
  - `getCount(key)`
  - `getAll()`

Build/Run (Starter)
  cd singleton-metrics/src
  javac com/example/metrics/*.java
  java com.example.metrics.App

Useful Demo Commands (after you fix it)
- Concurrency check:
  java com.example.metrics.ConcurrencyCheck
- Reflection attack check:
  java com.example.metrics.ReflectionAttack
- Serialization check:
  java com.example.metrics.SerializationCheck

Note
This starter is intentionally broken. Some of these checks will "succeed" in breaking the singleton
until you fix the implementation.




---

# Preparation Notes (Diagram Style)

## 11. Current Design (Broken Starter)

```
┌──────────────────────────────────────────────────────────────┐
│               MetricsRegistry (BROKEN)                       │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    private static MetricsRegistry instance;  ← no Holder    │
│    private Map<String, Long> counters = new HashMap<>();     │
│                                                              │
│  Constructor:                                                │
│    ⚠ public MetricsRegistry()  ← NOT PRIVATE                │
│      anyone can call new MetricsRegistry() directly          │
│                                                              │
│  getInstance():                                              │
│    ⚠ public static MetricsRegistry getInstance() {          │
│          if (instance == null) {  ← NO SYNCHRONIZATION       │
│              instance = new MetricsRegistry();               │
│          }                                                   │
│          return instance;         ← RACE CONDITION           │
│      }                                                       │
│                                                              │
│  Public methods:                                             │
│    increment(key) / getCount(key) / getAll()                 │
│    ← NOT synchronized — data races under concurrency         │
│                                                              │
│  ⚠ Does NOT implement Serializable (or implements it         │
│    but has NO readResolve())                                 │
│    → ObjectInputStream.readObject() creates a NEW object     │
│                                                              │
│  ⚠ No reflection guard in constructor                        │
│    Constructor.setAccessible(true).newInstance() → new obj   │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│               MetricsLoader (BROKEN)                         │
│──────────────────────────────────────────────────────────────│
│  public MetricsRegistry loadFromFile(String path) {          │
│      ...                                                     │
│      MetricsRegistry registry = new MetricsRegistry();  ⚠   │
│                              ← creates a SEPARATE object     │
│                                NOT the singleton             │
│      // counters loaded here are invisible to getInstance()  │
│  }                                                           │
└──────────────────────────────────────────────────────────────┘

FOUR WAYS the singleton guarantee is broken in the starter:

  1. Public constructor
       new MetricsRegistry()  → always works, no guard

  2. Unsafe lazy check (race condition)
       Thread A: if (instance == null) → true
       Thread B: if (instance == null) → true   ← same time
       Thread A: instance = new MetricsRegistry()
       Thread B: instance = new MetricsRegistry()  ← second one!
       Both threads now hold DIFFERENT objects.
       Counters split across them.

  3. Reflection attack
       Constructor<MetricsRegistry> c =
           MetricsRegistry.class.getDeclaredConstructor();
       c.setAccessible(true);
       MetricsRegistry r2 = c.newInstance();  ← bypasses private

  4. Serialization attack
       serialize(MetricsRegistry.getInstance()) → bytes on disk
       MetricsRegistry r2 = deserialize(bytes)  → NEW OBJECT
       getInstance() != r2  ← two different instances in JVM

  5. MetricsLoader breaks it further
       loadFromFile() uses new MetricsRegistry()
       → counters loaded into a DIFFERENT object, never seen
         by getInstance() callers — App reads all zeros
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: Public constructor          [SINGLETON VIOLATION]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MetricsRegistry class definition                     │
│                                                              │
│  public MetricsRegistry() { ... }                            │
│  ^^^^^^                                                      │
│                                                              │
│  IMPACT:                                                     │
│    new MetricsRegistry()  → works every time                 │
│    100 callers → 100 different registry objects              │
│    No shared counter state — every object starts at zero     │
│                                                              │
│  FIX: Make constructor private.                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: Unsynchronized getInstance()  [THREAD-SAFETY FAIL] │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MetricsRegistry.getInstance()                        │
│                                                              │
│  if (instance == null) {                                     │
│      instance = new MetricsRegistry();  ← NOT ATOMIC         │
│  }                                                           │
│                                                              │
│  TIMELINE under 2 threads:                                   │
│    T1 reads instance → null                                  │
│    T2 reads instance → null    ← both pass the if            │
│    T1 creates MetricsRegistry@001                            │
│    T2 creates MetricsRegistry@002  ← SECOND INSTANCE         │
│    T1 returns @001, T2 returns @002                          │
│    Two singletons exist. Counters split across them.         │
│                                                              │
│  FIX: Use the Static Holder class (JVM class loading          │
│       is guaranteed atomic by the JLS).                      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: No reflection guard         [REFLECTION ATTACK]    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MetricsRegistry constructor                          │
│                                                              │
│  Constructor<MetricsRegistry> c =                            │
│      MetricsRegistry.class.getDeclaredConstructor();         │
│  c.setAccessible(true);    ← bypasses private visibility     │
│  MetricsRegistry r2 = c.newInstance();  ← second instance   │
│                                                              │
│  Even a private constructor is defeated by reflection.       │
│  FIX: Track construction with a static boolean flag;         │
│       throw RuntimeException if constructor is called twice. │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: No readResolve()          [SERIALIZATION ATTACK]   │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MetricsRegistry — missing readResolve() method       │
│                                                              │
│  serialize(MetricsRegistry.getInstance()) → bytes on disk    │
│  MetricsRegistry r2 = deserialize(bytes)                     │
│  r2 == MetricsRegistry.getInstance()  →  false  ⚠           │
│                                                              │
│  ObjectInputStream.readObject() reconstructs the object      │
│  directly from bytes, bypassing the constructor entirely.    │
│  Without readResolve(), there is no hook to return the       │
│  existing singleton — a brand new object appears.            │
│                                                              │
│  FIX: Implement readResolve() returning getInstance().       │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 5: MetricsLoader bypasses singleton  [BYPASS]         │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MetricsLoader.loadFromFile()                         │
│                                                              │
│  MetricsRegistry registry = new MetricsRegistry();  ⚠        │
│                           ← SEPARATE object from getInstance │
│                                                              │
│  App.java calls MetricsRegistry.getInstance() → gets @001    │
│  MetricsLoader creates @002 and loads file counters into it  │
│  App.java reads @001 — all counters are zero                 │
│  @002 holds real data but is immediately discarded           │
│                                                              │
│  FIX: MetricsLoader.loadFromFile() must call getInstance()   │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│               MetricsRegistry (FIXED)                        │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    private static boolean instanceCreated = false;           │
│    private final Map<String, Long> counters = new HashMap<>();│
│    private static final long serialVersionUID = 1L;          │
│                                                              │
│  Private constructor + reflection guard:                     │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  private MetricsRegistry() {                           │  │
│  │      if (instanceCreated) {                            │  │
│  │          throw new RuntimeException(                   │  │
│  │              "Cannot create another instance");        │  │
│  │      }                                                 │  │
│  │      instanceCreated = true;   ← marks first creation  │  │
│  │  }                                                     │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  Static Holder — thread-safe lazy initialization:            │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  private static class Holder {                         │  │
│  │      static final MetricsRegistry INSTANCE =           │  │
│  │          new MetricsRegistry();                        │  │
│  │  }                                                     │  │
│  └────────────────────────────────────────────────────────┘  │
│  JVM loads Holder class exactly once, the first time         │
│  getInstance() is called. Class loading is guaranteed        │
│  atomic by the Java Language Specification.                  │
│                                                              │
│  public static MetricsRegistry getInstance() {               │
│      return Holder.INSTANCE;  ← no synchronized needed       │
│  }                                                           │
│                                                              │
│  Serialization guard:                                        │
│    implements Serializable                                   │
│    private Object readResolve() {                            │
│        return getInstance();  ← deserialization returns same │
│    }                             singleton, not a new obj    │
│                                                              │
│  Thread-safe public methods:                                 │
│    public synchronized void increment(String key) { ... }    │
│    public synchronized long getCount(String key)  { ... }    │
│    public synchronized Map<String,Long> getAll()  { ... }    │
│    public synchronized void setCount(String key, long v) {}  │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│               MetricsLoader (FIXED)                          │
│──────────────────────────────────────────────────────────────│
│  public MetricsRegistry loadFromFile(String path) {          │
│                                                              │
│      MetricsRegistry registry =                              │
│          MetricsRegistry.getInstance();  ← THE SINGLETON     │
│                                                              │
│      // loads props into the ONE shared global registry      │
│      return registry;                                        │
│  }                                                           │
└──────────────────────────────────────────────────────────────┘

HOW EACH ATTACK IS NOW BLOCKED:

  Constructor calls    → private + instanceCreated guard throws
  Concurrency          → Holder loaded once by JVM (atomic)
  Reflection attack    → instanceCreated=true → second call throws
  Serialization attack → readResolve() returns Holder.INSTANCE
  MetricsLoader bypass → now uses getInstance(), not new


WHY STATIC HOLDER IS BETTER THAN DOUBLE-CHECKED LOCKING:

  DOUBLE-CHECKED LOCKING:                STATIC HOLDER (this code):
  ───────────────────────                ──────────────────────────
  private volatile static                private static class Holder {
      MetricsRegistry instance;              static final MetricsRegistry
                                                 INSTANCE = new MetricsRegistry();
  public static MetricsRegistry          }
      getInstance() {
      if (instance == null) {            public static MetricsRegistry
          synchronized (               getInstance() {
            MetricsRegistry.class) {       return Holder.INSTANCE;
              if (instance == null) {  }
                  instance = new
                    MetricsRegistry();
              }
          }
      }
      return instance;
  }

  volatile + 2x null check + lock    →   No volatile. No lock. No null check.
  volatile required for JMM fix      →   JVM class-loading guarantee handles it.
  Lock cost on every write            →   Zero synchronization overhead on reads.
  Easy to write wrong (miss volatile) →   Three lines. Impossible to get wrong.
  Lazy init but complex               →   Lazy init and simple.

  VERDICT: Static Holder is simpler, faster, and just as correct.
           No risk of forgetting volatile. No lock contention.
           Preferred idiom for singletons in modern Java.


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                                  AFTER
  ──────                                  ─────
  public constructor             →        private constructor
  if (instance==null) naive lazy →        Static Holder class (JVM-safe)
  no reflection guard            →        instanceCreated flag + RuntimeException
  no Serializable / readResolve  →        implements Serializable + readResolve()
  MetricsLoader: new Registry()  →        MetricsLoader: getInstance()
  unsynchronized public methods  →        synchronized on all public methods
```
