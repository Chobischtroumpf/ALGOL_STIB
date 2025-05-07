package algo.transit.enums;

public enum TType {
    BUS,
    TRAIN,
    TRAM,
    METRO,
    FOOT,
    UNKNOWN;

    public static TType fromString(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) return UNKNOWN;

        try {
            return valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}