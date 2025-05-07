package algo.transit.utils;

import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

public class TimeUtils {
    /**
     * Calculate minutes between two LocalTime objects with adjustments for transit time formats
     */
    public static long calculateMinutesBetween(
            @NotNull LocalTime start,
            @NotNull LocalTime end,
            int dayDifference
    ) {
        // Calculate base duration within a day
        long startMinutes = start.getHour() * 60 + start.getMinute();
        long endMinutes = end.getHour() * 60 + end.getMinute();

        // Add day differences
        return endMinutes - startMinutes + (dayDifference * 1440L);
    }

    public static long calculateMinutesBetween(
            @NotNull LocalTime start,
            @NotNull LocalTime end) {
        return calculateMinutesBetween(start, end, end.isBefore(start) ? 1 : 0);
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
