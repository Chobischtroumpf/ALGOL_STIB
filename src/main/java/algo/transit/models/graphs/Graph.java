package algo.transit.models.graphs;

import algo.transit.models.Stop;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public class Graph {
    private final Map<String, Node> stops;

    public Graph(@NotNull Map<String, Stop> stops) {
        this.stops = stops.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new Node(entry.getValue())));
    }

    private void constructGraph() {
    
    }
}
