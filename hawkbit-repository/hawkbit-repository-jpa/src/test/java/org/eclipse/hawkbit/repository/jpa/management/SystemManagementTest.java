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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.context.AccessContext.asSystem;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.withTenantAndUser;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.test.util.DisposableSqlTestDatabaseExtension;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
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

    @Autowired
    private DataSource dataSource;

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
