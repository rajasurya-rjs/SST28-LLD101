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

## Class Diagram

```
BookingService (Singleton - Bill Pugh Holder)
 |
 |-- has-many --> City
 |-- has-many --> Movie (multilingual titles via Map<String, String>)
 |-- has-many --> Theater
 |                  \-- has-many --> Screen
 |                                    \-- has-many --> Seat (SeatCategory: SILVER/GOLD/DIAMOND)
 |-- has-many --> Show (owns ReentrantLock for concurrency)
 |                  |-- refs --> Movie
 |                  |-- refs --> Screen
 |                  |-- has-many --> ShowSeat (Seat + SeatStatus + finalPrice)
 |                  \-- has-many --> PricingRule <<Strategy>>
 |                                    |-- ShowTimingPricingRule
 |                                    |-- DayOfWeekPricingRule
 |                                    |-- WeekOfMonthPricingRule
 |                                    \-- DemandPricingRule
 |-- has-many --> Booking
 |                  |-- refs --> User (email = unique ID)
 |                  |-- refs --> Show
 |                  |-- has-many --> ShowSeat
 |                  |-- has-one --> Payment
 |                  \-- has-one --> ScheduledFuture (hold timer)
 |-- has-many --> User
 |-- has-many --> BookingObserver <<Observer>>
 |                  \-- NotificationService
 |-- uses --> PaymentStrategyFactory
 |              \-- creates --> PaymentStrategy <<Strategy>>
 |                                |-- UpiPaymentStrategy
 |                                |-- CardPaymentStrategy
 |                                \-- NetBankingPaymentStrategy
 \-- owns --> ScheduledExecutorService (hold timer pool)
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
