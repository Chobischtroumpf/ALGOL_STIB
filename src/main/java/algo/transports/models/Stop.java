package algo.transports.models;

public class Stop {
    private final String stopId;
    private final String name;
    private final double latitude;
    private final double longitude;

    public Stop(String stopId, String name, double latitude, double longitude) {
        this.stopId     = stopId;
        this.name       = name;
        this.latitude   = latitude;
        this.longitude  = longitude;
    }

    public String getId() {
        return stopId;
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return latitude;
    }

    public double getLon() {
        return longitude;
    }
}
