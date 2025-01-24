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

@Feature("SecurityTests - RolloutGroupManagement")
@Story("SecurityTests RolloutGroupManagement")
class RolloutGroupManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.get(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.getWithDetailedStatus(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByRolloutPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.countByRollout(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countTargetsOfRolloutsGroupPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRolloutPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRollout(1L, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRolloutAndRsqlPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutAndRsql(1L, "name==*", PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findTargetsOfRolloutGroupPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(1L, PAGE),
                List.of(SpPermission.READ_ROLLOUT, SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findTargetsOfRolloutGroupByRsqlPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(PAGE, 1L, "name==*"),
                List.of(SpPermission.READ_ROLLOUT, SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRolloutAndRsqlWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE),
                List.of(SpPermission.READ_ROLLOUT));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRolloutWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }
}
