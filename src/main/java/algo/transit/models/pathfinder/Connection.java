package algo.transit.models.pathfinder;

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

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull Connection createWalkingConnection(
            String fromStop,
            String toStop,
            LocalTime currentTime,
            int walkTimeMinutes,
            int dayOffset
    ) {
        // Very short walks are treated as transfers with no time cost
        if (walkTimeMinutes <= 1) {
            return new Connection(
                    fromStop,
                    toStop,
                    "",
                    "",
                    "transfer",
                    currentTime,
                    currentTime,
                    "FOOT"
            );
        }

        // Calculate walking end time
        LocalTime arrivalTime = currentTime.plusMinutes(walkTimeMinutes);

        // Normal walking connections use actual calculated time
        return new Connection(
                fromStop,
                toStop,
                "",
                "",
                "",
                currentTime,
                arrivalTime,
                "FOOT"
        );
    }
}