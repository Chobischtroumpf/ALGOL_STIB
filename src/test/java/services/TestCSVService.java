package services;

import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import algo.transit.services.CSVService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestCSVService {
    private CSVService csvService;

    @BeforeEach
    public void setUp() {
        // Create an instance of CSVService with test files
        csvService = new CSVService(
                new Path[]{Path.of("src", "test", "resources", "testRoutes.csv")},
                new Path[]{Path.of("src", "test", "resources", "testStopTimes.csv")},
                new Path[]{Path.of("src", "test", "resources", "testStops.csv")},
                new Path[]{Path.of("src", "test", "resources", "testTrips.csv")});
    }

    @Test
    public void testGetStops() {
        Map<String, Stop> stops = csvService.getStops();

        assertNotNull(stops, "The map of stops should not be null");
        assertFalse(stops.isEmpty(), "The map of stops should not be empty");

        Stop montgomeryStop = stops.get("STIB-0089");
        assertNotNull(montgomeryStop, "The Montgomery stop should exist");
        assertEquals("MONTGOMERY", montgomeryStop.getName(), "Stop name should be MONTGOMERY");
        assertEquals(50.838006, montgomeryStop.getLat(), 0.0001, "Latitude should match");
        assertEquals(4.40897, montgomeryStop.getLon(), 0.0001, "Longitude should match");
    }

    @Test
    public void testGetRoutes() {
        Map<String, Route> routes = csvService.getRoutes();

        assertNotNull(routes, "The map of routes should not be null");
        assertFalse(routes.isEmpty(), "The map of routes should not be empty");

        Route metroRoute = routes.get("STIB-1");
        assertNotNull(metroRoute, "The metro route should exist");
        assertEquals("1", metroRoute.getShortName(), "Route short name should be 1");
        assertEquals("GARE DE L'OUEST - STOCKEL", metroRoute.getLongName(), "Route long name should match");
        assertEquals("METRO", metroRoute.getType().toString(), "Route type should be METRO");
    }

    @Test
    public void testGetTrips() {
        Map<String, Route> routes = csvService.getRoutes();
        Map<String, Trip>  trips = csvService.getTrips(routes);

        assertNotNull(trips, "The map of trips should not be null");
        assertFalse(trips.isEmpty(), "The map of trips should not be empty");

        Trip busTrip = trips.get("STIB-124698409288866001");
        assertNotNull(busTrip, "The bus trip should exist");
        assertNotNull(busTrip.getRoute(), "The trip's route should not be null");
        assertEquals("STIB-15", busTrip.getRoute().getRouteId(), "Trip's route ID should match");
    }

    @Test
    public void testSetStopTimes() {
        Map<String, Stop>  stops = csvService.getStops();
        Map<String, Route> routes = csvService.getRoutes();
        Map<String, Trip>  trips = csvService.getTrips(routes);

        csvService.setStopTimes(stops, trips);

        Trip trip = trips.get("STIB-124698409288866001");
        assertNotNull(trip, "Trip should exist");
        assertFalse(trip.getStops().isEmpty(), "Trip should have stops after setting stop times");

        // For a valid stop in the stop times data
        // The test data has several references to stops that are not in the stops data,
        // so we check for at least one successful addition
        boolean hasValidStop = false;
        for (var entry : trip.getStops().entrySet()) {
            if (stops.containsKey(entry.getValue().getRight().getStopId())) {
                hasValidStop = true;
                break;
            }
        }

        assertTrue(hasValidStop, "At least one stop should be successfully added to the trip");
    }

    @Test
    public void testCleanupUnusedStops() {
        Map<String, Stop>  stops  = csvService.getStops();
        Map<String, Route> routes = csvService.getRoutes();

        int initialSize = stops.size();

        // Choose a stop and add a route to it to prevent it from being cleaned up
        Stop someStop = stops.values().iterator().next();
        Route someRoute = routes.values().iterator().next();
        someStop.addRoute(someRoute);

        int removedCount = csvService.cleanupUnusedStops(stops);

        assertEquals(initialSize - 1, removedCount,
                "One stop should remain and the rest should be counted as removed");
        assertTrue(stops.containsKey(someStop.getStopId()),
                "The stop with a route should not be removed");
    }
}