package algo.transports.controllers;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.Trip;
import algo.transports.services.CSVService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetaController {
    Map<String, Route>  routes;
    Map<String, Stop>   stops;
    Map<String, Trip>   trips;

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

        // TODO: Cleanup bad stops
        System.out.println("Stops with no routes:");
        List<String> badStops = new ArrayList<>();
        for (Map.Entry<String, Stop> entry : stops.entrySet()) {
            if (entry.getValue().getRoutes().isEmpty()) {
                badStops.add(entry.getKey());
                // System.out.println("Stop with no routes: " + entry.getValue());
            }
        }

        // TODO: Better cleanup
        for (String stopId : badStops) {
            stops.remove(stopId);
        }
        System.out.println("Removed " + badStops.size() + " stops with no routes");
    }

    public MetaController(Map<String, Route> routes, Map<String, Stop> stops, Map<String, Trip> trips) {
        this.routes = routes;
        this.stops = stops;
        this.trips = trips;
    }
}
