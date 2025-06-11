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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - TargetManagement")
@Story("SecurityTests TargetManagement")
class TargetManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByAssignedDistributionSet(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByFiltersPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByFilters(new FilterParams(null, null, null, null, null, null)),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByInstalledDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByInstalledDistributionSet(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void existsByInstalledOrAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.existsByInstalledOrAssignedDistributionSet(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsql("controllerId==id"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByRsqlAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndUpdatable("controllerId==id"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByRsqlAndCompatiblePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndCompatible("controllerId==id", 1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByRsqlAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndCompatibleAndUpdatable("controllerId==id", 1L),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByFailedInRolloutPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByFailedInRollout("1", 1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countPermissionsCheck() {
        assertPermissions(() -> targetManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createPermissionsCheck() {
        assertPermissions(() -> targetManagement.create(entityFactory.target().create().controllerId("controller").name("name")),
                List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createCollectionPermissionsCheck() {
        assertPermissions(() -> targetManagement.create(List.of(entityFactory.target().create().controllerId("controller").name("name"))),
                List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.delete(List.of(1L));
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deleteByControllerIDPermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.deleteByControllerID("controllerId");
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByTargetFilterQueryPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByTargetFilterQuery(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(1L, "controllerId==id", PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByRsqlAndNonDSAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndNonDSAndCompatibleAndUpdatable(1L, "controllerId==id"),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(
                () -> targetManagement.findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(List.of(1L), "controllerId==id",
                        entityFactory.distributionSetType().create().build(), PAGE
                ), List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByActionsInRolloutGroupPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByActionsInRolloutGroup(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable("controllerId==id", List.of(1L),
                entityFactory.distributionSetType().create().build()), List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByFailedRolloutAndNotInRolloutGroupsPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByFailedRolloutAndNotInRolloutGroups("1", List.of(1L), PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByFailedRolloutAndNotInRolloutGroupsPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByFailedRolloutAndNotInRolloutGroups("1", List.of(1L)),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByInRolloutGroupWithoutActionPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByInRolloutGroupWithoutAction(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByAssignedDistributionSet(1L, PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByAssignedDistributionSetAndRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByAssignedDistributionSetAndRsql(1L, "controllerId==id", PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByControllerCollectionIDPermissionsCheck() {
        assertPermissions(() -> targetManagement.getByControllerID(List.of("controllerId")), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByControllerIDPermissionsCheck() {
        assertPermissions(() -> targetManagement.getByControllerID("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByFiltersPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByFilters(new FilterParams(null, null, null, null, null, null), PAGE),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByInstalledDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByInstalledDistributionSet(1L, PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByInstalledDistributionSetAndRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByInstalledDistributionSetAndRsql(1L, "controllerId==id", PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByUpdateStatusPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByUpdateStatus(TargetUpdateStatus.IN_SYNC, PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findAllPermissionsCheck() {
        assertPermissions(() -> targetManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByRsql("controllerId==id", PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByTargetFilterQueryPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByTargetFilterQuery(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByTag(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRsqlAndTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByRsqlAndTag("controllerId==id", 1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignTypePermissionsCheck() {
        assertPermissions(() -> targetManagement.assignType(List.of("controllerId"), 1L), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unassignTypeByIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignType("controllerId"), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignTagWithHandlerPermissionsCheck() {
        assertPermissions(() -> targetManagement.assignTag(List.of("controllerId"), 1L, strings -> {}),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.assignTag(List.of("controllerId"), 1L),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unassignTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignTag(List.of("controllerId"), 1L), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unassignTagWithHandlerPermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignTag(List.of("controllerId"), 1L, strings -> {}),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unassignTypePermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignType(List.of("controllerId")), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignTypeByIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.assignType("controllerId", 1L), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updatePermissionsCheck() {
        assertPermissions(() -> targetManagement.update(entityFactory.target().update("controllerId")), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getPermissionsCheck() {
        assertPermissions(() -> targetManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getCollectionPermissionsCheck() {
        assertPermissions(() -> targetManagement.get(List.of(1L)), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void existsByControllerIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.existsByControllerId("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(
                () -> targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable("controllerId", 1L, "controllerId==id"),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getTagsPermissionsCheck() {
        assertPermissions(() -> targetManagement.getTags("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getControllerAttributesPermissionsCheck() {
        assertPermissions(() -> targetManagement.getControllerAttributes("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void requestControllerAttributesPermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.requestControllerAttributes("controllerId");
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void isControllerAttributesRequestedPermissionsCheck() {
        assertPermissions(() -> targetManagement.isControllerAttributesRequested("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByControllerAttributesRequestedPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByControllerAttributesRequested(PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createMetadataPermissionsCheck() {
        assertPermissions(
                () -> {
                    targetManagement.createMetadata("controllerId", Map.of("key", "value"));
                    return null;
                },
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getMetadataPermissionsCheck() {
        assertPermissions(() -> targetManagement.getMetadata("controllerId"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void updateMetadataPermissionsCheck() {
        assertPermissions(() -> {
                    targetManagement.updateMetadata("controllerId", "key", "value");
                    return null;
                },
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deleteMetadataPermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.deleteMetadata("controllerId", "key");
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }
}