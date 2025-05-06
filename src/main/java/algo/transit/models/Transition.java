package algo.transit.models;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

public record Transition(String fromStop, String toStop, String mode, String route, LocalTime departure,
                         LocalTime arrival, double cost) {

    @Contract(value = "_, _ -> new", pure = true)
    public static @NotNull Transition fromConnection(@NotNull Connection connection, double cost) {
        return new Transition(
                connection.fromStop(),
                connection.toStop(),
                connection.mode(),
                connection.routeName(),
                connection.departureTime(),
                connection.arrivalTime(),
                cost
        );
    }

    @Override
    public @NotNull String toString() {
        return "Take " + mode + " " + (route.isEmpty() ? "" : route + " ") +
                "from " + fromStop + " (" + departure + ") " +
                "to " + toStop + " (" + arrival + ")";
    }
}