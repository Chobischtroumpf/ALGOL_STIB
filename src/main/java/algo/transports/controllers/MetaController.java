package algo.transports.controllers;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.Trip;
import algo.transports.services.CSVService;

import java.util.Map;

public class MetaController {
    Map<String, Route> routes;
    Map<String, Stop> stops;
//    Map<String, List<StopTime>> stopTimes;
    Map<String, Trip> trips;

    public MetaController() {
        CSVService csvService = new CSVService();

        long startTime = System.nanoTime();
        this.routes = csvService.getRoutes();
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("routes: " + routes.size() + " (loaded in " + durationInSeconds + " seconds)");

        // Measure stops loading time
        startTime = System.nanoTime();
        this.stops = csvService.getStops();
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("stops: " + stops.size() + " (loaded in " + durationInSeconds + " seconds)");

        // Measure trips loading time
        startTime = System.nanoTime();
        this.trips = csvService.getTrips(this.routes);
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("trips: " + trips.size() + " (loaded in " + durationInSeconds + " seconds)");

        // Measure stopTimes loading time
        startTime = System.nanoTime();
        csvService.setStopTimes(stops, trips);
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("stopTimes loaded in " + durationInSeconds + " seconds");

        // print the first 10 routes
        System.out.println("First 10 routes:");
        int count = 0;
        for (Map.Entry<String, Route> entry : routes.entrySet()) {
            System.out.println(entry.getValue());
            count++;
            if (count >= 10) {
                break;
            }
        }

        // print the first 10 trips
        System.out.println("First 10 trips:");
        count = 0;
        for (Map.Entry<String, Trip> entry : trips.entrySet()) {
            System.out.println(entry.getValue());
            count++;
            if (count >= 10) {
                break;
            }
        }

        // print the first 10 stops
        System.out.println("First 10 stops:");
        count = 0;
        for (Map.Entry<String, Stop> entry : stops.entrySet()) {
            System.out.println(entry.getValue());
            count++;
            if (count >= 10) {
                break;
            }
        }
    }

    public MetaController(Map<String, Route> routes, Map<String, Stop> stops, Map<String, Trip> trips) {
        this.routes = routes;
        this.stops = stops;
        this.trips = trips;
    }
}
