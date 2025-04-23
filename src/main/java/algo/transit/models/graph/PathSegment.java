package algo.transit.models.graph;

import algo.transit.enums.TransportType;
import algo.transit.models.Route;
import algo.transit.models.Stop;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

/**
 * @param route null for walking
 */
public record PathSegment(Stop from, Stop to, LocalTime departureTime, LocalTime arrivalTime, Route route,
                          TransportType type) {

    @Override
    public @NotNull String toString() {
        String transportInfo = type == TransportType.FOOT ?
                "Walk" :
                "Take " + route.getType() + " " + route.getShortName();

        return transportInfo +
                " from " + from.getName() + " (" + departureTime + ")" +
                " to " + to.getName() + " (" + arrivalTime + ")";
    }
}
