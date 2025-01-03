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

@Feature("SecurityTests - RolloutGroupManagement")
@Story("SecurityTests RolloutGroupManagement")
public class RolloutGroupManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void getWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.get(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void getWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.get(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void getWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.getWithDetailedStatus(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void getWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.getWithDetailedStatus(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void countByRolloutWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.countByRollout(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void countByRolloutWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.countByRollout(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void countTargetsOfRolloutsGroupWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(1L));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void countTargetsOfRolloutsGroupWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(1L));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findByRollout(1L, PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findByRolloutWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findByRollout(1L, PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutAndRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findByRolloutAndRsql(1L, "name==*", PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findByRolloutAndRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findByRolloutAndRsql(1L, "name==*", PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT, SpPermission.READ_TARGET })
    void findTargetsOfRolloutGroupWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(1L, PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT,
            SpPermission.CREATE_ROLLOUT, SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findTargetsOfRolloutGroupWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(1L, PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT, SpPermission.READ_TARGET })
    void findTargetsOfRolloutGroupByRsqlWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(PAGE, 1L, "name==*"));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_TARGET, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findTargetsOfRolloutGroupByRsqlWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(PAGE, 1L, "name==*"));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutAndRsqlWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findByRolloutAndRsqlWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE));
    }

    @Test
    @Description("Tests that the method works when the user has the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.READ_ROLLOUT })
    void findByRolloutWithDetailedStatusWithPermissionWorks() {
        assertPermissionWorks(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE));
    }

    @Test
    @Description("Tests that the method throws InsufficientPermissionException when the user does not have the correct permission")
    @WithUser(principal = "user", authorities = { SpPermission.APPROVE_ROLLOUT, SpPermission.APPROVE_ROLLOUT, SpPermission.CREATE_ROLLOUT,
            SpPermission.DELETE_ROLLOUT, SpPermission.HANDLE_ROLLOUT, SpPermission.UPDATE_ROLLOUT })
    void findByRolloutWithDetailedStatusWithoutPermissionThrowsAccessDenied() {
        assertInsufficientPermission(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE));
    }
}
