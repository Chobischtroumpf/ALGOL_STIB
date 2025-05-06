package algo.transit;

import algo.transit.enums.TransportType;
import algo.transit.models.Stop;
import algo.transit.models.TransitPreference;
import algo.transit.models.Transition;
import algo.transit.pathfinding.TransitPathfinder;
import algo.transit.services.CSVService;
import algo.transit.utils.QuadTree;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        try {
            CommandLineArgs cmdArgs = parseCommandLineArgs(args);
            CSVService csvService = new CSVService();

            // Load basic data first
            System.out.println("Finding path from " + cmdArgs.startStop + " to " + cmdArgs.endStop + " at " + cmdArgs.startTime);

            // Load graph data first to access stops
            TransitPathfinder pathfinder = new TransitPathfinder(csvService);

            // Get stops from the graph
            Stop startStop = pathfinder.graph.getStop(cmdArgs.startStop);
            Stop endStop = pathfinder.graph.getStop(cmdArgs.endStop);

            // Calculate the distance if both stops are found
            double distance = 0.0;
            if (startStop != null && endStop != null) {
                distance = QuadTree.distance(
                        startStop.latitude, startStop.longitude,
                        endStop.latitude, endStop.longitude
                );
                System.out.println("Distance between stops: " + distance + " meters");
            }

            // Create preferences with distance information
            TransitPreference preferences = createPreferences(cmdArgs, distance);

            long startTime = System.currentTimeMillis();
            List<Transition> path = pathfinder.findPath(cmdArgs.startStop, cmdArgs.endStop, cmdArgs.startTime, preferences);
            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("Execution time: " + (executionTime / 1000.0) + " seconds");
            pathfinder.printPath(path);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Usage: java -jar transit.jar START_STOP END_STOP START_TIME [OPTIONS]");
        }
    }

    public static TransitPreference createPreferences(CommandLineArgs args, double distance) {
        Map<TransportType, Double> modeWeights = new HashMap<>();

        // Set default weights based on distance
        if (distance > 50000) {
            System.out.println("Using long-distance weights (distance > 50km)");
            modeWeights.put(TransportType.FOOT, 3.0);    // Discourage walking
            modeWeights.put(TransportType.BUS, 2.0);     // Discourage buses for long trips
            modeWeights.put(TransportType.TRAIN, 0.7);   // Highly prefer trains
            modeWeights.put(TransportType.METRO, 0.9);   // Prefer metro
            modeWeights.put(TransportType.TRAM, 1.0);    // Normal tram weight
        } else if (distance > 10000) {
            System.out.println("Using medium-distance weights (10km < distance < 50km)");
            modeWeights.put(TransportType.FOOT, 2.0);    // Discourage walking but less so
            modeWeights.put(TransportType.BUS, 1.0);     // Normal bus weight
            modeWeights.put(TransportType.TRAIN, 0.8);   // Prefer trains
            modeWeights.put(TransportType.METRO, 0.8);   // Prefer metro
            modeWeights.put(TransportType.TRAM, 0.9);    // Slightly prefer trams
        } else {
            System.out.println("Using short-distance weights (distance < 10km)");
            modeWeights.put(TransportType.FOOT, 1.0);    // Walking is fine for short distances
            modeWeights.put(TransportType.BUS, 1.0);     // Normal bus weight
            modeWeights.put(TransportType.TRAIN, 1.2);   // Slightly discourage trains (overkill for short trips)
            modeWeights.put(TransportType.METRO, 0.9);   // Slightly prefer metro
            modeWeights.put(TransportType.TRAM, 0.8);    // Prefer trams for short trips
        }

        // Apply custom weights (override defaults with user preferences)
        for (Map.Entry<String, Double> entry : args.modeWeights.entrySet()) {
            try {
                modeWeights.put(TransportType.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid transport type: " + entry.getKey() + ". Skipping.");
            }
        }

        // Consider reducing max walk time for longer distances
        double adjustedMaxWalkTime = args.maxWalkTime;
        if (distance > 20000) {
            adjustedMaxWalkTime = Math.min(adjustedMaxWalkTime, 5.0); // Cap walking time for longer trips
            System.out.println("Adjusted max walk time to " + adjustedMaxWalkTime + " minutes for long trip");
        }

        return new TransitPreference(args.walkingSpeed, adjustedMaxWalkTime, modeWeights, args.forbiddenModes, args.modeSwitchPenalty);
    }

    public static class CommandLineArgs {
        public String startStop;
        public String endStop;
        public LocalTime startTime;
        public double walkingSpeed = 80.0;
        public double maxWalkTime = 10.0;
        public List<TransportType> forbiddenModes = new ArrayList<>();
        public Map<String, Double> modeWeights = new HashMap<>();
        public double modeSwitchPenalty = 5.0;
    }

    public static CommandLineArgs parseCommandLineArgs(String[] args) {
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
                            cmdArgs.modeWeights.put(parts[0], Double.parseDouble(parts[1]));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid weight format: " + weightSpec + ". Expected format: mode:weight");
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid transport type: " + parts[0] + ". Skipping.");
                        }
                    }
                }
            } else if (arg.equals("--mode-switch-penalty") && i + 1 < args.length) {
                cmdArgs.modeSwitchPenalty = Double.parseDouble(args[++i]);
            } else if (arg.equals("--help")) {
                System.out.println("Usage: java -jar transit.jar START_STOP END_STOP START_TIME [OPTIONS]");
                System.out.println("Options:");
                System.out.println("  --walking-speed <speed>       Set walking speed in meters per minute (default: 80.0)");
                System.out.println("  --max-walk-time <time>       Set maximum walking time in minutes (default: 10.0)");
                System.out.println("  --forbidden-modes <modes>    Set forbidden transport modes (e.g., BUS, TRAIN)");
                System.out.println("  --mode-weights <mode:weight> Set custom weights for transport modes");
                System.out.println("  --mode-switch-penalty <penalty> Set penalty for mode switching (default: 5.0)");
                System.exit(0);
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return cmdArgs;
    }
}