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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - TargetTagManagement")
@Story("SecurityTests TargetTagManagement")
class TargetTagManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.create(entityFactory.tag().create().name("name")), List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createCollectionPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.create(List.of(entityFactory.tag().create().name("name"))),
                List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetTagManagement.delete("tag");
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findAllPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.findByRsql("name==tag", PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByNamePermissionsCheck() {
        assertPermissions(() -> targetTagManagement.getByName("tag"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getCollectionPermissionsCheck() {
        assertPermissions(() -> targetTagManagement.get(List.of(1L)), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updatePermissionsCheck() {
        assertPermissions(() -> targetTagManagement.update(entityFactory.tag().update(1L)), List.of(SpPermission.UPDATE_TARGET));
    }
}
