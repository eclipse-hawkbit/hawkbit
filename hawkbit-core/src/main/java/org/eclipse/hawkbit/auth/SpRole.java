/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
import lombok.extern.slf4j.Slf4j;

/**
 * Software provisioning roles that implies set of permissions and reflects high-level roles.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class SpRole {

    public static final String TARGET_ADMIN = "ROLE_TARGET_ADMIN";
    public static final String REPOSITORY_ADMIN = "ROLE_REPOSITORY_ADMIN";
    public static final String ROLLOUT_ADMIN = "ROLE_ROLLOUT_ADMIN";
    public static final String TENANT_ADMIN = "ROLE_TENANT_ADMIN";

    /** The role which contains the spring security context in case the system is executing code which is necessary to be privileged. */
    public static final String SYSTEM_ROLE = "ROLE_SYSTEM_CODE";
    /** The role which contains in the spring security context in case a controller is authenticated */
    public static final String CONTROLLER_ROLE = "ROLE_CONTROLLER";
    /** The role which contained in the spring security context in case that a controller is authenticated, but only as 'anonymous'. */
    public static final String CONTROLLER_ROLE_ANONYMOUS = "ROLE_CONTROLLER_ANONYMOUS";

    private static final String IMPLIES = " > ";
    private static final String LINE_BREAK = "\n";

    // @formatter:off
    public static final String TARGET_ADMIN_HIERARCHY =
            TARGET_ADMIN + IMPLIES + SpPermission.CREATE_TARGET + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.READ_TARGET + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.READ_TARGET_SECURITY_TOKEN + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.UPDATE_TARGET + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.DELETE_TARGET + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.CREATE_PREFIX + SpPermission.TARGET_TYPE + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.READ_TARGET_TYPE + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.UPDATE_TARGET_TYPE + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.DELETE_TARGET_TYPE + LINE_BREAK;
    public static final String REPOSITORY_ADMIN_HIERARCHY =
            REPOSITORY_ADMIN + IMPLIES + SpPermission.CREATE_PREFIX + SpPermission.SOFTWARE_MODULE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.READ_PREFIX + SpPermission.SOFTWARE_MODULE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.UPDATE_PREFIX + SpPermission.SOFTWARE_MODULE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.DELETE_PREFIX + SpPermission.SOFTWARE_MODULE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.READ_SOFTWARE_MODULE_ARTIFACT + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.CREATE_PREFIX + SpPermission.SOFTWARE_MODULE_TYPE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.READ_PREFIX + SpPermission.SOFTWARE_MODULE_TYPE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.UPDATE_PREFIX + SpPermission.SOFTWARE_MODULE_TYPE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.DELETE_PREFIX + SpPermission.SOFTWARE_MODULE_TYPE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.CREATE_PREFIX + SpPermission.DISTRIBUTION_SET + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.READ_PREFIX + SpPermission.DISTRIBUTION_SET + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.UPDATE_PREFIX + SpPermission.DISTRIBUTION_SET + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.DELETE_PREFIX + SpPermission.DISTRIBUTION_SET + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.CREATE_PREFIX + SpPermission.DISTRIBUTION_SET_TYPE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.READ_PREFIX + SpPermission.DISTRIBUTION_SET_TYPE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.UPDATE_PREFIX + SpPermission.DISTRIBUTION_SET_TYPE + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.DELETE_PREFIX + SpPermission.DISTRIBUTION_SET_TYPE + LINE_BREAK;
    public static final String ROLLOUT_ADMIN_HIERARCHY =
            ROLLOUT_ADMIN + IMPLIES + SpPermission.CREATE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.READ_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.UPDATE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.DELETE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.HANDLE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.APPROVE_ROLLOUT + LINE_BREAK;
    public static final String TENANT_ADMIN_HIERARCHY =
            TENANT_ADMIN + IMPLIES + TARGET_ADMIN + LINE_BREAK +
            TENANT_ADMIN + IMPLIES + REPOSITORY_ADMIN + LINE_BREAK +
            TENANT_ADMIN + IMPLIES + ROLLOUT_ADMIN + LINE_BREAK +
            TENANT_ADMIN + IMPLIES + SpPermission.TENANT_CONFIGURATION + LINE_BREAK;
    public static final String SYSTEM_ROLE_HIERARCHY =
            SYSTEM_ROLE + IMPLIES + TENANT_ADMIN + LINE_BREAK +
            SYSTEM_ROLE + IMPLIES + SpPermission.SYSTEM_ADMIN + LINE_BREAK;

    public static final String DEFAULT_ROLE_HIERARCHY =
            TARGET_ADMIN_HIERARCHY +
            REPOSITORY_ADMIN_HIERARCHY +
            ROLLOUT_ADMIN_HIERARCHY +
            TENANT_ADMIN_HIERARCHY +
            SYSTEM_ROLE_HIERARCHY;
    // @formatter:on
}