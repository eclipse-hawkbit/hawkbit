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
import org.eclipse.hawkbit.repository.exception.ConcurrentModificationException;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.OptimisticLockingFailureException;
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
    public void runningRolloutScheduler() {
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
                    callAndCatchConcurrentModificationException(
                            () -> rolloutManagement.checkRunningRollouts(fixedDelay));
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
    public void startingRolloutScheduler() {
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
                    callAndCatchConcurrentModificationException(
                            () -> rolloutManagement.checkStartingRollouts(fixedDelay));
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
                    callAndCatchConcurrentModificationException(
                            () -> rolloutManagement.checkCreatingRollouts(fixedDelay));
                    return null;
                });
            }
            return null;
        });
    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all
     * tenants from the {@link SystemManagement#findTenants()} and runs for each
     * tenant the {@link RolloutManagement#checkDeletingRollouts(long)} in the
     * {@link SystemSecurityContext}.
     */
    @Scheduled(initialDelayString = RolloutProperties.PROP_DELETING_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = RolloutProperties.PROP_DELETING_SCHEDULER_DELAY_PLACEHOLDER)
    public void deletingRolloutScheduler() {
        if (!rolloutProperties.getCreatingScheduler().isEnabled()) {
            return;
        }

        LOGGER.debug("rollout deleting schedule checker has been triggered.");
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
            LOGGER.info("Checking deleting rollouts for {} tenants", tenants.size());
            for (final String tenant : tenants) {
                tenantAware.runAsTenant(tenant, () -> {
                    final long fixedDelay = rolloutProperties.getDeletingScheduler().getFixedDelay();
                    callAndCatchConcurrentModificationException(
                            () -> rolloutManagement.checkDeletingRollouts(fixedDelay));
                    return null;
                });
            }
            return null;
        });
    }

    /**
     * Helper method to prevent logging exception of concurrent modification
     * exceptions which is based on the
     * {@link OptimisticLockingFailureException}.
     * 
     * There are corner cases where an concurrent modification of an entity can
     * happen.
     * 
     * E.g. during the starting of the first rollout-group, the actions are
     * created and started for the first rollout-group. After this the rollout
     * itself is set to {@link RolloutStatus#RUNNING}, but in the meantime the
     * rollout itself could be set to {@link RolloutStatus#DELETING} or
     * {@link RolloutStatus#DELETED} which will then fail into a
     * {@link OptimisticLockingFailureException} when trying to read the
     * rollout.
     * 
     * The scheduler are not locked, by means the state of the
     * {@link JpaRollout} can be changed in the meantime a scheduler is
     * currently working, e.g. starting, running next group etc. and the in case
     * the {@link JpaRollout} status is changed in the meantime another
     * scheduler will pick up the same rollout. This will lead maybe to
     * {@link OptimisticLockingFailureException} due one scheduler writes an
     * entity and the other scheduler just want to read the entity in another
     * transaction.
     * 
     * Jpa's default behavior is flush the persistince-context on each
     * find-statement. By means changing a entity in another transaction and
     * query it with the entity-refernce in another, it will lead to an
     * {@link OptimisticLockingFailureException}.
     * 
     * All in all this should not be a problem for the schedulers, because they
     * will be re-scheduled and keep continuing on the rollout's current status.
     * 
     * @param runnable
     *            the runnable to execute in a try-catch block
     */
    private static void callAndCatchConcurrentModificationException(final Runnable runnable) {
        try {
            runnable.run();
        } catch (final ConcurrentModificationException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }
}
