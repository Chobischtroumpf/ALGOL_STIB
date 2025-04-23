package algo.transports.models;

import java.time.LocalTime;

public class StopTime {
    private final String    tripId;
    private final String    stopId;
    private final LocalTime departureTime;
    private final int       stopSequence;

    // TODO: Remove
    private Stop            stop;
    private Trip            trip;

    public StopTime(String tripId, LocalTime departureTime, String stopId, int stopSequence) {
        this.tripId         = tripId;
        this.departureTime  = departureTime;
        this.stopId         = stopId;
        this.stopSequence   = stopSequence;
    }

    public String getTripId() {
        return tripId;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public String getStopId() {
        return stopId;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }
}
