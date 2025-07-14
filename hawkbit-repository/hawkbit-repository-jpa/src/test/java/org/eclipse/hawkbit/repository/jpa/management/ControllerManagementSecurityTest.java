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

import org.eclipse.hawkbit.im.authentication.SpRole;
import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * Feature: SecurityTests - ControllerManagement<br/>
 * Story: SecurityTests ControllerManagement
 */
class ControllerManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ControllerManagement#cancelActionStatus() method
     */
    @Test
    void addCancelActionStatusPermissionsCheck() {
        assertPermissions(() -> controllerManagement.addCancelActionStatus(entityFactory.actionStatus().create(0L)),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#getSoftwareModule() method
     */
    @Test
    void getSoftwareModulePermissionsCheck() {
        assertPermissions(() -> controllerManagement.getSoftwareModule(1L), List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#findTargetVisibleMetaDataBySoftwareModuleId() method
     */
    @Test
    void findTargetVisibleMetaDataBySoftwareModuleIdPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findTargetVisibleMetaDataBySoftwareModuleId(List.of(1L)),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#addInformationalActionStatus() method
     */
    @Test
    void addInformationalActionStatusPermissionsCheck() {
        assertPermissions(() -> controllerManagement.addInformationalActionStatus(entityFactory.actionStatus().create(0L)),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#addUpdateActionStatus() method
     */
    @Test
    void addUpdateActionStatusPermissionsCheck() {
        assertPermissions(() -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(0L)),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#findActiveActionWithHighestWeight() method
     */
    @Test
    void findActiveActionWithHighestWeightPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActiveActionWithHighestWeight("controllerId"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#findActiveActionsWithHighestWeight() method
     */
    @Test
    void findActiveActionsWithHighestWeightPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActiveActionsWithHighestWeight("controllerId", 1),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#findActionWithDetails() method
     */
    @Test
    void findActionWithDetailsPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActionWithDetails(1L), List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#findActionStatusByAction() method
     */
    @Test
    void findActionStatusByActionPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findActionStatusByAction(1L, Pageable.unpaged()),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#findOrRegisterTargetIfItDoesNotExist() method
     */
    @Test
    void findOrRegisterTargetIfItDoesNotExistPermissionsCheck() {
        assertPermissions(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", URI.create("someaddress")),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#findOrRegisterTargetIfItDoesNotExist() method
     */
    @Test
    void findOrRegisterTargetIfItDoesNotExistWithDetailsPermissionsCheck() {
        assertPermissions(
                () -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("controllerId", URI.create("someaddress"), "name", "type"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#getActionForDownloadByTargetAndSoftwareModule() method
     */
    @Test
    void getActionForDownloadByTargetAndSoftwareModulePermissionsCheck() {
        assertPermissions(() -> controllerManagement.getActionForDownloadByTargetAndSoftwareModule("controllerId", 1L),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#getPollingTime() method
     */
    @Test
    void getPollingTimePermissionsCheck() {
        assertPermissions(() -> controllerManagement.getPollingTime(null), List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#getPollingTimeForAction() method
     */
    @Test
    void getPollingTimeForActionPermissionsCheck() {
        final JpaAction action = new JpaAction();
        action.setId(1L);
        assertPermissions(() -> {
            try {
                controllerManagement.getPollingTimeForAction(action.getTarget(), action);
            } catch (final CancelActionNotAllowedException e) {
                // expected since action is not found
            }
            return null;
        }, List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#hasTargetArtifactAssigned() method
     */
    @Test
    void hasTargetArtifactAssignedPermissionsCheck() {
        assertPermissions(() -> controllerManagement.hasTargetArtifactAssigned("controllerId", "sha1Hash"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#hasTargetArtifactAssigned() method
     */
    @Test
    void hasTargetArtifactAssignedByIdPermissionsCheck() {
        assertPermissions(() -> controllerManagement.hasTargetArtifactAssigned(1L, "sha1Hash"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#updateControllerAttributes() method
     */
    @Test
    void updateControllerAttributesPermissionsCheck() {
        assertPermissions(() -> controllerManagement.updateControllerAttributes("controllerId", Map.of(), null),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#getByControllerId() method
     */
    @Test
    void getByControllerIdPermissionsCheck() {
        assertPermissions(() -> controllerManagement.getByControllerId("controllerId"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
        assertPermissions(() -> controllerManagement.getByControllerId("controllerId"),
                List.of(SpRole.SYSTEM_ROLE));
    }

    /**
     * Tests ControllerManagement#get() method
     */
    @Test
    void getPermissionsCheck() {
        assertPermissions(() -> controllerManagement.get(1L), List.of(SpringEvalExpressions.CONTROLLER_ROLE));
        assertPermissions(() -> controllerManagement.get(1L), List.of(SpRole.SYSTEM_ROLE));
    }

    /**
     * Tests ControllerManagement#getActionHistoryMessages() method
     */
    @Test
    void getActionHistoryMessagesPermissionsCheck() {
        assertPermissions(() -> controllerManagement.getActionHistoryMessages(1L, 1),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#cancelAction() method
     */
    @Test
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
        }, List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#updateActionExternalRef() method
     */
    @Test
    void updateActionExternalRefPermissionsCheck() {
        assertPermissions(() -> {
            controllerManagement.updateActionExternalRef(1L, "externalRef");
            return null;
        }, List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#getActionByExternalRef() method
     */
    @Test
    void getActionByExternalRefPermissionsCheck() {
        assertPermissions(() -> controllerManagement.getActionByExternalRef("externalRef"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#deleteExistingTarget() method
     */
    @Test
    void deleteExistingTargetPermissionsCheck() {
        assertPermissions(() -> {
            controllerManagement.deleteExistingTarget("controllerId");
            return null;
        }, List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#getInstalledActionByTarget() method
     */
    @Test
    void getInstalledActionByTargetPermissionsCheck() {
        final Target target = testdataFactory.createTarget();
        assertPermissions(
                () -> controllerManagement.getInstalledActionByTarget(target),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#activateAutoConfirmation() method
     */
    @Test
    void activateAutoConfirmationPermissionsCheck() {
        assertPermissions(
                () -> controllerManagement.activateAutoConfirmation("controllerId", "initiator", "remark"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#deactivateAutoConfirmation() method
     */
    @Test
    void deactivateAutoConfirmationPermissionsCheck() {
        assertPermissions(() -> {
            controllerManagement.deactivateAutoConfirmation("controllerId");
            return null;
        }, List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

    /**
     * Tests ControllerManagement#updateOfflineAssignedVersion() method
     */
    @Test
    void updateOfflineAssignedVersionPermissionsCheck() {
        assertPermissions(() -> controllerManagement.updateOfflineAssignedVersion("controllerId", "distributionName", "version"),
                List.of(SpringEvalExpressions.CONTROLLER_ROLE));
    }

}