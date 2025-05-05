package algo.transit.enums;

public enum TransportType {
    BUS,
    TRAIN,
    TRAM,
    METRO,
    FOOT,
    UNKNOWN;

    public static TransportType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) return UNKNOWN;

        try {
            return valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}