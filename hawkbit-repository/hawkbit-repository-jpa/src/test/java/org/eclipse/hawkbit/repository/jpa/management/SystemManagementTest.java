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

import org.eclipse.hawkbit.auth.SpRole;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
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
    void findTenantsReturnsAllTenantsNotOnlyWhichLoggedIn() {
        assertThat(systemManagement.findTenants(PAGE).getContent()).hasSize(1);

        createTestTenantsForSystemStatistics(2, 0, 0, 0);

        assertThat(systemManagement.findTenants(PAGE).getContent()).hasSize(3);
    }

    private void createTestTenantsForSystemStatistics(final int tenants, final int artifactSize, final int targets, final int updates) {
        final byte[] random = new byte[artifactSize];
        RND.nextBytes(random);

        for (int i = 0; i < tenants; i++) {
            final String tenantname = "TENANT" + i;
            SecurityContextSwitch.getAs(SecurityContextSwitch.withUserAndTenant("bumlux", tenantname, true, true, false, SpRole.SYSTEM_ROLE),
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