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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
    private final ThreadPoolTaskExecutor rolloutTaskExecutor;

    public RolloutScheduler(
        final RolloutHandler rolloutHandler, final SystemManagement systemManagement, final SystemSecurityContext systemSecurityContext,
        final int threadPoolSize) {
        this.systemManagement = systemManagement;
        this.rolloutHandler = rolloutHandler;
        this.systemSecurityContext = systemSecurityContext;
        this.rolloutTaskExecutor = threadPoolTaskExecutor(threadPoolSize);

    }

    /**
     * Scheduler method called by the spring-async mechanism. Retrieves all tenants from the {@link SystemManagement#findTenants} and
     * runs for each tenant the {@link RolloutHandler#handleAll()} in the {@link SystemSecurityContext}.
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

            systemManagement.forEachTenant(tenant -> {
                if (rolloutTaskExecutor == null) {
                    handleAll(tenant);
                } else {
                    handleAllAsync(tenant);
                }
            });
            return null;
        });
    }

    private void handleAll(String tenant) {
        log.trace("Handling rollout for tenant: {}", tenant);
        try {
            rolloutHandler.handleAll();
        } catch (Exception e) {
            log.error("Error processing rollout for tenant {}", tenant, e);
        }
    }

    private void handleAllAsync(String tenant) {
        rolloutTaskExecutor.submit(() -> systemSecurityContext.runAsSystemAsTenant(() -> {
            handleAll(tenant);
            return null;
        }, tenant));

    }

    private ThreadPoolTaskExecutor threadPoolTaskExecutor (int threadPoolSize) {
        if (threadPoolSize <= 1) {
            return null;
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setQueueCapacity(0); // forces a Synchronous Queue
        // This policy will block the submitter until a worker thread is free
        executor.setRejectedExecutionHandler(new BlockWhenFullPolicy());
        executor.setThreadNamePrefix("rollout-exec-");
        executor.initialize();
        return executor;
    }

}