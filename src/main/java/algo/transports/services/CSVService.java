package algo.transports.services;

import java.nio.file.Path;
import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.StopTime;
import algo.transports.models.Trip;
import algo.transports.repositories.CSVRepository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVService {
    Path[] routes_paths;
    Path[] stop_times_paths;
    Path[] stops_paths;
    Path[] trips_paths;

    public CSVService(){
        this.routes_paths = new Path[4];
        this.stop_times_paths = new Path[4];
        this.stops_paths = new Path[4];
        this.trips_paths = new Path[4];
        initRoutesPaths();
        initStopTimesPaths();
        initStopsPaths();
        initTripsPaths();
    }

    public CSVService(Path[] routes_paths, Path[] stop_times_paths, Path[] stops_paths, Path[] trips_paths) {
        this.routes_paths = routes_paths;
        this.stop_times_paths = stop_times_paths;
        this.stops_paths = stops_paths;
        this.trips_paths = trips_paths;
    }

    private void initRoutesPaths() {
        routes_paths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "routes.csv");
        routes_paths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "routes.csv");
        routes_paths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "routes.csv");
        routes_paths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "routes.csv");
    }

    private void initStopTimesPaths() {
        stop_times_paths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "stop_times.csv");
        stop_times_paths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "stop_times.csv");
        stop_times_paths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "stop_times.csv");
        stop_times_paths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "stop_times.csv");
    }

    private void initStopsPaths() {
        stops_paths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "stops.csv");
        stops_paths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "stops.csv");
        stops_paths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "stops.csv");
        stops_paths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "stops.csv");
    }

    private void initTripsPaths() {
        trips_paths[0] = Path.of("src", "main", "resources", "GTFS", "DELIJN", "trips.csv");
        trips_paths[1] = Path.of("src", "main", "resources", "GTFS", "SNCB", "trips.csv");
        trips_paths[2] = Path.of("src", "main", "resources", "GTFS", "TEC", "trips.csv");
        trips_paths[3] = Path.of("src", "main", "resources", "GTFS", "STIB", "trips.csv");
    }

    public Map<String, Route> getRoutes() {
        Map<String, Route> routes = new HashMap<>();
        for (Path path : routes_paths) {
            List<String[]> data = CSVRepository.readCSV(path);
            assert data != null;
            for (String[] row : data) {
                Route route = new Route(row[0], row[1], row[2], row[3]);
                routes.put(row[0], route);
            }
        }
        return routes;
    }

    public Map<String,Stop> getStops() {
        Map<String, Stop> stops = new HashMap<>();
        for (Path path : stops_paths) {
            List<String[]> data = CSVRepository.readCSV(path);
            assert data != null;
            for (String[] row : data) {
                Stop stop = new Stop(row[0], row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]));
                stops.put(row[0], stop);
            }
        }
        return stops;
    }

    public Map<String, StopTime> getStopTimes() {
        Map<String, StopTime> stopTimes = new HashMap<>();
        for (Path path : stop_times_paths) {
            List<String[]> data = CSVRepository.readCSV(path);
            assert data != null;
            for (String[] row : data) {
                row[1] = checkTime(row[1]);

                StopTime stopTime = new StopTime(row[0], LocalTime.parse(row[1]), row[2], Integer.parseInt(row[3]));
                stopTimes.put(row[0], stopTime);
            }
        }
        return stopTimes;
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
        if (second.length() == 1){
            second = "0" + second;
        }

        return (hour + ":" + minute + ":" + second);
    }

    public Map<String, Trip> getTrips() {
        Map<String, Trip> trips = new HashMap<>();
        for (Path path : trips_paths) {
            List<String[]> data = CSVRepository.readCSV(path);
            assert data != null;
            for (String[] row : data) {
                Trip trip = new Trip(row[0], row[1]);
                trips.put(row[0], trip);
            }
        }
        return trips;
    }

}
