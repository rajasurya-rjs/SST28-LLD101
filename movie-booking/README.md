# Movie Ticket Booking System

A low-level design implementation of a movie ticket booking system demonstrating multiple design patterns, concurrency handling, and dynamic pricing.

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Singleton** (Bill Pugh Holder) | `BookingService` | Single orchestrator managing all bookings |
| **Strategy** | `PricingRule`, `PaymentStrategy` | Pluggable pricing rules and payment methods |
| **Factory** | `PricingStrategyFactory`, `PaymentStrategyFactory` | Create strategies from configuration |
| **Builder** | `Show.Builder` | Shows have many configurable fields |
| **Observer** | `BookingObserver` / `NotificationService` | Notify on booking confirm/cancel |

## Class Diagrams

### Enums

```
+---------------------+    +---------------------+    +---------------------+
|  <<enumeration>>    |    |  <<enumeration>>    |    |  <<enumeration>>    |
|    SeatCategory     |    |     SeatStatus      |    |   BookingStatus     |
+---------------------+    +---------------------+    +---------------------+
| SILVER  (Rs.200)    |    | AVAILABLE           |    | INITIATED           |
| GOLD    (Rs.500)    |    | HELD                |    | SEATS_HELD          |
| DIAMOND (Rs.800)    |    | BOOKED              |    | CONFIRMED           |
+---------------------+    +---------------------+    | CANCELLED           |
| - basePrice: double |                               +---------------------+
+---------------------+
| + getBasePrice()    |
+---------------------+

+---------------------+    +---------------------+    +---------------------+
|  <<enumeration>>    |    |  <<enumeration>>    |    |  <<enumeration>>    |
|   PaymentMethod     |    |   PaymentStatus     |    |      DayType        |
+---------------------+    +---------------------+    +---------------------+
| UPI                 |    | PENDING             |    | WEEKDAY             |
| CARD                |    | SUCCESS             |    | WEEKEND             |
| NET_BANKING         |    | FAILED              |    +---------------------+
+---------------------+    | REFUNDED            |    | + from(LocalDate)   |
                           +---------------------+    +---------------------+

+---------------------+
|  <<enumeration>>    |
|    ShowTiming       |
+---------------------+
| MORNING             |
| AFTERNOON           |
| EVENING             |
| NIGHT               |
+---------------------+
| + from(LocalTime)   |
+---------------------+
```

### Core Models

```
+----------------------------+         +-----------------------------------+
|           City             |         |             Movie                 |
+----------------------------+         +-----------------------------------+
| - id: String               |         | - id: String                      |
| - name: String             |         | - title: String                   |
+----------------------------+         | - localizedTitles: Map<String,    |
| + getId(): String          |         |                        String>    |
| + getName(): String        |         | - durationMinutes: int            |
+----------------------------+         | - genre: String                   |
                                       +-----------------------------------+
                                       | + getTitle(lang): String           |
                                       | + getLocalizedTitles(): Map       |
                                       +-----------------------------------+

+----------------------------+         +-----------------------------------+
|           User             |         |           Payment                 |
+----------------------------+         +-----------------------------------+
| - email: String  [unique]  |         | - id: String                      |
| - name: String             |         | - bookingId: String               |
| - phone: String            |         | - amount: double                  |
+----------------------------+         | - method: PaymentMethod           |
| + getEmail(): String       |         | - status: PaymentStatus           |
| + getName(): String        |         | - timestamp: LocalDateTime        |
| + getPhone(): String       |         +-----------------------------------+
+----------------------------+         | + markSuccess()                   |
                                       | + markFailed()                    |
                                       | + markRefunded()                  |
                                       +-----------------------------------+

+----------------------------+
|           Seat             |
+----------------------------+
| - id: String               |
| - row: int                 |
| - col: int                 |
| - category: SeatCategory   |
+----------------------------+
| + getId(): String          |
| + getRow(): int            |
| + getCol(): int            |
| + getCategory(): SeatCat.. |
+----------------------------+
```

### Theater Structure (Theater has-many Screens, Screen has-many Seats)

```
+----------------------------+    1..*    +----------------------------+   1..*   +------------------+
|          Theater           |◆--------->|          Screen           |◆-------->|      Seat        |
+----------------------------+           +----------------------------+          +------------------+
| - id: String               |           | - id: String               |          | (see above)      |
| - name: String             |           | - name: String             |          +------------------+
| - cityId: String           |           | - theaterId: String        |
| - screens: List<Screen>    |           | - seats: List<Seat>        |
+----------------------------+           +----------------------------+
| + addScreen(Screen)        |           | + getSeats(): List<Seat>   |
| + getScreens(): List       |           +----------------------------+
+----------------------------+
```

### Show (Builder Pattern) — owns ReentrantLock for concurrency

```
+------------------------------------------+        +----------------------------+
|                 Show                     |        |        ShowSeat            |
+------------------------------------------+        +----------------------------+
| - id: String                             |  1..*  | - seat: Seat               |
| - movie: Movie                           |◆------>| - status: SeatStatus       |
| - screen: Screen                         |        | - finalPrice: double       |
| - startTime: LocalDateTime               |        +----------------------------+
| - endTime: LocalDateTime                 |        | + hold()                   |
| - seatMap: Map<String, ShowSeat>         |        |   AVAILABLE -> HELD        |
| - lock: ReentrantLock                    |        | + book()                   |
| - pricingRules: List<PricingRule>        |        |   HELD -> BOOKED           |
+------------------------------------------+        | + release()                |
| + holdSeats(List<String>)     [locked]   |        |   any -> AVAILABLE         |
| + confirmSeats(List<String>)  [locked]   |        +----------------------------+
| + releaseSeats(List<String>)  [locked]   |
| + getAvailableSeats(): List<ShowSeat>    |
| + getTotalSeats(): int                   |
| + getBookedSeatCount(): int              |
+------------------------------------------+
| <<static inner class>>                   |
| +--------------------------------------+ |
| |           Show.Builder               | |
| +--------------------------------------+ |
| | + id(String): Builder                | |
| | + movie(Movie): Builder              | |
| | + screen(Screen): Builder            | |
| | + startTime(LocalDateTime): Builder  | |
| | + pricingRules(List): Builder        | |
| | + build(): Show                      | |
| +--------------------------------------+ |
+------------------------------------------+
```

### Booking (references Show, User, Payment)

```
+-------------------------------------------+
|               Booking                     |
+-------------------------------------------+
| - id: String                              |
| - user: User  ---------------------->  User
| - show: Show  ---------------------->  Show
| - seats: List<ShowSeat>                   |
| - totalAmount: double                     |
| - status: BookingStatus                   |
| - payment: Payment  --------------->  Payment
| - createdAt: LocalDateTime                |
| - holdTimer: ScheduledFuture<?>           |
+-------------------------------------------+
| + setStatus(BookingStatus)                |
| + setPayment(Payment)                     |
| + setHoldTimer(ScheduledFuture)           |
| + cancelHoldTimer()                       |
+-------------------------------------------+
```

### Pricing Strategy (Strategy + Factory Pattern)

```
                  +---------------------------------------+
                  |     <<interface>> PricingRule         |
                  +---------------------------------------+
                  | + apply(price, Show, Seat): double    |
                  +---------------------------------------+
                       ^         ^         ^         ^
                       |         |         |         |
        +--------------+--+  +---+-------+ | +-------+------------+
        |                 |  |           | | |                    |
+-------+----------+ +----+--------+ +--+-+-------+ +------------+--------+
| ShowTimingPricing| | DayOfWeek   | | WeekOfMonth| |  DemandPricingRule  |
|      Rule        | | PricingRule | | PricingRule| +---------------------+
+------------------+ +-------------+ +------------+ | - highDemandThresh  |
| Morning:  0.8x  | | Weekend:1.3x| | Week1: 1.1x| | - multiplier        |
| Afternoon:0.9x  | | Weekday:1.0x| | Week4: 0.9x| +---------------------+
| Evening:  1.2x  | +-------------+ | Mid:   1.0x| | occupancy >= thresh |
| Night:    1.0x  |                  +------------+ |   => price * mult   |
+------------------+                                 +---------------------+

+------------------------------------------+
|       PricingStrategyFactory             |
+------------------------------------------+
| + defaultRules(): List<PricingRule>      |
| + defaultRulesWithDemand(): List         |
| + computePrice(Seat, Show,              |
|       List<PricingRule>): double         |
+------------------------------------------+
  Chains rules: base price -> rule1 -> rule2 -> ... -> final price
```

### Payment Strategy (Strategy + Factory Pattern)

```
              +------------------------------------------+
              |    <<interface>> PaymentStrategy         |
              +------------------------------------------+
              | + pay(bookingId, amount): Payment        |
              | + refund(Payment): Payment               |
              +------------------------------------------+
                     ^            ^            ^
                     |            |            |
          +----------+--+  +-----+------+  +--+--------------+
          |             |  |            |  |                 |
+---------+--------+ +--+----------+ +-+------------------+
| UpiPayment       | | CardPayment | | NetBankingPayment  |
|    Strategy      | |   Strategy  | |     Strategy       |
+------------------+ +-------------+ +--------------------+
| pay():  UPI txn  | | pay():CARD  | | pay(): NET_BANKING |
| refund(): UPI    | | refund()    | | refund()           |
+------------------+ +-------------+ +--------------------+

+------------------------------------------+
|       PaymentStrategyFactory             |
+------------------------------------------+
| + getStrategy(PaymentMethod):            |
|       PaymentStrategy                    |
+------------------------------------------+
  UPI -> UpiPaymentStrategy
  CARD -> CardPaymentStrategy
  NET_BANKING -> NetBankingPaymentStrategy
```

### Observer Pattern

```
              +------------------------------------------+
              |   <<interface>> BookingObserver          |
              +------------------------------------------+
              | + onBookingConfirmed(Booking)            |
              | + onBookingCancelled(Booking)            |
              +------------------------------------------+
                               ^
                               |
              +------------------------------------------+
              |        NotificationService              |
              +------------------------------------------+
              | + onBookingConfirmed(Booking)            |
              |     => prints confirmation + email       |
              | + onBookingCancelled(Booking)            |
              |     => prints cancellation + refund      |
              +------------------------------------------+
```

### BookingService (Singleton — Bill Pugh Holder)

```
+----------------------------------------------------------------+
|                      BookingService                            |
+----------------------------------------------------------------+
| - cities: Map<String, City>                                    |
| - movies: Map<String, Movie>                                   |
| - theaters: Map<String, Theater>                               |
| - shows: Map<String, Show>                                     |
| - bookings: Map<String, Booking>                               |
| - users: Map<String, User>                                     |
| - observers: List<BookingObserver>                              |
| - holdScheduler: ScheduledExecutorService                      |
| - holdTimeoutMillis: long                                      |
+----------------------------------------------------------------+
| <<Singleton>>                                                  |
| - BookingService()               [private constructor]         |
| + getInstance(): BookingService  [Bill Pugh Holder]            |
+----------------------------------------------------------------+
| <<Admin APIs>>                                                 |
| + addCity(City)                                                |
| + addMovie(Movie)                                              |
| + addTheater(Theater)                                          |
| + addShow(Show)                                                |
+----------------------------------------------------------------+
| <<User Query APIs>>                                            |
| + getTheatersInCity(cityId): List<Theater>                     |
| + getMoviesInCity(cityId): List<Movie>                         |
| + getShowsForMovie(movieId): List<Show>                        |
| + getShowsForMovie(movieId, cityId): List<Show>                |
| + getSeatMap(showId): Map<String, ShowSeat>                    |
+----------------------------------------------------------------+
| <<Booking APIs>>                                               |
| + initiateBooking(email, showId, seatIds): Booking             |
|     => hold seats + schedule timeout                           |
| + confirmBooking(bookingId, PaymentMethod): Booking            |
|     => cancel timer + pay + confirm seats                      |
| + cancelBooking(bookingId): Booking                            |
|     => release seats + refund + notify                         |
| - releaseHold(bookingId)                                       |
|     => auto-release on timeout                                 |
+----------------------------------------------------------------+
| <<User Management>>                                            |
| + registerUser(email, name, phone): User                       |
+----------------------------------------------------------------+
| <<Observer>>                                                   |
| + addObserver(BookingObserver)                                 |
| - notifyConfirmed(Booking)                                     |
| - notifyCancelled(Booking)                                     |
+----------------------------------------------------------------+
| + shutdown()                                                   |
+----------------------------------------------------------------+
```

### Full Relationship Diagram

```
                            +----------------+
                            |     City       |
                            +-------+--------+
                                    |
                         cityId     |
                            +-------+--------+
                            |    Theater     |
                            +-------+--------+
                                    | 1..*
                            +-------+--------+
                            |    Screen      |
                            +-------+--------+
                                    | 1..*
                            +-------+--------+
                            |     Seat       |
                            +----------------+
                                    |
                              wraps |
                            +-------+--------+          +----------------+
                            |   ShowSeat     |<---1..*--| <<PricingRule>>|
                            +-------+--------+          +-------+--------+
                                    | 1..*                      |
                            +-------+--------+          implemented by:
                  +-------->|     Show       |          - ShowTimingPricingRule
                  |         +--+----+--------+          - DayOfWeekPricingRule
                  |            |    |                    - WeekOfMonthPricingRule
                  | refs       |    | refs               - DemandPricingRule
                  |            |    |
          +-------+--+        |    +--------+
          |  Movie   |        |             |
          +----------+   +----+------+ +----+------+
                          |  Booking  | |   User    |
                          +----+------+ +-----------+
                               |
                          +----+------+
                          |  Payment  |
                          +-----------+
                               |
                    processed by:
              +----------------+----------------+
              |                |                |
    +---------+----+ +--------+-----+ +--------+---------+
    |UpiPayment    | |CardPayment   | |NetBankingPayment |
    |  Strategy    | |  Strategy    | |    Strategy       |
    +--------------+ +--------------+ +------------------+
              implements <<PaymentStrategy>>

    BookingService (Singleton) orchestrates everything
         |
         +--- observers ---> <<BookingObserver>>
                                  |
                           NotificationService
```

## Concurrency Model

- Each `Show` owns a `ReentrantLock`
- **holdSeats()**: Lock -> validate ALL seats are AVAILABLE -> mark ALL as HELD -> unlock (all-or-nothing)
- **confirmSeats()** / **releaseSeats()**: Same lock
- Two threads booking overlapping seats: one wins, one gets an exception
- **Hold timeout**: `ScheduledExecutorService` auto-releases seats after configurable duration

## Seat Booking State Machine

```
AVAILABLE --[hold]--> HELD --[book]--> BOOKED
    ^                   |                  |
    |---[release]-------+                  |
    |---[release (cancel)]----------------+
```

## Dynamic Pricing

Prices are computed by chaining `PricingRule` implementations:

1. **ShowTimingPricingRule**: Morning 0.8x, Afternoon 0.9x, Evening 1.2x, Night 1.0x
2. **DayOfWeekPricingRule**: Weekend 1.3x, Weekday 1.0x
3. **WeekOfMonthPricingRule**: First week 1.1x, Last week 0.9x, Middle 1.0x
4. **DemandPricingRule**: Above 70% occupancy -> 1.5x (applied at booking time)

Rules 1-3 are applied at show creation (display price). Rule 4 is applied at booking time (actual charge).

## Build & Run

```bash
cd movie-booking/src
javac com/example/moviebooking/*.java
java com.example.moviebooking.App
```

## Demo Scenarios

The `App.java` demonstrates:

1. **Admin setup** - Add cities, movies (with localized titles), theaters, screens, shows
2. **User browsing** - View theaters, movies, shows, seat maps
3. **Dynamic pricing** - Compare Gold seat prices: morning weekday vs evening weekend
4. **Booking flow** - Hold seats -> pay via UPI -> confirm
5. **Concurrent booking** - Two threads race for same seats (one fails, no double booking)
6. **Hold timeout** - Seats auto-released after 3 seconds if not paid
7. **Cancel & refund** - Cancel confirmed booking, refund via original payment method
