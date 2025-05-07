package algo.transit.pathfinders;

import algo.transit.enums.TType;
import algo.transit.models.common.Stop;
import algo.transit.models.pathfinder.Connection;
import algo.transit.models.pathfinder.TPreference;
import algo.transit.models.pathfinder.Transition;
import algo.transit.models.visualizer.StateRecorder;
import algo.transit.utils.QuadTree;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public abstract class AbstractPathfinder {
    protected final Map<String, Stop> stops;
    protected final QuadTree stopQuadTree;
    protected static final double MAX_LATITUDE = 52.0;
    protected static final double MIN_LATITUDE = 49.0;
    protected static final double MAX_LONGITUDE = 7.0;
    protected static final double MIN_LONGITUDE = 2.0;

    // Recording of algorithm steps
    public StateRecorder recorder;

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
        // Calculate waiting time
        long waitingMinutes = calculateMinutesBetween(currentTime, connection.departureTime());

        // Calculate transit time
        long transitMinutes = calculateMinutesBetween(connection.departureTime(), connection.arrivalTime());

        // Base cost: transit time + waiting time (with lower weight)
        double cost = transitMinutes + (waitingMinutes * 0.5); // Half penalty for waiting

        // Add mode-specific weights for the transit part
        TType mode = TType.fromString(connection.mode());
        Double modeWeight = preferences.modeWeights.get(mode);
        if (modeWeight != null) {
            // Only apply weight to the transit time, not the waiting time
            cost = (waitingMinutes * 0.5) + (transitMinutes * modeWeight);
        }

        // Add mode switch penalty
        if (!lastMode.equals("NONE") && !lastMode.equals(connection.mode())) cost += preferences.modeSwitchPenalty;
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

    /**
     * Determines if a departure time is worth considering based on waiting time
     */
    protected boolean isWorthConsideringTime(
            LocalTime currentTime,
            LocalTime departureTime,
            double maxWaitTime
    ) {
        long waitingMinutes = calculateMinutesBetween(currentTime, departureTime);

        // If maxWaitTime is specified, use it as the upper limit
        if (maxWaitTime > 0) return waitingMinutes <= maxWaitTime;

        // Otherwise, use the default rules
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

    /**
     * Calculate minutes between two LocalTime objects with adjustments for transit time formats
     */
    protected long calculateMinutesBetween(@NotNull LocalTime start, @NotNull LocalTime end) {
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

    /**
     * Checks if one time is before another
     */
    protected boolean isBefore(@NotNull LocalTime time1, @NotNull LocalTime time2) {
        // Normalize hours to 0-23 range
        int hour1 = time1.getHour() % 24;
        int hour2 = time2.getHour() % 24;

        // If hours are equal, compare minutes
        if (hour1 == hour2) return time1.getMinute() < time2.getMinute();
        return hour1 < hour2;
    }

    /**
     * Checks if one time is after another
     */
    protected boolean isAfter(@NotNull LocalTime time1, LocalTime time2) {
        return !time1.equals(time2) && !isBefore(time1, time2);
    }

    /**
     * Prints the found path
     */
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