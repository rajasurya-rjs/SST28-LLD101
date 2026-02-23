public class DeluxeRoomPricing implements RoomPricing {
    @Override
    public boolean supports(int roomType) {
        return roomType == LegacyRoomTypes.DELUXE;
    }

    @Override
    public double basePrice() {
        return 16000.0;
    }
}
