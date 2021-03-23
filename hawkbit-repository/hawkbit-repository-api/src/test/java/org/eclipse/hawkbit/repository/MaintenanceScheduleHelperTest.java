/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;

import com.cronutils.model.Cron;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Repository")
@Story("Maintenance Schedule Utility")
public class MaintenanceScheduleHelperTest {

    @Test
    @Description("Verifies that the Cron object is returned for valid cron expression")
    public void getCronFromExpressionValid() {
        final String validCron = "0 0 0 ? * 6"; // at 00:00 every Saturday
        assertThat(MaintenanceScheduleHelper.getCronFromExpression(validCron)).isNotNull().isInstanceOf(Cron.class);
    }

    @Test
    @Description("Verifies that the Duration object is returned for valid duration format (hh:mm or hh:mm:ss)")
    public void convertToISODurationValid() {
        final String duration = "00:10";
        assertThat(MaintenanceScheduleHelper.convertToISODuration(duration)).isNotNull().isInstanceOf(Duration.class);
    }

    @Test
    @Description("Verifies that the InvalidMaintenanceScheduleException is thrown for invalid duration format")
    public void validateDurationInvalid() {
        final String duration = "10";
        assertThatThrownBy(() -> MaintenanceScheduleHelper.validateDuration(duration))
                .isInstanceOf(InvalidMaintenanceScheduleException.class).hasMessage("Provided duration is not valid")
            .extracting("durationErrorIndex").isEqualTo(2);
    }

    @Test
    @Description("Verifies that the InvalidMaintenanceScheduleException is thrown for invalid cron expression")
    public void validateCronScheduleInvalid() {
        final String invalidCron = "0 0 0 * * 6";
        assertThatThrownBy(() -> MaintenanceScheduleHelper.validateCronSchedule(invalidCron))
                .isInstanceOf(InvalidMaintenanceScheduleException.class)
                .hasMessageContaining("Both, a day-of-week AND a day-of-month parameter, are not supported");
    }

    @Test
    @Description("Verifies that there is a maintenance window available for correct schedule, duration and timezone")
    public void getNextMaintenanceWindowValid() {
        final ZonedDateTime currentTime = ZonedDateTime.now();
        final String cronSchedule = String.format("0 %d %d %d %d ? %d", currentTime.getMinute(), currentTime.getHour(),
                currentTime.getDayOfMonth(), currentTime.getMonthValue(), currentTime.getYear());
        final String duration = "00:10";
        final String timezone = ZonedDateTime.now().getOffset().getId().replace("Z", "+00:00");
        assertThat(MaintenanceScheduleHelper.getNextMaintenanceWindow(cronSchedule, duration, timezone)).isPresent();
    }

    @Test
    @Description("Verifies the maintenance schedule when only one required field is present")
    public void validateMaintenanceScheduleAtLeastOneNotEmpty() {
        final String duration = "00:10";
        assertThatThrownBy(() -> MaintenanceScheduleHelper.validateMaintenanceSchedule(null, duration, null))
                .isInstanceOf(InvalidMaintenanceScheduleException.class)
                .hasMessage("All of schedule, duration and timezone should either be null or non empty.");
    }

    @Test
    @Description("Verifies that there is no valid maintenance window available, scheduled before current time")
    public void validateMaintenanceScheduleBeforeCurrentTime() {
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
