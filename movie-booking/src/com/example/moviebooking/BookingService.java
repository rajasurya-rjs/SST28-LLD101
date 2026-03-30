package com.example.moviebooking;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BookingService {

    private final Map<String, City> cities = new ConcurrentHashMap<>();
    private final Map<String, Movie> movies = new ConcurrentHashMap<>();
    private final Map<String, Theater> theaters = new ConcurrentHashMap<>();
    private final Map<String, Show> shows = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final List<BookingObserver> observers = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService holdScheduler = Executors.newScheduledThreadPool(2);
    private long holdTimeoutMillis = 5 * 60 * 1000; // 5 minutes default

    private static volatile boolean instanceCreated = false;

    private BookingService() {
        if (instanceCreated) {
            throw new IllegalStateException("Use getInstance() — reflection not allowed");
        }
        instanceCreated = true;
    }

    private static class Holder {
        private static final BookingService INSTANCE = new BookingService();
    }

    public static BookingService getInstance() {
        return Holder.INSTANCE;
    }

    // ========== Configuration ==========

    public void setHoldTimeoutMillis(long holdTimeoutMillis) {
        this.holdTimeoutMillis = holdTimeoutMillis;
    }

    public void addObserver(BookingObserver observer) {
        observers.add(observer);
    }

    // ========== Admin APIs ==========

    public void addCity(City city) {
        cities.put(city.getId(), city);
    }

    public void addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
    }

    public void addTheater(Theater theater) {
        if (!cities.containsKey(theater.getCityId())) {
            throw new IllegalArgumentException("City " + theater.getCityId() + " does not exist");
        }
        theaters.put(theater.getId(), theater);
    }

    public void addShow(Show show) {
        shows.put(show.getId(), show);
    }

    // ========== User Query APIs ==========

    public List<Theater> getTheatersInCity(String cityId) {
        return theaters.values().stream()
                .filter(t -> t.getCityId().equals(cityId))
                .collect(Collectors.toList());
    }

    public List<Movie> getMoviesInCity(String cityId) {
        Set<String> movieIds = new HashSet<>();
        List<Theater> cityTheaters = getTheatersInCity(cityId);
        Set<String> screenIds = new HashSet<>();
        for (Theater theater : cityTheaters) {
            for (Screen screen : theater.getScreens()) {
                screenIds.add(screen.getId());
            }
        }
        for (Show show : shows.values()) {
            if (screenIds.contains(show.getScreen().getId())) {
                movieIds.add(show.getMovie().getId());
            }
        }
        return movieIds.stream()
                .map(movies::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Show> getShowsForMovie(String movieId) {
        return shows.values().stream()
                .filter(s -> s.getMovie().getId().equals(movieId))
                .collect(Collectors.toList());
    }

    public List<Show> getShowsForMovie(String movieId, String cityId) {
        Set<String> screenIds = new HashSet<>();
        for (Theater theater : getTheatersInCity(cityId)) {
            for (Screen screen : theater.getScreens()) {
                screenIds.add(screen.getId());
            }
        }
        return shows.values().stream()
                .filter(s -> s.getMovie().getId().equals(movieId))
                .filter(s -> screenIds.contains(s.getScreen().getId()))
                .collect(Collectors.toList());
    }

    public Map<String, ShowSeat> getSeatMap(String showId) {
        Show show = shows.get(showId);
        if (show == null) {
            throw new IllegalArgumentException("Show " + showId + " does not exist");
        }
        return show.getSeatMap();
    }

    // ========== User Management ==========

    public User registerUser(String email, String name, String phone) {
        return users.computeIfAbsent(email, e -> new User(email, name, phone));
    }

    public User getUser(String email) {
        return users.get(email);
    }

    // ========== Booking APIs ==========

    public Booking initiateBooking(String userEmail, String showId, List<String> seatIds) {
        User user = users.get(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("User " + userEmail + " not registered");
        }
        Show show = shows.get(showId);
        if (show == null) {
            throw new IllegalArgumentException("Show " + showId + " does not exist");
        }

        // Hold seats (all-or-nothing, thread-safe)
        show.holdSeats(seatIds);

        // Compute total price with demand pricing
        List<PricingRule> demandRules = List.of(new DemandPricingRule(0.7, 1.5));
        List<ShowSeat> bookedSeats = new ArrayList<>();
        double totalAmount = 0;
        for (String seatId : seatIds) {
            ShowSeat showSeat = show.getSeatMap().get(seatId);
            double livePrice = showSeat.getFinalPrice();
            for (PricingRule rule : demandRules) {
                livePrice = rule.apply(livePrice, show, showSeat.getSeat());
            }
            livePrice = Math.round(livePrice * 100.0) / 100.0;
            showSeat.setFinalPrice(livePrice);
            bookedSeats.add(showSeat);
            totalAmount += livePrice;
        }

        String bookingId = "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = new Booking(bookingId, user, show, bookedSeats, totalAmount);

        // Schedule hold timeout
        ScheduledFuture<?> future = holdScheduler.schedule(
                () -> releaseHold(bookingId),
                holdTimeoutMillis,
                TimeUnit.MILLISECONDS
        );
        booking.setHoldTimer(future);
        bookings.put(bookingId, booking);

        return booking;
    }

    public Booking confirmBooking(String bookingId, PaymentMethod paymentMethod) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking " + bookingId + " does not exist");
        }
        if (booking.getStatus() != BookingStatus.SEATS_HELD) {
            throw new IllegalStateException("Booking " + bookingId + " is not in SEATS_HELD state, current: " + booking.getStatus());
        }

        // Cancel hold timer
        booking.cancelHoldTimer();

        // Process payment
        PaymentStrategy strategy = PaymentStrategyFactory.getStrategy(paymentMethod);
        Payment payment = strategy.pay(bookingId, booking.getTotalAmount());
        booking.setPayment(payment);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            List<String> seatIds = booking.getSeats().stream()
                    .map(s -> s.getSeat().getId())
                    .collect(Collectors.toList());
            booking.getShow().confirmSeats(seatIds);
            booking.setStatus(BookingStatus.CONFIRMED);
            notifyConfirmed(booking);
        } else {
            List<String> seatIds = booking.getSeats().stream()
                    .map(s -> s.getSeat().getId())
                    .collect(Collectors.toList());
            booking.getShow().releaseSeats(seatIds);
            booking.setStatus(BookingStatus.CANCELLED);
        }

        return booking;
    }

    public Booking cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking " + bookingId + " does not exist");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled, current: " + booking.getStatus());
        }

        // Release seats
        List<String> seatIds = booking.getSeats().stream()
                .map(s -> s.getSeat().getId())
                .collect(Collectors.toList());
        booking.getShow().releaseSeats(seatIds);

        // Process refund via original payment method
        PaymentStrategy strategy = PaymentStrategyFactory.getStrategy(booking.getPayment().getMethod());
        strategy.refund(booking.getPayment());

        booking.setStatus(BookingStatus.CANCELLED);
        notifyCancelled(booking);

        return booking;
    }

    private void releaseHold(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) return;
        if (booking.getStatus() != BookingStatus.SEATS_HELD) return;

        List<String> seatIds = booking.getSeats().stream()
                .map(s -> s.getSeat().getId())
                .collect(Collectors.toList());
        booking.getShow().releaseSeats(seatIds);
        booking.setStatus(BookingStatus.CANCELLED);
        System.out.println("  [TIMEOUT] Booking " + bookingId + " expired — seats released automatically");
    }

    // ========== Observer Notifications ==========

    private void notifyConfirmed(Booking booking) {
        for (BookingObserver observer : observers) {
            observer.onBookingConfirmed(booking);
        }
    }

    private void notifyCancelled(Booking booking) {
        for (BookingObserver observer : observers) {
            observer.onBookingCancelled(booking);
        }
    }

    // ========== Lifecycle ==========

    public void shutdown() {
        holdScheduler.shutdownNow();
    }
}
