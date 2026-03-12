Flyweight — Deduplicate Map Marker Styles (Refactoring)
------------------------------------------------------
Narrative (Current Code)
A CLI tool called **GeoDash** renders a large list of map markers (pins).
Right now, every `MapMarker` stores its own style fields (shape, color, size, filled).
When rendering thousands of markers, we end up creating thousands of duplicate style objects → memory blow-up.

Your Task
1) Extract an immutable `MarkerStyle` (shape, color, size, filled) as **intrinsic state**.
2) Implement `MarkerStyleFactory` that caches and returns shared `MarkerStyle` instances by key.
3) Modify `MapMarker` to hold:
   - `MarkerStyle` (intrinsic)
   - marker-specific fields (extrinsic): `lat`, `lng`, `label`
4) Update `MapDataSource` (marker creation pipeline) to obtain styles via the factory
   (no `new MarkerStyle(...)` during marker creation).

Acceptance Criteria
- Same rendering “cost” as before (same number of markers rendered, same output format).
- Identical style configurations reuse the same `MarkerStyle` instance
  (see `QuickCheck` — it should report a small number of unique styles).
- `MarkerStyle` is immutable (all fields final, no setters).
- `MapMarker` stores only extrinsic state plus a reference to shared `MarkerStyle`.

Hints
- Use a `Map<String, MarkerStyle>` cache in the factory.
- Key suggestion: `"PIN|RED|12|F"` (shape|color|size|filledFlag)

Build & Run
  cd flyweight-markers/src
  javac com/example/map/*.java
  java com.example.map.App

Repo intent
This is a **refactoring assignment**: the starter code is intentionally wasteful.
Students should refactor to Flyweight without changing the external behavior.

---

# Preparation Notes (Diagram Style)

## 11. Current Design (Broken Starter)

```
┌──────────────────────────────────────────────────────────────┐
│               MapMarker (BROKEN — WASTEFUL)                   │
│──────────────────────────────────────────────────────────────│
│  Fields (ALL stored per marker — duplicated thousands         │
│          of times for identical style combos):                │
│                                                              │
│    double lat                     ← extrinsic (unique)       │
│    double lng                     ← extrinsic (unique)       │
│    String label                   ← extrinsic (unique)       │
│    String shape                   ← intrinsic (duplicated!)  │
│    String color                   ← intrinsic (duplicated!)  │
│    int    size                    ← intrinsic (duplicated!)  │
│    boolean filled                 ← intrinsic (duplicated!)  │
│                                                              │
│  No separation of intrinsic vs extrinsic state.              │
│  Every marker is a self-contained blob of ALL fields.        │
│──────────────────────────────────────────────────────────────│
│  Constructor:                                                │
│    MapMarker(lat, lng, label, shape, color, size, filled)    │
│    ← each call allocates a new object with style fields      │
│──────────────────────────────────────────────────────────────│
│  getStyle():                                                 │
│    return shape + "|" + color + "|" + size + "|" + filled    │
│    ← builds a string every call, no shared object            │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│       MapDataSource (BROKEN — NO SHARING)                     │
│──────────────────────────────────────────────────────────────│
│  loadMarkers(count):                                         │
│    for each marker:                                          │
│      pick random shape, color, size, filled                  │
│      new MapMarker(lat, lng, label, shape, color, size,      │
│                    filled)                                    │
│      ← brand new object every time, even if style is same    │
│                                                              │
│  No factory. No cache. No deduplication.                     │
│                                                              │
│  30,000 markers → 30,000 sets of style fields in memory      │
│  Even though only 3×4×4×2 = 96 unique style combos exist.   │
└──────────────────────────────────────────────────────────────┘

MEMORY WASTE — what the starter does:

  Markers created:  30,000
  Unique styles:        96  (3 shapes × 4 colors × 4 sizes × 2 filled)
  Style objects:    30,000  ← one per marker, no sharing

  Each style stores: shape (String), color (String), size (int), filled (bool)
  Duplicate ratio: 30,000 / 96 ≈ 312× redundancy

  QuickCheck reports: ~30,000 unique style identities  ← BAD
  Should report:          ≤ 96                         ← GOOD
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: No separation of intrinsic/extrinsic  [FLYWEIGHT]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MapMarker class                                      │
│                                                              │
│  Every MapMarker holds lat, lng, label, shape, color,        │
│  size, filled — all in one flat class.                       │
│                                                              │
│  INTRINSIC (shared, repeats): shape, color, size, filled     │
│  EXTRINSIC (unique per marker): lat, lng, label              │
│                                                              │
│  IMPACT:                                                     │
│    With 30,000 markers but only 96 style combos,             │
│    29,904 style allocations are pure waste.                   │
│    Each duplicate style holds its own String references.     │
│    GC pressure and memory footprint scale linearly           │
│    with marker count instead of style count.                 │
│                                                              │
│  FIX: Extract intrinsic state into a separate MarkerStyle    │
│       class. MapMarker holds a reference to shared style.    │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: No caching / factory           [MEMORY BLOW-UP]    │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MapDataSource.loadMarkers()                          │
│                                                              │
│  Every marker creation path does:                            │
│    new MapMarker(lat, lng, label, shape, color, size, filled)│
│    ← always allocates new memory for style fields            │
│                                                              │
│  No factory exists to check "have I seen this style before?" │
│  No cache to return an existing instance.                    │
│                                                              │
│  IMPACT:                                                     │
│    10 markers with shape=PIN, color=RED, size=12, filled=T   │
│    → 10 separate style copies in memory                      │
│    Should be: 1 shared style, 10 references to it            │
│                                                              │
│  FIX: Create MarkerStyleFactory with a Map<String,           │
│       MarkerStyle> cache. Key = "PIN|RED|12|F".              │
│       computeIfAbsent returns existing or creates once.      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: Style not a first-class object  [NO REUSE]         │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: MapMarker — style is spread across 4 raw fields      │
│                                                              │
│  There is no MarkerStyle type. Style is just 4 loose fields  │
│  on MapMarker. This means:                                   │
│    - Cannot share style across markers (no object to share)  │
│    - Cannot compare styles (must compare 4 fields each time) │
│    - Cannot cache styles (nothing cacheable to store)        │
│    - getStyle() rebuilds a string on every call              │
│                                                              │
│  FIX: Create immutable MarkerStyle class (final fields,      │
│       no setters). This becomes the flyweight object.        │
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│         MarkerStyle (FLYWEIGHT — INTRINSIC STATE)            │
│──────────────────────────────────────────────────────────────│
│  Fields (ALL private final — immutable):                     │
│    private final String  shape;                              │
│    private final String  color;                              │
│    private final int     size;                               │
│    private final boolean filled;                             │
│──────────────────────────────────────────────────────────────│
│  Constructor:                                                │
│    MarkerStyle(shape, color, size, filled)                   │
│    ← only called by factory when cache misses                │
│──────────────────────────────────────────────────────────────│
│  toString():                                                 │
│    → "PIN|RED|12|F" (matches cache key format)               │
│                                                              │
│  IMMUTABLE: safe to share across any number of markers.      │
│  No risk of one marker's mutation affecting another.         │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│         MarkerStyleFactory (CACHE + DEDUPLICATION)            │
│──────────────────────────────────────────────────────────────│
│  Field:                                                      │
│    Map<String, MarkerStyle> cache = new HashMap<>()          │
│──────────────────────────────────────────────────────────────│
│  get(shape, color, size, filled):                            │
│    key = "PIN|RED|12|F"                                      │
│    cache.computeIfAbsent(key, k ->                           │
│        new MarkerStyle(shape, color, size, filled))          │
│    ← first call: creates and stores                          │
│    ← subsequent calls: returns cached instance               │
│──────────────────────────────────────────────────────────────│
│  cacheSize():                                                │
│    → number of unique styles created (should be ≤ 96)        │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│       MapMarker (FIXED — EXTRINSIC + SHARED REFERENCE)       │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    double lat            ← extrinsic (unique per marker)     │
│    double lng            ← extrinsic (unique per marker)     │
│    String label          ← extrinsic (unique per marker)     │
│    MarkerStyle style     ← SHARED reference to flyweight     │
│                                                              │
│  getStyle() → returns the shared MarkerStyle object          │
│  No shape/color/size/filled stored directly.                 │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│         MapDataSource (FIXED — USES FACTORY)                 │
│──────────────────────────────────────────────────────────────│
│  Field:                                                      │
│    MarkerStyleFactory factory = new MarkerStyleFactory()     │
│──────────────────────────────────────────────────────────────│
│  loadMarkers(count):                                         │
│    for each marker:                                          │
│      pick random shape, color, size, filled                  │
│      MarkerStyle style = factory.get(shape, color, size,     │
│                                      filled)                 │
│      ← factory returns CACHED instance if style seen before  │
│      new MapMarker(lat, lng, label, style)                   │
│      ← marker holds a REFERENCE, not its own copy            │
└──────────────────────────────────────────────────────────────┘

HOW FLYWEIGHT SHARING WORKS:

  factory.get("PIN","RED",12,true)  → creates MarkerStyle@001, caches it
  factory.get("PIN","RED",12,true)  → returns MarkerStyle@001 from cache
  factory.get("PIN","RED",12,true)  → returns MarkerStyle@001 from cache
  factory.get("CIRCLE","BLUE",14,false) → creates MarkerStyle@002

  3 markers with same style → 1 MarkerStyle object, 3 references
  No duplication. Bounded by unique combos, not marker count.


MEMORY COMPARISON — BEFORE vs AFTER:

  BEFORE (no Flyweight):
    30,000 markers × (lat + lng + label + shape + color + size + filled)
    = 30,000 full objects

  AFTER (with Flyweight):
    96 MarkerStyle objects (cached by factory)
    30,000 MapMarker objects × (lat + lng + label + 1 reference)
    Style memory: O(unique combos) instead of O(markers)

  QuickCheck reports: ≤ 96 unique style identities  ← CORRECT


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                                  AFTER
  ──────                                  ─────
  style fields in MapMarker      →        separate MarkerStyle class
  no style object to share       →        immutable flyweight object
  no factory / no cache          →        MarkerStyleFactory with HashMap
  new style per marker           →        computeIfAbsent returns cached
  30,000 style copies            →        ≤ 96 shared instances
  getStyle() builds string       →        getStyle() returns object ref
  memory grows with markers      →        memory grows with unique styles
```
