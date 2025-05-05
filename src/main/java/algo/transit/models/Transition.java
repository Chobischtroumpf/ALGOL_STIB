package algo.transit.models;

import java.time.LocalTime;

public class Transition {
    public final String fromStop;
    public final String toStop;
    public final String mode;
    public final String route;
    public final LocalTime departure;
    public final LocalTime arrival;
    public final double cost;

    public Transition(String fromStop, String toStop, String mode, String route,
                      LocalTime departure, LocalTime arrival, double cost) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.mode = mode;
        this.route = route;
        this.departure = departure;
        this.arrival = arrival;
        this.cost = cost;
    }

    public static Transition fromConnection(Connection connection, double cost) {
        return new Transition(
                connection.fromStop,
                connection.toStop,
                connection.mode,
                connection.routeName,
                connection.departureTime,
                connection.arrivalTime,
                cost
        );
    }

    @Override
    public String toString() {
        return "Take " + mode + " " + (route.isEmpty() ? "" : route + " ") +
                "from " + fromStop + " (" + departure + ") " +
                "to " + toStop + " (" + arrival + ")";
    }
}