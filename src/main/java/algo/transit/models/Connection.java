package algo.transit.models;

import java.time.LocalTime;

public class Connection {
    public final String fromStop;
    public final String toStop;
    public final String tripId;
    public final String routeId;
    public final String routeName;
    public final LocalTime departureTime;
    public final LocalTime arrivalTime;
    public final String mode;

    public Connection(String fromStop, String toStop, String tripId, String routeId,
                      String routeName, LocalTime departureTime, LocalTime arrivalTime, String mode) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.tripId = tripId;
        this.routeId = routeId;
        this.routeName = routeName;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.mode = mode;
    }

    public static Connection createWalkingConnection(String fromStop, String toStop,
                                                     LocalTime currentTime, int walkTimeMinutes) {
        return new Connection(
                fromStop,
                toStop,
                "",  // No trip ID
                "",  // No route ID
                "",  // No route name
                currentTime,
                currentTime.plusMinutes(walkTimeMinutes),
                "FOOT"
        );
    }
}