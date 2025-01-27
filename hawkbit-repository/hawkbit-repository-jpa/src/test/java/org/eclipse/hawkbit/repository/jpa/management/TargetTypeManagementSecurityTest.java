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
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - TargetTypeManagement")
@Story("SecurityTests TargetTypeManagement")
class TargetTypeManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByKeyPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.getByKey("key"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getByNamePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.getByName("name"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByNamePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.countByName("name"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.create(entityFactory.targetType().create().name("name")),
                List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createCollectionPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.create(List.of(entityFactory.targetType().create().name("name"))),
                List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetTypeManagement.delete(1L);
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findAllPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.findByRsql(PAGE, "name==tag"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByNamePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.findByName(PAGE, "name"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getCollectionPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.get(List.of(1L)), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updatePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.update(entityFactory.targetType().update(1L)), List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void assignCompatibleDistributionSetTypesPermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.assignCompatibleDistributionSetTypes(1L, List.of(1L)),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    @WithUser(principal = "user", authorities = { SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY })
    void unassignDistributionSetTypePermissionsCheck() {
        assertPermissions(() -> targetTypeManagement.unassignDistributionSetType(1L, 1L),
                List.of(SpPermission.UPDATE_TARGET, SpPermission.READ_REPOSITORY));
    }

}
