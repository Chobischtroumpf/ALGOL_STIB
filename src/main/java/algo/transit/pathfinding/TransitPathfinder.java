package algo.transit.pathfinding;

import algo.transit.models.*;
import algo.transit.services.CSVService;
import algo.transit.utils.QuadTree;
import algo.transit.enums.TransportType;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TransitPathfinder {
    public final TransitGraph graph;
    public int maxIterations = 50000000;
    public static final int MAX_REASONABLE_DURATION = 600;
    public static final int MAX_REASONABLE_WAIT = 60;
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
        for (Route route : routes.values()) transitGraph.addRouteType(route.routeId, route.type.toString());

        // Create connections
        createConnectionsFromTrips(transitGraph, trips);

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
                            trip.tripId,
                            route.routeId,
                            route.shortName,
                            departureTime,
                            arrivalTime,
                            route.type.toString()
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

        // For walking, convert distance to time using walking speed
        double walkingTimeEstimate = distance / preferences.walkingSpeed;

        // Try to find the best time estimate from actual transit data
        double bestTransitTimeEstimate = Double.MAX_VALUE;

        if (distance > 10000) { // 10km
            // Favor train estimates for long distances
            double trainEstimate = distance / 1000.0; // ~1000 m/min for trains
            return Math.min(trainEstimate, bestTransitTimeEstimate);
        }

        // Check if we have any direct routes between these stops or nearby stops
        Set<String> routesFromCurrentStop = new HashSet<>();
        if (currentStop.routes != null) {
            routesFromCurrentStop.addAll(currentStop.routes.keySet());
        }

        // Look for common routes that serve both stops
        Set<String> commonRoutes = new HashSet<>(routesFromCurrentStop);
        if (goalStop.routes != null) {
            commonRoutes.retainAll(goalStop.routes.keySet());
        }

        // If we have common routes, estimate transit time based on actual schedule data
        for (String routeId : commonRoutes) {
            double routeTimeEstimate = estimateTransitTime(currentStop, goalStop, routeId);
            if (routeTimeEstimate > 0) {
                bestTransitTimeEstimate = Math.min(bestTransitTimeEstimate, routeTimeEstimate);
            }
        }

        // If no direct route was found, we'll still need a transit estimate
        if (bestTransitTimeEstimate == Double.MAX_VALUE) {
            // Use direct time with a transfer penalty as our estimate
            bestTransitTimeEstimate = (distance / 250.0) + 15; // Assume 250 m/min with 15-min transfer
        }

        // Return the better of walking or transit time
        return Math.min(walkingTimeEstimate, bestTransitTimeEstimate);
    }

    private double estimateTransitTime(Stop fromStop, Stop toStop, String routeId) {
        // Look for trips that contain both stops
        Set<String> commonTrips = new HashSet<>(fromStop.trips.keySet());
        commonTrips.retainAll(toStop.trips.keySet());

        double bestTime = -1;

        for (String tripId : commonTrips) {
            Trip trip = fromStop.trips.get(tripId);
            if (trip != null && trip.route.routeId.equals(routeId)) {
                LocalTime fromTime = trip.getTimeForStop(fromStop);
                LocalTime toTime = trip.getTimeForStop(toStop);

                if (fromTime != null && toTime != null) {
                    // Calculate minutes between stops
                    long minutes = fromTime.until(toTime, ChronoUnit.MINUTES);
                    if (minutes < 0) minutes += 24 * 60; // Handle day wrapping

                    // Update our best time estimate
                    if (bestTime < 0 || minutes < bestTime) bestTime = minutes;
                }
            }
        }

        return bestTime;
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
        if (!graph.getStops().containsKey(startStopId) || !graph.getStops().containsKey(goalStopId)) return null;

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
            // Prune frontier if too large (to avoid memory issues)
            if (frontier.size() > FRONTIER_PRUNING_THRESHOLD) frontier = pruneFrontier(frontier);

            State current = frontier.poll();
            iterations++;

            // Skip states with unreasonable costs
            assert current != null;
            if (current.cost > maxReasonableCost) continue;

            // Check if goal reached
            if (current.stopId.equals(goalStopId)) return current.path;

            // Skip if we've found a better path to this state
            String lastMode = current.path.isEmpty() ? "NONE" : current.path.getLast().mode;
            String stateKey = current.stopId + ":" + current.time + ":" + lastMode;

            if (visited.containsKey(stateKey) && visited.get(stateKey) <= current.cost) continue;

            visited.put(stateKey, current.cost);

            // Get possible connections
            List<Connection> connections = graph.getOutgoingConnections(current.stopId, current.time, preferences);

            // Process connections
            for (Connection conn : connections) {
                String mode = conn.mode;

                // Skip forbidden modes
                if (isForbiddenMode(preferences, mode)) continue;

                // Skip already visited stops (avoid cycles)
                if (pathContainsStop(current.path, conn.toStop)) continue;

                // Calculate times using actual schedule data
                LocalTime departureTime = conn.departureTime;
                LocalTime arrivalTime = conn.arrivalTime;

                // Calculate wait and travel times
                long waitTime = Math.max(0, current.time.until(departureTime, ChronoUnit.MINUTES));

                // Skip unreasonable waits
                if (waitTime > MAX_REASONABLE_WAIT) continue;

                // Calculate travel time from schedule
                long travelTime = departureTime.until(arrivalTime, ChronoUnit.MINUTES);
                if (travelTime < 0) travelTime += 24 * 60; // Add a day if wrapping

                // Skip unreasonably long legs
                if (travelTime > MAX_REASONABLE_DURATION) continue;

                // Calculate base cost (real travel time)
                double transitionCost = travelTime;

                // Add waiting time to cost
                transitionCost += waitTime;

                // Apply mode weight
                double modeWeight = getPreferenceWeight(preferences, mode);
                transitionCost *= modeWeight;

                // Apply mode switching penalty
                if (!current.lastMode.equals("NONE") && !current.lastMode.equals(mode)) {
                    // Add a higher penalty for certain mode switches
                    if (current.lastMode.equals("TRAIN")) {
                        // Higher penalty for getting off a train (obviously)
                        transitionCost += preferences.modeSwitchPenalty * 1.5;
                    } else {
                        transitionCost += preferences.modeSwitchPenalty;
                    }
                }

                Transition transition = Transition.fromConnection(conn, transitionCost);
                double newHeuristic = calculateHeuristic(conn.toStop, goalStopId, preferences);
                State newState = current.createSuccessor(conn.toStop, arrivalTime, transitionCost, transition, newHeuristic);

                frontier.add(newState);
            }
        }

        // No path found
        return null;
    }

    private PriorityQueue<State> pruneFrontier(PriorityQueue<State> frontier) {
        Map<String, State> bestStates = new HashMap<>();
        Set<State> addedStates = new HashSet<>(); // Track states we've added

        // Group by destination stop (keep best state for each stop)
        for (State state : frontier) {
            String key = state.stopId;
            if (!bestStates.containsKey(key) || bestStates.get(key).getTotalCost() > state.getTotalCost()) {
                bestStates.put(key, state);
            }
        }

        // Create new frontier with best states
        PriorityQueue<State> prunedFrontier = new PriorityQueue<>();

        // Add all best states first
        for (State state : bestStates.values()) {
            prunedFrontier.add(state);
            addedStates.add(state); // Track what we've added
        }

        // Add additional states up to the limit (using HashSet for O(1) lookups)
        for (State state : frontier) {
            if (prunedFrontier.size() >= MAX_FRONTIER_AFTER_PRUNING) break;
            if (!addedStates.contains(state)) { // O(1) check instead of O(n)
                prunedFrontier.add(state);
                addedStates.add(state);
            }
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
}