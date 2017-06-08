/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
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
public final class DurationHelper {

    /**
     * Duration validation utility class. Checks if the requested duration is in
     * the defined min/max range.
     *
     */
    public static final class DurationRangeValidator {
        final Duration min;
        final Duration max;

        private DurationRangeValidator(final Duration min, final Duration max) {
            this.min = min;
            this.max = max;
        }

        /**
         * Checks if the requested duration is in the defined min/max range.
         * 
         * @param duration
         *            to checked
         * @return <code>true</code> if in time range
         */
        public boolean isWithinRange(final Duration duration) {
            return duration.compareTo(min) >= 0 && duration.compareTo(max) <= 0;
        }
    }

    /**
     * Format of the String expected in configuration file and in the database.
     */
    public static final String DURATION_FORMAT = "HH:mm:ss";

    private DurationHelper() {
        // utility class
    }

    /**
     * Creates a {@link DurationRangeValidator}.
     * 
     * @param min
     *            imum of range.
     * @param max
     *            imum of range.
     * @return {@link DurationRangeValidator} range.
     */
    public static DurationRangeValidator durationRangeValidator(final Duration min, final Duration max) {
        return new DurationRangeValidator(min, max);
    }

    /**
     * Converts a Duration into a formatted String
     * 
     * @param duration
     *            duration, which will be converted into a formatted String
     * @return String in the duration format, specified at
     *         {@link #DURATION_FORMAT}
     */
    public static String durationToFormattedString(final Duration duration) {
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
    public static Duration formattedStringToDuration(final String formattedDuration) {
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
    public static Duration getDurationByTimeValues(final long hours, final long minutes, final long seconds) {
        return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

}
