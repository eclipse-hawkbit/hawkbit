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
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - RolloutGroupManagement<br/>
 * Story: SecurityTests RolloutGroupManagement
 */
class RolloutGroupManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void getPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.get(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void getWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.getWithDetailedStatus(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void countByRolloutPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.countByRollout(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void countTargetsOfRolloutsGroupPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(1L), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRolloutPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRollout(1L, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRolloutAndRsqlPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutAndRsql(1L, "name==*", PAGE), List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findTargetsOfRolloutGroupPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(1L, PAGE),
                List.of(SpPermission.READ_ROLLOUT, SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findTargetsOfRolloutGroupByRsqlPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(1L, "name==*", PAGE),
                List.of(SpPermission.READ_ROLLOUT, SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRolloutAndRsqlWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(1L, "name==*", PAGE),
                List.of(SpPermission.READ_ROLLOUT));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRolloutWithDetailedStatusPermissionsCheck() {
        assertPermissions(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(1L, PAGE), List.of(SpPermission.READ_ROLLOUT));
    }
}
