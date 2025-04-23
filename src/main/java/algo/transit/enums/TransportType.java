package algo.transit.enums;

public enum TransportType {
    BUS,
    TRAIN,
    TRAM,
    METRO,
    UNKNOWN;

    /**
     * Convert a string to the corresponding TransportType enum
     *
     * @param typeStr String representation of transport type
     * @return The matching TransportType or UNKNOWN if no match
     */
    public static TransportType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return UNKNOWN;
        }

        try {
            return valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
