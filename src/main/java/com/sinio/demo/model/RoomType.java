package com.sinio.demo.model;

import java.util.Arrays;
import java.util.List;

public enum RoomType {
    DELUXE_KING("Deluxe King"),
    DELUXE_TWIN("Deluxe Twin"),
    SUITE_PANORAMA("Suite Panorama"),
    SUPERIOR_TWIN("Superior Twin"),
    STUDIO_LOFT("Studio Loft"),
    EXECUTIVE_SUITE("Executive Suite"),
    FAMILY_ROOM("Family Room"),
    PRESIDENTIAL_SUITE("Presidential Suite"),
    STANDARD("Standard"),
    VILLA("Villa");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static List<RoomType> defaultOrder() {
        return Arrays.asList(values());
    }
}
