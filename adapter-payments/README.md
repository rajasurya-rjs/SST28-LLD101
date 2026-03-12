# Adapter — Payments (Refactoring)

## Narrative (Current Code)
OrderService directly depends on two mismatched SDKs (`FastPayClient`, `SafeCashClient`), uses a string `provider` switch, and duplicates glue logic.

## Your Task
Introduce an **Adapter** so `OrderService` depends only on a `PaymentGateway` interface. Create:
- `PaymentGateway` (target interface): `String charge(String customerId, int amountCents)`
- `FastPayAdapter` and `SafeCashAdapter` mapping to their respective SDKs
- A simple map-based registry in `App` to select the gateway

Refactor `OrderService` to accept a `PaymentGateway` and remove provider branching.

## Acceptance Criteria
- `OrderService` calls **only** `PaymentGateway`
- Adding a new provider requires no change to `OrderService`
- Running `App` prints transaction IDs for both providers

## Hints
- Use constructor injection or a `Map<String, PaymentGateway>`
- Keep adapters stateless
- Use `Objects.requireNonNull` to validate inputs

## Build & Run
```bash
cd adapter-payments/src
javac com/example/payments/*.java
java com.example.payments.App
```

---

# Preparation Notes (Diagram Style)

## 11. Current Design (Broken Starter)

```
┌──────────────────────────────────────────────────────────────┐
│          FastPayClient (THIRD-PARTY SDK)                      │
│──────────────────────────────────────────────────────────────│
│  payNow(String custId, int amountCents) → String             │
│  ← unique method name, unique parameter order                │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│          SafeCashClient (THIRD-PARTY SDK)                     │
│──────────────────────────────────────────────────────────────│
│  createPayment(int amount, String user) → SafeCashPayment    │
│  ← different method name, REVERSED param order               │
│  ← returns an intermediate object, not a String              │
│                                                              │
│  SafeCashPayment:                                            │
│    confirm() → String   ← two-step process to get result     │
└──────────────────────────────────────────────────────────────┘

  Two SDKs. Two completely different interfaces.
  Neither matches the other.

┌──────────────────────────────────────────────────────────────┐
│      OrderService (BROKEN — DIRECT SDK COUPLING)              │
│──────────────────────────────────────────────────────────────│
│  Fields:                                                     │
│    FastPayClient fastPay;                                    │
│    SafeCashClient safeCash;                                   │
│    ← CONCRETE dependencies on both SDKs                      │
│──────────────────────────────────────────────────────────────│
│  charge(String provider, String customerId, int amount):     │
│                                                              │
│    if ("fastpay".equals(provider)) {                         │
│        return fastPay.payNow(customerId, amount);            │
│    } else if ("safecash".equals(provider)) {                 │
│        SafeCashPayment p =                                   │
│            safeCash.createPayment(amount, customerId);       │
│        return p.confirm();                                   │
│    } else {                                                  │
│        throw new IllegalArgumentException("unknown");        │
│    }                                                         │
│                                                              │
│  ⚠ String-based provider switching (if/else chain)           │
│  ⚠ Glue logic for each SDK duplicated inside OrderService    │
│  ⚠ Adding a 3rd provider → modify OrderService again         │
└──────────────────────────────────────────────────────────────┘

COUPLING CHAIN — how OrderService is trapped:

  OrderService
      ├── knows FastPayClient.payNow(custId, amount)
      ├── knows SafeCashClient.createPayment(amount, user)
      ├── knows SafeCashPayment.confirm()
      └── switches on provider string

  Every new provider → add import, add field, add else-if,
  learn that SDK's quirky API, write glue code here.
  OrderService grows without bound.
```

## 12. Issues

```
┌──────────────────────────────────────────────────────────────┐
│  ISSUE 1: Direct coupling to concrete SDKs    [COUPLING]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: OrderService fields + charge() method                │
│                                                              │
│  OrderService directly imports and holds:                    │
│    FastPayClient fastPay;                                    │
│    SafeCashClient safeCash;                                   │
│                                                              │
│  WHY IT'S BAD:                                               │
│    OrderService is tightly bound to two external SDKs.       │
│    If FastPayClient changes its method signature,            │
│    OrderService must change too.                             │
│    Cannot swap providers at runtime.                         │
│    Cannot unit-test OrderService without real SDK clients.   │
│                                                              │
│  FIX: Depend on a PaymentGateway interface, not concrete     │
│       SDK classes. Inject adapters via constructor.           │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 2: String-based provider switching    [BRANCHING]     │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: OrderService.charge()                                │
│                                                              │
│  if ("fastpay".equals(provider)) { ... }                     │
│  else if ("safecash".equals(provider)) { ... }               │
│  else throw ...                                              │
│                                                              │
│  WHY IT'S BAD:                                               │
│    Adding provider #3 → add another else-if                  │
│    Adding provider #10 → 10 branches in charge()             │
│    Violates Open/Closed Principle:                           │
│      OrderService must be MODIFIED to EXTEND behavior        │
│    Typos in string keys cause silent runtime failures        │
│                                                              │
│  FIX: Replace if/else with Map<String, PaymentGateway>       │
│       lookup. OrderService does gateways.get(provider).      │
│       Adding a provider = adding a map entry, not code.      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 3: Glue logic inside OrderService  [WRONG PLACE]      │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  WHERE: OrderService.charge() — inside each branch           │
│                                                              │
│  fastpay branch:                                             │
│    return fastPay.payNow(customerId, amount);                │
│    ← knows FastPay's method name + param order               │
│                                                              │
│  safecash branch:                                            │
│    SafeCashPayment p =                                       │
│        safeCash.createPayment(amount, customerId);           │
│    return p.confirm();                                       │
│    ← knows SafeCash's two-step flow + reversed params        │
│                                                              │
│  WHY IT'S BAD:                                               │
│    SDK-specific translation logic is buried in a service     │
│    that should only care about "charge a customer".          │
│    Each provider's quirks leak into business logic.          │
│                                                              │
│  FIX: Move glue logic into Adapter classes.                  │
│       FastPayAdapter.charge() calls payNow() internally.     │
│       SafeCashAdapter.charge() calls createPayment +         │
│       confirm() internally. OrderService never sees it.      │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  ISSUE 4: Mismatched SDK interfaces       [INCOMPATIBILITY]  │
│──────────────────────────────────────────────────────────────│
│                                                              │
│  FastPayClient:                                              │
│    payNow(String custId, int amountCents) → String           │
│                                                              │
│  SafeCashClient:                                             │
│    createPayment(int amount, String user) → SafeCashPayment  │
│    SafeCashPayment.confirm() → String                        │
│                                                              │
│  DIFFERENCES:                                                │
│    Method name: payNow vs createPayment                      │
│    Param order: (custId, amount) vs (amount, user)           │
│    Return type: String vs intermediate object → .confirm()   │
│    Steps: 1-step vs 2-step                                   │
│                                                              │
│  Without adapters, every consumer of these SDKs must         │
│  independently handle all these differences.                 │
│                                                              │
│  FIX: Each Adapter normalizes its SDK to the common          │
│       PaymentGateway.charge(customerId, amountCents) → String│
└──────────────────────────────────────────────────────────────┘
```

## 13. The Fix

```
┌──────────────────────────────────────────────────────────────┐
│       PaymentGateway (TARGET INTERFACE)                       │
│──────────────────────────────────────────────────────────────│
│  String charge(String customerId, int amountCents)           │
│  ← one uniform contract for ALL payment providers            │
│  ← OrderService depends ONLY on this                         │
└──────────────────────────────────────────────────────────────┘
          ▲                               ▲
          │ implements                     │ implements
          │                               │
┌─────────────────────────┐   ┌─────────────────────────────┐
│   FastPayAdapter        │   │    SafeCashAdapter           │
│─────────────────────────│   │─────────────────────────────│
│ FastPayClient client    │   │ SafeCashClient client        │
│─────────────────────────│   │─────────────────────────────│
│ charge(custId, amount): │   │ charge(custId, amount):      │
│   return client.payNow( │   │   SafeCashPayment p =        │
│     custId, amount);    │   │     client.createPayment(    │
│                         │   │       amount, custId);       │
│ ← translates to         │   │   return p.confirm();        │
│   FastPay's interface    │   │                              │
│                         │   │ ← translates to SafeCash's   │
│                         │   │   two-step interface          │
└─────────────────────────┘   └─────────────────────────────┘
          │                               │
          ▼                               ▼
┌─────────────────────────┐   ┌─────────────────────────────┐
│  FastPayClient (SDK)    │   │  SafeCashClient (SDK)        │
│  payNow(custId, amt)    │   │  createPayment(amt, user)    │
│  → "FP#cust-1:1299"    │   │  → SafeCashPayment           │
│                         │   │    .confirm()                │
│                         │   │  → "SC#pay(cust-2,1299)"     │
└─────────────────────────┘   └─────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│     OrderService (FIXED — DEPENDS ONLY ON INTERFACE)         │
│──────────────────────────────────────────────────────────────│
│  Field:                                                      │
│    Map<String, PaymentGateway> gateways                      │
│    ← injected via constructor                                │
│──────────────────────────────────────────────────────────────│
│  charge(provider, customerId, amountCents):                  │
│    PaymentGateway gw = gateways.get(provider);               │
│    if (gw == null) throw IllegalArgumentException            │
│    return gw.charge(customerId, amountCents);                │
│                                                              │
│  ← NO if/else chain                                          │
│  ← NO SDK imports                                            │
│  ← NO glue logic                                             │
│  ← Adding provider #3: just put a new adapter in the map     │
└──────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│     App (WIRING — MAP-BASED REGISTRY)                        │
│──────────────────────────────────────────────────────────────│
│  Map<String, PaymentGateway> gateways = new HashMap<>();     │
│  gateways.put("fastpay",                                     │
│      new FastPayAdapter(new FastPayClient()));               │
│  gateways.put("safecash",                                    │
│      new SafeCashAdapter(new SafeCashClient()));             │
│                                                              │
│  OrderService svc = new OrderService(gateways);              │
│  svc.charge("fastpay",  "cust-1", 1299);  → "FP#cust-1:…"  │
│  svc.charge("safecash", "cust-2", 1299);  → "SC#pay(…)"     │
│                                                              │
│  Adding a 3rd provider (e.g., StripeAdapter):                │
│    gateways.put("stripe", new StripeAdapter(new Stripe()));  │
│    ← OrderService is UNTOUCHED                               │
└──────────────────────────────────────────────────────────────┘

HOW THE ADAPTER TRANSLATES:

  Caller says:   gw.charge("cust-1", 1299)
                      │
       ┌──────────────┼──────────────────┐
       ▼              ▼                  ▼
  FastPayAdapter  SafeCashAdapter   (future adapter)
       │              │
       ▼              ▼
  client.payNow(  client.createPayment(
    "cust-1",       1299,          ← amount FIRST
    1299)           "cust-1")      ← customer SECOND
                      │
                      ▼
                  payment.confirm()

  Each adapter hides the SDK's quirks behind a uniform interface.


WHAT CHANGED — BEFORE vs AFTER:

  BEFORE                                  AFTER
  ──────                                  ─────
  OrderService holds SDK clients →        OrderService holds Map<PaymentGateway>
  if/else on provider string     →        map.get(provider).charge()
  glue logic in OrderService     →        glue logic in Adapter classes
  adding provider = modify svc   →        adding provider = add map entry
  coupled to FastPay + SafeCash  →        coupled only to PaymentGateway
  cannot unit-test without SDKs  →        can mock PaymentGateway easily
```
