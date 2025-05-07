package algo.transit.utils;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalTime;

public class TimeUtils {
    /**
     * Calculate minutes between two LocalTime objects with adjustments for transit time formats
     */
    public static long calculateMinutesBetween(
            @NotNull LocalTime start,
            @NotNull LocalTime end
    ) {
        int startHour = start.getHour();
        int endHour = end.getHour();

        // Adjust for GTFS times > 24 hours
        if (startHour >= 24) startHour %= 24;
        if (endHour >= 24) endHour %= 24;

        LocalTime adjustedStart = LocalTime.of(startHour, start.getMinute(), start.getSecond());
        LocalTime adjustedEnd = LocalTime.of(endHour, end.getMinute(), end.getSecond());

        // If end appears before start, assume it's the next day
        if (adjustedEnd.isBefore(adjustedStart)) adjustedEnd = adjustedEnd.plusHours(24);

        // Calculate duration
        Duration duration = Duration.between(adjustedStart, adjustedEnd);
        return duration.toMinutes();
    }

    /**
     * Checks if one time is before another
     */
    public static boolean isBefore(
            @NotNull LocalTime time1,
            @NotNull LocalTime time2
    ) {
        // Normalize hours to 0-23 range
        int hour1 = time1.getHour() % 24;
        int hour2 = time2.getHour() % 24;

        // If hours are equal, compare minutes
        if (hour1 == hour2) return time1.getMinute() < time2.getMinute();
        return hour1 < hour2;
    }

    /**
     * Checks if one time is after another
     */
    public static boolean isAfter(
            @NotNull LocalTime time1,
            LocalTime time2
    ) {
        return !time1.equals(time2) && !isBefore(time1, time2);
    }
}
