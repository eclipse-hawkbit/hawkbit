/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.ZonedDateTime;

import com.cronutils.model.Cron;
import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;
import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Repository<br/>
 * Story: Maintenance Schedule Utility
 */
class MaintenanceScheduleHelperTest {

    /**
     * Verifies that the Cron object is returned for valid cron expression
     */
    @Test    void getCronFromExpressionValid() {
        final String validCron = "0 0 0 ? * 6"; // at 00:00 every Saturday
        assertThat(MaintenanceScheduleHelper.getCronFromExpression(validCron)).isNotNull().isInstanceOf(Cron.class);
    }

    /**
     * Verifies that the Duration object is returned for valid duration format (hh:mm or hh:mm:ss)
     */
    @Test    void convertToISODurationValid() {
        final String duration = "00:10";
        assertThat(MaintenanceScheduleHelper.convertToISODuration(duration)).isNotNull().isInstanceOf(Duration.class);
    }

    /**
     * Verifies that the InvalidMaintenanceScheduleException is thrown for invalid duration format
     */
    @Test    void validateDurationInvalid() {
        final String duration = "10";
        assertThatThrownBy(() -> MaintenanceScheduleHelper.validateDuration(duration))
                .isInstanceOf(InvalidMaintenanceScheduleException.class).hasMessage("Provided duration is not valid")
                .extracting("durationErrorIndex").isEqualTo(2);
    }

    /**
     * Verifies that the InvalidMaintenanceScheduleException is thrown for invalid cron expression
     */
    @Test    void validateCronScheduleInvalid() {
        final String invalidCron = "0 0 0 * * 6";
        assertThatThrownBy(() -> MaintenanceScheduleHelper.validateCronSchedule(invalidCron))
                .isInstanceOf(InvalidMaintenanceScheduleException.class)
                .hasMessageContaining("Both, a day-of-week AND a day-of-month parameter, are not supported");
    }

    /**
     * Verifies that there is a maintenance window available for correct schedule, duration and timezone
     */
    @Test    void getNextMaintenanceWindowValid() {
        final ZonedDateTime currentTime = ZonedDateTime.now();
        final String cronSchedule = String.format("0 %d %d %d %d ? %d", currentTime.getMinute(), currentTime.getHour(),
                currentTime.getDayOfMonth(), currentTime.getMonthValue(), currentTime.getYear());
        final String duration = "00:10";
        final String timezone = ZonedDateTime.now().getOffset().getId().replace("Z", "+00:00");
        assertThat(MaintenanceScheduleHelper.getNextMaintenanceWindow(cronSchedule, duration, timezone)).isPresent();
    }

    /**
     * Verifies the maintenance schedule when only one required field is present
     */
    @Test    void validateMaintenanceScheduleAtLeastOneNotEmpty() {
        final String duration = "00:10";
        assertThatThrownBy(() -> MaintenanceScheduleHelper.validateMaintenanceSchedule(null, duration, null))
                .isInstanceOf(InvalidMaintenanceScheduleException.class)
                .hasMessage("All of schedule, duration and timezone should either be null or non empty.");
    }

    /**
     * Verifies that there is no valid maintenance window available, scheduled before current time
     */
    @Test    void validateMaintenanceScheduleBeforeCurrentTime() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        currentTime = currentTime.plusMinutes(-30);
        final String cronSchedule = String.format("0 %d %d %d %d ? %d", currentTime.getMinute(), currentTime.getHour(),
                currentTime.getDayOfMonth(), currentTime.getMonthValue(), currentTime.getYear());
        final String duration = "00:10";
        final String timezone = ZonedDateTime.now().getOffset().getId().replace("Z", "+00:00");
        assertThatThrownBy(
                () -> MaintenanceScheduleHelper.validateMaintenanceSchedule(cronSchedule, duration, timezone))
                .isInstanceOf(InvalidMaintenanceScheduleException.class)
                .hasMessage("No valid maintenance window available after current time");
    }
}
