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
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

@Feature("SecurityTests - ControllerManagement")
@Story("SecurityTests ControllerManagement")
class ControllerManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ControllerManagement#cancelActionStatus() method")
    void addCancelActionStatusPermissionsCheck() {
        assertPermissions(() -> controllerManagement.addCancelActionStatus(entityFactory.actionStatus().create(0L)),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getSoftwareModule() method")
    void getSoftwareModulePermissionsCheck() {
        assertPermissions(() -> controllerManagement.getSoftwareModule(1L), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#findTargetVisibleMetaDataBySoftwareModuleId() method")
    void findTargetVisibleMetaDataBySoftwareModuleIdPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findTargetVisibleMetaDataBySoftwareModuleId(List.of(1L)),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#addInformationalActionStatus() method")
    void addInformationalActionStatusPermissionsCheck() {
        assertPermissions(() -> controllerManagement.addInformationalActionStatus(entityFactory.actionStatus().create(0L)),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#addUpdateActionStatus() method")
    void addUpdateActionStatusPermissionsCheck() {
        assertPermissions(() -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(0L)),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#findActiveActionWithHighestWeight() method")
    void findActiveActionWithHighestWeightPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActiveActionWithHighestWeight("controllerId"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#findActiveActionsWithHighestWeight() method")
    void findActiveActionsWithHighestWeightPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActiveActionsWithHighestWeight("controllerId", 1),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#findActionWithDetails() method")
    void findActionWithDetailsPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActionWithDetails(1L), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#findActionStatusByAction() method")
    void findActionStatusByActionPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActionStatusByAction(Pageable.unpaged(), 1L),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#findOrRegisterTargetIfItDoesNotExist() method")
    void findOrRegisterTargetIfItDoesNotExistPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", URI.create("someaddress")),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#findOrRegisterTargetIfItDoesNotExist() method")
    void findOrRegisterTargetIfItDoesNotExistWithDetailsPermissionsCheck() {
        assertPermissions(
                () -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", URI.create("someaddress"), "name", "type"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getActionForDownloadByTargetAndSoftwareModule() method")
    void getActionForDownloadByTargetAndSoftwareModulePermissionsCheck() {
        assertPermissions(() -> controllerManagement.getActionForDownloadByTargetAndSoftwareModule("controllerId", 1L),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getPollingTime() method")
    void getPollingTimePermissionsCheck() {
        assertPermissions(() -> controllerManagement.getPollingTime(), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getMinPollingTime() method")
    void getMinPollingTimePermissionsCheck() {
        assertPermissions(() -> controllerManagement.getMinPollingTime(), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getMaxPollingTime() method")
    void getMaintenanceWindowPollCountPermissionsCheck() {
        assertPermissions(() -> controllerManagement.getMaintenanceWindowPollCount(),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getPollingTimeForAction() method")
    void getPollingTimeForActionPermissionsCheck() {
        final JpaAction action = new JpaAction();
        action.setId(1L);
        assertPermissions(() -> {
            try {
                controllerManagement.getPollingTimeForAction(action);
            } catch (final CancelActionNotAllowedException e) {
                // expected since action is not found
            }
            return null;
        }, List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#hasTargetArtifactAssigned() method")
    void hasTargetArtifactAssignedPermissionsCheck() {
        assertPermissions(() -> controllerManagement.hasTargetArtifactAssigned("controllerId", "sha1Hash"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#hasTargetArtifactAssigned() method")
    void hasTargetArtifactAssignedByIdPermissionsCheck() {
        assertPermissions(() -> controllerManagement.hasTargetArtifactAssigned(1L, "sha1Hash"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#updateControllerAttributes() method")
    void updateControllerAttributesPermissionsCheck() {
        assertPermissions(() -> controllerManagement.updateControllerAttributes("controllerId", Map.of(), null),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getByControllerId() method")
    void getByControllerIdPermissionsCheck() {
        assertPermissions(() -> controllerManagement.getByControllerId("controllerId"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
        assertPermissions(() -> controllerManagement.getByControllerId("controllerId"),
                List.of(SpPermission.SpringEvalExpressions.SYSTEM_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#get() method")
    void getPermissionsCheck() {
        assertPermissions(() -> controllerManagement.get(1L), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
        assertPermissions(() -> controllerManagement.get(1L), List.of(SpPermission.SpringEvalExpressions.SYSTEM_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getActionHistoryMessages() method")
    void getActionHistoryMessagesPermissionsCheck() {
        assertPermissions(() -> controllerManagement.getActionHistoryMessages(1L, 1),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#cancelAction() method")
    void cancelActionPermissionsCheck() {
        final JpaAction action = new JpaAction();
        action.setId(1L);
        assertPermissions(() -> {
            try {
                controllerManagement.cancelAction(action);
            } catch (final CancelActionNotAllowedException e) {
                // expected since action is not found
            }
            return null;
        }, List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#updateActionExternalRef() method")
    void updateActionExternalRefPermissionsCheck() {
        assertPermissions(() -> {
            controllerManagement.updateActionExternalRef(1L, "externalRef");
            return null;
        }, List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getActionByExternalRef() method")
    void getActionByExternalRefPermissionsCheck() {
        assertPermissions(() -> controllerManagement.getActionByExternalRef("externalRef"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#deleteExistingTarget() method")
    void deleteExistingTargetPermissionsCheck() {
        assertPermissions(() -> {
            controllerManagement.deleteExistingTarget("controllerId");
            return null;
        }, List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#getInstalledActionByTarget() method")
    void getInstalledActionByTargetPermissionsCheck() {
        final Target target = testdataFactory.createTarget();
        assertPermissions(
                () -> controllerManagement.getInstalledActionByTarget(target),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#activateAutoConfirmation() method")
    void activateAutoConfirmationPermissionsCheck() {
        assertPermissions(
                () -> controllerManagement.activateAutoConfirmation("controllerId", "initiator", "remark"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#deactivateAutoConfirmation() method")
    void deactivateAutoConfirmationPermissionsCheck() {
        assertPermissions(() -> {
            controllerManagement.deactivateAutoConfirmation("controllerId");
            return null;
        }, List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

    @Test
    @Description("Tests ControllerManagement#updateOfflineAssignedVersion() method")
    void updateOfflineAssignedVersionPermissionsCheck() {
        assertPermissions(() -> controllerManagement.updateOfflineAssignedVersion("controllerId", "distributionName", "version"),
                List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE));
    }

}