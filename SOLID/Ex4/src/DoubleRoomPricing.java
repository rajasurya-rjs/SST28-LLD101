public class DoubleRoomPricing implements RoomPricing {
    @Override
    public boolean supports(int roomType) {
        return roomType == LegacyRoomTypes.DOUBLE;
    }

    @Override
    public double basePrice() {
        return 15000.0;
    }
}
