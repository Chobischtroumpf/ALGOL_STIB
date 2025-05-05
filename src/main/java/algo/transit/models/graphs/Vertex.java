package algo.transit.models.graphs;

import algo.transit.enums.TransportType;

public class Vertex {
    private final Node   fromNode;
    private final Node   toNode;
    private final double distance;
    private final TransportType type;

    // For later
    private Vertex nextInPath;
    private double costFromStart;

    public Vertex(Node fromNode, Node toNode, double distance, TransportType type) {
        this.fromNode   = fromNode;
        this.toNode     = toNode;
        this.distance   = distance;
        this.type       = type;
        this.costFromStart = Double.MAX_VALUE;
    }

    public Node getFromNode() { return fromNode; }

    public Node getToNode() { return toNode; }

    public double getDistance() { return distance; }

    public TransportType getType() { return type; }

    public Vertex getNextInPath() { return nextInPath; }

    public void setNextInPath(Vertex nextInPath) { this.nextInPath = nextInPath; }

    public double getCostFromStart() { return costFromStart; }

    public void setCostFromStart(double costFromStart) { this.costFromStart = costFromStart; }

    @Override
    public String toString() {
        return String.format("Vertex[%s -> %s, type=%s, distance=%.3f]",
                fromNode.getStop().getStopId(),
                toNode.getStop().getStopId(),
                type,
                distance);
    }
}
