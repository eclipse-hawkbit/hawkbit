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
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - DistributionSetManagement<br/>
 * Story: SecurityTests DistributionSetManagement
 */
class DistributionSetManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<DistributionSet, DistributionSetManagement.Create, DistributionSetManagement.Update> {

    @Override
    protected DistributionSetManagement getRepositoryManagement() {
        return distributionSetManagement;
    }

    @Override
    protected DistributionSetManagement.Create getCreateObject() {
        return DistributionSetManagement.Create.builder().name("name").version("1.0.0").type(defaultDsType()).build();
    }

    @Override
    protected DistributionSetManagement.Update getUpdateObject() {
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
    void createMetadataPermissionsCheck() {
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
    void updateMetadataPermissionsCheck() {
        assertPermissions(() -> {
                    distributionSetManagement.updateMetadata(1L, "key", "value");
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
        assertPermissions(() -> {
            distributionSetManagement.lock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unlockPermissionsCheck() {
        assertPermissions(() -> {
            distributionSetManagement.unlock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByActionPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByAction(1L), List.of(SpPermission.READ_REPOSITORY));
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
    void getValidPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getValid(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getOrElseThrowExceptionPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getOrElseThrowException(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByCompletedPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByCompleted(true, PAGE), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByCompletedPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countByCompleted(true), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByDistributionSetFilterPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByDistributionSetFilter(DistributionSetFilter.builder().build(), PAGE),
                List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByDistributionSetFilterPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countByDistributionSetFilter(DistributionSetFilter.builder().build()),
                List.of(SpPermission.READ_REPOSITORY));
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
    void isInUsePermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.isInUse(1L), List.of(SpPermission.READ_REPOSITORY));
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
    void countByTypeIdPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countByTypeId(1L), List.of(SpPermission.READ_REPOSITORY));
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
        final DistributionSetType dsType = distributionSetTypeManagement.create(DistributionSetTypeManagement.Create.builder()
                .key("type").name("name").build());
        final DistributionSet ds = distributionSetManagement.create(DistributionSetManagement.Create.builder()
                .type(dsType).name("test").version("1.0.0").build());
        assertPermissions(() -> {
            ((DistributionSetManagement) distributionSetManagement).invalidate(ds);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }
}