package algo.transit.models.graphs;

import algo.transit.enums.TransportType;
import algo.transit.models.Stop;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Graph {
    private final Map<String, Node> nodes;
    private final List<Vertex>      vertices;

    public Graph(@NotNull Map<String, Stop> stops) {
        this.nodes = stops.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new Node(entry.getValue())));
        this.vertices = new ArrayList<>();
    }

    public Map<String, Node> getNodes() { return nodes; }

    public Node getNode(String stopId) { return nodes.get(stopId); }

    public List<Vertex> getVertices() { return vertices; }
}
