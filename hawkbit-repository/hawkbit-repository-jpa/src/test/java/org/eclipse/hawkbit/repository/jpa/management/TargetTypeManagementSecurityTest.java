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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
/**
 * Feature: SecurityTests - TargetTypeManagement<br/>
 * Story: SecurityTests TargetTypeManagement
 */
class TargetTypeManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByKeyPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.getByKey("key"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByNamePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.getByName("name"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByNamePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.countByName("name"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.create(entityFactory.targetType().create().name("name")),
                List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createCollectionPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.create(List.of(entityFactory.targetType().create().name("name"))),
                List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetTypeManagement.delete(1L);
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findAllPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.findByRsql("name==tag", PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByNamePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.findByName("name", PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getCollectionPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.get(List.of(1L)), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updatePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.update(entityFactory.targetType().update(1L)), List.of(SpPermission.UPDATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void assignCompatibleDistributionSetTypesPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.assignCompatibleDistributionSetTypes(1L, List.of(1L)),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY })
    void unassignDistributionSetTypePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.unassignDistributionSetType(1L, 1L),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

}
