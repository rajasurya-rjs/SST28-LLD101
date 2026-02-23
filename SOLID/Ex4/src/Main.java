import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Hostel Fee Calculator ===");

        List<RoomPricing> roomPricings = List.of(
                new SingleRoomPricing(),
                new DoubleRoomPricing(),
                new TripleRoomPricing(),
                new DeluxeRoomPricing());

        List<AddOnPricing> addOnPricings = List.of(
                new MessAddOnPricing(),
                new LaundryAddOnPricing(),
                new GymAddOnPricing());

        BookingRepository repo = new FakeBookingRepo();
        BookingRequest req = new BookingRequest(LegacyRoomTypes.DOUBLE, List.of(AddOn.LAUNDRY, AddOn.MESS));
        HostelFeeCalculator calc = new HostelFeeCalculator(roomPricings, addOnPricings, repo);
        calc.process(req);
    }
}
