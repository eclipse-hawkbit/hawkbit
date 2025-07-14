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

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - ConfirmationManagement<br/>
 * Story: SecurityTests ConfirmationManagement
 */
class ConfirmationManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ConfirmationManagement#findActiveActionsWaitingConfirmation() method
     */
    @Test
    void findActiveActionsWaitingConfirmationPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.findActiveActionsWaitingConfirmation("controllerId"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ConfirmationManagement#activateAutoConfirmation() method
     */
    @Test
    void activateAutoConfirmationPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.activateAutoConfirmation("controllerId", "initiator", "remark"),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ConfirmationManagement#getStatus() method
     */
    @Test
    void getStatusPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.getStatus("controllerId"), List.of(SpPermission.READ_TARGET),
                List.of(SpPermission.CREATE_TARGET));
        assertPermissions(() -> confirmationManagement.getStatus("controllerId"), List.of(SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ConfirmationManagement#confirmAction() method
     */
    @Test
    void confirmActionPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.confirmAction(1L, null, null),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ConfirmationManagement#denyAction() method
     */
    @Test
    void denyActionPermissionsCheck() {
        assertPermissions(() -> confirmationManagement.denyAction(1L, null, null),
                List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ConfirmationManagement#deactivateAutoConfirmation() method
     */
    @Test
    void deactivateAutoConfirmationPermissionsCheck() {
        assertPermissions(() -> {
            confirmationManagement.deactivateAutoConfirmation("controllerId");
            return null;
        }, List.of(SpPermission.READ_REPOSITORY, SpPermission.UPDATE_TARGET));
    }

}