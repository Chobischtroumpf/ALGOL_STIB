package algo.transit;

import algo.transit.models.common.Route;
import algo.transit.models.common.Stop;
import algo.transit.models.common.Trip;
import algo.transit.models.pathfinder.TPreference;
import algo.transit.models.pathfinder.Transition;
import algo.transit.pathfinders.DPathfinder;
import algo.transit.services.CSVService;
import algo.transit.utils.CLArgs;
import algo.transit.utils.CLParser;
import algo.transit.utils.QuadTree;

import java.util.List;
import java.util.Map;

import static algo.transit.utils.PathPrinter.printPath;

public class BETransitPathfinder {
    public static void main(String[] args) {
        try {
            CLArgs cmdArgs = CLParser.parseCommandLineArgs(args);
            CSVService csvService = new CSVService();

            System.out.println("Finding path from " + cmdArgs.getStartStop() + " to " + cmdArgs.getEndStop() + " at " + cmdArgs.getStartTime());
            if (cmdArgs.isArriveBy()) System.out.println("Mode: Arrive by (paths calculated to arrive at specified time)");

            long loadStartTime = System.currentTimeMillis();

            long start = System.currentTimeMillis();
            Map<String, Route> routes = csvService.getRoutes();
            System.out.println("Routes loaded in " + (System.currentTimeMillis() - start) + " ms");

            start = System.currentTimeMillis();
            Map<String, Stop> stops = csvService.getStops();
            System.out.println("Stops loaded in " + (System.currentTimeMillis() - start) + " ms");

            start = System.currentTimeMillis();
            Map<String, Trip> trips = csvService.getTrips(routes);
            System.out.println("Trips loaded in " + (System.currentTimeMillis() - start) + " ms");

            start = System.currentTimeMillis();
            csvService.linkData(stops, trips);
            System.out.println("Linking data took " + (System.currentTimeMillis() - start) + " ms");

            long loadTime = System.currentTimeMillis() - loadStartTime;
            System.out.println("Data loading time: " + (loadTime / 1000.0) + " seconds");

            Stop startStop = stops.get(cmdArgs.getStartStop());
            Stop endStop = stops.get(cmdArgs.getEndStop());

            if (startStop != null && endStop != null) {
                double distance = QuadTree.calculateDistance(
                        startStop.getLatitude(), startStop.getLongitude(),
                        endStop.getLatitude(), endStop.getLongitude()
                );
                System.out.println("Distance between stops: " + distance + " meters");
            }

            TPreference preferences = new TPreference(
                    cmdArgs.getWalkingSpeed(),
                    cmdArgs.getMaxWalkTime(),
                    cmdArgs.getModeWeights(),
                    cmdArgs.getForbiddenModes(),
                    cmdArgs.getOptimizationGoal()
            );

            DPathfinder dPathfinder = new DPathfinder(stops);

            long startTime = System.currentTimeMillis();
            List<Transition> path;

            if (cmdArgs.isArriveBy()) {
                // TODO: Implement reverse pathfinding
                System.out.println("Arrive-by mode not yet implemented. Using departure time instead.");
                path = dPathfinder.findPath(cmdArgs.getStartStop(), cmdArgs.getEndStop(), cmdArgs.getStartTime(), preferences);
            } else {
                path = dPathfinder.findPath(cmdArgs.getStartStop(), cmdArgs.getEndStop(), cmdArgs.getStartTime(), preferences);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("Pathfinding time: " + (executionTime / 1000.0) + " seconds");
            printPath(path, cmdArgs.getOutputFormat(), cmdArgs.isShowStats(), stops);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}