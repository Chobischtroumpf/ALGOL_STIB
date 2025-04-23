package controllers;

import algo.transit.controllers.GraphController;
import algo.transit.models.graph.PathSegment;
import algo.transit.models.Route;
import algo.transit.models.Stop;
import algo.transit.models.Trip;
import algo.transit.models.graph.Edge;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestGraphController {
    // Test data
    private Map<String, Stop>  stops;
    private Map<String, Trip>  trips;
    private Map<String, Route> routes;
    private GraphController    graphController;

    @BeforeEach
    public void setUp() {
        stops  = createTestStops();
        routes = createTestRoutes();
        trips  = createTestTrips(routes);
        setUpStopTimes(stops, trips);
        graphController = new GraphController(stops, trips, routes);
    }

    private @NotNull Map<String, Stop> createTestStops() {
        Map<String, Stop> testStops = new HashMap<>();

        Stop stop1 = new Stop("STOP1", "Test Stop 1", 50.8, 4.3);
        Stop stop2 = new Stop("STOP2", "Test Stop 2", 50.82, 4.32);
        Stop stop3 = new Stop("STOP3", "Test Stop 3", 50.83, 4.33);
        Stop stop4 = new Stop("STOP4", "Test Stop 4", 50.85, 4.35);

        testStops.put(stop1.getStopId(), stop1);
        testStops.put(stop2.getStopId(), stop2);
        testStops.put(stop3.getStopId(), stop3);
        testStops.put(stop4.getStopId(), stop4);

        return testStops;
    }

    private @NotNull Map<String, Route> createTestRoutes() {
        Map<String, Route> testRoutes = new HashMap<>();

        Route route1 = new Route("ROUTE1", "1", "Test Route 1", "BUS");
        Route route2 = new Route("ROUTE2", "2", "Test Route 2", "TRAM");

        testRoutes.put(route1.getRouteId(), route1);
        testRoutes.put(route2.getRouteId(), route2);

        return testRoutes;
    }

    private @NotNull Map<String, Trip> createTestTrips(@NotNull Map<String, Route> routes) {
        Map<String, Trip> testTrips = new HashMap<>();

        Trip trip1 = new Trip("TRIP1", routes.get("ROUTE1"));
        Trip trip2 = new Trip("TRIP2", routes.get("ROUTE2"));

        testTrips.put(trip1.getTripId(), trip1);
        testTrips.put(trip2.getTripId(), trip2);

        return testTrips;
    }

    private void setUpStopTimes(@NotNull Map<String, Stop> stops, @NotNull Map<String, Trip> trips) {
        Trip trip1 = trips.get("TRIP1");
        Trip trip2 = trips.get("TRIP2");

        // Trip 1: STOP1 -> STOP2 -> STOP3
        trip1.addStopTime(1, new ImmutablePair<>(LocalTime.of(8, 0), stops.get("STOP1")));
        trip1.addStopTime(2, new ImmutablePair<>(LocalTime.of(8, 15), stops.get("STOP2")));
        trip1.addStopTime(3, new ImmutablePair<>(LocalTime.of(8, 30), stops.get("STOP3")));

        // Trip 2: STOP2 -> STOP4
        trip2.addStopTime(1, new ImmutablePair<>(LocalTime.of(9, 0), stops.get("STOP2")));
        trip2.addStopTime(2, new ImmutablePair<>(LocalTime.of(9, 20), stops.get("STOP4")));

        Route route1 = trip1.getRoute();
        Route route2 = trip2.getRoute();

        stops.get("STOP1").addRoute(route1);
        route1.addPossibleStop(stops.get("STOP1"));

        stops.get("STOP2").addRoute(route1);
        route1.addPossibleStop(stops.get("STOP2"));
        stops.get("STOP2").addRoute(route2);
        route2.addPossibleStop(stops.get("STOP2"));

        stops.get("STOP3").addRoute(route1);
        route1.addPossibleStop(stops.get("STOP3"));

        stops.get("STOP4").addRoute(route2);
        route2.addPossibleStop(stops.get("STOP4"));
    }

    @Test
    public void testBuildGraph() {
        double walkingSpeed = 80.0;   // meters per minute
        double maxWalkingTime = 30.0; // minutes
        assertDoesNotThrow(() -> graphController.buildGraph(walkingSpeed, maxWalkingTime));
    }

    @Test
    public void testGraphStructure() throws Exception {
        graphController.buildGraph(80.0, 30.0);

        // Use reflection to access the private adjacencyList field
        Field adjacencyListField = GraphController.class.getDeclaredField("adjacencyList");
        adjacencyListField.setAccessible(true);
        Map<Stop, List<Edge>> adjacencyList =
                (Map<Stop, List<Edge>>) adjacencyListField.get(graphController);

        // Verify each stop has an entry in the adjacency list
        for (Stop stop : stops.values()) {
            assertTrue(adjacencyList.containsKey(stop),
                    "Adjacency list should contain an entry for each stop");

            // Each stop should have at least some edges
            // (either transit edges or walking edges)
        }

        // Check if transit edges were created
        // For example, stop1 should have an edge to stop2 in trip1
        Stop stop1 = stops.get("STOP1");
        boolean hasEdgeToStop2 = false;

        for (Edge edge : adjacencyList.get(stop1)) {
            if (edge.getTo().equals(stops.get("STOP2"))) {
                hasEdgeToStop2 = true;
                break;
            }
        }

        assertTrue(hasEdgeToStop2, "Stop1 should have an edge to Stop2");
    }

    @Test
    public void testFindShortestPath() {
        graphController.buildGraph(80.0, 30.0);

        // The current implementation returns an empty list
        // TODO: Implement the findShortestPath method
        Stop fromStop = stops.get("STOP1");
        Stop toStop = stops.get("STOP4");
        LocalTime departureTime = LocalTime.of(8, 0);

        List<PathSegment> path = graphController.findShortestPath(fromStop, toStop, departureTime);

        // Since the method is not implemented yet, we expect an empty list
        assertNotNull(path, "Path should not be null");
        assertTrue(path.isEmpty(), "Path should be empty since the method is not implemented");
    }

    @Test
    public void testCalculateDistance() throws Exception {
        // Use reflection to access the private calculateDistance method
        java.lang.reflect.Method calculateMethod = GraphController.class.getDeclaredMethod(
                "calculateDistance", Stop.class, Stop.class);
        calculateMethod.setAccessible(true);

        Stop stop1 = stops.get("STOP1");
        Stop stop2 = stops.get("STOP2");

        double distance = (double) calculateMethod.invoke(graphController, stop1, stop2);
        double expectedDistance = getExpectedDistance(stop1, stop2);

        assertEquals(expectedDistance, distance, 0.1,
                "The distance calculation should match the haversine formula");
        assertTrue(distance > 0, "Distance should be positive");
        assertTrue(distance < 10000, "Distance should be less than 10km for our test stops");
    }

    private static double getExpectedDistance(@NotNull Stop stop1, @NotNull Stop stop2) {
        final int EARTH_RADIUS = 6371000; // meters
        double lat1 = Math.toRadians(stop1.getLat());
        double lon1 = Math.toRadians(stop1.getLon());
        double lat2 = Math.toRadians(stop2.getLat());
        double lon2 = Math.toRadians(stop2.getLon());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }
}