package algo.transit.controllers;

import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import algo.transit.services.CSVService;

import java.time.LocalTime;
import java.util.Map;


public class MetaController {
    private final Map<String, Route> routes;
    private final Map<String, Stop>  stops;
    private final Map<String, Trip>  trips;

    public MetaController(double walkingSpeed, double maxWalkingTime) {
        CSVService csvService = new CSVService();

        System.out.println("Initializing transit system...");
        System.out.println("---------------------------------");

        // Load data from CSV files
        long startLoadTime = System.nanoTime();
        this.routes = csvService.getRoutes();
        long endLoadTime = System.nanoTime();
        double loadDurationInSeconds = (endLoadTime - startLoadTime) / 1_000_000_000.0;
        System.out.println("Loaded " + routes.size() + "   routes: " + loadDurationInSeconds + " s");

        long startStopTime = System.nanoTime();
        this.stops = csvService.getStops();
        long endStopTime = System.nanoTime();
        double stopDurationInSeconds = (endStopTime - startStopTime) / 1_000_000_000.0;
        System.out.println("Loaded " + stops.size() + "  stops:  " + stopDurationInSeconds + " s");

        long startTripTime = System.nanoTime();
        this.trips = csvService.getTrips(this.routes);
        long endTripTime = System.nanoTime();
        double tripDurationInSeconds = (endTripTime - startTripTime) / 1_000_000_000.0;
        System.out.println("Loaded " + trips.size() + " trips:  " + tripDurationInSeconds + " s");

        long startStopTimesTime = System.nanoTime();
        csvService.setStopTimes(stops, trips);
        long endStopTimesTime = System.nanoTime();
        double stopTimesDurationInSeconds = (endStopTimesTime - startStopTimesTime) / 1_000_000_000.0;
        System.out.println("Set stop times:       " + stopTimesDurationInSeconds + " s");

        // System.out.println("Cleaned up " + csvService.cleanupUnusedStops(stops) + " unused stops");
        System.out.println("---------------------------------");


        Stop stop = stops.get("STIB-5213");
        System.out.println("Trip count before filter: " + stop.getTrips().size());

        LocalTime time = LocalTime.of(10, 0);
        Map<String, Trip> filteredTrips = stop.getRelevantTrips(time);

        System.out.println(filteredTrips.size() + " trips after filter");
    }
}
