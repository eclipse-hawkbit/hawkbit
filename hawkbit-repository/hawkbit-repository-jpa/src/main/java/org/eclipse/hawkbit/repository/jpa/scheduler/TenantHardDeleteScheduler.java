/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import static org.eclipse.hawkbit.context.AccessContext.asSystem;

import java.util.concurrent.locks.Lock;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.repository.TenantMetaDataRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler that hard deletes the soft deleted tenants whose hard delete time passed. The delay between the runs is configured
 * via {@code hawkbit.tenant.hard-delete.scheduler.fixedDelay} (millis, default 1h).
 */
@Slf4j
public class TenantHardDeleteScheduler {

    private static final String LOCK_PREFIX = "tenant.hard-delete.";
    private static final String PROP_SCHEDULER_DELAY_PLACEHOLDER = "${hawkbit.tenant.hard-delete.scheduler.fixedDelay:3600000}";

    private final SystemManagement systemManagement;
    private final TenantMetaDataRepository tenantMetaDataRepository;
    private final LockRegistry<? extends Lock> lockRegistry;

    public TenantHardDeleteScheduler(
            final SystemManagement systemManagement, final TenantMetaDataRepository tenantMetaDataRepository,
            final LockRegistry<? extends Lock> lockRegistry) {
        this.systemManagement = systemManagement;
        this.tenantMetaDataRepository = tenantMetaDataRepository;
        this.lockRegistry = lockRegistry;
    }

    /**
     * Scheduler method which hard deletes all tenants due for hard delete. Failures are logged per tenant and the run continues
     * with the next tenant.
     */
    @Scheduled(initialDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER, fixedDelayString = PROP_SCHEDULER_DELAY_PLACEHOLDER)
    public void run() {
        log.debug("Tenant hard delete scheduler has been triggered.");
        // run as system code - to have the permission to query all tenants and to hard delete them
        asSystem(() -> tenantMetaDataRepository.findTenantsToHardDelete(System.currentTimeMillis()).forEach(this::hardDelete));
    }

    private void hardDelete(final String tenant) {
        final Lock lock = lockRegistry.obtain(LOCK_PREFIX + tenant);
        if (!lock.tryLock()) {
            // another node is hard deleting it
            return;
        }
        try {
            // re-check with a fresh read under the lock - the tenant could have been restored (or already hard deleted by another
            // node) since it was found as due, a restored tenant must not be hard deleted
            if (!isDueForHardDelete(tenant)) {
                log.debug("Tenant {} is no longer due for hard delete, skipped", tenant);
                return;
            }
            log.info("Hard deleting soft deleted tenant {}", tenant);
            systemManagement.deleteTenant(tenant);
        } catch (final RuntimeException e) {
            log.error("Hard delete of tenant {} failed. Continue with next tenant.", tenant, e);
        } finally {
            lock.unlock();
        }
    }

    private boolean isDueForHardDelete(final String tenant) {
        final JpaTenantMetaData metaData = (JpaTenantMetaData) tenantMetaDataRepository.findByTenant(tenant);
        final long now = System.currentTimeMillis();
        return metaData != null
                && metaData.getDeletedAt() != null && metaData.getDeletedAt() <= now
                && metaData.getHardDeleteAt() != null && metaData.getHardDeleteAt() <= now;
    }
}
