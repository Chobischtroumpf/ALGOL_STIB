package algo.transit.models;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

public record Connection(
        String fromStop,
        String toStop,
        String tripId,
        String routeId,
        String routeName,
        LocalTime departureTime,
        LocalTime arrivalTime,
        String mode
) {

    @Contract("_, _, _, _ -> new")
    public static @NotNull Connection createWalkingConnection(String fromStop, String toStop,
                                                              LocalTime currentTime, int walkTimeMinutes) {
        // Very short walks are treated as transfers with no time cost
        if (walkTimeMinutes <= 2) {
            return new Connection(
                    fromStop,
                    toStop,
                    "",  // No trip ID
                    "",  // No route ID
                    "transfer",  // Indicate this is a transfer
                    currentTime,
                    currentTime,  // Same arrival time = zero time cost
                    "FOOT"
            );
        }

        // Normal walking connections use actual calculated time
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