public interface BookingRepository {
    void save(String id, BookingRequest req, Money monthly, Money deposit);
}
