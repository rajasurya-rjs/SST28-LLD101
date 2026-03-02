public class TransportBookingService {
    private final DistanceService dist;
    private final AllocationService alloc;
    private final PaymentService pay;

    public TransportBookingService(DistanceService dist, AllocationService alloc, PaymentService pay) {
        this.dist = dist;
        this.alloc = alloc;
        this.pay = pay;
    }

    public void book(TripRequest req) {
        double km = dist.km(req.from, req.to);
        System.out.println("DistanceKm=" + km);

        String driver = alloc.allocate(req.studentId);
        System.out.println("Driver=" + driver);

        double fare = 50.0 + km * 6.6666666667;
        fare = Math.round(fare * 100.0) / 100.0;

        String txn = pay.charge(req.studentId, fare);
        System.out.println("Payment=PAID txn=" + txn);

        BookingReceipt r = new BookingReceipt("R-501", fare);
        System.out.println("RECEIPT: " + r.id + " | fare=" + String.format("%.2f", r.fare));
    }
}
