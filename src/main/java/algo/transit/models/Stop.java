package algo.transit.models;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class Stop {
    private final String stopId;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final Map<String, Route> routes;
    private final Map<String, Trip> trips;

    public Stop(String stopId, String name, double latitude, double longitude) {
        this.stopId = stopId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.routes = new HashMap<>();
        this.trips = new HashMap<>();
    }

    @Override
    public String toString() {
        return "Stop{" +
                "stopId='" + stopId + '\'' +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", routesCount=" + routes.size() +
                ", tripsCount=" + trips.size() +
                ", trips: " + trips.values() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        // If latitude and longitude are the same, we consider the stops to be the same
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Stop stop = (Stop) obj;
        return Double.compare(stop.latitude, latitude) == 0 &&  Double.compare(stop.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() { return stopId != null ? stopId.hashCode() : 0; }

    public String getStopId() { return stopId; }

    public String getName() {
        return name;
    }

    public double getLat() {
        return latitude;
    }

    public double getLon() {
        return longitude;
    }

    public Map<String, Route> getRoutes() { return routes; }

    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    public void addRoute(Route route) {
        routes.put(route.getRouteId(), route);
    }

    public Map<String, Trip> getTrips() { return trips; }

    public Trip getTrip(String tripId) {
        return trips.get(tripId);
    }

    public void addTrip(Trip trip) {
        trips.put(trip.getTripId(), trip);
    }

    public Map<String, Trip> getRelevantTrips(LocalTime time) {
        Map<String, Trip> relevantTrips = new HashMap<>();
        trips.forEach(
                (tripId, trip) -> {
                    if ((trip.getStartTime().isAfter(trip.getEndTime()) && trip.getStartTime().isAfter(time)) || trip.getEndTime().isAfter(time)) {
                        relevantTrips.put(tripId, trip);
                    }
                }
        );
        return relevantTrips;
    }
}
