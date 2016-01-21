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

    private Duration controllerPollTimeDuration;
    private Duration controllerOverduePollTimeDuration;

    private PollConfigurationHelper() {
    }

    /**
     * Bean post construct to calcualte the poll time and poll overdue time only
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
     * @return the overdue poll threshold configured in the configuration
     *         {@code hawkbit.server.controller.polling.overdue} or the default
     *         value which is {@code 00:05:00} never {@code null}.
     */
    public Duration getOverduePollTimeInterval() {
        return controllerOverduePollTimeDuration;
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
}
