/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model.helper;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;

/**
 * A singleton bean which holds configuration of the poll time
 * {@code hawkbit.server.controller.polling} and
 * {@code hawkbit.server.controller.polling.overdue} to have access to the
 * configuration in beans not instatinated by spring e.g. JPA entities which
 * cannot implement the {@link EnvironmentAware} interface to retrieve
 * environment variables.
 */
public final class PollConfigurationHelper {

    /**
     * Format of the expected Duration String. Pattern has to be in Valid Format
     * for SimpleDateFormat
     */

    private static final Logger LOG = LoggerFactory.getLogger(PollConfigurationHelper.class);
    private static final PollConfigurationHelper INSTANCE = new PollConfigurationHelper();

    private static final int DEFAULT_OVERDUE_HOUR = 0;
    private static final int DEFAULT_OVERDUE_MINUTE = 5;
    private static final int DEFAULT_OVERDUE_SECOND = 0;

    private static final int DEFAULT_POLL_HOUR = 0;
    private static final int DEFAULT_POLL_MINUTE = 5;
    private static final int DEFAULT_POLL_SECOND = 0;

    private static final int DEFAULT_MAX_HOUR = 23;
    private static final int DEFAULT_MAX_MINUTE = 59;
    private static final int DEFAULT_MAX_SECOND = 59;

    private static final int DEFAULT_MIN_HOUR = 0;
    private static final int DEFAULT_MIN_MINUTE = 0;
    private static final int DEFAULT_MIN_SECOND = 30;

    private static DurationHelper dh = new DurationHelper();

    @Autowired
    private ControllerPollProperties controllerPollProperties;

    @Autowired
    private SystemManagement systemManagement;

    private Duration configurationPollTime;
    private Duration configurationOverduePollTime;
    private Duration configurationMaximumPollTime;
    private Duration configurationMinimumPollTime;

    /**
     * @return a singleton instance of the environment helper.
     */
    public static PollConfigurationHelper getInstance() {
        return INSTANCE;
    }

    /**
     * Bean post construct to calculate the poll time and poll overdue time only
     * once.
     */
    @PostConstruct
    public void initializeConfigurationValues() {

        readGlobalDurationsFromConfiguration();

        validateGlobalDurations();
    }

    private void readGlobalDurationsFromConfiguration() {
        try {
            configurationMaximumPollTime = dh.formattedStringToDuration(controllerPollProperties.getMaxPollingTime());
        } catch (DateTimeParseException e) {
            // Set to default values
            configurationMaximumPollTime = dh.getDurationByTimeValues(DEFAULT_MAX_HOUR, DEFAULT_MAX_MINUTE,
                    DEFAULT_MAX_SECOND);
        }

        try {
            configurationMinimumPollTime = dh.formattedStringToDuration(controllerPollProperties.getMinPollingTime());
        } catch (DateTimeParseException e) {
            // Set to default values
            configurationMinimumPollTime = dh.getDurationByTimeValues(DEFAULT_MIN_HOUR, DEFAULT_MIN_MINUTE,
                    DEFAULT_MIN_SECOND);
        }

        try {
            configurationPollTime = dh.formattedStringToDuration(controllerPollProperties.getPollingTime());
        } catch (DateTimeParseException e) {
            configurationPollTime = dh.getDurationByTimeValues(DEFAULT_POLL_HOUR, DEFAULT_POLL_MINUTE,
                    DEFAULT_POLL_SECOND);
        }

        try {
            configurationOverduePollTime = dh
                    .formattedStringToDuration(controllerPollProperties.getPollingOverdueTime());
        } catch (DateTimeParseException e) {
            configurationOverduePollTime = dh.getDurationByTimeValues(DEFAULT_OVERDUE_HOUR, DEFAULT_OVERDUE_MINUTE,
                    DEFAULT_OVERDUE_SECOND);
        }

    }

    private void validateGlobalDurations() {

        if (configurationMaximumPollTime.compareTo(configurationMinimumPollTime) < 0) {
            // min value > max value -> use default values for both durations
            LOG.warn("The configured maximum value of the polling time is smaller"
                    + " than the configured minimum value. Both are replaced by default values.");

            configurationMaximumPollTime = dh.getDurationByTimeValues(DEFAULT_MAX_HOUR, DEFAULT_MAX_MINUTE,
                    DEFAULT_MAX_SECOND);
            configurationMinimumPollTime = dh.getDurationByTimeValues(DEFAULT_MIN_HOUR, DEFAULT_MIN_MINUTE,
                    DEFAULT_MIN_SECOND);
        }

        if (!isWithinRange(configurationPollTime)) {
            // poll time value not within allowed range ==> use default value
            configurationPollTime = dh.getDurationByTimeValues(DEFAULT_POLL_HOUR, DEFAULT_POLL_MINUTE,
                    DEFAULT_POLL_SECOND);
        }

        if (!isWithinRange(configurationOverduePollTime)) {
            // overdue poll time value not within range => use default value
            configurationOverduePollTime = dh.getDurationByTimeValues(DEFAULT_OVERDUE_HOUR, DEFAULT_OVERDUE_MINUTE,
                    DEFAULT_OVERDUE_SECOND);
        }
    }

    private boolean isWithinRange(@NotNull Duration duration) {
        return duration.compareTo(configurationMinimumPollTime) > 0
                && duration.compareTo(configurationMaximumPollTime) < 0;
    }

    /**
     * @return the poll time interval stored in the tenant meta data. If there
     *         is no tenant specific configuration the global value, configured
     *         in the configuration {@code hawkbit.server.controller.polling} or
     *         the default value which is {@code 00:05:00} never {@code null}.
     */
    public Duration getPollTimeInterval() {
        Duration tenantPollTimeInterval = systemManagement.getTenantMetadata().getPollingTime();

        if (tenantPollTimeInterval != null) {
            if (isWithinRange(tenantPollTimeInterval)) {
                return tenantPollTimeInterval;
            }
            LOG.warn(
                    "Tenant {} has stored a pollign interval {} which is not in the allowed range. Configured default value is loaded.",
                    systemManagement.currentTenant(), tenantPollTimeInterval);
        }
        return configurationPollTime;
    }

    /**
     * @return the poll time interval configured in the configuration
     *         {@code hawkbit.server.controller.polling} or the default value
     *         which is {@code 00:05:00} never {@code null}. This method ignores
     *         eventual tenant specific configurations.
     */
    public Duration getGlobalPollTimeInterval() {
        return configurationPollTime;
    }

    /**
     * @return the overdue poll time interval stored in the tenant meta data. If
     *         there is no tenant specific configuration the global value,
     *         configured in the configuration
     *         {@code hawkbit.server.controller.polling} or the default value
     *         which is {@code 00:05:00} never {@code null}.
     */
    public Duration getOverduePollTimeInterval() {
        Duration tenantOverduePollTimeInterval = systemManagement.getTenantMetadata().getPollingOverdueTime();

        if (tenantOverduePollTimeInterval != null) {
            if (isWithinRange(tenantOverduePollTimeInterval)) {
                return tenantOverduePollTimeInterval;
            }
            LOG.warn(
                    "Tenant {} has stored an overdue polling interval {} which is not in the allowed range. Configured default value is loaded.",
                    systemManagement.currentTenant(), tenantOverduePollTimeInterval);
        }
        return configurationOverduePollTime;
    };

    /**
     * @return the overdue poll time interval configured in the configuration
     *         {@code hawkbit.server.controller.polling.overdue} or the default
     *         value which is {@code 00:05:00} never {@code null}.
     */
    public Duration getGlobalOverduePollTimeInterval() {
        return configurationOverduePollTime;
    }

    /**
     * @return the maximum poll time duration configured in the configuration
     *         {@code hawkbit.server.controller.polling.overdue} or the default
     *         value which is {@code 23:59:00} never {@code null}.
     */
    public Duration getMaximumPollingInterval() {
        return configurationMaximumPollTime;
    }

    /**
     * @return the minimum poll time duration configured in the configuration
     *         {@code hawkbit.server.controller.polling.overdue} or the default
     *         value which is {@code 23:59:00} never {@code null}.
     */
    public Duration getMinimumPollingInterval() {
        return configurationMinimumPollTime;
    }

    /**
     * sets the ControllerPollProperties in a not spring handled context. Don't
     * forget to call {@code initializeConfigurationValues} afterwards to read
     * the values from the PollProperties.
     * 
     * @param controllerPollProperties
     *            the controller poll properties
     */
    public void setControllerPollProperties(ControllerPollProperties controllerPollProperties) {
        this.controllerPollProperties = controllerPollProperties;
    }

    /**
     * sets the SystemManagement instance which is responsible for tenant
     * specific configuration.
     * 
     * @param systemManagement
     *            the SystemManagemnt instance
     */
    public void setSystemManagement(SystemManagement systemManagement) {
        this.systemManagement = systemManagement;
    }
}
