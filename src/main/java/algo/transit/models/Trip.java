package algo.transit.models;

import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Trip {
    private final String    tripId;
    private Route           route;
    private final Map<Integer, Pair<LocalTime, Stop>> stops = new HashMap<>();

    public Trip(String tripId, Route route) {
        this.tripId = tripId;
        this.route  = route;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "tripId='" + tripId + '\'' +
                ", route=" + route +
                ", stops=" + stops.values() +
                '}';
    }

    public String getId() {
        return tripId;
    }

    public Route getRoute() throws NullPointerException {
        if (route == null) throw new NullPointerException("Route is not set");
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Map<Integer, Pair<LocalTime, Stop>> getStops() throws NullPointerException {
        return stops;
    }

    public Pair<LocalTime, Stop> getStopTime(int stopSequence) throws NullPointerException {
        return stops.get(stopSequence);
    }

    public void addStopTime(int stopSequence, Pair<LocalTime, Stop> stop) {
        stops.put(stopSequence, stop);
    }

    public List<Map.Entry<Integer, Pair<LocalTime, Stop>>> getOrderedStopTimes() {
        List<Map.Entry<Integer, Pair<LocalTime, Stop>>> orderedStops = new ArrayList<>(stops.entrySet());

        orderedStops.sort(Map.Entry.comparingByKey());
        return orderedStops;
    }
}
