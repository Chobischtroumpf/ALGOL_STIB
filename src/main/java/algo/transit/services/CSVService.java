package algo.transit.services;

import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class CSVService {
    private final Path[] routesPaths, stopTimesPaths, stopsPaths, tripsPaths;

    public CSVService() {
        this(DefaultRoutesPaths, DefaultStopTimesPaths, DefaultStopsPaths, DefaultTripsPaths);
    }

    public CSVService(Path[] routesPaths, Path[] stopTimesPaths, Path[] stopsPaths, Path[] tripsPaths) {
        this.routesPaths = routesPaths;
        this.stopTimesPaths = stopTimesPaths;
        this.stopsPaths = stopsPaths;
        this.tripsPaths = tripsPaths;
    }

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

    interface FromCSV<T> {
        T fromCSV(String[] row);
    }

    private static class CSVIterator<T> implements Iterator<T>, Iterable<T>, AutoCloseable {
        private final FromCSV<T> converter;
        private final CSVReader reader;
        private String[] next = null;

        private CSVIterator(Path filePath, FromCSV<T> converter) throws IOException, CsvException {
            this.converter = converter;
            this.reader = new CSVReader(new BufferedReader(new FileReader(filePath.toString()), 8192 * 16));
            reader.readNextSilently(); // Discard header
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
        public T next() {
            if (next == null && !hasNext()) return null;
            String[] current = next;
            next = null;
            return converter.fromCSV(current);
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }
    }

    private static <T> Iterable<T> readCSV(Path filePath, FromCSV<T> converter) {
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
                routes.put(route.getId(), route);
        return routes;
    }

    public Map<String, Stop> getStops() {
        Map<String, Stop> stops = new HashMap<>();
        for (Path path : stopsPaths)
            for (Stop stop : readCSV(path, row -> new Stop(row[0], row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]))))
                stops.put(stop.getId(), stop);
        return stops;
    }

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
                    stop.addRoute(trip.getRoute());
                }
            }
        }
    }

    public Map<String, Trip> getTrips(Map<String, Route> routes) {
        Map<String, Trip> trips = new HashMap<>();
        for (Path path : tripsPaths)
            for (Trip trip : readCSV(path, row -> new Trip(row[0], routes.get(row[1]))))
                trips.put(trip.getId(), trip);
        return trips;
    }

    private static LocalTime checkTime(String time) {
        // Let's write a parser that check that the time is in the right format, and corrects it if not
        String[] timeArr = time.split(":");

        int hour    = Integer.parseInt(timeArr[0]);
        int minute  = Integer.parseInt(timeArr[1]);
        int second  = Integer.parseInt(timeArr[2]);

        if (hour > 23) {
            hour %= 24;
            // day += 1;
        }

        if (minute > 59) {
            minute %= 60;
            // hour += 1;
        }

        if (second > 59) {
            second %= 60;
            // minute += 1;
        }

        return LocalTime.of(hour, minute, second);
    }
}