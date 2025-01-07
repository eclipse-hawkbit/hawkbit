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

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;

@Slf4j
@Feature("SecurityTests - TargetManagement")
@Story("SecurityTests TargetManagement")
public class TenantConfigurationManagementSecurityTest extends AbstractJpaIntegrationTest {

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void addOrUpdateConfigurationPermissionsCheck() {
        assertPermissions(() -> tenantConfigurationManagement.addOrUpdateConfiguration("authentication.header.enabled", true),
                List.of(SpPermission.TENANT_CONFIGURATION));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void addOrUpdateConfigurationWithMapPermissionsCheck() {
        assertPermissions(() -> tenantConfigurationManagement.addOrUpdateConfiguration(Map.of("authentication.header.enabled", true)),
                List.of(SpPermission.TENANT_CONFIGURATION));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void deleteConfigurationPermissionsCheck() {
        assertPermissions(() -> {
            tenantConfigurationManagement.deleteConfiguration("authentication.header.enabled");
            return null;
        }, List.of(SpPermission.TENANT_CONFIGURATION));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getConfigurationValuePermissionsCheck() {
        assertPermissions(() -> tenantConfigurationManagement.getConfigurationValue("authentication.header.enabled"),
                List.of(SpPermission.READ_TENANT_CONFIGURATION));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getConfigurationValueWithTypePermissionsCheck() {
        assertPermissions(() -> tenantConfigurationManagement.getConfigurationValue("authentication.header.enabled", Boolean.class),
                List.of(SpPermission.READ_TENANT_CONFIGURATION));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void getGlobalConfigurationValuePermissionsCheck() {
        assertPermissions(() -> tenantConfigurationManagement.getGlobalConfigurationValue("authentication.header.enabled", Boolean.class),
                List.of(SpPermission.READ_TENANT_CONFIGURATION));
    }

    @Test
    @Description("Tests ManagementAPI PreAuthorized method with correct and insufficient permissions.")
    void pollStatusResolverPermissionsCheck() {
        assertPermissions(() -> tenantConfigurationManagement.pollStatusResolver(), List.of(SpPermission.READ_TARGET));
    }
}
