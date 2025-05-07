package algo.transit;

import algo.transit.models.common.Route;
import algo.transit.models.common.Stop;
import algo.transit.models.common.Trip;
import algo.transit.models.pathfinder.TPreference;
import algo.transit.models.pathfinder.Transition;
import algo.transit.models.visualizer.StateRecorder;
import algo.transit.pathfinders.DPathfinder;
import algo.transit.services.CSVService;
import algo.transit.utils.CLParser;
import algo.transit.utils.CLParser.CLArgs;
import algo.transit.utils.QuadTree;
import algo.transit.visualizers.DVisualizer;

import java.util.List;
import java.util.Map;

import static algo.transit.utils.PathPrinter.printPath;

public class ALGOL {
    public static void main(String[] args) {
        try {
            CLArgs cmdArgs = CLParser.parseCommandLineArgs(args);
            CSVService csvService = new CSVService();

            System.out.println("Finding path from " + cmdArgs.startStop + " to " + cmdArgs.endStop + " at " + cmdArgs.startTime);
            if (cmdArgs.arriveBy) System.out.println("Mode: Arrive by (paths calculated to arrive at specified time)");

            long loadStartTime = System.currentTimeMillis();
            Map<String, Route> routes = csvService.getRoutes();
            Map<String, Stop> stops = csvService.getStops();
            Map<String, Trip> trips = csvService.getTrips(routes);
            csvService.linkData(stops, trips);
            long loadTime = System.currentTimeMillis() - loadStartTime;
            System.out.println("Data loading time: " + (loadTime / 1000.0) + " seconds");

            Stop startStop = stops.get(cmdArgs.startStop);
            Stop endStop = stops.get(cmdArgs.endStop);

            if (startStop != null && endStop != null) {
                double distance = QuadTree.calculateDistance(
                        startStop.latitude, startStop.longitude,
                        endStop.latitude, endStop.longitude
                );
                System.out.println("Distance between stops: " + distance + " meters");
            }

            TPreference preferences = new TPreference(
                    cmdArgs.walkingSpeed,
                    cmdArgs.maxWalkTime,
                    cmdArgs.modeWeights,
                    cmdArgs.forbiddenModes,
                    cmdArgs.optimizationGoal
            );

            DPathfinder dPathfinder = new DPathfinder(stops);

            long startTime = System.currentTimeMillis();
            List<Transition> path;

            if (cmdArgs.arriveBy) {
                // TODO: Implement reverse pathfinding
                System.out.println("Arrive-by mode not yet implemented. Using departure time instead.");
                path = dPathfinder.findPath(cmdArgs.startStop, cmdArgs.endStop, cmdArgs.startTime, preferences);
            } else {
                path = dPathfinder.findPath(cmdArgs.startStop, cmdArgs.endStop, cmdArgs.startTime, preferences);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("Pathfinding time: " + (executionTime / 1000.0) + " seconds");
            printPath(path, cmdArgs.outputFormat, cmdArgs.showStats, stops);

            if (cmdArgs.visualize) {
                StateRecorder recorder = dPathfinder.recorder;

                DVisualizer visualizer = new DVisualizer(stops);
                visualizer.setAlgorithmData(recorder);
                visualizer.setVisible(true);

                System.out.println("Visualization window is open. Close it to exit the program.");
                visualizer.waitForCompletion();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}