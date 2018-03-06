/**
 * Copyright (c) Siemens AG, 2018
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static com.cronutils.model.CronType.QUARTZ;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

/**
 * Helper class to check validity of maintenance schedule definition and manage
 * scheduling of maintenance window using a cron expression based scheduler. It
 * also provides a helper method for conversion of duration specified in
 * HH:mm:ss format to ISO format.
 */
public class MaintenanceScheduleHelper {

    ExecutionTime scheduleExecutor = null;
    Duration duration = null;
    TimeZone timeZone = null;

    /**
     * Constructor that accepts a cron expression, duration and time zone and
     * instantiates the cron parser and scheduler executor.
     *
     * @param cronSchedule
     *            is the cron expression to be used for scheduling the
     *            maintenance window. Expression has 6 mandatory fields and 1
     *            last optional field: "second minute hour dayofmonth month
     *            weekday year"
     * @param duration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes
     * @param timezone
     *            is the time zone specified as +/-hh:mm offset from UTC. For
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     */
    public MaintenanceScheduleHelper(String cronSchedule, String duration, String timeZone) {
        this.timeZone = TimeZone.getTimeZone(ZoneOffset.of(timeZone));
        this.duration = Duration.parse(convertToISODuration(duration));

        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
        CronParser parser = new CronParser(cronDefinition);
        Cron quartzCron = parser.parse(cronSchedule);
        this.scheduleExecutor = ExecutionTime.forCron(quartzCron);
    }

    /**
     * Method calculates the next available maintenance window within the
     * schedule but after a given time.
     *
     * @param after
     *            is the {@link ZonedDateTime} after which the window is
     *            required
     *
     * @return {@link Optional<ZonedDateTime>} of the next available window. In
     *         case there is none, returns empty value.
     */
    public Optional<ZonedDateTime> nextExecution(ZonedDateTime after) {
        try {
            ZonedDateTime next = this.scheduleExecutor.nextExecution(after);
            return Optional.of(next);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Method checks if there are any more valid maintenance windows after a
     * given time.
     *
     * @param after
     *            is the {@link ZonedDateTime} after which the windows are
     *            checked
     *
     * @return true if there is at least one valid schedule remaining, else
     *         false.
     */
    public boolean hasValidScheduleAfter(ZonedDateTime after) {
        return nextExecution(after).isPresent();
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
     *            year"
     * @param duration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes
     * @param timezone
     *            is the time zone specified as +/-hh:mm offset from UTC. For
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone
     *
     * @return true if the schedule is valid, else throw an exception
     *
     * @throws InvalidMaintenanceScheduleException
     *             if the defined schedule fails the validity criteria.
     */
    public static boolean validateMaintenanceSchedule(String cronSchedule, String duration, String timezone) {
        // check if schedule, duration and timezone are all not null.
        if (cronSchedule != null && duration != null && timezone != null) {
            // check if schedule, duration and timezone are all not empty.
            if (!(cronSchedule.isEmpty() || duration.isEmpty() || timezone.isEmpty())) {
                ZonedDateTime now = ZonedDateTime.now(ZoneOffset.of(timezone));
                MaintenanceScheduleHelper scheduleHelper = new MaintenanceScheduleHelper(cronSchedule, duration,
                        timezone);
                // check if there is a window currently active or available in
                // future.
                if (!scheduleHelper.hasValidScheduleAfter(now.minus(Duration.parse(convertToISODuration(duration))))) {
                    throw new InvalidMaintenanceScheduleException(
                            "No valid maintenance window available after current time");
                }
            } else {
                throw new InvalidMaintenanceScheduleException("Either of schedule, duration or timezone empty.");
            }
        } else if (!(cronSchedule == null && duration == null && timezone == null)) {
            throw new InvalidMaintenanceScheduleException(
                    "All of schedule, duration and timezone should either be null or non empty.");
        }

        return true;
    }

    /**
     * Convert the time interval or duration specified in "HH:mm:ss" format to
     * ISO format.
     *
     * @param timeInterval
     *            in "HH:mm:ss" string format. This format is popularly used but
     *            can be confused with time of the day, hence conversion to ISO
     *            specified format for time duration is required
     *
     * @return the time interval or duration in ISO format
     *
     * @throws DateTimeParseException
     *             if the text cannot be converted to ISO format.
     */
    public static String convertToISODuration(String timeInterval) {
        return Duration.between(LocalTime.MIN, LocalTime.parse(timeInterval)).toString();
    }
}
