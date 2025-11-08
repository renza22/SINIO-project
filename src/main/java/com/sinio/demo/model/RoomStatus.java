package com.sinio.demo.model;

public enum RoomStatus {
    AVAILABLE("Tersedia"),
    BOOKED("Terbooking"),
    OCCUPIED("Terisi"),
    CLEANING("Pembersihan"),
    MAINTENANCE("Perawatan");

    private final String displayName;

    RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
