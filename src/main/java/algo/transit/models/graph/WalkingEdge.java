package algo.transit.models.graph;

import algo.transit.models.Stop;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

public class WalkingEdge extends Edge {
    private final int durationMinutes;

    public WalkingEdge(Stop from, Stop to, int durationMinutes) {
        super(from, to);
        this.durationMinutes = durationMinutes;
    }

    @Override
    public int getDurationMinutes(LocalTime departureTime) { return durationMinutes; }

    @Override
    public LocalTime getArrivalTime(@NotNull LocalTime departureTime) { return departureTime.plusMinutes(durationMinutes); }
}