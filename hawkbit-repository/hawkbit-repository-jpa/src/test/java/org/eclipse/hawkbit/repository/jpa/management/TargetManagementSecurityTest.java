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
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.TargetManagement.Update;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
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
    void countByRsqlPermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsql("controllerId==id"), List.of(SpPermission.READ_TARGET));
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
        assertPermissions(() -> targetManagement.create(Create.builder().controllerId("controller").name("name").build()),
                List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createCollectionPermissionsCheck() {
        assertPermissions(() -> targetManagement.create(List.of(Create.builder().controllerId("controller").name("name").build())),
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
    void deleteByControllerIdPermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.deleteByControllerId("controllerId");
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
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
    void countByRsqlAndNonDsAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(() -> targetManagement.countByRsqlAndNonDsAndCompatibleAndUpdatable(1L, "controllerId==id"),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatablePermissionsCheck() {
        assertPermissions(
                () -> targetManagement.findByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
                        List.of(1L), "controllerId==id", defaultDsType(), PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
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
        assertPermissions(
                () -> targetManagement.countByRsqlAndNotInRolloutGroupsAndCompatibleAndUpdatable(
                        "controllerId==id", List.of(1L), defaultDsType()),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_ROLLOUT));
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
        assertPermissions(() -> targetManagement.getByControllerId(List.of("controllerId")), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByControllerIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.getByControllerId("controllerId"), List.of(SpPermission.READ_TARGET));
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
    void assignTypeByIdPermissionsCheck() {
        assertPermissions(() -> targetManagement.assignType("controllerId", 1L), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updatePermissionsCheck() {
        assertPermissions(() -> targetManagement.update(Update.builder().id(1L).build()), List.of(SpPermission.UPDATE_TARGET));
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
    void createMetadataMapPermissionsCheck() {
        assertPermissions(
                () -> {
                    targetManagement.createMetadata("controllerId", Map.of("key", "value"));
                    return null;
                },
                List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getMetadataPermissionsCheck() {
        assertPermissions(() -> targetManagement.getMetadata("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void createMetadataPermissionsCheck() {
        assertPermissions(() -> {
                    targetManagement.createMetadata("controllerId", "key", "value");
                    return null;
                },
                List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deleteMetadataPermissionsCheck() {
        assertPermissions(() -> {
            targetManagement.deleteMetadata("controllerId", "key");
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }
}