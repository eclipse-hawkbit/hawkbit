/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rollout;

import java.util.List;

import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler to schedule the
 * {@link RolloutManagement#checkRunningRollouts(long)}. The delay between the
 * checks be be configured using the property
 * {@link #PROP_SCHEDULER_DELAY_PLACEHOLDER}.
 */
@Component
// don't active the rollout scheduler in test, otherwise it is hard to test
// rolloutmanagement and leads weird side-effects maybe.
@Profile("!test")
public class RolloutScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolloutScheduler.class);

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Autowired
    private RolloutProperties rolloutProperties;

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants()} and runs for each
     * tenant the {@link RolloutManagement#checkRunningRollouts(long)} in the
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = RolloutProperties.PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = RolloutProperties.PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void rolloutScheduler() {
        if (!rolloutProperties.getScheduler().isEnabled()) {
            return;
        }

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
            final List<String> tenants = systemManagement.findTenants();
            LOGGER.info("Checking rollouts for {} tenants", tenants.size());
            for (final String tenant : tenants) {
                tenantAware.runAsTenant(tenant, () -> {
                    final long fixedDelay = rolloutProperties.getScheduler().getFixedDelay();
                    rolloutManagement.checkRunningRollouts(fixedDelay);
                    return null;
                });
            }
            return null;
        });
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants()} and runs for each
     * tenant the {@link RolloutManagement#checkStartingRollouts(long)} in the
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = RolloutProperties.PROP_STARTING_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = RolloutProperties.PROP_STARTING_SCHEDULER_DELAY_PLACEHOLDER)
    public void staringRolloutScheduler() {
        if (!rolloutProperties.getStartingScheduler().isEnabled()) {
            return;
        }

        LOGGER.debug("rollout starting schedule checker has been triggered.");
        // run this code in system code privileged to have the necessary
        // permission to query and create entities.
        systemSecurityContext.runAsSystem(() -> {
            // workaround eclipselink that is currently not possible to
            // execute a query without multitenancy if MultiTenant
            // annotation is used.
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So
            // iterate through all tenants and execute the rollout check for
            // each tenant seperately.
            final List<String> tenants = systemManagement.findTenants();
            LOGGER.info("Checking starting rollouts for {} tenants", tenants.size());
            for (final String tenant : tenants) {
                tenantAware.runAsTenant(tenant, () -> {
                    final long fixedDelay = rolloutProperties.getStartingScheduler().getFixedDelay();
                    rolloutManagement.checkStartingRollouts(fixedDelay);
                    return null;
                });
            }
            return null;
        });
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants()} and runs for each
     * tenant the {@link RolloutManagement#checkCreatingRollouts(long)} in the
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = RolloutProperties.PROP_CREATING_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = RolloutProperties.PROP_CREATING_SCHEDULER_DELAY_PLACEHOLDER)
    public void creatingRolloutScheduler() {
        if (!rolloutProperties.getCreatingScheduler().isEnabled()) {
            return;
        }

        LOGGER.debug("rollout creating schedule checker has been triggered.");
        // run this code in system code privileged to have the necessary
        // permission to query and create entities.
        systemSecurityContext.runAsSystem(() -> {
            // workaround eclipselink that is currently not possible to
            // execute a query without multitenancy if MultiTenant
            // annotation is used.
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So
            // iterate through all tenants and execute the rollout check for
            // each tenant seperately.
            final List<String> tenants = systemManagement.findTenants();
            LOGGER.info("Checking creating rollouts for {} tenants", tenants.size());
            for (final String tenant : tenants) {
                tenantAware.runAsTenant(tenant, () -> {
                    final long fixedDelay = rolloutProperties.getCreatingScheduler().getFixedDelay();
                    rolloutManagement.checkCreatingRollouts(fixedDelay);
                    return null;
                });
            }
            return null;
        });
    }
}
