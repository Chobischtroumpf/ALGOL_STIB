package algo.transit.models.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(of = "tripId")
public class Trip {
    private final String tripId;
    private final Route route;
    private final ArrayList<Stop> stops = new ArrayList<>();
    private final ArrayList<LocalTime> times = new ArrayList<>();

    public void addStopTime(int stopSequence, @NotNull LocalTime time, @NotNull Stop stop) {
        // Ensure lists are large enough
        while (stops.size() <= stopSequence) {
            stops.add(null);
            times.add(null);
        }

        stops.set(stopSequence, stop);
        times.set(stopSequence, time);

        if (route != null) {
            route.getPossibleStops().add(stop);
            stop.getRoutes().put(route.getRouteId(), route);
            stop.getTrips().put(tripId, this);
        }
    }

    public LocalTime getTimeForStop(@NotNull Stop stop) {
        for (int i = 0; i < stops.size(); i++) {
            if (stop.equals(stops.get(i))) {
                return times.get(i);
            }
        }
        return null;
    }

    public List<Stop> getOrderedStops() {
        List<Stop> result = new ArrayList<>();
        for (Stop stop : stops) {
            if (stop != null) {
                result.add(stop);
            }
        }
        return result;
    }
}