# Distributed Cache System

A distributed cache system that supports `get(key)` and `put(key, value)` with configurable nodes, pluggable distribution strategy, and pluggable eviction policy.

## Class Diagram

```
┌─────────────────────────────┐     ┌──────────────────────────────┐
│  EvictionPolicy<K,V>        │     │  DistributionStrategy        │
│  <<interface>>               │     │  <<interface>>               │
│─────────────────────────────│     │──────────────────────────────│
│  onAccess(K key)            │     │  getNode(String key,         │
│  onInsert(K key, V value)   │     │          int totalNodes): int│
│  evict(): K                 │     └──────────────────────────────┘
│  onRemove(K key)            │                ▲
│  isFull(): boolean          │                │ implements
└─────────────────────────────┘     ┌──────────────────────────────┐
          ▲                         │  ModuloDistribution          │
          │ implements              │──────────────────────────────│
┌─────────────────────────────┐     │  hash(key) % totalNodes      │
│  LRUEvictionPolicy<K,V>    │     └──────────────────────────────┘
│─────────────────────────────│
│  - capacity: int            │     ┌──────────────────────────────┐
│  - accessOrderMap: LHM      │     │  Database<K,V>               │
│─────────────────────────────│     │  <<interface>>               │
│  Uses LinkedHashMap with     │     │──────────────────────────────│
│  accessOrder=true for LRU   │     │  fetch(K key): V             │
│  tracking. Evicts eldest.   │     │  store(K key, V value)       │
└─────────────────────────────┘     └──────────────────────────────┘
                                               ▲
                                               │ implements
                                    ┌──────────────────────────────┐
                                    │  MockDatabase<K,V>           │
                                    │  (HashMap-backed)            │
                                    └──────────────────────────────┘

┌────────────────────────────────┐  ┌─────────────────────────────────┐
│  EvictionPolicyFactory         │  │  DistributionStrategyFactory     │
│────────────────────────────────│  │─────────────────────────────────│
│  create(type, capacity)        │  │  create(type)                    │
│  "lru" → LRUEvictionPolicy     │  │  "modulo" → ModuloDistribution   │
└────────────────────────────────┘  └─────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  CacheNode<K,V>                                                 │
│─────────────────────────────────────────────────────────────────│
│  - nodeId: String                                               │
│  - store: HashMap<K,V>                                          │
│  - evictionPolicy: EvictionPolicy<K,V>                          │
│─────────────────────────────────────────────────────────────────│
│  + get(key): V                                                  │
│  + put(key, value): void                                        │
│  + containsKey(key): boolean                                    │
│  + size(): int                                                  │
│  On put: if full & new key → evict LRU, then insert             │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  DistributedCacheManager<K,V>  (Main Entry Point / Facade)      │
│─────────────────────────────────────────────────────────────────│
│  - nodes: List<CacheNode<K,V>>                                  │
│  - distributionStrategy: DistributionStrategy                   │
│  - database: Database<K,V>                                      │
│─────────────────────────────────────────────────────────────────│
│  + get(key): V     → route to node → HIT or MISS (fetch DB)    │
│  + put(key, value) → route to node → store + write-through DB   │
│  + printStats()                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Design Patterns

| Pattern   | Usage                                                        |
|-----------|--------------------------------------------------------------|
| Strategy  | `DistributionStrategy` and `EvictionPolicy` interfaces       |
| Factory   | `EvictionPolicyFactory` and `DistributionStrategyFactory`    |
| Facade    | `DistributedCacheManager` as the single entry point          |

## How Data Is Distributed Across Nodes

The `DistributionStrategy` interface determines which cache node stores a given key. The current implementation (`ModuloDistribution`) uses:

```
nodeIndex = Math.abs(key.hashCode()) % totalNodes
```

This ensures each key consistently maps to the same node. To add a new strategy (e.g., consistent hashing), implement the `DistributionStrategy` interface and register it in `DistributionStrategyFactory`.

## How Cache Miss Is Handled

```
Client calls: manager.get("user:42")
       │
       ▼
  distributionStrategy.getNode("user:42", 3)  →  Node-1
       │
       ├── node.containsKey("user:42")?
       │      YES → return cached value              [CACHE HIT]
       │      NO  → database.fetch("user:42")        [CACHE MISS]
       │             → node.put("user:42", value)     [CACHE FILL]
       │             → return value
```

## How Eviction Works

Each `CacheNode` has a fixed capacity. When a node is full and a **new** key is inserted:

1. `evictionPolicy.evict()` identifies the least recently used key
2. That key is removed from the node's store
3. The new key-value pair is inserted

The `LRUEvictionPolicy` uses a `LinkedHashMap` with `accessOrder=true` which automatically maintains access order. The eldest entry (least recently accessed) is always the first in iteration order.

## Extensibility

**Adding a new eviction policy** (e.g., LFU):
1. Create `LFUEvictionPolicy<K,V>` implementing `EvictionPolicy<K,V>`
2. Add `"lfu"` case in `EvictionPolicyFactory.create()`

**Adding a new distribution strategy** (e.g., consistent hashing):
1. Create `ConsistentHashDistribution` implementing `DistributionStrategy`
2. Add `"consistent-hashing"` case in `DistributionStrategyFactory.create()`

No existing classes need modification (Open/Closed Principle).

## Build & Run

```bash
cd distributed-cache/src
javac com/example/cache/*.java
java com.example.cache.App
```
