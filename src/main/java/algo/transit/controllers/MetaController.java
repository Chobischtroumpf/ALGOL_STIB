package algo.transit.controllers;

import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import algo.transit.services.CSVService;

import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class MetaController {
    public static double EARTH_RAD = 6371.0;
    private final double walkingSpeed;
    private final double maxWalkingTime;

    private final Map<String, Route> routes;
    private final Map<String, Stop>  stops;
    private final Map<String, Trip>  trips;

    public MetaController(double walkingSpeed, double maxWalkingTime) {
        this.walkingSpeed   = walkingSpeed;
        this.maxWalkingTime = maxWalkingTime;

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

        // long startLinkTime = System.nanoTime();
        // csvService.linkData(stops, trips);
        // long endLinkTime = System.nanoTime();
        // double stopTimesDurationInSeconds = (endLinkTime - startLinkTime) / 1_000_000_000.0;
        // System.out.println("Linked data:          " + stopTimesDurationInSeconds + " s");

        // System.out.println("Cleaned up " + csvService.cleanupUnusedStops(stops) + " unused stops");
        System.out.println("---------------------------------");
    }

    public boolean isWithinWalkingDistance(@NotNull Stop stop1, Stop stop2) {
        return calculateDistance(stop1, stop2) < (walkingSpeed * maxWalkingTime) / 1000;
    }

    /// Haversine formula to calculate the distance
    public double calculateDistance(@NotNull Stop stop1, @NotNull Stop stop2) {
        double distance;

        double lat1 = stop1.getLat();
        double lon1 = stop1.getLon();
        double lat2 = stop2.getLat();
        double lon2 = stop2.getLon();

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // sqrt( sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2) )
        double sqrSinDLat = Math.pow(Math.sin(dLat / 2), 2);
        double sqrSinDLon = Math.pow(Math.sin(dLon / 2), 2);
        double cosLat1 = Math.cos(Math.toRadians(lat1));
        double cosLat2 = Math.cos(Math.toRadians(lat2));

        double a = Math.sqrt(sqrSinDLat + cosLat1 * cosLat2 * sqrSinDLon);
        distance = 2 * EARTH_RAD * Math.asin(a);

        return distance;
    }

    public boolean isInBox(double @NotNull [] box, @NotNull Stop stop) {
        if ((stop.getLat() > box[0]) && (stop.getLat() < box[1])) // check latitude
            return ((stop.getLon() > box[2]) && (stop.getLon() < box[3]));
        return false;
    }
}
