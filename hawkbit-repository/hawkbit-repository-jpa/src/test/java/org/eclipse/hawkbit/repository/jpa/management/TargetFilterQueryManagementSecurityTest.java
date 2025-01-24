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
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

@Feature("SecurityTests - TargetFilterQueryManagement")
@Story("SecurityTests TargetFilterQueryManagement")
class TargetFilterQueryManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createPermissionsCheck() {
        assertPermissions(
                () -> targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("name").query("controllerId==id")),
                List.of(SpPermission.CREATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deletePermissionsCheck() {
        assertPermissions(() -> {
            targetFilterQueryManagement.delete(1L);
            return null;
        }, List.of(SpPermission.DELETE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void verifyTargetFilterQuerySyntaxPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.verifyTargetFilterQuerySyntax("controllerId==id"),
                List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findAllPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findAll(PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.count(), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByAutoAssignDistributionSetIdPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.countByAutoAssignDistributionSetId(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByNamePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByName(PAGE, "filterName"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void countByNamePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.countByName("filterName"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByRsqlPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByRsql(PAGE, "name==id"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByQueryPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByQuery(PAGE, "controllerId==id"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByAutoAssignDistributionSetIdPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByAutoAssignDistributionSetId(PAGE, 1L),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findByAutoAssignDSAndRsqlPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findByAutoAssignDSAndRsql(PAGE, 1L, "rsqlParam"),
                List.of(SpPermission.READ_TARGET, SpPermission.READ_REPOSITORY));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findWithAutoAssignDSPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.findWithAutoAssignDS(PAGE), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getTargetFilterQueryByIdPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.get(1L), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getTargetFilterQueryByNamePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.getByName("filterName"), List.of(SpPermission.READ_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updatePermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.update(entityFactory.targetFilterQuery().update(1L)),
                List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updateAutoAssignDSPermissionsCheck() {
        assertPermissions(() -> targetFilterQueryManagement.updateAutoAssignDS(new AutoAssignDistributionSetUpdate(1L).weight(1)),
                List.of(SpPermission.UPDATE_TARGET));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void cancelAutoAssignmentForDistributionSetPermissionsCheck() {
        assertPermissions(() -> {
            targetFilterQueryManagement.cancelAutoAssignmentForDistributionSet(1L);
            return null;
        }, List.of(SpPermission.UPDATE_TARGET));
    }
}
