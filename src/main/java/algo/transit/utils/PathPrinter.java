package algo.transit.utils;

import algo.transit.models.pathfinder.Transition;
import algo.transit.models.common.Stop;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static algo.transit.utils.TimeUtils.calculateMinutesBetween;

public class PathPrinter {
    /**
     * Prints the found path
     */
    public static void printPath(
            @NotNull List<Transition> path,
            String outputFormat,
            boolean showStats,
            @NotNull Map<String, Stop> stops
    ) {
        if (path.isEmpty()) {
            System.out.println("No path found.");
            return;
        }

        if (outputFormat == null || outputFormat.isEmpty()) outputFormat = "detailed";

        Transition firstTransition = path.getFirst();
        Transition lastTransition = path.getLast();

        LocalTime startTime = firstTransition.departure();
        LocalTime endTime = lastTransition.arrival();

        long totalMinutes = calculateMinutesBetween(startTime, endTime);
        int totalTransfers = countTransfers(path, stops);

        if (outputFormat.equalsIgnoreCase("summary")) {
            printSummaryPath(path, totalMinutes, totalTransfers, stops);
        } else {
            printDetailedPath(path, totalMinutes, stops);
        }

        if (showStats) printPathStatistics(path, stops);
    }

    private static void printDetailedPath(
            @NotNull List<Transition> path,
            long totalMinutes,
            @NotNull Map<String, Stop> stops
    ) {
        System.out.println("\nOptimal route:");
        System.out.println("======================");

        Transition firstTransition = path.getFirst();
        Transition lastTransition = path.getLast();

        System.out.println("From: " + stops.get(firstTransition.fromStop()).name);
        System.out.println("To: " + stops.get(lastTransition.toStop()).name);
        System.out.println("Departure: " + firstTransition.departure());
        System.out.println("Arrival: " + lastTransition.arrival());
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

    private static void printSummaryPath(
            @NotNull List<Transition> path,
            long totalMinutes,
            int totalTransfers,
            @NotNull Map<String, Stop> stops
    ) {
        System.out.println("Route Summary:");
        System.out.println("--------------");
        System.out.println("From: " + stops.get(path.getFirst().fromStop()).name);
        System.out.println("To: " + stops.get(path.getLast().toStop()).name);
        System.out.println("Departure: " + path.getFirst().departure());
        System.out.println("Arrival: " + path.getLast().arrival());
        System.out.println("Travel time: " + totalMinutes + " minutes");
        System.out.println("Transfers: " + totalTransfers);
        System.out.println("Segments: " + path.size());
    }

    private static void printPathStatistics(
            @NotNull List<Transition> path,
            @NotNull Map<String, Stop> stops
    ) {
        System.out.println("\nAdditional Statistics:");
        System.out.println("=======================");

        // Time-related statistics
        LocalTime startTime = path.getFirst().departure();
        LocalTime endTime = path.getLast().arrival();
        long totalMinutes = calculateMinutesBetween(startTime, endTime);

        // Mode statistics
        Map<String, Integer> modeCount = new HashMap<>();
        Map<String, Long> modeDuration = new HashMap<>();
        Map<String, Long> modeDistance = new HashMap<>();

        // Calculate wait time
        long totalWaitTime = 0;
        long totalInVehicleTime = 0;

        String lastMode = "NONE";
        LocalTime lastArrival = null;

        for (Transition transition : path) {
            String mode = transition.mode();

            // Count segments by mode
            modeCount.put(mode, modeCount.getOrDefault(mode, 0) + 1);

            // Calculate duration by mode
            long duration = calculateMinutesBetween(transition.departure(), transition.arrival());
            modeDuration.put(mode, modeDuration.getOrDefault(mode, 0L) + duration);

            // Calculate waiting time
            if (lastArrival != null && !lastMode.equals("NONE")) {
                long wait = calculateMinutesBetween(lastArrival, transition.departure());
                if (wait > 0) {
                    totalWaitTime += wait;
                }
            }

            totalInVehicleTime += duration;

            // Calculate approximate distance
            Stop fromStop = stops.get(transition.fromStop());
            Stop toStop = stops.get(transition.toStop());
            if (fromStop != null && toStop != null) {
                double distance = QuadTree.calculateDistance(
                        fromStop.latitude, fromStop.longitude,
                        toStop.latitude, toStop.longitude
                );
                modeDistance.put(mode, modeDistance.getOrDefault(mode, 0L) + Math.round(distance));
            }

            lastMode = mode;
            lastArrival = transition.arrival();
        }

        // Print time breakdown
        System.out.println("Time breakdown:");
        System.out.println("  Total travel time: " + totalMinutes + " minutes");
        System.out.println("  In-vehicle time: " + totalInVehicleTime + " minutes (" +
                Math.round(totalInVehicleTime * 100.0 / totalMinutes) + "%)");
        System.out.println("  Waiting time: " + totalWaitTime + " minutes (" +
                Math.round(totalWaitTime * 100.0 / totalMinutes) + "%)");

        // Print mode statistics
        System.out.println("\nMode statistics:");
        for (Map.Entry<String, Integer> entry : modeCount.entrySet()) {
            String mode = entry.getKey();
            int count = entry.getValue();
            long duration = modeDuration.getOrDefault(mode, 0L);
            long distance = modeDistance.getOrDefault(mode, 0L);

            System.out.println("  " + mode + ": " + count + " segments, " +
                    duration + " minutes, ~" + (distance / 1000) + " km");
        }

        // Print transfer information
        int transfers = countTransfers(path, stops);
        System.out.println("\nTransfers: " + transfers);

        System.out.println("=======================");
    }

    private static int countTransfers(
            @NotNull List<Transition> path,
            @NotNull Map<String, Stop> stops
    ) {
        if (path.isEmpty()) return 0;

        int transfers = 0;
        String lastMode = "NONE";
        String lastRoute = "";

        for (Transition transition : path) {
            String currentMode = transition.mode();
            String currentRoute = transition.route();

            if (!lastMode.equals("NONE") &&
                    (!lastMode.equals(currentMode) ||
                            !lastRoute.equals(currentRoute) && !currentMode.equals("FOOT"))) {
                transfers++;
            }

            lastMode = currentMode;
            lastRoute = currentRoute;
        }

        return transfers;
    }
}
