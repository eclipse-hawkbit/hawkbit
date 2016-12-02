/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import java.util.List;

import org.eclipse.hawkbit.repository.AutoAssignProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler to check target filters for auto assignment of distribution sets
 */
// don't active the auto assign scheduler in test, otherwise it is hard to test
@Profile("!test")
@EnableConfigurationProperties(AutoAssignProperties.class)
public class AutoAssignScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAssignScheduler.class);

    private final TenantAware tenantAware;

    private final SystemManagement systemManagement;

    private final SystemSecurityContext systemSecurityContext;

    private final AutoAssignChecker autoAssignChecker;

    /**
     * Instantiates a new AutoAssignScheduler
     * 
     * @param tenantAware
     *            to run as specific tenant
     * @param systemManagement
     *            to find all tenants
     * @param systemSecurityContext
     *            to run as system
     * @param autoAssignChecker
     *            to run a check as tenant
     */
    public AutoAssignScheduler(final TenantAware tenantAware, final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final AutoAssignChecker autoAssignChecker) {
        this.tenantAware = tenantAware;
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.autoAssignChecker = autoAssignChecker;
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants()} and runs for each
     * tenant the auto assignments defined in the target filter queries
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = AutoAssignProperties.Scheduler.PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = AutoAssignProperties.Scheduler.PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void autoAssignScheduler() {
        LOGGER.debug("auto assign schedule checker has been triggered.");
        // run this code in system code privileged to have the necessary
        // permission to query and create entities.
        systemSecurityContext.runAsSystem(() -> {
            // workaround eclipselink that is currently not possible to
            // execute a query without multitenancy if MultiTenant
            // annotation is used.
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=355458. So
            // iterate through all tenants and execute the rollout check for
            // each tenant separately.
            final List<String> tenants = systemManagement.findTenants();
            LOGGER.info("Checking target filter queries for tenants: {}", tenants.size());
            for (final String tenant : tenants) {
                tenantAware.runAsTenant(tenant, () -> {

                    autoAssignChecker.check();

                    return null;
                });
            }
            return null;
        });
    }
}
