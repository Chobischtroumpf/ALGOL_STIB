package algo.transit.models.graph;

import algo.transit.models.Stop;

import java.time.LocalTime;
import java.util.Objects;

public class ArrivalKey {
    private final Stop      stop;
    private final LocalTime time;

    public ArrivalKey(Stop stop, LocalTime time) {
        this.stop = stop;
        this.time = time;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ArrivalKey other = (ArrivalKey) obj;
        return this.stop.equals(other.stop) && this.time.equals(other.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stop, time);
    }
}
