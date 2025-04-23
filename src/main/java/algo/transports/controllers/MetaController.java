package algo.transports.controllers;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.Trip;
import algo.transports.services.CSVService;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalTime;
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

        // Test line 71
        testLine71();
    }

    public MetaController(Map<String, Route> routes, Map<String, Stop> stops, Map<String, Trip> trips) {
        this.routes = routes;
        this.stops = stops;
        this.trips = trips;
    }

    private void testLine71() {
        System.out.println("\n--- TESTING LINE 71 (STIB) ---");

        // Find Route for line 71
        Route line71 = null;
        for (Route route : routes.values()) {
            if (route.getShortName().equals("71") && route.getRouteID().startsWith("STIB-")) {
                line71 = route;
                System.out.println("Found Route 71: " + route);
                break;
            }
        }

        if (line71 == null) {
            System.out.println("Line 71 from STIB not found!");
            return;
        }

        // Find all trips for this route
        List<Trip> line71Trips = new ArrayList<>();
        for (Trip trip : trips.values()) {
            try {
                if (trip.getRoute().equals(line71)) {
                    line71Trips.add(trip);
                }
            } catch (Exception e) {
                // Skip problematic trips
            }
        }

        System.out.println("Found " + line71Trips.size() + " trips for Line 71");

        // Take the first trip and print details
        if (!line71Trips.isEmpty()) {
            Trip firstTrip = line71Trips.getFirst();
            System.out.println("\nTrip ID: " + firstTrip.getTripId());

            List<Map.Entry<Integer, Pair<LocalTime, Stop>>> orderedStops = firstTrip.getOrderedStopTimes();

            if (orderedStops.isEmpty()) {
                System.out.println("No stops found for this trip.");
                return;
            }

            // First stop
            Map.Entry<Integer, Pair<LocalTime, Stop>> firstEntry = orderedStops.getFirst();
            LocalTime firstTime = firstEntry.getValue().getLeft();
            Stop firstStop = firstEntry.getValue().getRight();

            System.out.println("Starts at: " + firstStop.getName() + " at " + firstTime);

            // Process each subsequent stop
            for (int i = 1; i < orderedStops.size(); i++) {
                Map.Entry<Integer, Pair<LocalTime, Stop>> entry = orderedStops.get(i);
                Map.Entry<Integer, Pair<LocalTime, Stop>> prevEntry = orderedStops.get(i - 1);

                int sequence = entry.getKey();
                LocalTime time = entry.getValue().getLeft();
                Stop stop = entry.getValue().getRight();

                LocalTime prevTime = prevEntry.getValue().getLeft();
                long minutesDiff = java.time.temporal.ChronoUnit.MINUTES.between(prevTime, time);

                System.out.println("Stop " + sequence + ": " + stop.getName()
                        + " at " + time + " (+" + minutesDiff + " min)");
            }

            // Total journey time
            Map.Entry<Integer, Pair<LocalTime, Stop>> lastEntry = orderedStops.getLast();
            LocalTime lastTime = lastEntry.getValue().getLeft();
            long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(firstTime, lastTime);

            System.out.println("Total journey time: " + totalMinutes + " minutes");
        }
    }
}
