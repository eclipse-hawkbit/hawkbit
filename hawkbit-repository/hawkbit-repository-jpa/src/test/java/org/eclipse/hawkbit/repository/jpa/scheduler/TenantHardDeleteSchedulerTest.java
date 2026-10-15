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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withTenantAndUser;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withTenantAndUserAndAllPermissions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.Lock;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * Test class for {@link TenantHardDeleteScheduler}.
 * <p/>
 * Feature: Component Tests - Repository<br/>
 * Story: Tenant hard delete scheduler
 */
@SuppressWarnings("java:S6813") // constructor injects are not possible for test classes
class TenantHardDeleteSchedulerTest extends AbstractJpaIntegrationTest {

    @Autowired
    private LockRegistry<Lock> lockRegistry;


    /**
     * Verifies that a soft deleted tenant whose hard delete time passed is hard deleted together with its data.
     */
    @Test
    void dueTenantHardDeleted() {
        final String tenant = "HARD_DELETE_DUE";
        createTenantWithTarget(tenant);
        softDeleteDueForHardDelete(tenant);

        new TenantHardDeleteScheduler(systemManagement, tenantMetaDataRepository, lockRegistry).run();

        assertThat(findTenantMetaData(tenant)).isNull();
        assertThat(countTargets(tenant)).isZero();
    }

    /**
     * Verifies that a soft deleted tenant whose hard delete time didn't pass yet is kept with its data.
     */
    @Test
    void tenantBeforeHardDeleteTimeKept() {
        final String tenant = "HARD_DELETE_NOT_DUE";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(1, ChronoUnit.HOURS));

        new TenantHardDeleteScheduler(systemManagement, tenantMetaDataRepository, lockRegistry).run();

        assertThat(findTenantMetaData(tenant).getDeletedAt()).isNotNull(); // still soft deleted
        assertThat(countTargets(tenant)).isEqualTo(1);
    }

    /**
     * Verifies that a tenant restored before the scheduler run is kept with its data.
     */
    @Test
    void restoredTenantKept() {
        final String tenant = "HARD_DELETE_RESTORED";
        createTenantWithTarget(tenant);
        softDeleteDueForHardDelete(tenant);
        systemManagement.restoreTenant(tenant);

        new TenantHardDeleteScheduler(systemManagement, tenantMetaDataRepository, lockRegistry).run();

        assertActiveWithTarget(tenant);
    }

    /**
     * Verifies that a tenant restored after it was found as due, but before it is hard deleted (while the scheduler obtains the
     * lock), is kept with its data - the scheduler re-checks it right before the hard delete.
     */
    @Test
    void tenantRestoredWhileHardDeletingKept() {
        final String tenant = "HARD_DELETE_RESTORE_RACE";
        createTenantWithTarget(tenant);
        softDeleteDueForHardDelete(tenant);
        final LockRegistry<Lock> restoringLockRegistry = key -> {
            systemManagement.restoreTenant(tenant);
            return lockRegistry.obtain(key);
        };

        new TenantHardDeleteScheduler(systemManagement, tenantMetaDataRepository, restoringLockRegistry).run();

        assertActiveWithTarget(tenant);
    }

    private void assertActiveWithTarget(final String tenant) {
        final JpaTenantMetaData metaData = findTenantMetaData(tenant);
        assertThat(metaData).isNotNull();
        assertThat(metaData.getDeletedAt()).isNull();
        assertThat(countTargets(tenant)).isEqualTo(1);
    }

    // soft delete with soft and hard delete times now - the tenant is due for hard delete at the next run
    private void softDeleteDueForHardDelete(final String tenant) {
        final Instant now = Instant.now();
        systemManagement.softDeleteTenant(tenant, now, now);
    }

    private void createTenantWithTarget(final String tenant) {
        SecurityContextSwitch.runAs(withTenantAndUserAndAllPermissions(tenant, "bumlux"),
                () -> testdataFactory.createTarget("target_of_" + tenant));
    }

    // tenant isn't auto created (via createTenantMetadata) - would fail for soft deleted tenants
    private long countTargets(final String tenant) {
        return SecurityContextSwitch.getAs(
                withTenantAndUser(tenant, "bumlux", new String[] { SpRole.TENANT_ADMIN }, false, false),
                () -> targetRepository.count());
    }

    private JpaTenantMetaData findTenantMetaData(final String tenant) {
        return (JpaTenantMetaData) tenantMetaDataRepository.findByTenant(tenant);
    }
}
