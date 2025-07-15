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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - TargetManagement<br/>
 * Story: SecurityTests TargetManagement
 */
@Slf4j
class TargetManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByAssignedDistributionSet(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByFiltersPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByFilters(new FilterParams(null, null, null, null, null, null)),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByInstalledDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByInstalledDistributionSet(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void existsByInstalledOrAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.existsByInstalledOrAssignedDistributionSet(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsql("controllerId==id"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByRsqlAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndUpdatable("controllerId==id"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByRsqlAndCompatiblePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndCompatible("controllerId==id", 1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByRsqlAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndCompatibleAndUpdatable("controllerId==id", 1L),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByFailedInRolloutPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByFailedInRollout("1", 1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countPermissionsCheck() {
        assertPermissions(() -> targetManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createPermissionsCheck() {
        assertPermissions(() -> targetManagement.create(entityFactory.target().create().controllerId("controller").name("name")),
                List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createCollectionPermissionsCheck() {
        assertPermissions(() -> targetManagement.create(List.of(entityFactory.target().create().controllerId("controller").name("name"))),
                List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.delete(List.of(1L));
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deleteByControllerIDPermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.deleteByControllerID("controllerId");
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByTargetFilterQueryPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByTargetFilterQuery(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(1L, "controllerId==id", PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByRsqlAndNonDSAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndNonDSAndCompatibleAndUpdatable(1L, "controllerId==id"),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(
                () -> targetManagement.findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(List.of(1L), "controllerId==id",
                        entityFactory.distributionSetType().create().build(), PAGE
                ), List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByActionsInRolloutGroupPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByActionsInRolloutGroup(1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable("controllerId==id", List.of(1L),
                entityFactory.distributionSetType().create().build()), List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByFailedRolloutAndNotInRolloutGroupsPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByFailedRolloutAndNotInRolloutGroups("1", List.of(1L), PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByFailedRolloutAndNotInRolloutGroupsPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByFailedRolloutAndNotInRolloutGroups("1", List.of(1L)),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByInRolloutGroupWithoutActionPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByInRolloutGroupWithoutAction(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByAssignedDistributionSet(1L, PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByAssignedDistributionSetAndRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByAssignedDistributionSetAndRsql(1L, "controllerId==id", PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByControllerCollectionIDPermissionsCheck() {
        assertPermissions(() -> targetManagement.getByControllerID(List.of("controllerId")), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByControllerIDPermissionsCheck() {
        assertPermissions(() -> targetManagement.getByControllerID("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByFiltersPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByFilters(new FilterParams(null, null, null, null, null, null), PAGE),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByInstalledDistributionSetPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByInstalledDistributionSet(1L, PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByInstalledDistributionSetAndRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByInstalledDistributionSetAndRsql(1L, "controllerId==id", PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByUpdateStatusPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByUpdateStatus(TargetUpdateStatus.IN_SYNC, PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findAllPermissionsCheck() {
        assertPermissions(() -> targetManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByRsql("controllerId==id", PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByTargetFilterQueryPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByTargetFilterQuery(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByTag(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlAndTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByRsqlAndTag("controllerId==id", 1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignTypePermissionsCheck() {
        assertPermissions(() -> targetManagement.assignType(List.of("controllerId"), 1L), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unassignTypeByIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignType("controllerId"), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignTagWithHandlerPermissionsCheck() {
        assertPermissions(() -> targetManagement.assignTag(List.of("controllerId"), 1L, strings -> {}),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.assignTag(List.of("controllerId"), 1L),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unassignTagPermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignTag(List.of("controllerId"), 1L), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unassignTagWithHandlerPermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignTag(List.of("controllerId"), 1L, strings -> {}),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unassignTypePermissionsCheck() {
        assertPermissions(() -> targetManagement.unassignType(List.of("controllerId")), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignTypeByIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.assignType("controllerId", 1L), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updatePermissionsCheck() {
        assertPermissions(() -> targetManagement.update(entityFactory.target().update("controllerId")), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getPermissionsCheck() {
        assertPermissions(() -> targetManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getCollectionPermissionsCheck() {
        assertPermissions(() -> targetManagement.get(List.of(1L)), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void existsByControllerIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.existsByControllerId("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(
                () -> targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable("controllerId", 1L, "controllerId==id"),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getTagsPermissionsCheck() {
        assertPermissions(() -> targetManagement.getTags("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getControllerAttributesPermissionsCheck() {
        assertPermissions(() -> targetManagement.getControllerAttributes("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void requestControllerAttributesPermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.requestControllerAttributes("controllerId");
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void isControllerAttributesRequestedPermissionsCheck() {
        assertPermissions(() -> targetManagement.isControllerAttributesRequested("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByControllerAttributesRequestedPermissionsCheck() {
        assertPermissions(() -> targetManagement.findByControllerAttributesRequested(PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createMetadataPermissionsCheck() {
        assertPermissions(
                () -> {
                    targetManagement.createMetadata("controllerId", Map.of("key", "value"));
                    return null;
                },
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getMetadataPermissionsCheck() {
        assertPermissions(() -> targetManagement.getMetadata("controllerId"), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_REPOSITORY })
    void updateMetadataPermissionsCheck() {
        assertPermissions(() -> {
                    targetManagement.updateMetadata("controllerId", "key", "value");
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
            targetManagement.deleteMetadata("controllerId", "key");
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }
}