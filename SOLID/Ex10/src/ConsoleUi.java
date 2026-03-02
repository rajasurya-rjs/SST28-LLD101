interface DistanceService {
    double km(GeoPoint a, GeoPoint b);
}

interface AllocationService {
    String allocate(String studentId);
}

interface PaymentService {
    String charge(String studentId, double amount);
}
