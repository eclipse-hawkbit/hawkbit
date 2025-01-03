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

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@Feature("SecurityTests - ControllerManagement")
@Story("SecurityTests ControllerManagement")
class ControllerManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void addCancelActionStatusWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.addCancelActionStatus(entityFactory.actionStatus().create(0L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void addCancelActionStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.addCancelActionStatus(entityFactory.actionStatus().create(0L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getSoftwareModuleWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getSoftwareModule(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getSoftwareModuleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getSoftwareModule(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void findTargetVisibleMetaDataBySoftwareModuleIdWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.findTargetVisibleMetaDataBySoftwareModuleId(List.of(1L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findTargetVisibleMetaDataBySoftwareModuleIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.findTargetVisibleMetaDataBySoftwareModuleId(List.of(1L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void addInformationalActionStatusWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.addInformationalActionStatus(entityFactory.actionStatus().create(0L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void addInformationalActionStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.addInformationalActionStatus(entityFactory.actionStatus().create(0L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void addUpdateActionStatusWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(0L)));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void addUpdateActionStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(0L)));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void findActiveActionWithHighestWeightWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.findActiveActionWithHighestWeight("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findActiveActionWithHighestWeightWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.findActiveActionWithHighestWeight("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void findActiveActionsWithHighestWeightWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.findActiveActionsWithHighestWeight("controllerId", 1));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findActiveActionsWithHighestWeightWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.findActiveActionsWithHighestWeight("controllerId", 1));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void findActionWithDetailsWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.findActionWithDetails(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findActionWithDetailsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.findActionWithDetails(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void findActionStatusByActionWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.findActionStatusByAction(Pageable.unpaged(), 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findActionStatusByActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.findActionStatusByAction(Pageable.unpaged(), 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void findOrRegisterTargetIfItDoesNotExistWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", URI.create("someaddress")));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findOrRegisterTargetIfItDoesNotExistWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", null));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void findOrRegisterTargetIfItDoesNotExistWithDetailsWithPermissionWorks() {
        assertPermissionWorks(
                () -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", URI.create("someaddress"), "name", "type"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findOrRegisterTargetIfItDoesNotExistWithDetailsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", null, "name", "type"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getActionForDownloadByTargetAndSoftwareModuleWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getActionForDownloadByTargetAndSoftwareModule("controllerId", 1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getActionForDownloadByTargetAndSoftwareModuleWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getActionForDownloadByTargetAndSoftwareModule("controllerId", 1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getPollingTimeWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getPollingTime());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getPollingTimeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getPollingTime());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getMinPollingTimeWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getMinPollingTime());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getMinPollingTimeWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getMinPollingTime());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getMaintenanceWindowPollCountWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getMaintenanceWindowPollCount());
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getMaintenanceWindowPollCountWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getMaintenanceWindowPollCount());
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getPollingTimeForActionWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getPollingTimeForAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getPollingTimeForActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getPollingTimeForAction(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void hasTargetArtifactAssignedWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.hasTargetArtifactAssigned("controllerId", "sha1Hash"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void hasTargetArtifactAssignedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.hasTargetArtifactAssigned("controllerId", "sha1Hash"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void hasTargetArtifactAssignedByIdWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.hasTargetArtifactAssigned(1L, "sha1Hash"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void hasTargetArtifactAssignedByIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.hasTargetArtifactAssigned(1L, "sha1Hash"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void registerRetrievedWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.registerRetrieved(1L, "message"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void registerRetrievedWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.registerRetrieved(1L, "message"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void updateControllerAttributesWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.updateControllerAttributes("controllerId", Map.of(), null));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void updateControllerAttributesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.updateControllerAttributes("controllerId", Map.of(), null));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getByControllerIdWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getByControllerIdWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getByControllerId("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getActionHistoryMessagesWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getActionHistoryMessages(1L, 1));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getActionHistoryMessagesWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getActionHistoryMessages(1L, 1));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void cancelActionWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.cancelAction(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void cancelActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.cancelAction(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void updateActionExternalRefWithPermissionWorks() {
        assertPermissionWorks(() -> {
            controllerManagement.updateActionExternalRef(1L, "externalRef");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void updateActionExternalRefWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            controllerManagement.updateActionExternalRef(1L, "externalRef");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getActionByExternalRefWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getActionByExternalRef("externalRef"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getActionByExternalRefWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getActionByExternalRef("externalRef"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void deleteExistingTargetWithPermissionWorks() {
        assertPermissionWorks(() -> {
            controllerManagement.deleteExistingTarget("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void deleteExistingTargetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            controllerManagement.deleteExistingTarget("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void getInstalledActionByTargetWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.getInstalledActionByTarget("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getInstalledActionByTargetWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.getInstalledActionByTarget("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void activateAutoConfirmationWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.activateAutoConfirmation("controllerId", "initiator", "remark"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void activateAutoConfirmationWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.activateAutoConfirmation("controllerId", "initiator", "remark"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void deactivateAutoConfirmationWithPermissionWorks() {
        assertPermissionWorks(() -> {
            controllerManagement.deactivateAutoConfirmation("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void deactivateAutoConfirmationWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            controllerManagement.deactivateAutoConfirmation("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void updateOfflineAssignedVersionWithPermissionWorks() {
        assertPermissionWorks(() -> controllerManagement.updateOfflineAssignedVersion("controllerId", "distributionName", "version"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void updateOfflineAssignedVersionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> controllerManagement.updateOfflineAssignedVersion("controllerId", "distributionName", "version"));
    }
}