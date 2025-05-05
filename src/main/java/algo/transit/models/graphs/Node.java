package algo.transit.models.graphs;

import algo.transit.models.Stop;

import java.util.List;

public class Node {
    private Stop stop;
    private List<Node> neighbors;

    public Node(Stop stop) {
        this.stop = stop;
    }

}
