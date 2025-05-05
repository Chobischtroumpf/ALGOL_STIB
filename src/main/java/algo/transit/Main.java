package algo.transit;

import algo.transit.enums.TransportType;
import algo.transit.models.TransitPreference;
import algo.transit.models.Transition;
import algo.transit.pathfinding.TransitPathfinder;
import algo.transit.services.CSVService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        try {
            CommandLineArgs cmdArgs = parseCommandLineArgs(args);
            TransitPreference preferences = createPreferences(cmdArgs);
            CSVService csvService = new CSVService();
            TransitPathfinder pathfinder = new TransitPathfinder(csvService);

            if (cmdArgs.maxIterations > 0) pathfinder.setMaxIterations(cmdArgs.maxIterations);
            if (cmdArgs.maxFrontierSize > 0) pathfinder.setMaxFrontierSize(cmdArgs.maxFrontierSize);

            System.out.println("Finding path from " + cmdArgs.startStop + " to " + cmdArgs.endStop + " at " + cmdArgs.startTime);

            long startTime = System.currentTimeMillis();
            List<Transition> path = pathfinder.findPath(cmdArgs.startStop, cmdArgs.endStop, cmdArgs.startTime, preferences);
            long executionTime = System.currentTimeMillis() - startTime;

            System.out.println("Execution time: " + (executionTime / 1000.0) + " seconds");
            pathfinder.printPath(path);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static TransitPreference createPreferences(CommandLineArgs args) {
        Map<TransportType, Double> modeWeights = new HashMap<>();

        // Set default weights
        modeWeights.put(TransportType.FOOT, 1.0);
        modeWeights.put(TransportType.BUS, 1.0);
        modeWeights.put(TransportType.TRAIN, 1.0);
        modeWeights.put(TransportType.METRO, 1.0);
        modeWeights.put(TransportType.TRAM, 1.0);

        // Apply custom weights
        for (Map.Entry<String, Double> entry : args.modeWeights.entrySet()) {
            try {
                modeWeights.put(TransportType.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException e) {
            }
        }

        return new TransitPreference(args.walkingSpeed, args.maxWalkTime, modeWeights, args.forbiddenModes);
    }

    public static class CommandLineArgs {
        public String startStop;
        public String endStop;
        public LocalTime startTime;
        public double walkingSpeed = 80.0;
        public double maxWalkTime = 10.0;
        public List<TransportType> forbiddenModes = new ArrayList<>();
        public Map<String, Double> modeWeights = new HashMap<>();
        public int maxIterations = 50000000;
        public int maxFrontierSize = 100000000;
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
                        }
                    }
                }
            } else if (arg.equals("--max-iterations") && i + 1 < args.length) {
                cmdArgs.maxIterations = Integer.parseInt(args[++i]);
            } else if (arg.equals("--max-frontier-size") && i + 1 < args.length) {
                cmdArgs.maxFrontierSize = Integer.parseInt(args[++i]);
            }
        }

        return cmdArgs;
    }
}