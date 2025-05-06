package algo.transit.pathfinding;

import algo.transit.models.*;
import algo.transit.utils.QuadTree;
import algo.transit.enums.TransportType;

import java.time.LocalTime;
import java.util.*;

public class TransitGraph {
    public final Map<String, Stop> stops;
    public final Map<String, List<Connection>> connectionsByDeparture;
    public QuadTree spatialIndex;
    public final Map<String, String> routeTypes;
    public final Map<String, Double> averageModeSpeeds;
    public final Map<String, List<WalkableStop>> walkableStopsCache;
    public double minLat, maxLat, minLon, maxLon;
    public Map<String, Set<String>> stopGroups = new HashMap<>();
    public static final double SAME_STOP_THRESHOLD = 50.0;

    public record WalkableStop(String stopId, double distanceMeters, int walkTimeMinutes) { }

    public TransitGraph(Map<String, Stop> stops) {
        this.stops = stops;
        this.connectionsByDeparture = new HashMap<>();
        this.routeTypes = new HashMap<>();
        this.averageModeSpeeds = new HashMap<>();
        this.walkableStopsCache = new HashMap<>();
        calculateBounds();
        initializeSpatialIndex();
    }

    private void calculateBounds() {
        if (stops.isEmpty()) {
            minLat = maxLat = 50.0;
            minLon = maxLon = 4.0;
            return;
        }

        Stop firstStop = stops.values().iterator().next();
        minLat = maxLat = firstStop.latitude;
        minLon = maxLon = firstStop.longitude;

        for (Stop stop : stops.values()) {
            minLat = Math.min(minLat, stop.latitude);
            maxLat = Math.max(maxLat, stop.latitude);
            minLon = Math.min(minLon, stop.longitude);
            maxLon = Math.max(maxLon, stop.longitude);
        }

        double latPadding = (maxLat - minLat) * 0.1;
        double lonPadding = (maxLon - minLon) * 0.1;
        minLat -= latPadding;
        maxLat += latPadding;
        minLon -= lonPadding;
        maxLon += lonPadding;
    }

    private void initializeSpatialIndex() {
        spatialIndex = new QuadTree(minLon, minLat, maxLon, maxLat, 0);
        for (Stop stop : stops.values()) {
            spatialIndex.insert(stop);
        }
    }

    public void addRouteType(String routeId, String routeType) {
        routeTypes.put(routeId, routeType);
    }

    public void addConnection(Connection connection) {
        String fromStop = connection.fromStop;
        connectionsByDeparture
                .computeIfAbsent(fromStop, _ -> new ArrayList<>())
                .add(connection);
    }

    public List<Connection> getOutgoingConnections(String stopId, LocalTime currentTime,
                                                   TransitPreference preferences) {
        List<Connection> connections = new ArrayList<>();
        Stop currentStop = stops.get(stopId);

        if (currentStop == null) {
            return connections;
        }

        // Add direct transit connections from schedule data
        // We'll search for the next departures from this stop across all routes
        Map<String, Trip> relevantTrips = new HashMap<>();

        // First, get all the trips that serve this stop
        if (currentStop.trips != null) {
            for (Trip trip : currentStop.trips.values()) {
                LocalTime departureTimeForStop = trip.getTimeForStop(currentStop);

                // Only consider trips that depart after the current time
                // and within a reasonable window (4 hours)
                if (departureTimeForStop != null &&
                        !departureTimeForStop.isBefore(currentTime) &&
                        departureTimeForStop.isBefore(currentTime.plusHours(4))) {

                    relevantTrips.put(trip.tripId, trip);
                }
            }
        }

        // Now create connections from these trips
        for (Trip trip : relevantTrips.values()) {
            Route route = trip.getRoute();
            if (route == null) continue;

            // Skip forbidden modes
            if (preferences.forbiddenModes.contains(route.type)) continue;

            List<Stop> orderedStops = trip.getOrderedStops();
            int currentStopIndex = -1;

            // Find index of current stop in this trip
            for (int i = 0; i < orderedStops.size(); i++) {
                if (orderedStops.get(i).stopId.equals(stopId)) {
                    currentStopIndex = i;
                    break;
                }
            }

            // If stop found, create connections to subsequent stops
            if (currentStopIndex >= 0 && currentStopIndex < orderedStops.size() - 1) {
                LocalTime departureTime = trip.getTimeForStop(currentStop);

                // Create connections to all subsequent stops
                for (int i = currentStopIndex + 1; i < orderedStops.size(); i++) {
                    Stop nextStop = orderedStops.get(i);
                    LocalTime arrivalTime = trip.getTimeForStop(nextStop);

                    if (departureTime != null && arrivalTime != null) {
                        Connection conn = new Connection(
                                stopId,
                                nextStop.stopId,
                                trip.tripId,
                                route.routeId,
                                route.shortName,
                                departureTime,
                                arrivalTime,
                                route.type.toString()
                        );

                        connections.add(conn);
                    }
                }
            }
        }

        // Add walking connections
        if (!preferences.forbiddenModes.contains(TransportType.FOOT)) {
            Set<String> walkableStopIds = new HashSet<>();
            getWalkableStops(stopId, preferences).stream()
                    .filter(walkable -> {
                        // Avoid too many walking connections to similar destinations
                        Stop stop = stops.get(walkable.stopId);
                        if (stop == null) return false;

                        // Prioritize walking to stops with good transit connections
                        boolean hasGoodTransit = stop.routes.values().stream()
                                .anyMatch(r -> r.type == TransportType.TRAIN || r.type == TransportType.METRO);

                        return hasGoodTransit || walkableStopIds.add(walkable.stopId);
                    })
                    .limit(5)  // Limit walking connections
                    .map(walkable -> Connection.createWalkingConnection(
                            stopId, walkable.stopId, currentTime, walkable.walkTimeMinutes))
                    .forEach(connections::add);
        }

        return connections;
    }

    public List<WalkableStop> getWalkableStops(String stopId, TransitPreference preferences) {
        // Check cache
        String cacheKey = stopId + "-" + preferences.maxWalkingTime + "-" + preferences.walkingSpeed;
        if (walkableStopsCache.containsKey(cacheKey)) return walkableStopsCache.get(cacheKey);

        // Initialize stop groups if needed
        if (stopGroups.isEmpty()) initializeStopGroups();

        List<WalkableStop> walkableStops = new ArrayList<>();
        Stop fromStop = stops.get(stopId);

        if (fromStop == null) return walkableStops;

        // Calculate max walking distance
        double maxWalkDistance = preferences.walkingSpeed * preferences.maxWalkingTime;
        double maxWalkDistanceSquared = maxWalkDistance * maxWalkDistance;

        // Find stops within walking distance using spatial index
        List<Stop> nearbyStops = spatialIndex.findNearby(
                fromStop.latitude,
                fromStop.longitude,
                maxWalkDistance
        );

        // Calculate walk times using distance squared for efficient comparison
        Map<String, Double> distanceSquaredMap = new HashMap<>();

        for (Stop toStop : nearbyStops) {
            // Skip self or stops in the same group
            if (toStop.stopId.equals(stopId) || areInSameStopGroup(stopId, toStop.stopId)) continue;

            // Calculate distance squared (more efficient than actual distance)
            double distanceSquared = QuadTree.distanceSquared(
                    fromStop.latitude, fromStop.longitude,
                    toStop.latitude, toStop.longitude
            );

            if (distanceSquared <= maxWalkDistanceSquared) {
                // Only compute actual distance when needed for time calculation
                double distance = Math.sqrt(distanceSquared);
                int walkTimeMinutes = (int) Math.ceil(distance / preferences.walkingSpeed);
                walkableStops.add(new WalkableStop(toStop.stopId, distance, walkTimeMinutes));
                distanceSquaredMap.put(toStop.stopId, distanceSquared);
            }
        }

        // Sort by distance squared (most efficient)
        walkableStops.sort(Comparator.comparingDouble(w -> distanceSquaredMap.getOrDefault(w.stopId, Double.MAX_VALUE)));

        // Limit to the 5 closest walkable stops for efficiency
        if (walkableStops.size() > 5) walkableStops = walkableStops.subList(0, 5);

        // Cache the result
        walkableStopsCache.put(cacheKey, walkableStops);

        return walkableStops;
    }

    private void initializeStopGroups() {
        for (Stop stop : stops.values()) {
            // Skip if already in a group
            if (stopGroups.containsKey(stop.stopId)) continue;

            // Create a new group for this stop
            Set<String> group = new HashSet<>();
            group.add(stop.stopId);
            stopGroups.put(stop.stopId, group);

            // Use spatial index to find nearby stops efficiently
            List<Stop> nearbyStops = spatialIndex.findNearby(stop.latitude, stop.longitude, SAME_STOP_THRESHOLD);

            // Add nearby stops to the group
            for (Stop nearbyStop : nearbyStops) {
                if (!stop.stopId.equals(nearbyStop.stopId)) {
                    group.add(nearbyStop.stopId);
                    stopGroups.put(nearbyStop.stopId, group);
                }
            }
        }
    }

    private boolean areInSameStopGroup(String stopId1, String stopId2) {
        if (!stopGroups.containsKey(stopId1) || !stopGroups.containsKey(stopId2)) return false;
        return stopGroups.get(stopId1) == stopGroups.get(stopId2);
    }

    public Stop getStop(String stopId) {
        return stops.get(stopId);
    }

    public Map<String, Stop> getStops() {
        return stops;
    }
}