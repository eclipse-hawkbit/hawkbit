/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rollout Management properties.
 *
 */
@ConfigurationProperties("hawkbit.autoassign")
public class AutoAssignProperties {
    /**
     * Autoassign scheduler configuration.
     */
    public static class Scheduler {
        // used by @Scheduled annotation which needs constant
        public static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.autoassign.scheduler.fixedDelay:60000}";

        /**
         * Schedule where the autoassign scheduler looks necessary state changes in
         * milliseconds.
         */
        private long fixedDelay = 60000L;

        public long getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(final long fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

    }

    private final Scheduler scheduler = new Scheduler();

    public Scheduler getScheduler() {
        return scheduler;
    }

}
