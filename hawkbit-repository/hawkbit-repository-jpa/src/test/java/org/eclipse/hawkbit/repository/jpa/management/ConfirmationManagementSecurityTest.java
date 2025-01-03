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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - ConfirmationManagement")
@Story("SecurityTests ConfirmationManagement")
class ConfirmationManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void findActiveActionsWaitingConfirmationWithPermissionWorks() {
        assertPermissionWorks(() -> confirmationManagement.findActiveActionsWaitingConfirmation("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void findActiveActionsWaitingConfirmationWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> confirmationManagement.findActiveActionsWaitingConfirmation("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET })
    void activateAutoConfirmationWithPermissionWorks() {
        assertPermissionWorks(() -> confirmationManagement.activateAutoConfirmation("controllerId", "initiator", "remark"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void activateAutoConfirmationWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> confirmationManagement.activateAutoConfirmation("controllerId", "initiator", "remark"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET })
    void getStatusWithPermissionWorks() {
        assertPermissionWorks(() -> confirmationManagement.getStatus("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void getStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> confirmationManagement.getStatus("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET })
    void autoConfirmActiveActionsWithPermissionWorks() {
        assertPermissionWorks(() -> confirmationManagement.autoConfirmActiveActions("controllerId"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void autoConfirmActiveActionsWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> confirmationManagement.autoConfirmActiveActions("controllerId"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET })
    void confirmActionWithPermissionWorks() {
        assertPermissionWorks(() -> confirmationManagement.confirmAction(1L, null, null));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void confirmActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> confirmationManagement.confirmAction(1L, null, null));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void denyActionWithPermissionWorks() {
        assertPermissionWorks(() -> confirmationManagement.denyAction(1L, null, null));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void denyActionWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> confirmationManagement.denyAction(1L, null, null));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.SpringEvalExpressions.CONTROLLER_ROLE })
    void deactivateAutoConfirmationWithPermissionWorks() {
        assertPermissionWorks(() -> {
            confirmationManagement.deactivateAutoConfirmation("controllerId");
            return null;
        });
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.CREATE_REPOSITORY, SpPermission.DELETE_REPOSITORY,
            SpPermission.UPDATE_REPOSITORY })
    void deactivateAutoConfirmationWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> {
            confirmationManagement.deactivateAutoConfirmation("controllerId");
            return null;
        });
    }
}