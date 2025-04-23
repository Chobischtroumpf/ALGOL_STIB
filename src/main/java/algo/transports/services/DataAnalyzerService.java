package algo.transports.services;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.StopTime;
import algo.transports.models.Trip;

import java.util.List;
import java.util.Map;

public class DataAnalyzerService {

    public static void formatCSVData(Map<String, Route> routes, Map<String, Stop> stops,
                                     Map<String, List<StopTime>> stopTimes, Map<String, Trip> trips) {

        for (Map.Entry<String, List<StopTime>> entry : stopTimes.entrySet()) {
            String tripId = entry.getKey();
            List<StopTime> stopTimesList = entry.getValue();
            Trip trip = trips.get(tripId);

            // If trip exists
            if (trip != null) {
                // Set the route for this trip
                Route route = routes.get(trip.getRouteId());
                if (route != null) {
                    trip.setRoute(route);
                }

                // For each stop time
                for (StopTime stopTime : stopTimesList) {
                    Stop stop = stops.get(stopTime.getStopId());
                    if (stop != null) {
                        // Link stop time to stop
                        stopTime.setStop(stop);
                        // Link stop time to trip
                        stopTime.setTrip(trip);
                        // Add stop time to trip
                        trip.addStopTime(stopTime.getStopSequence(), stopTime);
                    }
                }
            }
        }
    }
}