package algo.transit.controllers;

import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import algo.transit.services.CSVService;

import java.util.Map;


public class MetaController {
    private final Map<String, Route> routes;
    private final Map<String, Stop>  stops;
    private final Map<String, Trip>  trips;
    private final RouteController    routeController;

    public MetaController() {
        CSVService csvService = new CSVService();

        long startTime = System.nanoTime();
        this.routes = csvService.getRoutes();
        long endTime = System.nanoTime();
        double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Routes: " + routes.size() + "   (loaded in " + durationInSeconds + " seconds)");

        // Measure stops loading time
        startTime = System.nanoTime();
        this.stops = csvService.getStops();
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Stops:  " + stops.size() + "  (loaded in " + durationInSeconds + " seconds)");

        // Measure trips loading time
        startTime = System.nanoTime();
        this.trips = csvService.getTrips(this.routes);
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("Trips:  " + trips.size() + " (loaded in " + durationInSeconds + " seconds)");

        // Measure stopTimes loading time
        startTime = System.nanoTime();
        csvService.setStopTimes(stops, trips);
        endTime = System.nanoTime();
        durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("StopTimes loaded in " + durationInSeconds + " seconds");

        // Cleanup
        System.out.println("Cleaned up " + csvService.cleanupUnusedStops(stops) + " unused Stops");

        // Initialize the RouteController
        this.routeController = new RouteController(routes, trips);
    }

    public MetaController(Map<String, Route> routes, Map<String, Stop> stops, Map<String, Trip> trips) {
        this.routes = routes;
        this.stops  = stops;
        this.trips  = trips;
        this.routeController = new RouteController(routes, trips);
    }
}
