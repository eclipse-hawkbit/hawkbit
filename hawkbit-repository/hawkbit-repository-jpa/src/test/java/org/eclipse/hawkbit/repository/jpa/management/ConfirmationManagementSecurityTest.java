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
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - ConfirmationManagement")
@Story("SecurityTests ConfirmationManagement")
class ConfirmationManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ConfirmationManagement#findActiveActionsWaitingConfirmation() method")
    void findActiveActionsWaitingConfirmationPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.findActiveActionsWaitingConfirmation("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ConfirmationManagement#activateAutoConfirmation() method")
    void activateAutoConfirmationPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.activateAutoConfirmation("controllerId", "initiator", "remark"),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ConfirmationManagement#getStatus() method")
    void getStatusPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.getStatus("controllerId"), List.of(SpPermission.READ_TARGET),
                List.of(SpPermission.CREATE_TARGET));
        assertPermissions(() -> confirmationManagement.getStatus("controllerId"), List.of(SpPermission.SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ConfirmationManagement#autoConfirmActiveActions() method")
    void autoConfirmActiveActionsPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.autoConfirmActiveActions("controllerId"),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ConfirmationManagement#confirmAction() method")
    void confirmActionPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.confirmAction(1L, null, null),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ConfirmationManagement#denyAction() method")
    void denyActionPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.denyAction(1L, null, null),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ConfirmationManagement#deactivateAutoConfirmation() method")
    void deactivateAutoConfirmationPermissionsCheck() {
        assertPermissions(() -> {
            confirmationManagement.deactivateAutoConfirmation("controllerId");
            return null;
        }, List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

}