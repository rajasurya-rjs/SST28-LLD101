public class TripleRoomPricing implements RoomPricing {
    @Override
    public boolean supports(int roomType) {
        return roomType == LegacyRoomTypes.TRIPLE;
    }

    @Override
    public double basePrice() {
        return 12000.0;
    }
}
