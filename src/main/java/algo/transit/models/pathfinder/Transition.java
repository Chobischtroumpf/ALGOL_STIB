package algo.transit.models.pathfinder;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

public record Transition(
        String fromStop,
        String toStop,
        String mode,
        String route,
        LocalTime departure,
        LocalTime arrival,
        int dayOffset,
        double cost
) {
    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull Transition fromConnection(
            @NotNull Connection connection,
            double cost,
            int currentDayOffset
    ) {
        // Calculate if the arrival crosses to next day
        int newDayOffset = currentDayOffset;
        if (connection.arrivalTime().isBefore(connection.departureTime())) newDayOffset++;

        return new Transition(
                connection.fromStop(),
                connection.toStop(),
                connection.mode(),
                connection.routeName(),
                connection.departureTime(),
                connection.arrivalTime(),
                newDayOffset,
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