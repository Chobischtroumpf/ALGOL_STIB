package algo.transit.controllers;

import algo.transit.enums.TransportType;
import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import algo.transit.models.graph.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // TODO: Implement A* search to find the shortest path
        return List.of();
    }
}
