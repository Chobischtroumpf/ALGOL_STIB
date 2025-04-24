package algo.transit.models.graph;

import algo.transit.models.Stop;

import java.time.LocalTime;
import java.util.Objects;

public class SearchNode implements Comparable<SearchNode> {
    public final Stop          stop;
    public final LocalTime     arrivalTime;
    public final SearchNode    parent;
    public final Edge  edge;
    public final int   gScore; // Cost from start to this node
    private final int   fScore; // Estimated total cost (g + h)

    public SearchNode(Stop stop, LocalTime arrivalTime, SearchNode parent, Edge edge, int gScore, int fScore) {
        this.stop           = stop;
        this.arrivalTime    = arrivalTime;
        this.parent         = parent;
        this.edge   = edge;
        this.gScore = gScore;
        this.fScore = fScore;
    }

    @Override
    public int compareTo(SearchNode other) {
        // Compare based on f-score (g + h)
        return Integer.compare(this.fScore, other.fScore);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SearchNode other = (SearchNode) obj;
        return this.stop.equals(other.stop) && this.arrivalTime.equals(other.arrivalTime);
    }

    @Override
    public int hashCode() { return Objects.hash(stop, arrivalTime); }
}
