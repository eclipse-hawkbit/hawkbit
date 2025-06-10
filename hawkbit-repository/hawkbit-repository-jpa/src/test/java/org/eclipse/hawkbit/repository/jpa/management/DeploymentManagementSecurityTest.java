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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@Feature("SecurityTests - DeploymentManagement")
@Story("SecurityTests DeploymentManagement")
class DeploymentManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignDistributionSetsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.assignDistributionSets(
                        List.of(new DeploymentRequest("controllerId", 1L, Action.ActionType.SOFT, 1L, 1, "maintenanceSchedule",
                                "maintenanceWindowDuration", "maintenanceWindowTimeZone", true))),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignDistributionSetsWithInitiatedByPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.assignDistributionSets("initiator",
                        List.of(new DeploymentRequest("controllerId", 1L, Action.ActionType.SOFT, 1L, 1, "maintenanceSchedule",
                                "maintenanceWindowDuration", "maintenanceWindowTimeZone", true)), "message"),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void offlineAssignedDistributionSetsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.offlineAssignedDistributionSets(List.of()), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void offlineAssignedDistributionSetsWithInitiatedByPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.offlineAssignedDistributionSets(List.of(), "initiator"),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void cancelActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.cancelAction(1L), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countActionsByTargetWithFilterPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionsByTarget("rsqlParam", "controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionsByTarget("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countActionsAllPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionsAll(), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countActionsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActions("id==1"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findAction(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    void findActionsAllPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionsAll(Pageable.unpaged()), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActionsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActions("id==1", Pageable.unpaged()), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionsByTarget("rsql==param", "controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActionsByTargetWithControllerIdPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionsByTarget("controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActionStatusByActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionStatusByAction(1L, Pageable.unpaged()), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countActionStatusByActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.countActionStatusByAction(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findMessagesByActionStatusIdPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findMessagesByActionStatusId(1L, PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActionWithDetailsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActionWithDetails(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActiveActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActiveActionsByTarget("controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findInActiveActionsByTargetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findInActiveActionsByTarget("controllerId", Pageable.unpaged()),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findActiveActionsWithHighestWeightPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.findActiveActionsWithHighestWeight("controllerId", 1), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void forceQuitActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.forceQuitAction(1L), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void forceTargetActionPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.forceTargetAction(1L), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void cancelInactiveScheduledActionsForTargetsPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.cancelInactiveScheduledActionsForTargets(List.of(1L));
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void startScheduledActionsByRolloutGroupParentPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.startScheduledActionsByRolloutGroupParent(1L, 1L, 1L);
            return null;
        }, List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void startScheduledActionsPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.startScheduledActions(List.of());
            return null;
        }, List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getAssignedDistributionSetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.getAssignedDistributionSet("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getInstalledDistributionSetPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.getInstalledDistributionSet("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deleteActionsByStatusAndLastModifiedBeforePermissionsCheck() {
        assertPermissions(() -> deploymentManagement.deleteActionsByStatusAndLastModifiedBefore(Set.of(Action.Status.CANCELED), 1L),
                List.of(SpPermission.SpringEvalExpressions.SYSTEM_ROLE));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void hasPendingCancellationsPermissionsCheck() {
        assertPermissions(() -> deploymentManagement.hasPendingCancellations(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void cancelActionsForDistributionSetPermissionsCheck() {
        assertPermissions(() -> {
            deploymentManagement.cancelActionsForDistributionSet(DistributionSetInvalidation.CancelationType.FORCE,
                    entityFactory.distributionSet().create().build());
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }
}