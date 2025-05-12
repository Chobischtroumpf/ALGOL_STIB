package algo.transit.services;

import algo.transit.models.common.Route;
import algo.transit.models.common.Stop;
import algo.transit.models.common.Trip;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CSVService {
    public static final Path[] DefaultRoutesPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "routes.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "routes.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "routes.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "routes.csv")
    };
    public static final Path[] DefaultStopTimesPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "stop_times.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "stop_times.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "stop_times.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "stop_times.csv")
    };
    public static final Path[] DefaultStopsPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "stops.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "stops.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "stops.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "stops.csv")
    };
    public static final Path[] DefaultTripsPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "trips.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "trips.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "trips.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "trips.csv")
    };
    public final Path[] routesPaths, stopTimesPaths, stopsPaths, tripsPaths;

    public CSVService() {
        this(DefaultRoutesPaths, DefaultStopTimesPaths, DefaultStopsPaths, DefaultTripsPaths);
    }

    public CSVService(
            Path[] routesPaths,
            Path[] stopTimesPaths,
            Path[] stopsPaths,
            Path[] tripsPaths
    ) {
        this.routesPaths = routesPaths;
        this.stopTimesPaths = stopTimesPaths;
        this.stopsPaths = stopsPaths;
        this.tripsPaths = tripsPaths;
    }

    @Contract("_, _ -> new")
    public static <T> @NotNull Iterable<T> readCSV(
            Path filePath,
            FromCSV<T> converter
    ) {
        try {
            return new CSVIterator<>(filePath, converter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalTime checkTime(@NotNull String time) {
        int hour = (time.charAt(0) - '0') * 10 + (time.charAt(1) - '0');
        int minute = (time.charAt(3) - '0') * 10 + (time.charAt(4) - '0');
        int second = (time.charAt(6) - '0') * 10 + (time.charAt(7) - '0');

        if (hour > 23) hour %= 24;
        if (minute > 59) minute %= 60;
        if (second > 59) second %= 60;

        return LocalTime.of(hour, minute, second);
    }

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<>();

        List<CompletableFuture<Void>> futures = Arrays.stream(routesPaths)
                .map(path -> CompletableFuture.runAsync(() -> {
                    try {
                        System.out.println("Reading routes from " + path);
                        for (Route route : readCSV(path, row -> new Route(row[0].intern(), row[1].intern(), row[2].intern(), row[3].intern()))) {
                            synchronized (routes) {
                                routes.put(route.routeId, route);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading routes from " + path + ": " + e.getMessage());
                    }
                }))
                .toList();

        futures.forEach(CompletableFuture::join);
        System.out.println("Loaded " + routes.size() + " routes");
        return routes;
    }

    public Map<String, Stop> getStops() {
        Map<String, Stop> stops = new HashMap<>();

        List<CompletableFuture<Void>> futures = Arrays.stream(stopsPaths)
                .map(path -> CompletableFuture.runAsync(() -> {
                    try {
                        System.out.println("Reading stops from " + path);
                        for (Stop stop : readCSV(path, row -> new Stop(row[0].intern(), row[1].intern(), Double.parseDouble(row[2]), Double.parseDouble(row[3])))) {
                            synchronized (stops) {
                                stops.put(stop.stopId, stop);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading stops from " + path + ": " + e.getMessage());
                    }
                }))
                .toList();

        futures.forEach(CompletableFuture::join);
        System.out.println("Loaded " + stops.size() + " stops");
        return stops;
    }

    public void linkData(
            Map<String, Stop> stops,
            Map<String, Trip> trips
    ) {
        List<CompletableFuture<Integer>> futures = Arrays.stream(stopTimesPaths)
                .map(path -> CompletableFuture.supplyAsync(() -> {
                    int count = 0;
                    try {
                        System.out.println("Reading stop times from " + path);
                        for (String[] row : readCSV(path, r -> r)) {
                            if (row.length < 4) {
                                System.err.println("Invalid stop time row: " + Arrays.toString(row));
                                continue;
                            }

                            String tripId = row[0];
                            String stopId = row[2];

                            LocalTime departureTime;
                            try {
                                departureTime = checkTime(row[1]);
                            } catch (Exception e) {
                                System.err.println("Invalid time format in stop_times: " + row[1]);
                                continue;
                            }

                            int stopSequence;
                            try {
                                stopSequence = Integer.parseInt(row[3]);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid stop sequence: " + row[3]);
                                continue;
                            }

                            Stop stop = stops.get(stopId);
                            Trip trip = trips.get(tripId);

                            if (stop != null && trip != null) {
                                synchronized (trip) {
                                    trip.addStopTime(stopSequence, departureTime, stop);
                                }
                                synchronized (trip.getRoute()) {
                                    trip.getRoute().possibleStops.add(stop);
                                }
                                synchronized (stop) {
                                    stop.routes.put(trip.getRoute().routeId, trip.getRoute());
                                    stop.trips.put(trip.tripId, trip);
                                }
                                count++;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading stop times from " + path + ": " + e.getMessage());
                    }
                    return count;
                }))
                .toList();

        int totalCount = futures.stream().mapToInt(CompletableFuture::join).sum();
        System.out.println("Linked " + totalCount + " stop times");
    }

    public Map<String, Trip> getTrips(Map<String, Route> routes) {
        Map<String, Trip> trips = new HashMap<>();

        List<CompletableFuture<Void>> futures = Arrays.stream(tripsPaths)
                .map(path -> CompletableFuture.runAsync(() -> {
                    try {
                        System.out.println("Reading trips from " + path);
                        for (Trip trip : readCSV(path, row -> {
                            Route route = routes.get(row[1]);
                            return new Trip(row[0], route);
                        })) {
                            synchronized (trips) {
                                trips.put(trip.tripId, trip);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading trips from " + path + ": " + e.getMessage());
                    }
                }))
                .toList();

        futures.forEach(CompletableFuture::join);
        System.out.println("Loaded " + trips.size() + " trips");
        return trips;
    }


    public interface FromCSV<T> {
        T fromCSV(String[] row);
    }

    public static class CSVIterator<T> implements Iterator<T>, Iterable<T>, AutoCloseable {
        private final FromCSV<T> converter;
        private final CsvParser parser;
        private final BufferedReader reader;
        private String[] next = null;

        public CSVIterator(
                @NotNull Path filePath,
                FromCSV<T> converter
        ) throws IOException {
            this.converter = converter;
            this.reader = new BufferedReader(new FileReader(filePath.toString()), 8192 * 64);

            // Configure parser settings
            CsvParserSettings settings = new CsvParserSettings();
            settings.setLineSeparatorDetectionEnabled(true);
            settings.setHeaderExtractionEnabled(true);  // Skip header row
            settings.setMaxCharsPerColumn(-1);  // No limit on column length

            // Create parser with settings
            this.parser = new CsvParser(settings);
            this.parser.beginParsing(reader);
        }

        @Override
        public void close() throws Exception {
            parser.stopParsing();
            reader.close();
        }

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            next = parser.parseNext();
            return next != null;
        }

        @Override
        public @Nullable T next() {
            if (next == null && !hasNext()) return null;
            String[] current = next;
            next = null;
            return converter.fromCSV(current);
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return this;
        }
    }
}