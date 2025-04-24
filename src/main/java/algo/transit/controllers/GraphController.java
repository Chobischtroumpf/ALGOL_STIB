package algo.transit.controllers;

import algo.transit.enums.TransportType;
import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import algo.transit.models.graph.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.*;

public class GraphController {
    private final Map<String, Stop>  stops;
    private final Map<String, Trip>  trips;
    private final Map<String, Route> routes;

    private final Map<Stop, List<Edge>> adjacencyList;
    private final SpatialIndex spatialIndex;

    public GraphController(Map<String, Stop> stops, Map<String, Trip> trips, Map<String, Route> routes) {
        this.stops  = stops;
        this.trips  = trips;
        this.routes = routes;
        this.adjacencyList = new HashMap<>();
        this.spatialIndex  = new SpatialIndex();
    }

    public void buildGraph(double walkingSpeed, double maxWalkingTime) {
        indexStops();
        createTransitEdges();
        createWalkingEdges(walkingSpeed, maxWalkingTime);
    }

    private void indexStops() {
        for (Stop stop : stops.values()) {
            spatialIndex.insert(stop);
            adjacencyList.put(stop, new ArrayList<>());
        }
    }

    private void createTransitEdges() {
        // Create direct edges from trip data
        for (Trip trip : trips.values()) {
            Map<Integer, Pair<LocalTime, Stop>> stopTimes = trip.getStops();

            List<Map.Entry<Integer, Pair<LocalTime, Stop>>> sortedStops = new ArrayList<>(stopTimes.entrySet());
            sortedStops.sort(Map.Entry.comparingByKey());

            // Connect consecutive stops in each trip
            for (int i = 0; i < sortedStops.size() - 1; i++) {
                Stop from = sortedStops.get(i).getValue().getRight();
                Stop to = sortedStops.get(i + 1).getValue().getRight();
                LocalTime departureTime = sortedStops.get(i).getValue().getLeft();
                LocalTime arrivalTime = sortedStops.get(i + 1).getValue().getLeft();

                // Create transit edge
                TransitEdge edge = new TransitEdge(
                        from, to,
                        departureTime, arrivalTime,
                        trip.getRoute(),
                        TransportType.valueOf(trip.getRoute().getType().toString())
                );

                adjacencyList.get(from).add(edge);
            }
        }
    }

    private void createWalkingEdges(double walkingSpeed, double maxWalkingTime) {
        // Calculate maximum walking distance in meters
        double maxWalkingDistance = walkingSpeed * maxWalkingTime;

        // For each stop, find nearby stops within walking distance
        for (Stop fromStop : stops.values()) {
            List<Stop> nearbyStops = spatialIndex.findNearbyStops(fromStop, maxWalkingDistance);

            for (Stop toStop : nearbyStops) {
                if (fromStop.equals(toStop)) continue;

                double distance = calculateDistance(fromStop, toStop);
                int walkingTimeMinutes = (int) Math.ceil(distance / walkingSpeed);

                // Create walking edge
                WalkingEdge edge = new WalkingEdge(fromStop, toStop, walkingTimeMinutes);
                adjacencyList.get(fromStop).add(edge);
            }
        }
    }

    // Haversine formula for calculating distance between two points on Earth
    private double calculateDistance(@NotNull Stop stop1, @NotNull Stop stop2) {
        final int EARTH_RADIUS = 6371000; // meters
        double lat1 = Math.toRadians(stop1.getLat());
        double lon1 = Math.toRadians(stop1.getLon());
        double lat2 = Math.toRadians(stop2.getLat());
        double lon2 = Math.toRadians(stop2.getLon());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    public List<PathSegment> findShortestPath(Stop fromStop, Stop toStop, LocalTime departureTime) {
        PriorityQueue<SearchNode> openSet = new PriorityQueue<>();
        Map<ArrivalKey, SearchNode> visited = new HashMap<>();

        SearchNode startNode = new SearchNode(fromStop, departureTime, null, null, 0, estimateTime(fromStop, toStop));
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            SearchNode current = openSet.poll();
            if (current.stop.equals(toStop)) return reconstructPath(current); // Found the destination

            // Skip if we've already found a better path to this stop at this time or earlier
            ArrivalKey key = new ArrivalKey(current.stop, current.arrivalTime);
            if (visited.containsKey(key) && !visited.get(key).equals(current)) continue;

            visited.put(key, current);

            List<Edge> edges = adjacencyList.get(current.stop);
            if (edges == null) continue;

            for (Edge edge : edges) {
                // Skip unavailable edges
                if (edge instanceof TransitEdge transitEdge) {
                    if (!transitEdge.isAvailableAt(current.arrivalTime)) continue;
                }

                LocalTime nextArrivalTime = edge.getArrivalTime(current.arrivalTime);
                int gScore = current.gScore + edge.getDurationMinutes(current.arrivalTime);

                SearchNode nextNode = new SearchNode(
                        edge.getTo(),
                        nextArrivalTime,
                        current,
                        edge,
                        gScore,
                        gScore + estimateTime(edge.getTo(), toStop)
                );

                // Check if we've already visited this stop at this time or earlier
                ArrivalKey nextKey = new ArrivalKey(nextNode.stop, nextNode.arrivalTime);
                if (visited.containsKey(nextKey)) { SearchNode existingNode = visited.get(nextKey);
                    if (nextNode.gScore >= existingNode.gScore) continue; // Skip if we found a better path
                }

                openSet.add(nextNode);
            }
        }

        return List.of(); // No path found
    }

    private int estimateTime(Stop from, Stop to) {
        // Heuristic: straight-line distance divided by average speed
        double distance = calculateDistance(from, to);
        double estimatedMinutes = distance / 667.0;
        return (int) Math.ceil(estimatedMinutes);
    }

    private @NotNull List<PathSegment> reconstructPath(SearchNode goalNode) {
        List<PathSegment> reversedPath = new ArrayList<>();
        SearchNode current = goalNode;

        while (current.parent != null) {
            Edge edge = current.edge;

            // Create path segment
            if (edge instanceof TransitEdge transitEdge) {
                reversedPath.add(new PathSegment(
                        transitEdge.getFrom(),
                        transitEdge.getTo(),
                        transitEdge.getDepartureTime(),
                        current.arrivalTime,
                        transitEdge.getRoute(),
                        transitEdge.getType()
                ));
            } else if (edge instanceof WalkingEdge) {
                // For walking edges
                reversedPath.add(new PathSegment(
                        edge.getFrom(),
                        edge.getTo(),
                        current.parent.arrivalTime,
                        current.arrivalTime,
                        null, // No route for walking
                        TransportType.FOOT
                ));
            }

            current = current.parent;
        }

        // Reverse the path to get it in the correct order
        Collections.reverse(reversedPath);
        return reversedPath;
    }
}
