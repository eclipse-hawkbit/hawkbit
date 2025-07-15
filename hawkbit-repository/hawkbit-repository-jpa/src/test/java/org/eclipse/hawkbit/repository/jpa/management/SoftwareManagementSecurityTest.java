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
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - SoftwareManagement<br/>
 * Story: SecurityTests SoftwareManagement
 */
class SoftwareManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<SoftwareModule, SoftwareModuleCreate<SoftwareModule>, SoftwareModuleUpdate> {

    @Override
    protected RepositoryManagement<SoftwareModule, SoftwareModuleCreate<SoftwareModule>, SoftwareModuleUpdate> getRepositoryManagement() {
        return softwareModuleManagement;
    }

    @Override
    protected SoftwareModuleCreate<SoftwareModule> getCreateObject() {
        return entityFactory.softwareModule().create().name("name").version("version").type("type");
    }

    @Override
    protected SoftwareModuleUpdate getUpdateObject() {
        return entityFactory.softwareModule().update(1L).locked(true);
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createMetaDataPermissionsCheck() {
        assertPermissions(
                () -> softwareModuleManagement.updateMetadata(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value")),
                List.of(SpPermission.UPDATE_REPOSITORY));
        assertPermissions(() -> {
            softwareModuleManagement.createMetadata(
                    List.of(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value")));
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deleteMetaDataPermissionsCheck() {
        assertPermissions(() -> {
            softwareModuleManagement.deleteMetadata(1L, "key");
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByAssignedToPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findByAssignedTo(1L, PAGE), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void countByAssignedToPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.countByAssignedTo(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByTextAndTypePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findByTextAndType("text", 1L, PAGE), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    void getByNameAndVersionAndTypePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findByNameAndVersionAndType("name", "version", 1L),
                List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getMetaDataBySoftwareModuleIdPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.getMetadata(1L, "key"), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findMetaDataBySoftwareModuleIdPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.getMetadata(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findMetaDataBySoftwareModuleIdAndTargetVisiblePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(1L, PAGE),
                List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findByTypePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findByType(1L, PAGE), List.of(SpPermission.READ_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void lockPermissionsCheck() {
        assertPermissions(() -> {
            softwareModuleManagement.lock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void unlockPermissionsCheck() {
        assertPermissions(() -> {
            softwareModuleManagement.unlock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updateMetaDataPermissionsCheck() {
        assertPermissions(
                () -> softwareModuleManagement.updateMetadata(entityFactory.softwareModuleMetadata().update(1L, "key").value("value")),
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findMetaDataBySoftwareModuleIdsAndTargetVisiblePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(List.of(1L)),
                List.of(SpPermission.READ_REPOSITORY));
    }

}
