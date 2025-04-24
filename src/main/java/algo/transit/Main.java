package algo.transit;

import algo.transit.controllers.MetaController;
import algo.transit.models.graph.PathSegment;

import java.time.LocalTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        double walkingSpeed   = 80.0; // default meters per minute
        double maxWalkingTime = 30.0; // default walking time

        if (args.length >= 2) {
            try {
                walkingSpeed = Double.parseDouble(args[0]);
                maxWalkingTime = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid arguments. Using defaults.");
            }
        }

        MetaController metaController = new MetaController(walkingSpeed, maxWalkingTime);
        System.out.println("Transit system initialized successfully.");

        // Example 1: Find shortest path by time
        System.out.println("Example 1: Shortest path by time");
        System.out.println("------------------------------");

        String fromStopId = "DELIJN-105277"; // Schoten Grote Singel
        String toStopId = "STIB-0089";       // MONTGOMERY
        LocalTime departureTime = LocalTime.of(10, 0); // 10:00 AM

        List<PathSegment> path = metaController.findShortestPath(fromStopId, toStopId, departureTime);
        printPath(path);
    }

    private static void printPath(List<PathSegment> path) {
        if (path == null || path.isEmpty()) {
            System.out.println("No path found.");
            return;
        }

        System.out.println("Path found:");
        for (PathSegment segment : path) {
            System.out.println(segment);
        }
    }
}
