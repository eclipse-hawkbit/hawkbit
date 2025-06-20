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
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - DistributionSetManagement<br/>
 * Story: SecurityTests DistributionSetManagement
 */
class DistributionSetManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<DistributionSet, DistributionSetCreate, DistributionSetUpdate> {

    @Override
    protected RepositoryManagement<DistributionSet, DistributionSetCreate, DistributionSetUpdate> getRepositoryManagement() {
        return distributionSetManagement;
    }

    @Override
    protected DistributionSetCreate getCreateObject() {
        return entityFactory.distributionSet().create().name("name").version("1.0.0").type("type");
    }

    @Override
    protected DistributionSetUpdate getUpdateObject() {
        return entityFactory.distributionSet().update(0L).name("a new name")
                .description("a new description").version("a new version").requiredMigrationStep(true);
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
    void getMetadataPermissiosCheck() {
        assertPermissions(() -> distributionSetManagement.getMetadata(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updateMetadataPermissionsCheck() {
        assertPermissions(() -> {
                    distributionSetManagement.updateMetadata(1L,"key", "value");
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
        distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key("type").name("name"));
        assertPermissions(() -> {
            distributionSetManagement.invalidate(entityFactory.distributionSet().create().name("name").version("1.0").type("type").build());
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }
}