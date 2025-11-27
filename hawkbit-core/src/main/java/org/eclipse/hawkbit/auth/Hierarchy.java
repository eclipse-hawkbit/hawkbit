/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.auth;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Hierarchy {

    // @formatter:off
    public static final String DEFAULT =
            SpPermission.TARGET_HIERARCHY +
            SpPermission.SOFTWARE_MODULE_HIERARCHY +
            SpPermission.DISTRIBUTION_SET_HIERARCHY +
            SpPermission.TENANT_CONFIGURATION_HIERARCHY +
            SpRole.DEFAULT_ROLE_HIERARCHY;
    // @formatter:on

    private static RoleHierarchy roleHierarchy;

    public static RoleHierarchy getRoleHierarchy() {
        return roleHierarchy;
    }

    public static void setRoleHierarchy(final RoleHierarchy roleHierarchy) {
        Hierarchy.roleHierarchy = roleHierarchy;
    }
}