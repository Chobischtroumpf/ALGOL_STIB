package algo.transit.pathfinders;

import algo.transit.enums.TType;
import algo.transit.models.common.Stop;
import algo.transit.models.pathfinder.Connection;
import algo.transit.models.pathfinder.TPreference;
import algo.transit.models.pathfinder.Transition;
import algo.transit.utils.QuadTree;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static algo.transit.utils.TimeUtils.calculateMinutesBetween;

public abstract class AbstractPathfinder {
    // Constants for spatial indexing
    protected static final double MAX_LATITUDE = 52.0;
    protected static final double MIN_LATITUDE = 49.0;
    protected static final double MAX_LONGITUDE = 7.0;
    protected static final double MIN_LONGITUDE = 2.0;

    protected final Map<String, Stop> stops;
    protected final QuadTree stopQuadTree;

    protected AbstractPathfinder(Map<String, Stop> stops) {
        this.stops = stops;
        this.stopQuadTree = buildQuadTree(stops);
    }

    protected @NotNull QuadTree buildQuadTree(@NotNull Map<String, Stop> stops) {
        System.out.println("Building QuadTree for spatial stop indexing...");
        QuadTree tree = new QuadTree(MIN_LONGITUDE, MIN_LATITUDE, MAX_LONGITUDE, MAX_LATITUDE, 0);
        for (Stop stop : stops.values()) tree.insert(stop);
        System.out.println("QuadTree built successfully");
        return tree;
    }

    /**
     * Abstract method that concrete pathfinders must implement
     */
    public abstract List<Transition> findPath(
            String startStopId,
            String endStopId,
            LocalTime startTime,
            TPreference preferences
    );

    /**
     * Calculate the cost of a transition based on preferences
     */
    protected double calculateTransitionCost(
            LocalTime currentTime,
            @NotNull Connection connection,
            String lastMode,
            @NotNull TPreference preferences
    ) {
        long waitingMinutes = calculateMinutesBetween(
                currentTime,
                connection.departureTime(),
                0);
        long transitMinutes = calculateMinutesBetween(
                connection.departureTime(),
                connection.arrivalTime(),
                connection.arrivalTime().isBefore(connection.departureTime()) ? 1 : 0);

        double cost;
        String goal = preferences.getOptimizationGoal();

        if (goal == null || goal.isEmpty() || goal.equalsIgnoreCase("time")) {
            // Half penalty for waiting
            cost = transitMinutes + (waitingMinutes * 0.5);
        } else if (goal.equalsIgnoreCase("transfers")) {
            // Heavily penalize mode changes to minimize transfers
            cost = (transitMinutes * 0.1) + (waitingMinutes * 0.01);
        } else if (goal.equalsIgnoreCase("walking")) {
            // Heavily penalize walking
            if (connection.mode().equals("FOOT")) {
                // Heavy penalty for walking
                cost = transitMinutes * 5.0;
            } else {
                cost = transitMinutes + (waitingMinutes * 0.5);
            }
        } else {
            cost = transitMinutes + (waitingMinutes * 0.5);
        }

        // Apply mode-specific weights
        TType mode = TType.fromString(connection.mode());
        Double modeWeight = preferences.getModeWeights().get(mode);
        if (modeWeight != null) {
            // Only apply weight to the transit time, not the waiting time
            if (goal != null && goal.equalsIgnoreCase("transfers")) {
                // For transfer optimization, still preserve some weight difference
                cost = (waitingMinutes * 0.1) + (transitMinutes * Math.min(1.5, modeWeight));
            } else {
                cost = (waitingMinutes * 0.5) + (transitMinutes * modeWeight);
            }
        }

        // When we detect a mode change, enforce minimum transfer time
        if (!lastMode.equals("NONE") && !lastMode.equals(connection.mode())) {
            // SPECIAL CASE FOR WALKING - skip the wait time check
            if (connection.mode().equals("FOOT")) {
                double penalty = calculateModeSwitchPenalty(lastMode, connection.mode(), goal);
                cost += penalty;
            } else {
                // For non-walking modes, check transfer time as usual
                Stop fromStop = stops.get(connection.fromStop());
                Stop toStop = stops.get(connection.toStop());
                double minTransferTime = calculateTransferTime(fromStop, toStop);

                if (waitingMinutes < minTransferTime) return -1.0;

                double penalty = calculateModeSwitchPenalty(lastMode, connection.mode(), goal);
                cost += penalty;
            }
        }

        return Math.max(0.1, cost); // Ensure positive cost
    }

    /**
     * Determines if exploring a particular stop is worthwhile based on spatial position
     */
    protected boolean isWorthExploring(
            @NotNull Stop currentStop,
            @NotNull Stop nextStop,
            @NotNull Stop targetStop
    ) {
        // Calculate current and next distances to target
        double currentDistance = QuadTree.calculateDistance(
                currentStop.getLatitude(), currentStop.getLongitude(),
                targetStop.getLatitude(), targetStop.getLongitude()
        );

        double nextDistance = QuadTree.calculateDistance(
                nextStop.getLatitude(), nextStop.getLongitude(),
                targetStop.getLatitude(), targetStop.getLongitude()
        );

        // Skip if we're moving significantly away from the target
        double deviationTolerance = 1.25;

        // For shorter segments, allow more deviation
        if (currentDistance < 5000) deviationTolerance = 2.0; // Allow more deviation for local transit

        // Calculate direct distance between stops
        double segmentDistance = QuadTree.calculateDistance(
                currentStop.getLatitude(), currentStop.getLongitude(),
                nextStop.getLatitude(), nextStop.getLongitude()
        );

        // If this is a very short segment, always allow it
        if (segmentDistance < 1000) return true;

        // Check if overall direction is reasonable
        return nextDistance < currentDistance * deviationTolerance;
    }

    /**
     * Determines if a departure time is worth considering based on waiting time
     */
    protected boolean isWorthConsideringTime(
            LocalTime currentTime,
            LocalTime departureTime
    ) {
        long waitingMinutes = calculateMinutesBetween(currentTime, departureTime);

        // Otherwise, use the default rules
        // Case 1: Very short wait (always consider)
        if (waitingMinutes <= 30) return true;

        // Case 2: Reasonable wait time during same day
        if (waitingMinutes <= 3 * 60) return true;

        // Case 3: Overnight case - first departure next morning
        if (currentTime.getHour() >= 20 && departureTime.getHour() <= 10) {
            // For end-of-day to morning trips, allow longer waiting times
            return waitingMinutes <= 12 * 60;
        }

        // Case 4: Day transition without being overnight case
        return waitingMinutes <= 4 * 60;
    }

    protected double calculateTransferTime(
            Stop fromStop,
            Stop toStop
    ) {
        double baseTime = 2.0;

        if (fromStop == null || toStop == null) return baseTime;

        double distance = QuadTree.calculateDistance(
                fromStop.getLatitude(), fromStop.getLongitude(),
                toStop.getLatitude(), toStop.getLongitude()
        );

        double transferTime = baseTime + (distance / 100.0);

        // Station complexity factor based on number of routes
        // More routes = more complex station = more transfer time
        int fromRoutes = fromStop.getRoutes().size();
        int toRoutes = toStop.getRoutes().size();

        if (fromRoutes > 5 || toRoutes > 5) {
            transferTime += 2.0;
        } else if (fromRoutes > 2 || toRoutes > 2) {
            transferTime += 1.0;
        }

        return Math.min(Math.max(transferTime, 1.0), 5.0);
    }

    protected double calculateModeSwitchPenalty(
            String fromMode,
            String toMode,
            String optimizationGoal
    ) {
        double penalty = 5.0;

        // Higher penalty for transfers optimization
        if ("transfers".equalsIgnoreCase(optimizationGoal)) {
            penalty = 500.0;
            return penalty;
        }

        // Mode-specific adjustments
        if (fromMode.equals("TRAIN") && toMode.equals("BUS")) {
            penalty = 8.0;
        } else if (fromMode.equals("BUS") && toMode.equals("BUS")) {
            penalty = 4.0;
        } else if (fromMode.equals("TRAM") && toMode.equals("TRAM")) {
            penalty = 3.0;
        } else if (fromMode.equals("FOOT") || toMode.equals("FOOT")) {
            penalty = 2.0;
        }

        return penalty;
    }
}