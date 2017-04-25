/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rollout;

import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler to schedule the {@link RolloutManagement#handleRollouts()}. The
 * delay between the checks be be configured using the property from
 * {#PROP_SCHEDULER_DELAY_PLACEHOLDER}.
 */
public class RolloutScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolloutScheduler.class);

    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.rollout.scheduler.fixedDelay:2000}";

    private final SystemManagement systemManagement;

    private final RolloutManagement rolloutManagement;

    private final SystemSecurityContext systemSecurityContext;

    /**
     * Constructor.
     * 
     * @param systemManagement
     *            to find all tenants
     * @param rolloutManagement
     *            to run the rollout handler
     * @param systemSecurityContext
     *            to run as system
     */
    public RolloutScheduler(final SystemManagement systemManagement, final RolloutManagement rolloutManagement,
            final SystemSecurityContext systemSecurityContext) {
        this.systemManagement = systemManagement;
        this.rolloutManagement = rolloutManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants()} and runs for each
     * tenant the {@link RolloutManagement#handleRollouts()} in the
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void runningRolloutScheduler() {
        LOGGER.debug("rollout schedule checker has been triggered.");

        // run this code in system code privileged to have the necessary
        // permission to query and create entities.
        systemSecurityContext.runAsSystem(() -> {
            // workaround eclipselink that is currently not possible to
            // execute a query without multitenancy if MultiTenant
            // annotation is used.
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So
            // iterate through all tenants and execute the rollout check for
            // each tenant seperately.

            systemManagement.forEachTenant(tenant -> rolloutManagement.handleRollouts());

            return null;
        });
    }

}
