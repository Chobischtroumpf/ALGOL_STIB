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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Service class for handling CSV files related to transit data.
 */
public class CSVService {
    private final Path[] routesPaths, stopTimesPaths, stopsPaths, tripsPaths;

    public CSVService() {
        this(DefaultRoutesPaths, DefaultStopTimesPaths, DefaultStopsPaths, DefaultTripsPaths);
    }

    /**
     * Constructor that allows custom file paths for routes, stop times, stops, and trips.
     *
     * @param routesPaths    Array of paths to the routes CSV files.
     * @param stopTimesPaths Array of paths to the stop times CSV files.
     * @param stopsPaths     Array of paths to the stops CSV files.
     * @param tripsPaths     Array of paths to the trips CSV files.
     */
    public CSVService(Path[] routesPaths, Path[] stopTimesPaths, Path[] stopsPaths, Path[] tripsPaths) {
        this.routesPaths = routesPaths;
        this.stopTimesPaths = stopTimesPaths;
        this.stopsPaths = stopsPaths;
        this.tripsPaths = tripsPaths;
    }

    // Default file paths
    private static final Path[] DefaultRoutesPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "routes.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "routes.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "routes.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "routes.csv")
    };

    private static final Path[] DefaultStopTimesPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "stop_times.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "stop_times.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "stop_times.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "stop_times.csv")
    };

    private static final Path[] DefaultStopsPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "stops.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "stops.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "stops.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "stops.csv")
    };

    private static final Path[] DefaultTripsPaths = new Path[]{
            Path.of("src", "main", "resources", "GTFS", "DELIJN", "trips.csv"),
            Path.of("src", "main", "resources", "GTFS", "SNCB", "trips.csv"),
            Path.of("src", "main", "resources", "GTFS", "TEC", "trips.csv"),
            Path.of("src", "main", "resources", "GTFS", "STIB", "trips.csv")
    };

    /**
     * Functional interface for converting a CSV row into an object of type T.
     *
     * @param <T> The type of object to be created from a CSV row.
     */
    interface FromCSV<T> { T fromCSV(String[] row); }

    /**
     * Iterator for reading and converting rows from a CSV file.
     *
     * @param <T> The type of object to be created from each row.
     */
    private static class CSVIterator<T> implements Iterator<T>, Iterable<T>, AutoCloseable {
        private final FromCSV<T> converter;
        private final CSVReader reader;
        private String[] next = null;

        /**
         * Constructs a CSVIterator for a given file and converter.
         *
         * @param filePath  Path to the CSV file.
         * @param converter Converter to transform rows into objects.
         * @throws IOException If an I/O error occurs.
         * @throws CsvException If a CSV parsing error occurs.
         */
        private CSVIterator(@NotNull Path filePath, FromCSV<T> converter) throws IOException, CsvException {
            this.converter = converter;
            this.reader = new CSVReader(new BufferedReader(new FileReader(filePath.toString()), 8192 * 16));
            reader.readNextSilently(); // discard header
        }

        @Override
        public void close() throws Exception { reader.close(); }

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

    /**
     * Reads a CSV file and converts its rows into objects of type T.
     *
     * @param filePath  Path to the CSV file.
     * @param converter Converter to transform rows into objects.
     * @param <T>       The type of object to be created.
     * @return An iterable of objects created from the CSV rows.
     */
    @Contract("_, _ -> new")
    private static <T> @NotNull Iterable<T> readCSV(Path filePath, FromCSV<T> converter) {
        try {
            return new CSVIterator<>(filePath, converter);
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<>();
        for (Path path : routesPaths)
            for (Route route : readCSV(path, row -> new Route(row[0], row[1], row[2], row[3])))
                routes.put(route.getRouteId(), route);
        return routes;
    }

    public Map<String, Stop> getStops() {
        Map<String, Stop> stops = new HashMap<>();
        for (Path path : stopsPaths)
            for (Stop stop : readCSV(path, row -> new Stop(row[0], row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]))))
                stops.put(stop.getStopId(), stop);
        return stops;
    }

    /**
     * Sets stop times for trips and associates stops with routes.
     *
     * @param stops A map of stop IDs to Stop objects.
     * @param trips A map of trip IDs to Trip objects.
     */
    public void setStopTimes(Map<String, Stop> stops, Map<String, Trip> trips) {
        for (Path path : stopTimesPaths) {
            for (String[] row : readCSV(path, row -> row)) {
                String tripId = row[0];
                String stopId = row[2];
                LocalTime departureTime = checkTime(row[1]);
                int stopSequence = Integer.parseInt(row[3]);

                Stop stop = stops.get(stopId);
                Trip trip = trips.get(tripId);

                if (stop == null) System.out.println("Stop not found: " + stopId);
                if (trip == null) System.out.println("Trip not found: " + tripId);

                if (stop != null && trip != null) {
                    trip.addStopTime(stopSequence, new ImmutablePair<>(departureTime, stop));

                    // Trips don't necessarily contain all the stops
                    // Sadly adds about 2-3 seconds to the loading time :(
                    Route route = trip.getRoute();
                    if (route != null) {
                        route.addPossibleStop(stop);
                        stop.addRoute(route);
                    }
                }
            }
        }
    }

    public Map<String, Trip> getTrips(Map<String, Route> routes) {
        Map<String, Trip> trips = new HashMap<>();
        for (Path path : tripsPaths)
            for (Trip trip : readCSV(path, row -> new Trip(row[0], routes.get(row[1]))))
                trips.put(trip.getTripId(), trip);
        return trips;
    }

    public int cleanupUnusedStops(@NotNull Map<String, Stop> stops) {
        List<String> badStops = new ArrayList<>();
        for (Map.Entry<String, Stop> entry : stops.entrySet()) {
            if (entry.getValue().getRoutes().isEmpty()) badStops.add(entry.getKey());
        }

        // Remove the unused stops
        for (String stopId : badStops) stops.remove(stopId);

        return badStops.size();
    }

    /**
     * Parses and validates a time string, correcting invalid values if necessary.
     *
     * @param time The time string in the format "HH:mm:ss".
     * @return A LocalTime object representing the parsed time.
     */
    private static LocalTime checkTime(@NotNull String time) {
        String[] timeArr = time.split(":");

        int hour    = Integer.parseInt(timeArr[0]);
        int minute  = Integer.parseInt(timeArr[1]);
        int second  = Integer.parseInt(timeArr[2]);

        if (hour > 23) {
            hour %= 24;
            // TODO: day += 1;
        }

        if (minute > 59) {
            minute %= 60;
            // TODO: hour += 1;     // shouldn't ever happen
        }

        if (second > 59) {
            second %= 60;
            // TODO: minute += 1;   // shouldn't ever happen
        }

        return LocalTime.of(hour, minute, second);
    }
}