package algo.transit.models.graphs;

import algo.transit.models.Stop;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private Stop stop;
    private List<Vertex> outgoingVertices;
    private List<Vertex> incomingVertices;

    public Node(Stop stop) {
        this.stop = stop;
        this.outgoingVertices = new ArrayList<>();
        this.incomingVertices = new ArrayList<>();
    }

    public void addOutgoingVertex(Vertex vertex) { outgoingVertices.add(vertex); }

    public void addIncomingVertex(Vertex vertex) { incomingVertices.add(vertex); }

    public List<Vertex> getOutgoingVertices() {return outgoingVertices; }

    public List<Vertex> getIncomingVertices() { return incomingVertices; }

    public Stop getStop() { return stop; }

    public int getNeighborCount() { return outgoingVertices.size(); }
}
