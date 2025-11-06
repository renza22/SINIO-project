package com.sinio.demo.model;

public enum RoomStatus {
    AVAILABLE("Tersedia"),
    BOOKED("Terbooking"),
    MAINTENANCE("Perawatan");

    private final String displayName;

    RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
