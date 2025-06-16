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

    @Contract("_, _, _ -> new")
    private static @NotNull DijkstraState getDijkstraState(
            @NotNull Connection connection,
            double transitionCost,
            @NotNull DijkstraState current
    ) {
        int newDayOffset = current.dayOffset;
        if (connection.arrivalTime().isBefore(connection.departureTime())) newDayOffset++;

        Transition transition = Transition.fromConnection(
                connection,
                transitionCost,
                current.dayOffset
        );

        List<Transition> newPath = new ArrayList<>(current.path);
        newPath.add(transition);

        // Count transfers - different mode = transfer
        int transfers = current.transfers;
        if (!current.lastMode.equals("NONE") && !current.lastMode.equals(connection.mode())) {
            transfers++;
        }

        return new DijkstraState(
                connection.toStop(),
                connection.arrivalTime(),
                newDayOffset,
                current.cost + transitionCost,
                newPath,
                connection.mode(),
                transfers
        );
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

        // Initialize Dijkstra algorithm
        PriorityQueue<DijkstraState> priorityQueue = new PriorityQueue<>();
        Map<String, Double> bestCosts = new HashMap<>();

        DijkstraState initialState = new DijkstraState(
                startStopId, startTime, 0, 0.0, new ArrayList<>(), "NONE", 0
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

            // Generate and process all possible transitions from current state
            List<Connection> connections = findPossibleConnections(current, preferences, endStop);
            for (Connection connection : connections) {
                double transitionCost = calculateTransitionCost(
                        current.time,
                        connection,
                        current.lastMode,
                        preferences
                );

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

    private @NotNull List<Connection> findPossibleConnections(
            @NotNull DijkstraState current,
            TPreference preferences,
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
            @NotNull TPreference preferences,
            Stop targetStop
    ) {
        LocalTime currentTime = current.time;

        for (Trip trip : currentStop.getTrips().values()) {
            Route route = trip.getRoute();
            if (route == null) continue;
            if (preferences.getForbiddenModes().contains(route.getType())) continue;

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
                if (tripStops.get(i).getStopId().equals(currentStop.getStopId())) {
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
                if (!isWorthExploring(currentStop, nextStop, stops.get(targetStop.getStopId()))) continue;

                if (nextStopTime != null && (isAfter(nextStopTime, tripStopTime) ||
                        (nextStopTime.equals(tripStopTime) && i > currentStopIndex))) {

                    connections.add(new Connection(
                            currentStop.getStopId(),
                            nextStop.getStopId(),
                            trip.getTripId(),
                            route.getRouteId(),
                            route.getShortName(),
                            tripStopTime,
                            nextStopTime,
                            route.getType().toString()
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
        if (preferences.getForbiddenModes().contains(TType.FOOT)) return;

        // Find nearby stops within walking distance
        double maxWalkingDistance = preferences.getWalkingSpeed() * preferences.getMaxWalkingTime();
        List<Stop> nearbyStops = stopQuadTree.findNearby(
                currentStop.getLatitude(),
                currentStop.getLongitude(),
                maxWalkingDistance
        );

        for (Stop nearbyStop : nearbyStops) {
            // Skip if from and to are the same stop or refer to same physical location
            if (nearbyStop.getStopId().equals(currentStop.getStopId()) || nearbyStop.getName().equals(currentStop.getName())) continue;

            // Calculate walking time
            double distance = QuadTree.calculateDistance(
                    currentStop.getLatitude(), currentStop.getLongitude(),
                    nearbyStop.getLatitude(), nearbyStop.getLongitude()
            );

            int walkingTimeMinutes = (int) Math.ceil(distance / preferences.getWalkingSpeed());

            // Skip if it takes too long to walk
            if (walkingTimeMinutes > preferences.getMaxWalkingTime()) continue;

            connections.add(Connection.createWalkingConnection(
                    currentStop.getStopId(),
                    nearbyStop.getStopId(),
                    current.time,
                    walkingTimeMinutes
            ));
        }
    }

    /**
     * Class to represent a state in the Dijkstra algorithm
     */
    private record DijkstraState(
            String stopId,
            LocalTime time,
            int dayOffset,
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
}