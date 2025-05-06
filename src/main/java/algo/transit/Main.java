package algo.transit;

import algo.transit.enums.TransportType;
import algo.transit.models.*;
import algo.transit.pathfinders.DPathfinder;
import algo.transit.services.CSVService;
import algo.transit.utils.QuadTree;
import algo.transit.visualizers.DVisualizer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        try {
            CommandLineArgs cmdArgs = parseCommandLineArgs(args);
            CSVService csvService = new CSVService();

            System.out.println("Finding path from " + cmdArgs.startStop + " to " + cmdArgs.endStop + " at " + cmdArgs.startTime);

            long loadStartTime = System.currentTimeMillis();
            Map<String, Route> routes = csvService.getRoutes();
            Map<String, Stop> stops = csvService.getStops();
            Map<String, Trip> trips = csvService.getTrips(routes);
            csvService.linkData(stops, trips);
            long loadTime = System.currentTimeMillis() - loadStartTime;
            System.out.println("Data loading time: " + (loadTime / 1000.0) + " seconds");

            Stop startStop = stops.get(cmdArgs.startStop);
            Stop endStop = stops.get(cmdArgs.endStop);

            double distance;
            if (startStop != null && endStop != null) {
                distance = QuadTree.distance(
                        startStop.latitude, startStop.longitude,
                        endStop.latitude, endStop.longitude
                );
                System.out.println("Distance between stops: " + distance + " meters");
            }

            // Create preferences
            TransitPreference preferences = new TransitPreference(
                    cmdArgs.walkingSpeed,
                    cmdArgs.maxWalkTime,
                    cmdArgs.modeWeights,
                    cmdArgs.forbiddenModes,
                    cmdArgs.modeSwitchPenalty,
                    cmdArgs.maxTransfers
            );

            // Initialize pathfinder
            DPathfinder dPathfinder = new DPathfinder(stops);

            // Find path
            long startTime = System.currentTimeMillis();
            List<Transition> path = dPathfinder.findPath(cmdArgs.startStop, cmdArgs.endStop, cmdArgs.startTime, preferences);
            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("Pathfinding time: " + (executionTime / 1000.0) + " seconds");
            dPathfinder.printPath(path);

            // If visualization was enabled, wait for the user to close the window
            if (cmdArgs.visualize) {
                PathfindingRecorder recorder = dPathfinder.recorder;

                // Create and configure the visualizer
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

    public static class CommandLineArgs {
        public String startStop;
        public String endStop;
        public LocalTime startTime;
        public double walkingSpeed = 80.0;
        public double maxWalkTime = 10.0;
        public List<TransportType> forbiddenModes = new ArrayList<>();
        public Map<TransportType, Double> modeWeights = new HashMap<>();
        public double modeSwitchPenalty = 5.0;
        public int maxTransfers = 50;
        public boolean visualize = false;
    }

    public static @NotNull CommandLineArgs parseCommandLineArgs(String @NotNull [] args) {
        CommandLineArgs cmdArgs = new CommandLineArgs();

        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: java -jar transit.jar START_STOP END_STOP START_TIME [OPTIONS]");
        }

        cmdArgs.startStop = args[0];
        cmdArgs.endStop = args[1];

        try {
            cmdArgs.startTime = LocalTime.parse(args[2], TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Use HH:MM format (e.g., 08:00)");
        }

        // Parse optional arguments
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("--walking-speed") && i + 1 < args.length) {
                cmdArgs.walkingSpeed = Double.parseDouble(args[++i]);
            } else if (arg.equals("--max-walk-time") && i + 1 < args.length) {
                cmdArgs.maxWalkTime = Double.parseDouble(args[++i]);
            } else if (arg.equals("--forbidden-modes") && i + 1 < args.length) {
                while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    try {
                        cmdArgs.forbiddenModes.add(TransportType.valueOf(args[++i]));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid transport type: " + args[i] + ". Skipping.");
                    }
                }
            } else if (arg.equals("--mode-weights") && i + 1 < args.length) {
                while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    String weightSpec = args[++i];
                    String[] parts = weightSpec.split(":");
                    if (parts.length == 2) {
                        try {
                            cmdArgs.modeWeights.put(TransportType.valueOf(parts[0]), Double.parseDouble(parts[1]));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid weight format: " + weightSpec + ". Expected format: mode:weight");
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid transport type: " + parts[0] + ". Skipping.");
                        }
                    }
                }
            } else if (arg.equals("--mode-switch-penalty") && i + 1 < args.length) {
                cmdArgs.modeSwitchPenalty = Double.parseDouble(args[++i]);
            } else if (arg.equals("--max-transfers") && i + 1 < args.length) {
                cmdArgs.maxTransfers = Integer.parseInt(args[++i]);
            } else if (arg.equals("--visualize")) {
                cmdArgs.visualize = true;
            } else if (arg.equals("--help")) {
                System.out.println("Usage: java -jar transit.jar START_STOP END_STOP START_TIME [OPTIONS]");
                System.out.println("Options:");
                System.out.println("  --walking-speed <speed>       Set walking speed in meters per minute (default: 80.0)");
                System.out.println("  --max-walk-time <time>       Set maximum walking time in minutes (default: 10.0)");
                System.out.println("  --forbidden-modes <modes>    Set forbidden transport modes (e.g., BUS, TRAIN)");
                System.out.println("  --mode-weights <mode:weight> Set custom weights for transport modes");
                System.out.println("  --mode-switch-penalty <penalty> Set penalty for mode switching (default: 5.0)");
                System.out.println("  --max-transfers <number>     Set maximum number of transfers allowed (default: 5)");
                System.out.println("  --visualize                  Enable visualization of the pathfinding algorithm");
                System.exit(0);
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return cmdArgs;
    }
}