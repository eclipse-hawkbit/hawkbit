/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.im.authentication;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Software provisioning roles that implies set of permissions and reflects high-level roles.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class SpRole {

    private static final String IMPLIES = " > ";
    private static final String LINE_BREAK = "\n";

    public static final String TARGET_ADMIN = "ROLE_TARGET_ADMIN";
    public static final String TARGET_ADMIN_HIERARCHY =
            TARGET_ADMIN + IMPLIES + SpPermission.READ_TARGET + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.READ_TARGET_SEC_TOKEN + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.UPDATE_TARGET + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.CREATE_TARGET + LINE_BREAK +
            TARGET_ADMIN + IMPLIES + SpPermission.DELETE_TARGET + LINE_BREAK;

    public static final String REPOSITORY_ADMIN = "ROLE_REPOSITORY_ADMIN";
    public static final String REPOSITORY_ADMIN_HIERARCHY =
            REPOSITORY_ADMIN + IMPLIES + SpPermission.READ_REPOSITORY + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.UPDATE_REPOSITORY + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.CREATE_REPOSITORY + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.DELETE_REPOSITORY + LINE_BREAK +
            REPOSITORY_ADMIN + IMPLIES + SpPermission.DOWNLOAD_REPOSITORY_ARTIFACT + LINE_BREAK;

    public static final String ROLLOUT_ADMIN = "ROLE_ROLLOUT_ADMIN";
    public static final String ROLLOUT_ADMIN_HIERARCHY =
            ROLLOUT_ADMIN + IMPLIES + SpPermission.READ_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.CREATE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.UPDATE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.DELETE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.HANDLE_ROLLOUT + LINE_BREAK +
            ROLLOUT_ADMIN + IMPLIES + SpPermission.APPROVE_ROLLOUT + LINE_BREAK;

    public static final String TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    public static final String TENANT_ADMIN_HIERARCHY =
            TENANT_ADMIN + IMPLIES + TARGET_ADMIN + LINE_BREAK +
            TENANT_ADMIN + IMPLIES + REPOSITORY_ADMIN + LINE_BREAK +
            TENANT_ADMIN + IMPLIES + ROLLOUT_ADMIN + LINE_BREAK +
            TENANT_ADMIN + IMPLIES + SpPermission.TENANT_CONFIGURATION + LINE_BREAK;

    public static final String SYSTEM_ADMIN_HIERARCHY =
            SpPermission.SYSTEM_ADMIN + IMPLIES + TENANT_ADMIN + LINE_BREAK;

    public static String DEFAULT_ROLE_HIERARCHY =
            TARGET_ADMIN_HIERARCHY +
            REPOSITORY_ADMIN_HIERARCHY +
            ROLLOUT_ADMIN_HIERARCHY + TENANT_ADMIN_HIERARCHY +
            SYSTEM_ADMIN_HIERARCHY;
}