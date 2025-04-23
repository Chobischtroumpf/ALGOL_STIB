package algo.transit.models.graph;

import algo.transit.models.Stop;

import java.time.LocalTime;

public abstract class Edge {
    protected final Stop from;
    protected final Stop to;

    public Edge(Stop from, Stop to) {
        this.from = from;
        this.to   = to;
    }

    public Stop getFrom() { return from; }
    public Stop getTo() { return to; }

    // Abstract methods to be implemented by edge types
    public abstract int getDurationMinutes(LocalTime departureTime);

    public abstract LocalTime getArrivalTime(LocalTime departureTime);
}
