package algo.transports.controllers;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.StopTime;
import algo.transports.models.Trip;
import algo.transports.services.CSVService;

import java.util.List;
import java.util.Map;

public class MetaController {
    Map<String, Route> routes;
    Map<String, Stop> stops;
    Map<String, List<StopTime>> stopTimes;
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
        this.trips = csvService.getTrips();
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("trips: " + trips.size() + " (loaded in " + durationInSeconds + " seconds)");

        // Measure stopTimes loading time
        startTime = System.nanoTime();
        this.stopTimes = csvService.getStopTimes();
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("stopTimes: " + stopTimes.size() + " (loaded in " + durationInSeconds + " seconds)");
    }

    public MetaController(Map<String, Route> routes, Map<String, Stop> stops, Map<String, List<StopTime>> stopTimes, Map<String, Trip> trips) {
        this.routes = routes;
        this.stops = stops;
        this.stopTimes = stopTimes;
        this.trips = trips;
    }
}
