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
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - TargetFilterQueryManagement<br/>
 * Story: SecurityTests TargetFilterQueryManagement
 */
class TargetFilterQueryManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void createPermissionsCheck() {
        assertPermissions(
                () -> targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("name").query("controllerId==id")),
                List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetFilterQueryManagement.delete(1L);
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void verifyTargetFilterQuerySyntaxPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.verifyTargetFilterQuerySyntax("controllerId==id"),
                List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findAllPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void countPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void countByAutoAssignDistributionSetIdPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.countByAutoAssignDistributionSetId(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByNamePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByName("filterName", PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void countByNamePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.countByName("filterName"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByRsql("name==id", PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByQueryPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByQuery("controllerId==id", PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByAutoAssignDistributionSetIdPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByAutoAssignDistributionSetId(1L, PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findByAutoAssignDSAndRsqlPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByAutoAssignDSAndRsql(1L, "rsqlParam", PAGE),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void findWithAutoAssignDSPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findWithAutoAssignDS(PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void getTargetFilterQueryByIdPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void getTargetFilterQueryByNamePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.getByName("filterName"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void updatePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.update(entityFactory.targetFilterQuery().update(1L)),
                List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void updateAutoAssignDSPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(1L).weight(1)),
                List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test    void cancelAutoAssignmentForDistributionSetPermissionsCheck() {
        assertPermissions(() -> {
            targetFilterQueryManagement.cancelAutoAssignmentForDistributionSet(1L);
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }
}
