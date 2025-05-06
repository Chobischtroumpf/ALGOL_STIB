package algo.transit.models;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Trip {
    public final String tripId;
    public final Route route;
    private final TreeMap<Integer, Pair<LocalTime, Stop>> stopsBySequence = new TreeMap<>();
    private final Map<String, Pair<Integer, LocalTime>> stopInfoByStopId = new HashMap<>();
    private List<Stop> orderedStops = new ArrayList<>();

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

    public Route getRoute() {
        if (route == null) throw new NullPointerException("Route is not set");
        return route;
    }

    public void addStopTime(int stopSequence, Pair<LocalTime, Stop> stopData) {
        stopsBySequence.put(stopSequence, stopData);
        stopInfoByStopId.put(stopData.getRight().stopId, Pair.of(stopSequence, stopData.getLeft()));

        // Maintain ordered stops list
        if (orderedStops.size() <= stopSequence) while (orderedStops.size() <= stopSequence) orderedStops.add(null);
        orderedStops.set(stopSequence, stopData.getRight());
    }

    public LocalTime getTimeForStop(@NotNull Stop stop) {
        Pair<Integer, LocalTime> info = stopInfoByStopId.get(stop.stopId); // O(1) lookup instead of O(n) iteration
        return info != null ? info.getRight() : null;
    }

    public List<Stop> getOrderedStops() {
        return orderedStops.stream().filter(Objects::nonNull).collect(Collectors.toList()); // No need to rebuild each time, already maintained
    }

    public Map<Integer, Pair<LocalTime, Stop>> getStops() {
        return stopsBySequence;
    }
}