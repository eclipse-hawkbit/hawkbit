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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.eclipse.hawkbit.repository.test.util.DisposableSqlTestDatabaseExtension;
import org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Feature: Component Tests - Repository<br/>
 * Story: System Management
 */
@ExtendWith(DisposableSqlTestDatabaseExtension.class)
class SystemManagementTest extends AbstractJpaIntegrationTest {

    /**
     * Ensures that findTenants returns all tenants and not only restricted to the tenant which currently is logged in
     */
    @Test
    void findTenantsReturnsAllTenantsNotOnlyWhichLoggedIn() throws Exception {
        assertThat(systemManagement.findTenants(PAGE).getContent()).hasSize(1);

        createTestTenantsForSystemStatistics(2, 0, 0, 0);

        assertThat(systemManagement.findTenants(PAGE).getContent()).hasSize(3);
    }

    /**
     * Ensures that getSystemUsageStatisticsWithTenants returns the usage of all tenants and not only the first 1000 (max page size).
     */
    @Test
    void systemUsageReportCollectsStatisticsOfManyTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(1050, 0, 0, 0);

        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatisticsWithTenants().getTenants();
        assertThat(tenants).hasSize(1051); // +1 from the setup
    }

    /**
     * Checks that the system report calculates correctly the artifact size of all tenants in the system. It ignores deleted software modules with their artifacts.
     */
    @Test
    void systemUsageReportCollectsArtifactsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(2, 1234, 0, 0);

        // overall data
        assertThat(systemManagement.getSystemUsageStatistics().getOverallArtifacts()).isEqualTo(2);
        assertThat(systemManagement.getSystemUsageStatistics().getOverallArtifactVolumeInBytes()).isEqualTo(1234 * 2);

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatisticsWithTenants().getTenants();
        assertThat(tenants).hasSize(3);
        final TenantUsage tenantUsage0 = new TenantUsage("TENANT0");
        tenantUsage0.setArtifacts(1);
        tenantUsage0.setOverallArtifactVolumeInBytes(1234);
        final TenantUsage tenantUsage1 = new TenantUsage("TENANT1");
        tenantUsage1.setArtifacts(1);
        tenantUsage1.setOverallArtifactVolumeInBytes(1234);
        assertThat(tenants).containsOnly(new TenantUsage("DEFAULT"),
                tenantUsage0,
                tenantUsage1);
    }

    /**
     * Checks that the system report calculates correctly the targets size of all tenants in the system
     */
    @Test
    void systemUsageReportCollectsTargetsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(2, 0, 100, 0);

        // overall data
        assertThat(systemManagement.getSystemUsageStatistics().getOverallTargets()).isEqualTo(200);
        assertThat(systemManagement.getSystemUsageStatistics().getOverallActions()).isZero();

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatisticsWithTenants().getTenants();
        assertThat(tenants).hasSize(3);
        final TenantUsage tenantUsage0 = new TenantUsage("TENANT0");
        tenantUsage0.setTargets(100);
        final TenantUsage tenantUsage1 = new TenantUsage("TENANT1");
        tenantUsage1.setTargets(100);
        assertThat(tenants).containsOnly(new TenantUsage("DEFAULT"), tenantUsage0, tenantUsage1);
    }

    /**
     * Checks that the system report calculates correctly the actions size of all tenants in the system
     */
    @Test
    void systemUsageReportCollectsActionsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(2, 0, 20, 2);

        // 2 tenants, 100 targets each, 2 deployments per target => 400
        assertThat(systemManagement.getSystemUsageStatistics().getOverallActions()).isEqualTo(80);

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatisticsWithTenants().getTenants();
        assertThat(tenants).hasSize(3);
        final TenantUsage tenantUsage0 = new TenantUsage("TENANT0");
        tenantUsage0.setTargets(20);
        tenantUsage0.setActions(40);
        final TenantUsage tenantUsage1 = new TenantUsage("TENANT1");
        tenantUsage1.setTargets(20);
        tenantUsage1.setActions(40);
        assertThat(tenants).containsOnly(new TenantUsage("DEFAULT"), tenantUsage0, tenantUsage1);
    }

    private byte[] createTestTenantsForSystemStatistics(final int tenants, final int artifactSize, final int targets,
            final int updates) throws Exception {
        final Random randomgen = new Random();
        final byte[] random = new byte[artifactSize];
        randomgen.nextBytes(random);

        for (int i = 0; i < tenants; i++) {
            final String tenantname = "TENANT" + i;
            SecurityContextSwitch.runAs(SecurityContextSwitch.withUserAndTenant("bumlux", tenantname, true, true, false,
                    SpringEvalExpressions.SYSTEM_ROLE), () -> {
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

        return random;
    }

    private List<Target> createTestTargets(final int targets) {
        return testdataFactory.createTargets(targets, "testTargetOfTenant", "testTargetOfTenant");
    }

    private void createTestArtifact(final byte[] random) {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        artifactManagement.create(
                new ArtifactUpload(new ByteArrayInputStream(random), sm.getId(), "file1", false, random.length));
    }

    private void createDeletedTestArtifact(final byte[] random) {
        final DistributionSet ds = testdataFactory.createDistributionSet("deleted garbage", true);
        ds.getModules().forEach(module -> {
            artifactManagement.create(
                    new ArtifactUpload(new ByteArrayInputStream(random), module.getId(), "file1", false, random.length));
            softwareModuleManagement.delete(module.getId());
        });
    }
}