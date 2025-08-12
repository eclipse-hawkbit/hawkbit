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
import java.util.Set;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpRole;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Feature: SecurityTests - DeploymentManagement<br/>
 * Story: SecurityTests DeploymentManagement
 */
class DeploymentManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignDistributionSetsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.assignDistributionSets(
                        List.of(new DeploymentRequest("controllerId", 1L, Action.ActionType.SOFT, 1L, 1, "maintenanceSchedule",
                                "maintenanceWindowDuration", "maintenanceWindowTimeZone", true))),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignDistributionSetsWithInitiatedByPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.assignDistributionSets("initiator",
                        List.of(new DeploymentRequest("controllerId", 1L, Action.ActionType.SOFT, 1L, 1, "maintenanceSchedule",
                                "maintenanceWindowDuration", "maintenanceWindowTimeZone", true)), "message"),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void offlineAssignedDistributionSetsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.offlineAssignedDistributionSets(List.of()),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void offlineAssignedDistributionSetsWithInitiatedByPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.offlineAssignedDistributionSets("initiator", List.of()),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void cancelActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.cancelAction(1L), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countActionsByTargetWithFilterPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionsByTarget("rsqlParam", "controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionsByTarget("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countActionsAllPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionsAll(), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countActionsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActions("id==1"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findAction(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    void findActionsAllPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionsAll(Pageable.unpaged()), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActionsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActions("id==1", Pageable.unpaged()), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionsByTarget("rsql==param", "controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActionsByTargetWithControllerIdPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionsByTarget("controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActionStatusByActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionStatusByAction(1L, Pageable.unpaged()), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countActionStatusByActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionStatusByAction(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findMessagesByActionStatusIdPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findMessagesByActionStatusId(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActionWithDetailsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionWithDetails(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActiveActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActiveActionsByTarget("controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findInActiveActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findInActiveActionsByTarget("controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findActiveActionsWithHighestWeightPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActiveActionsWithHighestWeight("controllerId", 1), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void forceQuitActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.forceQuitAction(1L), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void forceTargetActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.forceTargetAction(1L), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void cancelInactiveScheduledActionsForTargetsPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.cancelInactiveScheduledActionsForTargets(List.of(1L));
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void startScheduledActionsByRolloutGroupParentPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.startScheduledActionsByRolloutGroupParent(1L, 1L, 1L);
            return null;
        }, List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void startScheduledActionsPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.startScheduledActions(List.of());
            return null;
        }, List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.getAssignedDistributionSet("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getInstalledDistributionSetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.getInstalledDistributionSet("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deleteActionsByStatusAndLastModifiedBeforePermissionsCheck() {
        assertPermissions(() -> deploymentManagement.deleteActionsByStatusAndLastModifiedBefore(Set.of(Action.Status.CANCELED), 1L),
                List.of(SpRole.SYSTEM_ROLE));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void hasPendingCancellationsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.hasPendingCancellations(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void cancelActionsForDistributionSetPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.cancelActionsForDistributionSet(
                    DistributionSetInvalidation.CancelationType.FORCE, new JpaDistributionSet());
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }
}