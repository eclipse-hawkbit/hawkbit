/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;
import static org.eclipse.hawkbit.context.AccessContext.asSystem;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withTenantAndUser;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withTenantAndUserAndAllPermissions;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withUser;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.awaitility.Awaitility;
import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.helper.SystemManagementHolder;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.test.util.DisposableSqlTestDatabaseExtension;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: System Management
 */
@ExtendWith(DisposableSqlTestDatabaseExtension.class)
class SystemManagementTest extends AbstractJpaIntegrationTest {

    // tolerance for time asserts, covers the time between taking 'now' in the test and in the tested code
    private static final long TIME_TOLERANCE_MS = 5_000;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private RepositoryProperties repositoryProperties;

    /**
     * Ensures that findTenants returns all tenants and not only restricted to the tenant which currently is logged in
     */
    @Test
    void findTenantsReturnsAllTenantsNotOnlyWhichLoggedIn() {
        assertThat(listTenants()).hasSize(1);
        createTestTenantsForSystemStatistics(2, 0, 0, 0);
        assertThat(listTenants()).hasSize(3);
    }

    /**
     * Verifies that tenant names equal after trim and upper case are the same tenant - creating a variant returns the existing one.
     */
    @Test
    void createTenantMetadataTrimmedCaseVariantReturnsExisting() {
        final TenantMetaData created = asSystem(() -> systemManagement.createTenantMetadata("acme"));

        assertThat(created.getTenant()).isEqualTo("ACME");
        assertThat(asSystem(() -> systemManagement.createTenantMetadata(" Acme ")).getId()).isEqualTo(created.getId());
        assertThat(listTenants()).containsOnlyOnce("ACME");
    }

    /**
     * Verifies that a legacy tenant stored with surrounding spaces is resolved by the trimmed upper case runtime tenant and that
     * a variant of it can't be created.
     */
    @Test
    void legacyTenantWithSpacesIsResolvedAndNotCreatedAgain() {
        final long id = asSystem(() -> systemManagement.createTenantMetadata("legacy")).getId();
        // simulates a legacy tenant - JpaTenantMetaData always normalizes the tenant, so the raw value is set with native SQL
        new JdbcTemplate(dataSource).update("UPDATE sp_tenant SET tenant = ? WHERE id = ?", " legacy ", id);

        final TenantMetaData metaData = SecurityContextSwitch.getAs(
                withTenantAndUser("LEGACY", "bumlux", new String[] { SpRole.SYSTEM_ROLE }, false, false),
                systemManagement::getTenantMetadataWithoutDetails);
        assertThat(metaData.getId()).isEqualTo(id);
        assertThat(metaData.getTenant()).isEqualTo("LEGACY");
        assertThat(listTenants()).containsOnlyOnce("LEGACY");
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> asSystem(() -> systemManagement.createTenantMetadata("LEGACY")));
    }

    /**
     * Verifies that deleting a tenant by a trimmed / case variant deletes it.
     */
    @Test
    void deleteTenantByTrimmedCaseVariant() {
        asSystem(() -> systemManagement.createTenantMetadata("todelete"));

        asSystem(() -> systemManagement.deleteTenant(" ToDelete "));

        assertThat(listTenants()).doesNotContain("TODELETE");
    }

    /**
     * Verifies that a soft delete with the configured delays sets the soft delete time to now + the configured soft delete delay,
     * the deleted by to the actor, the hard delete time to now + the configured hard delete delay (both counted from now) and
     * keeps the tenant data.
     */
    @Test
    void softDeleteWithConfiguredDelay() {
        final String tenant = "SOFT_CONFIGURED_DELAY";
        createTenantWithTarget(tenant);

        final Duration softDeleteAfter = repositoryProperties.getTenantSoftDeleteAfter();
        final Duration hardDeleteAfter = repositoryProperties.getTenantHardDeleteAfter();
        repositoryProperties.setTenantSoftDeleteAfter(Duration.ofDays(7));
        repositoryProperties.setTenantHardDeleteAfter(Duration.ofDays(30));
        try {
            final long now = System.currentTimeMillis();
            systemManagement.softDeleteTenant(tenant);

            final JpaTenantMetaData metaData = findTenantMetaData(tenant);
            assertThat(metaData.getDeletedAt()).isCloseTo(now + Duration.ofDays(7).toMillis(), within(TIME_TOLERANCE_MS));
            assertThat(metaData.getDeletedBy()).isEqualTo("bumlux");
            assertThat(metaData.getHardDeleteAt()).isCloseTo(now + Duration.ofDays(30).toMillis(), within(TIME_TOLERANCE_MS));
            assertThat(countTargets(tenant)).isEqualTo(1);
        } finally {
            repositoryProperties.setTenantSoftDeleteAfter(softDeleteAfter);
            repositoryProperties.setTenantHardDeleteAfter(hardDeleteAfter);
        }
    }

    /**
     * Verifies that a soft delete with the configured delays fails if soft deletes are disabled (the configured hard delete delay
     * is not positive) and leaves the tenant unchanged.
     */
    @Test
    void softDeleteWithConfiguredDelayFailsIfSoftDeletesDisabled() {
        final String tenant = "SOFT_DISABLED";
        createTenantWithTarget(tenant);

        final Duration hardDeleteAfter = repositoryProperties.getTenantHardDeleteAfter();
        repositoryProperties.setTenantHardDeleteAfter(Duration.ZERO);
        try {
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> systemManagement.softDeleteTenant(tenant));
        } finally {
            repositoryProperties.setTenantHardDeleteAfter(hardDeleteAfter);
        }

        assertNoDeletionMarks(findTenantMetaData(tenant));
        assertThat(countTargets(tenant)).isEqualTo(1);
    }

    /**
     * Verifies that a soft delete with given times sets the soft delete and hard delete times to them and the deleted by to the
     * actor.
     */
    @Test
    void softDeleteWithGivenTimes() {
        final String tenant = "SOFT_GIVEN_TIMES";
        createTenantWithTarget(tenant);

        final Instant softDeleteAt = Instant.now().plus(1, HOURS);
        final Instant hardDeleteAt = Instant.now().plus(2, HOURS);
        systemManagement.softDeleteTenant(tenant, softDeleteAt, hardDeleteAt);

        final JpaTenantMetaData metaData = findTenantMetaData(tenant);
        assertThat(metaData.getDeletedAt()).isEqualTo(softDeleteAt.toEpochMilli());
        assertThat(metaData.getDeletedBy()).isEqualTo("bumlux");
        assertThat(metaData.getHardDeleteAt()).isEqualTo(hardDeleteAt.toEpochMilli());
    }

    /**
     * Verifies that a soft delete with a soft delete time in the past makes the tenant soft deleted immediately and keeps its data.
     */
    @Test
    void immediateSoftDelete() {
        final String tenant = "SOFT_IMMEDIATE";
        createTenantWithTarget(tenant);

        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));

        final JpaTenantMetaData metaData = findTenantMetaData(tenant);
        assertThat(metaData.getDeletedAt()).isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(countTargets(tenant)).isEqualTo(1);
    }

    /**
     * Verifies that a soft delete with a hard delete time before the soft delete time fails and leaves the tenant unchanged.
     */
    @Test
    void softDeleteWithHardDeleteBeforeSoftDeleteFails() {
        final String tenant = "SOFT_HARD_BEFORE_SOFT";
        createTenantWithTarget(tenant);

        final Instant softDeleteAt = Instant.now().plus(2, HOURS);
        final Instant hardDeleteAt = softDeleteAt.minusMillis(1);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> systemManagement.softDeleteTenant(tenant, softDeleteAt, hardDeleteAt));

        assertNoDeletionMarks(findTenantMetaData(tenant));
    }

    /**
     * Verifies that a soft delete of an unknown tenant fails as not found.
     */
    @Test
    void softDeleteOfUnknownTenantFails() {
        final Instant softDeleteAt = Instant.now().minusSeconds(1);
        final Instant hardDeleteAt = Instant.now().plus(2, HOURS);
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> systemManagement.softDeleteTenant("GHOST", softDeleteAt, hardDeleteAt));
    }

    /**
     * Verifies that a new soft delete of a tenant with a pending (not yet effective) soft delete reschedules it - replaces the soft
     * delete, deleted by and hard delete times.
     */
    @Test
    void pendingSoftDeleteRescheduled() {
        final String tenant = "SOFT_RESCHEDULED";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().plus(1, HOURS), Instant.now().plus(2, HOURS));

        final Instant softDeleteAt = Instant.now().plus(2, ChronoUnit.DAYS);
        final Instant hardDeleteAt = Instant.now().plus(3, ChronoUnit.DAYS);
        SecurityContextSwitch.runAs(withUser("rescheduler", SpRole.SYSTEM_ROLE),
                () -> systemManagement.softDeleteTenant(tenant, softDeleteAt, hardDeleteAt));

        final JpaTenantMetaData metaData = findTenantMetaData(tenant);
        assertThat(metaData.getDeletedAt()).isEqualTo(softDeleteAt.toEpochMilli());
        assertThat(metaData.getDeletedBy()).isEqualTo("rescheduler");
        assertThat(metaData.getHardDeleteAt()).isEqualTo(hardDeleteAt.toEpochMilli());
    }

    /**
     * Verifies that a repeated soft delete of an already soft deleted tenant fails and keeps its soft delete, deleted by and hard
     * delete times.
     */
    @Test
    void repeatedSoftDeleteOfSoftDeletedTenantFails() {
        final String tenant = "SOFT_REPEATED";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));
        final JpaTenantMetaData before = findTenantMetaData(tenant);

        final Instant softDeleteAt = Instant.now().plus(1, HOURS);
        final Instant hardDeleteAt = Instant.now().plus(2, HOURS);
        SecurityContextSwitch.runAs(withUser("repeater", SpRole.SYSTEM_ROLE), () -> {
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> systemManagement.softDeleteTenant(tenant, softDeleteAt, hardDeleteAt));
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> systemManagement.softDeleteTenant(tenant));
        });

        final JpaTenantMetaData after = findTenantMetaData(tenant);
        assertThat(after.getDeletedAt()).isEqualTo(before.getDeletedAt());
        assertThat(after.getDeletedBy()).isEqualTo(before.getDeletedBy());
        assertThat(after.getHardDeleteAt()).isEqualTo(before.getHardDeleteAt());
    }

    /**
     * Verifies that a delete hard deletes an active tenant and its data.
     */
    @Test
    void immediateDeleteOfActiveTenant() {
        final String tenant = "HARD_DELETE";
        createTenantWithTarget(tenant);

        systemManagement.deleteTenant(tenant);

        assertTenantHardDeleted(tenant);
    }

    /**
     * Verifies that a delete hard deletes a soft deleted tenant and its data.
     */
    @Test
    void immediateDeleteOfSoftDeletedTenant() {
        final String tenant = "HARD_SOFT_DELETED";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));

        systemManagement.deleteTenant(tenant);

        assertTenantHardDeleted(tenant);
    }

    /**
     * Verifies that restore of a soft deleted tenant clears the deletion marks and keeps the tenant data.
     */
    @Test
    void softDeletedTenantRestored() {
        final String tenant = "RESTORE_SOFT_DELETED";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));

        systemManagement.restoreTenant(tenant);

        assertNoDeletionMarks(findTenantMetaData(tenant));
        assertThat(countTargets(tenant)).isEqualTo(1);
    }

    /**
     * Verifies that restore of a tenant with a pending soft delete cancels it.
     */
    @Test
    void pendingSoftDeleteCancelledByRestore() {
        final String tenant = "RESTORE_PENDING";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().plus(1, HOURS), Instant.now().plus(2, HOURS));

        systemManagement.restoreTenant(tenant);

        assertNoDeletionMarks(findTenantMetaData(tenant));
    }

    /**
     * Verifies that restore of an active tenant succeeds and leaves the tenant unchanged.
     */
    @Test
    void restoreOfActiveTenant() {
        final String tenant = "RESTORE_ACTIVE";
        createTenantWithTarget(tenant);
        final JpaTenantMetaData before = findTenantMetaData(tenant);

        systemManagement.restoreTenant(tenant);

        final JpaTenantMetaData after = findTenantMetaData(tenant);
        assertNoDeletionMarks(after);
        assertThat(after.getOptLockRevision()).isEqualTo(before.getOptLockRevision());
        assertThat(after.getLastModifiedAt()).isEqualTo(before.getLastModifiedAt());
    }

    /**
     * Verifies that restore of an unknown tenant fails as not found.
     */
    @Test
    void restoreOfUnknownTenantFails() {
        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> systemManagement.restoreTenant("GHOST"));
    }

    /**
     * Verifies that the tenant metadata of a soft deleted tenant isn't resolvable in its own context (even with implicit tenant
     * creation enabled) and the tenant isn't re-created - it stays soft deleted.
     */
    @Test
    void tenantMetadataOfSoftDeletedTenant() {
        final String tenant = "METADATA_SOFT_DELETED";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));
        final Long deletedAt = findTenantMetaData(tenant).getDeletedAt();

        final boolean implicitTenantCreateAllowed = repositoryProperties.isImplicitTenantCreateAllowed();
        repositoryProperties.setImplicitTenantCreateAllowed(true);
        try {
            SecurityContextSwitch.runAs(tenantUserWithoutAutoCreate(tenant), () -> {
                assertThatExceptionOfType(EntityNotFoundException.class)
                        .isThrownBy(() -> systemManagement.getTenantMetadataWithoutDetails());
                assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> systemManagement.getTenantMetadata());
            });
        } finally {
            repositoryProperties.setImplicitTenantCreateAllowed(implicitTenantCreateAllowed);
        }

        assertThat(findTenantMetaData(tenant).getDeletedAt()).isEqualTo(deletedAt);
        assertThat(countTargets(tenant)).isEqualTo(1);
    }

    /**
     * Verifies that an explicit creation of a soft deleted tenant fails as already existing.
     */
    @Test
    void explicitCreationOfSoftDeletedTenant() {
        final String tenant = "CREATE_SOFT_DELETED";
        createTenantWithTarget(tenant);
        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));

        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> systemManagement.createTenantMetadata(tenant));
    }

    /**
     * Verifies that an action executed for each tenant is executed for active tenants and tenants with a pending soft delete,
     * but not for soft deleted tenants.
     */
    @Test
    void iteratingTenantsSkipsSoftDeletedTenants() {
        createTenantWithTarget("ALPHA");
        createTenantWithTarget("PENDING");
        createTenantWithTarget("ACME");
        systemManagement.softDeleteTenant("PENDING", Instant.now().plus(1, HOURS), Instant.now().plus(2, HOURS));
        systemManagement.softDeleteTenant("ACME", Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));

        assertThat(listTenants()).contains("ALPHA", "PENDING").doesNotContain("ACME");
    }

    /**
     * Verifies the cached soft delete check of the current tenant: false for an active and an unknown tenant, true right after
     * a soft delete and false right after a restore (the local cache is evicted, the cache is warmed before each change).
     */
    @Test
    void isTenantSoftDeletedReflectsSoftDeleteAndRestore() {
        final String tenant = "CHECK_SOFT_DELETED";
        createTenantWithTarget(tenant);

        assertThat(isTenantSoftDeleted("CHECK_UNKNOWN")).isFalse();
        assertThat(isTenantSoftDeleted(tenant)).isFalse();

        systemManagement.softDeleteTenant(tenant, Instant.now().minusSeconds(1), Instant.now().plus(2, HOURS));
        assertThat(isTenantSoftDeleted(tenant)).isTrue();

        systemManagement.restoreTenant(tenant);
        assertThat(isTenantSoftDeleted(tenant)).isFalse();
    }

    /**
     * Verifies that a soft delete scheduled shortly ahead makes the cached check turn from false to true once the soft delete
     * time passed, although the cache is warm.
     */
    @Test
    void isTenantSoftDeletedTurnsTrueOnceScheduledTimePassed() {
        final String tenant = "CHECK_SCHEDULED";
        createTenantWithTarget(tenant);

        systemManagement.softDeleteTenant(tenant, Instant.now().plusSeconds(2), Instant.now().plus(2, HOURS));
        assertThat(isTenantSoftDeleted(tenant)).isFalse();

        // well below the cache TTL (10s), so the cached deletedAt is compared with the current time
        Awaitility.await().atMost(Duration.ofSeconds(6)).pollInterval(Duration.ofMillis(200))
                .until(() -> isTenantSoftDeleted(tenant));
    }

    /**
     * Verifies that the static system management holder is initialized with the system management bean (proxy).
     */
    @Test
    void systemManagementHolderInitializedWithBean() {
        assertThat(SystemManagementHolder.getInstance()).isSameAs(systemManagement);
    }

    private boolean isTenantSoftDeleted(final String tenant) {
        return SecurityContextSwitch.getAs(
                withTenantAndUser(tenant, "bumlux", new String[] { SpRole.SYSTEM_ROLE }, false, false),
                () -> systemManagement.isTenantSoftDeleted());
    }

    private void assertTenantHardDeleted(final String tenant) {
        assertThat(findTenantMetaData(tenant)).isNull();
        assertThat(countTargets(tenant)).isZero();
    }

    private static void assertNoDeletionMarks(final JpaTenantMetaData metaData) {
        assertThat(metaData.getDeletedAt()).isNull();
        assertThat(metaData.getDeletedBy()).isNull();
        assertThat(metaData.getHardDeleteAt()).isNull();
    }

    private void createTenantWithTarget(final String tenant) {
        SecurityContextSwitch.runAs(withTenantAndUserAndAllPermissions(tenant, "bumlux"),
                () -> testdataFactory.createTarget("target_of_" + tenant));
    }

    private long countTargets(final String tenant) {
        return SecurityContextSwitch.getAs(tenantUserWithoutAutoCreate(tenant), () -> targetRepository.count());
    }

    // tenant isn't auto created (via createTenantMetadata) - would fail for soft deleted tenants
    private static WithUser tenantUserWithoutAutoCreate(final String tenant) {
        return withTenantAndUser(tenant, "bumlux", new String[] { SpRole.TENANT_ADMIN }, false, false);
    }

    private JpaTenantMetaData findTenantMetaData(final String tenant) {
        return (JpaTenantMetaData) tenantMetaDataRepository.findByTenant(tenant);
    }

    private List<String> listTenants() {
        final List<String> tenants = new ArrayList<>();
        systemManagement.forEachTenantAsSystem(tenants::add);
        return tenants;
    }

    private void createTestTenantsForSystemStatistics(final int tenants, final int artifactSize, final int targets, final int updates) {
        final byte[] random = new byte[artifactSize];
        RND.nextBytes(random);

        for (int i = 0; i < tenants; i++) {
            final String tenantname = "TENANT" + i;
            SecurityContextSwitch.getAs(
                    withTenantAndUser(tenantname, "bumlux", new String[] { SpRole.SYSTEM_ROLE }, true, true),
                    () -> {
                        systemManagement.getTenantMetadataWithoutDetails();
                        if (artifactSize > 0) {
                            createTestArtifact(random);
                            createDeletedTestArtifact(random);
                        }
                        if (targets > 0) {
                            final List<Target> createdTargets = createTestTargets(targets);
                            if (updates > 0) {
                                for (int x = 0; x < updates; x++) {
                                    final DistributionSet ds = testdataFactory
                                            .createDistributionSet("to be deployed" + x, true);

                                    assignDistributionSet(ds, createdTargets);
                                }
                            }
                        }

                        return null;
                    });
        }
    }

    private List<Target> createTestTargets(final int targets) {
        return testdataFactory.createTargets(targets, "testTargetOfTenant", "testTargetOfTenant");
    }

    private void createTestArtifact(final byte[] random) {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), null, random.length, null, sm.getId(), "file1", false));
    }

    private void createDeletedTestArtifact(final byte[] random) {
        final DistributionSet ds = testdataFactory.createDistributionSet("deleted garbage", true);
        ds.getModules().forEach(module -> {
            artifactManagement.create(
                    new ArtifactUpload(new ByteArrayInputStream(random), null, random.length, null, module.getId(), "file1", false));
            softwareModuleManagement.delete(module.getId());
        });
    }
}
