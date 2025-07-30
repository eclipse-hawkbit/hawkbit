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
import java.util.Map;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpRole;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValueCreate;
import org.junit.jupiter.api.Test;

/**
 * Feature: SecurityTests - SoftwareManagement<br/>
 * Story: SecurityTests SoftwareManagement
 */
class SoftwareManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<SoftwareModule, SoftwareModuleManagement.Create, SoftwareModuleManagement.Update> {

    @Override
    protected RepositoryManagement getRepositoryManagement() {
        return softwareModuleManagement;
    }

    @Override
    protected SoftwareModuleManagement.Create getCreateObject() {
        return SoftwareModuleManagement.Create.builder().type(getASmType()).name("name").version("version").build();
    }

    @Override
    protected SoftwareModuleManagement.Update getUpdateObject() {
        return SoftwareModuleManagement.Update.builder().id(1L).locked(true).build();
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createMetaDataPermissionsCheck() {
        assertPermissions(
                () -> {
                    softwareModuleManagement.createMetadata(1L, "key", new MetadataValueCreate("value"));
                    return null;
                },
                List.of(SpPermission.UPDATE_REPOSITORY));
        assertPermissions(() -> {
            softwareModuleManagement.createMetadata(
                    1L,
                    Map.of("key", new MetadataValueCreate("value")));
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
                () -> {
                    softwareModuleManagement.createMetadata(1L, "key", new MetadataValueCreate("value"));
                    return null;
                },
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findMetaDataBySoftwareModuleIdsAndTargetVisiblePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(List.of(1L)),
                List.of(SpRole.SYSTEM_ROLE));
    }
}