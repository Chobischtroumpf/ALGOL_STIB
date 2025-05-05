package algo.transit.models;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static algo.transit.controllers.MetaController.EARTH_RAD;

public class Stop {
    private final String stopId;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final Map<String, Route> routes;
    private final Map<String, Trip>  trips;

    public Stop(String stopId, String name, double latitude, double longitude) {
        this.stopId = stopId;
        this.name   = name;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.routes = new HashMap<>();
        this.trips  = new HashMap<>();
    }

    public Stream<Map.Entry<String, Trip>> getTripsAfter(LocalTime time) {
        return trips.entrySet().stream().filter((entry) -> {
            var trip = entry.getValue();
            return trip.getEndTime().isAfter(time) || (trip.getStartTime().isAfter(trip.getEndTime()) && trip.getStartTime().isAfter(time));
        });
    }

    public double[] calculateBoundingBox(double radius) {
        double[] boundingBox = new double[4];
        double lat = this.getLat();
        double lon = this.getLon();

        double dLat = Math.toDegrees(radius / EARTH_RAD);
        double dLon = Math.toDegrees(radius / (EARTH_RAD * Math.cos(Math.toRadians(lat))));

        boundingBox[0] = lat - dLat; // min latitude
        boundingBox[1] = lat + dLat; // max latitude
        boundingBox[2] = lon - dLon; // min longitude
        boundingBox[3] = lon + dLon; // max longitude

        return boundingBox;
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
        return Double.compare(stop.latitude, latitude) == 0 && Double.compare(stop.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() { return stopId != null ? stopId.hashCode() : 0; }

    public String getStopId() { return stopId; }

    public String getName() { return name; }

    public double getLat() { return latitude; }

    public double getLon() { return longitude; }

    public Map<String, Route> getRoutes() { return routes; }

    public Route getRoute(String routeId) { return routes.get(routeId); }

    public void addRoute(Route route) { routes.put(route.getRouteId(), route); }

    public Map<String, Trip> getTrips() { return trips; }

    public Trip getTrip(String tripId) { return trips.get(tripId); }

    public void addTrip(Trip trip) { trips.put(trip.getTripId(), trip); }
}