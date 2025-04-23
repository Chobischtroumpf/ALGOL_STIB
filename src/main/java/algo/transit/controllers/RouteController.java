package algo.transit.controllers;

import algo.transit.models.Route;
import algo.transit.models.Trip;

import java.util.Map;

public class RouteController {
    private final Map<String, Route> routes;
    private final Map<String, Trip>  trips;

    public RouteController(Map<String, Route> routes, Map<String, Trip> trips) {
        this.routes = routes;
        this.trips = trips;
    }
}
