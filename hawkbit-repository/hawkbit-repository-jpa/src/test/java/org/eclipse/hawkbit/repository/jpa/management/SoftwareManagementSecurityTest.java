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
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractRepositoryManagementSecurityTest;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - SoftwareManagement")
@Story("SecurityTests SoftwareManagement")
public class SoftwareManagementSecurityTest
        extends AbstractRepositoryManagementSecurityTest<SoftwareModule, SoftwareModuleCreate, SoftwareModuleUpdate> {

    @Override
    protected RepositoryManagement<SoftwareModule, SoftwareModuleCreate, SoftwareModuleUpdate> getRepositoryManagement() {
        return softwareModuleManagement;
    }

    @Override
    protected SoftwareModuleCreate getCreateObject() {
        return entityFactory.softwareModule().create().name("name").version("version").type("type");
    }

    @Override
    protected SoftwareModuleUpdate getUpdateObject() {
        return entityFactory.softwareModule().update(1L).locked(true);
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createMetaDataPermissionsCheck() {
        assertPermissions(
                () -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value")),
                List.of(SpPermission.UPDATE_REPOSITORY));
        assertPermissions(() -> softwareModuleManagement.createMetaData(
                List.of(entityFactory.softwareModuleMetadata().create(1L).key("key").value("value"))), List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deleteMetaDataPermissionsCheck() {
        assertPermissions(() -> {
            softwareModuleManagement.deleteMetaData(1L, "key");
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByAssignedToPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findByAssignedTo(PAGE, 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByAssignedToPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.countByAssignedTo(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByTextAndTypePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findByTextAndType(PAGE, "text", 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    void getByNameAndVersionAndTypePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.getByNameAndVersionAndType("name", "version", 1L),
                List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getMetaDataBySoftwareModuleIdPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.getMetaDataBySoftwareModuleId(1L, "key"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findMetaDataBySoftwareModuleIdPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findMetaDataBySoftwareModuleId(PAGE, 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countMetaDataBySoftwareModuleIdPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.countMetaDataBySoftwareModuleId(1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findMetaDataBySoftwareModuleIdAndTargetVisiblePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(PAGE, 1L),
                List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findMetaDataByRsqlPermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findMetaDataByRsql(PAGE, 1L, "key==value"), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByTypePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findByType(PAGE, 1L), List.of(SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void lockPermissionsCheck() {
        assertPermissions(() -> {
            softwareModuleManagement.lock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void unlockPermissionsCheck() {
        assertPermissions(() -> {
            softwareModuleManagement.unlock(1L);
            return null;
        }, List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updateMetaDataPermissionsCheck() {
        assertPermissions(
                () -> softwareModuleManagement.updateMetaData(entityFactory.softwareModuleMetadata().update(1L, "key").value("value")),
                List.of(SpPermission.UPDATE_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findMetaDataBySoftwareModuleIdsAndTargetVisiblePermissionsCheck() {
        assertPermissions(() -> softwareModuleManagement.findMetaDataBySoftwareModuleIdsAndTargetVisible(List.of(1L)),
                List.of(SpPermission.READ_REPOSITORY));
    }

}
