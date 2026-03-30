package com.example.moviebooking;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class App {

    public static void main(String[] args) throws Exception {
        BookingService service = BookingService.getInstance();
        service.addObserver(new NotificationService());
        service.setHoldTimeoutMillis(3000); // 3 seconds for demo

        // ========== 1. ADMIN SETUP ==========
        System.out.println("=== ADMIN SETUP ===\n");

        City bangalore = new City("city1", "Bangalore");
        City mumbai = new City("city2", "Mumbai");
        service.addCity(bangalore);
        service.addCity(mumbai);
        System.out.println("Cities added: " + bangalore + ", " + mumbai);

        // Movies with multilingual support
        Movie movie1 = new Movie("m1", "Inception",
                Map.of("hi", "इन्सेप्शन", "ta", "இன்செப்ஷன்"),
                148, "Sci-Fi");
        Movie movie2 = new Movie("m2", "RRR",
                Map.of("hi", "आरआरआर", "te", "ఆర్ఆర్ఆర్"),
                187, "Action");
        service.addMovie(movie1);
        service.addMovie(movie2);
        System.out.println("Movies added: " + movie1 + ", " + movie2);
        System.out.println("  Inception in Hindi: " + movie1.getTitle("hi"));
        System.out.println("  RRR in Telugu: " + movie2.getTitle("te"));

        // Theater with 2 screens
        Theater theater1 = new Theater("t1", "PVR Orion", "city1");
        service.addTheater(theater1);

        // Screen 1: 3 rows (Silver=4, Gold=4, Diamond=2)
        List<Seat> screen1Seats = new ArrayList<>();
        for (int col = 1; col <= 4; col++)
            screen1Seats.add(new Seat("S-" + col, 1, col, SeatCategory.SILVER));
        for (int col = 1; col <= 4; col++)
            screen1Seats.add(new Seat("G-" + col, 2, col, SeatCategory.GOLD));
        for (int col = 1; col <= 2; col++)
            screen1Seats.add(new Seat("D-" + col, 3, col, SeatCategory.DIAMOND));

        Screen screen1 = new Screen("scr1", "Screen 1", "t1", screen1Seats);
        theater1.addScreen(screen1);

        // Screen 2: smaller
        List<Seat> screen2Seats = new ArrayList<>();
        for (int col = 1; col <= 3; col++)
            screen2Seats.add(new Seat("S-" + col, 1, col, SeatCategory.SILVER));
        for (int col = 1; col <= 3; col++)
            screen2Seats.add(new Seat("G-" + col, 2, col, SeatCategory.GOLD));

        Screen screen2 = new Screen("scr2", "Screen 2", "t1", screen2Seats);
        theater1.addScreen(screen2);
        System.out.println("Theater added: " + theater1 + " with screens: " + screen1 + ", " + screen2);

        // Shows with different timings for pricing demo
        List<PricingRule> defaultRules = PricingStrategyFactory.defaultRules();

        Show morningShow = new Show.Builder()
                .id("show1")
                .movie(movie1)
                .screen(screen1)
                .startTime(LocalDateTime.of(2026, 4, 1, 9, 0))  // Wednesday morning
                .pricingRules(defaultRules)
                .build();

        Show eveningShow = new Show.Builder()
                .id("show2")
                .movie(movie1)
                .screen(screen1)
                .startTime(LocalDateTime.of(2026, 4, 4, 18, 30)) // Saturday evening
                .pricingRules(defaultRules)
                .build();

        Show show3 = new Show.Builder()
                .id("show3")
                .movie(movie2)
                .screen(screen2)
                .startTime(LocalDateTime.of(2026, 4, 2, 15, 0))  // Thursday afternoon
                .pricingRules(defaultRules)
                .build();

        service.addShow(morningShow);
        service.addShow(eveningShow);
        service.addShow(show3);
        System.out.println("Shows added: " + morningShow.getId() + ", " + eveningShow.getId() + ", " + show3.getId());

        // ========== 2. USER BROWSING ==========
        System.out.println("\n=== USER BROWSING ===\n");

        System.out.println("Theaters in Bangalore:");
        for (Theater t : service.getTheatersInCity("city1")) {
            System.out.println("  " + t);
        }

        System.out.println("\nMovies in Bangalore:");
        for (Movie m : service.getMoviesInCity("city1")) {
            System.out.println("  " + m);
        }

        System.out.println("\nShows for 'Inception' in Bangalore:");
        for (Show s : service.getShowsForMovie("m1", "city1")) {
            System.out.println("  " + s);
        }

        System.out.println("\nSeat map for morning show (show1):");
        printSeatMap(service.getSeatMap("show1"));

        // ========== 3. DYNAMIC PRICING COMPARISON ==========
        System.out.println("\n=== DYNAMIC PRICING COMPARISON ===\n");

        ShowSeat morningGold = morningShow.getSeatMap().get("G-1");
        ShowSeat eveningGold = eveningShow.getSeatMap().get("G-1");
        System.out.println("Gold seat G-1 pricing:");
        System.out.println("  Base price:                    Rs." + String.format("%.2f", SeatCategory.GOLD.getBasePrice()));
        System.out.println("  Morning show (Wed, Apr 1):     Rs." + String.format("%.2f", morningGold.getFinalPrice()));
        System.out.println("  Evening show (Sat, Apr 4):     Rs." + String.format("%.2f", eveningGold.getFinalPrice()));
        System.out.println("  (Morning=0.8x timing * 1.0x weekday * 1.1x first-week = " +
                String.format("%.2f", 500 * 0.8 * 1.0 * 1.1) + ")");
        System.out.println("  (Evening=1.2x timing * 1.3x weekend * 1.1x first-week = " +
                String.format("%.2f", 500 * 1.2 * 1.3 * 1.1) + ")");

        // ========== 4. BOOKING FLOW ==========
        System.out.println("\n=== BOOKING FLOW ===\n");

        User user1 = service.registerUser("alice@example.com", "Alice", "9876543210");
        System.out.println("Registered: " + user1);

        System.out.println("\nInitiating booking for 2 Gold seats (G-1, G-2) on evening show...");
        Booking booking1 = service.initiateBooking("alice@example.com", "show2", List.of("G-1", "G-2"));
        System.out.println("Booking created: " + booking1);

        System.out.println("\nSeat map after hold:");
        printSeatMap(service.getSeatMap("show2"));

        System.out.println("\nConfirming booking with UPI...");
        booking1 = service.confirmBooking(booking1.getId(), PaymentMethod.UPI);
        System.out.println("Booking confirmed: " + booking1);

        System.out.println("\nSeat map after confirmation:");
        printSeatMap(service.getSeatMap("show2"));

        // ========== 5. CONCURRENT BOOKING DEMO ==========
        System.out.println("\n=== CONCURRENT BOOKING DEMO ===\n");

        User user2 = service.registerUser("bob@example.com", "Bob", "9876543211");
        User user3 = service.registerUser("charlie@example.com", "Charlie", "9876543212");
        System.out.println("Two users trying to book same seats (D-1, D-2) on morning show simultaneously...\n");

        CountDownLatch startLatch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();
                Booking b = service.initiateBooking("bob@example.com", "show1", List.of("D-1", "D-2"));
                service.confirmBooking(b.getId(), PaymentMethod.CARD);
                results.add("Bob: SUCCESS - booked D-1, D-2");
            } catch (Exception e) {
                results.add("Bob: FAILED - " + e.getMessage());
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();
                Booking b = service.initiateBooking("charlie@example.com", "show1", List.of("D-1", "D-2"));
                service.confirmBooking(b.getId(), PaymentMethod.NET_BANKING);
                results.add("Charlie: SUCCESS - booked D-1, D-2");
            } catch (Exception e) {
                results.add("Charlie: FAILED - " + e.getMessage());
            }
        });

        thread1.start();
        thread2.start();
        startLatch.countDown(); // release both threads simultaneously
        thread1.join();
        thread2.join();

        System.out.println("Results:");
        for (String r : results) {
            System.out.println("  " + r);
        }
        System.out.println("(One succeeded, one failed — no double booking!)");

        // ========== 6. HOLD TIMEOUT DEMO ==========
        System.out.println("\n=== HOLD TIMEOUT DEMO ===\n");

        User user4 = service.registerUser("dave@example.com", "Dave", "9876543213");
        System.out.println("Dave holds seats S-1, S-2 on morning show but does NOT pay...");
        Booking timeoutBooking = service.initiateBooking("dave@example.com", "show1", List.of("S-1", "S-2"));
        System.out.println("Booking status: " + timeoutBooking.getStatus());

        System.out.println("Seat S-1 status: " + morningShow.getSeatMap().get("S-1").getStatus());
        System.out.println("Waiting for hold timeout (3 seconds)...");
        Thread.sleep(4000);

        System.out.println("Seat S-1 status after timeout: " + morningShow.getSeatMap().get("S-1").getStatus());
        System.out.println("Booking status after timeout: " + timeoutBooking.getStatus());

        // ========== 7. CANCEL & REFUND DEMO ==========
        System.out.println("\n=== CANCEL & REFUND DEMO ===\n");

        System.out.println("Alice cancelling her evening show booking...");
        Booking cancelled = service.cancelBooking(booking1.getId());
        System.out.println("Booking after cancel: " + cancelled);

        System.out.println("\nSeat map after cancellation (G-1, G-2 available again):");
        printSeatMap(service.getSeatMap("show2"));

        // ========== CLEANUP ==========
        service.shutdown();
        System.out.println("\n=== DONE ===");
    }

    private static void printSeatMap(Map<String, ShowSeat> seatMap) {
        for (ShowSeat showSeat : seatMap.values()) {
            System.out.println("    " + showSeat);
        }
    }
}
