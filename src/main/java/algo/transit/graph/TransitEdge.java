package algo.transit.graph;

import algo.transit.enums.TransportType;
import algo.transit.models.Route;
import algo.transit.models.Stop;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalTime;

public class TransitEdge extends Edge {
    private final LocalTime     departureTime;
    private final LocalTime     arrivalTime;
    private final Route         route;
    private final TransportType type;

    public TransitEdge(Stop from, Stop to, LocalTime departureTime,
                       LocalTime arrivalTime, Route route, TransportType type) {
        super(from, to);
        this.departureTime = departureTime;
        this.arrivalTime   = arrivalTime;
        this.route         = route;
        this.type          = type;
    }

    @Override
    public int getDurationMinutes(LocalTime departureTime) {
        return (int) Duration.between(this.departureTime, arrivalTime).toMinutes();
    }

    @Override
    public LocalTime getArrivalTime(LocalTime departureTime) { return arrivalTime; }

    public LocalTime getDepartureTime() { return departureTime; }

    public Route getRoute() { return route; }

    public TransportType getType() { return type; }

    public boolean isAvailableAt(@NotNull LocalTime time) { return !time.isAfter(departureTime); }
}
