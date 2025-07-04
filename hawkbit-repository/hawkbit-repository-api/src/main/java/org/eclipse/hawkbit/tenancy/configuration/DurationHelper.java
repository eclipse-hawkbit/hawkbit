/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy.configuration;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import lombok.NoArgsConstructor;

/**
 * This class is a helper for converting a duration into a string and for the other way. The string is in the format expected
 * in configuration and database - in {@link Duration} default format or in custom format like "01:00:00" or "01:01:50:50"
 * (starting with seconds, minutes, hours, days from the end).
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class DurationHelper {

    private static final DateTimeFormatter DURATION_FORMATER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long SECONDS_PER_DAY = 24 * 60 * 60L; // 24 hours * 60 minutes * 60 seconds
    private static final Duration DAY = Duration.ofDays(1);

    /**
     * Converts a Duration into a formatted String
     *
     * @param duration duration, which will be converted into a formatted String
     * @return String in the duration format, specified as HH:mm:ss or d+:HH:mm:ss
     */
    public static String toString(final Duration duration) {
        if (duration == null) {
            return null;
        }

        if (duration.compareTo(DAY) < 0) { // backward compatible HH:mm:ss
            return LocalTime.ofSecondOfDay(duration.toSeconds()).format(DURATION_FORMATER);
        } else { // custom format d+:HH:mm:ss
            return duration.toDays() + ":" + LocalTime.ofSecondOfDay(duration.toSeconds() % SECONDS_PER_DAY).format(DURATION_FORMATER);
        }
    }

    /**
     * Converts a formatted String into a Duration object.
     *
     * @param durationStr String in {@link Duration} default format or in custom format like "01:00:00" or "01:01:50:50"
     *                    (starting with seconds, minutes, hours, days from the end)
     * @return duration as a {@link Duration} object
     * @throws DateTimeParseException when String is in wrong format
     */
    public static Duration fromString(final String durationStr) {
        if (durationStr == null) {
            return null;
        }

        if (durationStr.charAt(0) == 'P') {
            // Handle ISO-8601 format, e.g., "PT1H30M"
            return Duration.parse(durationStr);
        } else {
            // Handle custom format, e.g., "01:00:00" or "01:01:50:50"
            final String[] split = durationStr.split(":");
            if (split.length == 1) { // ss
                return Duration.ofSeconds(Long.parseLong(split[0]));
            } else if (split.length == 2) { // mm:ss
                return Duration
                        .ofMinutes(Long.parseLong(split[0]))
                        .plusSeconds(Long.parseLong(split[1]));
            } else if (split.length == 3) { // HH:mm:ss
                return Duration
                        .ofHours(Long.parseLong(split[0]))
                        .plusMinutes(Long.parseLong(split[1]))
                        .plusSeconds(Long.parseLong(split[2]));
            } else if (split.length == 4) { // d:HH:mm:ss
                return Duration
                        .ofDays(Long.parseLong(split[0]))
                        .plusHours(Long.parseLong(split[1]))
                        .plusMinutes(Long.parseLong(split[2]))
                        .plusSeconds(Long.parseLong(split[3]));
            } else {
                throw new IllegalArgumentException("No more then 4 chunks (split by ':') are allowed in duration");
            }
        }
    }
}