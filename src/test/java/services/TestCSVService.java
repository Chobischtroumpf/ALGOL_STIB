package services;

import algo.transports.models.Stop;
import algo.transports.services.CSVService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TestCSVService {

    @Test
    public void testCSVService() {
        // Create an instance of CSVService
        CSVService csvService = new CSVService(new Path[]{Path.of("src", "test", "resources", "testRoutes.csv")},
                new Path[]{Path.of("src", "test", "resources", "testStopTimes.csv")},
                new Path[]{Path.of("src", "test", "resources", "testStops.csv")},
                new Path[]{Path.of("src", "test", "resources", "testTrips.csv")});

        // Call the method to read CSV files
        Map<String, Stop> temp = csvService.getStops();

        // Check if the list is not null
        assert temp != null : "The list of stops should not be null";
        // Check if the list is not empty
        assert !temp.isEmpty() : "The list of stops should not be empty";
        // Check if the first element is of type Stop
        assert temp.get("STIB-0089") != null : "The first stop should be of type Stop";

    }
}