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
@Feature("SecurityTests - SystemManagement")
@Story("SecurityTests SystemManagement")
public class SystemManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void findTenantsPermissionWorks() {
        assertPermissions(() -> systemManagement.findTenants(PAGE), List.of(SpPermission.SYSTEM_ADMIN));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deleteTenantPermissionsCheck() {
        assertPermissions(() -> {
            systemManagement.deleteTenant("tenant");
            return null;
        }, List.of(SpPermission.SYSTEM_ADMIN));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void forEachTenantTenantPermissionsCheck() {
        assertPermissions(() -> {
            systemManagement.forEachTenant(log::info);
            return null;
        }, List.of(SpPermission.SpringEvalExpressions.SYSTEM_ROLE));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getSystemUsageStatisticsWithTenantsPermissionsCheck() {
        assertPermissions(() -> systemManagement.getSystemUsageStatisticsWithTenants(), List.of(SpPermission.SYSTEM_ADMIN));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getSystemUsageStatisticsPermissionsCheck() {
        assertPermissions(() -> systemManagement.getSystemUsageStatistics(), List.of(SpPermission.SYSTEM_ADMIN));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getTenantMetadataPermissionsCheck() {
        assertPermissions(() -> systemManagement.getTenantMetadata(), List.of(SpPermission.READ_REPOSITORY, SpPermission.READ_TARGET, SpPermission.READ_TENANT_CONFIGURATION));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getTenantMetadataByTenantPermissionsCheck() {
        assertPermissions(() -> systemManagement.getTenantMetadata(1L), List.of(SpPermission.SpringEvalExpressions.SYSTEM_ROLE));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void createTenantMetadataPermissionsCheck() {
        assertPermissions(() -> systemManagement.createTenantMetadata("tenant"), List.of(SpPermission.SpringEvalExpressions.SYSTEM_ROLE));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void updateTenantMetadataPermissionsCheck() {
        assertPermissions(() -> systemManagement.updateTenantMetadata(1L), List.of(SpPermission.TENANT_CONFIGURATION));
    }
}
