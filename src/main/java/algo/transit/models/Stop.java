package algo.transit.models;

import java.util.HashMap;
import java.util.Map;

public class Stop {
    public final String stopId;
    public final String name;
    public final double latitude;
    public final double longitude;
    public final Map<String, Route> routes;
    public final Map<String, Trip> trips;

    public Stop(String stopId, String name, double latitude, double longitude) {
        this.stopId = stopId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.routes = new HashMap<>();
        this.trips = new HashMap<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Stop stop = (Stop) obj;
        return Double.compare(stop.latitude, latitude) == 0 &&
                Double.compare(stop.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return stopId != null ? stopId.hashCode() : 0;
    }
}