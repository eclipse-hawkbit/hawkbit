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

import javax.annotation.PostConstruct;

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
 *
 *
 *
 *
 */
public final class PollConfigurationHelper {

    /**
     * Expected format of the Polling Time String
     */
    public static final String EXPECTED_POLLING_TIME_FORMAT = "(([0-1]?[0-9]|2[0-3])(:[0-5][0-9]){2})";

    private static final Logger LOG = LoggerFactory.getLogger(PollConfigurationHelper.class);
    private static final PollConfigurationHelper INSTANCE = new PollConfigurationHelper();

    private static final int DEFAULT_OVERDUE_HOUR = 0;
    private static final int DEFAULT_OVERDUE_MINUTE = 5;
    private static final int DEFAULT_OVERDUE_SECOND = 0;

    private static final int DEFAULT_POLL_HOUR = 0;
    private static final int DEFAULT_POLL_MINUTE = 5;
    private static final int DEFAULT_POLL_SECOND = 0;

    @Autowired
    private ControllerPollProperties controllerPollProperties;

    @Autowired
    private SystemManagement systemManagement;

    private Duration controllerPollTimeDuration;
    private Duration controllerOverduePollTimeDuration;

    private PollConfigurationHelper() {
    }

    /**
     * Bean post construct to calculate the poll time and poll overdue time only
     * once.
     */
    @PostConstruct
    public void postConstruct() {
        final long[] controllerPollTimeSplit = splitInterval(controllerPollProperties.getPollingTime(),
                DEFAULT_POLL_HOUR, DEFAULT_POLL_MINUTE, DEFAULT_POLL_SECOND);
        this.controllerPollTimeDuration = Duration.ZERO.plusHours(controllerPollTimeSplit[0])
                .plusMinutes(controllerPollTimeSplit[1]).plusSeconds(controllerPollTimeSplit[2]);

        final long[] controllerOverduePollTimeSplit = splitInterval(controllerPollProperties.getPollingOverdueTime(),
                DEFAULT_OVERDUE_HOUR, DEFAULT_OVERDUE_MINUTE, DEFAULT_OVERDUE_SECOND);
        this.controllerOverduePollTimeDuration = Duration.ZERO.plusHours(controllerOverduePollTimeSplit[0])
                .plusMinutes(controllerOverduePollTimeSplit[1]).plusSeconds(controllerOverduePollTimeSplit[2]);
    }

    /**
     * @return the poll time interval configured in the configuration
     *         {@code hawkbit.server.controller.polling} or the default value
     *         which is {@code 00:05:00} never {@code null}.
     */
    public Duration getPollTimeInterval() {
        return controllerPollTimeDuration;
    }

    /**
     * @return the poll time interval configured in the configuration
     *         {@code hawkbit.server.controller.polling} or the default value
     *         which is {@code 00:05:00} never {@code null}.
     */
    public String getPollTimeIntervalAsFormattedString() {
        return durationToFormattedString(controllerPollTimeDuration);
    }

    /**
     * @return the overdue poll threshold configured in the configuration
     *         {@code hawkbit.server.controller.polling.overdue} or the default
     *         value which is {@code 00:05:00} never {@code null}.
     */
    public Duration getOverduePollTimeInterval() {
        return controllerOverduePollTimeDuration;
    }

    /**
     * @return the overdue poll threshold as a formatted string (HH:MM:SS)
     *         configured in the configuration
     *         {@code hawkbit.server.controller.polling.overdue} or the default
     *         value which is {@code 00:05:00} never {@code null}.
     */
    public String getOverduePollTimeIntervalAsFormattedString() {
        return durationToFormattedString(controllerOverduePollTimeDuration);
    }

    /**
     * @return a singleton instance of the environment helper.
     */
    public static PollConfigurationHelper getInstance() {
        return INSTANCE;
    }

    private long[] splitInterval(final String interval, final long defaultHour, final long defaultMinute,
            final long defaultSecond) {
        if (interval != null) {
            final String[] split = interval.split(":");
            if (split.length == 3) {
                try {
                    return new long[] { Long.parseLong(split[0]), Long.parseLong(split[1]), Long.parseLong(split[2]) };
                } catch (final NumberFormatException e) {
                    LOG.warn("Cannot parse the given poll configuration {}", interval);
                }
            }
        }
        LOG.warn("Using default configuration hour:{} min:{}, second:{}", defaultHour, defaultMinute, defaultSecond);
        return new long[] { defaultHour, defaultMinute, defaultSecond };
    }

    private String durationToFormattedString(Duration duration) {
        long seconds = duration.getSeconds();
        long minuts = seconds / 60;
        long hours = minuts / 60;

        seconds = seconds % 60;
        minuts = minuts % 60;
        hours = hours % 100;

        return String.format("%02d:%02d:%02d", hours, minuts, seconds);
    }

    private Duration formattedStringToDuration(String formattedDuration) {
        if (formattedDuration == null || !formattedDuration.matches(EXPECTED_POLLING_TIME_FORMAT)) {
            LOG.warn("Cannot parse the given poll configuration {}", formattedDuration);
            throw new IllegalArgumentException(
                    "String is not in the EXPECTED_POLLING_TIME_FORMAT. Parsing not possible.");
        }

        final String[] split = formattedDuration.split(":");
        if (split.length == 3) {
            try {
                return Duration.ofHours(Long.parseLong(split[0])).plusMinutes(Long.parseLong(split[1]))
                        .plusSeconds(Long.parseLong(split[2]));
            } catch (final NumberFormatException e) {
                LOG.warn("Cannot parse the given poll configuration {}", formattedDuration);
            }
        }

        LOG.error("Cannot parse the given poll String {}, even it matches the regex \"{}\".", formattedDuration,
                EXPECTED_POLLING_TIME_FORMAT);
        throw new IllegalArgumentException("String is in the expected format. But parsing is not possible anyway.");
    }
}
