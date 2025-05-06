package algo.transit.services;

import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

public class CSVService {
    public final Path[] routesPaths, stopTimesPaths, stopsPaths, tripsPaths;

    public CSVService() {
        this(DefaultRoutesPaths, DefaultStopTimesPaths, DefaultStopsPaths, DefaultTripsPaths);
    }

    public CSVService(Path[] routesPaths, Path[] stopTimesPaths, Path[] stopsPaths, Path[] tripsPaths) {
        this.routesPaths = routesPaths;
        this.stopTimesPaths = stopTimesPaths;
        this.stopsPaths = stopsPaths;
        this.tripsPaths = tripsPaths;
    }

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

    public interface FromCSV<T> {
        T fromCSV(String[] row);
    }

    public static class CSVIterator<T> implements Iterator<T>, Iterable<T>, AutoCloseable {
        public final FromCSV<T> converter;
        public final CSVReader reader;
        public String[] next = null;

        public CSVIterator(@NotNull Path filePath, FromCSV<T> converter) throws IOException, CsvException {
            this.converter = converter;
            this.reader = new CSVReader(new BufferedReader(new FileReader(filePath.toString()), 8192 * 16));
            reader.readNextSilently(); // discard header
        }

        @Override
        public void close() throws Exception {
            reader.close();
        }

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            try {
                return (next = reader.readNext()) != null;
            } catch (IOException | CsvValidationException e) {
                throw new RuntimeException(e);
            }
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

    @Contract("_, _ -> new")
    public static <T> @NotNull Iterable<T> readCSV(Path filePath, FromCSV<T> converter) {
        try {
            return new CSVIterator<>(filePath, converter);
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<>();
        int routeCount = 0;

        for (Path path : routesPaths) {
            try {
                System.out.println("Reading routes from " + path.toString());
                for (Route route : readCSV(path, row -> new Route(row[0].intern(), row[1].intern(), row[2].intern(), row[3].intern()))) {
                    routes.put(route.routeId, route);
                    routeCount++;
                }
            } catch (Exception e) {
                System.err.println("Error reading routes from " + path + ": " + e.getMessage());
            }
        }

        System.out.println("Loaded " + routeCount + " routes");
        return routes;
    }

    public Map<String, Stop> getStops() {
        Map<String, Stop> stops = new HashMap<>();
        int stopCount = 0;

        for (Path path : stopsPaths) {
            try {
                System.out.println("Reading stops from " + path.toString());
                for (Stop stop : readCSV(path, row -> new Stop(row[0].intern(), row[1].intern(), Double.parseDouble(row[2]), Double.parseDouble(row[3])))) {
                    stops.put(stop.stopId, stop);
                    stopCount++;
                }
            } catch (Exception e) {
                System.err.println("Error reading stops from " + path + ": " + e.getMessage());
            }
        }

        System.out.println("Loaded " + stopCount + " stops");
        return stops;
    }

    public void linkData(Map<String, Stop> stops, Map<String, Trip> trips) {
        int stopTimeCount = 0;

        for (Path path : stopTimesPaths) {
            try {
                System.out.println("Reading stop times from " + path.toString());

                for (String[] row : readCSV(path, row -> row)) {
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
                        trip.addStopTime(stopSequence, departureTime, stop);
                        stopTimeCount++;

                        Route route = trip.getRoute();
                        if (route != null) {
                            route.possibleStops.add(stop);
                            stop.routes.put(route.routeId, route);
                            stop.trips.put(trip.tripId, trip);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error reading stop times from " + path + ": " + e.getMessage());
            }
        }

        System.out.println("Linked " + stopTimeCount + " stop times");
    }

    public Map<String, Trip> getTrips(Map<String, Route> routes) {
        Map<String, Trip> trips = new HashMap<>();
        int tripCount = 0;

        for (Path path : tripsPaths) {
            try {
                System.out.println("Reading trips from " + path.toString());

                for (Trip trip : readCSV(path, row -> {
                    Route route = routes.get(row[1]);
                    return new Trip(row[0], route);
                })) {
                    trips.put(trip.tripId, trip);
                    tripCount++;
                }
            } catch (Exception e) {
                System.err.println("Error reading trips from " + path + ": " + e.getMessage());
            }
        }

        System.out.println("Loaded " + tripCount + " trips");
        return trips;
    }

    public static LocalTime checkTime(@NotNull String time) {
        String[] timeArr = time.split(":");

        if (timeArr.length != 3) {
            throw new IllegalArgumentException("Invalid time format: " + time);
        }

        int hour = Integer.parseInt(timeArr[0]);
        int minute = Integer.parseInt(timeArr[1]);
        int second = Integer.parseInt(timeArr[2]);

        // Handle hour values > 23 properly
        if (hour > 23) {
            // For GTFS, hours can be >24 to represent service past midnight
            // We'll standardize to 0-23 for simplicity
            hour %= 24;
        }

        if (minute > 59) minute %= 60;
        if (second > 59) second %= 60;

        return LocalTime.of(hour, minute, second);
    }
}