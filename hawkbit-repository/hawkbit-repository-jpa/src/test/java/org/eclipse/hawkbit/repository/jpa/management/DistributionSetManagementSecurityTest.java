/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - DistributionSetManagement<br/>
 * Story: SecurityTests DistributionSetManagement
 */
class DistributionSetManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<DistributionSet, DistributionSetManagement.Create, DistributionSetManagement.Update> {

    @Override
    protected DistributionSetManagement findRepositoryManagement() {
        return distributionSetManagement;
    }

    @Override
    protected DistributionSetManagement.Create findCreateObject() {
        return DistributionSetManagement.Create.builder().name("name").version("1.0.0").type(defaultDsType()).build();
    }

    @Override
    protected DistributionSetManagement.Update findUpdateObject() {
        return DistributionSetManagement.Update.builder().id(0L).name("a new name")
                .description("a new description").version("a new version").requiredMigrationStep(true).build();
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignSoftwareModulesPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.assignSoftwareModules(1L, List.of(1L)), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.assignTag(List.of(1L), 1L),
                List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests that the method throws InsufficientPermissionException when the user does not have the correct permission
     */
    @Test
    void unassignTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.unassignTag(List.of(1L), 1L),
                List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createMetadataMapPermissionsCheck() {
        assertPermissions(
                () -> {
                    distributionSetManagement.createMetadata(1L, Map.of("key", "value"));
                    return null;
                },
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getMetadataPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getMetadata(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createMetadataPermissionsCheck() {
        assertPermissions(() -> {
                    distributionSetManagement.createMetadata(1L, "key", "value");
                    return null;
                },
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deleteMetadataPermissionsCheck() {
        assertPermissions(() -> {
            distributionSetManagement.deleteMetadata(1L, "key");
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void lockPermissionsCheck() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        assertPermissions(() -> distributionSetManagement.lock(ds), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unlockPermissionsCheck() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        assertPermissions(() -> distributionSetManagement.unlock(ds), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getWithDetailsPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getWithDetails(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByNameAndVersionPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByNameAndVersion("name", "version"), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getValidAndCompletePermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getValidAndComplete(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.get(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByTag(1L, PAGE), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlAndTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByRsqlAndTag("rsql", 1L, PAGE), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unassignSoftwareModulePermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.unassignSoftwareModule(1L, 1L), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countRolloutsByStatusForDistributionSetPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countRolloutsByStatusForDistributionSet(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countActionsByStatusForDistributionSetPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countActionsByStatusForDistributionSet(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countAutoAssignmentsForDistributionSetPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countAutoAssignmentsForDistributionSet(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void invalidatePermissionsCheck() {
        final DistributionSet ds = testdataFactory.createDistributionSet();
        assertPermissions(() -> distributionSetManagement.invalidate(ds),
                List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }
}