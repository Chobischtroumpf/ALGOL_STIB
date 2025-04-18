package algo.transports.services;

import algo.transports.models.Route;
import algo.transports.models.Stop;
import algo.transports.models.StopTime;
import algo.transports.models.Trip;

import java.util.Map;

public class DataAnalyzerService {

    public static void formatCSVData(Map<String, Route> routes, Map<String, Stop> stops,
                                      Map<String, StopTime> stopTimes, Map<String, Trip> trips) {

        for (Map.Entry<String, StopTime> entry : stopTimes.entrySet()) {
            StopTime stopTime = entry.getValue();
            Trip trip = trips.get(stopTime.getTripId());
            Stop stop = stops.get(stopTime.getStopId());
            if (trip != null && stop != null) {
                stopTime.setStop(stop);
                stopTime.setTrip(trip);
                trip.addStopTime(stopTime.getStopSequence(), stopTime);
            }
        }
        for (Map.Entry<String, Trip> entry : trips.entrySet()) {
            Trip trip = entry.getValue();
            Route route = routes.get(trip.getRouteId());
            if (route != null) {
                trip.setRoute(route);
            }
        }
    }

}
