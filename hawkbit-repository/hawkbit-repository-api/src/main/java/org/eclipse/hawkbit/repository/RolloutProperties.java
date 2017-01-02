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
@ConfigurationProperties("hawkbit.rollout")
public class RolloutProperties {
    // used by @Scheduled annotation which needs constant
    public static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.rollout.scheduler.fixedDelay:30000}";

    // used by @Scheduled annotation which needs constant
    public static final String PROP_CREATING_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.rollout.creatingScheduler.fixedDelay:2000}";

    // used by @Scheduled annotation which needs constant
    public static final String PROP_STARTING_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.rollout.startingScheduler.fixedDelay:2000}";

    // used by @Scheduled annotation which needs constant
    public static final String PROP_READY_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.rollout.readyScheduler.fixedDelay:30000}";

    /**
     * Rollout scheduler configuration.
     */
    public static class Scheduler {

        /**
         * Schedule where the rollout scheduler looks necessary state changes in
         * milliseconds.
         */
        private long fixedDelay;

        private boolean enabled = true;

        public Scheduler(final long fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        public long getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(final long fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }
    }

    private final Scheduler scheduler = new Scheduler(30000L);

    private final Scheduler creatingScheduler = new Scheduler(2000L);

    private final Scheduler startingScheduler = new Scheduler(2000L);

    private final Scheduler readyScheduler = new Scheduler(30000L);

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Scheduler getCreatingScheduler() {
        return creatingScheduler;
    }

    public Scheduler getStartingScheduler() {
        return startingScheduler;
    }

    public Scheduler getReadyScheduler() {
        return readyScheduler;
    }
}
