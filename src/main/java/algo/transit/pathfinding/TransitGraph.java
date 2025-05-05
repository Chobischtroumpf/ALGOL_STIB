package algo.transit.pathfinding;

import algo.transit.models.Connection;
import algo.transit.models.Stop;
import algo.transit.models.TransitPreference;
import algo.transit.utils.QuadTree;
import algo.transit.enums.TransportType;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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
    public static final double SAME_STOP_THRESHOLD = 30.0; // 30 meters

    public static class WalkableStop {
        public final String stopId;
        public final double distanceMeters;
        public final int walkTimeMinutes;

        public WalkableStop(String stopId, double distanceMeters, int walkTimeMinutes) {
            this.stopId = stopId;
            this.distanceMeters = distanceMeters;
            this.walkTimeMinutes = walkTimeMinutes;
        }
    }

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
                .computeIfAbsent(fromStop, k -> new ArrayList<>())
                .add(connection);
    }

    public List<Connection> getOutgoingConnections(String stopId, LocalTime currentTime,
                                                   TransitPreference preferences) {
        List<Connection> connections = new ArrayList<>();

        // Add direct transit connections - limit to next 2 hours only
        if (connectionsByDeparture.containsKey(stopId)) {
            LocalTime maxTime = currentTime.plusHours(2);
            connectionsByDeparture.get(stopId).stream()
                    .filter(conn -> !conn.departureTime.isBefore(currentTime) &&
                            conn.departureTime.isBefore(maxTime))
                    .sorted(Comparator.comparing(c -> c.departureTime))
                    .limit(20) // Only take the next 20 departures
                    .forEach(connections::add);
        }

        // Add walking connections (limit to 3 stops)
        if (!preferences.forbiddenModes.contains(TransportType.FOOT)) {
            getWalkableStops(stopId, preferences).stream()
                    .limit(3)  // Only consider 3 closest walkable stops
                    .map(walkable -> Connection.createWalkingConnection(
                            stopId, walkable.stopId, currentTime, walkable.walkTimeMinutes))
                    .forEach(connections::add);
        }

        return connections;
    }

    public List<WalkableStop> getWalkableStops(String stopId, TransitPreference preferences) {
        // Check cache
        String cacheKey = stopId + "-" + preferences.maxWalkingTime + "-" + preferences.walkingSpeed;
        if (walkableStopsCache.containsKey(cacheKey)) {
            return walkableStopsCache.get(cacheKey);
        }

        // Initialize stop groups if needed
        if (stopGroups.isEmpty()) {
            initializeStopGroups();
        }

        List<WalkableStop> walkableStops = new ArrayList<>();
        Stop fromStop = stops.get(stopId);

        if (fromStop == null) {
            return walkableStops;
        }

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
            if (toStop.stopId.equals(stopId) || areInSameStopGroup(stopId, toStop.stopId)) {
                continue;
            }

            // Calculate distance squared (more efficient than actual distance)
            double distanceSquared = distanceSquared(
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
        if (walkableStops.size() > 5) {
            walkableStops = walkableStops.subList(0, 5);
        }

        // Cache the result
        walkableStopsCache.put(cacheKey, walkableStops);

        return walkableStops;
    }

    private void initializeStopGroups() {
        for (Stop stop : stops.values()) {
            // Skip if already in a group
            if (stopGroups.containsKey(stop.stopId)) {
                continue;
            }

            // Create a new group for this stop
            Set<String> group = new HashSet<>();
            group.add(stop.stopId);
            stopGroups.put(stop.stopId, group);

            // Use spatial index to find nearby stops efficiently
            List<Stop> nearbyStops = spatialIndex.findNearby(
                    stop.latitude, stop.longitude, SAME_STOP_THRESHOLD);

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
        if (!stopGroups.containsKey(stopId1) || !stopGroups.containsKey(stopId2)) {
            return false;
        }
        return stopGroups.get(stopId1) == stopGroups.get(stopId2);
    }

    private static double distanceSquared(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;
        return distance * distance;
    }

    public void calculateAverageModeSpeeds() {
        Map<String, List<Double>> speedSamples = new HashMap<>();

        // Initialize collection for each mode
        for (TransportType mode : TransportType.values()) {
            speedSamples.put(mode.toString(), new ArrayList<>());
        }

        // Process all connections to calculate speed samples
        for (List<Connection> connections : connectionsByDeparture.values()) {
            for (Connection conn : connections) {
                String mode = conn.mode;

                // Skip walking, we calculate that differently
                if (mode.equals("FOOT")) continue;

                Stop fromStop = stops.get(conn.fromStop);
                Stop toStop = stops.get(conn.toStop);

                if (fromStop != null && toStop != null) {
                    // Calculate distance in meters
                    double distance = QuadTree.distance(
                            fromStop.latitude, fromStop.longitude,
                            toStop.latitude, toStop.longitude
                    );

                    // Calculate time in minutes
                    LocalTime departure = conn.departureTime;
                    LocalTime arrival = conn.arrivalTime;

                    // Handle day wrapping
                    long minutes = departure.until(arrival, ChronoUnit.MINUTES);
                    if (minutes < 0) minutes += 24 * 60; // Add a day

                    // Avoid division by zero
                    if (minutes > 0) {
                        double speed = distance / minutes; // meters per minute
                        speedSamples.get(mode).add(speed);
                    }
                }
            }
        }

        // Calculate average speeds
        for (Map.Entry<String, List<Double>> entry : speedSamples.entrySet()) {
            List<Double> samples = entry.getValue();

            if (!samples.isEmpty()) {
                // Calculate average
                double sum = 0;
                for (Double speed : samples) {
                    sum += speed;
                }
                double avg = sum / samples.size();
                averageModeSpeeds.put(entry.getKey(), avg);
            }
        }

        // Set default values if no samples available
        if (!averageModeSpeeds.containsKey("METRO")) averageModeSpeeds.put("METRO", 120.0);
        if (!averageModeSpeeds.containsKey("TRAM")) averageModeSpeeds.put("TRAM", 100.0);
        if (!averageModeSpeeds.containsKey("BUS")) averageModeSpeeds.put("BUS", 80.0);
        if (!averageModeSpeeds.containsKey("TRAIN")) averageModeSpeeds.put("TRAIN", 166.0);
    }

    public double getModeSpeed(String mode) {
        if (averageModeSpeeds.isEmpty()) {
            calculateAverageModeSpeeds();
        }

        if (mode.equals("FOOT")) {
            return 80.0; // Default walking speed
        }

        return averageModeSpeeds.getOrDefault(mode, 80.0);
    }

    public Stop getStop(String stopId) {
        return stops.get(stopId);
    }

    public Map<String, Stop> getStops() {
        return stops;
    }
}