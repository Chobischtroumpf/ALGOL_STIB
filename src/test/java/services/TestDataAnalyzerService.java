package services;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.StopTime;
import algo.transports.models.Trip;
import algo.transports.services.CSVService;
import algo.transports.services.DataAnalyzerService;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;

public class TestDataAnalyzerService {

    @Test
    public void testDataAnalyzerService() throws IOException {
        Path routesPath = Path.of("src", "test", "resources", "testRoutes.csv");
        Path stopsPath = Path.of("src", "test", "resources", "testStops.csv");
        Path stopTimesPath = Path.of("src", "test", "resources", "testStopTimes.csv");
        Path tripPath = Path.of("src", "test", "resources", "testTrip.csv");

        //output file
        Path outputPath = Path.of("src", "test", "resources", "output.csv");
        // Create an instance of CSVService
        CSVService csvService = new CSVService(new Path[]{routesPath}, new Path[]{stopTimesPath}, new Path[]{stopsPath}, new Path[]{tripPath});
        Map<String, Route> routes = csvService.getRoutes();
        Map<String, Stop> stops = csvService.getStops();
        Map<String, StopTime> stopTimes = csvService.getStopTimes();
        Map<String, Trip> trips = csvService.getTrips();

        // Call the method to format CSV data
        DataAnalyzerService.formatCSVData(routes, stops, stopTimes, trips);

        // Check if the data is formatted correctly
        Trip trip = trips.get(trips.keySet().toArray()[0]);
        File file = new File(outputPath.toString());
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //write the trip to the output file
        Writer writer = new FileWriter(file, true);
        writer.write("Trip ID: " + trip.getTripId() + "\n");
        writer.write("Route ID: " + trip.getRouteId() + "\n");
        writer.write("Route Short Name: " + trip.getRoute().getRouteShortName() + "\n");
        writer.write("Route Long Name: " + trip.getRoute().getRouteLongName() + "\n");
        writer.write("Route Type: " + trip.getRoute().getRouteType() + "\n");
        writer.write("Trip Stop Times: \n");
        for (Map.Entry<Integer, StopTime> stopTimeEntry : trip.getStopTimes().entrySet()) {
            StopTime stopTime = stopTimeEntry.getValue();
            writer.write("Stop ID: " + stopTime.getStopId() + "\n");
            writer.write("Stop Name: " + stopTime.getStop().getName() + "\n");
            writer.write("Departure Time: " + stopTime.getDepartureTime() + "\n");
            writer.write("Stop Sequence: " + stopTime.getStopSequence() + "\n");
        }
        writer.write("\n");
        writer.close();
    }
}
