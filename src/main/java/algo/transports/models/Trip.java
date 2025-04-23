package algo.transports.models;

import java.util.HashMap;
import java.util.Map;

public class Trip {
    private final String    tripId;
    private final String    routeId;
    private Route           route; // Not required
    private Map<Integer, StopTime> stopTimes;

    public Trip(String tripId, String routeId) {
        this.tripId     = tripId;
        this.routeId    = routeId;
        this.stopTimes  = new HashMap<>();
    }

    public String getTripId() {
        return tripId;
    }

    public String getRouteId() {
        return routeId;
    }

    public Route getRoute() throws NullPointerException {
        if (route == null) {
            throw new NullPointerException("Route is not set");
        }
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Map<Integer, StopTime> getStopTimes() throws NullPointerException {
        if (stopTimes == null) {
            throw new NullPointerException("Stop times are not set");
        }
        return stopTimes;
    }

    public void setStopTimes(Map<Integer, StopTime> stopTimes) {
        this.stopTimes = stopTimes;
    }

    public StopTime getStopTime(int stopSequence) throws NullPointerException {
        if (stopTimes == null) {
            throw new NullPointerException("Stop times are not set");
        }
        return stopTimes.get(stopSequence);
    }

    public void addStopTime(int stopSequence, StopTime stopTime) {
        if (stopTimes == null) {
            throw new NullPointerException("Stop times are not set");
        }
        stopTimes.put(stopSequence, stopTime);
    }
}
