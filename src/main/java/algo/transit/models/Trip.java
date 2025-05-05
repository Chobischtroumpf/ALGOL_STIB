package algo.transit.models;

import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalTime;
import java.util.*;

public class Trip {
    public final String tripId;
    public final Route route;
    public final TreeMap<Integer, Pair<LocalTime, Stop>> stops = new TreeMap<>();

    public Trip(String tripId, Route route) {
        this.tripId = tripId;
        this.route = route;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return tripId.equals(((Trip) obj).tripId);
    }

    @Override
    public int hashCode() {
        return tripId != null ? tripId.hashCode() : 0;
    }

    public String getTripId() {
        return tripId;
    }

    public Route getRoute() {
        if (route == null) throw new NullPointerException("Route is not set");
        return route;
    }

    public Map<Integer, Pair<LocalTime, Stop>> getStops() {
        return stops;
    }

    public void addStopTime(int stopSequence, Pair<LocalTime, Stop> stop) {
        stops.put(stopSequence, stop);
    }

    public LocalTime getTimeForStop(Stop stop) {
        for (Pair<LocalTime, Stop> pair : stops.values())
            if (pair.getRight().equals(stop)) return pair.getLeft();
        return null;
    }

    public LocalTime getEndTime() {
        return stops.get(stops.lastKey()).getLeft();
    }

    public LocalTime getStartTime() {
        return stops.get(stops.firstKey()).getLeft();
    }

    public boolean containsStop(Stop stop) {
        return stops.values().stream()
                .map(Pair::getRight)
                .anyMatch(s -> s.equals(stop));
    }

    public List<Stop> getOrderedStops() {
        return stops.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().getRight())
                .toList();
    }
}