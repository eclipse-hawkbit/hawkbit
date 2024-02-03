/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rollout;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler to schedule the {@link RolloutHandler#handleAll()}. The
 * delay between the checks be configured using the property from
 * {#PROP_SCHEDULER_DELAY_PLACEHOLDER}.
 */
@Slf4j
public class RolloutScheduler {

    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.rollout.scheduler.fixedDelay:2000}";

    private final SystemManagement systemManagement;

    private final RolloutHandler rolloutHandler;

    private final SystemSecurityContext systemSecurityContext;

    /**
     * Constructor.
     * 
     * @param systemManagement
     *            to find all tenants
     * @param rolloutHandler
     *            to run the rollout handler
     * @param systemSecurityContext
     *            to run as system
     */
    public RolloutScheduler(final SystemManagement systemManagement, final RolloutHandler rolloutHandler,
            final SystemSecurityContext systemSecurityContext) {
        this.systemManagement = systemManagement;
        this.rolloutHandler = rolloutHandler;
        this.systemSecurityContext = systemSecurityContext;
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants} and runs for each
     * tenant the {@link RolloutHandler#handleAll()} in the
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void runningRolloutScheduler() {
        log.debug("rollout schedule checker has been triggered.");

        // run this code in system code privileged to have the necessary
        // permission to query and create entities.
        systemSecurityContext.runAsSystem(() -> {
            // workaround eclipselink that is currently not possible to
            // execute a query without multi-tenancy if MultiTenant
            // annotation is used.
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So
            // iterate through all tenants and execute the rollout check for
            // each tenant seperately.

            systemManagement.forEachTenant(tenant -> rolloutHandler.handleAll());

            return null;
        });
    }

}
