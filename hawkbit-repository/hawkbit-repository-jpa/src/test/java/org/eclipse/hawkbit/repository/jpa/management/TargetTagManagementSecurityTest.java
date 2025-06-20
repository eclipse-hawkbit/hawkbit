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
import org.junit.jupiter.api.Test;

@Slf4j
/**
 * Feature: SecurityTests - TargetTagManagement<br/>
 * Story: SecurityTests TargetTagManagement
 */
class TargetTagManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.create(entityFactory.tag().create().name("name")), List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createCollectionPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.create(List.of(entityFactory.tag().create().name("name"))),
                List.of(SpPermission.CREATE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetTagManagement.delete("tag");
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findAllPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.findByRsql("name==tag", PAGE), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getByNamePermissionsCheck() {
        assertPermissions(() -> targetTagManagement.getByName("tag"), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getCollectionPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.get(List.of(1L)), List.of(SpPermission.READ_TARGET));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updatePermissionsCheck() {
        assertPermissions(() -> targetTagManagement.update(entityFactory.tag().update(1L)), List.of(SpPermission.UPDATE_TARGET));
    }
}
