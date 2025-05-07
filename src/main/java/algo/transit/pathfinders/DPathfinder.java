package algo.transit.pathfinders;

import algo.transit.enums.TType;
import algo.transit.models.common.Route;
import algo.transit.models.common.Stop;
import algo.transit.models.common.Trip;
import algo.transit.models.pathfinder.Connection;
import algo.transit.models.pathfinder.TPreference;
import algo.transit.models.pathfinder.Transition;
import algo.transit.models.visualizer.StateRecorder;
import algo.transit.utils.QuadTree;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.*;

import static algo.transit.utils.TimeUtils.isAfter;
import static algo.transit.utils.TimeUtils.isBefore;

public class DPathfinder extends AbstractPathfinder {

    public DPathfinder(Map<String, Stop> stops) {
        super(stops);
    }

    /**
     * Finds the shortest path using Dijkstra's algorithm
     */
    @Override
    public List<Transition> findPath(
            String startStopId,
            String endStopId,
            LocalTime startTime,
            TPreference preferences
    ) {
        recorder = new StateRecorder();
        recorder.setStartAndEndStops(startStopId, endStopId);

        Stop startStop = stops.get(startStopId);
        Stop endStop = stops.get(endStopId);

        if (startStop == null || endStop == null) {
            System.err.println("Start or end stop not found");
            return Collections.emptyList();
        }

        // Calculate max transfers based on journey distance
        int maxTransfers = calculateMaxTransfers(startStop, endStop, preferences.optimizationGoal);

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
            if (iterations % 1000 == 0) System.out.println("Iteration: " + iterations + " " + current);

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
            if (current.transfers > maxTransfers) continue;

            // Get max wait time based on time of day
            double maxWaitTime = calculateMaxWaitTime(current.time);

            // Generate and process all possible transitions from current state
            List<Connection> connections = findPossibleConnections(current, preferences, endStop, maxWaitTime);
            for (Connection connection : connections) {
                double transitionCost = calculateTransitionCost(current.time, connection, current.lastMode, preferences);

                if (transitionCost < 0) continue;

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
            TPreference preferences,
            Stop targetStop,
            double maxWaitTime
    ) {
        List<Connection> connections = new ArrayList<>();
        Stop currentStop = stops.get(current.stopId);

        if (currentStop == null) return connections;
        addTransitConnections(connections, current, currentStop, preferences, targetStop, maxWaitTime);
        addWalkingConnections(connections, current, currentStop, preferences);

        return connections;
    }

    private void addTransitConnections(
            List<Connection> connections,
            @NotNull DijkstraState current,
            @NotNull Stop currentStop,
            @NotNull TPreference preferences,
            Stop targetStop,
            double maxWaitTime
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
            if (!isWorthConsideringTime(currentTime, tripStopTime, maxWaitTime)) continue;

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

            if (currentStopIndex == -1 || currentStopIndex == tripStops.size() - 1)
                continue; // Current stop not found or is the last stop

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
            @NotNull TPreference preferences
    ) {
        if (preferences.forbiddenModes.contains(TType.FOOT)) return;

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
            double distance = QuadTree.calculateDistance(
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
}