package algo.transports.services;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.Trip;
import algo.transports.repositories.CSVRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVService {
    Path[] routesPaths;
    Path[] stopTimesPaths;
    Path[] stopsPaths;
    Path[] tripsPaths;

    public CSVService() {
        this.routesPaths = new Path[4];
        this.stopTimesPaths = new Path[4];
        this.stopsPaths = new Path[4];
        this.tripsPaths = new Path[4];
        initRoutesPaths();
        initStopTimesPaths();
        initStopsPaths();
        initTripsPaths();
    }

    public CSVService(Path[] routesPaths, Path[] stopTimesPaths, Path[] stopsPaths, Path[] tripsPaths) {
        this.routesPaths = routesPaths;
        this.stopTimesPaths = stopTimesPaths;
        this.stopsPaths = stopsPaths;
        this.tripsPaths = tripsPaths;
    }

    private void initRoutesPaths() {
        routesPaths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "routes.csv");
        routesPaths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "routes.csv");
        routesPaths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "routes.csv");
        routesPaths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "routes.csv");
    }

    private void initStopTimesPaths() {
        stopTimesPaths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "stop_times.csv");
        stopTimesPaths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "stop_times.csv");
        stopTimesPaths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "stop_times.csv");
        stopTimesPaths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "stop_times.csv");
    }

    private void initStopsPaths() {
        stopsPaths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "stops.csv");
        stopsPaths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "stops.csv");
        stopsPaths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "stops.csv");
        stopsPaths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "stops.csv");
    }

    private void initTripsPaths() {
        tripsPaths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "trips.csv");
        tripsPaths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "trips.csv");
        tripsPaths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "trips.csv");
        tripsPaths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "trips.csv");
    }

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<>();
        for (Path path : routesPaths) {
            List<String[]> data = CSVRepository.readCSV(path);
            if (data != null) {
                for (String[] row : data) {
                    Route route = new Route(row[0], row[1], row[2], row[3]);
                    routes.put(row[0], route);
                }
            }
        }
        return routes;
    }

    public Map<String, Stop> getStops() {
        Map<String, Stop> stops = new HashMap<>();
        for (Path path : stopsPaths) {
            List<String[]> data = CSVRepository.readCSV(path);
            if (data != null) {
                for (String[] row : data) {
                    Stop stop = new Stop(row[0], row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]));
                    stops.put(row[0], stop);
                }
            }
        }
        return stops;
    }

    public void setStopTimes(Map<String, Stop> stops, Map<String, Trip> trips) {

        for (Path path : stopTimesPaths) {
            List<String[]> data = CSVRepository.readCSV(path);
            if (data != null) {
                for (String[] row : data) {
                    String tripId = row[0];
                    String stopId = row[2];
                    String departureTime = checkTime(row[1]);
                    int stopSequence = Integer.parseInt(row[3]);

                    Stop stop = stops.get(stopId);
                    Trip trip = trips.get(tripId);
                    if (stop == null) {
                        System.out.println("Stop not found: " + stopId);
                    }
                    if (trip == null) {
                        System.out.println("Trip not found: " + tripId);
                    }
                    if (stop != null && trip != null) {
                        trip.addStopTime(stopSequence, new ImmutablePair<>(LocalTime.parse(departureTime), stop));

                        stop.addRoute(trip.getRoute());
                    }
                }
            }
        }
    }

    public Map<String, Trip> getTrips(Map<String, Route> routes) {
        Map<String, Trip> trips = new HashMap<>();
        for (Path path : tripsPaths) {
            List<String[]> data = CSVRepository.readCSV(path);
            if (data != null) {
                for (String[] row : data) {
                    Trip trip = new Trip(row[0], routes.get(row[1]));
                    trips.put(row[0], trip);
                }
            }
        }
        return trips;
    }

    private String checkTime(String time) {
        // Let's write a parser that check that the time is in the right format,
        // and corrects it if not
        String[] timeArr = time.split(":");
        String hour = timeArr[0];
        String minute = timeArr[1];
        String second = timeArr[2];

        int h = Integer.parseInt(hour);
        int m = Integer.parseInt(minute);
        int s = Integer.parseInt(second);
        if (h > 23) {
            // modulo 24
            hour = String.valueOf(h % 24);
        }
        if (m > 59) {
            // modulo 60
            minute = String.valueOf(m % 60);
        }
        if (s > 59) {
            // modulo 60
            second = String.valueOf(s % 60);
        }

        if (hour.length() == 1) {
            hour = "0" + hour;
        }
        if (minute.length() == 1) {
            minute = "0" + minute;
        }
        if (second.length() == 1) {
            second = "0" + second;
        }

        return (hour + ":" + minute + ":" + second);
    }
}