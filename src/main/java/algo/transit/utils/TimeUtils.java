package algo.transit.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;

@UtilityClass
public class TimeUtils {
    /**
     * Calculate minutes between two LocalTime objects with adjustments for transit time formats
     */
    public long calculateMinutesBetween(
            @NotNull LocalTime start,
            @NotNull LocalTime end,
            int dayDifference
    ) {
        long startMinutes = start.getHour() * 60L + start.getMinute();
        long endMinutes = end.getHour() * 60L + end.getMinute();
        return endMinutes - startMinutes + (dayDifference * 1440L);
    }

    public long calculateMinutesBetween(
            @NotNull LocalTime start,
            @NotNull LocalTime end) {
        return calculateMinutesBetween(start, end, end.isBefore(start) ? 1 : 0);
    }

    /**
     * Checks if one time is before another, handling hours > 23.
     */
    public boolean isBefore(
            @NotNull LocalTime time1,
            @NotNull LocalTime time2
    ) {
        int hour1 = time1.getHour();
        int hour2 = time2.getHour();

        if (hour1 == hour2) {
            return time1.getMinute() < time2.getMinute();
        }
        return hour1 < hour2;
    }

    /**
     * Checks if one time is after another
     */
    public boolean isAfter(
            @NotNull LocalTime time1,
            LocalTime time2
    ) {
        return !time1.equals(time2) && !isBefore(time1, time2);
    }
}