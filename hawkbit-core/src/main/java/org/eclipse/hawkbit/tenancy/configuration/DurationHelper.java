package org.eclipse.hawkbit.tenancy.configuration;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

/**
 * This class is a helper for converting a duration into a string and for the
 * other way. The string is in the format expected in configuration and database
 * {@link #DURATION_FORMAT}.
 *
 */
public class DurationHelper {

    /**
     * Format of the String expected in configuration file and in the database.
     */
    public static final String DURATION_FORMAT = "HH:mm:ss";

    /**
     * Converts a Duration into a formatted String
     * 
     * @param duration
     *            duration, which will be converted into a formatted String
     * @return String in the duration format, specified at
     *         {@link #DURATION_FORMAT}
     */
    public String durationToFormattedString(final Duration duration) {
        if (duration == null) {
            return null;
        }

        return LocalTime.ofNanoOfDay(duration.toNanos()).format(DateTimeFormatter.ofPattern(DURATION_FORMAT));
    }

    /**
     * Converts a formatted String into a Duration object.
     * 
     * @param formattedDuration
     *            String in {@link #DURATION_FORMAT}
     * @return duration
     * @throws DateTimeParseException
     *             when String is in wrong format
     */
    public Duration formattedStringToDuration(final String formattedDuration) throws DateTimeParseException {
        if (formattedDuration == null) {
            return null;
        }

        final TemporalAccessor ta = DateTimeFormatter.ofPattern(DURATION_FORMAT).parse(formattedDuration.trim());
        return Duration.between(LocalTime.MIDNIGHT, LocalTime.from(ta));
    }

    /**
     * converts values of time constants to a Duration object..
     * 
     * @param hours
     *            count of hours
     * @param minutes
     *            count of minutes
     * @param seconds
     *            count of seconds
     * @return duration
     */
    public Duration getDurationByTimeValues(final long hours, final long minutes, final long seconds) {
        return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

    public DurationRangeValidator durationRangeValidator(final Duration min, final Duration max) {
        return new DurationRangeValidator(min, max);
    }

    public static class DurationRangeValidator {
        final Duration min;
        final Duration max;

        private DurationRangeValidator(final Duration min, final Duration max) {
            this.min = min;
            this.max = max;
        }

        public boolean isWithinRange(final Duration duration) {
            return duration.compareTo(min) > 0 && duration.compareTo(max) < 0;
        }
    }
}
