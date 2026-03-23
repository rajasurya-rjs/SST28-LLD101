package com.example.parkinglot;

import java.util.Objects;
import java.util.UUID;

public final class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final Slot slotAssigned;
    private final SlotType allocatedSlotType;
    private final long entryTime;

    public ParkingTicket(Vehicle vehicle, Slot slotAssigned, long entryTime) {
        this.ticketId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.slotAssigned = Objects.requireNonNull(slotAssigned, "slotAssigned");
        this.allocatedSlotType = slotAssigned.getSlotType();
        this.entryTime = entryTime;
    }

    public String getTicketId() { return ticketId; }
    public Vehicle getVehicle() { return vehicle; }
    public Slot getSlotAssigned() { return slotAssigned; }
    public SlotType getAllocatedSlotType() { return allocatedSlotType; }
    public long getEntryTime() { return entryTime; }

    @Override
    public String toString() {
        return "Ticket{" + ticketId
                + ", vehicle=" + vehicle
                + ", slot=" + slotAssigned.getId()
                + ", type=" + allocatedSlotType
                + ", entry=" + entryTime + "}";
    }
}
