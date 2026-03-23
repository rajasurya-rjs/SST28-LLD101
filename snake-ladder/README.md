# Snake & Ladder — Low-Level Design

## Problem
Design a Snake and Ladder game with `n×n` board, `x` players, and two versions (Easy/Difficult).
Game is created via a Factory. Client calls `makeTurn()` in a loop until the game ends.

---

## Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          GameVersion <<enum>>                            │
│──────────────────────────────────────────────────────────────────────────│
│  EASY, DIFFICULT                                                         │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                        Snake (immutable)                                  │
│──────────────────────────────────────────────────────────────────────────│
│  - head: int,  - tail: int                                               │
│  + getHead(), getTail()                                                  │
│  Validates: head > tail, different rows                                  │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                       Ladder (immutable)                                  │
│──────────────────────────────────────────────────────────────────────────│
│  - start: int,  - end: int                                               │
│  + getStart(), getEnd()                                                  │
│  Validates: end > start, different rows                                  │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                           Player                                         │
│──────────────────────────────────────────────────────────────────────────│
│  - name: String (final),  - position: int (starts at 0)                  │
│  + getName(), getPosition(), setPosition(int)                            │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                            Dice                                          │
│──────────────────────────────────────────────────────────────────────────│
│  - random: Random                                                        │
│  + roll(): int  → returns 1-6                                            │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                      TurnResult (immutable)                              │
│──────────────────────────────────────────────────────────────────────────│
│  - playerName: String,  - diceRolls: List<Integer>                       │
│  - startPosition: int,  - endPosition: int,  - message: String           │
│  + toString() → human-readable turn summary                              │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                           Board                                          │
│──────────────────────────────────────────────────────────────────────────│
│  - size: int,  - snakes: Map<Int,Snake>,  - ladders: Map<Int,Ladder>     │
│  + getMaxPosition(): int          → size * size                          │
│  + getFinalPosition(int pos): int → resolves snake/ladder at pos         │
│  + getSnakeAt(int), getLadderAt(int)                                     │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                     BoardGenerator (static utility)                      │
│──────────────────────────────────────────────────────────────────────────│
│  + generate(int n): Board                                                │
│    Places n snakes + n ladders randomly with constraints:                │
│    - Different rows, no overlapping endpoints, no cycles                 │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                   TurnStrategy <<interface>>                              │
│──────────────────────────────────────────────────────────────────────────│
│  + executeTurn(Player, Dice, Board): TurnResult                          │
└──────────────────────────────────────────────────────────────────────────┘
          ▲                                    ▲
          │ implements                         │ implements
┌─────────────────────────┐       ┌────────────────────────────────┐
│   EasyTurnStrategy      │       │   DifficultTurnStrategy        │
│─────────────────────────│       │────────────────────────────────│
│ Roll, move, if 6 roll   │       │ Roll, move, if 6 roll again   │
│ again. No penalty.      │       │ 3 consecutive sixes = UNDO    │
│ Unlimited sixes OK.     │       │ entire turn, back to start.   │
└─────────────────────────┘       └────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                      Game (orchestrator)                                  │
│──────────────────────────────────────────────────────────────────────────│
│  - board: Board,  - players: List<Player>,  - dice: Dice                 │
│  - turnStrategy: TurnStrategy,  - currentPlayerIndex: int                │
│  - winners: List<Player>                                                 │
│  + makeTurn(): TurnResult   → delegates to turnStrategy                  │
│  + isGameOver(): boolean    → players.size() < 2                         │
│  + getWinners(): List<Player>                                            │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│                   GameFactory (static utility)                            │
│──────────────────────────────────────────────────────────────────────────│
│  + create(int n, List<String> names, GameVersion): Game                  │
│    Assembles Board, Players, Dice, Strategy → returns Game               │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Relationships

```
GameFactory ──creates──▶ Game
                          ├── has-a ──▶ Board (contains Snake[], Ladder[])
                          ├── has-a ──▶ Dice
                          ├── has-a ──▶ List<Player>
                          └── has-a ──▶ TurnStrategy (interface)
                                           ▲           ▲
                                           │           │
                                    EasyTurnStrategy  DifficultTurnStrategy

BoardGenerator ──creates──▶ Board
TurnStrategy   ──returns──▶ TurnResult
```

---

## Design Patterns

**Factory Pattern** — `GameFactory.create(n, players, version)` assembles the entire game. Client never sees Board, Dice, Strategy, or BoardGenerator. Just calls create and gets a Game.

**Strategy Pattern** — `Game` holds a `TurnStrategy` interface. At runtime it's either `EasyTurnStrategy` or `DifficultTurnStrategy`. Game calls `turnStrategy.executeTurn()` without knowing which version. Adding a new version = one new class, zero changes to Game.

---

## How makeTurn() Works

1. Get current player
2. Delegate to `turnStrategy.executeTurn(player, dice, board)`
   - Roll dice, compute new position
   - If overshoot (> n²) → stay
   - Apply `board.getFinalPosition()` → resolves snake/ladder
   - Easy: if rolled 6 → roll again, no limit
   - Difficult: if 3 consecutive sixes → undo entire turn
3. If player reached n² → add to winners, remove from active list
4. Advance turn index to next player
5. Return `TurnResult`

Game ends when `players.size() < 2`.

---

## Build & Run

```bash
cd snake-ladder/src
javac com/example/snakeladder/*.java
java com.example.snakeladder.App
```
