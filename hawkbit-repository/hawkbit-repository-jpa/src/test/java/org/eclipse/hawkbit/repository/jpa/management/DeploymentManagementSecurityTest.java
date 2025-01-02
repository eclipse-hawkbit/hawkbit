package org.eclipse.hawkbit.repository.jpa.management;

import static org.assertj.core.api.Assertions.assertThatNoException;

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
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@Feature("SecurityTests - DeploymentManagement")
@Story("SecurityTests DeploymentManagement")
class DeploymentManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET })
    void assignDistributionSetsWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.assignDistributionSets(
                List.of(new DeploymentRequest("controllerId", 1L, Action.ActionType.SOFT, 1L, 1, "maintenanceSchedule",
                        "maintenanceWindowDuration", "maintenanceWindowTimeZone", true))));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.READ_TARGET })
    void assignDistributionSetsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.assignDistributionSets(List.of()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET })
    void assignDistributionSetsWithInitiatedByWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.assignDistributionSets("initiator",
                List.of(new DeploymentRequest("controllerId", 1L, Action.ActionType.SOFT, 1L, 1, "maintenanceSchedule",
                        "maintenanceWindowDuration", "maintenanceWindowTimeZone", true)), "message"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.READ_TARGET })
    void assignDistributionSetsWithInitiatedByWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.assignDistributionSets("initiator", List.of(), "message"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY })
    void offlineAssignedDistributionSetsWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.offlineAssignedDistributionSets(List.of()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.READ_TARGET })
    void offlineAssignedDistributionSetsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.offlineAssignedDistributionSets(List.of()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET })
    void offlineAssignedDistributionSetsWithInitiatedByWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.offlineAssignedDistributionSets(List.of(), "initiator"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.READ_TARGET })
    void offlineAssignedDistributionSetsWithInitiatedByWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.offlineAssignedDistributionSets(List.of(), "initiator"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void cancelActionWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.cancelAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.READ_TARGET })
    void cancelActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.cancelAction(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countActionsByTargetWithFilterWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.countActionsByTarget("rsqlParam", "controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void countActionsByTargetWithFilterWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.countActionsByTarget("rsqlParam", "controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countActionsByTargetWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.countActionsByTarget("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void countActionsByTargetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.countActionsByTarget("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countActionsAllWithPermissionWorks() {
        assertThatNoException().isThrownBy(() -> deploymentManagement.countActionsAll());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void countActionsAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.countActionsAll());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countActionsWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.countActions("id==1"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void countActionsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.countActions("rsqlParam"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActionWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void findActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findAction(1L));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActionsAllWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActionsAll(Pageable.unpaged()));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findActionsAllWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActionsAll(Pageable.unpaged()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActionsWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActions("id==1", Pageable.unpaged()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findActionsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActions("rsql==param", Pageable.unpaged()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActionsByTargetWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActionsByTarget("rsql==param", "controllerId", Pageable.unpaged()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findActionsByTargetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActionsByTarget("rsql==param", "controllerId", Pageable.unpaged()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActionsByTargetWithControllerIdWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActionsByTarget("controllerId", Pageable.unpaged()));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findActionsByTargetWithControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActionsByTarget("controllerId", Pageable.unpaged()));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActionStatusByActionWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActionStatusByAction(Pageable.unpaged(), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findActionStatusByActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActionStatusByAction(Pageable.unpaged(), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void countActionStatusByActionWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.countActionStatusByAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void countActionStatusByActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.countActionStatusByAction(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findMessagesByActionStatusIdWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findMessagesByActionStatusId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findMessagesByActionStatusIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findMessagesByActionStatusId(PAGE, 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActionWithDetailsWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActionWithDetails(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findActionWithDetailsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActionWithDetails(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActiveActionsByTargetWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActiveActionsByTarget(Pageable.unpaged(), "controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void findActiveActionsByTargetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActiveActionsByTarget(Pageable.unpaged(), "controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findInActiveActionsByTargetWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findInActiveActionsByTarget(Pageable.unpaged(), "controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void findInActiveActionsByTargetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findInActiveActionsByTarget(Pageable.unpaged(), "controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActiveActionsWithHighestWeightWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.findActiveActionsWithHighestWeight("controllerId", 1));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void findActiveActionsWithHighestWeightWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.findActiveActionsWithHighestWeight("controllerId", 1));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void forceQuitActionWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.forceQuitAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.READ_TARGET })
    void forceQuitActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.forceQuitAction(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void forceTargetActionWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.forceTargetAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.DELETE_TARGET, SpPermission.CREATE_TARGET })
    void forceTargetActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.forceTargetAction(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void cancelInactiveScheduledActionsForTargetsWithPermissionWorks() {
        assertPermissionWorks(() -> {
            deploymentManagement.cancelInactiveScheduledActionsForTargets(List.of(1L));
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.READ_TARGET })
    void cancelInactiveScheduledActionsForTargetsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            deploymentManagement.cancelInactiveScheduledActionsForTargets(List.of(1L));
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void startScheduledActionsByRolloutGroupParentWithPermissionWorks() {
        assertPermissionWorks(() -> {
            deploymentManagement.startScheduledActionsByRolloutGroupParent(1L, 1L, 1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void startScheduledActionsByRolloutGroupParentWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            deploymentManagement.startScheduledActionsByRolloutGroupParent(1L, 1L, 1L);
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void startScheduledActionsWithPermissionWorks() {
        assertPermissionWorks(() -> {
            deploymentManagement.startScheduledActions(List.of());
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void startScheduledActionsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            deploymentManagement.startScheduledActions(List.of());
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getAssignedDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.getAssignedDistributionSet("controllerId"));
    }

    @Test
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void getAssignedDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.getAssignedDistributionSet("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getInstalledDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.getInstalledDistributionSet("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void getInstalledDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.getInstalledDistributionSet("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.SYSTEM_ROLE })
    void deleteActionsByStatusAndLastModifiedBeforeWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.deleteActionsByStatusAndLastModifiedBefore(Set.of(Action.Status.CANCELED), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void deleteActionsByStatusAndLastModifiedBeforeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.deleteActionsByStatusAndLastModifiedBefore(Set.of(Action.Status.CANCELED), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void hasPendingCancellationsWithPermissionWorks() {
        assertPermissionWorks(() -> deploymentManagement.hasPendingCancellations(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_TARGET, SpPermission.DELETE_TARGET, SpPermission.UPDATE_TARGET })
    void hasPendingCancellationsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> deploymentManagement.hasPendingCancellations(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET })
    void cancelActionsForDistributionSetWithPermissionWorks() {
        assertPermissionWorks(() -> {
            deploymentManagement.cancelActionsForDistributionSet(DistributionSetInvalidation.CancelationType.FORCE,
                    entityFactory.distributionSet().create().build());
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void cancelActionsForDistributionSetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            deploymentManagement.cancelActionsForDistributionSet(DistributionSetInvalidation.CancelationType.FORCE,
                    entityFactory.distributionSet().create().build());
            return null;
        });
    }
}