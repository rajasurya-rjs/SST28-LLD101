# Pen Design — Low-Level Design

## Problem
Design a Pen system where `Pen pen = PenFactory.getPen("ink-pen", "blue", "with-cap")` creates a pen with type-specific `write()`, `refill()`, `start()`, `close()` behaviors. Must also support adding grip over write (Decorator) and pencil without refill (ISP).

---

## Class Diagram

```
           <<interface>>                     <<interface>>
            Writable                         WriteStrategy
  ┌──────────────────────────┐        ┌──────────────────────────┐
  │ + write(String text)      │        │ + apply(text, color, ink)│
  │ + start()                 │        └──┬───────┬───────┬──────┘
  │ + close()                 │           │       │       │
  └──────────┬───────────────┘       InkWrite  BallWrite GelWrite
             │ extends               Strategy  Strategy  Strategy
             ▼
       <<interface>>                   <<interface>>
        Refillable                    MechanismStrategy
  ┌──────────────────────────┐     ┌───────────────────────┐
  │ + refill(String color)    │     │ + open()               │
  └──────────┬───────────────┘     │ + shut()               │
             │ implements           └──┬────────────┬───────┘
             ▼                         │            │
  ┌──────────────────────────┐     WithCap      ClickMechanism
  │          Pen              │     Mechanism
  │  - writeStrategy          │
  │  - mechanismStrategy      │
  │  - color, inkLevel        │
  │  - started: boolean       │
  └──────────────────────────┘

  GripDecorator ─implements─▶ Writable  (wraps any Writable — Decorator)
  Pencil ───────implements─▶ Writable  (no refill — ISP)
  PenFactory ──creates─────▶ Pen       (static factory)
```

---

## Relationships

```
PenFactory ──creates──▶ Pen
                         ├── has-a ──▶ WriteStrategy (Ink/Ball/Gel)
                         └── has-a ──▶ MechanismStrategy (WithCap/Click)

GripDecorator ──wraps──▶ Writable (any pen or pencil)
Pencil ──implements──▶ Writable only (no Refillable)
Pen ──implements──▶ Refillable (extends Writable)
```

---

## Design Patterns

**Strategy** — `WriteStrategy` (3 impls) + `MechanismStrategy` (2 impls). Pen composes both. Avoids 3x2=6 subclass explosion. Each pen type writes differently, each mechanism opens/closes differently.

**Factory** — `PenFactory.getPen(type, color, mechanism)` maps strings to strategy objects and assembles the Pen. Client never sees strategy classes.

**Decorator** — `GripDecorator` wraps any `Writable`. Adds grip behavior around `write()` with zero changes to existing classes. Can stack multiple decorators.

**ISP** — `Writable` (write/start/close) and `Refillable` (extends Writable, adds refill). Pen implements Refillable. Pencil implements only Writable. Clients depend on the narrowest interface they need.

---

## Follow-Up Answers

**Q1: How to add grip over write() with minimal changes?**
Add `GripDecorator implements Writable`. It wraps any `Writable`, intercepts `write()` to add grip before/after, delegates everything else. Zero changes to Pen or any strategy. One new class.

**Q2: What if client wants a pencil without refill()?**
`Pencil implements Writable` (not `Refillable`). The `Writable` interface has write/start/close but no refill. Client code that only needs writing depends on `Writable` — works with both Pen and Pencil.

---

## Build & Run

```bash
cd pen-design/src
javac com/example/pen/*.java
java com.example.pen.App
```
