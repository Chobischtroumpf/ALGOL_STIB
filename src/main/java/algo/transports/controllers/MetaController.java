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
    Map<String,Stop> stops;
    Map<String, StopTime> stopTimes;
    Map<String, Trip> trips;

    public MetaController() {
        CSVService csvService = new CSVService();
        this.routes = csvService.getRoutes();
        this.stops = csvService.getStops();
        this.stopTimes = csvService.getStopTimes();
        this.trips = csvService.getTrips();
    }

    public MetaController(Map<String, Route> routes, Map<String, Stop> stops, Map<String, StopTime> stopTimes, Map<String, Trip> trips) {
        this.routes = routes;
        this.stops = stops;
        this.stopTimes = stopTimes;
        this.trips = trips;
    }
}
