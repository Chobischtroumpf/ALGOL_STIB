package algo.transit.models;

import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.*;

public class Trip {
    public final String tripId;
    public final Route route;
    private final ArrayList<Stop> stops = new ArrayList<>();
    private final ArrayList<LocalTime> times = new ArrayList<>();

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

    public void addStopTime(int stopSequence, @NotNull LocalTime time, @NotNull Stop stop) {
        while (stops.size() <= stopSequence) {
            stops.add(null);
            times.add(null);
        }

        stops.set(stopSequence, stop);
        times.set(stopSequence, time);

        if (route != null) {
            route.possibleStops.add(stop);
            stop.routes.put(route.routeId, route);
            stop.trips.put(tripId, this);
        }
    }

    public LocalTime getTimeForStop(@NotNull Stop stop) {
        // Reference equality
        for (int i = 0; i < stops.size(); i++) if (stops.get(i) == stop) return times.get(i);
        // Fall back to equals
        for (int i = 0; i < stops.size(); i++) if (stop.equals(stops.get(i))) return times.get(i);
        return null;
    }

    public List<Stop> getOrderedStops() {
        // Create a list with only non-null stops
        List<Stop> result = new ArrayList<>(stops.size());
        for (Stop stop : stops) if (stop != null) result.add(stop);
        return result;
    }
}