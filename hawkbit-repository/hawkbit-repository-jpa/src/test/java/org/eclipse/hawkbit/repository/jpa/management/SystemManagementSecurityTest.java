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
import org.eclipse.hawkbit.im.authentication.SpRole;
import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

@Slf4j
/**
 * Feature: SecurityTests - SystemManagement<br/>
 * Story: SecurityTests SystemManagement
 */
class SystemManagementSecurityTest extends AbstractJpaIntegrationTest {

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void findTenantsPermissionWorks() {
        assertPermissions(() -> systemManagement.findTenants(PAGE), List.of(SpPermission.SYSTEM_ADMIN));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void deleteTenantPermissionsCheck() {
        assertPermissions(() -> {
            systemManagement.deleteTenant("tenant");
            return null;
        }, List.of(SpPermission.SYSTEM_ADMIN));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void forEachTenantTenantPermissionsCheck() {
        assertPermissions(() -> {
            systemManagement.forEachTenant(log::info);
            return null;
        }, List.of(SpRole.SYSTEM_ROLE));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getSystemUsageStatisticsWithTenantsPermissionsCheck() {
        assertPermissions(() -> systemManagement.getSystemUsageStatisticsWithTenants(), List.of(SpPermission.SYSTEM_ADMIN));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getSystemUsageStatisticsPermissionsCheck() {
        assertPermissions(() -> systemManagement.getSystemUsageStatistics(), List.of(SpPermission.SYSTEM_ADMIN));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getTenantMetadataPermissionsCheck() {
        assertPermissions(() -> systemManagement.getTenantMetadata(), List.of(SpPermission.READ_REPOSITORY), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> systemManagement.getTenantMetadata(), List.of(SpPermission.READ_TARGET), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> systemManagement.getTenantMetadata(), List.of(SpPermission.READ_TENANT_CONFIGURATION), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> systemManagement.getTenantMetadata(), List.of(SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getTenantMetadataWithoutDetailsPermissionsCheck() {
        assertPermissions(() -> systemManagement.getTenantMetadataWithoutDetails(), List.of(SpPermission.READ_REPOSITORY), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> systemManagement.getTenantMetadataWithoutDetails(), List.of(SpPermission.READ_TARGET), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> systemManagement.getTenantMetadataWithoutDetails(), List.of(SpPermission.READ_TENANT_CONFIGURATION), List.of(SpPermission.CREATE_REPOSITORY));
        assertPermissions(() -> systemManagement.getTenantMetadataWithoutDetails(), List.of(SpringEvalExpressions.CONTROLLER_ROLE), List.of(SpPermission.CREATE_REPOSITORY));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void getTenantMetadataByTenantPermissionsCheck() {
        assertPermissions(() -> systemManagement.getTenantMetadata(1L), List.of(SpRole.SYSTEM_ROLE));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void createTenantMetadataPermissionsCheck() {
        assertPermissions(() -> systemManagement.createTenantMetadata("tenant"), List.of(SpRole.SYSTEM_ROLE));
    }

    /**
     * Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.
     */
    @Test
    void updateTenantMetadataPermissionsCheck() {
        assertPermissions(() -> systemManagement.updateTenantMetadata(1L), List.of(SpPermission.TENANT_CONFIGURATION));
    }
}
