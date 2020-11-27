/**
 * Copyright (c) Siemens AG, 2018
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;
import org.springframework.util.StringUtils;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

/**
 * Helper class to check validity of maintenance schedule definition and manage
 * scheduling of maintenance window using a cron expression based scheduler. It
 * also provides a helper method for conversion of duration specified in
 * HH:mm:ss format to ISO format.
 */
public final class MaintenanceScheduleHelper {

    private static final CronParser cronParser = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));

    private MaintenanceScheduleHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Calculate the next available maintenance window.
     *
     * @param cronSchedule
     *            is a cron expression with 6 mandatory fields and 1 last
     *            optional field: "second minute hour dayofmonth month weekday
     *            year".
     * @param duration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes.
     * @param timezone
     *            is the time zone specified as +/-hh:mm offset from UTC. For
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     *
     * @return { @link Optional<ZonedDateTime>} of the next available window. In
     *         case there is none, or there are maintenance window validation
     *         errors, returns empty value.
     *
     */
    // Exception squid:S1166 - if there are validation error(format of cron
    // expression or duration is wrong), we simply return empty value
    @SuppressWarnings("squid:S1166")
    public static Optional<ZonedDateTime> getNextMaintenanceWindow(final String cronSchedule, final String duration,
            final String timezone) {
        try {
            final ExecutionTime scheduleExecutor = ExecutionTime.forCron(getCronFromExpression(cronSchedule));
            final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.of(timezone));
            final ZonedDateTime after = now.minus(convertToISODuration(duration));
            return scheduleExecutor.nextExecution(after);
        } catch (final RuntimeException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Parse the given cron expression with quartz parser.
     *
     * @param cronSchedule
     *            is a cron expression with 6 mandatory fields and 1 last
     *            optional field: "second minute hour dayofmonth month weekday
     *            year".
     *
     * @return {@link Cron} object, that corresponds to the expression.
     *
     * @throws IllegalArgumentException
     *             if the cron expression doesn't have a valid format.
     */
    public static Cron getCronFromExpression(final String cronSchedule) {
        return cronParser.parse(cronSchedule);
    }

    /**
     * Check if the maintenance schedule definition is valid in terms of
     * validity of cron expression, duration and availability of at least one
     * valid maintenance window. Further a maintenance schedule is valid if
     * either all the parameters: schedule, duration and time zone are valid or
     * are null.
     *
     * @param cronSchedule
     *            is a cron expression with 6 mandatory fields and 1 last
     *            optional field: "second minute hour dayofmonth month weekday
     *            year".
     * @param duration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes.
     * @param timezone
     *            is the time zone specified as +/-hh:mm offset from UTC. For
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     *
     * @throws InvalidMaintenanceScheduleException
     *             if the defined schedule fails the validity criteria.
     */
    public static void validateMaintenanceSchedule(final String cronSchedule, final String duration,
            final String timezone) {
        if (allNotEmpty(cronSchedule, duration, timezone)) {
            validateCronSchedule(cronSchedule);
            validateDuration(duration);
            // check if there is a window currently active or available in
            // future.
            if (!getNextMaintenanceWindow(cronSchedule, duration, timezone).isPresent()) {
                throw new InvalidMaintenanceScheduleException(
                        "No valid maintenance window available after current time");
            }
        } else if (atLeastOneNotEmpty(cronSchedule, duration, timezone)) {
            throw new InvalidMaintenanceScheduleException(
                    "All of schedule, duration and timezone should either be null or non empty.");
        }
    }

    private static boolean atLeastOneNotEmpty(final String cronSchedule, final String duration, final String timezone) {
        return !(StringUtils.isEmpty(cronSchedule) && StringUtils.isEmpty(duration) && StringUtils.isEmpty(timezone));
    }

    private static boolean allNotEmpty(final String cronSchedule, final String duration, final String timezone) {
        return !StringUtils.isEmpty(cronSchedule) && !StringUtils.isEmpty(duration) && !StringUtils.isEmpty(timezone);
    }

    /**
     * Convert the time interval or duration specified in "HH:mm:ss" format to
     * ISO format.
     *
     * @param timeInterval
     *            in "HH:mm:ss" string format. This format is popularly used but
     *            can be confused with time of the day, hence conversion to ISO
     *            specified format for time duration is required.
     *
     * @return {@link Duration} in ISO format.
     *
     * @throws DateTimeParseException
     *             if the text cannot be converted to ISO format.
     */
    public static Duration convertToISODuration(final String timeInterval) {
        return Duration.between(LocalTime.MIN, convertDurationToLocalTime(timeInterval));
    }

    private static LocalTime convertDurationToLocalTime(final String timeInterval) {
        return LocalTime.parse(StringUtils.trimWhitespace(timeInterval));
    }

    /**
     * Validates the format of the maintenance window duration
     *
     * @param duration
     *            in "HH:mm:ss" string format. This format is popularly used but
     *            can be confused with time of the day, hence conversion to ISO
     *            specified format for time duration is required.
     *
     * @throws InvalidMaintenanceScheduleException
     *             if the duration doesn't have a valid format to be converted
     *             to ISO.
     */
    public static void validateDuration(final String duration) {
        try {
            if (StringUtils.hasText(duration)) {
                convertDurationToLocalTime(duration);
            }
        } catch (final DateTimeParseException e) {
            throw new InvalidMaintenanceScheduleException("Provided duration is not valid", e, e.getErrorIndex());
        }
    }

    /**
     * Validates the format of the maintenance window cron expression
     *
     * @param cronSchedule
     *            is a cron expression with 6 mandatory fields and 1 last
     *            optional field: "second minute hour dayofmonth month weekday
     *            year".
     *
     * @throws InvalidMaintenanceScheduleException
     *             if the cron expression doesn't have a valid quartz format.
     */
    public static void validateCronSchedule(final String cronSchedule) {
        try {
            if (StringUtils.hasText(cronSchedule)) {
                getCronFromExpression(cronSchedule);
            }
        } catch (final IllegalArgumentException e) {
            throw new InvalidMaintenanceScheduleException(e.getMessage(), e);
        }
    }
}
