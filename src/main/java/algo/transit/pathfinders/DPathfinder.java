package algo.transit.pathfinders;

import algo.transit.enums.TransportType;
import algo.transit.models.*;
import algo.transit.utils.QuadTree;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.Duration;
import java.util.*;

public class DPathfinder {
    private final Map<String, Stop> stops;
    private final QuadTree stopQuadTree;
    private static final double MAX_LATITUDE = 52.0;
    private static final double MIN_LATITUDE = 49.0;
    private static final double MAX_LONGITUDE = 7.0;
    private static final double MIN_LONGITUDE = 2.0;

    // Recording of algorithm steps
    public PathfindingRecorder recorder;

    public DPathfinder(Map<String, Stop> stops) {
        this.stops = stops;
        this.stopQuadTree = buildQuadTree(stops);
    }

    private @NotNull QuadTree buildQuadTree(@NotNull Map<String, Stop> stops) {
        System.out.println("Building QuadTree for spatial stop indexing...");
        QuadTree tree = new QuadTree(MIN_LONGITUDE, MIN_LATITUDE, MAX_LONGITUDE, MAX_LATITUDE, 0);
        for (Stop stop : stops.values()) tree.insert(stop);
        System.out.println("QuadTree built successfully");
        return tree;
    }

    /**
     * Finds the shortest path using Dijkstra's algorithm
     */
    public List<Transition> findPath(
            String startStopId,
            String endStopId,
            LocalTime startTime,
            TransitPreference preferences
    ) {
        recorder = new PathfindingRecorder();
        recorder.setStartAndEndStops(startStopId, endStopId);

        Stop startStop = stops.get(startStopId);
        Stop endStop = stops.get(endStopId);

        if (startStop == null || endStop == null) {
            System.err.println("Start or end stop not found");
            return Collections.emptyList();
        }

        double directDistance = QuadTree.distance(
                startStop.latitude, startStop.longitude,
                endStop.latitude, endStop.longitude
        );
        System.out.println("Direct distance: " + directDistance + " meters");

        // Initialize Dijkstra algorithm
        PriorityQueue<DijkstraState> priorityQueue = new PriorityQueue<>();
        Map<String, Double> bestCosts = new HashMap<>();

        DijkstraState initialState = new DijkstraState(
                startStopId, startTime, 0, new ArrayList<>(), "NONE", 0
        );
        priorityQueue.add(initialState);
        bestCosts.put(startStopId, 0.0);

        int iterations = 0;

        while (!priorityQueue.isEmpty()) {
            iterations++;
            DijkstraState current = priorityQueue.poll();

            recorder.recordExploredState(current.stopId);

            // Log progress periodically
            if (iterations % 1000 == 0) System.out.println("Iteration " + iterations + ": " + current);

            // If we've reached the destination, return the path
            if (current.stopId.equals(endStopId)) {
                System.out.println("Path found in " + iterations + " iterations");

                recorder.recordFinalPath(current.path);

                return current.path;
            }

            // Skip if we've found a better path to this stop
            Double bestCost = bestCosts.get(current.stopId);
            if (bestCost != null && bestCost < current.cost) continue;

            // Skip if we've reached the maximum number of transfers
            if (current.transfers > preferences.maxTransfers) continue;

            // Generate and process all possible transitions from current state
            List<Connection> connections = findPossibleConnections(current, preferences, endStop);
            for (Connection connection : connections) {
                double transitionCost = calculateTransitionCost(current.time, connection, current.lastMode, preferences);
                DijkstraState successor = getDijkstraState(connection, transitionCost, current);
                Double existingCost = bestCosts.get(connection.toStop());

                // If this is a better path, update and add to queue
                if (existingCost == null || successor.cost < existingCost) {
                    bestCosts.put(connection.toStop(), successor.cost);
                    priorityQueue.add(successor);
                }
            }
        }

        return Collections.emptyList();
    }

    @Contract("_, _, _ -> new")
    private static @NotNull DijkstraState getDijkstraState(
            Connection connection,
            double transitionCost,
            @NotNull DijkstraState current
    ) {
        Transition transition = Transition.fromConnection(connection, transitionCost);

        List<Transition> newPath = new ArrayList<>(current.path);
        newPath.add(transition);

        // Count transfers - different mode = transfer
        int transfers = current.transfers;
        if (!current.lastMode.equals("NONE") && !current.lastMode.equals(connection.mode())) transfers++;

        return new DijkstraState(
                connection.toStop(),
                connection.arrivalTime(),
                current.cost + transitionCost,
                newPath,
                connection.mode(),
                transfers
        );
    }

    /**
     * Class to represent a state in the Dijkstra algorithm
     */
    private record DijkstraState(
            String stopId,
            LocalTime time,
            double cost,
            List<Transition> path,
            String lastMode,
            int transfers
    ) implements Comparable<DijkstraState> {

        @Contract(pure = true)
        @Override
        public int compareTo(@NotNull DijkstraState other) {
            // Pure Dijkstra: compare only on cost
            return Double.compare(this.cost, other.cost);
        }

        @Override
        public @NotNull String toString() {
            return "State{stopId='" + stopId + "', time=" + time +
                    ", cost=" + cost + ", transfers=" + transfers +
                    ", pathLen=" + path.size() + '}';
        }
    }

    private @NotNull List<Connection> findPossibleConnections(
            @NotNull DijkstraState current,
            TransitPreference preferences,
            Stop targetStop
    ) {
        List<Connection> connections = new ArrayList<>();
        Stop currentStop = stops.get(current.stopId);

        if (currentStop == null) return connections;
        addTransitConnections(connections, current, currentStop, preferences, targetStop);
        addWalkingConnections(connections, current, currentStop, preferences);

        return connections;
    }

    private void addTransitConnections(
            List<Connection> connections,
            @NotNull DijkstraState current,
            @NotNull Stop currentStop,
            TransitPreference preferences,
            Stop targetStop
    ) {
        LocalTime currentTime = current.time;

        for (Trip trip : currentStop.trips.values()) {
            Route route = trip.getRoute();
            if (route == null) continue;
            if (preferences.forbiddenModes.contains(route.type)) continue;

            LocalTime tripStopTime = trip.getTimeForStop(currentStop);

            // Skip if the trip's time is null or before current time
            if (tripStopTime == null || isBefore(tripStopTime, currentTime)) continue;

            // Time window pruning - skip connections with excessive wait times
            if (!isWorthConsideringTime(currentTime, tripStopTime)) continue;

            // Get all stops after this one in the trip
            List<Stop> tripStops = trip.getOrderedStops();
            int currentStopIndex = -1;

            // Find current stop index
            for (int i = 0; i < tripStops.size(); i++) {
                if (tripStops.get(i).stopId.equals(currentStop.stopId)) {
                    currentStopIndex = i;
                    break;
                }
            }

            if (currentStopIndex == -1 || currentStopIndex == tripStops.size() - 1) continue; // Current stop not found or is the last stop

            // Add connections to the next few stops (not all of them)
            int maxStopsToConsider = Math.min(tripStops.size(), currentStopIndex + 5);

            for (int i = currentStopIndex + 1; i < maxStopsToConsider; i++) {
                Stop nextStop = tripStops.get(i);
                LocalTime nextStopTime = trip.getTimeForStop(nextStop);

                // Direction-based pruning - skip if moving away from target
                if (!isWorthExploring(currentStop, nextStop, stops.get(targetStop.stopId))) continue;

                if (nextStopTime != null && (isAfter(nextStopTime, tripStopTime) ||
                        (nextStopTime.equals(tripStopTime) && i > currentStopIndex))) {

                    connections.add(new Connection(
                            currentStop.stopId,
                            nextStop.stopId,
                            trip.tripId,
                            route.routeId,
                            route.shortName,
                            tripStopTime,
                            nextStopTime,
                            route.type.toString()
                    ));
                }
            }
        }
    }

    private void addWalkingConnections(
            List<Connection> connections,
            DijkstraState current,
            Stop currentStop,
            @NotNull TransitPreference preferences
    ) {
        if (preferences.forbiddenModes.contains(TransportType.FOOT)) return;

        // Find nearby stops within walking distance
        double maxWalkingDistance = preferences.walkingSpeed * preferences.maxWalkingTime;
        List<Stop> nearbyStops = stopQuadTree.findNearby(
                currentStop.latitude,
                currentStop.longitude,
                maxWalkingDistance
        );

        for (Stop nearbyStop : nearbyStops) {
            if (nearbyStop.stopId.equals(currentStop.stopId)) continue;

            // Calculate walking time
            double distance = QuadTree.distance(
                    currentStop.latitude, currentStop.longitude,
                    nearbyStop.latitude, nearbyStop.longitude
            );

            int walkingTimeMinutes = (int) Math.ceil(distance / preferences.walkingSpeed);

            // Skip if it takes too long to walk
            if (walkingTimeMinutes > preferences.maxWalkingTime) continue;

            connections.add(Connection.createWalkingConnection(
                    currentStop.stopId,
                    nearbyStop.stopId,
                    current.time,
                    walkingTimeMinutes
            ));
        }
    }

    private double calculateTransitionCost(
            LocalTime currentTime,
            @NotNull Connection connection,
            String lastMode,
            @NotNull TransitPreference preferences
    ) {
        // Calculate waiting time
        long waitingMinutes = calculateMinutesBetween(currentTime, connection.departureTime());

        // Calculate transit time
        long transitMinutes = calculateMinutesBetween(connection.departureTime(), connection.arrivalTime());

        // Base cost: transit time + waiting time (with lower weight)
        double cost = transitMinutes + (waitingMinutes * 0.5); // Half penalty for waiting

        // Add mode-specific weights for the transit part
        TransportType mode = TransportType.fromString(connection.mode());
        Double modeWeight = preferences.modeWeights.get(mode);
        if (modeWeight != null) {
            // Only apply weight to the transit time, not the waiting time
            cost = (waitingMinutes * 0.5) + (transitMinutes * modeWeight);
        }

        // Add mode switch penalty
        if (!lastMode.equals("NONE") && !lastMode.equals(connection.mode())) cost += preferences.modeSwitchPenalty;
        return Math.max(0.1, cost); // Ensure positive cost
    }

    private boolean isWorthExploring(
            @NotNull Stop currentStop,
            @NotNull Stop nextStop,
            @NotNull Stop targetStop
    ) {
        // Calculate current and next distances to target
        double currentDistance = QuadTree.distance(
                currentStop.latitude, currentStop.longitude,
                targetStop.latitude, targetStop.longitude
        );

        double nextDistance = QuadTree.distance(
                nextStop.latitude, nextStop.longitude,
                targetStop.latitude, targetStop.longitude
        );

        // Skip if we're moving significantly away from the target
        // Allow some deviation (125%) to account for non-direct routes
        double deviationTolerance = 1.25;

        // For shorter segments, allow more deviation
        if (currentDistance < 5000) deviationTolerance = 2.0; // Allow more deviation for local transit

        // Calculate direct distance between stops
        double segmentDistance = QuadTree.distance(
                currentStop.latitude, currentStop.longitude,
                nextStop.latitude, nextStop.longitude
        );

        // If this is a very short segment, always allow it
        if (segmentDistance < 1000) return true;

        // Check if overall direction is reasonable
        return nextDistance < currentDistance * deviationTolerance;
    }

    private boolean isWorthConsideringTime(LocalTime currentTime, LocalTime departureTime) {
        long waitingMinutes = calculateMinutesBetween(currentTime, departureTime);

        // Case 1: Very short wait (always consider)
        if (waitingMinutes <= 30) return true;

        // Case 2: Reasonable wait time during same day
        if (waitingMinutes <= 180) return true; // 3 hours max regular wait

        // Case 3: Overnight case - first departure next morning
        if (currentTime.getHour() >= 20 && departureTime.getHour() <= 10) {
            // For end-of-day to morning trips, allow longer waiting times
            return waitingMinutes <= 12 * 60; // Up to 12 hours for overnight
        }

        // Case 4: Day transition without being overnight case
        // For other cases, limit to 4 hours wait
        return waitingMinutes <= 240;
    }

    private long calculateMinutesBetween(@NotNull LocalTime start, @NotNull LocalTime end) {
        int startHour = start.getHour();
        int endHour = end.getHour();

        // Adjust for GTFS times > 24 hours
        if (startHour >= 24) startHour %= 24;
        if (endHour >= 24) endHour %= 24;

        LocalTime adjustedStart = LocalTime.of(startHour, start.getMinute(), start.getSecond());
        LocalTime adjustedEnd = LocalTime.of(endHour, end.getMinute(), end.getSecond());

        // If end appears before start, assume it's the next day
        if (adjustedEnd.isBefore(adjustedStart)) adjustedEnd = adjustedEnd.plusHours(24);

        // Calculate duration
        Duration duration = Duration.between(adjustedStart, adjustedEnd);
        return duration.toMinutes();
    }

    private boolean isBefore(@NotNull LocalTime time1, @NotNull LocalTime time2) {
        // Normalize hours to 0-23 range
        int hour1 = time1.getHour() % 24;
        int hour2 = time2.getHour() % 24;

        // If hours are equal, compare minutes
        if (hour1 == hour2) return time1.getMinute() < time2.getMinute();
        return hour1 < hour2;
    }

    private boolean isAfter(@NotNull LocalTime time1, LocalTime time2) {
        return !time1.equals(time2) && !isBefore(time1, time2);
    }

    public void printPath(@NotNull List<Transition> path) {
        if (path.isEmpty()) {
            System.out.println("No path found.");
            return;
        }

        System.out.println("\nOptimal route:");
        System.out.println("======================");

        Transition firstTransition = path.getFirst();
        Transition lastTransition = path.getLast();

        LocalTime startTime = firstTransition.departure();
        LocalTime endTime = lastTransition.arrival();

        long totalMinutes = calculateMinutesBetween(startTime, endTime);

        System.out.println("From: " + stops.get(firstTransition.fromStop()).name);
        System.out.println("To: " + stops.get(lastTransition.toStop()).name);
        System.out.println("Departure: " + startTime);
        System.out.println("Arrival: " + endTime);
        System.out.println("Total travel time: " + totalMinutes + " minutes");
        System.out.println("======================");

        // Print detailed path
        int i = 0;
        while (i < path.size()) {
            Transition current = path.get(i);
            String currentMode = current.mode();
            String currentRoute = current.route();

            // Find the last consecutive transition with the same mode and route
            int j = i;
            while (j + 1 < path.size() &&
                    path.get(j + 1).mode().equals(currentMode) &&
                    path.get(j + 1).route().equals(currentRoute) &&
                    !currentMode.equals("FOOT")) {
                j++;
            }

            // Print as a single segment if multiple transitions were found
            Stop fromStop = stops.get(current.fromStop());
            Stop toStop = stops.get(path.get(j).toStop());

            System.out.println((i + 1) + ". " + currentMode +
                    (currentRoute.isEmpty() ? "" : " " + currentRoute) +
                    " from " + fromStop.name +
                    " (" + current.departure() + ") to " +
                    toStop.name + " (" + path.get(j).arrival() + ")");

            // Move index to next segment
            i = j + 1;
        }

        System.out.println("======================");
    }
}