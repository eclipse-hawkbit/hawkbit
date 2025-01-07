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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - DistributionSetManagement")
@Story("SecurityTests DistributionSetManagement")
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

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    public void assignSoftwareModulesPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.assignSoftwareModules(1L, List.of(1L)), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.assignTag(List.of(1L), 1L),
                List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    void unassignTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.unassignTag(List.of(1L), 1L),
                List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createMetaDataPermissionsCheck() {
        assertPermissions(
                () -> distributionSetManagement.createMetaData(1L, List.of(entityFactory.generateTargetMetadata("key", "value"))),
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deleteMetaDataPermissionsCheck() {
        assertPermissions(() -> {
            distributionSetManagement.deleteMetaData(1L, "key");
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void lockPermissionsCheck() {
        assertPermissions(() -> {
            distributionSetManagement.lock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unlockPermissionsCheck() {
        assertPermissions(() -> {
            distributionSetManagement.unlock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByActionPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getByAction(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getWithDetailsPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getWithDetails(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByNameAndVersionPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getByNameAndVersion("name", "version"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getValidAndCompletePermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getValidAndComplete(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getValidPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getValid(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getOrElseThrowExceptionPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getOrElseThrowException(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findMetaDataByDistributionSetIdPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findMetaDataByDistributionSetId(PAGE, 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countMetaDataByDistributionSetIdPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countMetaDataByDistributionSetId(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findMetaDataByDistributionSetIdAndRsqlPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findMetaDataByDistributionSetIdAndRsql(PAGE, 1L, "rsql"),
                List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByCompletedPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByCompleted(PAGE, true), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByCompletedPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countByCompleted(true), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByDistributionSetFilterPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByDistributionSetFilter(PAGE, DistributionSetFilter.builder().build()),
                List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByDistributionSetFilterPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countByDistributionSetFilter(DistributionSetFilter.builder().build()),
                List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByTag(PAGE, 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRsqlAndTagPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.findByRsqlAndTag(PAGE, "rsql", 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getMetaDataByDistributionSetIdPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.getMetaDataByDistributionSetId(1L, "key"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void isInUsePermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.isInUse(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unassignSoftwareModulePermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.unassignSoftwareModule(1L, 1L), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updateMetaDataPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.updateMetaData(1L, entityFactory.generateDsMetadata("key", "value")),
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByTypeIdPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countByTypeId(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countRolloutsByStatusForDistributionSetPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countRolloutsByStatusForDistributionSet(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countActionsByStatusForDistributionSetPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countActionsByStatusForDistributionSet(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countAutoAssignmentsForDistributionSetPermissionsCheck() {
        assertPermissions(() -> distributionSetManagement.countAutoAssignmentsForDistributionSet(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void invalidatePermissionsCheck() {
        assertPermissions(() -> {
            distributionSetManagement.invalidate(entityFactory.distributionSet().create().name("name").version("1.0").type("type").build());
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY, SpPermission.READ_REPOSITORY));
    }
}