package algo.transit.pathfinding;

import algo.transit.models.Connection;
import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Transition;
import algo.transit.models.TransitPreference;
import algo.transit.services.CSVService;
import algo.transit.utils.QuadTree;
import algo.transit.enums.TransportType;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TransitPathfinder {
    public final TransitGraph graph;
    public int maxIterations = 50000000;
    public int maxFrontierSize = 100000000;
    public static final int MAX_REASONABLE_DURATION = 600; // 10 hours
    public static final int MAX_REASONABLE_WAIT = 60; // 1 hour
    public static final int MAX_FRONTIER_AFTER_PRUNING = 100000;
    public static final int FRONTIER_PRUNING_THRESHOLD = 200000;

    public TransitPathfinder(CSVService csvService) {
        this.graph = loadGraphData(csvService);
    }

    private TransitGraph loadGraphData(CSVService csvService) {
        // Load data
        Map<String, Stop> stops = csvService.getStops();
        Map<String, Route> routes = csvService.getRoutes();
        Map<String, algo.transit.models.Trip> trips = csvService.getTrips(routes);

        // Link data
        csvService.linkData(stops, trips);

        // Create graph
        TransitGraph transitGraph = new TransitGraph(stops);

        // Add route types
        for (Route route : routes.values()) {
            transitGraph.addRouteType(route.getRouteId(), route.getType().toString());
        }

        // Create connections
        createConnectionsFromTrips(transitGraph, trips);

        // Calculate speeds
        transitGraph.calculateAverageModeSpeeds();

        return transitGraph;
    }

    private void createConnectionsFromTrips(TransitGraph graph, Map<String, algo.transit.models.Trip> trips) {
        for (algo.transit.models.Trip trip : trips.values()) {
            Route route = trip.getRoute();
            Map<Integer, org.apache.commons.lang3.tuple.Pair<LocalTime, Stop>> stopTimes = trip.getStops();

            if (stopTimes.size() < 2 || route == null) continue;

            // Create connections between consecutive stops
            List<Integer> sequences = new ArrayList<>(stopTimes.keySet());
            Collections.sort(sequences);

            for (int i = 0; i < sequences.size() - 1; i++) {
                int fromSequence = sequences.get(i);
                int toSequence = sequences.get(i + 1);

                org.apache.commons.lang3.tuple.Pair<LocalTime, Stop> fromPair = stopTimes.get(fromSequence);
                org.apache.commons.lang3.tuple.Pair<LocalTime, Stop> toPair = stopTimes.get(toSequence);

                if (fromPair != null && toPair != null) {
                    LocalTime departureTime = fromPair.getLeft();
                    LocalTime arrivalTime = toPair.getLeft();
                    Stop fromStop = fromPair.getRight();
                    Stop toStop = toPair.getRight();

                    Connection connection = new Connection(
                            fromStop.stopId,
                            toStop.stopId,
                            trip.getTripId(),
                            route.getRouteId(),
                            route.getShortName(),
                            departureTime,
                            arrivalTime,
                            route.getType().toString()
                    );

                    graph.addConnection(connection);
                }
            }
        }
    }

    private double calculateHeuristic(String stopId, String goalStopId, TransitPreference preferences) {
        if (stopId.equals(goalStopId)) return 0;

        Stop currentStop = graph.getStop(stopId);
        Stop goalStop = graph.getStop(goalStopId);
        if (currentStop == null || goalStop == null) return 0;

        // Calculate straight-line distance
        double distance = QuadTree.distance(
                currentStop.latitude, currentStop.longitude,
                goalStop.latitude, goalStop.longitude
        );

        // Determine available modes for heuristic
        List<String> availableModes = new ArrayList<>();
        if (!preferences.forbiddenModes.contains(TransportType.METRO)) availableModes.add("METRO");
        if (!preferences.forbiddenModes.contains(TransportType.TRAM)) availableModes.add("TRAM");
        if (!preferences.forbiddenModes.contains(TransportType.BUS)) availableModes.add("BUS");
        if (!preferences.forbiddenModes.contains(TransportType.TRAIN)) availableModes.add("TRAIN");

        // Default to walking if all transport modes are forbidden
        if (availableModes.isEmpty()) availableModes.add("FOOT");

        // Find the fastest mode's effective speed
        double fastestEffectiveSpeed = 0;
        for (String mode : availableModes) {
            double speed = graph.getModeSpeed(mode);
            double weight = getPreferenceWeight(preferences, mode);
            double effectiveSpeed = speed / weight;
            fastestEffectiveSpeed = Math.max(fastestEffectiveSpeed, effectiveSpeed);
        }

        // Ensure we have a non-zero speed
        if (fastestEffectiveSpeed <= 0) fastestEffectiveSpeed = preferences.walkingSpeed;

        // Calculate the minimum possible travel time
        return distance / fastestEffectiveSpeed;
    }

    private double getPreferenceWeight(TransitPreference preferences, String mode) {
        try {
            TransportType type = TransportType.valueOf(mode);
            return preferences.modeWeights.getOrDefault(type, 1.0);
        } catch (IllegalArgumentException e) {
            return 1.0;
        }
    }

    public List<Transition> findPath(String startStopId, String goalStopId, LocalTime startTime,
                                     TransitPreference preferences) {
        if (!graph.getStops().containsKey(startStopId) || !graph.getStops().containsKey(goalStopId)) {
            return null;
        }

        // Calculate initial heuristic
        double initialHeuristic = calculateHeuristic(startStopId, goalStopId, preferences);

        // Create initial state
        State initialState = State.createInitialState(startStopId, startTime, initialHeuristic);

        // Priority queue for A* search
        PriorityQueue<State> frontier = new PriorityQueue<>();
        frontier.add(initialState);

        // Track visited states
        Map<String, Double> visited = new HashMap<>();

        // Metrics
        int iterations = 0;
        int pruningOperations = 0;

        // Estimate direct trip time
        Stop startStop = graph.getStop(startStopId);
        Stop goalStop = graph.getStop(goalStopId);
        double directDistanceMeters = 0;

        if (startStop != null && goalStop != null) {
            directDistanceMeters = QuadTree.distance(
                    startStop.latitude, startStop.longitude,
                    goalStop.latitude, goalStop.longitude
            );
        }

        // Rough estimate of direct trip time at 60 km/h (1000 m/min)
        int directTripMinutes = (int) Math.ceil(directDistanceMeters / 1000.0);
        int maxReasonableCost = Math.max(MAX_REASONABLE_DURATION, directTripMinutes * 3);

        while (!frontier.isEmpty() && iterations < maxIterations) {
            // Prune frontier if too large
            if (frontier.size() > FRONTIER_PRUNING_THRESHOLD) {
                frontier = pruneFrontier(frontier);
                pruningOperations++;
            }

            State current = frontier.poll();
            iterations++;

            // Skip states with unreasonable costs
            if (current.cost > maxReasonableCost) continue;

            // Check if goal reached
            if (current.stopId.equals(goalStopId)) {
                return current.path;
            }

            // Skip if we've found a better path to this state
            String lastMode = current.path.isEmpty() ? "NONE" : current.path.getLast().mode;
            String stateKey = current.stopId + ":" + current.time + ":" + lastMode;

            if (visited.containsKey(stateKey) && visited.get(stateKey) <= current.cost) {
                continue;
            }

            visited.put(stateKey, current.cost);

            // Get possible connections
            List<Connection> connections = graph.getOutgoingConnections(
                    current.stopId, current.time, preferences);

            // Process connections
            for (Connection conn : connections) {
                String mode = conn.mode;

                // Skip forbidden modes
                if (isForbiddenMode(preferences, mode)) continue;

                // Skip already visited stops (avoid cycles)
                if (pathContainsStop(current.path, conn.toStop)) continue;

                // Calculate times
                LocalTime departureTime = conn.departureTime;
                LocalTime arrivalTime = conn.arrivalTime;

                // Calculate wait and travel times
                long waitTime = Math.max(0, current.time.until(departureTime, ChronoUnit.MINUTES));

                // Skip unreasonable waits
                if (waitTime > MAX_REASONABLE_WAIT) continue;

                // Handle day wrapping for travel time
                long travelTime = departureTime.until(arrivalTime, ChronoUnit.MINUTES);
                if (travelTime < 0) travelTime += 24 * 60; // Add a day

                // Skip unreasonably long legs
                if (travelTime > maxReasonableCost) continue;

                // Apply mode weight
                double modeWeight = getPreferenceWeight(preferences, mode);
                double transitionCost = (waitTime + travelTime) * modeWeight;

                // Early termination if cost too high
                if (current.cost + transitionCost > maxReasonableCost) continue;

                // Create transition
                Transition transition = Transition.fromConnection(conn, transitionCost);

                // Calculate heuristic
                double newHeuristic = calculateHeuristic(conn.toStop, goalStopId, preferences);

                // Create new state
                State newState = current.createSuccessor(
                        conn.toStop, arrivalTime, transitionCost, transition, newHeuristic);

                frontier.add(newState);
            }
        }

        // No path found
        return null;
    }

    private PriorityQueue<State> pruneFrontier(PriorityQueue<State> frontier) {
        PriorityQueue<State> prunedFrontier = new PriorityQueue<>();
        for (int i = 0; i < MAX_FRONTIER_AFTER_PRUNING && !frontier.isEmpty(); i++) {
            prunedFrontier.add(frontier.poll());
        }
        return prunedFrontier;
    }

    private boolean isForbiddenMode(TransitPreference preferences, String mode) {
        try {
            TransportType type = TransportType.valueOf(mode);
            return preferences.forbiddenModes.contains(type);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean pathContainsStop(List<Transition> path, String stopId) {
        for (Transition t : path) {
            if (t.toStop.equals(stopId)) return true;
        }
        return false;
    }

    public static String formatStopName(String stopName) {
        // Remove prefix like "STIB-", "DELIJN-", etc.
        String name = stopName.replaceAll("^(STIB-|DELIJN-|SNCB-|TEC-)", "");

        // Remove parenthesized text at the end
        name = name.replaceAll("\\s*\\(.*\\)\\s*$", "");

        // Capitalize words properly
        String[] words = name.split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            String lowerWord = word.toLowerCase();
            if (lowerWord.equals("de") || lowerWord.equals("du") ||
                    lowerWord.equals("des") || lowerWord.equals("la") ||
                    lowerWord.equals("le") || lowerWord.equals("les") ||
                    lowerWord.equals("en") || lowerWord.equals("et") ||
                    lowerWord.equals("a")) {

                formatted.append(lowerWord).append(" ");
            } else {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }

    public void printPath(List<Transition> path) {
        if (path == null || path.isEmpty()) {
            System.out.println("No path found.");
            return;
        }

        // Group transitions by route
        List<List<Transition>> routeSegments = new ArrayList<>();
        List<Transition> currentSegment = new ArrayList<>();
        currentSegment.add(path.get(0));

        // Group sequential transitions with the same route
        for (int i = 1; i < path.size(); i++) {
            Transition prev = path.get(i - 1);
            Transition curr = path.get(i);

            boolean sameRoute = prev.route.equals(curr.route) && prev.mode.equals(curr.mode);
            boolean isWalking = curr.mode.equals("FOOT");

            // Don't group walking segments or different routes
            if (!sameRoute || isWalking) {
                routeSegments.add(new ArrayList<>(currentSegment));
                currentSegment.clear();
            }

            currentSegment.add(curr);
        }

        // Add the last segment
        if (!currentSegment.isEmpty()) {
            routeSegments.add(currentSegment);
        }

        // Print the grouped segments
        for (List<Transition> segment : routeSegments) {
            Transition first = segment.getFirst();
            Transition last = segment.getLast();

            Stop fromStop = graph.getStop(first.fromStop);
            Stop toStop = graph.getStop(last.toStop);

            String fromStopName = (fromStop != null) ? formatStopName(fromStop.name) : first.fromStop;
            String toStopName = (toStop != null) ? formatStopName(toStop.name) : last.toStop;

            String routeInfo = first.route.isEmpty() ? "" : " " + first.route;

            System.out.println(
                    "Take " + first.mode + routeInfo +
                            " from " + fromStopName + " (" + first.departure + ")" +
                            " to " + toStopName + " (" + last.arrival + ")"
            );
        }
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public int getMaxFrontierSize() {
        return maxFrontierSize;
    }

    public void setMaxFrontierSize(int maxFrontierSize) {
        this.maxFrontierSize = maxFrontierSize;
    }

    public TransitGraph getGraph() {
        return graph;
    }
}